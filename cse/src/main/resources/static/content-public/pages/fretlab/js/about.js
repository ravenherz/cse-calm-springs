(function () {
  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function safeHref(href) {
    const h = String(href || '').trim();
    if (!h) return null;
    if (/^https?:\/\//i.test(h)) return h;
    if (/^mailto:/i.test(h)) return h;
    if (/^[a-z0-9_./#?&=%-]+$/i.test(h) && h.indexOf('..') < 0) return h;
    return null;
  }

  function inlineMd(text) {
    let s = esc(text);
    s = s.replace(/`([^`]+)`/g, function (_, code) {
      return '<code>' + code + '</code>';
    });
    s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, function (_, label, href) {
      const safe = safeHref(href);
      if (!safe) return esc(label);
      const ext = /^https?:\/\//i.test(safe);
      return '<a href="' + esc(safe) + '"' + (ext ? ' target="_blank" rel="noopener noreferrer"' : '') + '>' + label + '</a>';
    });
    return s;
  }

  function markdownToHtml(md) {
    const lines = String(md || '').replace(/\r\n/g, '\n').split('\n');
    const out = [];
    let i = 0;
    let inList = false;

    function closeList() {
      if (inList) {
        out.push('</ul>');
        inList = false;
      }
    }

    while (i < lines.length) {
      const line = lines[i];
      const trimmed = line.trim();

      if (!trimmed) {
        closeList();
        i++;
        continue;
      }

      if (/^---+$/.test(trimmed)) {
        closeList();
        out.push('<hr>');
        i++;
        continue;
      }

      const h = /^(#{1,3})\s+(.+)$/.exec(trimmed);
      if (h) {
        closeList();
        const level = h[1].length;
        out.push('<h' + level + '>' + inlineMd(h[2]) + '</h' + level + '>');
        i++;
        continue;
      }

      if (/^[-*]\s+/.test(trimmed)) {
        if (!inList) {
          out.push('<ul>');
          inList = true;
        }
        out.push('<li>' + inlineMd(trimmed.replace(/^[-*]\s+/, '')) + '</li>');
        i++;
        continue;
      }

      closeList();
      const para = [trimmed];
      i++;
      while (i < lines.length) {
        const next = lines[i].trim();
        if (!next || /^---+$/.test(next) || /^#{1,3}\s+/.test(next) || /^[-*]\s+/.test(next)) break;
        para.push(next);
        i++;
      }
      out.push('<p>' + inlineMd(para.join(' ')) + '</p>');
    }

    closeList();
    return out.join('\n');
  }

  window.FretAbout = {
    markdownToHtml: markdownToHtml
  };
})();
