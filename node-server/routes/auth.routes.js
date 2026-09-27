const router = require("express").Router();
const { body } = require("express-validator");
const authController = require("../controllers/auth.controller");
const { validate } = require("../middleware/validation.middleware");
const { authenticate } = require("../middleware/auth.middleware");

// Registration with strong validation
router.post(
  "/register",
  validate([
    body("fullName").trim().isLength({ min: 3, max: 120 }).withMessage("Full name must be 3-120 characters"),
    body("email").isEmail().withMessage("Invalid email format"),
    body("phone").matches(/^[+]?[0-9]{10,13}$/).withMessage("Invalid phone number"),
    body("password")
      .isLength({ min: 8, max: 60 }).withMessage("Password must be 8-60 characters")
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/)
      .withMessage("Password must contain uppercase, lowercase, digit and special character"),
    body("department").notEmpty().withMessage("Department is required"),
    body("semester").optional().isInt({ min: 1, max: 8 }).withMessage("Semester must be 1-8"),
    body("cgpa").optional().isFloat({ min: 0, max: 10 }).withMessage("CGPA must be 0-10"),
  ]),
  authController.register
);

// Login with validation
router.post(
  "/login",
  validate([
    body("email").isEmail().withMessage("Invalid email format"),
    body("password").notEmpty().withMessage("Password is required"),
  ]),
  authController.login
);

// Logout (requires valid token)
router.post("/logout", authenticate, authController.logout);

// Forgot password -> sends OTP
router.post(
  "/forgot-password",
  validate([body("email").isEmail().withMessage("Invalid email format")]),
  (req, res, next) => {
    req.body.purpose = req.body.purpose || "PASSWORD_RESET";
    next();
  },
  authController.sendOtp
);

// Send OTP (registration / email verification)
router.post(
  "/send-otp",
  validate([body("email").isEmail().withMessage("Invalid email format")]),
  authController.sendOtp
);

// Verify OTP
router.post(
  "/verify-otp",
  validate([
    body("email").isEmail().withMessage("Invalid email format"),
    body("otpCode").notEmpty().withMessage("OTP is required"),
  ]),
  authController.verifyOtp
);

// Reset password
router.post(
  "/reset-password",
  validate([
    body("email").isEmail().withMessage("Invalid email format"),
    body("otpCode").notEmpty().withMessage("OTP is required"),
    body("newPassword")
      .isLength({ min: 8 }).withMessage("Password must be 8-60 characters")
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/)
      .withMessage("Password must contain uppercase, lowercase, digit and special character"),
  ]),
  authController.resetPassword
);

// Current user profile (requires valid token)
router.get("/me", authenticate, authController.me);

// Change password (requires valid token)
router.put(
  "/change-password",
  authenticate,
  validate([
    body("oldPassword").notEmpty().withMessage("Current password is required"),
    body("newPassword")
      .isLength({ min: 8 }).withMessage("Password must be 8-60 characters")
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/)
      .withMessage("Password must contain uppercase, lowercase, digit and special character"),
  ]),
  authController.changePassword
);

module.exports = router;
