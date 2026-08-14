# Fretboard Lab — Suggestions

Snapshot from codebase review (Aug 2026). **v1 backlog (sections 1–7) is cleared.** Baseline is strong: Explore layers (CAGED / quartal / NPS / Berklee), Chord Analyzer, Fit Scale, scale chords / diatonic 7ths, profiles + ZIP, en/ru i18n, themes, PWA shell, deep links, Drills, Theory syllabus, piano strip, keyboard Chord Analyzer, theory locks + headless runner.

Priorities: **P0** = high user friction · **P1** = clear win soon · **P2** = polish / debt.  
Effort: **S** hours · **M** days · **L** multi-day.

---

## Suggested order of attack

1. ~~**P0** — touch + responsive~~ → done.
2. ~~**P1 quick wins + structural peel**~~ → done (caches, debounce, a11y, tip i18n, regen, chord-ui / board-view / scale-details, incremental `#neck`, deep links).
3. **P1 theory honesty** — clear amber `known` fixtures (Fit Scale F#, quartal trichord, C7alt).
4. **P2 a11y leftovers** — modal focus trap; Explore/Theory neck keyboard (4.5 left those out).
5. **P2 quality / size** — NPS·Berklee·deeplink locks; peel more out of `app.js` (~2.3k).

---

## 8. Next wave (Open)

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 8.1 | **Clear amber theory debt** | Promote `known` → `lock`: F# Ionian Fit Scale spelling; C–F–Bb stays unmatched (not `Fsus4/C`); C7alt (no5) → `C7alt`. | P1 | M | `js/music.js`, `js/chords.js`, fixtures | Open |
| 8.2 | **Finish modal focus trap** | 4.2 moved focus in/out + labelled dialog; Tab still escapes the Profiles modal. | P2 | S | `js/profiles.js` | Open |
| 8.3 | **Explore / Theory neck keyboard** | 4.5 covers Analyzer (+ Drills build) only; Explore markers and Theory demos still pointer-first. | P2 | L | `js/board-view.js`, Explore / Theory | Open |
| 8.4 | **NPS / Berklee / deeplink lock fixtures** | 6.2 locked geom · CAGED · quartal; NPS/Berklee boxes and more deeplink edge cases still need locks. | P2 | M | `js/nps.js`, `berklee.js`, `deeplink.js`, fixtures | Open |
| 8.5 | **Further peel `app.js`** | After 2.1 still ~2276 lines (persistence, guitar chip, legend/tipbox, keybinds, `createModules`). Extract another factory slice when the next feature touches that area. | P2 | L | `js/app.js` | Open |

---

## 1. UX / product

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 1.1 | **Usable touch / narrow viewports** | Body min-width removed; fretboard scrolls in `.board-wrap`; header/tabs/profiles reflow at 960/720/480. | P0 | L | `css/style.css` | **Done** |
| 1.2 | **Touch equivalents for hover + right-click** | Placement ghost via `pointermove`/`pointerdown`; long-press (~480ms) opens chord note menu (pin / interval ghost). | P0 | M | `js/board-view.js` | **Done** |
| 1.3 | **Shareable deep links** | Hash overlay (`#r=0&f=major&m=3&l=caged`) on top of `localStorage`; profiles untouched. `js/deeplink.js` + boot/`replaceState` sync. | P1 | M | `js/deeplink.js`, `js/app.js` | **Done** |
| 1.4 | **Parent scale beyond church modes** | `Music.parentScale` now returns relative mode 0 for any `modes[]` family. | P1 | M | `js/music.js`, fixtures | **Done** |
| 1.5 | **Scale catalog search / jump** | Explore **Find scale** search jumps to any family/mode across categories (EN identity names included for RU UI). | P2 | M | Explore controls | **Done** |
| 1.6 | **CAGED honesty on non-6-string guitars** | Gate CAGED when `stringCount < 6`; qualify (`≈` / treble-six copy) when `> 6`. Layer picker + legend/tipbox i18n. | P2 | M | `js/app.js`, i18n | **Done** |
| 1.7 | **PWA / offline fonts** (if productizing) | Self-hosted Outfit + IBM Plex Mono (`css/fonts.css`, `fonts/*.woff2`); `manifest.webmanifest` + icons; `sw.js` shell cache. No Google Fonts CDN. | P2 | M | `index.html`, `fonts/`, `sw.js`, `manifest.webmanifest` | **Done** |
| 1.8 | **Piano mirror strip** | 4 octaves from lowest root at/above guitar open low; slider beside Piano title; scale colors + click-to-hear. No Chord Analyzer placement (see `docs/piano.md` B/C). | P2 | M | `js/piano-view.js` | **Done** |
| 1.9 | **Notes / string Explore layer** | Mutually exclusive with CAGED / quartal; one NPS box per scale degree (count 3·4; +12 repeats). | P2 | M | `js/nps.js`, Explore Layers | **Done** |
| 1.10 | **Berklee (7PS) Explore layer** | Mutually exclusive overlay; one 5-fret position per scale degree (+12 repeats). | P2 | M | `js/berklee.js`, Explore Layers | **Done** |

---

## 2. Architecture / maintainability

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 2.1 | **Split `app.js` by surface** | Was ~2884 lines. Extracted `js/chord-ui.js` (`FretChordUi`), `js/scale-details.js` (`FretScaleDetails`) and `js/board-view.js` (`FretBoardView`) as `create(deps)` factories; shared view state in one `ui` object. Orchestrator still large — see **8.5**. | P1 | L | `js/app.js`, `js/chord-ui.js`, `js/scale-details.js`, `js/board-view.js` | **Done** |
| 2.2 | **i18n regen as a checked script** | `tools/regen-i18n.ps1` regenerates `en.js`/`ru.js` from JSON. | P1 | S | `tools/regen-i18n.ps1` | **Done** |
| 2.3 | **Deduplicate `allScaleKeys`** | Single `Music.allScaleKeys()` (flatten `SCALE_CATS`); used by Explore fallbacks and `chordScales`. | P2 | S | `js/music.js`, `js/app.js`, `js/chords.js` | **Done** |
| 2.4 | **Retire dead board-tuning path** | `tuningId` / `customPcs` no longer in state/serialize (7.5). `Music.TUNINGS` remains for std6 fallback labels. | P2 | M | `js/app.js` | **Done** (serialize path) |
| 2.5 | **Remove or use `fretFraction`** | Removed unused stub from `music.js` (board stays linear via `FretGeom`). | P2 | S | `js/music.js` | **Done** |

---

## 3. Performance

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 3.1 | **Stop full `#neck` rebuild every `render()`** | Layered `#neck-rails` / `#neck-music` / `#neck-chords` with input fingerprints; rails stay put across label/scale toggles. Resize invalidates music when quartal/root-lines need width. | P1 | L | `js/board-view.js` | **Done** |
| 3.2 | **Cache `chordScales` per chord render** | `getChordScales` memos by root/intervals/key/names. | P1 | S | `js/app.js` | **Done** |
| 3.3 | **Debounce `localStorage` writes** | `render()` → `scheduleSave()` (~220ms); sync `saveState()` kept for profile/critical paths; flushed on `beforeunload`. | P1 | S | `js/app.js` | **Done** |
| 3.4 | **Cap interval-ghost DOM** | Interval ghosts only in a padded fret window around the voicing (skip occupied cells); no full string×fret flood. | P2 | M | `js/board-view.js` | **Done** |

---

## 4. Accessibility

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 4.1 | **Tab semantics** | `role="tablist"` / `role="tab"` / `aria-selected` / roving tabindex + arrow keys. | P1 | S | `index.html`, `applyTabUI` | **Done** |
| 4.2 | **Modal focus management** | Focus moves into dialog; `aria-labelledby` from title; restore focus on close. Focus trap → **8.2**. | P1 | M | `js/profiles.js` | **Done** (partial) |
| 4.3 | **Visible keyboard focus** | Shared `:focus-visible` gold rings for tabs/controls. | P1 | S | `css/style.css` | **Done** |
| 4.4 | **Play controls as real `<button>`s** | CAGED/NPS/Berklee `.box-play`, Analyzer shape ▶, and scale-details ▶ are `<button type="button">` (scale chip split into body + play to avoid nested buttons). | P2 | S | `js/board-view.js`, `chord-ui.js`, `scale-details.js` | **Done** |
| 4.5 | **Fretboard without pointer** | String/Fret Place/Remove controls, focusable `#neck` cursor (arrows/Enter/Delete), and live chord announce in Chord Analyzer (+ Drills build). Explore/Theory → **8.3**. | P2 | L | Chord Analyzer | **Done** |

---

## 5. i18n / content

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 5.1 | **Localize within-octave tip interval names** | `tip.qualityNames[]` + `Music.qualityFull` reads `FretI18n.raw`. | P1 | M | `js/music.js`, `i18n/` | **Done** |
| 5.2 | **i18n regen script** | Same as 2.2. | P1 | S | `tools/regen-i18n.ps1` | **Done** |
| 5.3 | **Remove orphan `modeInfo.Phrygian Major`** | Dropped; keep `Phrygian Dominant`. | P2 | S | `i18n/` | **Done** |
| 5.4 | **Hardcoded string colors vs theme tokens** | `--string-bass`/`--string-treble` + `color-mix` via `--string-t`; remaining `#111`/`#fff` → theme tokens. | P2 | S | `css/dark.css`, `light.css`, `style.css`, `js/board-view.js` | **Done** |

---

## 6. Testing / quality

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 6.1 | **Use the empty `known` bucket** | Amber debt filed (clear via **8.1**): F# Ionian Fit Scale; quartal C–F–Bb unmatched; C7alt (no5). | P1 | S | `js/tests/fixtures.js` | **Done** |
| 6.2 | **Lock fixtures for geometry / CAGED / quartal** | Pure-module locks for `FretGeom` / `FretCaged` / `FretQuartal` in `test.html`. | P1 | M | `js/tests/fixtures.js`, `test.html` | **Done** |
| 6.3 | **Chord-scale regression fixtures** | Lock: Cmaj7 → Ionian+Lydian; Dm7 → Dorian. | P1 | S | `js/tests/fixtures.js` | **Done** |
| 6.4 | **Headless / CI entry for theory tests** | `tools/run-theory-tests.ps1` — brace balance + Chrome/Edge headless `data-strict-ok`. | P2 | M | `tools/run-theory-tests.ps1` | **Done** |
| 6.5 | **i18n key parity check** | Lock: every `ru` leaf path exists in `en` (`FretI18n.vocab`). | P2 | S | fixtures, `js/i18n.js` | **Done** |

---

## 7. Bugs / edge cases / tech debt

| # | Suggestion | Why | Pri | Effort | Where | Status |
|---|------------|-----|-----|--------|-------|--------|
| 7.1 | **BPM max mismatch** | Clamp aligned to HTML/`context` **40–300**. | P1 | S | `js/app.js` | **Done** |
| 7.2 | **Top-level `fretCount` whitelist vs profiles** | Accept any fret count **12–24**. | P1 | S | `sanitizeState` | **Done** |
| 7.3 | **Orphan chord notes when string count drops** | `pruneChordNotesToBoard()` runs from `syncBoardSource()` each render. | P2 | S | `js/app.js` | **Done** |
| 7.4 | **Profiles drag is mouse-oriented** | Reorder + photo crop use Pointer Events + `setPointerCapture` / `elementFromPoint`. | P2 | M | `js/profiles.js` | **Done** |
| 7.5 | **Legacy state fields still persisted** | Dropped `tuningId` / `customPcs` / `cagedKey` from live state + serialize; load still accepts legacy `cagedKey` → `scaleKey`. | P2 | M | `serializeState` / `sanitizeState` | **Done** |
| 7.6 | **`alert()` on storage full** | Uses `showImportStatus(..., false)` in the Profiles header status line. | P2 | S | `js/profiles.js` | **Done** |

---

## Module sizes (JS)

| File | ~Lines | Role |
|------|--------|------|
| `app.js` | 2276 | Orchestrator / state / controls |
| `board-view.js` | 1259 | Fretboard draw + placement |
| `profiles.js` | 972 | Guitar library |
| `chord-ui.js` | 898 | Chord Analyzer result UI |
| `theory.js` | 667 | Theory syllabus |
| `drills.js` | 598 | Guided drills |
| `music.js` | 435 | Theory catalog |
| `scale-details.js` | 374 | Scale chords panel |
| `chords.js` | 338 | Chord engine |
| `caged.js` | 246 | CAGED boxes |
| `audio.js` | 216 | Synth |
| `quartal.js` | 194 | Quartal voicings |
| `deeplink.js` | 192 | Hash deep links |
| `piano-view.js` | 174 | Piano strip |
| `nps.js` | 167 | Notes-per-string |
| `berklee.js` | 156 | Berklee 7PS |
| `i18n.js` | 151 | Vocab runtime |
| `geometry.js` | 51 | Board math |

---

## Out of scope / already solid

Do not treat as missing: Explore layers, quartal, Chord Analyzer proposals + pin + ghost + keyboard place, Fit Scale (engine gaps tracked in **8.1**), scale chords / diatonic 7ths + play sequence + climbing roots, spoiler persistence, themes, PWA shell, ZIP export/import, en/ru vocab subset lock, theory lock suite + `run-theory-tests.ps1`, deep links, Drills, Theory companion board, piano mirror (no Analyzer placement — `docs/piano.md`).
