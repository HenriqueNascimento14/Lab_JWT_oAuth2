# Relatório técnico — OAuth2 e JWT na prática

**Nome:** Henrique Silva do Nascimento  
**RA:** 1710832413038  
**Data de entrega:** 27/11/2026

## 1. Introdução

Neste trabalho, usei a SocialConnect como exemplo para estudar como proteger rotas em uma API com Spring Boot. Escolhi uma situação simples: o operador precisa consultar os beneficiários, mas só o administrador pode excluir um cadastro. Para demonstrar essa diferença, o laboratório inclui login, controle de acesso por perfil, renovação de tokens e logout.

## 2. Investigação teórica

**Autenticação** é quando o sistema verifica quem está tentando entrar. No projeto, isso acontece quando a API confere o usuário e a senha. **Autorização** é quando o sistema verifica o que aquela conta pode fazer. Por exemplo, um operador pode entrar normalmente e ainda assim ter a exclusão bloqueada.

O **OAuth2** é um padrão que permite a um aplicativo receber acesso a determinados recursos. Nesse modelo, um servidor de autorização emite o token, e a API valida esse token antes de liberar o acesso. O **JWT** é um formato que pode ser usado para representar o token. No laboratório, a própria aplicação emite o JWT no login e usa o Spring Security para validá-lo. Um fluxo OAuth2 com servidor de autorização, como Authorization Code com PKCE, seria uma evolução do projeto. [RFC 6749](https://www.rfc-editor.org/rfc/rfc6749) · [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

O **access token** é enviado no cabeçalho `Authorization: Bearer <token>` ao chamar uma rota protegida. No projeto, o JWT informa quem é o usuário, qual é o perfil dele e quando o token vence. A assinatura permite verificar se essas informações foram alteradas. Como o conteúdo do JWT pode ser lido, o projeto mantém senhas e outros dados sensíveis fora dele. [RFC 7519](https://www.rfc-editor.org/rfc/rfc7519.html)

O **refresh token** serve para conseguir um novo access token sem digitar a senha de novo. A API gera um valor aleatório e guarda apenas o seu hash em memória. A cada renovação, ela entrega um novo refresh token e invalida o anterior. Isso evita que o mesmo refresh seja usado duas vezes. [RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html)

## 3. Mini-laboratório implementado

No `POST /auth/login`, o Spring Security confere as credenciais e a API devolve os dois tokens. O access token vale **cinco minutos** e a sessão de renovação vale **uma hora a partir do login**. O `POST /auth/refresh` troca os tokens. O `POST /auth/logout` invalida o refresh enviado, mas o access token já emitido continua válido até vencer.

A regra de exclusão foi colocada diretamente no método responsável pela operação:

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
```

A primeira anotação indica a rota de exclusão. A segunda faz o Spring verificar se a conta tem perfil de administrador antes de executar o método. Se o token estiver ausente ou inválido, a API responde `401`. Se o token for válido, mas a conta não tiver permissão para excluir, responde `403`. Quando um administrador exclui um cadastro existente, a resposta é `204`, com o corpo vazio. [Spring Security — Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)

No Postman, fiz o login como operador e usei o access token para consultar os beneficiários. A API retornou os dois cadastros de exemplo. Depois tentei excluir o beneficiário de ID `1` com o mesmo token e recebi `403 Forbidden`. Esse foi o resultado esperado, porque o operador pode consultar os cadastros, mas a exclusão exige outro perfil. O projeto também inclui testes automatizados para o login, a exclusão por perfil, a renovação e a revogação do refresh token.

## 4. Conexão com a SocialConnect

Na SocialConnect, essa regra permite que uma pessoa consulte os cadastros para trabalhar sem ter permissão para apagá-los. No laboratório, as contas, os beneficiários e as sessões ficam em memória para facilitar a demonstração. Ao reiniciar a API, os dados de exemplo são restaurados e os tokens anteriores deixam de funcionar.

Em uma versão usada de verdade, seria importante manter as sessões em um armazenamento persistente e usar HTTPS. Também seria necessário decidir se o logout deve aguardar os cinco minutos de validade do access token ou se o acesso precisa ser interrompido imediatamente.

## 5. Conclusão e aprendizados

O teste que mais me ajudou a entender o tema foi usar o **mesmo token** para duas ações. Ele funcionou para listar os beneficiários, mas a exclusão devolveu `403`. Assim, consegui ver na prática que fazer login comprova quem está acessando, enquanto a permissão depende da ação que a pessoa quer realizar. Se eu continuasse o projeto, o próximo passo seria ligar a API a um servidor de autorização OAuth2 e repetir os testes.

## Referências

- IETF. [RFC 6749 — The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749)
- IETF. [RFC 7519 — JSON Web Token](https://www.rfc-editor.org/rfc/rfc7519.html)
- IETF. [RFC 9700 — Best Current Practice for OAuth 2.0 Security](https://www.rfc-editor.org/rfc/rfc9700.html)
- Spring Security. [OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- Spring Security. [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
