(function (root) {
  function create(deps) {
    let audioCtx = null;
    let seqTimers = [];
    let master = null;

    function ctx() {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      if (audioCtx.state === 'suspended') audioCtx.resume();
      if (!master || master.context !== audioCtx) {
        master = audioCtx.createGain();
        master.gain.value = 0.82;
        const lpMaster = audioCtx.createBiquadFilter();
        lpMaster.type = 'lowpass';
        lpMaster.frequency.value = 4200;
        lpMaster.Q.value = 0.65;
        const comp = audioCtx.createDynamicsCompressor();
        comp.threshold.value = -16;
        comp.knee.value = 20;
        comp.ratio.value = 2.8;
        comp.attack.value = 0.004;
        comp.release.value = 0.22;
        master.connect(lpMaster);
        lpMaster.connect(comp);
        comp.connect(audioCtx.destination);
      }
      return audioCtx;
    }

    /** Buffer length — long enough for natural KS decay on all pitches (not truncated highs). */
    function bufferSeconds(freq) {
      // Mild pitch tilt only: lows need a bit more tail; highs still get ~2.1s of headroom.
      if (freq < 110) return 2.75;
      if (freq < 200) return 2.45;
      if (freq < 400) return 2.25;
      return 2.15;
    }

    /**
     * Karplus–Strong plucked string with pluck-position excitation and
     * frequency-aware damping. Sustain is mostly physical (feedback), not
     * a hard short envelope on treble notes.
     */
    function pluckBuffer(ac, freq, seconds) {
      const sr = ac.sampleRate;
      const N = Math.max(2, Math.round(sr / freq));
      const len = Math.max(N + 1, Math.floor(sr * seconds));
      const buf = ac.createBuffer(1, len, sr);
      const out = buf.getChannelData(0);
      const delay = new Float32Array(N);

      // Pluck ~1/5 along the string → richer odd/even mix than pure noise.
      const pluckAt = Math.max(1, Math.min(N - 2, Math.round(N * 0.22)));
      for (let i = 0; i < N; i++) {
        const tri = i < pluckAt ? i / pluckAt : (N - i) / (N - pluckAt);
        const noise = Math.random() * 2 - 1;
        delay[i] = (0.72 * tri + 0.28 * noise) * (0.55 + 0.45 * Math.sin((Math.PI * i) / N));
      }

      // Low damping (higher feedback) = longer ring. Highs damp a little faster,
      // like real wound/plain strings, without starving their envelope.
      const stretch = freq < 100 ? 0.18 : freq < 220 ? 0.28 : freq < 450 ? 0.38 : 0.48;
      const feedback = freq < 100 ? 0.9965
        : freq < 220 ? 0.9955
        : freq < 450 ? 0.9942
        : 0.9928;

      let ptr = 0;
      let prev = 0;
      for (let i = 0; i < len; i++) {
        const x = delay[ptr];
        const avg = (x + prev) * 0.5;
        const y = avg * (1 - stretch) + x * stretch;
        prev = x;
        delay[ptr] = y * feedback;
        ptr = (ptr + 1) % N;
        out[i] = y;
      }

      // Soft fade at the very end so leftover energy never clicks.
      const fade = Math.min(len, Math.floor(sr * 0.08));
      const fadeStart = len - fade;
      for (let i = fadeStart; i < len; i++) {
        out[i] *= (len - i) / fade;
      }

      let peak = 0.0001;
      for (let i = 0; i < len; i++) {
        const a = Math.abs(out[i]);
        if (a > peak) peak = a;
      }
      const norm = 0.88 / peak;
      for (let i = 0; i < len; i++) out[i] *= norm;
      return buf;
    }

    function playMidi(m, gainScale) {
      if (!deps.getSound()) return;
      try {
        const ac = ctx();
        const freq = deps.midiToFreq(m);
        const t = ac.currentTime;
        const dur = bufferSeconds(freq);
        const amp = (gainScale != null ? gainScale : 1) * 0.5;
        const src = ac.createBufferSource();
        src.buffer = pluckBuffer(ac, freq, dur);

        // Gentle high-pass — less rumble on low strings, barely touches highs.
        const hp = ac.createBiquadFilter();
        hp.type = 'highpass';
        hp.frequency.value = Math.min(90, Math.max(40, freq * 0.18));
        hp.Q.value = 0.5;

        // Soft body / cavity presence around 180–280 Hz.
        const body = ac.createBiquadFilter();
        body.type = 'peaking';
        body.frequency.value = 200;
        body.Q.value = 0.7;
        body.gain.value = freq < 250 ? 2.8 : 1.4;

        // Pick / attack sparkle — less on very high notes (already bright).
        const bright = ac.createBiquadFilter();
        bright.type = 'peaking';
        bright.frequency.value = Math.min(3200, Math.max(1200, freq * 3.2));
        bright.Q.value = 0.9;
        bright.gain.value = freq < 300 ? 2.4 : freq < 600 ? 1.6 : 0.8;

        // Tone darkens gradually; treble notes keep more air longer.
        const lp = ac.createBiquadFilter();
        lp.type = 'lowpass';
        const openTone = Math.min(5600, 2400 + freq * 2.6);
        const closedTone = Math.max(freq * 2.2, freq < 200 ? 500 : 900);
        lp.frequency.setValueAtTime(openTone, t);
        lp.frequency.exponentialRampToValueAtTime(closedTone, t + dur * 0.9);
        lp.Q.value = 0.55;

        // Envelope: quick attack, then let the string decay — don't choke highs early.
        const g = ac.createGain();
        g.gain.setValueAtTime(0.0001, t);
        g.gain.exponentialRampToValueAtTime(Math.max(0.001, amp), t + 0.005);
        g.gain.exponentialRampToValueAtTime(Math.max(0.001, amp * 0.62), t + 0.09);
        g.gain.exponentialRampToValueAtTime(0.0001, t + dur);

        src.connect(hp);
        hp.connect(body);
        body.connect(bright);
        bright.connect(lp);
        lp.connect(g);
        g.connect(master);
        src.start(t);
        src.stop(t + dur + 0.02);
      } catch (e) { /* audio unavailable */ }
    }

    function seqMark(sf, on) {
      const neck = deps.getNeck();
      neck.querySelectorAll('.marker[data-sf="' + sf + '"]').forEach((el) => el.classList.toggle('playing', on));
    }

    function orderNotes(notes, rootOverride) {
      const root = rootOverride != null ? rootOverride : deps.getRootPc();
      const up = notes.slice().sort((a, b) => a.m - b.m);
      const down = up.slice().reverse();
      const g = deps.getGroove();
      if (g === 'upsideDown') return down;
      if (g === 'bothFromDown') return up.concat(down.slice(1));
      if (g === 'bothFromUp') return down.concat(up.slice(1));
      if (g === 'lowestRoot' || g === 'upperRoot') {
        const base = g === 'lowestRoot' ? up : down;
        const idx = base.findIndex((n) => (((n.m % 12) + 12) % 12) === root);
        return idx > 0 ? base.slice(idx) : base;
      }
      return up;
    }

    function playSequence(notes, rootOverride) {
      if (!deps.getSound() || !notes.length) return;
      const gap = 60000 / deps.getBpm();
      seqTimers.forEach(clearTimeout);
      seqTimers = [];
      const neck = deps.getNeck();
      neck.querySelectorAll('.marker.playing').forEach((el) => el.classList.remove('playing'));
      const markMs = Math.min(380, gap * 0.75);
      orderNotes(notes, rootOverride).forEach((note, i) => {
        seqTimers.push(setTimeout(() => {
          playMidi(note.m);
          seqMark(note.s + ':' + note.f, true);
          seqTimers.push(setTimeout(() => seqMark(note.s + ':' + note.f, false), markMs));
        }, i * gap));
      });
    }

    /** Strike all MIDI notes at once (chord / block voicing). */
    function playSimultaneous(midiList, gainScale) {
      if (!deps.getSound() || !midiList || !midiList.length) return;
      const scale = gainScale != null ? gainScale : (1 / Math.sqrt(midiList.length));
      midiList.forEach((m) => playMidi(m, scale));
    }

    /**
     * Play a list of chords (each an array of MIDI notes) one after another.
     * gapMs defaults to ~1.5 beats at current BPM so each block can ring.
     * Optional onBlock(i) runs when chord i starts; onBlock(-1) clears / ends.
     */
    function playBlocks(chordMidisList, gapMs, onBlock) {
      if (!deps.getSound() || !chordMidisList || !chordMidisList.length) return;
      const gap = gapMs != null ? gapMs : Math.max(520, (90000 / Math.max(40, deps.getBpm())));
      seqTimers.forEach(clearTimeout);
      seqTimers = [];
      const neck = deps.getNeck();
      if (neck) neck.querySelectorAll('.marker.playing').forEach((el) => el.classList.remove('playing'));
      const items = [];
      chordMidisList.forEach((list) => {
        if (list && list.length) items.push(list);
      });
      if (!items.length) return;
      if (typeof onBlock === 'function') onBlock(-1);
      items.forEach((list, i) => {
        seqTimers.push(setTimeout(() => {
          if (typeof onBlock === 'function') onBlock(i);
          playSimultaneous(list);
          if (i === items.length - 1 && typeof onBlock === 'function') {
            seqTimers.push(setTimeout(() => onBlock(-1), Math.min(gap, 700)));
          }
        }, i * gap));
      });
    }

    return {
      playMidi: playMidi,
      playSequence: playSequence,
      playSimultaneous: playSimultaneous,
      playBlocks: playBlocks
    };
  }

  root.FretAudio = { create: create };
})(typeof self !== 'undefined' ? self : this);
