/**
 * Turns a topic's learning resources into a quiz.
 *
 * A student opens a tutorial video or doc, and the panel keeps track of it. When
 * they press the stop marker ("I have learned up to here") questions are
 * generated for that topic. The backend guarantees the same question is never
 * returned to the same student twice, so this module does not de-duplicate.
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

  const LETTERS = ["A", "B", "C", "D"];
  const OPTIONS = ["optionA", "optionB", "optionC", "optionD"];

  function attach(panel, topic, subjectName) {
    if (!panel || !topic) return;

    const state = {
      topicId: topic.topicId,
      topicName: topic.topicName,
      subjectName: subjectName || "",
      opened: [],
      questions: [],
      answers: {},
      busy: false,
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
      <div id="lq-quiz" class="mt-3"></div>`;
    panel.insertBefore(marker, panel.firstChild);

    const generateBtn = marker.querySelector("#lq-generate");
    const summary = marker.querySelector("#lq-summary");
    const queue = marker.querySelector("#lq-queue");
    const quiz = marker.querySelector("#lq-quiz");

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
      summary.textContent = state.opened.length
        ? `${state.opened.length} resource${state.opened.length > 1 ? "s" : ""} opened for "${state.topicName}". Questions will cover only what you have seen, and will not repeat ones you already answered.`
        : "Open a tutorial or doc, then mark where you stopped learning.";
    }

    // ---- generation ---------------------------------------------------
    generateBtn.addEventListener("click", async () => {
      if (state.busy || !state.opened.length) return;
      state.busy = true;
      generateBtn.disabled = true;
      generateBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Generating...';
      state.questions = [];
      state.answers = {};
      quiz.innerHTML = "";

      const last = state.opened[state.opened.length - 1];
      try {
        const res = await window.API.post(`/topic-resources/${state.topicId}/questions`, {
          count: 5,
          difficulty: state.difficulty || null,
          resourceViewId: last && last.viewId ? last.viewId : null,
        });
        const data = (res && res.data) || {};
        state.questions = data.questions || [];
        state.topicName = data.topicName || state.topicName;
        renderQuiz(data);
        if (!state.questions.length) {
          quiz.innerHTML = `<div class="alert alert-warning mb-0 small">
            No unused questions are left for this topic. Try a different topic, or revisit
            the ones you have already answered.
          </div>`;
        }
      } catch (err) {
        toast(err.message || "Could not generate questions", "danger");
      } finally {
        state.busy = false;
        generateBtn.innerHTML = '<i class="bi bi-stars me-1"></i>I have learned up to here';
        renderQueue();
      }
    });

    function renderQuiz(data) {
      if (!state.questions.length) return;
      const fromBank = data.fromBank
        ? " AI questions were unavailable, so these come from the curated bank."
        : "";
      quiz.innerHTML = `
        <div class="d-flex justify-content-between align-items-center gap-2 flex-wrap mb-2">
          <div class="fw-bold small text-uppercase text-muted">
            <i class="bi bi-patch-question me-1"></i>Your questions
          </div>
          <span class="text-muted" style="font-size:.75rem">${safe(data.resourceTitle || "Based on what you studied")}${safe(fromBank)}</span>
        </div>
        ${state.questions.map(questionHtml).join("")}
        <button class="btn btn-primary btn-sm mt-2" id="lq-check">
          <i class="bi bi-check2-circle me-1"></i>Check my answers
        </button>
        <div id="lq-score" class="mt-2"></div>`;

      quiz.querySelectorAll("[data-quiz-choice]").forEach((input) => {
        input.addEventListener("change", () => {
          state.answers[input.dataset.quizChoice] = input.value;
        });
      });
      quiz.querySelector("#lq-check").addEventListener("click", checkAnswers);
    }

    function questionHtml(q) {
      const given = state.answers[q.questionId];
      return `
        <div class="learned-question" data-quiz-q="${attr(q.questionId)}">
          <div class="fw-semibold small mb-2">${safe(q.questionText)}</div>
          <div class="d-flex flex-column gap-1">
            ${OPTIONS.map((key, i) => {
              const letter = LETTERS[i];
              const picked = given === letter;
              let cls = "learned-option";
              if (given) {
                if (picked) cls += picked === given ? " is-correct" : " is-wrong";
                if (given === letter) cls += " is-answer";
              }
              return `
                <label class="${cls}">
                  <input type="radio" class="form-check-input mt-1" name="lq-${attr(q.questionId)}"
                    value="${letter}" data-quiz-choice="${attr(q.questionId)}"
                    ${picked ? "checked" : ""} ${given ? "disabled" : ""}>
                  <span class="small">${letter}. ${safe(q[key])}</span>
                </label>`;
            }).join("")}
          </div>
          <div class="learned-feedback small mt-1"></div>
        </div>`;
    }

    async function checkAnswers() {
      const unanswered = state.questions.filter((q) => !state.answers[q.questionId]);
      if (unanswered.length) {
        toast(`Please answer all ${unanswered.length} remaining question(s)`, "warning");
        return;
      }
      let correct = 0;
      for (const q of state.questions) {
        const box = quiz.querySelector(`[data-quiz-q="${q.questionId}"]`);
        if (!box) continue;
        const feedback = box.querySelector(".learned-feedback");
        try {
          const res = await window.API.post(
            `/topic-resources/questions/${q.questionId}/answer`,
            { selectedAnswer: state.answers[q.questionId] }
          );
          const data = (res && res.data) || {};
          const ok = !!data.correct;
          if (ok) correct += 1;
          box.classList.add(ok ? "is-correct" : "is-wrong");
          feedback.innerHTML = ok
            ? `<span class="text-success fw-semibold"><i class="bi bi-check-circle me-1"></i>Correct.</span>
               ${data.explanation ? `<div class="text-muted mt-1">${safe(data.explanation)}</div>` : ""}`
            : `<span class="text-danger fw-semibold"><i class="bi bi-x-circle me-1"></i>Incorrect. Answer: ${safe(data.correctAnswer || "-")}.</span>
               ${data.explanation ? `<div class="text-muted mt-1">${safe(data.explanation)}</div>` : ""}`;
        } catch (err) {
          feedback.innerHTML = `<span class="text-danger small">${safe(err.message || "Could not save this answer")}</span>`;
        }
      }
      quiz.querySelectorAll("[data-quiz-choice]").forEach((i) => (i.disabled = true));
      const score = quiz.querySelector("#lq-score");
      if (score) {
        score.innerHTML = `<div class="alert ${
          correct === state.questions.length ? "alert-success" : "alert-info"
        } mb-0 py-2 small">
          You scored <strong>${correct}/${state.questions.length}</strong>.
          Press the button above for a fresh set - these questions will not be repeated to you.
        </div>`;
      }
    }

    renderQueue();

    // Lets a host page (e.g. the Learn module) track extra resources the student
    // opens after the first one, without re-attaching the whole panel.
    return {
      record: (type, title, url) => record(type, title, url),
      state,
    };
  }

  window.TopicQuiz = { attach };
})();
