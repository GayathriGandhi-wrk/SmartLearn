/**
 * Reads the playback position out of the embedded YouTube player.
 *
 * "I have learned up to here" only means something if the app knows where "here"
 * is, and the plain iframe the Learn page embeds cannot tell it. The YouTube
 * IFrame API can, once the embed is loaded with `enablejsapi=1`, so this module
 * wraps an existing iframe element and hands back a plain object the caller can
 * read at any time.
 *
 * The API is loaded on demand and only once. If it never arrives - blocked, or
 * the student is offline - `attach` still returns a handle; it just reports no
 * position, and the caller carries on using the whole video rather than
 * pretending it knows where the student stopped.
 */
(function () {
  "use strict";

  const API_SRC = "https://www.youtube.com/iframe_api";

  // The player needs this before the API will talk to it.
  const ENABLED_IFRAME_RE = /[?&]enablejsapi=1/;

  let loading = false;
  let ready = false;
  const waiting = [];

  /** Loads the IFrame API once, resolving every caller that was waiting on it. */
  function load() {
    return new Promise((resolve) => {
      if (ready) return resolve();
      waiting.push(resolve);
      if (loading) return;
      loading = true;
      const script = document.createElement("script");
      script.src = API_SRC;
      script.async = true;
      script.onerror = () => {
        // Nothing can be tracked without the API, so the waiters are released
        // rather than left hanging on a promise that will never settle.
        loading = false;
        flush();
      };
      document.head.appendChild(script);
    });
  }

  function flush() {
    const pending = waiting.splice(0, waiting.length);
    pending.forEach((resolve) => resolve());
  }

  // The API announces itself by calling this global, so it has to be installed
  // before the script above runs.
  window.onYouTubeIframeAPIReady = function () {
    ready = true;
    flush();
  };

  const seconds = (value) => {
    const n = Number(value);
    return Number.isFinite(n) && n >= 0 ? Math.floor(n) : 0;
  };

  /**
   * Watches `iframe` and reports where the student has got to.
   *
   * @param iframe  the .learn-frame element created by VideoPicker
   * @param onTick  optional, called with { watchedSeconds, durationSeconds }
   *                 whenever the position changes
   * @returns {{ position: function, isReady: function, destroy: function }}
   */
  function attach(iframe, onTick) {
    const handle = {
      player: null,
      ready: false,
      last: { watchedSeconds: 0, durationSeconds: 0 },
    };

    /** The position, always a plain object so callers never see a YT player. */
    handle.position = function () {
      if (!handle.ready || !handle.player) return { watchedSeconds: 0, durationSeconds: 0 };
      let current = 0;
      let duration = 0;
      try {
        current = seconds(handle.player.getCurrentTime());
        duration = seconds(handle.player.getDuration());
      } catch (err) {
        // The player can throw while it is being torn down, which is not a
        // reason to lose the position the student already reached.
        return handle.last;
      }
      if (current > handle.last.watchedSeconds) {
        handle.last = { watchedSeconds: current, durationSeconds: duration };
      } else if (duration > 0) {
        handle.last.durationSeconds = duration;
      }
      return handle.last;
    };

    handle.isReady = function () {
      return handle.ready;
    };

    handle.destroy = function () {
      handle.ready = false;
      handle.player = null;
    };

    if (!iframe || !ENABLED_IFRAME_RE.test(iframe.getAttribute("src") || "")) {
      return handle;
    }

    load().then(() => {
      if (!window.YT || !window.YT.Player || !iframe.isConnected) return;
      // Constructing over the existing iframe keeps the src the Learn page
      // already chose, instead of replacing the player.
      handle.player = new window.YT.Player(iframe, {
        events: {
          onReady: () => {
            handle.ready = true;
            if (typeof onTick === "function") onTick(handle.position());
          },
        },
      });
    });

    return handle;
  }

  /**
   * Reports the position at a steady interval while `isPlaying` says the
   * student is still watching. Returns a stop function.
   */
  function poll(handle, isPlaying, everyMs, onTick) {
    const period = everyMs || 10000;
    const timer = setInterval(() => {
      if (!isPlaying()) return;
      if (typeof onTick === "function") onTick(handle.position());
    }, period);
    return () => clearInterval(timer);
  }

  window.PlayerProgress = { attach, poll, API_SRC };
})();
