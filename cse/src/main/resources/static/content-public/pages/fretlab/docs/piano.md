# Piano keyboard — feasibility notes

Branch: `piano`. Goal: how hard is a piano keyboard in Fretboard Lab, and what would “good” mean?

---

## Short answer

**A. Mirror strip — shipped.** Four octaves from the Explore root under `#board-wrap`; scale coloring + click-to-hear; no Chord Analyzer placement.  
**A full second instrument surface that matches Chord Analyzer (place notes, pin root, ghosts) is large (≈1–2 weeks),** mostly because the fretboard UI is string/fret-shaped and CAGED/quartal do not map to piano.

Theory + audio are already pitch-class / MIDI friendly. Geometry, markers, boxes, and placement are not.

---

## What we already have that helps

| Asset | Why it helps |
|-------|----------------|
| `Music.scaleOrdered` / `scalePcs` / `names()` / Fit Scale | Paint in-scale vs out-of-scale keys without new theory |
| `M.analyzeChord(midis, …)` | Chord Analyzer can stay MIDI-based; piano placement = MIDI list |
| `FretAudio.playMidi` / `playSimultaneous` / `playSequence` | Same pluck synth works for piano keys (timbre is guitar-like, but fine for v1) |
| Shared Explore state (`scaleKey`, family, mode) | Keyboard follows the same root/scale as the neck |
| Module pattern (`create(deps)`) | `js/piano-view.js` → `FretPianoView` |
| Degree / interval / tip helpers | Hover tip: name, degree, interval |

---

## What fights a piano

1. **Fretboard geometry is not pitch geometry** — CAGED/quartal stay guitar-only.
2. **Markers are `(string, fret)`** — Chord Analyzer placement on keys needs a PC/MIDI source of truth (epic C).
3. **Profiles / tuning** — piano range is independent of guitar midis.
4. **Audio identity** — Karplus–Strong is fine for the lab; sampled piano is out of scope.
5. **Layout** — v1 is 49 keys (4 octaves from root), not 88.

---

## Product shapes

### A. Mirror strip — **Done**

- Under `#fretboard` inside `#board-wrap` (hides with Profiles).
- 49 keys: lowest root at/above active guitar open low … `+48`.
- Toggle: slider beside Piano title (`state.showPiano`, default on; head stays when collapsed).
- Highlight: root / highlight-degree gold, scale blue, `fullChart` plain, else dim `.out`.
- Click / tap → `playMidi`; hover tip (note, degree, interval).
- Does **not** place Chord Analyzer notes.
- Files: `js/piano-view.js`, `#piano` in `index.html`, `.piano*` / `.switch*` in `css/style.css`, `controls.piano.*` i18n.

### B. Linked highlight with the neck

- Same as A, plus: hovering/playing a neck marker briefly lights matching PC on the piano (and maybe reverse).
- Needs a small event/callback bus from `board-view` ↔ `piano-view`.
- Effort: **+0.5–1 day** on top of A.

### C. Chord Analyzer on piano

- Click keys to add/remove chord tones; sync with neck.
- Effort: **L**; redesign toward PC/MIDI as analysis source first.

### D. Piano-roll / sequencer

- Time axis / groove recording — different product. **XL**; don’t bundle with A.

---

## Architecture (A)

```
js/piano-view.js   FretPianoView.create(deps)
  API:  renderPiano(), bindPianoEvents()
index.html         #piano under #fretboard in #board-wrap
css/style.css      .piano / .piano-key.white|black|root|scale|plain|out
i18n               controls.piano.*
app.js             createModules + render() → renderPiano(); hide via #board-wrap
```

No changes to `music.js` / `chords.js` for A.

---

## Decisions taken for A

1. Toggle via slider beside Piano title (`showPiano`, default on; head remains when off).
2. Range: 4 octaves from lowest scale-root at/above the active guitar’s lowest open string.
3. Both Explore and Chord Analyzer (mirror of Explore scale state).
4. Keep guitar KS timbre.
5. No computer-keyboard mapping yet.
