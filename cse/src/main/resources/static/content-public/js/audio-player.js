class PersistentAudioPlayer {
  constructor() {
    this.audio = new Audio();
    this.currentTrack = null;
    this.playlist = [];
    this.queue = [];
    this.isPlaying = false;
    this.currentSource = 'playlist';
    this.isTransitioning = false;
    this.replayEnabled = false;
    this.channel = new BroadcastChannel('audio_player_sync');
    
    this.loadState();
    this.setupEventListeners();
    this.setupChannelSync();
  }
  
  loadState() {
    const saved = localStorage.getItem('audioPlayerState');
    if (saved) {
      try {
        const state = JSON.parse(saved);
        this.volume = state.volume ?? 0.7;
        this.audio.volume = this.volume;
        this.queue = state.queue || [];
        this.replayEnabled = state.replayEnabled ?? false;
      } catch (e) {
        this.volume = 0.7;
        this.queue = [];
        this.replayEnabled = false;
      }
    } else {
      this.volume = 0.7;
      this.queue = [];
      this.replayEnabled = false;
    }
  }
  
  saveState() {
    const state = {
      currentTrack: this.currentTrack,
      playlist: this.playlist,
      queue: this.queue,
      volume: this.volume,
      replayEnabled: this.replayEnabled,
      timestamp: Date.now()
    };
    localStorage.setItem('audioPlayerState', JSON.stringify(state));
  }
  
  setupChannelSync() {
    this.channel.onmessage = (event) => {
      const { type, track, playlist, position, volume, queue } = event.data;
      
      if (type === 'state_sync') {
        this.playlist = playlist || this.playlist;
        if (track) {
          this.currentTrack = track;
          if (this.audio.src !== track.src) {
            this.audio.src = track.src;
            this.audio.currentTime = position || 0;
          }
        }
        this.volume = volume ?? this.volume;
        this.audio.volume = this.volume;
        if (queue) this.queue = queue;
        this.updateUI();
      }
      
      if (type === 'play') {
        this.audio.play();
        this.isPlaying = true;
        this.updateUI();
      }
      
      if (type === 'pause') {
        this.audio.pause();
        this.isPlaying = false;
        this.updateUI();
      }
      
      if (type === 'seek') {
        this.audio.currentTime = position;
      }
      
      if (type === 'queue_update') {
        this.queue = queue;
        this.updateQueueUI();
      }
    };
  }
  
  broadcastState() {
    this.channel.postMessage({
      type: 'state_sync',
      track: this.currentTrack,
      playlist: this.playlist,
      queue: this.queue,
      position: this.audio.currentTime,
      volume: this.volume
    });
  }
  
  broadcastQueueUpdate() {
    this.channel.postMessage({ type: 'queue_update', queue: this.queue });
  }
  
  setupEventListeners() {
    this.audio.addEventListener('timeupdate', () => {
      this.updateProgress();
      this.saveState();
    });
    
    this.audio.addEventListener('ended', () => {
      if (this.queue.length > 0) {
        this.nextInQueue();
      } else {
        this.next();
      }
    });
    
    this.audio.addEventListener('play', () => {
      this.isPlaying = true;
      this.updateUI();
    });
    
    this.audio.addEventListener('pause', () => {
      this.isPlaying = false;
      this.updateUI();
    });
    
    this.audio.addEventListener('loadedmetadata', () => {
      this.updateTrackDurations();
    });
  }
  
  toggleReplay() {
    this.replayEnabled = !this.replayEnabled;
    this.saveState();
    this.updateReplayButton();
    if (this.replayEnabled && this.queue.length > 0) {
      this.playFromQueue(0);
    } else if (this.replayEnabled && this.playlist.length > 0) {
      this.playTrack(0);
    }
    return this.replayEnabled;
  }
  
  updateReplayButton() {
    const btn = document.querySelector('.replay-btn');
    if (btn) {
      btn.classList.toggle('active', this.replayEnabled);
    }
  }
  
  updateTrackDurations() {
    document.querySelectorAll('.track-item[data-src]').forEach((item) => {
      const src = item.dataset.src;
      const durationEl = item.querySelector('.track-duration');
      if (durationEl && !durationEl.textContent) {
        const audio = new Audio();
        audio.src = src;
        audio.addEventListener('loadedmetadata', () => {
          if (!isNaN(audio.duration)) {
            durationEl.textContent = this.formatTime(audio.duration);
          }
        });
      }
    });
  }
  
  async loadPlaylistDurations(tracks) {
    let total = 0;
    const durationEls = document.querySelectorAll('.track-item .track-duration');
    
    for (let i = 0; i < tracks.length; i++) {
      const audio = new Audio();
      audio.src = tracks[i].src;
      
      await new Promise((resolve) => {
        audio.addEventListener('loadedmetadata', () => {
          if (!isNaN(audio.duration)) {
            total += audio.duration;
            if (durationEls[i]) {
              durationEls[i].textContent = this.formatTime(audio.duration);
            }
          }
          resolve();
        });
        audio.addEventListener('error', resolve);
        setTimeout(resolve, 2000);
      });
    }
    
    const headerEl = document.querySelector('.playlist-duration');
    if (headerEl && total > 0) {
      headerEl.textContent = `Total: ${this.formatTime(total)}`;
    }
    
    return total;
  }
  
  loadPlaylist(tracks, currentIndex = 0) {
    this.playlist = tracks;
    this.saveState();
    this.broadcastState();
  }
  
  addToQueue(track) {
    const exists = this.queue.some(t => t.src === track.src);
    if (exists) return false;
    const wasEmpty = this.queue.length === 0;
    this.queue.push(track);
    this.saveState();
    this.broadcastQueueUpdate();
    this.updateQueueUI();
    this.showPlayer();
    if (wasEmpty) {
      this.playFromQueue(0);
    }
    return true;
  }
  
  showPlayer() {
    const player = document.querySelector('.fixed-player');
    if (player) player.classList.add('visible');
  }
  
  hidePlayer() {
    const player = document.querySelector('.fixed-player');
    if (player) player.classList.remove('visible');
  }
  
  addAllToQueue(tracks) {
    const wasEmpty = this.queue.length === 0;
    tracks.forEach(track => {
      if (!this.queue.some(t => t.src === track.src)) {
        this.queue.push(track);
      }
    });
    this.saveState();
    this.broadcastQueueUpdate();
    this.updateQueueUI();
    if (wasEmpty) {
      this.playFromQueue(0);
    }
    this.showPlayer();
  }
  
  removeFromQueue(index) {
    if (index >= 0 && index < this.queue.length) {
      this.queue.splice(index, 1);
      this.saveState();
      this.broadcastQueueUpdate();
      this.updateQueueUI();
    }
  }
  
  clearQueue() {
    this.queue = [];
    this.saveState();
    this.broadcastQueueUpdate();
    this.updateQueueUI();
  }
  
  playFromQueue(index) {
    if (index >= 0 && index < this.queue.length) {
      const track = this.queue[index];
      this.currentTrack = track;
      this.audio.src = track.src;
      this.audio.play();
      this.isPlaying = true;
      this.currentSource = 'queue';
      this.saveState();
      this.broadcastState();
      this.updateUI();
      this.updateQueueUI();
    }
  }
  
  nextInQueue() {
    if (this.queue.length > 0) {
      const currentIndex = this.queue.findIndex(t => t.src === this.currentTrack?.src);
      if (currentIndex === -1) {
        this.playFromQueue(0);
      } else {
        const nextIndex = currentIndex + 1;
        if (nextIndex >= this.queue.length) {
          if (this.replayEnabled) {
            this.playFromQueue(0);
          } else {
            this.isPlaying = false;
            this.updateUI();
            this.updateQueueUI();
          }
        } else {
          this.playFromQueue(nextIndex);
        }
      }
    } else if (this.playlist.length > 0) {
      this.next();
    }
  }
  
  prevInQueue() {
    if (this.isTransitioning) return;
    this.isTransitioning = true;
    if (this.queue.length > 0) {
      const currentIndex = this.queue.findIndex(t => t.src === this.currentTrack?.src);
      if (currentIndex === -1) {
        this.playFromQueue(this.queue.length - 1);
      } else {
        const prevIndex = currentIndex <= 0 ? this.queue.length - 1 : currentIndex - 1;
        this.playFromQueue(prevIndex);
      }
    } else if (this.playlist.length > 0) {
      this.prev();
    }
    setTimeout(() => { this.isTransitioning = false; }, 300);
  }
  
  playTrack(index) {
    if (index < 0 || index >= this.playlist.length) return;
    
    const track = this.playlist[index];
    this.currentTrack = track;
    this.audio.src = track.src;
    
    const savedState = localStorage.getItem('audioPlayerState');
    if (savedState) {
      try {
        const state = JSON.parse(savedState);
        if (state.currentTrack && state.currentTrack.src === track.src) {
          this.audio.currentTime = state.currentTrack.currentTime || 0;
        }
      } catch (e) {}
    }
    
    this.showPlayer();
    this.audio.play();
    this.isPlaying = true;
    this.currentSource = 'playlist';
    this.saveState();
    this.broadcastState();
    this.updateUI();
  }
  
  togglePlay() {
    if (!this.audio.src) return;
    
    if (this.isPlaying) {
      this.audio.pause();
      this.channel.postMessage({ type: 'pause' });
    } else {
      if (this.audio.ended || this.audio.currentTime >= this.audio.duration - 0.5) {
        if (this.currentSource === 'queue' && this.queue.length > 0) {
          this.playFromQueue(0);
        } else {
          this.audio.currentTime = 0;
          this.audio.play();
          this.isPlaying = true;
          this.updateUI();
          this.channel.postMessage({ type: 'play' });
        }
      } else {
        this.audio.play();
        this.channel.postMessage({ type: 'play' });
      }
    }
  }
  
  next() {
    if (this.playlist.length === 0) return;
    const currentIndex = this.playlist.findIndex(t => t.src === this.currentTrack?.src);
    const nextIndex = currentIndex + 1;
    if (nextIndex >= this.playlist.length) {
      if (this.replayEnabled) {
        this.playTrack(0);
      }
    } else {
      this.playTrack(nextIndex);
    }
  }
  
  prev() {
    if (this.playlist.length === 0) return;
    const currentIndex = this.playlist.findIndex(t => t.src === this.currentTrack?.src);
    const prevIndex = currentIndex <= 0 ? this.playlist.length - 1 : currentIndex - 1;
    this.playTrack(prevIndex);
  }
  
  seek(time) {
    this.audio.currentTime = time;
    this.channel.postMessage({ type: 'seek', position: time });
  }
  
  setVolume(vol) {
    this.volume = vol;
    this.audio.volume = vol;
    this.saveState();
    this.broadcastState();
  }
  
  updateProgress() {
    const progressBar = document.querySelector('.progress');
    const currentTimeEl = document.querySelector('.time-current');
    const durationEl = document.querySelector('.time-duration');
    
    if (progressBar && this.audio.duration) {
      const percent = (this.audio.currentTime / this.audio.duration) * 100;
      progressBar.style.width = percent + '%';
    }
    
    if (currentTimeEl) {
      currentTimeEl.textContent = this.formatTime(this.audio.currentTime);
    }
    if (durationEl) {
      durationEl.textContent = this.formatTime(this.audio.duration);
    }
  }
  
  formatTime(seconds) {
    if (isNaN(seconds)) return '0:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
  
  updateUI() {
    document.querySelectorAll('.track-item').forEach((el, i) => {
      const isCurrentTrack = this.playlist[i]?.src === this.currentTrack?.src;
      el.classList.toggle('active', isCurrentTrack && this.isPlaying);
      el.classList.toggle('playing', isCurrentTrack && this.isPlaying);
    });
    
    const playBtn = document.querySelector('.play-btn');
    if (playBtn) {
      playBtn.textContent = this.isPlaying ? '⏸' : '▶';
    }
    
    if (this.currentTrack) {
      const titleEl = document.querySelector('.fixed-player .title');
      const artistEl = document.querySelector('.fixed-player .artist');
      if (titleEl) titleEl.textContent = this.currentTrack.title;
      if (artistEl) artistEl.textContent = this.currentTrack.artist;
    }
    
    const volumeSlider = document.querySelector('.volume-control input');
    if (volumeSlider) {
      volumeSlider.value = this.volume;
    }
    
    this.updateQueueBadge();
    this.updateQueueUI();
  }
  
  updateQueueBadge() {
    const badge = document.querySelector('.queue-badge');
    if (badge) {
      if (this.queue.length > 0) {
        badge.textContent = this.queue.length;
        badge.style.display = 'inline';
      } else {
        badge.style.display = 'none';
      }
    }
  }
  
  updateQueueUI() {
    this.updateQueueBadge();
    
    const queueList = document.querySelector('.queue-list');
    if (!queueList) return;
    
    if (this.queue.length === 0) {
      queueList.innerHTML = '<div class="queue-empty">Queue is empty</div>';
      return;
    }
    
    queueList.innerHTML = this.queue.map((track, i) => `
      <li class="queue-item ${this.currentTrack?.src === track.src && this.isPlaying ? 'active' : ''}" data-index="${i}">
        <span class="queue-item-title">${track.title}</span>
        <span class="queue-item-artist">${track.artist}</span>
        <button class="queue-remove" data-index="${i}">×</button>
      </li>
    `).join('');
    
    queueList.querySelectorAll('.queue-item').forEach(item => {
      item.addEventListener('click', (e) => {
        if (!e.target.classList.contains('queue-remove')) {
          this.playFromQueue(parseInt(item.dataset.index));
        }
      });
    });
    
    queueList.querySelectorAll('.queue-remove').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        this.removeFromQueue(parseInt(btn.dataset.index));
      });
    });
  }
  
  toggleQueuePanel() {
    const panel = document.querySelector('.queue-panel');
    if (panel) {
      panel.classList.toggle('open');
    }
  }
  
  restoreFromStorage() {
    const saved = localStorage.getItem('audioPlayerState');
    if (saved) {
      try {
        const state = JSON.parse(saved);
        if (state.queue && state.queue.length > 0) {
          this.queue = state.queue;
        }
        if (state.playlist && state.playlist.length > 0) {
          this.playlist = state.playlist;
          if (state.currentTrack) {
            this.currentTrack = state.currentTrack;
            this.audio.src = state.currentTrack.src;
            this.audio.currentTime = state.currentTrack.currentTime || 0;
            this.volume = state.volume ?? 0.7;
            this.audio.volume = this.volume;
            this.showPlayer();
            this.updateUI();
          }
        }
        this.updateQueueUI();
        this.updateReplayButton();
      } catch (e) {
        console.error('Failed to restore state:', e);
      }
    }
  }
}

