package com.example.civic_issue.security;

import com.example.civic_issue.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expiration = 1000 * 60 * 60 * 24; // 24 hours

    public String generateTokenWithRole(String phoneNumber, Role role) {
        return Jwts.builder()
                .setSubject(phoneNumber)
                .claim("role", role.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Role extractRole(String token) {
        String roleStr = parseClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public String extractPhoneNumber(String token) {
        return parseClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    // ✅ New helper for your JwtFilter
    public boolean validateToken(String token, String phoneNumber) {
        final String extractedPhone = extractPhoneNumber(token);
        return (extractedPhone.equals(phoneNumber) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
