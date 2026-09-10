(function () {
    function contextPrefix() {
        var path = window.location.pathname || '/';
        var marker = '/apps/login';
        var index = path.indexOf(marker);
        if (index >= 0) {
            return path.substring(0, index);
        }
        return '';
    }

    function api(suffix) {
        return contextPrefix() + suffix;
    }

    function siteHref() {
        var prefix = contextPrefix();
        return prefix ? prefix + '/' : '/';
    }

    function setupHref() {
        return contextPrefix() + '/apps/setup/';
    }

    function editorHref() {
        return contextPrefix() + '/editor';
    }

    function csrfToken() {
        var cookies = document.cookie ? document.cookie.split(';') : [];
        for (var i = 0; i < cookies.length; i++) {
            var part = cookies[i].trim();
            if (part.indexOf('XSRF-TOKEN=') === 0) {
                try {
                    return decodeURIComponent(part.substring('XSRF-TOKEN='.length));
                } catch (e) {
                    return part.substring('XSRF-TOKEN='.length);
                }
            }
        }
        return '';
    }

    function rememberCsrf(response) {
        if (!response || !response.headers || !response.headers.get) {
            return;
        }
        var value = response.headers.get('X-XSRF-TOKEN');
        if (!value) {
            return;
        }
        var secure = window.location.protocol === 'https:' ? '; Secure' : '';
        document.cookie = 'XSRF-TOKEN=' + encodeURIComponent(value)
            + '; Path=/; Max-Age=2592000; SameSite=Lax' + secure;
    }

    async function ensureCsrf() {
        var token = csrfToken();
        if (token) {
            return token;
        }
        var res = await fetch(api('/rest/site'), { credentials: 'same-origin', cache: 'no-store' });
        rememberCsrf(res);
        return csrfToken();
    }

    function wantsRegister() {
        var params = new URLSearchParams(window.location.search);
        return params.has('register');
    }

    function nextHref() {
        var params = new URLSearchParams(window.location.search);
        var next = params.get('next');
        if (!next) {
            return siteHref();
        }
        try {
            var url = new URL(next, window.location.origin);
            if (url.origin !== window.location.origin) {
                return siteHref();
            }
            return url.pathname + url.search + url.hash;
        } catch (e) {
            if (next.charAt(0) === '/') {
                return next;
            }
            return siteHref();
        }
    }

    function withNext(href) {
        var next = new URLSearchParams(window.location.search).get('next');
        if (!next) {
            return href;
        }
        var url = new URL(href, window.location.href);
        url.searchParams.set('next', next);
        return url.pathname + url.search + url.hash;
    }

    var errorEl = document.getElementById('banner-error');
    var okEl = document.getElementById('banner-ok');

    function showError(message) {
        if (!errorEl) {
            return;
        }
        if (!message) {
            errorEl.hidden = true;
            errorEl.textContent = '';
            return;
        }
        errorEl.hidden = false;
        errorEl.textContent = message;
        if (okEl) {
            okEl.hidden = true;
            okEl.textContent = '';
        }
    }

    function showOk(message) {
        if (!okEl) {
            return;
        }
        if (!message) {
            okEl.hidden = true;
            okEl.textContent = '';
            return;
        }
        okEl.hidden = false;
        okEl.textContent = message;
        if (errorEl) {
            errorEl.hidden = true;
            errorEl.textContent = '';
        }
    }

    function show(id, on) {
        var el = document.getElementById(id);
        if (el) {
            el.hidden = !on;
        }
    }

    async function readJson(res) {
        var text = await res.text();
        if (!text) {
            return {};
        }
        try {
            return JSON.parse(text);
        } catch (e) {
            return {};
        }
    }

    async function post(path, body) {
        var token = await ensureCsrf();
        if (!token) {
            showError('Could not get a security cookie. Wait a moment and try again.');
            return null;
        }
        var headers = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest',
            'X-XSRF-TOKEN': token
        };
        var res = await fetch(api(path), {
            method: 'POST',
            credentials: 'same-origin',
            cache: 'no-store',
            headers: headers,
            body: JSON.stringify(body)
        });
        rememberCsrf(res);
        if (res.status === 403) {
            token = await ensureCsrf();
            if (token) {
                headers['X-XSRF-TOKEN'] = token;
                res = await fetch(api(path), {
                    method: 'POST',
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: headers,
                    body: JSON.stringify(body)
                });
                rememberCsrf(res);
            }
        }
        var data = await readJson(res);
        if (!res.ok) {
            showError((data && data.message) || ('Request failed (' + res.status + ')'));
            return null;
        }
        return data;
    }

    function envelopeOk(data) {
        return data && (data.status === 200 || data.status === undefined);
    }

    function setNav(view) {
        var signin = document.getElementById('nav-signin');
        var register = document.getElementById('nav-register');
        var hide = view === 'user' || view === 'setup';
        if (signin) {
            signin.hidden = hide;
            signin.classList.toggle('active', view === 'signin');
            signin.setAttribute('href', withNext('./'));
        }
        if (register) {
            register.hidden = hide;
            register.classList.toggle('active', view === 'register');
            register.setAttribute('href', withNext('./?register'));
        }
    }

    function render(site) {
        var load = document.getElementById('panel-load');
        if (load) {
            load.hidden = true;
        }
        ['link-site', 'link-site-brand'].forEach(function (id) {
            var link = document.getElementById(id);
            if (link) {
                link.setAttribute('href', siteHref());
            }
        });
        var setup = document.getElementById('link-setup');
        if (setup) {
            setup.setAttribute('href', setupHref());
        }
        var editor = document.getElementById('link-editor');
        if (editor) {
            editor.setAttribute('href', editorHref());
        }
        var toRegister = document.getElementById('link-to-register');
        if (toRegister) {
            toRegister.setAttribute('href', withNext('./?register'));
        }
        var toSignin = document.getElementById('link-to-signin');
        if (toSignin) {
            toSignin.setAttribute('href', withNext('./'));
        }

        var who = document.getElementById('who');
        var title = document.getElementById('page-title');
        if (!site || !site.configured) {
            if (who) {
                who.hidden = true;
            }
            if (title) {
                title.textContent = 'Sign in';
            }
            document.title = 'Sign in';
            setNav('setup');
            show('panel-setup', true);
            show('panel-signin', false);
            show('panel-register', false);
            show('panel-user', false);
            return;
        }

        if (site.authenticated) {
            if (who) {
                who.hidden = !site.username;
                who.textContent = site.username || '';
            }
            var userName = document.getElementById('user-name');
            if (userName) {
                userName.textContent = site.username || 'user';
            }
            if (title) {
                title.textContent = 'Account';
            }
            document.title = 'Account';
            setNav('user');
            show('panel-setup', false);
            show('panel-signin', false);
            show('panel-register', false);
            show('panel-user', true);
            return;
        }

        if (who) {
            who.hidden = true;
        }
        var register = wantsRegister();
        if (title) {
            title.textContent = register ? 'Create account' : 'Sign in';
        }
        document.title = register ? 'Create account' : 'Sign in';
        setNav(register ? 'register' : 'signin');
        show('panel-setup', false);
        show('panel-signin', !register);
        show('panel-register', register);
        show('panel-user', false);
    }

    function goAfterAuth() {
        window.location.href = nextHref();
    }

    async function loadSite() {
        var res = await fetch(api('/rest/site'), { credentials: 'same-origin', cache: 'no-store' });
        rememberCsrf(res);
        if (!res.ok) {
            showError('Could not load site status');
            return null;
        }
        return readJson(res);
    }

    document.getElementById('form-signin').addEventListener('submit', async function (event) {
        event.preventDefault();
        var button = document.getElementById('btn-signin');
        if (button) {
            button.disabled = true;
        }
        showError('');
        showOk('');
        try {
            var data = await post('/account/auth', {
                'loginpanel-username': document.getElementById('loginpanel-username').value.trim(),
                'loginpanel-password': document.getElementById('loginpanel-password').value
            });
            if (!data) {
                return;
            }
            if (envelopeOk(data)) {
                goAfterAuth();
                return;
            }
            showError(data.message || 'Could not sign in');
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    });

    document.getElementById('form-register').addEventListener('submit', async function (event) {
        event.preventDefault();
        var password = document.getElementById('ACCOUNT_PASSWORD').value;
        var retype = document.getElementById('ACCOUNT_PASSWORD_RETYPE').value;
        if (password !== retype) {
            showError("Passwords don't match");
            return;
        }
        var button = document.getElementById('btn-register');
        if (button) {
            button.disabled = true;
        }
        showError('');
        showOk('');
        try {
            var body = {
                ACCOUNT_LOGIN: document.getElementById('ACCOUNT_LOGIN').value.trim(),
                ACCOUNT_EMAIL: document.getElementById('ACCOUNT_EMAIL').value.trim(),
                ACCOUNT_PASSWORD: password,
                ACCOUNT_PASSWORD_RETYPE: retype
            };
            var shown = document.getElementById('ACCOUNT_SHOWN_NAME').value.trim();
            if (shown) {
                body.ACCOUNT_SHOWN_NAME = shown;
            }
            var data = await post('/account/register', body);
            if (!data) {
                return;
            }
            if (envelopeOk(data)) {
                goAfterAuth();
                return;
            }
            showError(data.message || 'Could not create the account');
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    });

    document.getElementById('btn-logout').addEventListener('click', async function () {
        showError('');
        showOk('');
        var data = await post('/account/logout', {});
        if (!data) {
            return;
        }
        if (envelopeOk(data)) {
            window.location.href = withNext('./');
            return;
        }
        showError(data.message || 'Could not log out');
    });

    loadSite().then(function (site) {
        if (!site) {
            document.getElementById('panel-load').hidden = true;
            return;
        }
        render(site);
    }).catch(function () {
        document.getElementById('panel-load').hidden = true;
        showError('Could not load site status');
    });
})();
