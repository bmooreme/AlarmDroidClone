# Alexa for Shopping Blocker

A small Chrome extension (Manifest V3) that stops Amazon's "Alexa for
Shopping" pop-in sidebar (the panel formerly branded "Rufus") from opening
while you browse or search on amazon.com and other Amazon storefronts.

## How it works

Amazon renames and re-classes this panel fairly often, so this extension
doesn't rely on one fixed selector:

1. **`blocker.css`** hides a list of known ids/classes/attributes
   immediately at `document_start`, before the page has a chance to paint
   the panel.
2. **`blocker.js`** runs a heuristic sweep of the page for any element
   whose `id`, `class`, `aria-label`, `data-testid`, or iframe `src`/`title`
   mentions the panel by name (`rufus` or `alexa` + `shop`), and hides it.
3. A `MutationObserver` re-runs that sweep whenever Amazon injects new DOM,
   so panels that load after the initial page render are still caught.
4. It clears the `localStorage`/`sessionStorage` keys Amazon uses to decide
   whether to auto-open the panel, so it doesn't spring back open on your
   next search in the same tab.
5. It also detects Amazon's client-side (pushState) navigation between
   search results/product pages and re-sweeps after each one.

## Install (unpacked, for local/manual use)

1. Open `chrome://extensions` in Chrome (or any Chromium browser).
2. Turn on **Developer mode** (top-right toggle).
3. Click **Load unpacked** and select this folder
   (`chrome-extension/alexa-shopping-sidebar-blocker`).
4. Visit amazon.com and search for a product — the sidebar should no longer
   pop in.

## Usage

Click the extension icon to toggle blocking on/off. Toggling it reloads any
open Amazon tabs so the change takes effect immediately. The popup also
shows how many matching elements were hidden on the active Amazon tab.

## Notes / limitations

- Amazon can change the panel's markup at any time (it has already been
  renamed once, from "Rufus" to "Alexa for Shopping"). If the sidebar
  starts appearing again after an Amazon update, the heuristic patterns in
  `blocker.js` (`NAME_PATTERN`) are the first place to update.
- This extension only touches Amazon's own DOM/storage; it does not block
  network requests or modify your Amazon account/Alexa settings.
