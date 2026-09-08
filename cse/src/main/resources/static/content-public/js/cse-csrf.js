/**
 * CSRF for jQuery POSTs and HTML forms. Auth cookies stay HttpOnly; this reads XSRF-TOKEN only.
 * Public HTML ids are unchanged. Native form POST stays a top-level navigation (SameSite=Lax).
 */
(function (window, document) {
    function cookiePath() {
        var meta = document.querySelector('meta[name="cse-context"]');
        if (meta && meta.content) {
            var fromMeta = meta.content.replace(/\/+$/, '');
            return fromMeta.length ? fromMeta : '/';
        }
        var path = window.location.pathname || '/';
        var editor = path.indexOf('/editor');
        if (editor > 0) {
            return path.substring(0, editor);
        }
        var parts = path.split('/');
        return parts.length > 1 && parts[1] ? '/' + parts[1] : '/';
    }

    function pageCsrfToken() {
        var meta = document.querySelector('meta[name="csrf-token"]');
        if (meta && meta.content) {
            return meta.content;
        }
        var input = document.querySelector('input[name="_csrf"]');
        return input && input.value ? input.value : '';
    }

    function cseCsrfToken() {
        var match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
        return match ? decodeURIComponent(match[1]) : '';
    }

    function ensureXsrfCookie() {
        if (cseCsrfToken()) {
            return;
        }
        var token = pageCsrfToken();
        if (!token) {
            return;
        }
        var secure = window.location.protocol === 'https:' ? '; Secure' : '';
        document.cookie = 'XSRF-TOKEN=' + encodeURIComponent(token)
            + '; Path=' + cookiePath()
            + '; Max-Age=2592000; SameSite=Lax'
            + secure;
    }

    function isSafeMethod(method) {
        var verb = (method || 'GET').toUpperCase();
        return verb === 'GET' || verb === 'HEAD' || verb === 'OPTIONS' || verb === 'TRACE';
    }

    function ensureCsrfOnForm(form) {
        ensureXsrfCookie();
        var token = cseCsrfToken() || pageCsrfToken();
        if (!token) {
            return;
        }
        var inputs = form.querySelectorAll('input[name="_csrf"]');
        if (inputs.length === 0) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            form.appendChild(input);
            inputs = form.querySelectorAll('input[name="_csrf"]');
        }
        for (var i = 0; i < inputs.length; i++) {
            inputs[i].value = token;
        }
    }

    function ensureCsrfOnAllForms() {
        var forms = document.getElementsByTagName('form');
        for (var i = 0; i < forms.length; i++) {
            var method = forms[i].getAttribute('method') || forms[i].method;
            if (!isSafeMethod(method)) {
                ensureCsrfOnForm(forms[i]);
            }
        }
    }

    ensureXsrfCookie();
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', ensureCsrfOnAllForms);
    } else {
        ensureCsrfOnAllForms();
    }

    if (window.jQuery) {
        window.jQuery.ajaxSetup({
            beforeSend: function (xhr, settings) {
                if (isSafeMethod(settings.type || settings.method)) {
                    return;
                }
                ensureXsrfCookie();
                var token = cseCsrfToken() || pageCsrfToken();
                if (token) {
                    xhr.setRequestHeader('X-XSRF-TOKEN', token);
                }
            }
        });
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form || !form.tagName || form.tagName.toLowerCase() !== 'form') {
            return;
        }
        if (isSafeMethod(form.getAttribute('method') || form.method)) {
            return;
        }
        ensureCsrfOnForm(form);
    }, true);
})(window, document);
