/**
 * AI-Powered Student Performance Prediction & Personalized Learning System
 * Node.js + Express Gateway Server
 *
 * Responsibilities:
 *  - Serve the static frontend
 *  - JWT + authentication middleware
 *  - Email OTP delivery
 *  - File upload handling
 *  - REST API proxy to the Spring Boot backend
 *  - Session handling, API validation, error handling, logging
 */

require("dotenv").config();
const express = require("express");
const cors = require("cors");
const helmet = require("helmet");
const rateLimit = require("express-rate-limit");
const path = require("path");

const config = require("./config");
const logger = require("./utils/logger");

const app = express();
const PORT = config.port;

// Security headers.
// The frontend uses inline <script> blocks and loads Chart.js/Bootstrap from CDN,
// so the default Helmet CSP (script-src 'self') would block them and break every page.
// Allow inline scripts, https CDNs, and cross-origin fetch to the AI service.
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      ...helmet.contentSecurityPolicy.getDefaultDirectives(),
      "script-src": ["'self'", "'unsafe-inline'", "https:"],
      "style-src": ["'self'", "'unsafe-inline'", "https:"],
      "img-src": ["'self'", "data:", "blob:", "https:"],
      "font-src": ["'self'", "data:", "https:"],
      "connect-src": ["'self'", "http://localhost:5000", "https:"],
    },
  },
}));

// CORS
app.use(cors({
  origin: config.corsOrigins,
  credentials: true,
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
  message: { success: false, message: "Too many requests, please try again later." },
});
app.use("/api", limiter);

// Parsers
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true }));

// Static files
app.use(express.static(path.join(__dirname, "..", "frontend")));
app.use("/uploads", express.static(path.join(__dirname, "uploads")));

// Request logging
app.use((req, res, next) => {
  logger.info(`${req.method} ${req.originalUrl}`);
  next();
});

// Routes
app.use("/api/auth", require("./routes/auth.routes"));
app.use("/api/proxy", require("./routes/proxy.routes"));
app.use("/api/upload", require("./routes/upload.routes"));
// Learn module: real YouTube video IDs, because YouTube no longer supports
// search embeds. Mounted before the catch-all proxy so it is never forwarded.
app.use("/api/videos", require("./routes/video.routes"));

// Health check
app.get("/api/health", (req, res) => {
  res.json({ success: true, message: "Node gateway is running", time: new Date().toISOString() });
});

// Generic API proxy: forwards every other /api/:module/* request to Spring Boot
// (e.g. /api/analytics/overview -> http://localhost:8080/api/analytics/overview).
// Must be mounted AFTER /api/auth, /api/upload and /api/health, and BEFORE the
// SPA fallback so API calls never receive index.html.
app.use("/api", require("./routes/proxy.routes"));

// SPA fallback (only reached for non-API routes)
app.get("*", (req, res) => {
  res.sendFile(path.join(__dirname, "..", "frontend", "index.html"));
});

// Error handler
app.use((err, req, res, next) => {
  logger.error("Unhandled error: " + err.message);
  res.status(err.status || 500).json({
    success: false,
    message: err.message || "Internal server error",
    stack: config.env === "development" ? err.stack : undefined,
  });
});

app.listen(PORT, () => {
  logger.info(`Node gateway running on http://localhost:${PORT}`);
});

module.exports = app;
