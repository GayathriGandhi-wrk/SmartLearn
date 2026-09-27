/**
 * YouTube Data API service.
 *
 * The Learn module needs a real video ID for every lesson, because YouTube
 * dropped support for search embeds (listType=search) in November 2020 and now
 * answers 404/410 for them. Google's documented replacement is exactly this:
 * call search.list, then embed the video the student picks.
 */
const config = require("../config");
const logger = require("../utils/logger");

const API_BASE = "https://www.googleapis.com/youtube/v3";

/** Video ID characters: 11 chars from the URL-safe base64 alphabet. */
const VIDEO_ID_RE = /^[A-Za-z0-9_-]{11}$/;

/**
 * Search videos for a topic.
 *
 * `search.list` snippets do not carry duration, and a follow-up `videos.list`
 * call would double the quota cost for a cosmetic field, so only snippet data
 * is returned. `videoEmbeddable` filters out videos the owner blocked from
 * third-party playback, which is the whole point of this endpoint.
 *
 * Resolves to { items, degraded, message } - when the API key is missing or the
 * call fails, `degraded` is true so the frontend can say so plainly.
 */
async function searchVideos(query) {
  const q = String(query || "").trim();
  if (!q) {
    const err = new Error("Enter something to search for");
    err.status = 400;
    throw err;
  }

  if (!config.youtube.apiKey) {
    return { items: [], degraded: true, message: "YouTube search is not configured on this server." };
  }

  const url = new URL(`${API_BASE}/search`);
  url.searchParams.set("part", "snippet");
  url.searchParams.set("type", "video");
  url.searchParams.set("videoEmbeddable", "true");
  url.searchParams.set("safeSearch", "strict");
  url.searchParams.set("relevanceLanguage", "en");
  url.searchParams.set("maxResults", String(config.youtube.maxResults));
  url.searchParams.set("q", q);
  url.searchParams.set("key", config.youtube.apiKey);

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.youtube.timeoutMs);
  try {
    const resp = await fetch(url, { signal: controller.signal });
    const body = await resp.json().catch(() => null);

    if (!resp.ok) {
      const reason = (body && body.error && body.error.message) || `YouTube API returned ${resp.status}`;
      logger.warn("YouTube search failed: " + reason);
      // 403/400 here almost always means a bad or restricted API key.
      return { items: [], degraded: true, message: "Video search is unavailable right now." };
    }

    const items = (body.items || [])
      .map((it) => {
        const id = (it.id && it.id.videoId) || "";
        if (!VIDEO_ID_RE.test(id)) return null;
        const sn = it.snippet || {};
        const thumbs = sn.thumbnails || {};
        return {
          videoId: id,
          title: sn.title || "Untitled video",
          channelTitle: sn.channelTitle || "",
          publishedAt: sn.publishedAt || "",
          thumbnail: (thumbs.medium || thumbs.default || {}).url || "",
          watchUrl: `https://www.youtube.com/watch?v=${id}`,
          embedUrl: `https://www.youtube.com/embed/${id}?rel=0`,
        };
      })
      .filter(Boolean);

    return { items, degraded: false, message: "" };
  } catch (err) {
    logger.warn("YouTube search error: " + err.message);
    return { items: [], degraded: true, message: "Video search is unavailable right now." };
  } finally {
    clearTimeout(timer);
  }
}

module.exports = { searchVideos };
