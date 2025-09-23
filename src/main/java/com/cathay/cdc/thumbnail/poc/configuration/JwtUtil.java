package com.cathay.cdc.thumbnail.poc.configuration;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {
    private final Key signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        // secret must be sufficiently long for HS256 (use at least 32 bytes)
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
        log.info("✅ JwtUtil initialized with expiration={} ms", expirationMs);
    }

    public String generateToken(String username, Collection<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
        log.info("🔑 Generated JWT for user={} with roles={} exp={}", username, roles, exp);
        return token;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey) // must match token generation secret
                    .build()
                    .parseClaimsJws(token);
            log.debug("✅ JWT is valid");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ JWT expired at {}", e.getClaims().getExpiration());
        } catch (UnsupportedJwtException e) {
            log.error("❌ Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("❌ Malformed JWT: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("❌ Invalid JWT signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("❌ Empty or null JWT: {}", e.getMessage());
        }
        return false;
    }

    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                    .parseClaimsJws(token).getBody();
            String username = claims.getSubject();
            log.info("👤 Extracted username from JWT: {}", username);
            return username;
        } catch (Exception e) {
            log.error("❌ Failed to extract username from token: {}", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                    .parseClaimsJws(token).getBody();
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List) {
                List<String> roles = ((List<?>) rolesObj).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                log.info("📌 Extracted roles from JWT: {}", roles);
                return roles;
            }
            log.warn("⚠️ No roles claim found in JWT");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Failed to extract roles from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
