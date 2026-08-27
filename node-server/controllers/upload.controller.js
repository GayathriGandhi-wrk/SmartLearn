/**
 * Upload controller - receives files on the gateway and forwards
 * the stored file path to Spring Boot.
 */
const { proxyRequest } = require("../services/proxy.service");
const logger = require("../utils/logger");

exports.uploadProfilePhoto = async (req, res) => {
  if (!req.file) {
    return res.status(400).json({ success: false, message: "No file uploaded" });
  }
  const relativePath = req.file.path.split("uploads").pop().replace(/\\/g, "/");
  try {
    const result = await proxyRequest({
      method: "PUT",
      path: "/students/me/photo-url",
      token: req.user ? req.user.token : undefined,
      body: { profilePhoto: `uploads${relativePath}` },
    });
    return res.json({ success: true, message: "Photo uploaded", data: { path: `uploads${relativePath}`, result: result.data } });
  } catch (err) {
    logger.error("Photo upload forward failed: " + err.message);
    // Even if Spring is unreachable, the file is saved locally.
    return res.json({
      success: true,
      message: "Photo uploaded (waiting for backend sync)",
      data: { path: `uploads${relativePath}` },
    });
  }
};

exports.uploadDocument = async (req, res) => {
  if (!req.file) {
    return res.status(400).json({ success: false, message: "No file uploaded" });
  }
  const relativePath = req.file.path.split("uploads").pop().replace(/\\/g, "/");
  return res.json({ success: true, message: "Document uploaded", data: { path: `uploads${relativePath}` } });
};
