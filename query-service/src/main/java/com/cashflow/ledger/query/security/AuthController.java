package com.cashflow.ledger.query.security;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Local-only token issuance so the service can be exercised without a real
 * identity provider. See the class comment on {@link JwtService}.
 */
@RestController
public class AuthController {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    // demo / demo123 - local development only.
    private static final Map<String, String> DEMO_USERS = Map.of(
            "demo", ENCODER.encode("demo123")
    );

    private final JwtService jwtService;
    private final long expirationSeconds;

    public AuthController(JwtService jwtService,
                           @Value("${app.jwt.expiration-seconds}") long expirationSeconds) {
        this.jwtService = jwtService;
        this.expirationSeconds = expirationSeconds;
    }

    @PostMapping("/auth/token")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        String encoded = DEMO_USERS.get(request.username());
        if (encoded == null || !ENCODER.matches(request.password(), encoded)) {
            throw new BadCredentialsException("Invalid username or password");
        }
        String token = jwtService.generateToken(request.username());
        return ResponseEntity.ok(TokenResponse.bearer(token, expirationSeconds));
    }
}
