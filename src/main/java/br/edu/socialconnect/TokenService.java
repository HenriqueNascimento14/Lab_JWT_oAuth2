package br.edu.socialconnect;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class TokenService {
    private static final Duration ACCESS_LIFETIME = Duration.ofMinutes(5);
    private static final Duration SESSION_LIFETIME = Duration.ofHours(1);

    private final JwtEncoder encoder;
    private final UserDetailsService users;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, RefreshSession> sessions = new HashMap<>();

    public TokenService(JwtEncoder encoder, UserDetailsService users) {
        this.encoder = encoder;
        this.users = users;
    }

    public synchronized TokenResponse startSession(String username) {
        Instant now = Instant.now();
        removeExpiredSessions(now);
        return issueTokens(new RefreshSession(username, now.plus(SESSION_LIFETIME)), now);
    }

    public synchronized TokenResponse refresh(String refreshToken) {
        Instant now = Instant.now();
        removeExpiredSessions(now);
        String oldHash = hash(refreshToken);
        RefreshSession session = sessions.get(oldHash);
        if (session == null) {
            throw new ResponseStatusException(UNAUTHORIZED);
        }
        TokenResponse tokens = issueTokens(session, now);
        sessions.remove(oldHash);
        return tokens;
    }

    public synchronized void revoke(String refreshToken) {
        sessions.remove(hash(refreshToken));
    }

    private TokenResponse issueTokens(RefreshSession session, Instant now) {
        var user = users.loadUserByUsername(session.username());
        var claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(session.username())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_LIFETIME))
                .id(UUID.randomUUID().toString())
                .claim("authorities", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(hash(refreshToken), session);
        return new TokenResponse("Bearer", accessToken, refreshToken, ACCESS_LIFETIME.toSeconds());
    }

    private void removeExpiredSessions(Instant now) {
        sessions.values().removeIf(session -> !session.expiresAt().isAfter(now));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private record RefreshSession(String username, Instant expiresAt) {}

    public record TokenResponse(String tokenType, String accessToken, String refreshToken, long expiresIn) {}
}
