package com.finance.tracker.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * FIX: generateToken() now accepts the role and embeds it as a JWT claim.
 *
 * ROOT CAUSE OF BLANK ADMIN PAGE:
 *   The original generateToken(email) produced a token with NO role claim.
 *   When the frontend decoded the JWT it found no role → treated the user as
 *   anonymous → redirected or rendered a blank page at /admin.
 *
 * HOW IT IS FIXED:
 *   - generateToken(email, role) now stores role inside the "role" claim.
 *   - extractRole(token) lets callers read it without another DB hit.
 *   - AuthService.login() and register() both pass the role to generateToken().
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate a JWT that carries both the subject (email) and the user's role.
     *
     * @param email user's email (JWT subject)
     * @param role  plain role name, e.g. "ADMIN" or "USER"
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /** Extract the role claim from a token (e.g. "ADMIN"). */
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
