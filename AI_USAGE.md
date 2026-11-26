Declaração de Uso de IA — Avaliação A2
- Nome: Henrique Silva do Nascimento
- RA: 1710832413038
- Tema: OAuth2 e JWT na Prática
- Ferramenta utilizada: ChatGPT
  
Como usei a IA
Escolhi o Tema 2 e usei o ChatGPT para entender o que o edital pedia e montar o mini-laboratório da SocialConnect. O ChatGPT gerou a primeira versão do código em Spring Boot, os testes de integração, a coleção do Postman, o README e uma versão inicial do relatório técnico. Também usei a ferramenta para entender como o projeto funciona e tirar dúvidas durante os testes.
Exemplos de pedidos que fiz
- “haja como um estudante de spring boot”
- “eu escolho tema 2”
- “me explique o que vc fez”
  
O que testei e aprendi
Testei a API manualmente no Postman, consegui entrar como operador e receber o accessToken e o refreshToken.

Usei o accessToken para listar os beneficiários e confirmei que a API retornou os dois cadastros de exemplo. Em seguida, tentei excluir a beneficiária de ID 1 com o mesmo token e recebi 403 Forbidden. Esse teste me ajudou a entender a diferença entre autenticação e autorização. A API reconheceu minha conta de operador e permitiu a consulta. A exclusão foi bloqueada porque essa ação exige o perfil de administrador.
