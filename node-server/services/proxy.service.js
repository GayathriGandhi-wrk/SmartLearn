/**
 * Spring Boot REST API proxy service.
 * Forwards requests to the Spring Boot backend and normalizes errors.
 */
const config = require("../config");
const { parseError } = require("../utils/apiUtils");

/**
 * @param {object} options { method, path, token, body, params, query }
 */
async function proxyRequest({ method, path, token, body, params, query }) {
  let url = `${config.springApiBase}${path}`;
  if (params && Object.keys(params).length) {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => qs.append(k, v));
    url += `?${qs.toString()}`;
  }
  if (query && typeof query === "object") {
    url = url.replace(/\/:[^/]+/g, "");
  }

  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.springTimeoutMs);

  try {
    const response = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
    clearTimeout(timer);
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      const err = new Error(data.message || `Upstream error (${response.status})`);
      err.status = response.status;
      err.data = data;
      throw err;
    }
    return data;
  } catch (err) {
    clearTimeout(timer);
    if (err.name === "AbortError") {
      const e = new Error("Upstream service timed out");
      e.status = 504;
      throw e;
    }
    throw err;
  }
}

module.exports = { proxyRequest };
