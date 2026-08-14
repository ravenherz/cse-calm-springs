# Fretboard Lab — Multi-platform deployment

How this app can ship on **Web**, **Windows**, **macOS**, **iOS**, and **Android** without rewriting the theory/UI stack.

## What we have today

Fretboard Lab is a **zero-build static app**: HTML + CSS + vanilla JS modules, no bundler, no framework, no `package.json`. It already:

- Runs from `file://` or any static HTTP host (and is also embeddable under Spring as `content-public/pages/fretlab`)
- Persists UI state / profiles / theme / language in **`localStorage`**
- Plays notes via **Web Audio API** (`AudioContext`)
- **Self-hosts** Outfit / IBM Plex Mono (`css/fonts.css` + `fonts/*.woff2`; OFL in `fonts/OFL.txt`)
- Ships a **web app manifest** + icons and an optional **service worker** (`sw.js`) for shell offline cache
- Imports/exports guitar profiles as ZIP in the browser

That shape is a good fit for **one web codebase → many shells**. Do not fork separate Swift/Kotlin/C# UIs unless a platform forces it.

---

## Recommended strategy: one web core, thin native shells

```
                    ┌─────────────────────────┐
                    │  Fretboard Lab (static) │
                    │  index.html + css + js  │
                    └───────────┬─────────────┘
                                │
       ┌────────────┬───────────┼───────────┬────────────┐
       ▼            ▼           ▼           ▼            ▼
     Web         Windows      macOS        iOS        Android
   (CDN/host)   (Tauri/       (Tauri/   (Capacitor) (Capacitor)
                 Electron)     Electron)
```

**Source of truth:** the current `fretlab/` tree (or a copy published as a versioned static artifact).

**Shells only own:** window/chrome, icons, store metadata, filesystem/share bridges if needed, offline packaging of assets (fonts!), and OS permissions for audio.

**Avoid:** React Native / Flutter rewrites; they buy little for a DOM-heavy fretboard and cost a full second UI.

---

## Platform notes

### 1. Web (baseline — ship first and always)

**Options**

| Approach | Fit |
|----------|-----|
| Static host (GitHub Pages, Cloudflare Pages, S3+CloudFront, Netlify) | Best for a public product URL |
| Keep embedding in Calm Springs Engine | Best for the existing product site |
| Both | Same build artifact; Spring serves one copy, CDN serves another |

**Work to make it “app-like”**

- Add a **web app manifest** + icons (PWA install on desktop/Android). ✅ `manifest.webmanifest`, `icons/icon-192.png` / `icon-512.png`
- Optional **service worker** for offline (cache `index.html`, CSS, JS, favicon, self-hosted fonts). ✅ `sw.js`
- **Self-host Outfit / IBM Plex Mono** so offline and store shells don’t depend on Google. ✅ `css/fonts.css` + `fonts/`
- Ensure HTTPS (required for PWA install, reliable AudioContext resume policies).
- Keep relative asset paths (already friendly to nested `/pages/fretlab/` mounts).

**Limits**

- iOS Safari PWA is weak (no real App Store presence, background/audio quirks).
- Desktop “install as app” is fine for power users, not a substitute for store listings.

**Verdict:** Keep Web as the canonical deploy. Every other platform wraps this same folder.

---

### 2. Windows standalone

**Preferred: Tauri 2**

- Ships a small native EXE + WebView2 (Edge) host.
- Loads the static `fretlab` dist (folder or embedded assets).
- Tiny runtime vs Electron; good for a tool-sized app.

**Alternative: Electron**

- Heavier (~100MB+), but familiar packaging (`electron-builder` → `.exe` / `.msi` / Store MSIX).
- Use only if you need Electron-specific APIs or team expertise.

**Alternative: PWA / Edge sidebar**

- Zero packaging; users “Install site as app”. Weak discovery, no classic installer.

**Desktop-specific polish**

- Window title, min size (fretboard is wide — set a sensible minimum width).
- Custom menu optional; most controls are already in-page.
- `localStorage` works inside the WebView; for backup/sync later, bridge ZIP export to real files via Tauri FS APIs.
- Code signing + SmartScreen: plan a certificate for public Windows installs.

**Verdict:** Tauri wrapping the static site is the best Windows path.

---

### 3. macOS standalone

**Same shell as Windows: Tauri 2** (or Electron if already chosen).

- Produce `.app` / `.dmg`; optionally Mac App Store via proper sandboxing.
- **Apple Developer Program** + notarization required for Gatekeeper-friendly distribution outside the Store.
- WebView is WKWebView — AudioContext and `localStorage` are fine; test autoplay/sound gesture rules.
- Retina: CSS already uses layout %, but smoke-test marker/SVG stroke scaling on HiDPI.

**Verdict:** One Tauri project targeting `windows` + `macos` from the same web assets.

---

### 4. Android

**Preferred: Capacitor**

- Creates an Android Studio project with a WebView loading the bundled static assets.
- Publish AAB to Play Store.
- Audio: unlock `AudioContext` on first tap (you already gate sound behind UI); verify Bluetooth audio if relevant.
- Safe areas / notches: extend existing viewport meta; test header + playback bar on small phones and landscape tablets (fretboard wants landscape).
- Back button: map to tab/modal dismiss if you add native back handling later.
- Offline: assets ship inside the APK; self-host fonts.

