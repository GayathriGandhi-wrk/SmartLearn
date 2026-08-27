const { test } = require("node:test");
const assert = require("node:assert");

const apiUtils = require("../utils/apiUtils");
const { signToken, verifyToken } = require("../middleware/auth.middleware");

test("success helper builds a JSON response", () => {
  const res = {
    status(code) { this.code = code; return this; },
    json(obj) { this.body = obj; return this; },
  };
  apiUtils.success(res, { id: 1 }, "OK");
  assert.strictEqual(res.code, 200);
  assert.strictEqual(res.body.success, true);
  assert.strictEqual(res.body.data.id, 1);
});

test("failure helper builds an error response", () => {
  const res = {
    status(code) { this.code = code; return this; },
    json(obj) { this.body = obj; return this; },
  };
  apiUtils.failure(res, "bad request", 400);
  assert.strictEqual(res.code, 400);
  assert.strictEqual(res.body.success, false);
});

test("generateOtp returns numeric string of given length", () => {
  const otp = apiUtils.generateOtp(6);
  assert.strictEqual(otp.length, 6);
  assert.ok(/^\d+$/.test(otp));
});

test("JWT sign and verify round trip", () => {
  const token = signToken({ uid: 1, role: "STUDENT" }, "1h");
  assert.ok(token);
  const decoded = verifyToken(token);
  assert.strictEqual(decoded.uid, 1);
  assert.strictEqual(decoded.role, "STUDENT");
});

test("verifyToken rejects invalid tokens", () => {
  assert.throws(() => verifyToken("not-a-jwt"), /jwt|token/i);
});
