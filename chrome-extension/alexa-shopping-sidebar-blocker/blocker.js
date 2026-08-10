/*
 * Alexa for Shopping Blocker
 *
 * Amazon's "Alexa for Shopping" panel (formerly branded "Rufus") pops in
 * from the side of the page on searches/product pages. Amazon reshuffles
 * its class names and element ids frequently (including per-user test
 * cohorts), so instead of hard-coding one selector this script:
 *
 *   1. Applies a static CSS hide-list immediately (blocker.css) to avoid
 *      a flash of the panel before JS runs.
 *   2. Runs a heuristic DOM sweep that flags any element whose id, class,
 *      aria-label, data-testid, or iframe src/title mentions the panel
 *      by name ("rufus" / "alexa" + "shop").
 *   3. Watches the DOM with a MutationObserver so panels injected after
 *      load (e.g. on search) get caught too.
 *   4. Clears the localStorage/sessionStorage flags Amazon uses to decide
 *      whether to auto-open the panel, so it doesn't spring back open on
 *      the next page in the same session.
 *   5. Re-sweeps on SPA-style navigations (Amazon reuses pushState
 *      instead of a full reload between searches).
 */

(() => {
  const STORAGE_KEY = "alexaShoppingBlockerEnabled";
  const NAME_PATTERN = /rufus|alexa[-_ ]?(for[-_ ]?)?shop/i;
  const STORAGE_KEY_PATTERN = /^rufus:|alexashopping/i;
  const HIDE_ATTR = "data-asb-hidden";

  let enabled = true;
  let hiddenCount = 0;
  let observer = null;

  const getSetting = () =>
    new Promise((resolve) => {
      try {
        chrome.storage.sync.get({ [STORAGE_KEY]: true }, (items) => {
          resolve(items[STORAGE_KEY] !== false);
        });
      } catch (e) {
        resolve(true);
      }
    });

  function matches(str) {
    return !!str && NAME_PATTERN.test(str);
  }

  function isAlexaShoppingElement(el) {
    if (!(el instanceof Element)) return false;
    if (el.hasAttribute(HIDE_ATTR)) return false;

    if (matches(el.id)) return true;
    if (typeof el.className === "string" && matches(el.className)) return true;
    if (matches(el.getAttribute("aria-label"))) return true;
    if (matches(el.getAttribute("data-testid"))) return true;

    const tag = el.tagName;
    if (tag === "IFRAME" || tag === "IMG" || tag === "A") {
      if (matches(el.getAttribute("src"))) return true;
      if (matches(el.getAttribute("title"))) return true;
      if (matches(el.getAttribute("href"))) return true;
    }

    return false;
  }

  function hide(el) {
    if (el.hasAttribute(HIDE_ATTR)) return;
    el.setAttribute(HIDE_ATTR, "true");
    el.style.setProperty("display", "none", "important");
    el.style.setProperty("visibility", "hidden", "important");
    el.style.setProperty("pointer-events", "none", "important");
    hiddenCount += 1;
  }

  function scrubDockedBody() {
    const body = document.body;
    if (!body) return;
    [...body.classList]
      .filter((c) => /rufus[-_]?docked|alexa[-_]?shopping[-_]?docked/i.test(c))
      .forEach((c) => body.classList.remove(c));
    body.style.removeProperty("margin-right");
    body.style.removeProperty("margin-left");
    document.documentElement.style.removeProperty("--rufus-docked-panel-width");
  }

  function sweep(root) {
    if (!enabled || !root || !root.querySelectorAll) return;

    let candidates;
    if (isAlexaShoppingElement(root)) {
      hide(root);
    }
    candidates = root.querySelectorAll(
      '[id],[class],[aria-label],[data-testid],iframe[src],iframe[title]'
    );
    for (const el of candidates) {
      if (isAlexaShoppingElement(el)) hide(el);
    }
    scrubDockedBody();
  }

  function clearPersistedState() {
    try {
      for (const store of [window.localStorage, window.sessionStorage]) {
        if (!store) continue;
        const toRemove = [];
        for (let i = 0; i < store.length; i++) {
          const key = store.key(i);
          if (key && STORAGE_KEY_PATTERN.test(key)) toRemove.push(key);
        }
        toRemove.forEach((k) => store.removeItem(k));
      }
    } catch (e) {
      // storage access can throw in some frame contexts; ignore.
    }
  }

  let sweepScheduled = false;
  function scheduleSweep() {
    if (sweepScheduled || !enabled) return;
    sweepScheduled = true;
    const run = () => {
      sweepScheduled = false;
      sweep(document.documentElement);
    };
    if (window.requestIdleCallback) {
      requestIdleCallback(run, { timeout: 500 });
    } else {
      setTimeout(run, 50);
    }
  }

  function startObserving() {
    if (observer) return;
    observer = new MutationObserver((mutations) => {
      for (const m of mutations) {
        if (m.addedNodes && m.addedNodes.length) {
          scheduleSweep();
          break;
        }
        if (m.type === "attributes") {
          scheduleSweep();
          break;
        }
      }
    });
    observer.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["class", "id", "aria-label", "data-state", "style"],
    });
  }

  function stopObserving() {
    if (observer) {
      observer.disconnect();
      observer = null;
    }
  }

  function patchHistoryForSpaNav() {
    const fire = () => {
      clearPersistedState();
      scheduleSweep();
    };
    ["pushState", "replaceState"].forEach((fn) => {
      const original = history[fn];
      history[fn] = function (...args) {
        const result = original.apply(this, args);
        fire();
        return result;
      };
    });
    window.addEventListener("popstate", fire);
  }

  // Respond to popup requests for current status.
  try {
    chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
      if (msg && msg.type === "asb-get-status") {
        sendResponse({ enabled, hiddenCount });
      }
      return true;
    });
  } catch (e) {
    // extension context can be invalidated during reload; ignore.
  }

  try {
    chrome.storage.onChanged.addListener((changes, area) => {
      if (area === "sync" && STORAGE_KEY in changes) {
        enabled = changes[STORAGE_KEY].newValue !== false;
        if (enabled) {
          clearPersistedState();
          sweep(document.documentElement);
          startObserving();
        } else {
          stopObserving();
        }
      }
    });
  } catch (e) {
    // ignore
  }

  async function init() {
    enabled = await getSetting();
    if (!enabled) return;

    clearPersistedState();
    patchHistoryForSpaNav();

    const boot = () => {
      sweep(document.documentElement);
      startObserving();
    };

    if (document.documentElement) boot();
    document.addEventListener("DOMContentLoaded", boot, { once: true });
    // Amazon lazy-loads the panel well after load; keep checking briefly.
    let checks = 0;
    const interval = setInterval(() => {
      checks += 1;
      sweep(document.documentElement);
      if (checks >= 20) clearInterval(interval); // ~20s of coverage
    }, 1000);
  }

  init();
})();