**Alternatives**

- TWA (Trusted Web Activity) around the hosted HTTPS app — thinner packaging, needs a live URL and Digital Asset Links.
- Pure PWA from Chrome — easy, weak store presence.

**Verdict:** Capacitor + landscape-first UX testing.

---

### 5. iOS

**Preferred: Capacitor** (same project as Android)

- Xcode project, WebView, App Store IPA.
- Requires Apple Developer Program, privacy strings, screenshots, review.
- **WKWebView audio:** must start/resume on a user gesture; your “sound on” / note click path should be enough — verify cold start.
- `localStorage` is fine but can be cleared under storage pressure; treat ZIP export as the durable backup story (or later iCloud Documents via a Capacitor plugin).
- PWA-from-Safari is **not** a substitute for App Store if you care about discovery.

**UI constraints**

- Fretboard density: prioritize **landscape** or a “compact controls” breakpoint.
- Hover-dependent tips (`#tip`) need touch equivalents (tap-to-pin tip, long-press) — already partly click-driven; audit hover-only paths before store submit.

**Verdict:** Capacitor iOS + Android from one mobile shell; don’t rely on iOS PWA alone.

---

## Cross-cutting work (do once for all platforms)

1. **Artifact pipeline**  
   - Treat `fretlab/` (or an `export/` copy) as the release payload.  
   - Optional: tiny script to regenerate `i18n/*.js`, copy fonts into `fonts/`, rewrite `index.html` font `<link>`s to local files, stamp a version string.

2. **Self-host fonts** ✅  
   - Removes CDN dependency for offline, Tauri, and Capacitor packages.

3. **PWA basics (Web + optional TWA)** ✅  
   - `manifest.webmanifest`, icons `192`/`512`, theme color, `display: standalone`, `sw.js`.

4. **Touch & narrow viewports**  
   - Landscape fretboard layout; larger hit targets; tip/context-menu without hover.

5. **Audio policy checklist**  
   - Resume `AudioContext` on first user gesture on every shell (especially iOS).

6. **Storage & backup**  
   - Document that profiles live in WebView storage; keep ZIP import/export as the cross-device transfer path.  
   - Later: optional cloud sync (out of scope for v1 shells).

7. **Single versioning**  
   - One app version in `context` / about UI / store listings; shells only bump native wrapper versions when the web core changes.

8. **Analytics / crash reporting (optional)**  
   - Prefer privacy-light or none for a theory tool; if needed, one web-side approach that works inside all WebViews.

---

## Suggested packaging matrix

| Target | Wrapper | Output | Store / channel |
|--------|---------|--------|-----------------|
| Web | none / static host | folder or zip | URL (and/or Spring embed) |
| Web installable | PWA | same + SW/manifest | Browser install |
| Windows | Tauri 2 | `.msi` / `.exe` | Site download; optional MS Store |
| macOS | Tauri 2 | `.dmg` / `.app` | Site download; optional Mac App Store |
| Android | Capacitor | `.aab` | Google Play |
| iOS | Capacitor | `.ipa` | App Store |

**Team tooling estimate (order-of-magnitude)**

- Harden web + fonts + PWA: small  
- Tauri Windows+macOS skeleton: medium  
- Capacitor iOS+Android + store assets: medium–large (mostly store/compliance/UI polish)

---

## Phased rollout

### Phase A — Web productized
- ~~Self-hosted fonts~~ · ~~PWA manifest + icons~~ · ~~optional SW (`sw.js`)~~
- Touch/landscape pass (done earlier).
- Publish stable public URL **and** keep Spring embedding if required.
- Still optional: version badge in About / footer.

### Phase B — Desktop
- Tauri project pointing at the Phase A artifact.
- Signed Windows + notarized macOS builds from CI.
- Smoke-test audio, localStorage, profile ZIP, language/theme.

### Phase C — Mobile stores
- Capacitor iOS/Android sharing the same artifact.
- Landscape default / responsive controls.
- Store listings, privacy policy, screenshots.
- Test audio unlock and storage survival across OS updates.

### Phase D — Hardening (optional)
- Native file picker for ZIP via plugins.
- Deeper offline guarantees.
- Tablet-optimized fretboard chrome.

---

## What not to do (for this codebase)

- Don’t rewrite in React Native/Flutter “for mobile.”
- Don’t maintain five copies of `music.js` / `app.js`.
- Don’t depend on Google Fonts or a live Spring server inside store binaries.
- Don’t assume `file://` quirks equal WebView quirks — always test the packaged WebView.

---

## Decision summary

| Question | Answer |
|----------|--------|
| One codebase? | Yes — current static Fretboard Lab |
| Web? | Static host + optional PWA; Spring embed remains valid |
| Windows / Mac? | **Tauri 2** shell (Electron only if needed) |
| iOS / Android? | **Capacitor** dual project |
| Shared prep? | Self-host fonts, touch/landscape, audio gesture, versioned static artifact |

This keeps theory, i18n, CAGED, quartal, and profiles identical everywhere, and limits native work to packaging and platform UX edges.
