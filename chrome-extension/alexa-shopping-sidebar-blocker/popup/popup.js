const STORAGE_KEY = "alexaShoppingBlockerEnabled";
const AMAZON_URL_PATTERN = /^https?:\/\/([^/]+\.)?amazon\.[a-z.]+\//i;

const toggle = document.getElementById("enabled-toggle");
const statusEl = document.getElementById("status");
const notAmazonEl = document.getElementById("not-amazon");

function setStatusText(text) {
  statusEl.textContent = text;
}

async function getActiveAmazonTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (tab && tab.url && AMAZON_URL_PATTERN.test(tab.url)) return tab;
  return null;
}

async function refreshLiveStatus() {
  const tab = await getActiveAmazonTab();
  if (!tab) {
    notAmazonEl.classList.remove("hidden");
    return;
  }
  notAmazonEl.classList.add("hidden");
  try {
    const response = await chrome.tabs.sendMessage(tab.id, {
      type: "asb-get-status",
    });
    if (response) {
      setStatusText(
        response.enabled
          ? `Blocking active — ${response.hiddenCount} element(s) hidden on this page.`
          : "Blocking is off for this tab."
      );
    }
  } catch (e) {
    // Content script may not be injected yet (e.g. page still loading).
    setStatusText("Reload the page to see live stats.");
  }
}

async function init() {
  const items = await chrome.storage.sync.get({ [STORAGE_KEY]: true });
  toggle.checked = items[STORAGE_KEY] !== false;
  await refreshLiveStatus();
}

toggle.addEventListener("change", async () => {
  const enabled = toggle.checked;
  await chrome.storage.sync.set({ [STORAGE_KEY]: enabled });
  setStatusText(enabled ? "Enabling…" : "Disabling…");

  const amazonTabs = await chrome.tabs.query({ url: "*://*.amazon.*/*" });
  await Promise.all(
    amazonTabs.map((t) => chrome.tabs.reload(t.id).catch(() => {}))
  );

  setTimeout(refreshLiveStatus, 800);
});

init();
