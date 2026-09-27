/**
 * Video controller - powers the Learn module's "choose a video" step.
 *
 * YouTube does not allow search results to be embedded, so the gateway looks up
 * real video IDs here and the frontend embeds only the one the student picks.
 * `videoEmbeddable=true` on the upstream call means every result the student
 * sees is allowed to play in a frame.
 */
const { searchVideos } = require("../services/youtube.service");
const { success, failure, asyncHandler } = require("../utils/apiUtils");
const config = require("../config");

/** Guard against the student submitting an absurdly long query. */
const MAX_Q = 200;

exports.search = asyncHandler(async (req, res) => {
  const q = String(req.query.q || "").trim().slice(0, MAX_Q);
  if (!q) return failure(res, "Enter something to search for", 400);

  const result = await searchVideos(q);
  return success(
    res,
    {
      query: q,
      items: result.items,
      // The frontend shows a plain "unavailable" notice for this.
      degraded: result.degraded,
      message: result.message,
      configured: !!config.youtube.apiKey,
    },
    result.degraded ? "Video search unavailable" : "Videos found"
  );
});
