package com.example.telemetryservice.utility;

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
import java.util.List;

@Service
public class JwtUtility {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtility.class);

    @Value("${JWT_PUBLIC_KEY}")
    private String JWT_PUBLIC_KEY;

    @Value("${JWT_ISSUER}")
    private String JWT_ISSUER;

    public boolean validateToken() {
        LOGGER.debug("Attempting to validate JWT from request");

        return validateToken(getTokenFromHeader());
    }

    public boolean validateToken(String token) {
        LOGGER.debug("Attempting to validate provided JWT");

        try {
            Claims claims = getClaims(token);
            List<String> authorizations = claims.get("authorizations", List.class);

            if(authorizations == null || !authorizations.contains("TELEMETRY_API_ACCESS")) {
                LOGGER.error("JWT does not have TELEMETRY_API_ACCESS");
                return false;
            }

            LOGGER.debug("JWT is valid");
            return true;
        } catch (Exception ex) {
            LOGGER.error("Failed to validate token: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return false;
        }
    }

    public String getServiceName() {
        return getServiceNameFromToken(getTokenFromHeader());
    }

    public String getServiceNameFromToken(String token) {
        try {
            Claims claims = getClaims(token);

            if(!claims.get("principalType", String.class).equals("SERVICE_ACCOUNT"))
                throw new RuntimeException("JWT is not a service account token");

            return claims.getSubject();
        } catch(Exception ex) {
            throw new RuntimeException("Failed to get service name from JWT", ex);
        }
    }

    private String getTokenFromHeader() {
        LOGGER.debug("Attempting to get token from header");
        try {
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
        } catch (Exception ex) {
            LOGGER.error("Failed to get token from header: " + ex.getMessage());
            LOGGER.debug("Stack trace: ", ex);
            return null;
        }
    }

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
