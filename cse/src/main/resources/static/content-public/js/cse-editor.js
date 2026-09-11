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

(function (document) {
    function syncAction(action) {
        var inherit = action.querySelector('input[type="checkbox"][name$="Inherit"]');
        if (!inherit) {
            return;
        }
        action.querySelectorAll('input[type="checkbox"][name$="RoleIds"]').forEach(function (box) {
            box.disabled = inherit.disabled || inherit.checked;
        });
        var dropdown = inherit.closest('.access-check-dropdown');
        if (dropdown) {
            syncDropdownSummary(dropdown);
        }
    }

    function syncDropdownSummary(dropdown) {
        var summary = dropdown.querySelector('.access-check-summary');
        var inherit = dropdown.querySelector('input[type="checkbox"][name$="Inherit"]');
        if (!summary) {
            return;
        }
        if (inherit && inherit.checked) {
            summary.textContent = 'Inherit from parent';
            return;
        }
        var names = [];
        dropdown.querySelectorAll('input[type="checkbox"][name$="RoleIds"]:checked').forEach(function (box) {
            var label = box.closest('label');
            var text = label ? label.textContent.trim() : '';
            if (text) {
                names.push(text);
            }
        });
        summary.textContent = names.length ? names.join(', ') : 'No roles';
    }

    function bindAccessInherit(root) {
        (root || document).querySelectorAll('.access-action').forEach(function (action) {
            var inherit = action.querySelector('input[type="checkbox"][name$="Inherit"]');
            if (!inherit || inherit.dataset.accessBound) {
                return;
            }
            inherit.dataset.accessBound = '1';
            inherit.addEventListener('change', function () {
                syncAction(action);
            });
            action.querySelectorAll('input[type="checkbox"][name$="RoleIds"]').forEach(function (box) {
                box.addEventListener('change', function () {
                    var dropdown = box.closest('.access-check-dropdown');
                    if (dropdown) {
                        syncDropdownSummary(dropdown);
                    }
                });
            });
            syncAction(action);
        });
    }

    function bindAccessDropdowns(root) {
        (root || document).querySelectorAll('.access-check-dropdown').forEach(function (dropdown) {
            if (dropdown.dataset.accessDropBound) {
                return;
            }
            dropdown.dataset.accessDropBound = '1';
            dropdown.addEventListener('toggle', function () {
                if (!dropdown.open) {
                    return;
                }
                document.querySelectorAll('.access-check-dropdown[open]').forEach(function (other) {
                    if (other !== dropdown) {
                        other.removeAttribute('open');
                    }
                });
            });
        });
    }

    function showAccessTab(panel, key) {
        var tabs = panel.querySelectorAll('.access-tabs [role="tab"]');
        var panes = panel.querySelectorAll('.access-action[data-access-panel]');
        var found = false;
        panes.forEach(function (pane) {
            if (pane.getAttribute('data-access-panel') === key) {
                found = true;
            }
        });
        if (!found) {
            key = 'read';
        }
        tabs.forEach(function (tab) {
            var on = tab.getAttribute('data-access-tab') === key;
            tab.classList.toggle('is-active', on);
            tab.setAttribute('aria-selected', on ? 'true' : 'false');
            tab.tabIndex = on ? 0 : -1;
        });
        panes.forEach(function (pane) {
            pane.classList.toggle('is-active', pane.getAttribute('data-access-panel') === key);
        });
    }

    function bindAccessTabs(root) {
        (root || document).querySelectorAll('.access-panel').forEach(function (panel) {
            var nav = panel.querySelector('.access-tabs');
            if (!nav || nav.dataset.accessTabsBound) {
                return;
            }
            nav.dataset.accessTabsBound = '1';
            nav.addEventListener('click', function (event) {
                var tab = event.target.closest('[role="tab"][data-access-tab]');
                if (!tab || !nav.contains(tab)) {
                    return;
                }
                event.preventDefault();
                showAccessTab(panel, tab.getAttribute('data-access-tab'));
                tab.focus();
            });
            nav.addEventListener('keydown', function (event) {
                var tabs = Array.prototype.slice.call(nav.querySelectorAll('[role="tab"]'));
                var current = document.activeElement;
                var index = tabs.indexOf(current);
                if (index < 0) {
                    return;
                }
                var next = -1;
                if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
                    next = (index + 1) % tabs.length;
                } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
                    next = (index - 1 + tabs.length) % tabs.length;
                } else if (event.key === 'Home') {
                    next = 0;
                } else if (event.key === 'End') {
                    next = tabs.length - 1;
                }
                if (next < 0) {
                    return;
                }
                event.preventDefault();
                showAccessTab(panel, tabs[next].getAttribute('data-access-tab'));
                tabs[next].focus();
            });
        });
    }

    function bindAccess(root) {
        bindAccessTabs(root);
        bindAccessDropdowns(root);
        bindAccessInherit(root);
    }

    document.addEventListener('click', function (event) {
        document.querySelectorAll('.access-check-dropdown[open]').forEach(function (dropdown) {
            if (!dropdown.contains(event.target)) {
                dropdown.removeAttribute('open');
            }
        });
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () {
            bindAccess(document);
        });
    } else {
        bindAccess(document);
    }
})(document);

