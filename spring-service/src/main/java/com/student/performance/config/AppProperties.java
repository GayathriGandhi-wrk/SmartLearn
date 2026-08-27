package com.student.performance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Otp otp = new Otp();
    private final AiService aiService = new AiService();
    private final Upload upload = new Upload();
    private final Cors cors = new Cors();

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
        private long rememberMeExpirationMs = 604800000;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
        public long getRememberMeExpirationMs() { return rememberMeExpirationMs; }
        public void setRememberMeExpirationMs(long rememberMeExpirationMs) { this.rememberMeExpirationMs = rememberMeExpirationMs; }
    }

    public static class Otp {
        private int expiryMinutes = 15;
        private int length = 6;

        public int getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
        public int getLength() { return length; }
        public void setLength(int length) { this.length = length; }
    }

    public static class AiService {
        private String baseUrl = "http://localhost:5000";
        private int timeoutSeconds = 30;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class Upload {
        private String dir = "./uploads";
        private long maxSizeMb = 5;

        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public long getMaxSizeMb() { return maxSizeMb; }
        public void setMaxSizeMb(long maxSizeMb) { this.maxSizeMb = maxSizeMb; }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(String allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public Jwt getJwt() { return jwt; }
    public Otp getOtp() { return otp; }
    public AiService getAiService() { return aiService; }
    public Upload getUpload() { return upload; }
    public Cors getCors() { return cors; }
}
