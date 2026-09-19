(function () {
  const STORAGE_KEY = 'csePlayerState';
  const CHANNEL_NAME = 'cse-player';
  const TICK_MS = 400;
  const tabId = Math.random().toString(36).slice(2) + String(Date.now());

  const audio = new Audio();
  let channel = null;
  try {
    channel = new BroadcastChannel(CHANNEL_NAME);
  } catch (e) {
    channel = null;
  }

  let queue = [];
  let playlist = [];
  let current = null;
  let currentSource = 'playlist';
  let playing = false;
  let replay = false;
  let isLeader = false;
  let lastTime = 0;
  let lastDuration = 0;
  let lastTickAt = 0;
  let lastPersistAt = 0;
  let applyingRemote = false;
  let switchingTrack = false;

  function srcKey(src) {
    if (!src) {
      return '';
    }
    try {
      return new URL(src, location.href).href;
    } catch (e) {
      return src;
    }
  }

  function sameSrc(a, b) {
    const left = typeof a === 'string' ? a : (a && a.src);
    const right = typeof b === 'string' ? b : (b && b.src);
    return !!(left && right && srcKey(left) === srcKey(right));
  }

  function plainTrack(track) {
    if (!track || !track.src) {
      return null;
    }
    return {
      src: track.src,
      title: track.title || 'Unknown',
      artist: track.artist || '',
      duration: track.duration || ''
    };
  }

  function plainList(list) {
    return (list || []).map(plainTrack).filter(Boolean);
  }

  function trackFromRow(row) {
    if (!row || !row.dataset.src) {
      return null;
    }
    return {
      src: row.dataset.src,
      title: row.dataset.title || 'Unknown',
      artist: row.dataset.artist || '',
      duration: row.dataset.duration || '',
      row: row
    };
  }

  function tracksIn(playlistEl) {
    return Array.from(playlistEl.querySelectorAll('.cse-track'))
        .map(trackFromRow)
        .filter(Boolean);
  }

  function $(selector) {
    return document.querySelector(selector);
  }

  function showPlayer() {
    const el = $('.cse-player');
    if (el && (current || queue.length > 0)) {
      el.classList.add('visible');
    }
  }

  function formatTime(seconds) {
    if (!isFinite(seconds) || seconds < 0) {
      return '0:00';
    }
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return mins + ':' + String(secs).padStart(2, '0');
  }

  function reportedTime() {
    return isLeader ? (audio.currentTime || 0) : lastTime;
  }

  function reportedDuration() {
    if (isLeader && isFinite(audio.duration) && audio.duration > 0) {
      return audio.duration;
    }
    return lastDuration;
  }

  function snapshot() {
    return {
      from: tabId,
      current: plainTrack(current),
      playlist: plainList(playlist),
      queue: plainList(queue),
      currentSource: currentSource,
      replay: replay,
      playing: playing,
      currentTime: reportedTime(),
      duration: reportedDuration(),
      ts: Date.now()
    };
  }

  function persist() {
    try {
      lastPersistAt = Date.now();
      localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot()));
    } catch (e) {
      /* quota / private mode */
    }
  }

  function persistTick() {
    if (Date.now() - lastPersistAt >= 2000) {
      persist();
    }
  }

  function post(type, extra) {
    if (applyingRemote || !channel) {
      return;
    }
    const payload = Object.assign(snapshot(), extra || {}, { type: type });
    try {
      channel.postMessage(payload);
    } catch (e) {
      /* closed */
    }
  }

  function publish(type, extra) {
    if (type === 'tick') {
      persistTick();
    } else {
      persist();
    }
    post(type, extra);
  }

  function becomeLeader() {
    isLeader = true;
  }

  function stopLocalAudio() {
    audio.pause();
    try {
      audio.removeAttribute('src');
      audio.load();
    } catch (e) {
      /* ignore */
    }
  }

  function applyRemote(msg, opts) {
    if (!msg || msg.from === tabId) {
      return;
    }
    opts = opts || {};
    applyingRemote = true;
    try {
      if (msg.playlist) {
        playlist = msg.playlist;
      }
      if (msg.queue) {
        queue = msg.queue;
      }
      if (typeof msg.replay === 'boolean') {
        replay = msg.replay;
      }
      if (opts.takeover) {
        isLeader = false;
        stopLocalAudio();
      }
      if (opts.takeover || !isLeader) {
        if (msg.current) {
          current = msg.current;
        }
        if (msg.currentSource) {
          currentSource = msg.currentSource;
        }
        if (typeof msg.currentTime === 'number') {
          lastTime = msg.currentTime;
        }
        if (typeof msg.duration === 'number' && msg.duration > 0) {
          lastDuration = msg.duration;
        }
        playing = !!msg.playing;
      }
      if (current) {
        showPlayer();
      }
      updateUi({ skipQueue: opts.skipQueue });
      paintProgress(lastTime, lastDuration);
    } finally {
      applyingRemote = false;
    }
  }

  function paintProgress(time, duration) {
    const bar = $('.cse-player .progress');
    const timeEl = $('.cse-player .time-current');
    if (bar) {
      bar.style.width = duration > 0 ? ((time / duration) * 100) + '%' : '0%';
    }
    if (timeEl) {
      timeEl.textContent = formatTime(time);
    }
  }

  function updateProgress() {
    if (!isLeader) {
      return;
    }
    lastTime = audio.currentTime || 0;
    if (isFinite(audio.duration) && audio.duration > 0) {
      lastDuration = audio.duration;
    }
    paintProgress(lastTime, lastDuration);
    const now = Date.now();
    if (now - lastTickAt >= TICK_MS) {
      lastTickAt = now;
      publish('tick');
    }
  }

  function updateQueueBadge() {
    const badge = $('.cse-player .queue-badge');
    if (!badge) {
      return;
    }
    if (queue.length > 0) {
      badge.textContent = String(queue.length);
      badge.style.display = 'inline-flex';
    } else {
      badge.textContent = '';
      badge.style.display = 'none';
    }
  }

  function updateQueueUi() {
    const list = $('.cse-queue-list');
    updateQueueBadge();
    syncEnqueueButtons();
    if (!list) {
      return;
    }
    if (queue.length === 0) {
      list.innerHTML = '<li class="cse-queue-empty">Queue is empty</li>';
      return;
    }
    list.innerHTML = queue.map(function (track, i) {
      const active = sameSrc(current, track) && playing ? ' active' : '';
      return '<li class="cse-queue-item' + active + '" data-index="' + i + '">' +
          '<span class="cse-queue-title"></span>' +
          '<span class="cse-queue-artist"></span>' +
          '<button type="button" class="cse-queue-remove" data-index="' + i + '">×</button>' +
          '</li>';
    }).join('');
    Array.from(list.querySelectorAll('.cse-queue-item')).forEach(function (item, i) {
      item.querySelector('.cse-queue-title').textContent = queue[i].title;
      item.querySelector('.cse-queue-artist').textContent = queue[i].artist;
      item.addEventListener('click', function (e) {
        if (e.target.closest('.cse-queue-remove')) {
          return;
        }
        playFromQueue(i);
      });
    });
    Array.from(list.querySelectorAll('.cse-queue-remove')).forEach(function (btn) {
      btn.addEventListener('click', function (e) {
        e.stopPropagation();
        removeFromQueue(parseInt(btn.dataset.index, 10));
      });
    });
  }

  function updateUi(opts) {
    opts = opts || {};
    document.querySelectorAll('.cse-track').forEach(function (row) {
      const on = current && sameSrc(row.dataset.src, current.src) && playing;
      row.classList.toggle('playing', on);
    });
    const playBtn = $('.cse-player .play-btn');
    if (playBtn) {
      playBtn.textContent = playing ? '⏸' : '▶';
    }
    const replayBtn = $('.cse-player .replay-btn');
    if (replayBtn) {
      replayBtn.classList.toggle('active', replay);
    }
    if (current) {
      const titleEl = $('.cse-player .title');
      const artistEl = $('.cse-player .artist');
      if (titleEl) {
        titleEl.textContent = current.title;
      }
      if (artistEl) {
        artistEl.textContent = current.artist;
      }
      showPlayer();
    }
    if (opts.skipQueue) {
      updateQueueBadge();
      syncEnqueueButtons();
      return;
    }
    updateQueueUi();
  }

  function seekAudioTo(seconds, thenPlay) {
    const go = function () {
      try {
        audio.currentTime = seconds || 0;
      } catch (e) {
        /* not seekable yet */
      }
      if (thenPlay) {
        const playPromise = audio.play();
        if (playPromise && playPromise.catch) {
          playPromise.catch(function () {
            switchingTrack = false;
            playing = false;
            updateUi();
            publish('state');
          });
        }
      } else {
        switchingTrack = false;
      }
    };
    if (audio.readyState >= 1) {
      go();
    } else {
      audio.addEventListener('loadedmetadata', go, { once: true });
    }
  }

  function playTrack(track, source, list, startTime) {
    if (!track || !track.src) {
      return;
    }
    becomeLeader();
    switchingTrack = true;
    current = plainTrack(track);
    currentSource = source;
    if (list) {
      playlist = plainList(list);
    }
    lastTime = startTime || 0;
    playing = true;
    audio.src = current.src;
    showPlayer();
    updateUi();
    seekAudioTo(lastTime, true);
    publish('takeover');
  }

  function playFromQueue(index) {
    if (index < 0 || index >= queue.length) {
      return;
    }
    playTrack(queue[index], 'queue', null, 0);
  }

  function playFromPlaylist(index) {
    if (index < 0 || index >= playlist.length) {
      return;
    }
    playTrack(playlist[index], 'playlist', playlist, 0);
  }

  function next() {
    if (!isLeader) {
      return;
    }
    if (queue.length > 0 && currentSource === 'queue') {
      const i = queue.findIndex(function (t) { return sameSrc(t, current); });
      const nextIndex = i + 1;
      if (nextIndex < queue.length) {
        playFromQueue(nextIndex);
      } else if (replay) {
        playFromQueue(0);
      } else {
        playing = false;
        updateUi();
        publish('state');
      }
      return;
    }
    if (playlist.length === 0) {
      playing = false;
      updateUi();
      publish('state');
      return;
    }
    const i = playlist.findIndex(function (t) { return sameSrc(t, current); });
    const nextIndex = i + 1;
    if (nextIndex < playlist.length) {
      playFromPlaylist(nextIndex);
    } else if (replay) {
      playFromPlaylist(0);
    } else {
      playing = false;
      updateUi();
      publish('state');
    }
  }

  function pauseLocal() {
    if (isLeader) {
      audio.pause();
    }
    playing = false;
    updateUi();
    persist();
  }

  function togglePlay() {
    if (!current) {
      if (queue.length > 0) {
        playFromQueue(0);
      } else if (playlist.length > 0) {
        playFromPlaylist(0);
      }
      return;
    }
    if (playing) {
      if (isLeader) {
        pauseLocal();
        publish('state');
      } else {
        post('command', { action: 'pause' });
        playing = false;
        updateUi();
      }
      return;
    }
    playTrack(current, currentSource, playlist, lastTime);
  }

  function seekTo(ratioOrTime, isRatio) {
    const duration = reportedDuration();
    if (!duration) {
      return;
    }
    const time = isRatio ? ratioOrTime * duration : ratioOrTime;
    lastTime = time;
    paintProgress(lastTime, duration);
    if (isLeader) {
      try {
        audio.currentTime = time;
      } catch (e) {
        /* ignore */
      }
      publish('tick');
    } else {
      post('command', { action: 'seek', currentTime: time });
    }
  }

  function enqueue(track, silent) {
    const plain = plainTrack(track);
    if (!plain) {
      return false;
    }
    if (queue.some(function (t) { return sameSrc(t, plain); })) {
      return false;
    }
    queue.push(plain);
    if (!silent) {
      updateQueueUi();
      showPlayer();
      publish('state');
    }
    return true;
  }

  function enqueueAll(list) {
    let added = 0;
    (list || []).forEach(function (track) {
      if (enqueue(track, true)) {
        added += 1;
      }
    });
    if (added > 0) {
      updateQueueUi();
      showPlayer();
      publish('state');
    } else {
      syncEnqueueButtons();
    }
    return added;
  }

  function syncEnqueueButtons() {
    document.querySelectorAll('.cse-playlist').forEach(function (playlistEl) {
      const rows = Array.from(playlistEl.querySelectorAll('.cse-track'));
      let queuedCount = 0;
      rows.forEach(function (row) {
        const inQueue = queue.some(function (track) { return sameSrc(row.dataset.src, track); });
        if (inQueue) {
          queuedCount += 1;
        }
        const btn = row.querySelector('.cse-enqueue');
        if (btn) {
          btn.classList.toggle('queued', inQueue);
          btn.setAttribute('aria-label', inQueue ? 'In queue' : 'Add to queue');
          btn.setAttribute('title', inQueue ? 'In queue' : 'Add to queue');
        }
      });
      const allBtn = playlistEl.querySelector('.cse-enqueue-all');
      if (allBtn) {
        const allQueued = rows.length > 0 && queuedCount === rows.length;
        allBtn.classList.toggle('queued', allQueued);
        allBtn.setAttribute('aria-label', allQueued ? 'Playlist is in queue' : 'Add playlist to queue');
        allBtn.setAttribute('title', allQueued ? 'Playlist is in queue' : 'Add playlist to queue');
      }
    });
  }

  function removeFromQueue(index) {
    if (index < 0 || index >= queue.length) {
      return;
    }
    const removed = queue.splice(index, 1)[0];
    if (sameSrc(removed, current) && currentSource === 'queue') {
      if (queue.length === 0) {
        if (isLeader) {
          audio.pause();
        }
        playing = false;
        current = null;
      } else if (isLeader) {
        playFromQueue(Math.min(index, queue.length - 1));
        return;
      }
    }
    updateQueueUi();
    updateUi();
    publish('state');
  }

  function clearQueue() {
    queue.length = 0;
    const panel = $('.cse-queue');
    if (panel) {
      panel.classList.remove('open');
    }
    updateQueueUi();
    publish('state');
  }

  function restoreFromStorage() {
    let saved = null;
    try {
      saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
    } catch (e) {
      saved = null;
    }
    if (!saved || !saved.current) {
      return;
    }
    current = saved.current;
    playlist = saved.playlist || [];
    queue = saved.queue || [];
    currentSource = saved.currentSource || 'playlist';
    replay = !!saved.replay;
    lastTime = saved.currentTime || 0;
    lastDuration = saved.duration || 0;
    playing = false;
    isLeader = false;
    showPlayer();
    updateUi();
    paintProgress(lastTime, lastDuration);
  }

  function onMessage(msg) {
    if (!msg || msg.from === tabId) {
      return;
    }
    if (msg.type === 'hello') {
      if (isLeader) {
        post('state');
      }
      return;
    }
    if (msg.type === 'bye') {
      if (isLeader) {
        return;
      }
      playing = false;
      try {
        const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
        if (saved && typeof saved.currentTime === 'number') {
          lastTime = saved.currentTime;
        }
        if (saved && typeof saved.duration === 'number' && saved.duration > 0) {
          lastDuration = saved.duration;
        }
      } catch (e) {
        /* ignore */
      }
      updateUi();
      paintProgress(lastTime, lastDuration);
      return;
    }
    if (msg.type === 'command') {
      if (!isLeader) {
        return;
      }
      if (msg.action === 'pause') {
        pauseLocal();
        publish('state');
      } else if (msg.action === 'seek' && typeof msg.currentTime === 'number') {
        seekTo(msg.currentTime, false);
      }
      return;
    }
    if (msg.type === 'takeover') {
      applyRemote(msg, { takeover: true });
      return;
    }
    if (msg.type === 'state' || msg.type === 'tick') {
      if (isLeader && msg.type === 'tick') {
        return;
      }
      applyRemote(msg, { takeover: false, skipQueue: msg.type === 'tick' });
    }
  }

  function bindChrome() {
    const playBtn = $('.cse-player .play-btn');
    if (playBtn) {
      playBtn.addEventListener('click', togglePlay);
    }
    const replayBtn = $('.cse-player .replay-btn');
    if (replayBtn) {
      replayBtn.addEventListener('click', function () {
        replay = !replay;
        updateUi();
        publish('state');
      });
    }
    const queueBtn = $('.cse-player .queue-btn');
    if (queueBtn) {
      queueBtn.addEventListener('click', function () {
        const panel = $('.cse-queue');
        if (panel) {
          panel.classList.toggle('open');
        }
        showPlayer();
      });
    }
    const clearBtn = $('.cse-queue .clear-queue-btn');
    if (clearBtn) {
      clearBtn.addEventListener('click', clearQueue);
    }
    const progressBar = $('.cse-player .progress-bar');
    if (progressBar) {
      progressBar.addEventListener('click', function (e) {
        const rect = progressBar.getBoundingClientRect();
        const ratio = (e.clientX - rect.left) / rect.width;
        if (ratio >= 0 && ratio <= 1) {
          seekTo(ratio, true);
        }
      });
    }
  }

  function bindPlaylists() {
    document.querySelectorAll('.cse-playlist').forEach(function (playlistEl) {
      if (playlistEl.dataset.cseBound === '1') {
        return;
      }
      playlistEl.dataset.cseBound = '1';
      const list = tracksIn(playlistEl);
      playlistEl.querySelectorAll('.cse-track').forEach(function (row, index) {
        row.addEventListener('click', function (e) {
          if (e.target.closest('.cse-enqueue')) {
            return;
          }
          playTrack(list[index], 'playlist', list, 0);
        });
      });
      playlistEl.querySelectorAll('.cse-enqueue').forEach(function (btn, index) {
        btn.addEventListener('click', function (e) {
          e.stopPropagation();
          enqueue(list[index]);
          syncEnqueueButtons();
        });
      });
      const allBtn = playlistEl.querySelector('.cse-enqueue-all');
      if (allBtn) {
        allBtn.addEventListener('click', function (e) {
          e.stopPropagation();
          enqueueAll(list);
        });
      }
    });
  }

  audio.addEventListener('timeupdate', updateProgress);
  audio.addEventListener('ended', next);
  audio.addEventListener('play', function () {
    switchingTrack = false;
    if (!isLeader) {
      return;
    }
    playing = true;
    updateUi({ skipQueue: true });
  });
  audio.addEventListener('pause', function () {
    if (!isLeader || applyingRemote || switchingTrack || audio.ended) {
      return;
    }
    playing = false;
    updateUi({ skipQueue: true });
    publish('state');
  });

  if (channel) {
    channel.onmessage = function (event) {
      onMessage(event.data);
    };
  } else {
    window.addEventListener('storage', function (event) {
      if (event.key !== STORAGE_KEY || !event.newValue) {
        return;
      }
      try {
        const msg = JSON.parse(event.newValue);
        if (!msg || msg.from === tabId) {
          return;
        }
        if (isLeader && msg.playing) {
          applyRemote(msg, { takeover: true });
        } else {
          applyRemote(msg, { takeover: false });
        }
      } catch (e) {
        /* ignore */
      }
    });
  }
  window.addEventListener('pagehide', function () {
    persist();
    if (isLeader) {
      post('bye');
    }
  });
  window.addEventListener('pageshow', function (event) {
    if (!event.persisted) {
      return;
    }
    isLeader = false;
    switchingTrack = false;
    stopLocalAudio();
    restoreFromStorage();
    post('hello');
  });
  document.addEventListener('visibilitychange', function () {
    if (document.visibilityState === 'hidden' && isLeader) {
      persist();
    }
  });

  function refresh() {
    bindPlaylists();
    syncEnqueueButtons();
    updateUi();
  }

  document.addEventListener('DOMContentLoaded', function () {
    bindChrome();
    bindPlaylists();
    if (window.MutationObserver) {
      new MutationObserver(function () {
        bindPlaylists();
        syncEnqueueButtons();
      }).observe(document.documentElement, { childList: true, subtree: true });
    }
    restoreFromStorage();
    post('hello');
    updateUi();
  });

  window.CsePlayer = {
    refresh: refresh,
    pause: pauseLocal
  };
})();
