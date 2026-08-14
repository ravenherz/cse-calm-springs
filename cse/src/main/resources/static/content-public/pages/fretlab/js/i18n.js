(function (root) {
  let current = {};
  let fallback = {};
  let lang = 'en';
  const vocabs = {};

  function dig(obj, path) {
    if (!obj || !path) return undefined;
    const parts = path.split('.');
    let cur = obj;
    for (let i = 0; i < parts.length; i++) {
      if (cur == null || typeof cur !== 'object') return undefined;
      cur = cur[parts[i]];
    }
    return cur;
  }

  function resolve(path) {
    const v = dig(current, path);
    if (v !== undefined && v !== null) return v;
    return dig(fallback, path);
  }

  function interpolate(str, vars) {
    if (!vars || typeof str !== 'string') return str;
    return str.replace(/\{(\w+)\}/g, (_, k) => (vars[k] != null ? String(vars[k]) : '{' + k + '}'));
  }

  function t(path, vars) {
    let key = path;
    if (vars && vars.count != null) {
      const plural = vars.count === 1 ? path + '_one' : path + '_other';
      if (resolve(plural) != null) key = plural;
    }
    const val = resolve(key);
    if (val == null) return path;
    if (typeof val === 'string') return interpolate(val, vars);
    return val;
  }

  function has(path) {
    return resolve(path) != null;
  }

  function storedLang() {
    try {
      return localStorage.getItem('fretboard-lab-lang') || 'en';
    } catch (e) {
      return 'en';
    }
  }

  function setStoredLang(code) {
    try {
      localStorage.setItem('fretboard-lab-lang', code);
    } catch (e) { /* ignore */ }
  }

  function deepMerge(target, src) {
    if (!src || typeof src !== 'object') return target;
    Object.keys(src).forEach((k) => {
      const sv = src[k];
      if (sv && typeof sv === 'object' && !Array.isArray(sv)) {
        if (!target[k] || typeof target[k] !== 'object') target[k] = {};
        deepMerge(target[k], sv);
      } else {
        target[k] = sv;
      }
    });
    return target;
  }

  function applyVocab(code, en, loc) {
    const base = en || vocabs.en || {};
    fallback = base;
    if (code === 'ru' && loc) {
      current = {};
      deepMerge(current, base);
      deepMerge(current, loc);
      lang = 'ru';
    } else {
      current = base;
      lang = 'en';
    }
    setStoredLang(lang);
    return current;
  }

  function applyDom(rootEl) {
    const scope = rootEl || document;
    scope.querySelectorAll('[data-i18n]').forEach((el) => {
      const key = el.getAttribute('data-i18n');
      const attr = el.getAttribute('data-i18n-attr');
      const val = t(key);
      if (typeof val !== 'string') return;
      if (attr) attr.split(/\s+/).forEach((a) => { if (a) el.setAttribute(a, val); });
      else el.textContent = val;
    });
    scope.querySelectorAll('[data-i18n-html]').forEach((el) => {
      const key = el.getAttribute('data-i18n-html');
      const val = t(key);
      if (typeof val === 'string') el.innerHTML = val;
    });
    const title = t('app.title');
    if (title && title !== 'app.title') document.title = title;
    if (typeof document !== 'undefined' && document.documentElement) {
      document.documentElement.lang = lang;
    }
  }

  function register(code, data) {
    if (!data || typeof data !== 'object') throw new Error('invalid vocab: ' + code);
    vocabs[code === 'ru' ? 'ru' : 'en'] = data;
  }

  function applyLang(code) {
    const want = code === 'ru' ? 'ru' : 'en';
    if (!vocabs.en) return false;
    if (want === 'ru' && !vocabs.ru) return false;
    applyVocab(want, vocabs.en, vocabs.ru);
    applyDom(document);
    return true;
  }

  function load(code) {
    const want = (code || storedLang() || 'en') === 'ru' ? 'ru' : 'en';
    if (!vocabs.en) {
      return Promise.reject(new Error('English vocabulary not registered'));
    }
    if (want === 'ru' && !vocabs.ru) {
      applyVocab('en', vocabs.en, null);
      return Promise.resolve(current);
    }
    applyVocab(want, vocabs.en, vocabs.ru);
    return Promise.resolve(current);
  }

  function setLang(code) {
    const want = code === 'ru' ? 'ru' : 'en';
    if (applyLang(want)) return Promise.resolve(current);
    return load(want).then(() => {
      applyDom(document);
      if (want === 'ru' && lang !== 'ru') {
        return Promise.reject(new Error('Russian vocabulary unavailable'));
      }
      return current;
    });
  }

  root.FretI18n = {
    register: register,
    load: load,
    setLang: setLang,
    applyLang: applyLang,
    t: t,
    has: has,
    applyDom: applyDom,
    storedLang: storedLang,
    setStoredLang: setStoredLang,
    lang: () => lang,
    raw: resolve,
    vocab: function (code) {
      return vocabs[code === 'ru' ? 'ru' : 'en'] || null;
    }
  };
})(typeof self !== 'undefined' ? self : this);
