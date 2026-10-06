/**
 * Video picker - shared by the My Subjects topic panel and the Learn page.
 *
 * YouTube retired search embeds (listType=search) in Nov 2020 and answers
 * 404/410 for them, so a topic is never framed directly. Instead we ask the
 * gateway for real, embeddable video IDs and play the one the student picks.
 * The gateway applies videoEmbeddable=true, so every result is safe to frame.
 */
(function () {
  const safe = (v) => window.UI.safeHtml(v == null ? "" : String(v));
  const attr = (v) => safe(v).replace(/"/g, "&quot;").replace(/'/g, "&#39;");

  // Video IDs are 11 characters from a URL-safe base64 alphabet.
  const VIDEO_ID_RE = /^[A-Za-z0-9_-]{11}$/;

  /** Links arrive from query strings, so only real http(s) URLs are ever used. */
  function httpUrl(url) {
    if (!url) return "";
    let parsed;
    try { parsed = new URL(url); } catch { return ""; }
    if (parsed.protocol !== "http:" && parsed.protocol !== "https:") return "";
    return parsed.href;
  }

  /** Classify a YouTube link: does it already name a specific video? */
  function youtubeInfo(url) {
    const base = httpUrl(url);
    if (!base) return { isYouTube: false, videoId: "", query: "", watchUrl: "" };
    let parsed;
    try { parsed = new URL(base); } catch { return { isYouTube: false, videoId: "", query: "", watchUrl: "" }; }
    const host = parsed.hostname.replace(/^www\./, "").toLowerCase();
    if (host !== "youtube.com" && host !== "youtu.be" && host !== "m.youtube.com") {
      return { isYouTube: false, videoId: "", query: "", watchUrl: base };
    }
    let videoId = parsed.searchParams.get("v") || "";
    if (!videoId && host === "youtu.be") videoId = parsed.pathname.split("/").filter(Boolean)[0] || "";
    if (!videoId) {
      const parts = parsed.pathname.split("/").filter(Boolean);
      if (["shorts", "embed", "live"].includes(parts[0])) videoId = parts[1] || "";
    }
    if (!VIDEO_ID_RE.test(videoId || "")) videoId = "";
    return {
      isYouTube: true,
      videoId,
      query: parsed.searchParams.get("search_query") || "",
      watchUrl: videoId ? `https://www.youtube.com/watch?v=${videoId}` : "",
    };
  }

  // enablejsapi=1 is what lets PlayerProgress read the playback position, which
  // is how "I have learned up to here" knows where the student stopped.
  const embedUrl = (videoId) =>
    `https://www.youtube.com/embed/${encodeURIComponent(videoId)}?rel=0&enablejsapi=1`;

  /** The player markup. Only ever called with a validated 11-char video ID. */
  function playerHtml(videoId, title) {
    if (!VIDEO_ID_RE.test(String(videoId || ""))) return "";
    return `<iframe class="learn-frame" src="${attr(embedUrl(videoId))}"
      title="${attr(title || "Video player")}"
      referrerpolicy="strict-origin-when-cross-origin"
      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
      allowfullscreen></iframe>`;
  }

  const search = (q) => window.API.get(`/videos/search?q=${encodeURIComponent(q)}`);

  /**
   * Render the chooser into `host`.
   * opts: { query, onPick(video), autoSearch, autoPick, openInTab, searchLabel }
   * onPick receives { videoId, title, channelTitle, watchUrl, embedUrl }.
   * autoPick plays the top result straight away, so a student who arrives from
   * a YouTube resource lands on a video instead of a list to click through.
   * openInTab turns the cards into real links to that video on youtube.com,
   * which is what a student wants when they ask to leave the app.
   */
  function mount(host, opts) {
    const o = opts || {};
    const onPick = o.onPick || function () {};
    const startQuery = o.query || "";
    const searchLabel = o.searchLabel || "Search YouTube";

    host.innerHTML = `
      <form class="row g-2 align-items-end" data-vp-form>
        <div class="col-12 col-md">
          <label class="form-label small fw-semibold" for="vp-q">${safe(searchLabel)}</label>
          <input id="vp-q" class="form-control" value="${attr(startQuery)}" maxlength="200"
                 placeholder="Type a topic to find videos" />
        </div>
        <div class="col-12 col-md-auto">
          <button class="btn btn-primary w-100" type="submit" data-vp-search>
            <i class="bi bi-search me-1"></i>Find videos
          </button>
        </div>
      </form>
      <div class="mt-2" data-vp-results></div>`;

    const form = host.querySelector("[data-vp-form]");
    const input = host.querySelector("#vp-q");
    const btn = host.querySelector("[data-vp-search]");
    const results = host.querySelector("[data-vp-results]");

    /** Normalise one search result into the shape onPick receives. */
    const toVideo = (v) => ({
      videoId: v.videoId,
      title: v.title,
      channelTitle: v.channelTitle,
      watchUrl: v.watchUrl,
      embedUrl: embedUrl(v.videoId),
    });

    function renderResults(rawItems) {
      // Defence in depth: the gateway already drops malformed IDs, but never
      // hand a student a link built from one.
      const items = (rawItems || []).filter((v) => v && VIDEO_ID_RE.test(String(v.videoId || "")));
      if (!items.length) {
        results.innerHTML = `<div class="alert alert-warning mb-0 small">
          <i class="bi bi-info-circle me-1"></i>No videos found. Try different words.</div>`;
        return;
      }
      // In openInTab mode the card is a plain link to that exact video, so the
      // browser handles the new tab and the view can still be recorded.
      const tag = o.openInTab ? "a" : "button";

      results.innerHTML = `<div class="row g-2">${items.map((v) => {
        const openAttrs = o.openInTab
          ? `href="${attr(v.watchUrl)}" target="_blank" rel="noopener noreferrer"
             data-quiz-res="VIDEO" data-quiz-title="${attr(v.title)}" data-quiz-url="${attr(v.watchUrl)}"`
          : `type="button" data-vp-video="${attr(v.videoId)}" data-vp-title="${attr(v.title)}"
             data-vp-channel="${attr(v.channelTitle)}" data-vp-url="${attr(v.watchUrl)}"`;
        return `
        <div class="col-12 col-md-6 col-xl-4">
          <${tag} class="video-card w-100 text-start text-decoration-none" ${openAttrs}>
            ${v.thumbnail ? `<img class="video-thumb" src="${attr(v.thumbnail)}" alt="" loading="lazy" referrerpolicy="no-referrer" />` : ""}
            <div class="p-2">
              <div class="fw-semibold small">${safe(v.title)}</div>
              <div class="text-muted" style="font-size:.75rem">${safe(v.channelTitle)}</div>
            </div>
          </${tag}>
        </div>`;
      }).join("")}</div>`;

      if (!o.openInTab) {
        results.querySelectorAll("[data-vp-video]").forEach((card) => {
          card.addEventListener("click", () => onPick({
            videoId: card.dataset.vpVideo,
            title: card.dataset.vpTitle,
            channelTitle: card.dataset.vpChannel,
            watchUrl: card.dataset.vpUrl,
            embedUrl: embedUrl(card.dataset.vpVideo),
          }));
        });
      }
    }
    async function doSearch() {
      const term = input.value.trim();
      if (!term) {
        results.innerHTML = `<div class="alert alert-secondary mb-0 small">
          Type a topic to find related videos.</div>`;
        return;
      }
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Searching...`;
      results.innerHTML = `<div class="spinner"></div>`;
      try {
        const res = await search(term);
        const data = (res && res.data) || {};
        if (data.degraded) {
          results.innerHTML = `<div class="alert alert-warning mb-0 small">
            <i class="bi bi-info-circle me-1"></i>${safe(data.message || "Video search is unavailable right now.")}</div>`;
          return;
        }
        renderResults(data.items || []);
        // Arrive-and-watch: play the best match now, keep "Change video" as the
        // escape hatch for students who want a different channel.
        if (o.autoPick && (data.items || []).length) onPick(toVideo(data.items[0]));
      } catch (err) {
        results.innerHTML = `<div class="alert alert-danger mb-0 small">
          ${safe((err && err.message) || "Search failed.")} Please try again.</div>`;
      } finally {
        btn.disabled = false;
        btn.innerHTML = `<i class="bi bi-search me-1"></i>Find videos`;
      }
    }

    form.addEventListener("submit", (e) => { e.preventDefault(); doSearch(); });

    if (o.autoSearch !== false) doSearch();
  }

  window.VideoPicker = { httpUrl, youtubeInfo, playerHtml, embedUrl, search, mount, VIDEO_ID_RE };
})();
