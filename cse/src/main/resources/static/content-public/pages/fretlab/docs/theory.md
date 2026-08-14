# Musical theory content — product notes

Goal: turn Fretboard Lab from “theory *on* the neck” into something that also *teaches* theory — without turning the app into a textbook site.

---

## Status

**Theory tab (near-course)** — grouped syllabus · **22 articles** · sectioned prose · See also · **companion fretboard** · **inline figures** (miniNeck + staff on foundations/chords/harmony articles) · About · en/ru.

**Inline explainers** — related-mode chips + learn-more in `#scale-details`; clickable chord alterations; legend / tipbox → Theory.

**Guided drills** — find degree · build ii–V–I · name this chord · Display locked on Drills tab.

Still open: more drill types mapped from articles; later figure extras (key signatures / staff Phase 2).

---

## Catalogue

| Section | Articles |
|---------|----------|
| Foundations | `pitchAndOctave`, `intervals`, `spellingAndEnharmonics` |
| Chords | `building`, `inversionsAndSlash`, `extensionsAndAlts`, `chords` |
| Harmony & progressions | `harmony`, `functionalHarmony`, `cadencesAndTurnarounds`, `secondaryDominants`, `minorHarmony`, `voiceLeading` |
| Scales & modes | `modes`, `melodicMinor`, `harmonicMinor`, `pentatonicAndBlues`, `symmetricScales`, `bebopAndChromatic` |
| On the guitar | `systems`, `quartalHarmony`, `readingTheNeck` |

Copy: `i18n/en.json` / `ru.json` → `theory.sections.*`, `theory.articles.*` (body as `{h,p[]}` sections). Structure: [`js/theory.js`](../js/theory.js) `SECTIONS` / `ARTICLES`.

---

## What’s already there

| Surface | Theory content |
|---------|----------------|
| Explore markers | Note names, chromatic Romans, interval qualities; Fit Scale spelling |
| `#scale-details` | Scale wheel, formula / steps, mood + characteristic notes, **related-mode chips**, parent scale, diatonic chords, **Learn more** → Theory |
| Chord Analyzer | Lead-sheet symbols; **alteration / paren hooks**; scale proposals; **`chordDiagramSvg`** applicature chips |
| Legend / tipbox | Layer hints + **Theory article shortcuts** |
| Layers | CAGED, quartal, N-notes/string, Berklee 7PS |
| **Theory tab** | Grouped syllabus + CTAs + **companion full board** · About at catalogue foot |
| **Drills tab** | Interactive prompts on the neck (`js/drills.js`) |

---

## Illustrative materials

The companion board is the interactive “now playing” scene. Inline figures are freeze-frames inside prose — compare side-by-side, captioned, optionally syncing the companion board via `demoCta`.

### Shipped

- Shared [`miniNeckSvg`](../js/chord-ui.js) (degree / name labels); Analyzer `chordDiagramSvg` delegates to it.
- Shared [`staffSvg`](../js/chord-ui.js) — minimal treble staff (`type: "staff"`); notes as `{ name, octave? }`, layouts `harmonic` | `melodic`.
- Lightweight strips: `type: "formula"` and `type: "romans"` (token chips; optional `demoCta`).
- Article `body` blocks may include `figures[]`; [`theory.js`](../js/theory.js) renders `.theory-figure` with optional ▶ play; click applies the matching CTA lesson.
- **miniNeck content:** `building`, `inversionsAndSlash`, `extensionsAndAlts`, `chords`, `voiceLeading`, `quartalHarmony`, `readingTheNeck`, `harmony` (ii–V–I), (+ interval dyads on `intervals`).
- **staff content:** `intervals`, `spellingAndEnharmonics`, triad stacks on `building`.
- **strips:** formulas on `building`; Roman `ii–V–I` on `harmony`.
- Captions en+ru; aria via `theory.figureAria` / `theory.figurePlay`.

**Embed shapes (live)**

```json
{
  "type": "miniNeck",
  "caption": "C · major",
  "chordPcs": [0, 4, 7],
  "chordRoot": 0,
  "labels": "degrees",
  "demoCta": 0
}
```

```json
{
  "type": "staff",
  "caption": "C–E · M3",
  "notes": [{ "name": "C" }, { "name": "E" }],
  "layout": "harmonic",
  "demoCta": 2
}
```

```json
{
  "type": "formula",
  "caption": "Major · 1–3–5",
  "tokens": ["1", "3", "5"],
  "demoCta": 0
}
```

```json
{
  "type": "romans",
  "caption": "ii–V–I in C",
  "tokens": ["ii", "V", "I"],
  "demoCta": 1
}
```

Captions live in the locale article body (en source of truth; ru override). Music glyphs stay language-neutral. Note names in staff figures use ASCII (`Eb`, `F#`); the renderer maps ♭/♯ if present.

### Design principles

- Prefer **reusing engine data** (PCs, midis, `placeChordPcs`, Fit Scale / `parseSpell`) over hand-drawn assets.
- Zero-build / SVG-first; any notation library must stay optional.
- Do **not** mount a second live `board-view` inside the article.
- Active figure highlight when `demoCta` matches the last applied demo.

### Next steps (figures)

1. ~~Staff SVG for `intervals` / `spellingAndEnharmonics`~~
2. ~~More mini necks on chord / harmony articles~~
3. ~~Optional polish — `quartalHarmony` / `readingTheNeck` miniNecks; staff on `building`; formula / Roman strips~~
4. **Later / optional** — key-signature callouts; more staff stacks; staff Phase 2 only if rhythm / multi-voice needs a library.

Enharmonic policy on staff: figures currently use **explicit** `name` spellings in JSON (good for teaching F♯ vs G♭). Fit Scale auto-spell remains available via Explore CTAs on the companion board.

### How this coexists with the companion board

```text
Article section
  ├── prose
  ├── inline figures (miniNeck / staff / …)  ← compare, freeze, caption
  └── CTA row → applyTheoryDemo / Drills     ← one live scene below
Companion #board-wrap (+ piano)
```

---

## Next increments

**Figures**

1. ~~Staff SVG — `intervals` + `spellingAndEnharmonics`~~
2. ~~More mini necks — chord/harmony (+ `quartalHarmony`, `readingTheNeck`)~~
3. ~~Formula / Roman strips; staff on `building`~~
4. Later: key-signature callouts; staff Phase 2 only if needed

**Elsewhere**

- More alteration → mode mappings; hover previews; alt hooks → Theory article id  
- Cross-link from Theory back to the live scale card section  
- Shareable deep links (`#r=&f=&m=&l=` / theory article / chord pcs) — see `js/deeplink.js`  
- Drill key picker / more cadences  

---

## Out of scope (for now)

- Full online course / accounts / progress sync  
- Figured bass or Nashville numbers as primary grammar  
- Sampled piano pedagogy tracks  
- One encyclopedia page per every Explore scale family  
- Full engraving suite (tuplets, multi-staff scores, classical voice-leading worksheets)  
- Multiple simultaneous live `board-view` instances inside one article  
