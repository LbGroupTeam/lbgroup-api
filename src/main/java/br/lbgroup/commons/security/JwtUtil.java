package br.lbgroup.commons.security;

import br.lbgroup.commons.util.Cryptographer;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.http.Cookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    private final String SECRET_KEY = Cryptographer.generateSecretKeyAsString();
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
    private static final long JWT_EXPIRATION_MS = Duration.ofMinutes(10).toMillis();

    public JwtUtil() throws Exception {
    }

    public String generateToken(String subject) {
        return JWT.create()
                .withSubject(subject)
                .withIssuer("lbgroup")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .sign(algorithm);
    }

    public Cookie generateCookie(String subject) {
        Cookie cookie = new Cookie("authToken", generateToken(subject));
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (JWT_EXPIRATION_MS / 1000));
        return cookie;
    }

    public String extractSubject(String token) {
        try {
            return JWT.require(algorithm).build().verify(token).getSubject();
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
