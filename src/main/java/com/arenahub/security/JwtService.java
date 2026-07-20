package com.arenahub.security;

import com.arenahub.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final String secret;
    private final long expireSeconds;

    public JwtService(
            @Value("${arenahub.jwt.secret}") String secret,
            @Value("${arenahub.jwt.expire-seconds}") long expireSeconds) {
        this.secret = secret;
        this.expireSeconds = expireSeconds;
    }

    public String createToken(Long userId, String username, Long playerId) {
        try {
            String headerPart = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
            String payload = "{\"uid\":" + userId
                    + ",\"username\":\"" + escape(username) + "\""
                    + ",\"pid\":" + playerId
                    + ",\"exp\":" + Instant.now().plusSeconds(expireSeconds).getEpochSecond()
                    + "}";
            String payloadPart = encode(payload);
            String signaturePart = sign(headerPart + "." + payloadPart);
            return headerPart + "." + payloadPart + "." + signaturePart;
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 生成失败", exception);
        }
    }

    public CurrentUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) {
                throw unauthorized();
            }
            String payload = new String(URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            long exp = longField(payload, "exp");
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录已过期");
            }
            return new CurrentUser(
                    longField(payload, "uid"),
                    stringField(payload, "username"),
                    longField(payload, "pid"));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private String encode(String json) {
        return URL_ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private BusinessException unauthorized() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "无效的登录凭证");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private long longField(String json, String field) {
        String marker = "\"" + field + "\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw unauthorized();
        }
        start += marker.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }

    private String stringField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw unauthorized();
        }
        start += marker.length();
        int end = start;
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        while (end < json.length()) {
            char current = json.charAt(end++);
            if (escaped) {
                builder.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return builder.toString();
            } else {
                builder.append(current);
            }
        }
        throw unauthorized();

    }
}
