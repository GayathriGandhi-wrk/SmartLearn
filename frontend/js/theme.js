// Theme manager - persists preference in localStorage
(function () {
  const KEY = "sp_theme";
  function apply(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    const toggles = document.querySelectorAll("[data-theme-toggle]");
    toggles.forEach((t) => {
      t.innerHTML = theme === "dark" ? '<i class="bi bi-sun"></i>' : '<i class="bi bi-moon"></i>';
    });
  }
  function init() {
    const saved = localStorage.getItem(KEY) || "light";
    apply(saved);
    document.addEventListener("click", (e) => {
      const toggle = e.target.closest("[data-theme-toggle]");
      if (!toggle) return;
      const next = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
      localStorage.setItem(KEY, next);
      apply(next);
    });
  }
  window.ThemeManager = { init, apply, get: () => localStorage.getItem(KEY) || "light" };
})();
