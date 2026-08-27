/**
 * Generic proxy controller - forwards authenticated requests to Spring Boot.
 * Path format: /api/proxy/:module/... maps to /api/:module/...
 */
const { proxyRequest } = require("../services/proxy.service");
const { parseError } = require("../utils/apiUtils");

exports.forward = async (req, res) => {
  const target = req.params[0] ? `/${req.params[0]}` : "";
  const query = req.query;
  const token = req.token || req.headers.authorization?.replace("Bearer ", "");
  try {
    const result = await proxyRequest({
      method: req.method,
      path: target,
      token,
      body: ["GET", "HEAD"].includes(req.method) ? undefined : req.body,
      params: query,
    });
    return res.json(result);
  } catch (err) {
    const parsed = parseError(err);
    return res.status(parsed.status).json({
      success: false,
      message: parsed.message,
      data: parsed.data,
    });
  }
};
