const router = require("express").Router();
const videoController = require("../controllers/video.controller");
const { authenticate } = require("../middleware/auth.middleware");

router.use(authenticate);

router.get("/search", videoController.search);

module.exports = router;
