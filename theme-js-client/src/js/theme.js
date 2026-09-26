/**
 * JS public shell. Fetches GET /rest/site (same query as GET /).
 * Layout matches the modern theme. Contract: /editor/api.
 */
(function () {
  function text(value) {
    return value == null ? '' : String(value);
  }

  function escapeHtml(value) {
    return text(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function qs(id) {
    return document.getElementById(id);
  }

  function setHidden(el, hidden) {
    if (el) {
      el.hidden = hidden;
    }
  }

  function pad2(n) {
    return n < 10 ? '0' + n : String(n);
  }

  function pageHref(page) {
    if (!page) {
      return './';
    }
    if (page.href) {
      return page.href;
    }
    return page.album
      ? './?album=' + encodeURIComponent(page.uri)
      : './?page=' + encodeURIComponent(page.uri);
  }

  function pageTitle(page) {
    return page.header || page.title || page.uri || '';
  }

  function isFeatured(section) {
    if (section.featured === true) {
      return true;
    }
    if (section.featured === false) {
      return false;
    }
    return !section.hasMore && (section.kind === 'about'
        || (section.pages && section.pages.length === 1));
  }

  function renderNav(site) {
    var nav = qs('site-nav');
    if (!nav) {
      return;
    }
    nav.innerHTML = '';
    if (site.view === 'error') {
      return;
    }
    (site.nav || []).forEach(function (cat) {
      var a = document.createElement('a');
      a.href = site.view === 'home' ? '#s-' + cat.slug : './#s-' + cat.slug;
      a.textContent = cat.title;
      nav.appendChild(a);
    });
  }

  function featureArticle(page, cls) {
    var href = pageHref(page);
    var html = '<article class="' + cls + '">';
    if (page.image) {
      html += '<a class="feature-media" href="' + escapeHtml(href) + '">'
        + '<img src="' + escapeHtml(page.image) + '" alt=""></a>';
    }
    html += '<div class="feature-copy"><h3><a href="' + escapeHtml(href) + '">'
      + escapeHtml(pageTitle(page)) + '</a></h3>';
    if (page.subHeader) {
      html += '<p class="feature-sub">' + escapeHtml(page.subHeader) + '</p>';
    }
    if (page.html) {
      html += '<div class="piece-body item-description-content">' + page.html + '</div>';
    }
    html += '</div></article>';
    return html;
  }

  function pieceCard(page, selectedTag) {
    var href = pageHref(page);
    var html = '<a class="piece-card" href="' + escapeHtml(href) + '">';
    if (page.image) {
      html += '<div class="piece-media"><img src="' + escapeHtml(page.image) + '" alt="">';
      if (page.tags && page.tags.length) {
        html += '<div class="piece-tags">';
        page.tags.forEach(function (tag) {
          html += '<span class="item-tag' + (tag === selectedTag ? ' selected' : '') + '">'
            + escapeHtml(tag) + '</span>';
        });
        html += '</div>';
      }
      html += '</div>';
    }
    html += '<h3>' + escapeHtml(pageTitle(page)) + '</h3>';
    if (page.subHeader) {
      html += '<p>' + escapeHtml(page.subHeader) + '</p>';
    }
    html += '</a>';
    return html;
  }

  function sectionHead(indexLabel, title, leadHtml) {
    return '<header class="section-head"><span class="section-index">'
      + escapeHtml(indexLabel) + '</span><div><h2>'
      + escapeHtml(title) + '</h2>' + (leadHtml || '') + '</div></header>';
  }

  function pieceGrid(pages, site, moreHtml) {
    var html = '<div class="piece-grid">';
    (pages || []).forEach(function (page) {
      html += pieceCard(page, site.selectedTag);
    });
    if (moreHtml) {
      html += moreHtml;
    }
    html += '</div>';
    return html;
  }

  function renderHome(site) {
    var html = '';
    if (site.introPage) {
      html += featureArticle(site.introPage, 'feature intro-feature');
    } else {
      var copy = site.copy || {};
      html += '<section class="hero"><div class="hero-copy">';
      html += '<div class="hero-title"><h4><b>'
        + escapeHtml(copy['welcome-title'] || 'Welcome') + '</b></h4></div>';
      if (copy['welcome-message']) {
        html += '<div class="hero-lede"><h6>' + escapeHtml(copy['welcome-message']) + '</h6></div>';
      }
      if (copy['welcome-description']) {
        html += '<div class="hero-body"><p>' + escapeHtml(copy['welcome-description']) + '</p></div>';
      }
      html += '</div></section>';
    }
    (site.sections || []).forEach(function (section, i) {
      html += '<section class="studio-section" id="s-' + escapeHtml(section.slug || '')
        + '" data-kind="' + escapeHtml(section.kind || 'default') + '">';
      html += sectionHead(pad2(i + 1), section.title || '',
          section.description ? '<p>' + escapeHtml(section.description) + '</p>' : '');
      if (isFeatured(section) && section.pages && section.pages[0]) {
        html += featureArticle(section.pages[0], 'feature');
      } else {
        var more = '';
        if (section.hasMore) {
          more = '<a class="piece-card piece-more" href="./?category='
            + encodeURIComponent(section.itemName || section.slug || '') + '">'
            + '<div class="piece-media piece-more-media"><h3>Display more</h3>'
            + '<p>All ' + escapeHtml(section.totalCount) + ' items</p></div></a>';
        }
        html += pieceGrid(section.pages, site, more);
      }
      html += '</section>';
    });
    if (!html) {
      html = '<p class="kicker">No published pages yet.</p>';
    }
    return html;
  }

  function renderRead(page) {
    if (!page) {
      return '<div class="state-panel"><p class="kicker">Not found.</p></div>';
    }
    var html = '<article class="reading">';
    html += '<p class="kicker"><a href="./">Index</a>';
    if (page.category) {
      html += '<span> / ' + escapeHtml(page.category) + '</span>';
    }
    html += '</p>';
    html += '<div class="reading-head"><div class="reading-titles">';
    html += '<h1>' + escapeHtml(pageTitle(page)) + '</h1>';
    if (page.subHeader) {
      html += '<p class="reading-sub">' + escapeHtml(page.subHeader) + '</p>';
    }
    html += '</div>';
    if (page.exportPdf && page.uri) {
      html += '<a class="export-pdf" href="./rest/pages/pdf?page='
        + encodeURIComponent(page.uri) + '" title="PDF">'
        + '<img src="./content-public/cse-core/images/pdf.png" alt="PDF" width="40" height="40"></a>';
    }
    html += '</div>';
    if (page.image && !page.album && !page.noTopDisplayImage) {
      html += '<figure class="reading-media"><img src="'
        + escapeHtml(page.image) + '" alt=""></figure>';
    }
    if (page.album && page.albumImages && page.albumImages.length) {
      html += '<div class="album-grid">';
      page.albumImages.forEach(function (img) {
        html += '<figure><a href="' + escapeHtml(img.full) + '" target="_blank" rel="noopener">'
          + '<img src="' + escapeHtml(img.src) + '" alt="' + escapeHtml(img.alt || '') + '">'
          + '</a>';
        if (img.description) {
          html += '<figcaption>' + escapeHtml(img.description) + '</figcaption>';
        }
        html += '</figure>';
      });
      html += '</div>';
    }
    if (page.html) {
      html += '<div class="piece-body item-description-content">' + page.html + '</div>';
    }
    if (page.tags && page.tags.length) {
      html += '<div class="reading-tags">';
      page.tags.forEach(function (tag) {
        html += '<a href="./?tag=' + encodeURIComponent(tag) + '"><span class="item-tag">'
          + escapeHtml(tag) + '</span></a>';
      });
      html += '</div>';
    }
    if (page.comments && page.comments.length) {
      html += '<div class="item-comments">';
      page.comments.forEach(function (comment) {
        html += '<div class="item-comment"><div class="comment-meta"><strong>'
          + escapeHtml(comment.author) + '</strong><span>'
          + escapeHtml(comment.date) + '</span>';
        if (comment.time) {
          html += '<span>at ' + escapeHtml(comment.time) + '</span>';
        }
        html += '</div><div>' + (comment.message || '') + '</div></div>';
      });
      html += '</div>';
    }
    html += '</article>';
    return html;
  }

  function renderList(site, heading, indexLabel, backLabel) {
    var html = '<section class="studio-section" data-kind="default">';
    html += sectionHead(indexLabel, heading,
        '<p><a href="./">' + escapeHtml(backLabel) + '</a></p>');
    var pages = [];
    (site.sections || []).forEach(function (section) {
      (section.pages || []).forEach(function (page) {
        pages.push(page);
      });
    });
    html += pieceGrid(pages, site, '');
    html += '</section>';
    return html;
  }

  function renderError(site) {
    var err = site.error || {};
    return '<div class="state-panel error">'
      + '<p class="kicker">Error</p>'
      + '<h1>' + escapeHtml(err.code || '') + '</h1>'
      + '<h2>' + escapeHtml(err.name || '') + '</h2>'
      + '<p>' + escapeHtml(err.description || '') + '</p>'
      + '<p>Something went terribly wrong...</p>'
      + '</div>';
  }

  function line(label, valueHtml) {
    if (!valueHtml) {
      return '';
    }
    return '<div><b>' + label + '</b> ' + valueHtml + '</div>';
  }

  function renderFoot(site) {
    var copy = site.copy || {};
    var foot = site.foot || {};
    var org = qs('foot-org');
    var footCopy = qs('foot-copy');
    var studio = qs('foot-studio');
    if (org) {
      if (foot.org) {
        org.innerHTML = foot.org;
      } else {
        var title = copy['company-title'] || site.siteName;
        var html = '';
        if (title) {
          html += '<div><h4><b>' + escapeHtml(title) + '</b></h4></div>';
        }
        html += line('Located at:', copy['company-address']
            ? '<label>' + escapeHtml(copy['company-address']) + '</label>' : '');
        html += line('Phone:', copy['company-phone']
            ? '<label>' + escapeHtml(copy['company-phone']) + '</label>' : '');
        html += line('E-mail:', copy['company-email']
            ? '<a href="mailto:' + escapeHtml(copy['company-email']) + '">'
              + escapeHtml(copy['company-email']) + '</a>' : '');
        if (copy['company-social']) {
          html += copy['company-social'];
        }
        org.innerHTML = html;
      }
    }
    if (footCopy) {
      if (foot.copy) {
        footCopy.innerHTML = foot.copy;
      } else {
        var holder = copy['copyright-holder']
          ? '<b>' + escapeHtml(copy['copyright-holder']) + '</b>'
            + (copy['copyright-since'] ? ' © ' + escapeHtml(copy['copyright-since']) : '')
          : '';
        var comment = copy['copyright-comment']
          ? '<div><label>' + escapeHtml(copy['copyright-comment']) + '</label></div>' : '';
        footCopy.innerHTML = (holder ? '<div>' + holder + '</div>' : '') + comment;
      }
    }
    if (studio) {
      if (foot.studio) {
        studio.innerHTML = foot.studio;
      } else {
      var builder = copy['builder-title']
        ? '<div><b>Built by <a href="'
          + escapeHtml(copy['builder-refer'] || 'https://ravenherz.com/')
          + '" target="_blank" rel="noopener">'
          + escapeHtml(copy['builder-title']) + '</a></b></div>'
        : '';
      var version = [copy['version-product'], copy['version-version'], copy['version-branch']]
        .filter(function (bit) { return bit; })
        .join(' ');
      var versionHtml = version
        ? '<div class="soft-version">' + escapeHtml(copy['version-product'] || '')
          + '<br>' + escapeHtml((copy['version-version'] || '')
            + (copy['version-branch'] ? ' ' + copy['version-branch'] : ''))
          + '</div>'
        : '';
      studio.innerHTML = builder + versionHtml;
      }
    }
  }

  function bindInjectedMedia(root) {
    if (window.CseWaveform && typeof window.CseWaveform.observe === 'function') {
      window.CseWaveform.observe(root || document);
    }
    if (window.CsePlayer && typeof window.CsePlayer.refresh === 'function') {
      window.CsePlayer.refresh();
    }
    if (window.CseVideoPlayer && typeof window.CseVideoPlayer.refresh === 'function') {
      window.CseVideoPlayer.refresh();
    }
  }

  function bindAccount(site) {
    var slot = qs('account-slot');
    var link = qs('account-link');
    if (!site.configured) {
      setHidden(slot, true);
      return;
    }
    setHidden(slot, false);
    if (!link) {
      return;
    }
    var root = text(site.contextPath);
    if (!root || root === '/') {
      root = '';
    } else if (root.charAt(root.length - 1) === '/') {
      root = root.slice(0, -1);
    }
    var next = encodeURIComponent(window.location.pathname + window.location.search);
    link.href = root + '/apps/login/?next=' + next;
    link.textContent = site.authenticated ? (site.username || 'Account') : 'Sign in';
  }

  function publicThemeUrl(site, suffix) {
    var root = text(site.contextPath);
    if (!root || root === '/') {
      root = '';
    } else if (root.charAt(root.length - 1) === '/') {
      root = root.slice(0, -1);
    }
    return root + '/content-public/themes/' + suffix;
  }

  function applySchema(site) {
    var link = qs('styles-schema');
    if (!link || !site.stylesTheme || !site.stylesSchema) {
      return;
    }
    link.href = publicThemeUrl(site, encodeURIComponent(site.stylesTheme)
      + '/css/color-schemas/' + encodeURIComponent(site.stylesSchema) + '.css');
  }

  function render(site) {
    document.title = site.htmlTitle || site.siteName || 'Calm Springs';
    var brand = qs('site-brand');
    if (brand) {
      brand.textContent = site.siteName || 'Calm Springs';
    }
    applySchema(site);
    renderNav(site);
    bindAccount(site);
    renderFoot(site);
    var main = qs('site-main');
    if (!main) {
      return;
    }
    if (site.view === 'error') {
      main.innerHTML = renderError(site);
    } else if (site.view === 'setup') {
      main.innerHTML = '<div class="state-panel"><p class="kicker">Setup</p>'
        + '<h1>Calm Springs</h1><p>This instance is not configured yet.</p></div>';
    } else if (site.view === 'page' || site.view === 'album') {
      main.innerHTML = renderRead(site.page);
    } else if (site.view === 'tag') {
      main.innerHTML = renderList(site, site.selectedTag || 'Tag', '#', 'All work');
    } else if (site.view === 'category') {
      var title = (site.sections && site.sections[0] && site.sections[0].title) || 'Category';
      main.innerHTML = renderList(site, title, 'All', 'Back to home');
    } else {
      main.innerHTML = renderHome(site);
    }
    bindInjectedMedia(main);
  }

  function restSiteUrl() {
    var path = window.location.pathname || '/';
    if (!path.endsWith('/')) {
      path += '/';
    }
    return path + 'rest/site' + window.location.search;
  }

  fetch(restSiteUrl(), { credentials: 'same-origin' })
    .then(function (res) {
      return res.json().then(function (body) {
        return body;
      });
    })
    .then(render)
    .catch(function () {
      var main = qs('site-main');
      if (main) {
        main.innerHTML = '<div class="state-panel"><p class="kicker">Error</p>'
          + '<p>Could not load GET /rest/site.</p></div>';
      }
    });
})();
