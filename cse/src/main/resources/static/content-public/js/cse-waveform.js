(function (global) {
    function samplesFromBase64(encoded) {
        var binary;
        try {
            binary = atob(encoded);
        } catch (err) {
            return null;
        }
        var raw = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) {
            raw[i] = binary.charCodeAt(i);
        }
        return new Int8Array(raw.buffer);
    }

    function strokeColor(canvas) {
        var color = getComputedStyle(canvas).color;
        return color && color !== 'rgba(0, 0, 0, 0)' ? color : '#e0b15a';
    }

    function draw(canvas) {
        var encoded = canvas.getAttribute('data-waveform');
        if (!encoded) {
            return;
        }
        var samples = canvas._waveformSamples;
        if (!samples) {
            samples = samplesFromBase64(encoded);
            if (!samples || samples.length === 0) {
                return;
            }
            canvas._waveformSamples = samples;
        }
        var cssW = canvas.clientWidth;
        var cssH = canvas.clientHeight;
        if (cssW < 1 || cssH < 1) {
            return;
        }
        var ctx = canvas.getContext('2d');
        if (!ctx) {
            return;
        }
        var dpr = window.devicePixelRatio || 1;
        var backingW = Math.max(1, Math.round(cssW * dpr));
        var backingH = Math.max(1, Math.round(cssH * dpr));
        if (canvas.width !== backingW) {
            canvas.width = backingW;
        }
        if (canvas.height !== backingH) {
            canvas.height = backingH;
        }
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, cssW, cssH);
        ctx.globalAlpha = 0.9;
        ctx.fillStyle = strokeColor(canvas);
        var n = samples.length;
        var mid = cssH / 2;
        var amp = Math.max(mid - 0.5, 1);
        for (var x = 0; x < cssW; x++) {
            var start = Math.floor(x * n / cssW);
            var end = Math.floor((x + 1) * n / cssW);
            if (end <= start) {
                end = Math.min(start + 1, n);
            }
            var lo = 127;
            var hi = -128;
            for (var i = start; i < end; i++) {
                var sample = samples[i];
                if (sample < lo) {
                    lo = sample;
                }
                if (sample > hi) {
                    hi = sample;
                }
            }
            var yTop = mid - (hi / 128) * amp;
            var yBot = mid - (lo / 128) * amp;
            ctx.fillRect(x, yTop, 1, Math.max(1, yBot - yTop));
        }
    }

    var resizeObserver = window.ResizeObserver ? new ResizeObserver(function (entries) {
        entries.forEach(function (entry) {
            draw(entry.target);
        });
    }) : null;
    var seen = typeof WeakSet === 'function' ? new WeakSet() : null;
    var listeningResize = false;

    function watchCanvas(canvas) {
        if (!canvas || (seen && seen.has(canvas))) {
            return;
        }
        if (seen) {
            seen.add(canvas);
        }
        if (resizeObserver) {
            resizeObserver.observe(canvas);
        }
        draw(canvas);
        if (window.requestAnimationFrame) {
            window.requestAnimationFrame(function () {
                draw(canvas);
            });
        }
    }

    function observe(root) {
        var scope = root && root.querySelectorAll ? root : document;
        scope.querySelectorAll('canvas[data-waveform]').forEach(watchCanvas);
        if (!resizeObserver && !listeningResize) {
            listeningResize = true;
            window.addEventListener('resize', function () {
                document.querySelectorAll('canvas[data-waveform]').forEach(draw);
            });
        }
    }

    function start() {
        observe(document);
        if (window.MutationObserver) {
            new MutationObserver(function () {
                observe(document);
            }).observe(document.documentElement, { childList: true, subtree: true });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }

    global.CseWaveform = {
        draw: draw,
        observe: observe
    };
})(window);
