# Authentication Module - API Testing Examples

All examples target the Node.js gateway: `http://localhost:3000/api`.

For Windows PowerShell, prefix each `curl` with `curl.exe`. The examples below use
bash/curl syntax and are fully runnable from Git Bash, WSL or Linux/macOS.

---

## 1. Registration

```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Ravi Kumar",
    "email": "ravi.kumar@example.com",
    "phone": "9876543210",
    "password": "Ravi@1234",
    "department": "Computer Science",
        "cgpa": 7.8
  }'
```

**Expected response (201 Created):**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": 5,
    "email": "ravi.kumar@example.com",
    "fullName": "Ravi Kumar",
    "role": "STUDENT",
    "studentId": 5,
    "verified": false,
    "message": "Registration successful"
  }
}
```

### Validation error example (weak password)

```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ravi Kumar","email":"ravi@example.com","phone":"9876543210","password":"123","department":"CS"}'
```

```json
{
  "timestamp": "2026-08-05T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "password": "Password must contain uppercase, lowercase, digit and special character"
  }
}
```

### Duplicate email (409 Conflict)

```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ravi Kumar","email":"student1@studentai.com","phone":"9876543210","password":"Ravi@1234","department":"CS"}'
```

---

## 2. Email OTP (registration verification)

### Send OTP

```bash
curl -X POST http://localhost:3000/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"ravi.kumar@example.com","purpose":"REGISTRATION"}'
```

When SMTP is not configured the gateway returns the code as `devOtp`:

```json
{
  "success": true,
  "message": "OTP generated (email disabled in dev)",
  "data": { "devOtp": "482913" }
}
```

### Verify OTP

```bash
curl -X POST http://localhost:3000/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"ravi.kumar@example.com","otpCode":"482913","purpose":"REGISTRATION"}'
```

### Wrong OTP (400)

```bash
curl -X POST http://localhost:3000/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"ravi.kumar@example.com","otpCode":"000000","purpose":"REGISTRATION"}'
```

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Invalid OTP code" }
```

---

## 3. Login (JWT + BCrypt verification)

```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student1@studentai.com","password":"Student@123","rememberMe":false}'
```

Save the token for subsequent calls:

```bash
export TOKEN="<paste token from response>"
```

### Wrong password (400)

```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student1@studentai.com","password":"WrongPass@1"}'
```

---

## 4. Current user (`/me`) - demonstrates JWT auth + role

```bash
curl -X GET http://localhost:3000/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "userId": 2,
    "email": "student1@studentai.com",
    "fullName": "Aarav Sharma",
    "role": "STUDENT",
    "phone": "9123456780",
    "verified": true,
    "active": true,
    "studentId": 1,
    "studentCode": "STU2024001",
    "department": "Computer Science",
        "cgpa": 8.4,
    "profileImage": null
  }
}
```

### No / bad token (401)

```bash
curl -X GET http://localhost:3000/api/auth/me
```

```json
{ "success": false, "message": "Authentication required" }
```

---

## 5. Forgot password -> OTP -> reset password

### Step A - request reset OTP

```bash
curl -X POST http://localhost:3000/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"student1@studentai.com"}'
```

### Step B - reset password with OTP

```bash
curl -X POST http://localhost:3000/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student1@studentai.com",
    "otpCode": "<otp-from-step-A>",
    "newPassword": "NewPass@456"
  }'
```

Note: OTPs are single-use; a new `/forgot-password` call is required after a failed
reset. Reset the password back with the same flow using `Student@123`.

---

## 6. Change password (authenticated)

```bash
curl -X PUT http://localhost:3000/api/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"oldPassword":"NewPass@456","newPassword":"Student@123"}'
```

### Wrong current password (400)

```bash
curl -X PUT http://localhost:3000/api/auth/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"oldPassword":"Wrong","newPassword":"Student@123"}'
```

---

## 7. Logout (authenticated)

```bash
curl -X POST http://localhost:3000/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## 8. Role-based access control

### Admin can access `/api/admin/**` (Spring enforces ROLE_ADMIN)

```bash
export ADMIN_TOKEN=$(curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@studentai.com","password":"Admin@123"}' | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

curl -X GET http://localhost:3000/api/proxy/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Student is rejected on `/admin/**` (403 Forbidden)

```bash
curl -X GET http://localhost:3000/api/proxy/admin/users \
  -H "Authorization: Bearer $TOKEN"
```

```json
{ "status": 403, "error": "Forbidden", "message": "You do not have permission to perform this action" }
```

---

## Full end-to-end test script (bash)

```bash
#!/usr/bin/env bash
set -e
BASE=http://localhost:3000/api
EMAIL="demo.test.$(date +%s)@example.com"

echo "== Register =="
curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d "{\"fullName\":\"Demo Tester\",\"email\":\"$EMAIL\",\"phone\":\"9876500000\",\"password\":\"Demo@1234\",\"department\":\"Computer Science\"}"

echo; echo "== Send OTP =="
OTP=$(curl -s -X POST $BASE/auth/send-otp -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"purpose\":\"REGISTRATION\"}" | python -c "import sys,json;print(json.load(sys.stdin)['data']['devOtp'])")
echo "devOtp=$OTP"

echo; echo "== Verify OTP =="
curl -s -X POST $BASE/auth/verify-otp -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"otpCode\":\"$OTP\",\"purpose\":\"REGISTRATION\"}"

echo; echo "== Login =="
TOKEN=$(curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Demo@1234\"}" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

echo; echo "== Me =="
curl -s $BASE/auth/me -H "Authorization: Bearer $TOKEN"

echo; echo "== Change password =="
curl -s -X PUT $BASE/auth/change-password -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"oldPassword":"Demo@1234","newPassword":"Demo@5678"}'

echo; echo "== Logout =="
curl -s -X POST $BASE/auth/logout -H "Authorization: Bearer $TOKEN"
echo; echo "Done."
```
