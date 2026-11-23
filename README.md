# A2 — OAuth2 e JWT na prática

Mini-laboratório em Java que usa uma versão didática da SocialConnect para demonstrar autenticação por JWT, autorização por perfil e renovação de tokens. Os dados são fictícios e ficam em memória.

## Como executar

1. Instale Java 17 ou superior e Maven 3.9. No terminal, confira com `java -version` e `mvn -v`.
2. Extraia o ZIP e abra o terminal na pasta que contém o `pom.xml`.
3. Rode `mvn spring-boot:run`. A primeira execução baixa as dependências. A API sobe em `http://127.0.0.1:8080`.
4. Importe `docs/SocialConnect.postman_collection.json` no Postman. Execute a coleção em ordem no Collection Runner. Se precisar repetir a exclusão com o mesmo ID, reinicie a API para repor os dados.

Se a porta 8080 estiver ocupada, use `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` e altere a variável `baseUrl` da coleção. Execute `mvn test` para rodar os testes automatizados.

## Contas de demonstração

| Usuário | Senha local | Perfil |
|---|---|---|
| `operador` | `OperadorLab2026!` | USER |
| `admin` | `AdminLab2026!` | ADMIN |

## Roteiro manual

1. Envie `POST /auth/login` com `Content-Type: application/json` e o corpo `{"username":"operador","password":"OperadorLab2026!"}`. O retorno inclui `accessToken`, `refreshToken` e `expiresIn`, em segundos.
2. Em `GET /beneficiarios`, configure a autorização do Postman como **Bearer Token** e cole o `accessToken`. O resultado esperado é **200** com dois beneficiários fictícios.
3. Em `DELETE /beneficiarios/1`, envie a requisição primeiro sem token (**401**) e depois com o token de operador (**403**).
4. Faça login com o administrador e repita o DELETE usando o token dele. O resultado deve ser **204**. Se repetir com o mesmo ID, obtenha **404**.
5. Envie `POST /auth/refresh` sem o cabeçalho de autorização e com o corpo `{"refreshToken":"COLE_AQUI"}`. A API devolve dois tokens novos. Reutilize o refresh anterior para conferir a resposta **401**.
6. Envie o refresh novo no mesmo formato para `POST /auth/logout`. O resultado deve ser **204**. Tentar renovar com esse refresh novamente deve retornar **401**.

## Como o código está organizado

| Arquivo | Responsabilidade |
|---|---|
| `SecurityConfig.java` | Configura o Spring Security, as contas locais, a chave e a validação do JWT. |
| `AuthController.java` | Recebe os pedidos de login, renovação e logout. |
| `TokenService.java` | Emite o JWT, armazena o hash do refresh e gerencia a renovação. |
| `BeneficiarioController.java` | Lista e exclui registros fictícios com regras de acesso por perfil. |
| `SocialConnectIntegrationTest.java` | Testa o fluxo por HTTP com a aplicação em execução. |

O JWT dura 5 minutos, e a sessão de renovação dura 1 hora a partir do login. O logout invalida o refresh enviado. O JWT já emitido permanece válido até a expiração. Reiniciar a aplicação recria a chave e apaga as sessões em memória.

O [relatório técnico](RELATORIO.md) explica a diferença entre o login local do laboratório e um fluxo OAuth2 com servidor de autorização. Antes de entregar, preencha seus dados no relatório e em [AI_USAGE.md](AI_USAGE.md), execute o roteiro e revise as conclusões com o que você observou.
