/**
 * Auth controller - proxies authentication endpoints to the Spring Boot
 * backend. The Spring backend is the single source of truth for user
 * records, OTP generation/persistence and JWT issuance; this gateway only
 * relays requests, sends the OTP email and normalizes errors.
 */
const { proxyRequest } = require("../services/proxy.service");
const { sendOtpEmail } = require("../services/email.service");
const { parseError } = require("../utils/apiUtils");

exports.register = async (req, res) => {
  try {
    const result = await proxyRequest({ method: "POST", path: "/auth/register", body: req.body });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

exports.login = async (req, res) => {
  try {
    const result = await proxyRequest({ method: "POST", path: "/auth/login", body: req.body });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

exports.logout = async (req, res) => {
  try {
    const result = await proxyRequest({ method: "POST", path: "/auth/logout", token: req.token });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

/**
 * Send OTP. Spring generates and persists the code and returns it; this
 * gateway emails the exact same code to the user. In development (no SMTP
 * configured) the code is echoed back as `devOtp` so flows can be tested.
 */
exports.sendOtp = async (req, res) => {
  const { email, purpose } = req.body;
  if (!email || !purpose) {
    return res.status(400).json({ success: false, message: "Email and purpose are required" });
  }
  try {
    const result = await proxyRequest({ method: "POST", path: "/auth/send-otp", body: { email, purpose } });
    const otpCode = result.data && result.data.otpCode;
    if (!otpCode) {
      throw new Error("Spring did not return an OTP code");
    }
    const emailResult = await sendOtpEmail(email, otpCode, purpose);
    return res.json({
      success: true,
      message: emailResult.success ? "OTP sent to your email" : "OTP generated (email disabled in dev)",
      data: { devOtp: emailResult.success ? undefined : otpCode },
    });
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

/**
 * Verify OTP - delegates validation entirely to Spring.
 */
exports.verifyOtp = async (req, res) => {
  const { email, otpCode, purpose } = req.body;
  if (!email || !otpCode || !purpose) {
    return res.status(400).json({ success: false, message: "Email, OTP and purpose are required" });
  }
  try {
    const result = await proxyRequest({ method: "POST", path: "/auth/verify-otp", body: req.body });
    return res.json({ success: true, message: "OTP verified successfully", data: result.data });
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

exports.resetPassword = async (req, res) => {
  try {
    const result = await proxyRequest({ method: "POST", path: "/auth/reset-password", body: req.body });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

/**
 * Current authenticated user profile.
 */
exports.me = async (req, res) => {
  try {
    const result = await proxyRequest({ method: "GET", path: "/auth/me", token: req.token });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

/**
 * Change password for the authenticated user.
 */
exports.changePassword = async (req, res) => {
  try {
    const result = await proxyRequest({ method: "PUT", path: "/auth/change-password", token: req.token, body: req.body });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({ success: false, message: parsed.message });
  }
};

module.exports = exports;
