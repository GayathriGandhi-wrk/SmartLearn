// API client - talks to the Node gateway, attaches JWT from storage
(function () {
  const API_BASE = (window.APP_CONFIG && window.APP_CONFIG.apiBase) || "http://localhost:3000/api";
  const TOKEN_KEY = "sp_token";
  const USER_KEY = "sp_user";

  function getToken() { return localStorage.getItem(TOKEN_KEY); }
  function setToken(t) { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY); }
  function getUser() {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; }
  }
  function setUser(u) { u ? localStorage.setItem(USER_KEY, JSON.stringify(u)) : localStorage.removeItem(USER_KEY); }
  function isAuthenticated() { return !!getToken(); }
  function logout() { setToken(null); setUser(null); window.location.href = "/pages/auth/login.html"; }

  async function request(method, path, body, isForm) {
    const headers = {};
    const token = getToken();
    if (token) headers["Authorization"] = "Bearer " + token;
    if (body && !isForm) headers["Content-Type"] = "application/json";

    let resp;
    try {
      resp = await fetch(API_BASE + path, {
        method,
        headers,
        body: isForm ? body : body ? JSON.stringify(body) : undefined,
      });
    } catch (err) {
      throw { status: 0, message: "Cannot reach the server. Is it running?" };
    }

    let data = null;
    try { data = await resp.json(); } catch { /* non-JSON */ }

    if (!resp.ok) {
      const message = (data && data.message) || `Request failed (${resp.status})`;
      if (resp.status === 401) {
        // Token expired/invalid
        setToken(null);
      }
      throw { status: resp.status, message, data };
    }
    return data || {};
  }

  window.API = {
    base: API_BASE,
    get: (p) => request("GET", p),
    post: (p, b) => request("POST", p, b),
    put: (p, b) => request("PUT", p, b),
    patch: (p, b) => request("PATCH", p, b),
    del: (p) => request("DELETE", p),
    upload: (p, file, field) => {
      const form = new FormData();
      form.append(field || "file", file);
      return request("POST", p, form, true);
    },
    getToken, setToken, getUser, setUser, isAuthenticated, logout,
  };
})();
