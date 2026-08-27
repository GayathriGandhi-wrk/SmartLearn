const router = require("express").Router();
const proxyController = require("../controllers/proxy.controller");
const { authenticate } = require("../middleware/auth.middleware");
const rateLimit = require("express-rate-limit");

// Stricter limit for mutation endpoints
const writeLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 60,
  message: { success: false, message: "Too many requests" },
});

// All routes under /api/proxy/* require authentication
router.use(authenticate);

router.all("/*", writeLimiter, proxyController.forward);

module.exports = router;
