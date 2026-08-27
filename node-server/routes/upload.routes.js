const router = require("express").Router();
const uploadController = require("../controllers/upload.controller");
const { uploadProfilePhoto, uploadDocument } = require("../middleware/upload.middleware");
const { authenticate } = require("../middleware/auth.middleware");

router.use(authenticate);

router.post("/profile-photo", uploadProfilePhoto.single("file"), uploadController.uploadProfilePhoto);
router.post("/document", uploadDocument.single("file"), uploadController.uploadDocument);

module.exports = router;
