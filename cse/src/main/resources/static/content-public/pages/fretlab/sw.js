/* Fretboard Lab — shell cache for offline / PWA.
 * Precaches the HTML chrome + CSS; caches JS / fonts / i18n / icons on first fetch.
 * Bump CACHE when shipping a breaking static layout change.
 */
const CACHE = 'fretlab-shell-v1';
const PRECACHE = [
  './',
  './index.html',
  './favicon.ico',
  './manifest.webmanifest',
  './css/fonts.css',
  './css/style.css',
  './css/dark.css',
  './css/light.css',
  './icons/icon-192.png',
  './icons/icon-512.png'
];

function sameOrigin(url) {
  try {
    return new URL(url).origin === self.location.origin;
  } catch (e) {
    return false;
  }
}

function shouldCachePath(pathname) {
  return /\/(css|js|fonts|i18n|icons)\//.test(pathname) ||
    pathname.endsWith('.webmanifest') ||
    pathname.endsWith('favicon.ico') ||
    pathname.endsWith('index.html') ||
    pathname.endsWith('/');
}

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(PRECACHE)).then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET' || !sameOrigin(req.url)) return;

  event.respondWith((async () => {
    const cache = await caches.open(CACHE);
    const cached = await cache.match(req);
    try {
      const res = await fetch(req);
      if (res && res.ok && shouldCachePath(new URL(req.url).pathname)) {
        cache.put(req, res.clone());
      }
      return res;
    } catch (err) {
      if (cached) return cached;
      if (req.mode === 'navigate') {
        const shell = await cache.match('./index.html');
        if (shell) return shell;
      }
      throw err;
    }
  })());
});