const player = new PersistentAudioPlayer();

document.addEventListener('DOMContentLoaded', () => {
  const volumeSlider = document.querySelector('.volume-control input');
  if (volumeSlider) {
    volumeSlider.value = player.volume;
    volumeSlider.addEventListener('input', (e) => {
      player.setVolume(parseFloat(e.target.value));
    });
  }
  
  const playBtn = document.querySelector('.play-btn');
  if (playBtn) {
    playBtn.addEventListener('click', () => player.togglePlay());
  }
  
  const prevBtn = document.querySelector('.prev-btn');
  if (prevBtn) {
    prevBtn.addEventListener('click', () => player.prevInQueue());
  }
  
  const nextBtn = document.querySelector('.next-btn');
  if (nextBtn) {
    nextBtn.addEventListener('click', () => player.nextInQueue());
  }
  
  const replayBtn = document.querySelector('.replay-btn');
  if (replayBtn) {
    replayBtn.addEventListener('click', () => player.toggleReplay());
  }
  
  const queueBtn = document.querySelector('.queue-btn');
  if (queueBtn) {
    queueBtn.addEventListener('click', () => player.toggleQueuePanel());
  }
  
  const clearQueueBtn = document.querySelector('.clear-queue-btn');
  if (clearQueueBtn) {
    clearQueueBtn.addEventListener('click', () => player.clearQueue());
  }
  
  const progressBar = document.querySelector('.progress-bar');
  if (progressBar) {
    progressBar.addEventListener('click', (e) => {
      const rect = progressBar.getBoundingClientRect();
      const percent = (e.clientX - rect.left) / rect.width;
      player.seek(percent * player.audio.duration);
    });
  }
  
  player.restoreFromStorage();
});
