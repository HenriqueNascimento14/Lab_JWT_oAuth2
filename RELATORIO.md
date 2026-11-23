# Relatório técnico — OAuth2 e JWT na prática

- Nome: [Seu nome]
- RA: [Seu RA]
- Data de entrega: 27/11/2026

## 1. Introdução

A SocialConnect precisa diferenciar quem pode consultar um cadastro de quem pode excluir um beneficiário. Se as duas operações exigissem apenas que o usuário estivesse logado, qualquer conta com acesso à listagem poderia apagar registros. O laboratório investiga como o Spring Security verifica a identidade do usuário e aplica permissões diferentes a rotas da mesma API.

O objetivo foi desenvolver um exemplo executável de login, verificação de JWT, renovação e logout. A demonstração usa dois perfis e registra o comportamento esperado em cada situação. A investigação teórica também apresenta o papel do OAuth2 e como um servidor de autorização se encaixaria na arquitetura da SocialConnect.

## 2. Investigação teórica

### Autenticação, autorização e OAuth2

A autenticação verifica a identidade apresentada. No laboratório, ela ocorre no login, quando o servidor confere o usuário e a senha. A autorização verifica se essa identidade tem permissão para fazer uma ação. Um operador autenticado pode listar beneficiários, enquanto a exclusão exige o perfil de administrador.

