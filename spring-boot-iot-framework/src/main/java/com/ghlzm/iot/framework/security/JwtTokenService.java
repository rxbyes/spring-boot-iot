package com.ghlzm.iot.framework.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ghlzm.iot.framework.config.IotProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 签发与解析服务。
 */
@Component
public class JwtTokenService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String DEFAULT_SECRET = "spring-boot-iot-default-jwt-secret";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final int tokenExpireSeconds;
    private final String tokenHeader;
    private final String tokenPrefix;

    public JwtTokenService(IotProperties iotProperties) {
        IotProperties.Security security = iotProperties.getSecurity();
        String secret = StringUtils.hasText(security.getJwtSecret()) ? security.getJwtSecret() : DEFAULT_SECRET;
        this.algorithm = Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));
        this.verifier = JWT.require(algorithm).build();
        this.tokenExpireSeconds = security.getTokenExpireSeconds() == null ? 7200 : security.getTokenExpireSeconds();
        this.tokenHeader = StringUtils.hasText(security.getTokenHeader()) ? security.getTokenHeader() : "Authorization";
        this.tokenPrefix = StringUtils.hasText(security.getTokenPrefix()) ? security.getTokenPrefix() : "Bearer";
    }

    /**
     * 签发访问令牌，当前包含 subject(username) 与 userId 声明。
     */
    public String createToken(Long userId, String username) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(tokenExpireSeconds)))
                .withSubject(username)
                .withClaim(CLAIM_USER_ID, userId)
                .sign(algorithm);
    }

    /**
     * 解析并校验令牌，失败时返回 empty。
     */
    public Optional<JwtUserPrincipal> parseToken(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            Long userId = decodedJWT.getClaim(CLAIM_USER_ID).asLong();
            String username = decodedJWT.getSubject();
            if (userId == null || !StringUtils.hasText(username)) {
                return Optional.empty();
            }
            return Optional.of(new JwtUserPrincipal(userId, username));
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }

    public int getTokenExpireSeconds() {
        return tokenExpireSeconds;
    }

    public String getTokenHeader() {
        return tokenHeader;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public String getAuthorizationHeaderValue(String token) {
        return tokenPrefix + " " + token;
    }
}
