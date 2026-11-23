package br.edu.socialconnect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SocialConnectIntegrationTest {
    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ObjectMapper json;

    @Test
    void loginValidaCredenciais() {
        assertThat(http.postForEntity("/auth/login", Map.of(), String.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(login("operador", "incorreta").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var response = login("operador", "OperadorLab2026!");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void exclusaoExigeJwtDeAdministrador() throws Exception {
        String operador = token(login("operador", "OperadorLab2026!"), "accessToken");
        String admin = token(login("admin", "AdminLab2026!"), "accessToken");

        assertThat(delete(null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(delete("invalido")).isEqualTo(HttpStatus.UNAUTHORIZED);
        var headers = new HttpHeaders();
        headers.setBearerAuth(operador);
        assertThat(http.exchange("/beneficiarios", HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delete(operador)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(delete(admin)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(delete(admin)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void refreshRotacionaTokenELogoutRevogaONovo() throws Exception {
        var login = login("operador", "OperadorLab2026!");
        String original = token(login, "refreshToken");
        assertThat(delete(original)).isEqualTo(HttpStatus.UNAUTHORIZED);

        var renewal = http.postForEntity("/auth/refresh", Map.of("refreshToken", original), String.class);
        assertThat(renewal.getStatusCode()).isEqualTo(HttpStatus.OK);
        String rotated = token(renewal, "refreshToken");
        assertThat(rotated).isNotEqualTo(original);
        assertThat(token(renewal, "accessToken")).isNotEqualTo(token(login, "accessToken"));
        assertThat(http.postForEntity("/auth/refresh", Map.of("refreshToken", original), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.postForEntity("/auth/logout", Map.of("refreshToken", rotated), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.postForEntity("/auth/refresh", Map.of("refreshToken", rotated), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<String> login(String username, String password) {
        return http.postForEntity("/auth/login", Map.of("username", username, "password", password), String.class);
    }

    private String token(ResponseEntity<String> response, String field) throws Exception {
        JsonNode body = json.readTree(response.getBody());
        assertThat(body.path(field).asText()).isNotBlank();
        return body.path(field).asText();
    }

    private HttpStatus delete(String token) {
        var headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return HttpStatus.valueOf(http.exchange(
                "/beneficiarios/1", HttpMethod.DELETE, new HttpEntity<>(headers), Void.class)
                .getStatusCode().value());
    }
}
