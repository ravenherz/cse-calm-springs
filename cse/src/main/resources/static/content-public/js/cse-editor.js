/**
 * Suppress the browser context menu. Custom menus still fire.
 * Opt out with data-allow-context-menu on a target or ancestor.
 * Editor pages include this first; public pages can later.
 */
(function (document) {
    document.addEventListener('contextmenu', function (e) {
        if (e.target && e.target.closest && e.target.closest('[data-allow-context-menu]')) {
            return;
        }
        e.preventDefault();
    });
})(document);
