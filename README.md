# A2 — OAuth2 e JWT na prática

Mini-laboratório em Java e Spring Boot que usa a SocialConnect para demonstrar login com JWT, controle de acesso por perfil, renovação de tokens e logout.

O exemplo tem dois perfis. O **operador** pode consultar os beneficiários. O **administrador** pode consultar e excluir. Os usuários e os beneficiários são fictícios e ficam em memória, então a API pode ser executada sem instalar um banco de dados.

## 1. Como ligar a API

Você precisa do **JDK 17 ou superior**, do **Maven 3.9** e do **Postman** para seguir o roteiro manual. Para conferir as duas primeiras instalações, abra o terminal e rode:

```bash
java -version
mvn -v
```

Abra o terminal na pasta que contém o arquivo `pom.xml` e execute:

```bash
mvn spring-boot:run
```

Na primeira vez, o Maven vai baixar as dependências. Quando aparecer `Started SocialConnectApplication` no terminal, a API estará pronta. **Deixe esse terminal aberto** enquanto faz os testes no Postman.

O endereço da API é `http://127.0.0.1:8080`. Se a porta 8080 estiver ocupada, inicie o projeto em outra porta:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Nesse caso, troque `8080` por `8081` nos endereços usados no Postman.

## 2. Contas de demonstração

| Usuário | Senha | O que pode fazer |
| --- | --- | --- |
| `operador` | `OperadorLab2026!` | Consultar beneficiários |
| `admin` | `AdminLab2026!` | Consultar e excluir beneficiários |

## 3. Teste manual no Postman

Depois de clicar em **Send**, confira o **código da resposta** na área inferior do Postman, acima do corpo. Alguns testes retornam o corpo em branco. Nesses casos, o código indica o que aconteceu.

### Passo 1 — Fazer login como operador

1. Crie uma nova requisição HTTP no Postman.
2. Troque o método de **GET** para **POST** e cole o endereço `http://127.0.0.1:8080/auth/login`.
3. Clique em **Body → raw**. No seletor ao lado de **raw**, troque **Text** por **JSON**.
4. Cole este corpo e clique em **Send**:

```json
{
  "username": "operador",
  "password": "OperadorLab2026!"
}
```

**Resultado esperado:** `200 OK`. A resposta traz `accessToken`, `refreshToken` e `expiresIn`.

- Copie o valor de **`accessToken`**, sem aspas, para usar nos próximos dois passos. Ele vale **5 minutos**.
- Guarde também o **`refreshToken`** do operador para o teste de renovação.
- Se aparecer **`415 Unsupported Media Type`**, volte ao **Body** e confira se selecionou **JSON**. Na aba **Headers**, o `Content-Type` deve ser `application/json`.

### Passo 2 — Listar os beneficiários como operador

1. Abra uma nova requisição com o método **GET**.
2. Use o endereço `http://127.0.0.1:8080/beneficiarios`.
3. Vá em **Authorization → Type → Bearer Token**.
4. Cole o **`accessToken` do operador** no campo **Token**, sem aspas. O Postman acrescenta `Bearer` automaticamente.
5. Deixe o **Body** vazio e clique em **Send**.

**Resultado esperado:** `200 OK`. Se a API acabou de ser iniciada, a lista contém **Ana Souza** (ID `1`) e **Bruno Lima** (ID `2`).

### Passo 3 — Tentar excluir como operador

Na requisição de listagem, troque **GET** por **DELETE** e adicione `/1` ao final do endereço:

```text
http://127.0.0.1:8080/beneficiarios/1
```

Com o **Body** vazio, clique em **Send** nas duas situações abaixo:

| Configuração em Authorization | Resultado esperado |
| --- | --- |
| **No Auth** | `401 Unauthorized`. A requisição está sem token. |
| **Bearer Token** com o access token do operador | `403 Forbidden`. O operador foi reconhecido, mas a exclusão exige o perfil de administrador. |

O **`403` é o resultado certo** desse teste, mesmo se o corpo aparecer em branco. Se der `401` usando o token do operador, confira se ele está no campo correto. Se já passaram cinco minutos, faça login novamente e use o token novo.

