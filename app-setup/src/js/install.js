(function () {
    function contextPrefix() {
        var path = window.location.pathname || '/';
        var marker = '/apps/setup';
        var index = path.indexOf(marker);
        if (index >= 0) {
            return path.substring(0, index);
        }
        return '';
    }

    function api(suffix) {
        return contextPrefix() + '/install' + suffix;
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

    async function ensureCsrf() {
        var token = csrfToken();
        if (token) {
            return token;
        }
        await fetch(api('/status'), { credentials: 'same-origin', cache: 'no-store' });
        return csrfToken();
    }

    var errorEl = document.getElementById('install-error');
    var persistEl = document.getElementById('install-persist');

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
    }

    function showPersist(message) {
        if (!persistEl) {
            return;
        }
        if (!message) {
            persistEl.hidden = true;
            persistEl.textContent = '';
            return;
        }
        persistEl.hidden = false;
        persistEl.textContent = message;
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

    async function getStatus() {
        var res = await fetch(api('/status'), { credentials: 'same-origin', cache: 'no-store' });
        if (res.status === 404) {
            window.location.href = contextPrefix() + '/';
            return null;
        }
        if (!res.ok) {
            showError('Could not load setup status');
            return null;
        }
        return readJson(res);
    }

    async function post(suffix, body) {
        var token = await ensureCsrf();
        if (!token) {
            showError('Could not get a security cookie. Wait a moment and try again.');
            return null;
        }
        var res = await fetch(api(suffix), {
            method: 'POST',
            credentials: 'same-origin',
            cache: 'no-store',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'X-XSRF-TOKEN': token
            },
            body: JSON.stringify(body)
        });
        if (res.status === 403) {
            token = await ensureCsrf();
            if (token) {
                res = await fetch(api(suffix), {
                    method: 'POST',
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Requested-With': 'XMLHttpRequest',
                        'X-XSRF-TOKEN': token
                    },
                    body: JSON.stringify(body)
                });
            }
        }
        var data = await readJson(res);
        if (res.status === 404) {
            window.location.href = contextPrefix() + '/';
            return null;
        }
        if (!res.ok) {
            showError(data.message || ('Request failed (' + res.status + ')'));
            return null;
        }
        showError('');
        return data;
    }

    var TITLE_KEY = 'cse-install-companyTitle';

    function applyStatus(status) {
        if (!status) {
            return;
        }
        var title = document.getElementById('site-title');
        if (title) {
            if (status.existingSite) {
                title.value = '';
            } else if (!title.value) {
                try {
                    var saved = sessionStorage.getItem(TITLE_KEY);
                    if (saved) {
                        title.value = saved;
                    }
                } catch (e) {
                    /* private mode */
                }
            }
        }
        var mongoHint = document.getElementById('mongo-hint');
        if (mongoHint) {
            if (status.mongoFromEnv && !status.mongoReady) {
                mongoHint.textContent = 'Environment Mongo is set but not reachable.';
            } else if (status.mongoFromEnv) {
                mongoHint.textContent = '';
            } else {
                mongoHint.textContent = 'Not stored in editor settings. Prefer CSE_MONGO_* on the next boot.';
            }
        }
        show('step-mongo', !status.mongoReady);
        show('step-owner', status.mongoReady && !status.hasOwner);
        show('step-finish', status.mongoReady && status.hasOwner);
        var existing = !!status.existingSite;
        show('finish-existing', existing);
        show('finish-personal', !existing);
        var titleField = document.getElementById('site-title');
        if (titleField) {
            titleField.required = !existing;
            if (existing) {
                titleField.value = '';
            }
        }
        var finishSubmit = document.getElementById('finish-submit');
        if (finishSubmit) {
            finishSubmit.textContent = existing ? 'Continue' : 'Finish';
        }
    }

    function formValues(form) {
        var data = {};
        var elements = form.elements;
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (el.name) {
                data[el.name] = el.value;
            }
        }
        return data;
    }

    async function refresh() {
        applyStatus(await getStatus());
    }

    async function withBusy(form, work) {
        var button = form.querySelector('button[type="submit"]');
        if (button) {
            button.disabled = true;
        }
        try {
            return await work();
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    }

    function fieldValue(id) {
        var el = document.getElementById(id);
        return el && el.value ? el.value.trim() : '';
    }

    function tokenOr(value, name) {
        return value || ('<' + name + '>');
    }

    function mongoHostPort() {
        var address = fieldValue('mongo-address');
        var port = fieldValue('mongo-port');
        var host = address;
        var colon = address.lastIndexOf(':');
        if (address && colon > 0 && address.indexOf(':') === colon) {
            var after = address.substring(colon + 1);
            if (/^\d+$/.test(after)) {
                host = address.substring(0, colon);
                if (!port) {
                    port = after;
                }
            }
        }
        return {
            host: tokenOr(host, 'address'),
            port: tokenOr(port || '27017', 'port')
        };
    }

    function mongoTerminalCommand() {
        var hp = mongoHostPort();
        return 'mongosh --host ' + hp.host
            + ' --port ' + hp.port
            + ' -u ' + tokenOr(fieldValue('mongo-cluster-admin'), 'cluster-admin')
            + ' -p --authenticationDatabase admin';
    }

    function mongoUseCommand() {
        return 'use ' + tokenOr(fieldValue('mongo-dbname'), 'dbname');
    }

    function mongoCreateCommand() {
        var db = tokenOr(fieldValue('mongo-dbname'), 'dbname');
        var user = fieldValue('mongo-user');
        var password = document.getElementById('mongo-password');
        var pwd = password ? password.value : '';
        return [
            'db.createUser({',
            '  user: ' + JSON.stringify(tokenOr(user, 'app-user')) + ',',
            '  pwd: ' + JSON.stringify(pwd || '<app-password>') + ',',
            '  roles: [',
            '    { role: "readWrite", db: ' + JSON.stringify(db) + ' },',
            '    { role: "dbAdmin", db: ' + JSON.stringify(db) + ' }',
            '  ]',
            '})'
        ].join('\n');
    }

    function mongoExitCommand() {
        return 'exit';
    }

    function renderMongoRecipe() {
        var terminal = document.getElementById('mongo-terminal');
        var useEl = document.getElementById('mongo-use');
        var createEl = document.getElementById('mongo-create');
        var exitEl = document.getElementById('mongo-exit');
        if (terminal) {
            terminal.textContent = mongoTerminalCommand();
        }
        if (useEl) {
            useEl.textContent = mongoUseCommand();
        }
        if (createEl) {
            createEl.textContent = mongoCreateCommand();
        }
        if (exitEl) {
            exitEl.textContent = mongoExitCommand();
        }
    }

    function mongoGuideOpen() {
        var selected = document.querySelector('input[name="mongo-path"]:checked');
        return selected && selected.value === 'guide';
    }

    function applyMongoPath() {
        show('mongo-guide', mongoGuideOpen());
        if (mongoGuideOpen()) {
            renderMongoRecipe();
        }
    }

    document.querySelectorAll('input[name="mongo-path"]').forEach(function (radio) {
        radio.addEventListener('change', applyMongoPath);
    });
    ['mongo-address', 'mongo-port', 'mongo-dbname', 'mongo-user', 'mongo-password',
            'mongo-cluster-admin'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) {
            el.addEventListener('input', renderMongoRecipe);
        }
    });
    function bindCopy(buttonId, textFn) {
        var button = document.getElementById(buttonId);
        if (!button) {
            return;
        }
        button.addEventListener('click', async function () {
            var text = textFn();
            try {
                await navigator.clipboard.writeText(text);
            } catch (e) {
                var area = document.createElement('textarea');
                area.value = text;
                document.body.appendChild(area);
                area.select();
                document.execCommand('copy');
                document.body.removeChild(area);
            }
            button.textContent = 'Copied';
            setTimeout(function () {
                button.textContent = 'Copy';
            }, 1500);
        });
    }
    bindCopy('mongo-copy-terminal', mongoTerminalCommand);
    bindCopy('mongo-copy-use', mongoUseCommand);
    bindCopy('mongo-copy-create', mongoCreateCommand);
    bindCopy('mongo-copy-exit', mongoExitCommand);
    applyMongoPath();

    document.getElementById('form-mongo').addEventListener('submit', async function (event) {
        event.preventDefault();
        await withBusy(event.target, async function () {
            var result = await post('/mongo', formValues(event.target));
            if (result) {
                showPersist(result.message || '');
                await refresh();
            }
        });
    });
    document.getElementById('form-owner').addEventListener('submit', async function (event) {
        event.preventDefault();
        await withBusy(event.target, async function () {
            var result = await post('/owner', formValues(event.target));
            if (result) {
                await refresh();
            }
        });
    });
    document.getElementById('form-finish').addEventListener('submit', async function (event) {
        event.preventDefault();
        await withBusy(event.target, async function () {
            var result = await post('/finish', formValues(event.target));
            if (result && result.redirect) {
                try {
                    sessionStorage.removeItem(TITLE_KEY);
                } catch (e) {
                    /* private mode */
                }
                window.location.href = result.redirect;
            }
        });
    });
    var siteTitle = document.getElementById('site-title');
    if (siteTitle) {
        siteTitle.addEventListener('input', function () {
            try {
                sessionStorage.setItem(TITLE_KEY, siteTitle.value);
            } catch (e) {
                /* private mode */
            }
        });
    }

    refresh();
})();
