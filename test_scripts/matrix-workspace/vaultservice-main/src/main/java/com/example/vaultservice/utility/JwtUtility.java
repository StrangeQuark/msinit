// Integration file: Auth

package com.example.vaultservice.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

@Service
public class JwtUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtility.class);

    @Value("${JWT_PUBLIC_KEY}")
    private String JWT_PUBLIC_KEY;

    @Value("${JWT_ISSUER}")
    private String JWT_ISSUER;

    public String extractId() {
        LOGGER.debug("Attempting to extract ID from JWT");

        String token = getTokenFromHeader();
        Claims claims = getClaims(token);

        LOGGER.debug("ID successfully extracted from JWT");
        return claims.get("principalId", String.class);
    }

    public boolean isAuthService() {
        Claims claims = getClaims(getTokenFromHeader());

        return claims.get("principalType", String.class).equals("SERVICE_ACCOUNT") &&
                claims.get("clientId", String.class).equals("auth");
    }

    private String getTokenFromHeader() {
        LOGGER.debug("Attempting to get token from header");
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("No request context available");
        }

        HttpServletRequest request = attrs.getRequest();
        String authHeader = request.getHeader("Authorization");

        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            LOGGER.debug("Token successfully retrieved from header");
            return authHeader.substring(7);
        }

        if(request.getCookies() != null) {
            for(Cookie cookie : request.getCookies()) {
                if(cookie.getName().equals("access_token") && !cookie.getValue().isBlank()) {
                    LOGGER.debug("Token successfully retrieved from cookie");
                    return cookie.getValue();
                }
            }
        }

        throw new RuntimeException("Missing or invalid Authorization header and access_token cookie");
    }

    // Integration function start: Telemetry
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
    // Integration function end: Telemetry

    private Claims getClaims(String token) {
        try {
            Key key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Decoders.BASE64.decode(JWT_PUBLIC_KEY)));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(JWT_ISSUER)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if(!claims.get("tokenType", String.class).equals("ACCESS"))
                throw new RuntimeException("JWT token type is invalid");

            if(claims.getId() == null || claims.get("principalId", String.class) == null)
                throw new RuntimeException("JWT is missing required claims");

            return claims;
        } catch(Exception ex) {
            throw new RuntimeException("Failed to validate JWT", ex);
        }
    }
}