### Passo 4 — Excluir como administrador

Volte à requisição de login do passo 1 e troque o corpo por:

```json
{
  "username": "admin",
  "password": "AdminLab2026!"
}
```

Clique em **Send** e copie o **`accessToken` do administrador**. Volte ao `DELETE /beneficiarios/1` e, em **Authorization → Bearer Token**, substitua o token do operador pelo do administrador. Clique em **Send**.

**Resultado esperado:** `204 No Content`. O corpo em branco é normal e significa que a exclusão foi concluída. Para confirmar, faça `GET /beneficiarios` com o token do administrador. Deve aparecer apenas **Bruno Lima**. Se repetir o DELETE do mesmo ID, a resposta será `404 Not Found`.

### Passo 5 — Renovar os tokens

1. Crie uma requisição **POST** para `http://127.0.0.1:8080/auth/refresh`.
2. Em **Authorization**, escolha **No Auth**.
3. Em **Body → raw → JSON**, cole o corpo abaixo e substitua o texto pelo **refresh token guardado no login do operador**:

```json
{
  "refreshToken": "COLE_AQUI_O_REFRESH_TOKEN"
}
```

**Resultado esperado:** `200 OK`, com um `accessToken` e um `refreshToken` novos. Guarde o **refresh novo** para o próximo passo. Se clicar em **Send** de novo com o **refresh antigo** ainda no corpo, a resposta será `401`. Cada refresh token só pode ser usado uma vez para renovar.

A sessão de renovação dura **uma hora a partir do login**. Você pode testar a renovação assim que fizer login, sem esperar o access token expirar.

### Passo 6 — Fazer logout e conferir a revogação

1. Crie uma requisição **POST** para `http://127.0.0.1:8080/auth/logout`.
2. Em **Authorization**, escolha **No Auth**. Em **Body → raw → JSON**, envie `{"refreshToken":"COLE_AQUI_O_REFRESH_NOVO"}` com o refresh recebido no passo anterior.
3. Clique em **Send**. O esperado é `204 No Content`.
4. Depois, tente usar esse mesmo refresh em `POST /auth/refresh`. O esperado é `401 Unauthorized`.

O logout impede novas renovações com esse refresh. O access token já emitido continua válido até a expiração.

## 4. Rodar todos os testes de uma vez

Para usar a coleção pronta, com a API ligada e os beneficiários de exemplo restaurados:

1. No Postman, clique em **Import** e selecione `docs/SocialConnect.postman_collection.json`.
2. Abra a coleção **SocialConnect - A2 Tema 2** e selecione **Run collection**.
3. Rode as **12 requisições na ordem original**. A coleção guarda os tokens e os reutiliza automaticamente.
4. Se estiver usando outra porta, atualize a variável **`baseUrl`** da coleção antes de rodar.

Se você já excluiu a Ana Souza no teste manual, **reinicie a API antes de rodar a coleção**. Para reiniciar, volte ao terminal, pressione `Ctrl+C` e execute `mvn spring-boot:run` novamente. Isso repõe os beneficiários e invalida os tokens anteriores.

Para executar os **3 testes de integração** do projeto, rode na pasta do `pom.xml`:

```bash
mvn test
```

## 5. Como o código está organizado

| Arquivo | O que faz |
| --- | --- |
| `SecurityConfig.java` | Configura as contas de teste, o Spring Security e a validação do JWT. |
| `AuthController.java` | Recebe as requisições de login, renovação e logout. |
| `TokenService.java` | Emite tokens, controla a validade e armazena o hash do refresh token. |
| `BeneficiarioController.java` | Lista os beneficiários e restringe a exclusão com `@PreAuthorize`. |
| `SocialConnectIntegrationTest.java` | Verifica por HTTP o login, as permissões, a renovação e a revogação. |

A própria API emite os tokens no login. O [relatório técnico](RELATORIO.md) explica como o laboratório se relaciona com OAuth2 e como seria a integração com um servidor de autorização. O [AI_USAGE.md](AI_USAGE.md) descreve como usei o ChatGPT no trabalho.
