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
        master.gain.value = 0.85;
        const lpMaster = audioCtx.createBiquadFilter();
        lpMaster.type = 'lowpass';
        lpMaster.frequency.value = 3000;
        lpMaster.Q.value = 0.707;
        const comp = audioCtx.createDynamicsCompressor();
        comp.threshold.value = -18;
        comp.knee.value = 18;
        comp.ratio.value = 3;
        comp.attack.value = 0.003;
        comp.release.value = 0.18;
        master.connect(lpMaster);
        lpMaster.connect(comp);
        comp.connect(audioCtx.destination);
      }
      return audioCtx;
    }

    function pluckBuffer(ac, freq, seconds) {
      const sr = ac.sampleRate;
      const N = Math.max(2, Math.round(sr / freq));
      const len = Math.max(N + 1, Math.floor(sr * seconds));
      const buf = ac.createBuffer(1, len, sr);
      const out = buf.getChannelData(0);
      const delay = new Float32Array(N);
      for (let i = 0; i < N; i++) {
        const env = Math.sin((Math.PI * i) / N);
        delay[i] = (Math.random() * 2 - 1) * (0.35 + 0.65 * env);
      }
      const stretch = freq < 120 ? 0.2 : freq < 250 ? 0.35 : 0.5;
      const feedback = Math.min(0.9975, 0.988 + (90 / freq) * 0.006);
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
      let peak = 0.0001;
      for (let i = 0; i < len; i++) {
        const a = Math.abs(out[i]);
        if (a > peak) peak = a;
      }
      const norm = 0.9 / peak;
      for (let i = 0; i < len; i++) out[i] *= norm;
      return buf;
    }

    function playMidi(m) {
      if (!deps.getSound()) return;
      try {
        const ac = ctx();
        const freq = deps.midiToFreq(m);
        const t = ac.currentTime;
        const dur = freq < 110 ? 2.4 : freq < 220 ? 1.9 : 1.45;
        const src = ac.createBufferSource();
        src.buffer = pluckBuffer(ac, freq, dur);

        const hp = ac.createBiquadFilter();
        hp.type = 'highpass';
        hp.frequency.value = Math.min(180, freq * 0.35);
        hp.Q.value = 0.5;

        const body = ac.createBiquadFilter();
        body.type = 'peaking';
        body.frequency.value = 220;
        body.Q.value = 0.8;
        body.gain.value = 3.5;

        const bright = ac.createBiquadFilter();
        bright.type = 'peaking';
        bright.frequency.value = Math.min(2800, freq * 4.5);
        bright.Q.value = 1.1;
        bright.gain.value = 2.2;

        const lp = ac.createBiquadFilter();
        lp.type = 'lowpass';
        lp.frequency.setValueAtTime(Math.min(5200, 1800 + freq * 3.2), t);
        lp.frequency.exponentialRampToValueAtTime(Math.max(420, freq * 1.8), t + dur * 0.85);
        lp.Q.value = 0.7;

        const g = ac.createGain();
        g.gain.setValueAtTime(0.0001, t);
        g.gain.exponentialRampToValueAtTime(0.55, t + 0.004);
        g.gain.exponentialRampToValueAtTime(0.22, t + 0.08);
        g.gain.exponentialRampToValueAtTime(0.0001, t + dur);

        src.connect(hp);
        hp.connect(body);
        body.connect(bright);
        bright.connect(lp);
        lp.connect(g);
        g.connect(master);
        src.start(t);
        src.stop(t + dur + 0.05);
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

    return {
      playMidi: playMidi,
      playSequence: playSequence
    };
  }

  root.FretAudio = { create: create };
})(typeof self !== 'undefined' ? self : this);
