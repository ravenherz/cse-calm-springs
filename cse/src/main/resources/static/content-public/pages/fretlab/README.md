# Fretboard Lab

Interactive guitar fretboard for exploring scales, shapes, and chords — with sound, your own guitar profiles, and English / Russian UI.

**Live:** https://stage.ravenherz.com/rhz-we/static-pages/fretlab/

Open `index.html` in a browser (no build step, no install). Settings and guitars persist in the browser; you can also export / import an `.flstate` backup from the Profiles tab (ZIP under the hood — rename to `.zip` to inspect). On phones and tablets the fretboard scrolls sideways; Chord Analyzer supports tap-to-place and long-press note menus.

---

## What it is

A small static web app for guitarists and theory-curious players. You pick a guitar (or use the built-in Standard), then work on a real fretted neck with note names, degrees, intervals, and optional plucked-string sound.

It started as a boredom experiment and grew into a practical practice / analysis tool.

---

## Five workspaces

### Explore

See any scale from a large catalog (church modes, melodic / harmonic minors, pentatonics, bebop, world scales, and more) on the neck.

- **CAGED shapes** — classic box outlines, root lines, and play-per-box
- **Quartal chords** — stacked-fourth voicings along the scale
- **Notes / string** — one ascending N-notes-per-string position per scale degree (3 · 4; optional +12 repeats)
- **Berklee (7PS)** — one 5-fret position window per scale degree (optional +12 repeats)
- Scale details: mood / characteristic notes, parent scale (church modes), diatonic **triads** and **7th chords** with diagrams, per-chord play, and **play sequence** (chords climb in pitch so the progression doesn’t drop an octave)

### Chord Analyzer

Click notes onto the neck; the app names the chord (jazz / lead-sheet style), offers alternative readings, alternate fingerings, and scales that fit. Pin a root, preview a proposed scale as a soft ghost, and arpeggiate or strike the chord.

### Drills

Guided practice on the neck: find a scale degree, build a C major ii–V–I, or name a placed chord from multiple choice.

### Theory

Short lessons (intervals, modes, chord symbols, practice systems, ii–V–I) with buttons that jump into Explore or Chord Analyzer with the matching root / scale / layer. **About** (this README) lives at the bottom of the Theory catalogue — embedded as `js/readme.js` (regenerate with `tools/regen-readme.ps1`).

### Profiles

Save guitars: tuning, frets, scale length, construction / role, photo, description. One active guitar drives the fretboard everywhere else. Export and import your whole lab state as an `.flstate` file.

---

## Everyday niceties

- **Fit Scale** note spelling (unique staff letters when possible) — or force sharps / flats
- Piano mirror under the neck (4 octaves from the scale root; click to hear)
- Dark / light theme
- Sound on/off, BPM, and groove for arpeggios and chord sequences
- Theory self-check: open `test.html`, or run `.\tools\run-theory-tests.ps1` (Chrome/Edge headless)

---

## For developers

Project conventions, module map, and theory rules live in [`context.md`](context.md). Other notes live under [`docs/`](docs/):

- [`docs/suggestions.md`](docs/suggestions.md) — improvement backlog  
- [`docs/theory.md`](docs/theory.md) — musical theory content directions  
- [`docs/multi-platform-deployment.md`](docs/multi-platform-deployment.md) — deploy / PWA notes  
- [`docs/piano.md`](docs/piano.md) — piano keyboard feasibility  

