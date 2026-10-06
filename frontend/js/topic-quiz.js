/**
 * Turns a topic's learning resources into an adaptive test.
 *
 * A student opens a tutorial video or doc, and this module keeps track of it.
 * When they mark where they stopped learning, the backend generates questions
 * for that topic and packs them into a real test, which the student then takes
 * in the Adaptive Test module. This module deliberately does not render the
 * questions itself: the test module already owns the countdown, grading, XP and
 * the test history, so duplicating any of that here would let the two drift
 * apart. The backend also guarantees the same question is never returned to the
 * same student twice, so there is nothing to de-duplicate on this side.
 *
 * Where the student got to in a video is passed in as a callback rather than
 * tracked here, because the player lives on the host page. The position is sent
 * to the backend before every generation, which is what makes the button honest:
 * the test covers the part that was watched, and the panel says so either way,
 * including when the video turned out to have no readable content.
 *
 * Public API: TopicQuiz.attach(panelElement, topic, subjectName)
 */
(function () {
  "use strict";

  const safe = (v) => (window.UI ? window.UI.safeHtml(v == null ? "" : String(v)) : "");
  const attr = (v) => safe(v).replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  const toast = (msg, type) => {
    if (window.UI && window.UI.showToast) window.UI.showToast(msg, type);
  };

  const TEST_PAGE = "/pages/student/adaptive-test.html";

  /** "8:12 of 20:04", the way the student reads a playback position. */
  function clock(totalSeconds) {
    const seconds = Math.max(0, Math.floor(Number(totalSeconds) || 0));
    const hh = Math.floor(seconds / 3600);
    const mm = Math.floor((seconds % 3600) / 60);
    const ss = seconds % 60;
    const pad = (n) => String(n).padStart(2, "0");
    return hh > 0 ? `${hh}:${pad(mm)}:${pad(ss)}` : `${mm}:${pad(ss)}`;
  }

  function attach(panel, topic, subjectName) {
    if (!panel || !topic) return;

    const state = {
      topicId: topic.topicId,
      topicName: topic.topicName,
      subjectName: subjectName || "",
      opened: [],
      testId: null,
      busy: false,
      // How far the student got, as reported by the host page's player. A page
      // has no position, so this stays null and the whole resource is used.
      watchPosition: typeof topic.watchPosition === "function" ? topic.watchPosition : null,
    };

    // ---- stop marker block, injected at the top of the panel -----------
    const marker = document.createElement("div");
    marker.className = "learned-marker mb-3";
    marker.innerHTML = `
      <div class="d-flex justify-content-between align-items-center gap-2 flex-wrap">
        <div>
          <div class="fw-bold small"><i class="bi bi-flag me-2 text-primary"></i>Learning check</div>
          <div class="text-muted" style="font-size:.78rem" id="lq-summary">
            Open a tutorial or doc, then mark where you stopped learning.
          </div>
        </div>
        <button class="btn btn-primary btn-sm" id="lq-generate" disabled>
          <i class="bi bi-stars me-1"></i>I have learned up to here
        </button>
      </div>
      <div id="lq-queue" class="mt-2"></div>
      <div id="lq-result" class="mt-3"></div>`;
    panel.insertBefore(marker, panel.firstChild);

    const generateBtn = marker.querySelector("#lq-generate");
    const summary = marker.querySelector("#lq-summary");
    const queue = marker.querySelector("#lq-queue");
    const result = marker.querySelector("#lq-result");

    // ---- tracking ------------------------------------------------------
    // The embedded player is a YouTube search for this topic, so watching it
    // counts as opening the first video.
    const embedded = topic.primaryVideoTitle;
    if (embedded) {
      record("VIDEO", embedded, topic.primaryVideoUrl || "");
    }

    panel.addEventListener("click", (event) => {
      const link = event.target.closest("[data-quiz-res]");
      if (!link || !panel.contains(link)) return;
      // Cards can be <a href> or a <button> that plays inline, so the url may
      // live in either place.
      const url = link.dataset.quizUrl || link.getAttribute("href") || "";
      record(link.dataset.quizRes, link.dataset.quizTitle || "", url);
    });

    /** The furthest point the student has played to in the current video. */
    function currentPosition() {
      if (!state.watchPosition) return null;
      let position = null;
      try {
        position = state.watchPosition();
      } catch (err) {
        return null;
      }
      const watched = Math.floor(Number(position && position.watchedSeconds) || 0);
      if (watched <= 0) return null;
      return {
        watchedSeconds: watched,
        durationSeconds: Math.floor(Number(position.durationSeconds) || 0) || null,
      };
    }

    function record(type, title, url) {
      if (!url || state.busy) return;
      const clean = url.split("#")[0];
      if (state.opened.some((r) => r.url === clean)) return;
      state.opened.push({ type: type || "VIDEO", title: title || "Resource", url: clean, viewId: null });
      renderQueue();

      const api = window.API;
      if (!api) return;
      api.post(`/topic-resources/${state.topicId}/views`, {
        resourceType: state.opened[state.opened.length - 1].type,
        resourceTitle: state.opened[state.opened.length - 1].title,
        resourceUrl: clean,
      })
        .then((res) => {
          const last = state.opened[state.opened.length - 1];
          if (last && res && res.data) last.viewId = res.data.viewId;
        })
        .catch(() => {
          /* tracking is best effort; never block the student */
        });
    }

    /**
     * Sends the playback position to the backend. Called on a timer while the
     * student watches, and once more immediately before generating, so the test
     * is never built from a position the backend has not caught up with.
     */
    function pushProgress() {
      const api = window.API;
      if (!api) return Promise.resolve();
      const position = currentPosition();
      if (!position) return Promise.resolve();
      // Only the video currently on screen is being watched, and a page has no
      // position, so only a tracked video is ever reported.
      const target = state.opened.find((r) => r.type !== "DOC") || state.opened[0];
      if (!target || !target.viewId || target.type === "DOC") return Promise.resolve();
      return api
        .patch(`/topic-resources/${state.topicId}/views/${target.viewId}/progress`, {
          watchedSeconds: position.watchedSeconds,
          durationSeconds: position.durationSeconds,
        })
        .then(() => {
          target.watchedSeconds = position.watchedSeconds;
          target.durationSeconds = position.durationSeconds;
          renderQueue();
        })
        .catch(() => {
          /* a missed position only makes the test broader, never wrong */
        });
    }

    function renderQueue() {
      const items = state.opened
        .map(
          (r) => `<span class="badge badge-soft me-1 mb-1">
            <i class="bi ${r.type === "DOC" ? "bi-file-earmark-text" : "bi-play-btn"} me-1"></i>${safe(r.title)}
          </span>`
        )
        .join("");
      queue.innerHTML = items;
      generateBtn.disabled = state.opened.length === 0 || state.busy;

      const position = currentPosition();
      let where = "";
      if (position) {
        where = position.durationSeconds
          ? ` You have watched ${clock(position.watchedSeconds)} of ${clock(position.durationSeconds)}, so that is all the test will cover.`
          : ` You have watched ${clock(position.watchedSeconds)}, so that is all the test will cover.`;
      } else if (state.opened.some((r) => r.type !== "DOC")) {
        where = " Start the video and the test will cover only what you have played.";
      }
      summary.textContent = state.opened.length
        ? `${state.opened.length} resource${state.opened.length > 1 ? "s" : ""} opened for "${state.topicName}".${where} You will not be given a question you have already answered.`
        : "Open a tutorial or doc, then mark where you stopped learning.";
    }

    // ---- generation ---------------------------------------------------
    generateBtn.addEventListener("click", async () => {
      if (state.busy || !state.opened.length) return;
      state.busy = true;
      generateBtn.disabled = true;
      generateBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Generating...';
      result.innerHTML = "";

      // The backend can only quiz the part that was watched, and it only knows
      // that if the last position has reached it.
      await pushProgress();

      const last = state.opened[state.opened.length - 1];
      try {
        const res = await window.API.post(`/topic-resources/${state.topicId}/questions`, {
          difficulty: state.difficulty || null,
          resourceViewId: last && last.viewId ? last.viewId : null,
          // No count is sent: the backend sizes the test from the concept, so
          // the same number is shown here and on the adaptive test page.
          createTest: true,
        });
        const data = (res && res.data) || {};
        state.testId = data.testId || null;
        renderResult(data);
      } catch (err) {
        toast(err.message || "Could not generate questions", "danger");
      } finally {
        state.busy = false;
        generateBtn.innerHTML = '<i class="bi bi-stars me-1"></i>I have learned up to here';
        renderQueue();
      }
    });

    function renderResult(data) {
      const total = (data.questions || []).length;
      if (!total) {
        result.innerHTML = `<div class="alert alert-warning mb-0 small">
          <i class="bi bi-exclamation-triangle me-1"></i>
          No unused questions are left for this topic. Try a different topic, or revisit
          the ones you have already answered.
        </div>`;
        return;
      }

      if (!data.testId) {
        // The questions exist but no test was made, so there is nowhere to send
        // the student. Say so rather than leaving a dead button.
        result.innerHTML = `<div class="alert alert-info mb-0 small">
          <i class="bi bi-info-circle me-1"></i>
          ${safe(total)} question${total > 1 ? "s" : ""} were prepared, but the test could not be
          created. Please try again.
        </div>`;
        return;
      }

      const fromBank = data.fromBank
        ? " AI was unavailable, so these come from the curated question bank."
        : "";

      // The most important line on the panel: whether the questions were written
      // from the lesson, and if not, why.
      const source = data.contentSource === "TRANSCRIPT"
        ? `Taken from the first ${clock(data.coveredSeconds || 0)} of the video.`
        : data.contentSource === "PAGE"
          ? "Taken from the page you read."
          : `This video or page could not be read, so the questions cover "${data.topicName}" in general. ${data.contentNote || ""}`;

      const mins = data.durationMinutes || "?";

      result.innerHTML = `
        <div class="alert alert-success mb-0">
          <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
            <div>
              <div class="fw-bold small">
                <i class="bi bi-patch-check me-1"></i>
                Your test is ready - ${safe(total)} question${total > 1 ? "s" : ""}, ${safe(mins)} min
              </div>
              <div class="text-muted mt-1" style="font-size:.78rem">
                ${safe(source)} Based on ${safe(data.resourceTitle || "what you just studied")}.${safe(fromBank)}
                Take it in the Adaptive Test module to get your score and explanations.
              </div>
            </div>
            <a class="btn btn-primary btn-sm text-nowrap" id="lq-go"
               href="${TEST_PAGE}?testId=${attr(data.testId)}">
              <i class="bi bi-play-circle me-1"></i>Start test
            </a>
          </div>
        </div>`;
    }

    renderQueue();

    // Lets a host page (e.g. the Learn module) track extra resources the student
    // opens after the first one, without re-attaching the whole panel.
    return {
      record: (type, title, url) => record(type, title, url),
      // Also lets the host page say when playback moves, so the panel can keep
      // the "you have watched..." line current without polling the player itself.
      refresh: () => renderQueue(),
      state,
    };
  }

  window.TopicQuiz = { attach };
})();
