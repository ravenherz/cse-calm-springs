(function (root) {
  function deepEq(a, b) {
    if (a === b) return true;
    if (a == null || b == null) return a === b;
    if (typeof a !== typeof b) return false;
    if (Array.isArray(a)) {
      if (!Array.isArray(b) || a.length !== b.length) return false;
      for (let i = 0; i < a.length; i++) if (!deepEq(a[i], b[i])) return false;
      return true;
    }
    if (typeof a === 'object') {
      const ka = Object.keys(a).sort();
      const kb = Object.keys(b).sort();
      if (!deepEq(ka, kb)) return false;
      for (let i = 0; i < ka.length; i++) if (!deepEq(a[ka[i]], b[ka[i]])) return false;
      return true;
    }
    return false;
  }

  function format(v) {
    try { return JSON.stringify(v); } catch (e) { return String(v); }
  }

  function chordSymbol(r) {
    if (!r || !r.matched) return null;
    return r.rootName + (r.suffix || '') + (r.slash ? '/' + r.bassName : '');
  }

  function pcsToMidis(pcs, octaveBase) {
    const base = octaveBase != null ? octaveBase : 60;
    return pcs.map((pc, i) => base + ((pc - pcs[0] + 12) % 12) + (i === 0 ? 0 : 0));
  }

  function midisFromPcsLowToHigh(pcs) {
    let last = -1;
    return pcs.map((pc) => {
      let m = 48 + ((pc % 12) + 12) % 12;
      while (m <= last) m += 12;
      last = m;
      return m;
    });
  }

  function runCase(c, M) {
    const out = { name: c.name, bucket: c.bucket || 'lock', ok: false, detail: '' };
    try {
      const got = c.run(M, {
        deepEq: deepEq,
        chordSymbol: chordSymbol,
        midisFromPcsLowToHigh: midisFromPcsLowToHigh,
        pcsToMidis: pcsToMidis,
        eq: function (a, b, msg) {
          if (!deepEq(a, b)) throw new Error((msg || 'assert') + ': expected ' + format(b) + ', got ' + format(a));
        }
      });
      if (got === false) throw new Error('returned false');
      out.ok = true;
      out.detail = c.passNote || 'ok';
    } catch (e) {
      out.ok = false;
      out.detail = e.message || String(e);
    }
    return out;
  }

  function run(suites) {
    const M = root.Music;
    if (!M || typeof M.analyzeChord !== 'function') {
      throw new Error('Music + chords.js required');
    }
    const results = [];
    suites.forEach((suite) => {
      (suite.cases || []).forEach((c) => {
        const r = runCase(c, M);
        r.suite = suite.name;
        results.push(r);
      });
    });

    let lockPass = 0, lockFail = 0, knownPass = 0, knownFail = 0;
    results.forEach((r) => {
      if (r.bucket === 'known') {
        if (r.ok) knownPass++; else knownFail++;
      } else {
        if (r.ok) lockPass++; else lockFail++;
      }
    });

    const summary = {
      lockPass: lockPass,
      lockFail: lockFail,
      knownPass: knownPass,
      knownFail: knownFail,
      strictOk: lockFail === 0
    };

    const rootEl = document.getElementById('results');
    const sumEl = document.getElementById('summary');
    if (sumEl) {
      sumEl.className = 'summary ' + (summary.strictOk ? 'ok' : 'bad');
      sumEl.innerHTML =
        '<strong>Strict (lock):</strong> ' + lockPass + ' passed' +
        (lockFail ? ', <strong>' + lockFail + ' failed</strong>' : '') +
        ' &nbsp;·&nbsp; <strong>Known debt:</strong> ' + knownFail + ' still open' +
        (knownPass ? ', ' + knownPass + ' cleared' : '') +
        (summary.strictOk
          ? ' <span class="badge ok">lock green</span>'
          : ' <span class="badge bad">lock broken</span>');
    }

    if (rootEl) {
      rootEl.innerHTML = '';
      let curSuite = '';
      results.forEach((r) => {
        if (r.suite !== curSuite) {
          curSuite = r.suite;
          const h = document.createElement('h2');
          h.textContent = curSuite;
          rootEl.appendChild(h);
        }
        const row = document.createElement('div');
        const kind = r.bucket === 'known' ? (r.ok ? 'known-cleared' : 'known') : (r.ok ? 'pass' : 'fail');
        row.className = 'case ' + kind;
        const tag = r.bucket === 'known' ? (r.ok ? 'CLEARED' : 'KNOWN') : (r.ok ? 'PASS' : 'FAIL');
        row.innerHTML =
          '<span class="tag">' + tag + '</span>' +
          '<span class="name">' + r.name + '</span>' +
          '<span class="detail">' + r.detail.replace(/</g, '&lt;') + '</span>';
        rootEl.appendChild(row);
      });
    }

    return { summary: summary, results: results };
  }

  root.TheoryTests = {
    run: run,
    deepEq: deepEq,
    chordSymbol: chordSymbol,
    midisFromPcsLowToHigh: midisFromPcsLowToHigh
  };
})(typeof self !== 'undefined' ? self : this);
