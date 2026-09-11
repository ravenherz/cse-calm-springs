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

    function wantsEdit() {
        var params = new URLSearchParams(window.location.search);
        return params.has('edit');
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
    var profile = null;
    var avatarValue = '';

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

    function hideLoad() {
        var load = document.getElementById('panel-load');
        if (!load) {
            return;
        }
        load.hidden = true;
        load.setAttribute('aria-busy', 'false');
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

    async function get(path) {
        var res = await fetch(api(path), { credentials: 'same-origin', cache: 'no-store' });
        rememberCsrf(res);
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
        var signedIn = view === 'user' || view === 'edit';
        var hide = view === 'setup';
        if (signin) {
            signin.hidden = hide;
            signin.textContent = signedIn ? 'Account' : 'Sign in';
            signin.classList.toggle('active', view === 'signin' || view === 'user');
            signin.setAttribute('href', withNext('./'));
        }
        if (register) {
            register.hidden = hide;
            register.textContent = signedIn ? 'Edit account' : 'Create account';
            register.classList.toggle('active', view === 'register' || view === 'edit');
            register.setAttribute('href', withNext(signedIn ? './?edit' : './?register'));
        }
    }

    function render(site) {
        hideLoad();
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

        var toEdit = document.getElementById('link-edit');
        if (toEdit) {
            toEdit.setAttribute('href', withNext('./?edit'));
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
            show('panel-edit', false);
            return;
        }

        if (site.authenticated) {
            paintAccount(site);
            var edit = wantsEdit();
            if (title) {
                title.textContent = edit ? 'Edit account' : 'Account';
            }
            document.title = edit ? 'Edit account' : 'Account';
            setNav(edit ? 'edit' : 'user');
            show('panel-setup', false);
            show('panel-signin', false);
            show('panel-register', false);
            show('panel-user', !edit);
            show('panel-edit', edit);
            if (edit) {
                fillEditForm();
            }
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
        show('panel-edit', false);
    }

    function paintAccount(site) {
        var who = document.getElementById('who');
        var login = (profile && profile.login) || (site && site.username) || '';
        var shown = profile && profile.shownName;
        if (who) {
            who.hidden = !login;
            who.textContent = login;
        }
        var userName = document.getElementById('user-name');
        if (userName) {
            userName.textContent = shown || login || 'user';
        }
        var userShown = document.getElementById('user-shown');
        if (userShown) {
            var extra = shown && login && shown !== login ? login : '';
            userShown.hidden = !extra;
            userShown.textContent = extra;
        }
        setAvatarImage(document.getElementById('user-avatar'), profile && profile.avatar);
        var editor = document.getElementById('link-editor');
        if (editor) {
            editor.hidden = !!(profile && profile.admin === false);
        }
    }

    function fillEditForm() {
        var data = profile || {};
        var login = document.getElementById('EDIT_LOGIN');
        if (login) {
            login.value = data.login || '';
        }
        var shown = document.getElementById('EDIT_SHOWN_NAME');
        if (shown) {
            shown.value = data.shownName || '';
        }
        var email = document.getElementById('EDIT_EMAIL');
        if (email) {
            email.value = data.emailAddress || '';
        }
        var bio = document.getElementById('EDIT_BIO');
        if (bio) {
            bio.value = data.bio || '';
        }
        ['EDIT_PASSWORD_CURRENT', 'EDIT_PASSWORD', 'EDIT_PASSWORD_RETYPE'].forEach(function (id) {
            var field = document.getElementById(id);
            if (field) {
                field.value = '';
            }
        });
        avatarValue = data.avatar || '';
        setAvatarImage(document.getElementById('edit-avatar-preview'), avatarValue);
        var clear = document.getElementById('btn-clear-avatar');
        if (clear) {
            clear.hidden = !avatarValue;
        }
    }

    function setAvatarImage(img, dataUrl) {
        if (!img) {
            return;
        }
        if (dataUrl) {
            img.src = dataUrl;
            img.hidden = false;
        } else {
            img.removeAttribute('src');
            img.hidden = true;
        }
    }

    function readFileAsDataUrl(file) {
        return new Promise(function (resolve, reject) {
            var reader = new FileReader();
            reader.onload = function () {
                resolve(String(reader.result || ''));
            };
            reader.onerror = function () {
                reject(new Error('Could not read image'));
            };
            reader.readAsDataURL(file);
        });
    }

    function loadImage(url) {
        return new Promise(function (resolve, reject) {
            var img = new Image();
            img.onload = function () {
                resolve(img);
            };
            img.onerror = function () {
                reject(new Error('Could not decode image'));
            };
            img.src = url;
        });
    }

    function squareJpeg(img, size, quality) {
        var w = img.naturalWidth || img.width;
        var h = img.naturalHeight || img.height;
        if (!w || !h) {
            return '';
        }
        var side = Math.min(w, h);
        var sx = Math.round((w - side) / 2);
        var sy = Math.round((h - side) / 2);
        var canvas = document.createElement('canvas');
        canvas.width = size;
        canvas.height = size;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#0b0c0f';
        ctx.fillRect(0, 0, size, size);
        ctx.drawImage(img, sx, sy, side, side, 0, 0, size, size);
        return canvas.toDataURL('image/jpeg', quality);
    }

    async function encodeAvatar(file) {
        if (!file) {
            throw new Error('Choose an image file');
        }
        if (file.type && file.type.indexOf('image/') !== 0) {
            throw new Error('Choose an image file');
        }
        var raw = await readFileAsDataUrl(file);
        var img = await loadImage(raw);
        var qualities = [0.85, 0.72, 0.6];
        for (var i = 0; i < qualities.length; i++) {
            var next = squareJpeg(img, 256, qualities[i]);
            if (next && next.length <= 350000) {
                return next;
            }
        }
        throw new Error('Image is too large after compression');
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

    document.getElementById('btn-edit-avatar').addEventListener('click', function () {
        document.getElementById('edit-avatar').click();
    });

    document.getElementById('edit-avatar').addEventListener('change', async function (event) {
        var file = event.target.files && event.target.files[0];
        event.target.value = '';
        if (!file) {
            return;
        }
        showError('');
        showOk('');
        try {
            avatarValue = await encodeAvatar(file);
            setAvatarImage(document.getElementById('edit-avatar-preview'), avatarValue);
            var clear = document.getElementById('btn-clear-avatar');
            if (clear) {
                clear.hidden = !avatarValue;
            }
        } catch (err) {
            showError((err && err.message) || 'Could not read that image');
        }
    });

    document.getElementById('btn-clear-avatar').addEventListener('click', function () {
        avatarValue = '';
        setAvatarImage(document.getElementById('edit-avatar-preview'), '');
        document.getElementById('btn-clear-avatar').hidden = true;
    });

    document.getElementById('form-edit').addEventListener('submit', async function (event) {
        event.preventDefault();
        var password = document.getElementById('EDIT_PASSWORD').value;
        var retype = document.getElementById('EDIT_PASSWORD_RETYPE').value;
        if (password || retype) {
            if (password !== retype) {
                showError("Passwords don't match");
                return;
            }
            if (!document.getElementById('EDIT_PASSWORD_CURRENT').value) {
                showError('Current password is required');
                return;
            }
        }
        var button = document.getElementById('btn-save-account');
        if (button) {
            button.disabled = true;
        }
        showError('');
        showOk('');
        try {
            var body = {
                ACCOUNT_EMAIL: document.getElementById('EDIT_EMAIL').value.trim(),
                ACCOUNT_SHOWN_NAME: document.getElementById('EDIT_SHOWN_NAME').value.trim(),
                ACCOUNT_BIO: document.getElementById('EDIT_BIO').value.trim(),
                ACCOUNT_AVATAR: avatarValue
            };
            var current = document.getElementById('EDIT_PASSWORD_CURRENT').value;
            if (password) {
                body.ACCOUNT_PASSWORD_CURRENT = current;
                body.ACCOUNT_PASSWORD = password;
                body.ACCOUNT_PASSWORD_RETYPE = retype;
            }
            var data = await post('/account/me', body);
            if (!data) {
                return;
            }
            if (envelopeOk(data)) {
                profile = data.restObject || profile;
                showOk('Saved');
                fillEditForm();
                paintAccount({ username: profile && profile.login });
                return;
            }
            showError(data.message || 'Could not save the account');
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    });

    loadSite().then(function (site) {
        if (!site) {
            hideLoad();
            return;
        }
        var ready = Promise.resolve();
        if (site.authenticated) {
            ready = get('/account/me').then(function (data) {
                if (data && envelopeOk(data)) {
                    profile = data.restObject || null;
                }
            });
        }
        return ready.then(function () {
            render(site);
        });
    }).catch(function () {
        hideLoad();
        showError('Could not load site status');
    });
})();
