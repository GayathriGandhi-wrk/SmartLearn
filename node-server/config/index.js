/**
 * Central configuration loaded from environment variables.
 */
const path = require("path");

module.exports = {
  env: process.env.NODE_ENV || "development",
  port: parseInt(process.env.PORT || "3000", 10),
  springApiBase: process.env.SPRING_API_BASE || "http://localhost:8080/api",
  springTimeoutMs: parseInt(process.env.SPRING_TIMEOUT_MS || "30000", 10),

  jwtSecret: process.env.JWT_SECRET || "c2VjcmV0LWtleS1mb3Itc3R1ZGVudC1wZXJmb3JtYW5jZS1zeXN0ZW0tMzItYnl0ZXMtbG9uZw==",
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || "24h",

  email: {
    host: process.env.EMAIL_HOST || "smtp.gmail.com",
    port: parseInt(process.env.EMAIL_PORT || "587", 10),
    user: process.env.EMAIL_USER || "",
    pass: process.env.EMAIL_PASS || "",
    secure: process.env.EMAIL_SECURE === "true",
  },

  upload: {
    dir: process.env.UPLOAD_DIR || path.join(__dirname, "..", "uploads"),
    maxMb: parseInt(process.env.MAX_UPLOAD_MB || "5", 10),
  },

  corsOrigins: (process.env.CORS_ORIGINS || "http://localhost:3000").split(","),
};
