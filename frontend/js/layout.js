// Shared UI helpers + layout components (navbar / sidebar / toasts)
(function () {
  function showToast(message, type) {
    let container = document.querySelector(".toast-container");
    if (!container) {
      container = document.createElement("div");
      container.className = "toast-container";
      document.body.appendChild(container);
    }
    const el = document.createElement("div");
    el.className = `alert alert-${type || "info"} shadow`;
    el.innerHTML = message;
    el.style.animation = "fadeUp 0.3s ease";
    container.appendChild(el);
    setTimeout(() => {
      el.style.transition = "opacity 0.4s";
      el.style.opacity = "0";
      setTimeout(() => el.remove(), 400);
    }, 3500);
    return el;
  }

  function showLoader(target, show) {
    let el = typeof target === "string" ? document.querySelector(target) : target;
    if (!el) return;
    if (show) {
      el.classList.add("skeleton");
      el.setAttribute("aria-busy", "true");
    } else {
      el.classList.remove("skeleton");
      el.removeAttribute("aria-busy");
    }
  }

  function safeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str == null ? "" : String(str);
    return div.innerHTML;
  }

  function initials(name) {
    if (!name) return "U";
    return name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join("").toUpperCase();
  }

  function roleBadge(role) {
    const map = { ADMIN: "badge-soft-red", STUDENT: "badge-soft-green", PARENT: "badge-soft-orange", TEACHER: "badge-soft" };
    const cls = map[role] || "badge-soft";
    return `<span class="badge ${cls}">${safeHtml(role)}</span>`;
  }

  function buildSidebar(items, active) {
    return `
      <div class="sidebar" id="sidebar">
        <a href="/index.html" class="brand">
          <div class="logo"><i class="bi bi-graph-up-arrow"></i></div>
          <span>Smart Learn</span>
        </a>
        ${items.map((s) => `
          ${s.header ? `<div class="nav-section">${s.header}</div>` : ""}
          ${s.items.map((i) => `
            <a href="${i.href}" class="nav-link ${i.href === active ? "active" : ""}" ${i.icon ? "" : ""}>
              ${i.icon ? `<i class="bi bi-${i.icon}"></i>` : ""}
              <span>${i.label}</span>
            </a>
          `).join("")}
        `).join("")}
        <div class="nav-section">Account</div>
        <a href="javascript:void(0)" class="nav-link" id="logout-link">
          <i class="bi bi-box-arrow-right"></i><span>Logout</span>
        </a>
      </div>`;
  }

  function buildNavbar(title, actionsHtml) {
    return `
      <nav class="navbar">
        <div style="display:flex;align-items:center;gap:0.75rem;">
          <button class="btn btn-ghost menu-toggle" id="menu-toggle" aria-label="Menu"><i class="bi bi-list"></i></button>
          <span class="page-title">${safeHtml(title)}</span>
        </div>
        <div class="actions">
          ${actionsHtml || ""}
          <button class="btn btn-ghost" data-theme-toggle aria-label="Theme toggle"><i class="bi bi-moon"></i></button>
          <div class="avatar" id="navbar-avatar" title="My profile">U</div>
        </div>
      </nav>`;
  }

  function renderShell(items, active, title, actionsHtml) {
    const app = document.getElementById("app");
    if (!app) return;
    app.innerHTML = `
      <div class="layout">
        ${buildSidebar(items, active)}
        <div class="main">
          ${buildNavbar(title, actionsHtml)}
          <div class="content" id="page-content"></div>
        </div>
      </div>`;
    // Mobile sidebar toggle
    const toggle = document.getElementById("menu-toggle");
    const sidebar = document.getElementById("sidebar");
    if (toggle) toggle.addEventListener("click", () => sidebar.classList.toggle("open"));
    // Logout
    const logoutLink = document.getElementById("logout-link");
    if (logoutLink) logoutLink.addEventListener("click", () => {
      window.API.logout();
    });
    // Avatar
    const avatar = document.getElementById("navbar-avatar");
    const user = window.API.getUser();
    if (avatar && user) avatar.textContent = initials(user.fullName || user.name || "U");
    // Inactive idle check -> redirect login
    if (!window.API.isAuthenticated() && window.location.pathname.includes("student")) {
      window.location.href = "/pages/auth/login.html";
    }
  }

  function roleItems() {
    const user = window.API.getUser() || {};
    const studentSections = [
      { header: "Main", items: [
        { label: "Dashboard", icon: "speedometer2", href: "/pages/student/dashboard.html" },
        { label: "My Subjects", icon: "book", href: "/pages/student/subjects.html" },
        { label: "Question Bank", icon: "card-text", href: "/pages/student/question-bank.html" },
      ]},
      { header: "Assessments", items: [
        { label: "Adaptive Test", icon: "clipboard-check", href: "/pages/student/adaptive-test.html" },
        { label: "My Results", icon: "trophy", href: "/pages/student/test-result.html" },
        { label: "Practice", icon: "pen", href: "/pages/student/practice-questions.html" },
      ]},
      { header: "AI Tools", items: [
        { label: "Predict My Score", icon: "crystal", href: "/pages/student/prediction.html" },
        { label: "Study Planner", icon: "calendar-check", href: "/pages/student/study-planner.html" },
        { label: "Recommendations", icon: "magic", href: "/pages/student/recommendation.html" },
      ]},
      { header: "Insights", items: [
        { label: "Analytics", icon: "bar-chart-line", href: "/pages/student/analytics.html" },
        { label: "Chat Assistant", icon: "robot", href: "/pages/student/chatbot.html" },
      ]},
    ];
    if (user.role === "ADMIN") {
      return [{ header: "Admin", items: [
        { label: "Manage Students", icon: "people", href: "/pages/admin/students.html" },
        { label: "Manage Subjects", icon: "book", href: "/pages/admin/subjects.html" },
        { label: "Manage Questions", icon: "card-text", href: "/pages/admin/questions.html" },
        { label: "My Profile", icon: "person", href: "/pages/admin/profile.html" },
      ]}];
    }
    return studentSections;
  }

  window.UI = { showToast, showLoader, safeHtml, initials, roleBadge, renderShell, roleItems };
})();
