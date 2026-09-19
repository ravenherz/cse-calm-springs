(function () {
  const bound = new WeakSet();

  function pad(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function formatTime(seconds) {
    if (!isFinite(seconds) || seconds < 0) {
      return '0:00';
    }
    const total = Math.floor(seconds);
    const h = Math.floor(total / 3600);
    const m = Math.floor((total % 3600) / 60);
    const s = total % 60;
    if (h > 0) {
      return h + ':' + pad(m) + ':' + pad(s);
    }
    return m + ':' + pad(s);
  }

  function pauseAudio() {
    if (window.CsePlayer && typeof window.CsePlayer.pause === 'function') {
      window.CsePlayer.pause();
    }
  }

  function pauseOthers(current) {
    document.querySelectorAll('.cse-video video').forEach(function (video) {
      if (video !== current && !video.paused) {
        video.pause();
      }
    });
  }

  function bindFigure(figure) {
    if (!figure || bound.has(figure) || figure.classList.contains('cse-video-processing')) {
      return;
    }
    const video = figure.querySelector('video');
    const frame = figure.querySelector('.cse-video-frame') || figure;
    if (!video) {
      return;
    }
    bound.add(figure);
    video.removeAttribute('controls');
    video.setAttribute('playsinline', '');
    video.setAttribute('preload', 'metadata');

    const bar = document.createElement('div');
    bar.className = 'cse-video-controls';
    bar.innerHTML = '<button type="button" class="cse-video-play" title="Play">▶</button>'
        + '<div class="cse-video-progress"><div class="cse-video-progress-fill"></div></div>'
        + '<span class="cse-video-time">0:00</span>'
        + '<button type="button" class="cse-video-mute" title="Mute">♪</button>'
        + '<button type="button" class="cse-video-full" title="Fullscreen">⛶</button>';
    frame.appendChild(bar);

    const playBtn = bar.querySelector('.cse-video-play');
    const fill = bar.querySelector('.cse-video-progress-fill');
    const timeEl = bar.querySelector('.cse-video-time');
    const muteBtn = bar.querySelector('.cse-video-mute');
    const fullBtn = bar.querySelector('.cse-video-full');
    const progress = bar.querySelector('.cse-video-progress');

    function sync() {
      const playing = !video.paused && !video.ended;
      playBtn.textContent = playing ? '❚❚' : '▶';
      playBtn.title = playing ? 'Pause' : 'Play';
      figure.classList.toggle('is-playing', playing);
      const duration = video.duration;
      const ratio = duration ? (video.currentTime / duration) : 0;
      fill.style.width = Math.max(0, Math.min(1, ratio)) * 100 + '%';
      timeEl.textContent = formatTime(video.currentTime)
          + (isFinite(duration) ? ' / ' + formatTime(duration) : '');
      muteBtn.textContent = video.muted || video.volume === 0 ? '×' : '♪';
    }

    playBtn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      if (video.paused) {
        pauseOthers(video);
        pauseAudio();
        video.play().catch(function () {});
      } else {
        video.pause();
      }
    });

    muteBtn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      video.muted = !video.muted;
      sync();
    });

    fullBtn.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      const node = frame;
      if (!document.fullscreenElement) {
        if (node.requestFullscreen) {
          node.requestFullscreen();
        } else if (video.webkitEnterFullscreen) {
          video.webkitEnterFullscreen();
        }
      } else if (document.exitFullscreen) {
        document.exitFullscreen();
      }
    });

    progress.addEventListener('click', function (e) {
      e.preventDefault();
      e.stopPropagation();
      const rect = progress.getBoundingClientRect();
      if (!rect.width || !isFinite(video.duration)) {
        return;
      }
      video.currentTime = ((e.clientX - rect.left) / rect.width) * video.duration;
    });

    video.addEventListener('play', function () {
      pauseOthers(video);
      pauseAudio();
      sync();
    });
    video.addEventListener('pause', sync);
    video.addEventListener('timeupdate', sync);
    video.addEventListener('loadedmetadata', sync);
    video.addEventListener('volumechange', sync);
    video.addEventListener('ended', sync);
    frame.addEventListener('click', function (e) {
      if (e.target.closest('.cse-video-controls')) {
        return;
      }
      if (video.paused) {
        pauseOthers(video);
        pauseAudio();
        video.play().catch(function () {});
      } else {
        video.pause();
      }
    });
    sync();
  }

  function refresh() {
    document.querySelectorAll('.cse-video').forEach(bindFigure);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', refresh);
  } else {
    refresh();
  }
  if (window.MutationObserver) {
    new MutationObserver(refresh).observe(document.documentElement, { childList: true, subtree: true });
  }

  window.CseVideoPlayer = {
    refresh: refresh
  };
})();
