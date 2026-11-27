package com.lighttoolbox.security;

import com.lighttoolbox.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析。
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    public String createToken(Long userId, String openId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + appProperties.getJwt().getExpireSeconds() * 1000);
        return Jwts.builder()
                .subject(openId)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Object uid = parse(token).get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(uid));
    }

    public String getOpenId(String token) {
        return parse(token).getSubject();
    }

    private SecretKey secretKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
