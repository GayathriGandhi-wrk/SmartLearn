// Auth page helpers
(function () {
  async function requireLoggedOut() {
    if (!window.API.isAuthenticated()) return;
    try {
      await API.get("/auth/me");
    } catch (err) {
      window.API.setToken(null);
      window.API.setUser(null);
    }
  }

  function bindForm(id, handler) {
    const form = document.getElementById(id);
    if (!form) return;
    form.addEventListener("submit", async (e) => {
      e.preventDefault();
      const btn = form.querySelector('button[type="submit"]');
      const original = btn.innerHTML;
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Please wait...';
      try {
        await handler(new FormData(form));
      } catch (err) {
        window.UI.showToast(`<i class="bi bi-exclamation-triangle me-2"></i>${window.UI.safeHtml(err.message)}`, "danger");
      } finally {
        btn.disabled = false;
        btn.innerHTML = original;
      }
    });
  }

  window.AuthPage = { requireLoggedOut, bindForm };
})();
