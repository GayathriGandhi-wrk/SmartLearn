/**
 * Email service using Nodemailer.
 * Sends OTP codes and welcome emails.
 */
const nodemailer = require("nodemailer");
const config = require("../config");
const logger = require("../utils/logger");

const transporter = nodemailer.createTransport({
  host: config.email.host,
  port: config.email.port,
  secure: config.email.secure,
  auth: {
    user: config.email.user,
    pass: config.email.pass,
  },
});

async function sendMail(to, subject, html) {
  try {
    await transporter.sendMail({
      from: `"Student Performance System" <${config.email.user}>`,
      to,
      subject,
      html,
    });
    logger.info(`Email sent to ${to}: ${subject}`);
    return { success: true };
  } catch (err) {
    logger.error(`Email failed to ${to}: ${err.message}`);
    // Do not fail the request in development when SMTP is not configured
    return { success: false, error: err.message };
  }
}

async function sendOtpEmail(to, otp, purpose) {
  const html = `
    <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;border:1px solid #e0e0e0;border-radius:8px;">
      <h2 style="color:#4f46e5;margin-top:0;">Student Performance System</h2>
      <p>Hello,</p>
      <p>Your One-Time Password (OTP) for <strong>${purpose}</strong> is:</p>
      <div style="text-align:center;padding:16px;background:#f3f4f6;border-radius:8px;font-size:28px;letter-spacing:8px;font-weight:bold;color:#111827;">${otp}</div>
      <p style="color:#6b7280;font-size:13px;">This OTP is valid for 15 minutes. If you did not request this, ignore this email.</p>
    </div>`;
  return sendMail(to, `Your OTP for ${purpose} - Student Performance System`, html);
}

async function sendWelcomeEmail(to, fullName) {
  const html = `
    <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;border:1px solid #e0e0e0;border-radius:8px;">
      <h2 style="color:#4f46e5;margin-top:0;">Welcome to Student Performance System!</h2>
      <p>Dear <strong>${fullName}</strong>,</p>
      <p>Your account has been created successfully. Start your personalized learning journey now — attempt adaptive tests, practice questions and track your predicted performance.</p>
      <p style="color:#6b7280;font-size:13px;">Best regards,<br/>Student Performance Team</p>
    </div>`;
  return sendMail(to, "Welcome to Student Performance System!", html);
}

module.exports = { sendOtpEmail, sendWelcomeEmail, sendMail };
