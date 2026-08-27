/**
 * Multer configuration for file uploads (profile photos, documents).
 */
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const { v4: uuidv4 } = require("uuid");
const config = require("../config");

const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
const ALLOWED_DOC_TYPES = ["application/pdf", "text/plain", "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function fileFilter(folder) {
  return (req, file, cb) => {
    const allowed = folder === "profiles" ? ALLOWED_IMAGE_TYPES : ALLOWED_DOC_TYPES;
    if (!allowed.includes(file.mimetype)) {
      return cb(new Error(folder === "profiles"
        ? "Only image files (JPEG, PNG, GIF, WEBP) are allowed"
        : "File type not allowed"));
    }
    cb(null, true);
  };
}

function storage(folder) {
  return multer.diskStorage({
    destination: (req, file, cb) => {
      const dir = path.join(config.upload.dir, folder);
      ensureDir(dir);
      cb(null, dir);
    },
    filename: (req, file, cb) => {
      const ext = path.extname(file.originalname || "").toLowerCase();
      cb(null, `${folder}_${uuidv4().replace(/-/g, "")}${ext}`);
    },
  });
}

const limits = { fileSize: config.upload.maxMb * 1024 * 1024 };

exports.uploadProfilePhoto = multer({ storage: storage("profiles"), fileFilter: fileFilter("profiles"), limits });
exports.uploadDocument = multer({ storage: storage("documents"), fileFilter: fileFilter("documents"), limits });
