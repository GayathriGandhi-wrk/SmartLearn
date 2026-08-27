/**
 * JWT utilities: sign, verify and attach user to request.
 * The signing key is the base64-decoded value that Spring Boot uses
 * (Keys.hmacShaKeyFor(Base64.decode(secret))), so tokens issued by
 * Spring can be verified here and vice-versa.
 */
const jwt = require("jsonwebtoken");
const config = require("../config");

function signingKey() {
  const s = config.jwtSecret || "change-me-secret";
  try {
    return Buffer.from(s, "base64");
  } catch {
    return Buffer.from(s, "utf8");
  }
}

exports.signToken = (payload, expiresIn) =>
  jwt.sign(payload, signingKey(), { expiresIn: expiresIn || config.jwtExpiresIn });

exports.verifyToken = (token) => jwt.verify(token, signingKey());

/**
 * Authentication middleware - validates the Authorization Bearer token
 * and attaches the decoded user to req.user.
 */
exports.authenticate = (req, res, next) => {
  const header = req.headers.authorization || "";
  if (!header.startsWith("Bearer ")) {
    return res.status(401).json({ success: false, message: "Authentication required" });
  }
  const token = header.substring(7);
  try {
    const decoded = exports.verifyToken(token);
    req.user = decoded;
    req.token = token;
    next();
  } catch (err) {
    return res.status(401).json({ success: false, message: "Invalid or expired token" });
  }
};

/**
 * Role-based authorization middleware.
 */
exports.authorize = (...roles) => (req, res, next) => {
  if (!req.user) {
    return res.status(401).json({ success: false, message: "Authentication required" });
  }
  if (!roles.includes(req.user.role)) {
    return res.status(403).json({ success: false, message: "Forbidden: insufficient role" });
  }
  next();
};
