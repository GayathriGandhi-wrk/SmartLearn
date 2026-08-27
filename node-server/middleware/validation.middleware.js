/**
 * API validation middleware using express-validator.
 */
const { validationResult } = require("express-validator");

exports.validate = (validations) => async (req, res, next) => {
  await Promise.all(validations.map((v) => v.run(req)));
  const errors = validationResult(req);
  if (errors.isEmpty()) {
    return next();
  }
  const messages = errors.array().map((e) => e.msg);
  return res.status(400).json({ success: false, message: messages[0], details: errors.array() });
};