O OAuth2 é um padrão de autorização. Em um caso de acesso delegado, o usuário autoriza um aplicativo cliente a acessar determinados recursos. O servidor de autorização emite os tokens. O servidor de recursos, que pode ser a própria API da SocialConnect, valida as credenciais recebidas nas requisições. As permissões delegadas podem ser representadas por escopos. Por exemplo, um cliente pode receber acesso para ler um recurso, enquanto a API verifica separadamente as permissões do usuário para executar a ação. [RFC 6749](https://www.rfc-editor.org/rfc/rfc6749)

Em uma aplicação web com um provedor externo, o fluxo Authorization Code com PKCE é uma opção para obter tokens. O cliente inicia o fluxo com um desafio derivado de um valor que guarda temporariamente. Ao trocar o código por tokens, ele apresenta o valor original. O servidor de autorização confere se corresponde ao desafio inicial, dificultando o uso de um código interceptado. O projeto didático emite tokens por endpoints próprios e usa o suporte de Resource Server do Spring Security para validá-los. A integração com um servidor de autorização OAuth2 fica como evolução do laboratório. [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html) · [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### Access token e JWT

O access token acompanha uma requisição protegida no cabeçalho `Authorization: Bearer <token>`. JWT é um formato para representar informações chamadas *claims*. Um JWT assinado na forma compacta tem três partes separadas por pontos, correspondentes ao cabeçalho, às informações e à assinatura. A assinatura permite verificar se o conteúdo foi alterado. As informações ficam codificadas e podem ser lidas por quem recebe o token, por isso o laboratório guarda apenas identificadores e permissões nele. [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html)

A API usa `sub` para identificar a conta, `iss` para identificar o emissor, `iat` para registrar o instante da emissão e `exp` para delimitar a validade. A autorização usa a *claim* personalizada `authorities`. No exemplo, um access token válido pode ser conferido pela API sem consultar o armazenamento de refresh tokens. A contrapartida é que uma permissão registrada no JWT continua presente até a expiração daquele token. Isso afeta situações como a retirada de acesso de um funcionário. Um tempo de validade menor reduz a janela, mas aumenta a frequência de renovação.

A assinatura HS256 usa uma chave compartilhada para emitir e validar. Ela atende ao exemplo porque uma única aplicação possui as duas responsabilidades. Se o sistema tiver um servidor de autorização e várias APIs, uma assinatura assimétrica permite que o emissor assine com a chave privada e que cada API valide com a chave pública. Nesse cenário, também é importante validar a audiência para aceitar somente tokens destinados à API que está recebendo a requisição. [Spring Security — JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

### Renovação, armazenamento e revogação

O refresh token permite solicitar um novo access token sem repetir as credenciais a cada expiração. Como tem validade mais longa, precisa de proteção tanto durante a transmissão quanto no armazenamento. No laboratório, o refresh é um valor aleatório opaco. O cliente recebe o valor original no JSON, enquanto o servidor guarda somente o resumo SHA-256. Quando o cliente solicita a renovação, o servidor calcula o resumo do valor recebido e procura a sessão correspondente. Para uso real, as requisições devem trafegar por HTTPS e os tokens devem ficar fora de logs, URLs e código-fonte. [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

A cada renovação a aplicação cria outro refresh token e remove o registro anterior. Repetir o token antigo retorna 401. Em um sistema de produção, é possível manter a relação entre os tokens de uma mesma sessão para revogar o token ativo ao detectar a reutilização de um anterior. O laboratório faz a rotação de uso único e simula a revogação no logout. Ele descarta os registros antigos, portanto a recusa de um token já usado não revoga automaticamente a sessão que emitiu o sucessor. [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

## 3. Mini-laboratório implementado

A aplicação foi organizada em um controlador de autenticação, um serviço de emissão de tokens, uma classe de configuração do Spring Security e um controlador de beneficiários. Com isso, as rotas tratam a entrada e a saída HTTP, enquanto o serviço concentra a lógica de validade e renovação. As contas de teste são criadas em memória, e suas senhas são armazenadas nesse ambiente com BCrypt. Os registros de beneficiários usam dados inventados e são recriados a cada inicialização.

No login, o `AuthenticationManager` confere as credenciais antes da emissão dos tokens. O access token tem validade de cinco minutos. A sessão de refresh tem validade de uma hora a partir do login. Quando ele é renovado, a aplicação mantém o prazo original da sessão. A verificação e a troca do refresh ocorrem em uma operação sincronizada, o que impede que duas requisições simultâneas consumam com sucesso o mesmo token dentro da mesma instância do laboratório.

Para acessar a listagem ou a exclusão, o cliente envia o JWT. O Spring Security confere a assinatura, a validade e o emissor. Depois, converte a lista `authorities` em permissões que podem ser usadas pela anotação `@PreAuthorize`. O método de exclusão começa assim.

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> excluir(@PathVariable long id) {
    if (beneficiarios.remove(id) == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
}
```

A configuração ativa a segurança de métodos com `@EnableMethodSecurity`. O Spring avalia `hasRole('ADMIN')` usando a autoridade `ROLE_ADMIN`. Sem credenciais válidas, a API responde 401. Se a conta estiver autenticada, mas faltar a autoridade requerida, a resposta é 403. O código procura o beneficiário somente depois de autorizar o método. Com permissão, um ID existente gera 204, e um ID ausente gera 404. [Spring Security — Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

A coleção Postman demonstra login, listagem, exclusão, renovação e revogação. Os testes automatizados exercitam a API por HTTP. Um caso verifica as credenciais, outro compara os perfis na exclusão, e o último confere a troca do refresh, a recusa do token anterior e a revogação no logout. Ao apresentar o trabalho, é possível demonstrar a diferença entre 401 e 403 rodando a mesma requisição com três condições de acesso.

## 4. Conexão com a SocialConnect

Na SocialConnect, a escolha de quem pode excluir precisa refletir as responsabilidades da equipe. O exemplo separa operador e administrador, mas os perfis reais dependeriam das regras do projeto. Uma organização poderia dar a leitura apenas a pessoas autorizadas em uma unidade. Nesse caso, verificar o perfil seria só parte da regra, pois a API também precisaria conferir a unidade à qual pertence o cadastro solicitado.

A versão original do sistema poderia aproveitar as regras de autorização nos controladores ou serviços existentes, integrando a autenticação a um servidor de autorização. Se houvesse várias instâncias da aplicação, os dados de sessão e revogação precisariam ficar em armazenamento persistente e compartilhado. O laboratório usa uma chave temporária e memória local para simplificar a execução. Reiniciar a API invalida os tokens emitidos e repõe os beneficiários fictícios.

A revogação no logout tem um efeito concreto. Ela impede novas renovações com o refresh revogado, enquanto o último access token pode funcionar até vencer. Se a SocialConnect precisar bloquear o acesso imediatamente em caso de desligamento ou comprometimento de conta, será necessário consultar um estado de revogação ou usar um modelo de validação que permita conferir o estado da autorização. O custo dessa decisão aparece em cada requisição que precisar fazer a consulta.

## 5. Conclusão e aprendizados

O laboratório reúne três decisões que podem ser testadas separadamente. A primeira é aceitar a identidade após validar as credenciais. A segunda é verificar se essa identidade tem acesso à operação pedida. A terceira é decidir por quanto tempo a sessão pode continuar e como encerrar a renovação. A exclusão de um beneficiário evidencia a importância da segunda decisão, pois é possível estar autenticado e receber 403 por falta de permissão.

A próxima evolução técnica seria integrar a API a um servidor de autorização e repetir os mesmos testes de acesso. Isso permitiria comparar a emissão local de tokens com um fluxo de autorização completo e avaliar como lidar com troca de chaves, encerramento de sessões e permissões que mudam no decorrer do dia.

[Antes da entrega, acrescente suas observações após executar a coleção e descreva o que você aprendeu ou faria diferente.]

## Referências

- IETF. [RFC 6749 — The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749)
- IETF. [RFC 7519 — JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519.html)
- IETF. [RFC 9700 — Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- Spring Security. [OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- Spring Security. [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
