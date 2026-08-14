(function () {
  const t = (path, vars) => (window.FretI18n && window.FretI18n.t(path, vars)) || path;

  const CONSTRUCTION_KEYS = ['electric', 'semiAcoustic', 'hollowBody', 'acoustic', 'classical', 'resonator'];
  const ROLE_KEYS = ['guitar', 'bass', 'baritone'];

  function constructionOpts() {
    return CONSTRUCTION_KEYS.map((v) => ({ v: v, l: t('profiles.construction.' + v) }));
  }

  function roleOpts() {
    return ROLE_KEYS.map((v) => ({ v: v, l: t('profiles.roles.' + v) }));
  }

  const CONSTRUCTION_OPTS = [
    { v: 'electric', l: 'Electric · solid body' },
    { v: 'semiAcoustic', l: 'Electric · semi-acoustic' },
    { v: 'hollowBody', l: 'Electric · hollow body / archtop' },
    { v: 'acoustic', l: 'Acoustic · steel string' },
    { v: 'classical', l: 'Acoustic · classical / nylon' },
    { v: 'resonator', l: 'Acoustic · resonator' }
  ];

  const ROLE_OPTS = [
    { v: 'guitar', l: 'Guitar' },
    { v: 'bass', l: 'Bass' },
    { v: 'baritone', l: 'Baritone' }
  ];

  const SCALE_PRESETS = [22.5, 23.5, 24, 24.75, 25, 25.5, 26.5, 27, 28, 30, 32, 34, 35, 36];

  const ROLE_DEFAULTS = {
    guitar: { tuning: [40, 45, 50, 55, 59, 64], scaleLen: 25.5, fretCount: 22, construction: 'electric' },
    bass: { tuning: [28, 33, 38, 43], scaleLen: 34, fretCount: 20, construction: 'electric' },
    baritone: { tuning: [35, 40, 45, 50, 54, 59], scaleLen: 27, fretCount: 22, construction: 'electric' }
  };

  const DEFAULT_GUITAR_PHOTO = 'res/guitar-default.png';
  const PROFILE_PLACEHOLDER =
    '<img class="prof-photo-img prof-photo-default" src="' + DEFAULT_GUITAR_PHOTO + '" alt="">';

  function profileIcon(inner) {
    return '<svg class="profile-ico" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' + inner + '</svg>';
  }

  const ICO_GRIP = profileIcon(
    '<circle cx="9" cy="6" r="1.35" fill="currentColor" stroke="none"/>' +
    '<circle cx="15" cy="6" r="1.35" fill="currentColor" stroke="none"/>' +
    '<circle cx="9" cy="12" r="1.35" fill="currentColor" stroke="none"/>' +
    '<circle cx="15" cy="12" r="1.35" fill="currentColor" stroke="none"/>' +
    '<circle cx="9" cy="18" r="1.35" fill="currentColor" stroke="none"/>' +
    '<circle cx="15" cy="18" r="1.35" fill="currentColor" stroke="none"/>'
  );
  const ICO_COPY = profileIcon(
    '<rect x="9" y="9" width="11" height="11" rx="2"/>' +
    '<path d="M5 15V5a2 2 0 0 1 2-2h10"/>'
  );
  const ICO_CHECK = profileIcon('<path d="M20 6 9 17l-5-5"/>');
  const ICO_TRASH = profileIcon(
    '<path d="M3 6h18"/>' +
    '<path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>' +
    '<path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/>' +
    '<path d="M10 11v6"/>' +
    '<path d="M14 11v6"/>'
  );

  const CRC_TABLE = (() => {
    const t = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      t[n] = c >>> 0;
    }
    return t;
  })();

  function crc32(bytes) {
    let c = 0xFFFFFFFF;
    for (let i = 0; i < bytes.length; i++) c = CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
    return (c ^ 0xFFFFFFFF) >>> 0;
  }

  function concatBytes(arrs) {
    let total = 0;
    arrs.forEach((a) => { total += a.length; });
    const out = new Uint8Array(total);
    let p = 0;
    arrs.forEach((a) => { out.set(a, p); p += a.length; });
    return out;
  }

  function makeZip(entries) {
    const enc = new TextEncoder();
    const parts = [];
    const central = [];
    let offset = 0;
    entries.forEach((e) => {
      const name = enc.encode(e.name);
      const crc = crc32(e.data);
      const local = new Uint8Array(30 + name.length);
      const v = new DataView(local.buffer);
      v.setUint32(0, 0x04034b50, true);
      v.setUint16(4, 20, true);
      v.setUint16(8, 0, true);
      v.setUint32(14, crc, true);
      v.setUint32(18, e.data.length, true);
      v.setUint32(22, e.data.length, true);
      v.setUint16(26, name.length, true);
      local.set(name, 30);
      parts.push(local, e.data);
      central.push({ name: name, crc: crc, size: e.data.length, offset: offset });
      offset += local.length + e.data.length;
    });
    const cparts = [];
    let csize = 0;
    central.forEach((c) => {
      const h = new Uint8Array(46 + c.name.length);
      const v = new DataView(h.buffer);
      v.setUint32(0, 0x02014b50, true);
      v.setUint16(4, 20, true);
      v.setUint16(6, 20, true);
      v.setUint16(10, 0, true);
      v.setUint32(16, c.crc, true);
      v.setUint32(20, c.size, true);
      v.setUint32(24, c.size, true);
      v.setUint16(28, c.name.length, true);
      v.setUint32(42, c.offset, true);
      h.set(c.name, 46);
      cparts.push(h);
      csize += h.length;
    });
    const eocd = new Uint8Array(22);
    const v = new DataView(eocd.buffer);
    v.setUint32(0, 0x06054b50, true);
    v.setUint16(8, entries.length, true);
    v.setUint16(10, entries.length, true);
    v.setUint32(12, csize, true);
    v.setUint32(16, offset, true);
    return concatBytes(parts.concat(cparts, [eocd]));
  }

  function parseZip(buf) {
    const view = new DataView(buf);
    const len = buf.byteLength;
    if (len < 22) throw new Error('zip');
    let eocd = -1;
    const scanStart = Math.max(0, len - 65557);
    for (let i = len - 22; i >= scanStart; i--) {
      if (view.getUint32(i, true) === 0x06054b50) { eocd = i; break; }
    }
    if (eocd < 0) throw new Error('zip');
    const entryCount = view.getUint16(eocd + 10, true);
    const cdirSize = view.getUint32(eocd + 12, true);
    const cdirOffset = view.getUint32(eocd + 16, true);
    if (cdirOffset + cdirSize > len) throw new Error('zip');
    const files = new Map();
    const dec = new TextDecoder();
    let p = cdirOffset;
    for (let i = 0; i < entryCount; i++) {
      if (p + 46 > len || view.getUint32(p, true) !== 0x02014b50) throw new Error('zip');
      const method = view.getUint16(p + 10, true);
      const crc = view.getUint32(p + 16, true);
      const csize = view.getUint32(p + 20, true);
      const nameLen = view.getUint16(p + 28, true);
      const extraLen = view.getUint16(p + 30, true);
      const commentLen = view.getUint16(p + 32, true);
      const diskNum = view.getUint16(p + 34, true);
      const localOffset = view.getUint32(p + 42, true);
      const name = dec.decode(new Uint8Array(buf, p + 46, nameLen));
      p += 46 + nameLen + extraLen + commentLen;
      if (method !== 0) throw new Error('zip');
      if (csize === 0xFFFFFFFF || diskNum !== 0) throw new Error('zip');
      if (view.getUint32(localOffset, true) !== 0x04034b50) throw new Error('zip');
      const lNameLen = view.getUint16(localOffset + 26, true);
      const lExtraLen = view.getUint16(localOffset + 28, true);
      const dataStart = localOffset + 30 + lNameLen + lExtraLen;
      if (dataStart + csize > len) throw new Error('zip');
      const data = new Uint8Array(buf, dataStart, csize);
      if (crc32(data) !== crc) throw new Error('zip');
      files.set(name, data);
    }
    return files;
  }

  const STATE_SCHEMA = 2;
  const STATE_SCHEMA_MAX = 2;

  function makeGuid() {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID().replace(/-/g, '');
    }
    let s = '';
    for (let i = 0; i < 32; i++) s += ((Math.random() * 16) | 0).toString(16);
    return s;
  }

  function extForMime(mime) {
    const m = String(mime || '').toLowerCase();
    if (m.indexOf('png') >= 0) return 'png';
    if (m.indexOf('webp') >= 0) return 'webp';
    if (m.indexOf('gif') >= 0) return 'gif';
    return 'jpg';
  }

  function mimeFromName(name) {
    const n = String(name || '').toLowerCase();
    if (/\.png$/i.test(n)) return 'image/png';
    if (/\.webp$/i.test(n)) return 'image/webp';
    if (/\.gif$/i.test(n)) return 'image/gif';
    return 'image/jpeg';
  }

  function parseDataUrl(dataUrl) {
    const s = String(dataUrl || '');
    const m = /^data:([^;,]+)?(;base64)?,(.*)$/i.exec(s);
    if (!m) return null;
    const mime = m[1] || 'application/octet-stream';
    const isB64 = !!m[2];
    const payload = m[3] || '';
    try {
      if (isB64) {
        const bin = atob(payload);
        const bytes = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        return { mime: mime, bytes: bytes };
      }
      const text = decodeURIComponent(payload);
      const bytes = new TextEncoder().encode(text);
      return { mime: mime, bytes: bytes };
    } catch (e) {
      return null;
    }
  }

  function bytesToDataUrl(bytes, mime) {
    const u8 = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
    let binary = '';
    const chunk = 0x8000;
    for (let i = 0; i < u8.length; i += chunk) {
      binary += String.fromCharCode.apply(null, u8.subarray(i, Math.min(i + chunk, u8.length)));
    }
    return 'data:' + (mime || 'application/octet-stream') + ';base64,' + btoa(binary);
  }

  function detachProfilePhotos(stateObj) {
    const entries = [];
    const out = JSON.parse(JSON.stringify(stateObj));
    (out.profiles || []).forEach((p) => {
      if (!p || typeof p.photo !== 'string') {
        if (p) p.photo = '';
        return;
      }
      if (p.photo.indexOf('data:') !== 0) {
        p.photo = '';
        return;
      }
      const parsed = parseDataUrl(p.photo);
      if (!parsed || !parsed.bytes.length) {
        p.photo = '';
        return;
      }
      const guid = makeGuid();
      const fileName = guid + '.' + extForMime(parsed.mime);
      entries.push({ name: 'blobs/' + fileName, data: parsed.bytes });
      p.photo = fileName;
    });
    return { state: out, blobEntries: entries };
  }

  function hydrateProfilePhotos(stateObj, files) {
    if (!stateObj || !files) return stateObj;
    (stateObj.profiles || []).forEach((p) => {
      if (!p || typeof p.photo !== 'string' || !p.photo) return;
      if (p.photo.indexOf('data:') === 0) return;
      const rel = p.photo.indexOf('blobs/') === 0 ? p.photo : 'blobs/' + p.photo;
      let data = files.get(rel);
      if (!data) {
        const needle = p.photo.replace(/^blobs\//, '');
        files.forEach((bytes, name) => {
          if (data) return;
          if (name === rel || name === 'blobs/' + needle) data = bytes;
        });
      }
      if (!data) {
        p.photo = '';
        return;
      }
      p.photo = bytesToDataUrl(data, mimeFromName(rel));
    });
    return stateObj;
  }

  function midiToStr(m) {
    return { n: m % 12, o: Math.floor(m / 12) - 1 };
  }

  function strToMidi(t) {
    return t.n + (t.o + 1) * 12;
  }

  function create(deps) {
    const $ = deps.$;
    const state = deps.state;
    const names = deps.names;
    const saveState = deps.saveState;
    const render = deps.render;
    const syncControls = deps.syncControls;
    const applyTabUI = deps.applyTabUI;
    const esc = deps.esc;
    const serializeState = deps.serializeState;
    const sanitizeState = deps.sanitizeState;

    let profileEditId = null;
    let pendingPhoto = '';
    let editTuning = [];
    let editTouched = false;
    let importStatusTimer = null;
    let dragProfileId = null;

    function openCropEditor(src, prevPhoto) {
      const box = $('.prof-photo-box');
      box.innerHTML =
        '<div class="crop-wrap">' +
        '<div class="crop-stage" id="crop-stage"></div>' +
        '<div class="crop-actions">' +
        '<button type="button" id="crop-apply" class="mini prof-save">' + t('profiles.cropApply') + '</button>' +
        '<button type="button" id="crop-cancel" class="mini">' + t('profiles.cancel') + '</button>' +
        '<span class="crop-hint">' + t('profiles.cropHint') + '</span>' +
        '</div></div>';
      const stage = $('#crop-stage');
      if (!src) { restorePhotoBox(); return; }
      let ready = false;
      let dw = 0, dh = 0, cx = 0, cy = 0, cw = 0, ch = 0;
      let teardown = function () {};
      const loadMsg = t('profiles.imageLoadError');
      const fail = (msg) => {
        ready = false;
        if (!document.body.contains(stage)) return;
        stage.innerHTML = '<span class="crop-error">' + msg + '</span>';
        const apply = $('#crop-apply');
        if (apply) apply.disabled = true;
      };
      $('#crop-apply').addEventListener('click', () => {
        if (!ready) { fail(loadMsg); return; }
        try {
          const nw = io.naturalWidth, nh = io.naturalHeight;
          if (!nw || !nh) throw new Error('bad image');
          const out = document.createElement('canvas');
          out.width = 900;
          out.height = 675;
          const ctx = out.getContext('2d');
          ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue('--stage-bg').trim() || '#0e1016';
          ctx.fillRect(0, 0, 900, 675);
          ctx.drawImage(io, cx * nw / dw, cy * nh / dh, cw * nw / dw, ch * nh / dh, 0, 0, 900, 675);
          teardown();
          pendingPhoto = out.toDataURL('image/jpeg', 0.78);
          restorePhotoBox();
        } catch (e) {
          teardown();
          fail(t('profiles.imageCropError'));
        }
      });
      $('#crop-cancel').addEventListener('click', () => {
        teardown();
        pendingPhoto = prevPhoto;
        restorePhotoBox();
      });
      const io = new Image();
      io.onerror = () => fail(loadMsg);
      io.onload = () => {
        if (!io.naturalWidth || !io.naturalHeight) { fail(t('profiles.imageNoDimensions')); return; }
        ready = true;
        const maxW = 340, maxH = 280;
        dw = io.naturalWidth;
        dh = io.naturalHeight;
        const sc = Math.min(1, maxW / dw, maxH / dh);
        dw = Math.max(1, Math.round(dw * sc));
        dh = Math.max(1, Math.round(dh * sc));
        stage.style.width = dw + 'px';
        stage.style.height = dh + 'px';
        const img = document.createElement('img');
        img.src = src;
        img.alt = t('profiles.cropPreviewAlt');
        img.width = dw;
        img.height = dh;
        stage.appendChild(img);

        cw = Math.min(dw, dh * 4 / 3);
        ch = cw * 3 / 4;
        if (ch > dh) { ch = dh; cw = ch * 4 / 3; }
        cw = Math.floor(cw);
        ch = Math.floor(ch);
        cx = Math.floor((dw - cw) / 2);
        cy = Math.floor((dh - ch) / 2);

        const cb = document.createElement('div');
        cb.className = 'crop-box';
        const hdl = document.createElement('div');
        hdl.className = 'crop-handle';
        cb.appendChild(hdl);
        stage.appendChild(cb);

        const minW = Math.min(48, dw);
        let mode = null, mx = 0, my = 0, bx = 0, by = 0, bw = 0;

        function paint() {
          cb.style.left = cx + 'px';
          cb.style.top = cy + 'px';
          cb.style.width = cw + 'px';
          cb.style.height = ch + 'px';
        }
        paint();

        function onMove(e) {
          if (!document.body.contains(cb)) { teardown(); return; }
          if (!mode) return;
          const dx = e.clientX - mx;
          const dy = e.clientY - my;
          if (mode === 'move') {
            cx = Math.max(0, Math.min(dw - cw, bx + dx));
            cy = Math.max(0, Math.min(dh - ch, by + dy));
          } else {
            let w = Math.max(minW, Math.min(dw - cx, bw + dx));
            let h = w * 3 / 4;
            if (h > dh - cy) { h = dh - cy; w = h * 4 / 3; }
            cw = Math.floor(w);
            ch = Math.floor(h);
          }
          paint();
        }

        function onUp() {
          if (!document.body.contains(cb)) { teardown(); return; }
          mode = null;
        }

        cb.addEventListener('pointerdown', (e) => {
          if (e.pointerType === 'mouse' && e.button !== 0) return;
          e.preventDefault();
          try { cb.setPointerCapture(e.pointerId); } catch (err) { /* ignore */ }
          mx = e.clientX;
          my = e.clientY;
          if (e.target === hdl) {
            mode = 'resize';
            bw = cw;
          } else {
            mode = 'move';
            bx = cx;
            by = cy;
          }
        });

        cb.addEventListener('pointermove', onMove);
        cb.addEventListener('pointerup', onUp);
        cb.addEventListener('pointercancel', onUp);
        teardown = () => {
          cb.removeEventListener('pointermove', onMove);
          cb.removeEventListener('pointerup', onUp);
          cb.removeEventListener('pointercancel', onUp);
        };
      };
      io.src = src;
      setTimeout(() => {
        if (!ready && document.body.contains(stage)) fail(loadMsg);
      }, 6000);
    }

    function restorePhotoBox() {
      const box = $('.prof-photo-box');
      const photoHtml = pendingPhoto
        ? '<img class="prof-photo-img" src="' + pendingPhoto + '" alt="' + t('profiles.photoAlt') + '">'
        : PROFILE_PLACEHOLDER;
      box.innerHTML =
        '<div class="prof-photo">' + photoHtml + '</div>' +
        '<div class="prof-photo-actions">' +
        '<button type="button" id="prof-photo-btn" class="mini">' + t('profiles.uploadPhoto') + '</button>' +
        (pendingPhoto ? '<button type="button" id="prof-photo-crop" class="mini">' + t('profiles.cropPhoto') + '</button>' : '') +
        (pendingPhoto ? '<button type="button" id="prof-photo-clear" class="mini">' + t('profiles.removePhoto') + '</button>' : '') +
        '<input type="file" id="prof-photo-input" accept="image/*" hidden>' +
        '</div>';
      bindPhotoControls();
    }

    function bindPhotoControls() {
      const btn = $('#prof-photo-btn');
      if (btn) btn.addEventListener('click', () => $('#prof-photo-input').click());
      const crop = $('#prof-photo-crop');
      if (crop) crop.addEventListener('click', () => openCropEditor(pendingPhoto, pendingPhoto));
      const clear = $('#prof-photo-clear');
      if (clear) clear.addEventListener('click', clearPhoto);
      const input = $('#prof-photo-input');
      if (input) input.addEventListener('change', (e) => {
        const f = e.target.files && e.target.files[0];
        if (!f) return;
        e.target.value = '';
        const fr = new FileReader();
        fr.onload = () => openCropEditor(fr.result, pendingPhoto);
        fr.readAsDataURL(f);
      });
    }

    function renderProfiles() {
      const grid = $('#profile-grid');
      grid.innerHTML = '';
      const consOpts = constructionOpts();
      const roles = roleOpts();
      state.profiles.forEach((p) => {
        const cons = consOpts.find((c) => c.v === p.construction) || consOpts[0];
        const role = roles.find((r) => r.v === p.role) || roles[0];
        const photo = p.photo
          ? '<img class="prof-photo-img" src="' + p.photo + '" alt="' + esc(p.name) + '">'
          : PROFILE_PLACEHOLDER;
        const tuningTxt = p.tuning.map((tn) => names()[tn.n] + tn.o).join(' ');
        const active = state.activeProfileId === p.id;
        const card = document.createElement('div');
        card.className = 'profile-card' + (active ? ' active' : '');
        card.dataset.id = p.id;
        card.innerHTML =
          '<span class="profile-grip" title="' + t('profiles.dragToReorder') + '" aria-label="' + t('profiles.dragToReorder') + '">' + ICO_GRIP + '</span>' +
          '<button type="button" class="profile-btn profile-del" data-del="' + p.id + '" title="' + t('profiles.removeGuitar') + '" aria-label="' + t('profiles.removeGuitar') + '">' + ICO_TRASH + '</button>' +
          '<button type="button" class="profile-btn profile-use' + (active ? ' on' : '') + '" data-use="' + p.id + '" title="' + (active ? t('profiles.usingGuitar') : t('profiles.useGuitar')) + '" aria-label="' + (active ? t('profiles.usingGuitar') : t('profiles.useGuitar')) + '">' + ICO_CHECK + '</button>' +
          '<button type="button" class="profile-btn profile-copy" data-copy="' + p.id + '" title="' + t('profiles.duplicateGuitar') + '" aria-label="' + t('profiles.duplicateGuitar') + '">' + ICO_COPY + '</button>' +
          '<div class="prof-photo">' + photo + '</div>' +
          '<div class="profile-body">' +
          '<div class="profile-name">' + esc(p.name) + '</div>' +
          '<div class="profile-desc' + (p.desc ? '' : ' empty') + '">' + esc(p.desc || t('profiles.noDescription')) + '</div>' +
          '<div class="profile-chips"><span>' + esc(cons.l) + '</span><span>' + esc(role.l) + '</span></div>' +
          '<div class="profile-meta">' + t('profiles.meta', { scaleLen: p.scaleLen, fretCount: p.fretCount }) + '</div>' +
          '<div class="profile-tuning">' + esc(tuningTxt) + '</div>' +
          '</div>';
        grid.appendChild(card);
      });
      const add = document.createElement('button');
      add.type = 'button';
      add.className = 'profile-card profile-add';
      add.innerHTML = '<span class="prof-add-plus">+</span><span class="prof-add-label">' + t('profiles.addGuitar') + '</span>';
      grid.appendChild(add);
    }

    function toggleActiveProfile(id) {
      state.activeProfileId = state.activeProfileId === id ? null : id;
      renderProfiles();
      saveState();
      if (typeof syncControls === 'function') syncControls();
    }

    function copyProfile(id) {
      const idx = state.profiles.findIndex((x) => x.id === id);
      if (idx < 0) return;
      const p = state.profiles[idx];
      state.profileSeq += 1;
      state.profiles.splice(idx + 1, 0, {
        id: state.profileSeq,
        name: (p.name + t('profiles.copySuffix')).slice(0, 60),
        construction: p.construction,
        role: p.role,
        scaleLen: p.scaleLen,
        fretCount: p.fretCount,
        tuning: p.tuning.map((tn) => ({ n: tn.n, o: tn.o })),
        photo: p.photo,
        desc: p.desc || ''
      });
      renderProfiles();
      saveState();
    }

    let modalReturnFocus = null;

    function openModal(html) {
      modalReturnFocus = document.activeElement;
      $('#modal').innerHTML = html;
      const overlay = $('#modal-overlay');
      overlay.classList.remove('hidden');
      overlay.querySelectorAll('[data-close]').forEach((el) => el.addEventListener('click', closeModal));
      const dialog = $('#modal');
      const title = dialog.querySelector('.modal-title, h2, h3');
      if (title) {
        if (!title.id) title.id = 'modal-title';
        dialog.setAttribute('aria-labelledby', title.id);
      } else {
        dialog.removeAttribute('aria-labelledby');
      }
      requestAnimationFrame(() => {
        const focusable = dialog.querySelector(
          'input:not([type="hidden"]), select, textarea, button, [href], [tabindex]:not([tabindex="-1"])'
        );
        if (focusable && focusable.focus) focusable.focus();
      });
    }

    function closeModal() {
      $('#modal-overlay').classList.add('hidden');
      $('#modal').innerHTML = '';
      profileEditId = null;
      pendingPhoto = '';
      editTuning = [];
      editTouched = false;
      const back = modalReturnFocus;
      modalReturnFocus = null;
      if (back && typeof back.focus === 'function') {
        try { back.focus(); } catch (e) { /* ignore */ }
      }
    }

    function openProfileModal(id) {
      profileEditId = id || null;
      const p = id ? state.profiles.find((x) => x.id === id) : null;
      pendingPhoto = p ? p.photo : '';
      const role = p ? p.role : 'guitar';
      const def = ROLE_DEFAULTS[role];
      editTuning = p ? p.tuning.slice() : def.tuning.map(midiToStr);
      editTouched = !!p;
      openModal(buildProfileForm(p ? p.name : '', p ? p.construction : def.construction, role, p ? p.scaleLen : def.scaleLen, p ? p.fretCount : def.fretCount, p ? p.desc : ''));
      renderTuneRows();
      bindProfileFormEvents();
    }

    function buildProfileForm(name, construction, role, scaleLen, fretCount, desc) {
      const consOpts = constructionOpts().map((c) => '<option value="' + c.v + '"' + (c.v === construction ? ' selected' : '') + '>' + esc(c.l) + '</option>').join('');
      const roleHtml = roleOpts().map((r) => '<option value="' + r.v + '"' + (r.v === role ? ' selected' : '') + '>' + r.l + '</option>').join('');
      const presets = SCALE_PRESETS.map((s) => '<option value="' + s + '"></option>').join('');
      const photoHtml = pendingPhoto
        ? '<img class="prof-photo-img" src="' + pendingPhoto + '" alt="' + t('profiles.photoAlt') + '">'
        : PROFILE_PLACEHOLDER;
      return '<div class="modal-head"><h2>' + (profileEditId ? t('profiles.editGuitar') : t('profiles.addGuitar')) + '</h2>' +
        '<button type="button" class="modal-x" data-close title="' + t('profiles.close') + '">&#10005;</button></div>' +
        '<div class="prof-photo-box">' +
        '<div class="prof-photo">' + photoHtml + '</div>' +
        '<div class="prof-photo-actions">' +
        '<button type="button" id="prof-photo-btn" class="mini">' + t('profiles.uploadPhoto') + '</button>' +
        (pendingPhoto ? '<button type="button" id="prof-photo-crop" class="mini">' + t('profiles.cropPhoto') + '</button>' : '') +
        (pendingPhoto ? '<button type="button" id="prof-photo-clear" class="mini">' + t('profiles.removePhoto') + '</button>' : '') +
        '<input type="file" id="prof-photo-input" accept="image/*" hidden>' +
        '</div></div>' +
        '<div class="form-grid">' +
        '<label>' + t('profiles.name') + '<input id="prof-name" type="text" maxlength="60" placeholder="' + t('profiles.namePlaceholder') + '" value="' + esc(name) + '"></label>' +
        '<label>' + t('profiles.type') + '<select id="prof-construction">' + consOpts + '</select></label>' +
        '<label>' + t('profiles.role') + '<select id="prof-role">' + roleHtml + '</select></label>' +
        '<label>' + t('profiles.scaleLength') + '<input id="prof-scale" type="number" min="20" max="40" step="0.25" list="prof-scale-list" value="' + scaleLen + '"></label>' +
        '<label>' + t('profiles.frets') + '<input id="prof-fret" type="number" min="15" max="24" step="1" list="prof-fret-list" value="' + fretCount + '"></label>' +
        '<label class="prof-desc-field">' + t('profiles.description') +
        '<textarea id="prof-desc" maxlength="255" rows="3" placeholder="' + t('profiles.descriptionPlaceholder') + '">' + esc(desc) + '</textarea>' +
        '<span class="prof-desc-count" id="prof-desc-count">' + t('profiles.descCount', { n: desc.length }) + '</span>' +
        '</label>' +
        '</div>' +
        '<datalist id="prof-scale-list">' + presets + '</datalist>' +
        '<datalist id="prof-fret-list"><option value="15"><option value="16"><option value="17"><option value="18"><option value="19"><option value="20"><option value="21"><option value="22"><option value="23"><option value="24"></datalist>' +
        '<div class="tune-head"><span class="lbl">' + t('profiles.tuning') + ' <i>' + t('profiles.tuningLowToHigh') + '</i></span>' +
        '<span class="tune-btns">' +
        '<button type="button" id="prof-rm-str" class="mini">' + t('profiles.minusString') + '</button>' +
        '<button type="button" id="prof-add-str" class="mini">' + t('profiles.plusString') + '</button>' +
        '</span></div>' +
        '<div id="prof-tune-rows"></div>' +
        '<div class="modal-actions">' +
        '<button type="button" id="prof-cancel" class="mini">' + t('profiles.cancel') + '</button>' +
        '<button type="button" id="prof-save" class="mini prof-save">' + t('profiles.save') + '</button>' +
        '</div>';
    }

    function renderTuneRows() {
      const wrap = $('#prof-tune-rows');
      wrap.innerHTML = '';
      const nm = names();
      editTuning.forEach((tn, i) => {
        const row = document.createElement('div');
        row.className = 'tune-row';
        const strLbl = document.createElement('span');
        strLbl.className = 'tune-str';
        strLbl.textContent = t('profiles.strLabel', { n: editTuning.length - i });
        const noteSel = document.createElement('select');
        noteSel.className = 'tune-note';
        noteSel.innerHTML = nm.map((nn, p) => '<option value="' + p + '"' + (p === tn.n ? ' selected' : '') + '>' + nn + '</option>').join('');
        const octSel = document.createElement('select');
        octSel.className = 'tune-oct';
        octSel.innerHTML = [0, 1, 2, 3, 4, 5, 6].map((o) => '<option value="' + o + '"' + (o === tn.o ? ' selected' : '') + '>' + o + '</option>').join('');
        row.appendChild(strLbl);
        row.appendChild(noteSel);
        row.appendChild(octSel);
        wrap.appendChild(row);
      });
    }

    function applyRoleDefaults(role) {
      const def = ROLE_DEFAULTS[role];
      editTuning = def.tuning.map(midiToStr);
      renderTuneRows();
      const scale = $('#prof-scale');
      if (scale) scale.value = def.scaleLen;
      const fret = $('#prof-fret');
      if (fret) fret.value = def.fretCount;
    }

    function clearPhoto() {
      pendingPhoto = '';
      restorePhotoBox();
    }

    function bindProfileFormEvents() {
      bindPhotoControls();
      $('#prof-role').addEventListener('change', (e) => {
        if (!editTouched) applyRoleDefaults(e.target.value);
      });
      $('#prof-add-str').addEventListener('click', () => {
        if (editTuning.length >= 10) return;
        const base = editTuning.length ? strToMidi(editTuning[0]) : 40;
        editTuning.unshift(midiToStr(base - 5));
        editTouched = true;
        renderTuneRows();
      });
      $('#prof-rm-str').addEventListener('click', () => {
        if (editTuning.length <= 1) return;
        editTuning.shift();
        editTouched = true;
        renderTuneRows();
      });
      $('#prof-tune-rows').addEventListener('input', (e) => {
        const row = e.target.closest('.tune-row');
        if (!row) return;
        const idx = Array.prototype.indexOf.call(row.parentNode.children, row);
        editTuning[idx] = {
          n: row.querySelector('.tune-note').value | 0,
          o: row.querySelector('.tune-oct').value | 0
        };
        editTouched = true;
      });
      $('#prof-scale').addEventListener('input', () => { editTouched = true; });
      const descField = $('#prof-desc');
      if (descField) {
        descField.addEventListener('input', () => {
          const c = $('#prof-desc-count');
          if (c) c.textContent = t('profiles.descCount', { n: descField.value.length });
        });
      }
      $('#prof-cancel').addEventListener('click', closeModal);
      $('#prof-save').addEventListener('click', saveProfileFromModal);
    }

    function saveProfileFromModal() {
      const role = $('#prof-role').value;
      const def = ROLE_DEFAULTS[role];
      const name = $('#prof-name').value.trim();
      const construction = $('#prof-construction').value;
      const scaleLen = Math.max(20, Math.min(40, parseFloat($('#prof-scale').value) || def.scaleLen));
      const fretCount = Math.max(15, Math.min(24, parseInt($('#prof-fret').value, 10) || def.fretCount));
      const desc = $('#prof-desc').value.slice(0, 255);
      const tuning = editTuning.slice();
      if (!tuning.length) tuning.push.apply(tuning, def.tuning.map(midiToStr));
      const existing = profileEditId ? state.profiles.find((x) => x.id === profileEditId) : null;
      const finalName = name || (existing && existing.name) || t('profiles.defaultName', { n: state.profiles.length + 1 });
      if (existing) {
        existing.name = finalName;
        existing.construction = construction;
        existing.role = role;
        existing.scaleLen = scaleLen;
        existing.fretCount = fretCount;
        existing.tuning = tuning;
        existing.photo = pendingPhoto;
        existing.desc = desc;
      } else {
        state.profileSeq += 1;
        state.profiles.push({
          id: state.profileSeq,
          name: finalName,
          construction: construction,
          role: role,
          scaleLen: scaleLen,
          fretCount: fretCount,
          tuning: tuning,
          photo: pendingPhoto,
          desc: desc
        });
      }
      closeModal();
      renderProfiles();
      if (!saveState()) showImportStatus(t('profiles.saveStorageFull'), false);
    }

    function confirmDeleteProfile(id) {
      const p = state.profiles.find((x) => x.id === id);
      if (!p) return;
      openModal(
        '<div class="modal-head"><h2>' + t('profiles.confirmRemoveTitle') + '</h2>' +
        '<button type="button" class="modal-x" data-close title="' + t('profiles.close') + '">&#10005;</button></div>' +
        '<p class="confirm-text">' + t('profiles.confirmRemoveBody', { name: esc(p.name) }) + '</p>' +
        '<div class="modal-actions">' +
        '<button type="button" id="prof-cancel" class="mini">' + t('profiles.cancel') + '</button>' +
        '<button type="button" id="prof-del-ok" class="mini prof-danger">' + t('profiles.remove') + '</button>' +
        '</div>'
      );
      $('#prof-cancel').addEventListener('click', closeModal);
      $('#prof-del-ok').addEventListener('click', () => {
        state.profiles = state.profiles.filter((x) => x.id !== id);
        if (state.activeProfileId === id) state.activeProfileId = null;
        closeModal();
        renderProfiles();
        saveState();
      });
    }

    function showImportStatus(msg, ok) {
      const el = $('#prof-import-status');
      if (!el) return;
      el.textContent = msg;
      el.className = 'prof-import-status ' + (ok ? 'ok' : 'err');
      clearTimeout(importStatusTimer);
      importStatusTimer = setTimeout(() => { el.textContent = ''; el.className = 'prof-import-status'; }, 6000);
    }

    function exportSettings() {
      const now = new Date().toISOString();
      const packed = detachProfilePhotos(serializeState());
      const manifest = JSON.stringify({
        app: 'fretboard-lab',
        schema: STATE_SCHEMA,
        exportedAt: now,
        format: 'flstate'
      }, null, 2);
      const payload = JSON.stringify({
        app: 'fretboard-lab',
        schema: STATE_SCHEMA,
        exportedAt: now,
        state: packed.state
      }, null, 2);
      const zip = makeZip([
        { name: 'manifest.json', data: new TextEncoder().encode(manifest) },
        { name: 'fretboard-lab-state.json', data: new TextEncoder().encode(payload) }
      ].concat(packed.blobEntries));
      const url = URL.createObjectURL(new Blob([zip], { type: 'application/zip' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'fretboard-lab-export-' + now.slice(0, 10) + '.flstate';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    }

    function importSettings(file) {
      if (file.size > 52428800) { showImportStatus(t('profiles.importTooLarge'), false); return; }
      const fr = new FileReader();
      fr.onerror = () => showImportStatus(t('profiles.importReadError'), false);
      fr.onload = () => {
        let files;
        try {
          files = parseZip(fr.result);
        } catch (e) {
          showImportStatus(t('profiles.importInvalidZip'), false);
          return;
        }
        const stData = files.get('fretboard-lab-state.json');
        if (!stData) {
          showImportStatus(t('profiles.importNoSettings'), false);
          return;
        }
        let parsed;
        try {
          parsed = JSON.parse(new TextDecoder().decode(stData));
        } catch (e) {
          showImportStatus(t('profiles.importCorruptJson'), false);
          return;
        }
        if (!parsed || parsed.app !== 'fretboard-lab' || !parsed.state || typeof parsed.state !== 'object') {
          showImportStatus(t('profiles.importUnknownFormat'), false);
          return;
        }
        if (typeof parsed.schema === 'number' && parsed.schema > STATE_SCHEMA_MAX) {
          showImportStatus(t('profiles.importNewerVersion'), false);
          return;
        }
        hydrateProfilePhotos(parsed.state, files);
        const importedCount = (parsed.state.profiles && parsed.state.profiles.length) || 0;
        confirmImport(parsed.state, importedCount);
      };
      fr.readAsArrayBuffer(file);
    }

    function confirmImport(rawState, importedCount) {
      const currentCount = state.profiles.length;
      openModal(
        '<div class="modal-head"><h2>' + t('profiles.importTitle') + '</h2>' +
        '<button type="button" class="modal-x" data-close title="' + t('profiles.close') + '">&#10005;</button></div>' +
        '<p class="confirm-text">' + t('profiles.confirmImportBody', { currentCount: currentCount, importedCount: importedCount }) + '</p>' +
        '<div class="modal-actions">' +
        '<button type="button" id="prof-import-cancel" class="mini">' + t('profiles.cancel') + '</button>' +
        '<button type="button" id="prof-import-ok" class="mini prof-save">' + t('profiles.importAction') + '</button>' +
        '</div>'
      );
      $('#prof-import-cancel').addEventListener('click', closeModal);
      $('#prof-import-ok').addEventListener('click', () => {
        sanitizeState(rawState);
        state.tab = 'profiles';
        saveState();
        syncControls();
        applyTabUI();
        closeModal();
        render();
        showImportStatus(t('profiles.importSuccess', { count: importedCount }), true);
      });
    }

    function bind() {
      $('#profile-grid').addEventListener('click', (e) => {
        if (e.target.closest('.profile-grip')) return;
        const use = e.target.closest('[data-use]');
        if (use) {
          toggleActiveProfile(parseInt(use.dataset.use, 10));
          return;
        }
        const copy = e.target.closest('[data-copy]');
        if (copy) {
          copyProfile(parseInt(copy.dataset.copy, 10));
          return;
        }
        const del = e.target.closest('[data-del]');
        if (del) {
          confirmDeleteProfile(parseInt(del.dataset.del, 10));
          return;
        }
        if (e.target.closest('.profile-add')) {
          openProfileModal(null);
          return;
        }
        const card = e.target.closest('.profile-card');
        if (card) openProfileModal(parseInt(card.dataset.id, 10));
      });

      const grid = $('#profile-grid');
      let dragStartX = 0, dragStartY = 0, dragArmed = false, dragMoved = false, dragPointerId = null;

      function endProfileDrag(commit) {
        if (!dragArmed) return;
        dragArmed = false;
        const id = dragProfileId;
        const moved = dragMoved;
        dragPointerId = null;
        dragProfileId = null;
        dragMoved = false;
        grid.classList.remove('dragging');
        grid.querySelectorAll('.profile-card.drag-src').forEach((el) => el.classList.remove('drag-src'));
        if (!commit || !moved || id == null) return;
        const order = Array.prototype.filter
          .call(grid.children, (el) => el.classList.contains('profile-card') && !el.classList.contains('profile-add'))
          .map((el) => parseInt(el.dataset.id, 10));
        state.profiles.sort((a, b) => order.indexOf(a.id) - order.indexOf(b.id));
        renderProfiles();
        saveState();
      }

      grid.addEventListener('pointerdown', (e) => {
        if (e.pointerType === 'mouse' && e.button !== 0) return;
        if (!e.target.closest('.profile-grip')) return;
        const card = e.target.closest('.profile-card');
        if (!card) return;
        e.preventDefault();
        dragProfileId = parseInt(card.dataset.id, 10);
        dragStartX = e.clientX;
        dragStartY = e.clientY;
        dragArmed = true;
        dragMoved = false;
        dragPointerId = e.pointerId;
        try { card.setPointerCapture(e.pointerId); } catch (err) { /* ignore */ }
      });

      document.addEventListener('pointermove', (e) => {
        if (!dragArmed || e.pointerId !== dragPointerId) return;
        if (!dragMoved && Math.abs(e.clientX - dragStartX) < 4 && Math.abs(e.clientY - dragStartY) < 4) return;
        if (!dragMoved) {
          dragMoved = true;
          const src = grid.querySelector('[data-id="' + dragProfileId + '"]');
          if (src) src.classList.add('drag-src');
          grid.classList.add('dragging');
        }
        e.preventDefault();
        const hit = document.elementFromPoint(e.clientX, e.clientY);
        const card = hit && hit.closest ? hit.closest('.profile-card') : null;
        if (!card || card.classList.contains('profile-add')) return;
        const overId = parseInt(card.dataset.id, 10);
        if (overId === dragProfileId) return;
        const rect = card.getBoundingClientRect();
        const after = e.clientX > rect.left + rect.width / 2;
        const src = grid.querySelector('.profile-card.drag-src');
        if (!src) return;
        if (after) {
          if (src.nextElementSibling === card) return;
          grid.insertBefore(src, card.nextElementSibling);
        } else {
          if (src.previousElementSibling === card) return;
          grid.insertBefore(src, card);
        }
      });

      document.addEventListener('pointerup', (e) => {
        if (e.pointerId !== dragPointerId) return;
        endProfileDrag(true);
      });
      document.addEventListener('pointercancel', (e) => {
        if (e.pointerId !== dragPointerId) return;
        endProfileDrag(false);
      });

      window.addEventListener('blur', () => {
        endProfileDrag(false);
      });

      $('#prof-export').addEventListener('click', exportSettings);
      $('#prof-import').addEventListener('click', () => $('#prof-import-input').click());
      $('#prof-import-input').addEventListener('change', (e) => {
        const f = e.target.files && e.target.files[0];
        if (f) importSettings(f);
        e.target.value = '';
      });

      $('#modal-overlay').addEventListener('click', (e) => {
        if (e.target === $('#modal-overlay')) closeModal();
      });
      document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && !$('#modal-overlay').classList.contains('hidden')) closeModal();
      });
    }

    return {
      renderProfiles: renderProfiles,
      openProfileModal: openProfileModal,
      bind: bind,
      strToMidi: strToMidi,
      midiToStr: midiToStr
    };
  }

  window.FretProfiles = {
    CONSTRUCTION_OPTS: CONSTRUCTION_OPTS,
    ROLE_OPTS: ROLE_OPTS,
    SCALE_PRESETS: SCALE_PRESETS,
    ROLE_DEFAULTS: ROLE_DEFAULTS,
    DEFAULT_GUITAR_PHOTO: DEFAULT_GUITAR_PHOTO,
    PROFILE_PLACEHOLDER: PROFILE_PLACEHOLDER,
    create: create
  };
})();
