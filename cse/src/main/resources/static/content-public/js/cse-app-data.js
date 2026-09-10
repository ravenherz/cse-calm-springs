/**
 * JSON store for packed apps. Never sends a Mongo collection name — only slug + table.
 * CSRF: cookie XSRF-TOKEN as header X-XSRF-TOKEN (same as cse-csrf.js).
 * Empty 403 bodies used to throw "Unexpected end of JSON input"; writes retry once after planting the cookie.
 */
(function (window, document) {
    function contextPath() {
        var meta = document.querySelector('meta[name="cse-context"]');
        if (meta && meta.content) {
            var fromMeta = meta.content.replace(/\/+$/, '');
            return fromMeta.length ? fromMeta : '';
        }
        var path = window.location.pathname || '/';
        var marker = '/static-pages/';
        var i = path.indexOf(marker);
        if (i > 0) {
            return path.substring(0, i);
        }
        return '';
    }

    function inferSlug() {
        var path = window.location.pathname || '';
        var marker = '/static-pages/';
        var i = path.indexOf(marker);
        if (i < 0) {
            return '';
        }
        var rest = path.substring(i + marker.length);
        var slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    function cookieTokens() {
        var tokens = [];
        var parts = document.cookie ? document.cookie.split(';') : [];
        for (var i = 0; i < parts.length; i++) {
            var part = parts[i].trim();
            if (part.indexOf('XSRF-TOKEN=') !== 0) {
                continue;
            }
            var raw = part.substring('XSRF-TOKEN='.length);
            try {
                tokens.push(decodeURIComponent(raw));
            } catch (e) {
                tokens.push(raw);
            }
        }
        return tokens;
    }

    var lastCsrf = '';

    function rememberCsrf(response) {
        if (!response || !response.headers || !response.headers.get) {
            return;
        }
        var value = response.headers.get('X-XSRF-TOKEN');
        if (value) {
            lastCsrf = value;
            writeCsrfCookie(value);
        }
    }

    function writeCsrfCookie(token) {
        if (!token) {
            return;
        }
        var secure = window.location.protocol === 'https:' ? '; Secure' : '';
        document.cookie = 'XSRF-TOKEN=' + encodeURIComponent(token)
            + '; Path=/; Max-Age=2592000; SameSite=Lax' + secure;
    }

    function csrfToken() {
        if (lastCsrf) {
            return lastCsrf;
        }
        var tokens = cookieTokens();
        if (tokens.length) {
            return tokens[tokens.length - 1];
        }
        var meta = document.querySelector('meta[name="csrf-token"]');
        if (meta && meta.content) {
            return meta.content;
        }
        var input = document.querySelector('input[name="_csrf"]');
        return input && input.value ? input.value : '';
    }

    function isSafe(method) {
        var verb = (method || 'GET').toUpperCase();
        return verb === 'GET' || verb === 'HEAD' || verb === 'OPTIONS' || verb === 'TRACE';
    }

    function fail(status, message, body) {
        var error = new Error(message || 'Request failed');
        error.status = status;
        error.body = body || {};
        return error;
    }

    function parseJson(response, text) {
        if (!text) {
            throw fail(response.status, response.status === 403
                ? 'Security check failed'
                : (response.statusText || 'Empty response'));
        }
        try {
            return JSON.parse(text);
        } catch (e) {
            throw fail(response.status, 'Invalid response');
        }
    }

    function CseAppData(options) {
        options = options || {};
        this.slug = options.slug || inferSlug();
        this.table = options.table || '';
        this.base = contextPath();
    }

    CseAppData.connect = function (options) {
        return new CseAppData(options);
    };

    CseAppData.prototype.list = function (opts) {
        opts = opts || {};
        var query = [];
        if (opts.limit) {
            query.push('limit=' + encodeURIComponent(opts.limit));
        }
        if (opts.after) {
            query.push('after=' + encodeURIComponent(opts.after));
        }
        if (opts.mine) {
            query.push('mine=1');
        }
        var suffix = query.length ? '?' + query.join('&') : '';
        return this.request('GET', this.tablePath() + suffix);
    };

    CseAppData.prototype.get = function (id) {
        return this.request('GET', this.tablePath() + '/' + encodeURIComponent(id));
    };

    CseAppData.prototype.insert = function (data) {
        return this.request('POST', this.tablePath(), data);
    };

    CseAppData.prototype.patch = function (id, data) {
        return this.request('PATCH', this.tablePath() + '/' + encodeURIComponent(id), data);
    };

    CseAppData.prototype.remove = function (id) {
        return this.request('DELETE', this.tablePath() + '/' + encodeURIComponent(id));
    };

    CseAppData.prototype.schema = function () {
        return this.request('GET', this.appPath() + '/_schema');
    };

    CseAppData.prototype.ensureTable = function (table, spec) {
        var name = table || this.table;
        return this.request('PUT', this.appPath() + '/_schema/' + encodeURIComponent(name), spec || {});
    };

    CseAppData.prototype.appPath = function () {
        return this.base + '/app-data/' + encodeURIComponent(this.slug);
    };

    CseAppData.prototype.tablePath = function () {
        return this.appPath() + '/' + encodeURIComponent(this.table);
    };

    CseAppData.prototype.plantCsrf = function () {
        return fetch(this.base + '/rest/site', {
            credentials: 'same-origin',
            cache: 'no-store',
            headers: { 'Accept': 'application/json' }
        }).then(function (response) {
            rememberCsrf(response);
            return response;
        });
    };

    CseAppData.prototype.request = function (method, path, body) {
        return this.send(method, path, body, false);
    };

    CseAppData.prototype.send = function (method, path, body, retried) {
        var self = this;
        var headers = {
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        };
        var init = { method: method, credentials: 'same-origin', cache: 'no-store', headers: headers };
        if (!isSafe(method)) {
            var token = csrfToken();
            if (token) {
                writeCsrfCookie(token);
                headers['X-XSRF-TOKEN'] = token;
            }
            headers['Content-Type'] = 'application/json';
            init.body = JSON.stringify(body == null ? {} : body);
        }
        return fetch(path, init).then(function (response) {
            rememberCsrf(response);
            return response.text().then(function (text) {
                if (!retried && !isSafe(method) && response.status === 403) {
                    var msg = '';
                    try {
                        msg = text ? (JSON.parse(text).message || '') : '';
                    } catch (e) {
                        msg = '';
                    }
                    if (!text || msg === 'Security check failed') {
                        return self.plantCsrf().then(function () {
                            return self.send(method, path, body, true);
                        });
                    }
                }
                var json = parseJson(response, text);
                if (!response.ok) {
                    throw fail(response.status, (json && json.message) || response.statusText, json);
                }
                return json;
            });
        });
    };

    window.CseAppData = CseAppData;
})(window, document);
