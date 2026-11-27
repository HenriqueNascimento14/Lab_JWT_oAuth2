Declaração de Uso de IA – Avaliação A2

Autor

* Nome: Henrique Silva do Nascimento
* RA: 1710832413038
* Tema: OAuth2 e JWT na Prática

Ferramentas utilizadas

* [x]	ChatGPT
* [ ]	Claude (Anthropic)
* [ ]	GitHub Copilot
* [ ]	Codeium
* [ ]	Gemini
* [ ]	Nenhuma ferramenta de IA foi utilizada

Como utilizei

Usei o ChatGPT para entender o que são OAuth 2.0 e JWT e como esses conceitos se relacionam com o laboratório da SocialConnect. Também pedi ajuda com a implementação e solicitei que cada mudança no código fosse explicada para eu conseguir acompanhar o que estava sendo feito.

Usei a ferramenta para entender como testar o login, a renovação do token e as permissões no Postman. Um exemplo que me ajudou foi o teste de exclusão de beneficiários. O operador conseguia consultar a lista, mas só o administrador tinha permissão para excluir um cadastro.

O que eu entendo 100%

* No laboratório, a própria SocialConnect confere o usuário e a senha e entrega um token de acesso no formato JWT.
* Eu uso o token de acesso para chamar as rotas protegidas. Quando ele vence, uso o refresh token para conseguir um novo token de acesso sem digitar a senha de novo.
* O operador pode listar os beneficiários. Para excluir um cadastro, o @PreAuthorize exige o perfil de administrador.
* Sem um token válido, a API retorna 401. Se o operador tiver um token válido e tentar excluir um cadastro, a API retorna 403 por falta de permissão.

O que precisei estudar mais

No começo, eu confundia OAuth 2.0 com JWT. Precisei rever essa diferença para entender que o JWT era o formato do token de acesso que eu usava no Postman. A integração com um servidor de autorização OAuth 2.0 ficou na parte teórica do trabalho.

Também precisei entender melhor em que momento usar cada token. Ficou mais fácil quando associei o token de acesso às rotas protegidas e o refresh token à rota de renovação.

Prompts utilizado

1. “Me explique o que são OAuth 2.0 e JWT, como funcionam e qual é a diferença entre eles.”
2. “Altere esse arquivo e implemente o lab de JWT na SocialConnect. Me explique o que mudou no meu código e por que você fez cada mudança.”
3. “Como uso o token de acesso e o refresh token da maneira correta no Postman? Me mostre o que fazer quando o token de acesso vencer.”
4. “Como eu testo no Postman se o operador consegue listar os beneficiários e se só o administrador consegue excluir um cadastro?”

Declaração de autenticidade

Declaro que:

1. Revisei o código e o relatório entregues e compreendo o conteúdo dos dois.
2. Sou capaz de explicar cada linha do código e cada parágrafo do relatório em uma arguição oral.
3. Usei a IA como apoio para tirar dúvidas e complementar meu aprendizado.
4. Não copiei código ou texto de colegas ou de fontes não autorizadas.

Data: 26/11/2026