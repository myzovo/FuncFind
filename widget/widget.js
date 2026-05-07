(() => {
  if (document.getElementById("site-nav-widget-host")) return;

  const DEFAULT_CONFIG = {
    apiBase: "http://localhost:8080",
    kbName: "default-kb",
    topK: 6,
    sessionId: "",
    title: "Site Navigator",
    subtitle: "Ask where a feature lives"
  };

  const userConfig = typeof window.__SITE_NAV_CONFIG__ === "object" ? window.__SITE_NAV_CONFIG__ : {};
  const config = { ...DEFAULT_CONFIG, ...userConfig };

  const host = document.createElement("div");
  host.id = "site-nav-widget-host";
  document.body.appendChild(host);

  const shadow = host.attachShadow({ mode: "open" });
  shadow.innerHTML = `
    <style>
      @import url("https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Sora:wght@400;500;600&display=swap");

      :host {
        all: initial;
        font-family: "Sora", sans-serif;
        position: fixed;
        right: 24px;
        bottom: 24px;
        z-index: 2147483646;
      }

      .widget {
        position: relative;
      }

      .fab {
        width: 54px;
        height: 54px;
        border-radius: 16px;
        border: none;
        background: linear-gradient(130deg, #1f8a70, #0f766e);
        color: #fff;
        font-weight: 700;
        letter-spacing: 0.02em;
        cursor: pointer;
        box-shadow: 0 14px 30px rgba(15, 118, 110, 0.25);
        display: flex;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s ease, box-shadow 0.2s ease;
      }

      .fab:hover {
        transform: translateY(-2px);
        box-shadow: 0 18px 36px rgba(15, 118, 110, 0.32);
      }

      .panel {
        position: absolute;
        right: 0;
        bottom: 68px;
        width: min(360px, 86vw);
        background: #fff;
        border-radius: 18px;
        border: 1px solid rgba(31, 138, 112, 0.15);
        box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
        padding: 18px 18px 16px;
        display: none;
        animation: rise 0.25s ease;
      }

      .panel.open {
        display: block;
      }

      .panel-header {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
      }

      .title {
        font-family: "Space Grotesk", sans-serif;
        font-size: 1.05rem;
        font-weight: 600;
      }

      .subtitle {
        font-size: 0.8rem;
        color: #64748b;
        margin-top: 4px;
      }

      .icon-btn {
        border: none;
        background: #f1f5f9;
        color: #0f172a;
        width: 28px;
        height: 28px;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 700;
      }

      .query {
        display: flex;
        gap: 8px;
        margin-bottom: 10px;
      }

      .query input {
        flex: 1;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 10px 12px;
        font-size: 0.9rem;
        font-family: "Sora", sans-serif;
      }

      .query button {
        border: none;
        border-radius: 10px;
        padding: 10px 14px;
        background: #1f8a70;
        color: #fff;
        font-weight: 600;
        cursor: pointer;
      }

      .status {
        font-size: 0.8rem;
        color: #64748b;
        margin-bottom: 8px;
      }

      .answer {
        font-size: 0.9rem;
        color: #0f172a;
        line-height: 1.5;
        background: #f8fafc;
        border-radius: 12px;
        padding: 10px 12px;
        border: 1px solid #e2e8f0;
        margin-bottom: 10px;
      }

      .locations {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .location {
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 10px 12px;
        background: #fff;
      }

      .location-title {
        font-size: 0.85rem;
        font-weight: 600;
        color: #0f172a;
        margin-bottom: 6px;
      }

      .location-meta {
        font-size: 0.75rem;
        color: #64748b;
        margin-bottom: 8px;
        word-break: break-all;
      }

      .locate-btn {
        border: none;
        border-radius: 10px;
        padding: 8px 10px;
        background: #0f766e;
        color: #fff;
        font-weight: 600;
        cursor: pointer;
      }

      @keyframes rise {
        from { opacity: 0; transform: translateY(8px); }
        to { opacity: 1; transform: translateY(0); }
      }

      @media (max-width: 640px) {
        :host {
          right: 16px;
          bottom: 16px;
        }
      }
    </style>
    <div class="widget">
      <button class="fab" id="navFab" aria-label="Open navigator">NAV</button>
      <div class="panel" id="navPanel">
        <div class="panel-header">
          <div>
            <div class="title">${escapeHtml(config.title)}</div>
            <div class="subtitle">${escapeHtml(config.subtitle)}</div>
          </div>
          <button class="icon-btn" id="navClose" aria-label="Close">×</button>
        </div>
        <form class="query" id="navForm">
          <input id="navInput" type="text" placeholder="Ask where a feature is" />
          <button type="submit">Ask</button>
        </form>
        <div class="status" id="navStatus">Idle</div>
        <div class="answer" id="navAnswer">Ask a question to get a location hint.</div>
        <div class="locations" id="navLocations"></div>
      </div>
    </div>
  `;

  const ui = {
    fab: shadow.getElementById("navFab"),
    panel: shadow.getElementById("navPanel"),
    close: shadow.getElementById("navClose"),
    form: shadow.getElementById("navForm"),
    input: shadow.getElementById("navInput"),
    status: shadow.getElementById("navStatus"),
    answer: shadow.getElementById("navAnswer"),
    locations: shadow.getElementById("navLocations")
  };

  const STORAGE_KEY = "site_nav_widget_locate";

  ui.fab.addEventListener("click", () => togglePanel(true));
  ui.close.addEventListener("click", () => togglePanel(false));

  ui.form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const question = ui.input.value.trim();
    if (!question) return;
    await askQuestion(question);
  });

  function togglePanel(open) {
    ui.panel.classList.toggle("open", Boolean(open));
    if (open) {
      setTimeout(() => ui.input.focus(), 50);
    }
  }

  async function askQuestion(question) {
    setStatus("Searching...");
    ui.answer.textContent = "";
    ui.locations.innerHTML = "";

    try {
      const response = await fetch(`${config.apiBase}/api/chat`, {
        method: "POST",
        headers: buildHeaders(),
        body: JSON.stringify({
          question,
          kbName: config.kbName,
          topK: config.topK
        })
      });

      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload?.error || "Request failed");
      }

      ui.answer.textContent = payload.answer || "No answer available.";
      renderLocations(collectLocations(payload));
      setStatus("Ready");
    } catch (error) {
      ui.answer.textContent = "Failed to retrieve answer.";
      setStatus(error.message || "Request failed");
    }
  }

  function buildHeaders() {
    const headers = { "Content-Type": "application/json" };
    if (config.sessionId) {
      headers["X-Client-Session-Id"] = config.sessionId;
    }
    return headers;
  }

  function setStatus(text) {
    ui.status.textContent = text;
  }

  function collectLocations(payload) {
    const locations = [];
    const seen = new Set();

    const rawLocations = Array.isArray(payload.locations) ? payload.locations : [];
    rawLocations.forEach((loc) => pushLocation(loc));

    const contexts = Array.isArray(payload.contextChunks) ? payload.contextChunks : [];
    contexts.forEach((chunk) => {
      pushLocation({
        pageUrl: chunk.pageUrl,
        selector: chunk.selector,
        xpath: chunk.xpath,
        evidence: chunk.evidence || chunk.text,
        elementText: chunk.elementText,
        context: chunk.context
      });
    });

    function pushLocation(loc) {
      if (!loc || (!loc.pageUrl && !loc.selector)) return;
      const key = `${loc.pageUrl || ""}::${loc.selector || ""}`;
      if (seen.has(key)) return;
      seen.add(key);
      locations.push(loc);
    }

    return locations;
  }

  function renderLocations(items) {
    if (!items.length) {
      const empty = document.createElement("div");
      empty.className = "status";
      empty.textContent = "No location hints returned.";
      ui.locations.appendChild(empty);
      return;
    }

    items.forEach((item) => {
      const card = document.createElement("div");
      card.className = "location";

      const title = document.createElement("div");
      title.className = "location-title";
      title.textContent = item.elementText || "Location";

      const meta = document.createElement("div");
      meta.className = "location-meta";
      meta.textContent = item.pageUrl || item.selector || "";

      const btn = document.createElement("button");
      btn.className = "locate-btn";
      btn.type = "button";
      btn.textContent = "Locate";
      btn.addEventListener("click", () => handleLocate(item));

      card.appendChild(title);
      card.appendChild(meta);
      if (item.evidence) {
        const evidence = document.createElement("div");
        evidence.className = "location-meta";
        evidence.textContent = item.evidence;
        card.appendChild(evidence);
      }
      card.appendChild(btn);
      ui.locations.appendChild(card);
    });
  }

  function handleLocate(location) {
    const target = resolveTargetUrl(location.pageUrl);
    if (target && !isSamePage(target)) {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(location));
      window.location.href = target;
      return;
    }

    const result = locateElement(location);
    if (!result.element) {
      setStatus(`Element not found (${result.reason}).`);
      reportLocateFailure(location, result.reason, result.method);
      return;
    }

    highlightElement(result.element);
    const methodNote = result.method ? ` via ${result.method}` : "";
    setStatus(`Element highlighted${methodNote}.`);
  }

  function resolveTargetUrl(value) {
    if (!value) return "";
    try {
      return new URL(value, window.location.href).toString();
    } catch {
      return "";
    }
  }

  function isSamePage(targetUrl) {
    try {
      const target = new URL(targetUrl, window.location.href);
      const current = new URL(window.location.href);
      return target.origin === current.origin && target.pathname === current.pathname;
    } catch {
      return false;
    }
  }

  function locateElement(location) {
    const attempts = [];
    if (hasText(location.xpath)) {
      const xpathResult = tryFindByXpath(location.xpath);
      if (xpathResult.error) {
        attempts.push("xpath syntax error");
      } else if (xpathResult.node && !isInsideWidget(xpathResult.node)) {
        return { element: xpathResult.node, method: "xpath" };
      } else {
        attempts.push("xpath not found");
      }
    }

    if (hasText(location.selector)) {
      const candidate = document.querySelector(location.selector);
      if (candidate && !isInsideWidget(candidate)) {
        return { element: candidate, method: "selector" };
      }
      attempts.push("selector not found");
    }

    if (hasText(location.elementText)) {
      const candidate = findByText(location.elementText);
      if (candidate && !isInsideWidget(candidate)) {
        return { element: candidate, method: "text" };
      }
      attempts.push("text not matched");
    }

    const reason = attempts.length ? attempts.join("; ") : "no locator";
    return { element: null, method: "", reason };
  }

  function tryFindByXpath(xpath) {
    try {
      const result = document.evaluate(
        xpath,
        document,
        null,
        XPathResult.FIRST_ORDERED_NODE_TYPE,
        null
      );
      return { node: result.singleNodeValue, error: false };
    } catch {
      return { node: null, error: true };
    }
  }

  function findByText(text) {
    const target = String(text || "").trim().toLowerCase();
    if (!target) return null;

    const selectors = [
      "a[href]",
      "button",
      "[role='button']",
      "[role='link']",
      "[role='menuitem']",
      "input[type=submit]",
      "button[type=submit]",
      "input[type=button]"
    ];

    const nodes = document.querySelectorAll(selectors.join(","));
    for (const node of nodes) {
      if (isInsideWidget(node)) continue;
      const textValue = (node.innerText || node.textContent || "").trim().toLowerCase();
      const aria = (node.getAttribute && node.getAttribute("aria-label")) || "";
      const title = (node.getAttribute && node.getAttribute("title")) || "";
      const hay = `${textValue} ${aria} ${title}`.trim();
      if (!hay) continue;
      if (hay.includes(target)) {
        return node;
      }
    }
    return null;
  }

  function reportLocateFailure(location, reason, method) {
    if (!config.apiBase) return;
    const payload = {
      kbName: config.kbName,
      status: "failed",
      pageUrl: location.pageUrl || "",
      selector: location.selector || "",
      xpath: location.xpath || "",
      elementText: location.elementText || "",
      method: method || "",
      reason: reason || ""
    };

    fetch(`${config.apiBase}/api/locate`, {
      method: "POST",
      headers: buildHeaders(),
      body: JSON.stringify(payload)
    }).catch(() => undefined);
  }

  function isInsideWidget(el) {
    return el.closest && el.closest("#site-nav-widget-host");
  }

  function hasText(value) {
    return value != null && String(value).trim().length > 0;
  }

  function highlightElement(el) {
    el.scrollIntoView({ behavior: "smooth", block: "center" });
    const rect = el.getBoundingClientRect();
    const overlay = document.createElement("div");

    overlay.style.position = "fixed";
    overlay.style.left = `${Math.max(rect.left - 6, 0)}px`;
    overlay.style.top = `${Math.max(rect.top - 6, 0)}px`;
    overlay.style.width = `${Math.max(rect.width + 12, 12)}px`;
    overlay.style.height = `${Math.max(rect.height + 12, 12)}px`;
    overlay.style.border = "2px solid #f08a5d";
    overlay.style.borderRadius = "10px";
    overlay.style.boxShadow = "0 0 0 6px rgba(240, 138, 93, 0.2)";
    overlay.style.pointerEvents = "none";
    overlay.style.zIndex = "2147483647";
    overlay.style.transition = "opacity 0.3s ease";

    document.body.appendChild(overlay);
    setTimeout(() => {
      overlay.style.opacity = "0";
      setTimeout(() => overlay.remove(), 300);
    }, 2200);
  }

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/\"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function checkPendingLocate() {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    sessionStorage.removeItem(STORAGE_KEY);
    try {
      const location = JSON.parse(raw);
      if (location) {
        handleLocate(location);
      }
    } catch {
      // ignore
    }
  }

  checkPendingLocate();
})();
