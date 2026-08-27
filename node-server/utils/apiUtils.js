/**
 * Utility helpers for API responses and common operations.
 */

exports.success = (res, data = null, message = "Success", status = 200) =>
  res.status(status).json({ success: true, message, data });

exports.failure = (res, message = "Error", status = 400, data = null) =>
  res.status(status).json({ success: false, message, data });

exports.asyncHandler = (fn) => (req, res, next) =>
  Promise.resolve(fn(req, res, next)).catch(next);

exports.parseError = (err) => {
  if (err.response && err.response.data) {
    return {
      status: err.response.status || 500,
      message: (err.response.data.message) || "Upstream service error",
      data: err.response.data,
    };
  }
  // Proxy service throws Error with .status and .data attached
  if (err.status) {
    return { status: err.status, message: err.message || "Upstream service error", data: err.data || null };
  }
  return { status: 500, message: err.message || "Upstream service error", data: null };
};

exports.generateOtp = (length = 6) => {
  let otp = "";
  for (let i = 0; i < length; i++) {
    otp += Math.floor(Math.random() * 10);
  }
  return otp;
};
