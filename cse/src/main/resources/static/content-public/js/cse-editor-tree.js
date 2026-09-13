/**
 * Shared editor tree: persist open folders, context menu, drag-and-drop.
 */
(function initResourceTree() {
    var tree = document.querySelector('.resource-tree');
    if (!tree) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initResourceTree);
        }
        return;
    }
    var meta = document.querySelector('meta[name="cse-context"]');
    var storageKey = 'cse.resource-tree.open:' + ((meta && meta.getAttribute('content')) || '');

    function groupRow(id) {
        if (!id) {
            return null;
        }
        var safe = (window.CSS && CSS.escape) ? CSS.escape(id) : String(id);
        return tree.querySelector(
                '.resource-tree-node:not(.resource-tree-file):not(.resource-tree-origin)'
                + ' > .resource-tree-row[data-group-id="' + safe + '"]');
    }

    function saveOpen() {
        var ids = [];
        var nodes = tree.querySelectorAll('.resource-tree-node.is-open');
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            if (node.classList.contains('resource-tree-origin')
                    || node.classList.contains('resource-tree-file')) {
                continue;
            }
            var row = node.querySelector(':scope > .resource-tree-row');
            var id = row ? row.getAttribute('data-group-id') : '';
            if (id) {
                ids.push(id);
            }
        }
        try {
            localStorage.setItem(storageKey, JSON.stringify(ids));
        } catch (ignore) {
        }
    }

    function setExpanded(node, open) {
        if (!node || node.classList.contains('resource-tree-origin')
                || node.classList.contains('resource-tree-file')) {
            return;
        }
        node.classList.toggle('is-open', open);
        var row = node.querySelector(':scope > .resource-tree-row');
        var toggle = row ? row.querySelector(':scope > .resource-tree-toggle:not(.is-leaf)') : null;
        if (toggle) {
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        }
    }

    function collapseNested(node) {
        var nested = node.querySelectorAll('.resource-tree-node.is-open');
        for (var i = 0; i < nested.length; i++) {
            setExpanded(nested[i], false);
        }
    }

    function restoreOpen() {
        var raw;
        try {
            raw = localStorage.getItem(storageKey);
        } catch (ignore) {
            return;
        }
        if (!raw) {
            return;
        }
        var ids;
        try {
            ids = JSON.parse(raw);
        } catch (ignore) {
            return;
        }
        if (!Array.isArray(ids)) {
            return;
        }
        for (var i = 0; i < ids.length; i++) {
            var row = groupRow(ids[i]);
            var node = row ? row.closest('.resource-tree-node') : null;
            if (!node) {
                continue;
            }
            setExpanded(node, true);
        }
    }

    restoreOpen();
    saveOpen();
    tree.addEventListener('cse-tree-persist', saveOpen);
    tree.addEventListener('click', function (e) {
        var toggle = e.target.closest('.resource-tree-toggle');
        if (!toggle || toggle.classList.contains('is-leaf')) {
            return;
        }
        var node = toggle.closest('.resource-tree-node');
        if (!node || node.classList.contains('resource-tree-origin')) {
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        var open = !node.classList.contains('is-open');
        setExpanded(node, open);
        if (!open) {
            collapseNested(node);
        }
        saveOpen();
    });
})();

(function initResourceTreeDnD() {
    var pane = document.querySelector('.resource-pane');
    var tree = document.querySelector('.resource-tree');
    var assignForm = document.getElementById('resource-assign-form');
    var assignResourceId = document.getElementById('assignResourceId');
    var assignGroupId = document.getElementById('assignGroupId');
    var moveForm = document.getElementById('resource-group-move-form');
    var moveGroupId = document.getElementById('dndMoveGroupId');
    var moveParentId = document.getElementById('dndMoveParentId');
    if (!tree || !assignForm || !assignResourceId || !assignGroupId
            || !moveForm || !moveGroupId || !moveParentId) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initResourceTreeDnD);
        }
        return;
    }
    var scope = document.querySelector('.resource-browser') || document;
    var defaultGroupId = tree.getAttribute('data-default-group-id') || '';
    var suppressClick = false;

    function isResourceDrag() {
        return document.body.classList.contains('is-resource-drag');
    }

    function isGroupDrag() {
        return document.body.classList.contains('is-group-drag');
    }

    function clearDropTargets() {
        var marked = document.querySelectorAll('.is-drop-target');
        for (var i = 0; i < marked.length; i++) {
            marked[i].classList.remove('is-drop-target');
        }
    }

    function isEmbedDrag() {
        return document.body.classList.contains('is-embed-drag');
    }

    function embedSnippet(row) {
        if (!row) {
            return '';
        }
        var tag = (row.getAttribute('data-embed-tag') || '').trim();
        var id = (row.getAttribute('data-embed-id') || '').trim();
        if (!tag || !id || !/^cse-[a-z]+$/.test(tag) || /["<>]/.test(id)) {
            return '';
        }
        return '<' + tag + ' id="' + id + '"></' + tag + '>';
    }

    function isEmbedDropTarget(el) {
        return !!(el && el.closest && el.closest('textarea.cse-embed-drop, #description.cse-embed-drop'));
    }

    function clearDrag() {
        document.body.classList.remove('is-resource-drag');
        document.body.classList.remove('is-group-drag');
        document.body.classList.remove('is-embed-drag');
        var dragging = document.querySelectorAll('.is-dragging');
        for (var i = 0; i < dragging.length; i++) {
            dragging[i].classList.remove('is-dragging');
        }
        clearDropTargets();
    }

    function isDescendantGroup(ancestorId, otherId) {
        if (!ancestorId || !otherId || ancestorId === otherId) {
            return false;
        }
        var ancestor = document.getElementById('group-' + ancestorId);
        var other = document.getElementById('group-' + otherId);
        return !!(ancestor && other && ancestor.contains(other));
    }

    function findDropTarget(e) {
        if (pane) {
            var folder = e.target.closest('.resource-folder-tile');
            if (folder && pane.contains(folder)) {
                return folder;
            }
        }
        var row = e.target.closest('.resource-tree-row');
        if (row && tree.contains(row)) {
            return row;
        }
        return null;
    }

    function canDropOn(target) {
        if (!target) {
            return false;
        }
        var targetId = target.getAttribute('data-group-id') || '';
        if (!targetId) {
            return false;
        }
        var dragging = document.querySelector('.is-dragging');
        if (isResourceDrag()) {
            if (target.getAttribute('data-accept-files') !== 'true') {
                return false;
            }
            var fromGroup = dragging
                    ? (dragging.getAttribute('data-group-id') || defaultGroupId) : '';
            return targetId !== fromGroup;
        }
        if (!isGroupDrag()) {
            return false;
        }
        if (target.getAttribute('data-can-create') !== 'true') {
            return false;
        }
        var fromId = dragging ? (dragging.getAttribute('data-group-id') || '') : '';
        var fromParent = dragging ? (dragging.getAttribute('data-parent-id') || '') : '';
        if (!fromId || fromId === targetId || fromParent === targetId
                || isDescendantGroup(fromId, targetId)) {
            return false;
        }
        return true;
    }

    function startGroupDrag(el, id) {
        if (!el || !id) {
            return false;
        }
        el.classList.add('is-dragging');
        document.body.classList.add('is-group-drag');
        return true;
    }

    if (pane) {
        pane.addEventListener('dragstart', function (e) {
            var folder = e.target.closest('.resource-folder-tile');
            if (folder && pane.contains(folder)) {
                if (e.target.closest('.thumb-actions')) {
                    e.preventDefault();
                    return;
                }
                if (folder.getAttribute('data-can-drag') === 'false') {
                    e.preventDefault();
                    return;
                }
                var folderId = folder.getAttribute('data-group-id');
                if (!folderId) {
                    e.preventDefault();
                    return;
                }
                e.dataTransfer.setData('application/x-cse-group', folderId);
                e.dataTransfer.setData('text/plain', folderId);
                e.dataTransfer.effectAllowed = 'move';
                startGroupDrag(folder, folderId);
                return;
            }
            var row = e.target.closest('.resource-row');
            if (!row || !pane.contains(row)) {
                return;
            }
            if (e.target.closest('.thumb-actions, .resource-tip')) {
                e.preventDefault();
                return;
            }
            var id = row.getAttribute('data-resource-id');
            if (!id) {
                e.preventDefault();
                return;
            }
            e.dataTransfer.setData('application/x-cse-resource', id);
            e.dataTransfer.setData('text/plain', id);
            e.dataTransfer.effectAllowed = 'move';
            row.classList.add('is-dragging');
            document.body.classList.add('is-resource-drag');
        });
    }

    tree.addEventListener('dragstart', function (e) {
        if (e.target.closest('.resource-tree-toggle, input, .resource-tree-create, .resource-tree-rename')) {
            e.preventDefault();
            return;
        }
        var row = e.target.closest('.resource-tree-row');
        if (!row) {
            e.preventDefault();
            return;
        }
        var resourceId = row.getAttribute('data-resource-id');
        var snippet = embedSnippet(row);
        if (resourceId) {
            e.dataTransfer.setData('application/x-cse-resource', resourceId);
            if (snippet) {
                e.dataTransfer.setData('application/x-cse-embed', snippet);
                e.dataTransfer.setData('text/plain', snippet);
                e.dataTransfer.effectAllowed = 'copyMove';
            } else {
                e.dataTransfer.setData('text/plain', resourceId);
                e.dataTransfer.effectAllowed = 'move';
            }
            row.classList.add('is-dragging');
            document.body.classList.add('is-resource-drag');
            return;
        }
        if (snippet) {
            e.dataTransfer.setData('application/x-cse-embed', snippet);
            e.dataTransfer.setData('text/plain', snippet);
            e.dataTransfer.effectAllowed = 'copy';
            row.classList.add('is-dragging');
            document.body.classList.add('is-embed-drag');
            return;
        }
        if (row.getAttribute('data-can-drag') !== 'true') {
            e.preventDefault();
            return;
        }
        var id = row.getAttribute('data-group-id');
        if (!id) {
            e.preventDefault();
            return;
        }
        e.dataTransfer.setData('application/x-cse-group', id);
        e.dataTransfer.setData('text/plain', id);
        e.dataTransfer.effectAllowed = 'move';
        startGroupDrag(row, id);
    });

    document.addEventListener('dragend', function () {
        if (isResourceDrag() || isGroupDrag() || isEmbedDrag()) {
            suppressClick = true;
        }
        clearDrag();
    });

    document.addEventListener('click', function (e) {
        if (!suppressClick) {
            return;
        }
        suppressClick = false;
        if (e.target.closest('.resource-tree-link, .resource-folder-link')) {
            e.preventDefault();
            e.stopPropagation();
        }
    }, true);

    scope.addEventListener('dragover', function (e) {
        if (isEmbedDropTarget(e.target)) {
            return;
        }
        if (!isResourceDrag() && !isGroupDrag()) {
            return;
        }
        clearDropTargets();
        var target = findDropTarget(e);
        if (!canDropOn(target)) {
            return;
        }
        e.preventDefault();
        e.dataTransfer.dropEffect = 'move';
        target.classList.add('is-drop-target');
    });

    scope.addEventListener('dragleave', function (e) {
        if (!e.relatedTarget || !scope.contains(e.relatedTarget)) {
            clearDropTargets();
        }
    });

    scope.addEventListener('drop', function (e) {
        if (isEmbedDropTarget(e.target)) {
            return;
        }
        if (!isResourceDrag() && !isGroupDrag()) {
            return;
        }
        var resourceDrag = isResourceDrag();
        var target = findDropTarget(e);
        var allowed = canDropOn(target);
        var resourceId = (e.dataTransfer.getData('application/x-cse-resource') || '').trim();
        var groupId = (e.dataTransfer.getData('application/x-cse-group')
                || (!resourceId ? e.dataTransfer.getData('text/plain') : '') || '').trim();
        if (resourceDrag) {
            resourceId = resourceId || (e.dataTransfer.getData('text/plain') || '').trim();
        }
        e.preventDefault();
        var fromGroupEl = document.querySelector('.is-dragging');
        var fromParent = fromGroupEl ? (fromGroupEl.getAttribute('data-parent-id') || '') : '';
        var fromResourceGroup = fromGroupEl
                ? (fromGroupEl.getAttribute('data-group-id') || defaultGroupId) : '';
        clearDrag();
        if (!allowed || !target) {
            return;
        }
        var targetId = target.getAttribute('data-group-id') || '';
        if (resourceDrag) {
            if (!resourceId || targetId === fromResourceGroup) {
                return;
            }
            assignResourceId.value = resourceId;
            assignGroupId.value = targetId;
            assignForm.requestSubmit();
            return;
        }
        if (!groupId || groupId === targetId) {
            return;
        }
        if (fromParent === targetId) {
            return;
        }
        moveGroupId.value = groupId;
        moveParentId.value = targetId;
        moveForm.requestSubmit();
    });
})();

(function initResourceTreeMenu() {
    var HOST = '.resource-tree-row, .resource-row, .page-row, .playlist-row, .app-row, .theme-row, .category-row, .resource-folder-tile';
    var tree = document.querySelector('.resource-tree');
    var browser = document.querySelector('.resource-browser') || tree;
    var menu = document.getElementById('resource-tree-menu');
    var holder = document.getElementById('resource-tree-create-holder');
    var form = document.getElementById('resource-tree-create');
    var parentInput = document.getElementById('treeCreateParent');
    var nameInput = document.getElementById('treeCreateName');
    var renameHolder = document.getElementById('resource-tree-rename-holder');
    var renameForm = document.getElementById('resource-tree-rename');
    var renameKindInput = document.getElementById('treeRenameKind');
    var renameIdInput = document.getElementById('treeRenameId');
    var renameNameInput = document.getElementById('treeRenameName');
    var renameItem = menu ? menu.querySelector('[data-action="rename"]') : null;
    var createItem = menu ? menu.querySelector('[data-action="new-group"]') : null;
    var createCategoryItem = menu ? menu.querySelector('[data-action="new-category"]') : null;
    var createPageWrap = menu ? menu.querySelector('[data-action="new-page-wrap"]') : null;
    var createPageItem = menu ? menu.querySelector('[data-action="new-page"]') : null;
    var createPageSubmenu = document.getElementById('resource-tree-page-submenu');
    var createAlbumWrap = menu ? menu.querySelector('[data-action="new-album-wrap"]') : null;
    var createAlbumItem = menu ? menu.querySelector('[data-action="new-album"]') : null;
    var createAlbumSubmenu = document.getElementById('resource-tree-album-submenu');
    var editItem = menu ? menu.querySelector('[data-action="edit"]') : null;
    var openItem = menu ? menu.querySelector('[data-action="open"]') : null;
    var downloadItem = menu ? menu.querySelector('[data-action="download"]') : null;
    var activateItem = menu ? menu.querySelector('[data-action="activate"]') : null;
    var deleteItem = menu ? menu.querySelector('[data-action="delete"]') : null;
    var catalogDeleteForm = document.getElementById('catalog-delete-form');
    var catalogDeleteReturn = document.getElementById('catalogDeleteReturn');
    var catalogDeleteId = document.getElementById('catalogDeleteId');
    var catalogDeleteName = document.getElementById('catalogDeleteName');
    var catalogDeletePath = document.getElementById('catalogDeletePath');
    var catalogDeleteSlug = document.getElementById('catalogDeleteSlug');
    var catalogDeleteTheme = document.getElementById('catalogDeleteTheme');
    var catalogDeleteGroup = document.getElementById('catalogDeleteGroup');
    var catalogActivateForm = document.getElementById('catalog-activate-form');
    var catalogActivateTheme = document.getElementById('catalogActivateTheme');
    var catalogActivateReturn = document.getElementById('catalogActivateReturn');
    if (!tree || !browser || !menu || !holder || !form || !parentInput || !nameInput
            || !renameHolder || !renameForm || !renameKindInput || !renameIdInput || !renameNameInput
            || !renameItem || !createItem || !createCategoryItem
            || !createPageWrap || !createPageItem || !createPageSubmenu
            || !createAlbumWrap || !createAlbumItem || !createAlbumSubmenu
            || !editItem || !openItem || !downloadItem || !activateItem || !deleteItem
            || !catalogDeleteForm || !catalogDeleteReturn || !catalogDeleteId
            || !catalogDeleteName || !catalogDeletePath || !catalogDeleteSlug
            || !catalogDeleteTheme || !catalogDeleteGroup
            || !catalogActivateForm || !catalogActivateTheme || !catalogActivateReturn) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initResourceTreeMenu);
        }
        return;
    }
    var menuHost = null;
    var menuParentId = '';
    var menuCreateHref = '';
    var menuCreatePageHref = '';
    var menuCreatePageMenu = false;
    var menuCreateAlbumHref = '';
    var menuCreateAlbumMenu = false;
    var menuEditHref = '';
    var menuOpenHref = '';
    var menuDownloadHref = '';

    function flag(el, name) {
        return !!(el && el.getAttribute(name) === 'true');
    }

    function appPath(path) {
        var meta = document.querySelector('meta[name="cse-context"]');
        var ctx = meta ? (meta.getAttribute('content') || '') : '';
        if (!ctx || ctx === '/') {
            return path;
        }
        return ctx.replace(/\/$/, '') + path;
    }

    function closestHost(el) {
        if (!el || !el.closest) {
            return null;
        }
        if (el.closest('.resource-plus-tile, #resource-tree-menu')) {
            return null;
        }
        return el.closest(HOST);
    }

    function optionsFromHost(host) {
        if (!host) {
            return {
                host: null,
                parentId: '',
                canCreate: true,
                canRename: false,
                canDelete: false,
                canActivate: false,
                createHref: '',
                createPageHref: '',
                createPageMenu: false,
                createAlbumHref: '',
                createAlbumMenu: false,
                editHref: '',
                openHref: '',
                downloadHref: ''
            };
        }
        return {
            host: host,
            parentId: host.getAttribute('data-group-id') || '',
            canCreate: flag(host, 'data-can-create'),
            canRename: flag(host, 'data-can-rename'),
            canDelete: flag(host, 'data-can-delete'),
            canActivate: flag(host, 'data-can-activate'),
            createHref: host.getAttribute('data-create-href') || '',
            createPageHref: host.getAttribute('data-create-page-href') || '',
            createPageMenu: flag(host, 'data-create-page-menu'),
            createAlbumHref: host.getAttribute('data-create-album-href') || '',
            createAlbumMenu: flag(host, 'data-create-album-menu'),
            editHref: host.getAttribute('data-edit-href') || '',
            openHref: host.getAttribute('data-open-href') || '',
            downloadHref: host.getAttribute('data-download-href') || ''
        };
    }

    function hasActions(options) {
        return options.canCreate || options.canRename || options.canDelete || options.canActivate
                || options.createHref || options.createPageHref || options.createPageMenu
                || options.createAlbumHref || options.createAlbumMenu
                || options.editHref || options.openHref || options.downloadHref;
    }

    function currentReturnGroup(host) {
        var field = renameForm.querySelector('[name="returnGroup"]');
        if (field && field.value) {
            return field.value;
        }
        return host ? (host.getAttribute('data-return-group') || '') : '';
    }

    function deleteConfirm(host) {
        var custom = host.getAttribute('data-delete-confirm');
        if (custom) {
            return custom;
        }
        switch (host.getAttribute('data-kind') || '') {
            case 'resource':
                return 'Delete this resource?';
            case 'page':
                return 'Delete this page?';
            case 'album':
                return 'Delete this album?';
            case 'playlist':
                return 'Delete this playlist?';
            case 'category':
                return 'Delete this category and all its pages?';
            case 'app':
                return 'Delete this app from Mongo and disk?';
            case 'theme':
                return 'Remove this theme pack from Mongo and disk?';
            case 'group':
                return 'Delete this group and all its resources?';
            default:
                return 'Delete this item?';
        }
    }

    function submitDelete(host) {
        var kind = host.getAttribute('data-kind') || '';
        var id = host.getAttribute('data-id') || '';
        var key = host.getAttribute('data-key') || id;
        if (!window.confirm(deleteConfirm(host))) {
            return;
        }
        catalogDeleteId.value = '';
        catalogDeleteName.value = '';
        catalogDeletePath.value = '';
        catalogDeleteSlug.value = '';
        catalogDeleteTheme.value = '';
        catalogDeleteGroup.value = '';
        catalogDeleteReturn.value = currentReturnGroup(host);
        if (kind === 'resource') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/resources/delete'));
            catalogDeletePath.value = key;
        } else if (kind === 'page' || kind === 'album') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/delete'));
            catalogDeleteName.value = id;
        } else if (kind === 'playlist') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/playlist/delete'));
            catalogDeleteId.value = id;
        } else if (kind === 'category') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/category/delete'));
            catalogDeleteId.value = id;
        } else if (kind === 'app') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/apps/delete'));
            catalogDeleteSlug.value = id;
        } else if (kind === 'theme') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/themes/delete'));
            catalogDeleteTheme.value = id;
        } else if (kind === 'group') {
            catalogDeleteForm.setAttribute('action', appPath('/editor/resources/group/delete'));
            catalogDeleteGroup.value = id;
        } else {
            return;
        }
        catalogDeleteForm.requestSubmit();
    }

    function fillHrefSubmenu(submenu, attr) {
        submenu.innerHTML = '';
        var rows = tree.querySelectorAll('.resource-tree-row[' + attr + ']');
        for (var i = 0; i < rows.length; i++) {
            var href = rows[i].getAttribute(attr) || '';
            if (!href) {
                continue;
            }
            var item = document.createElement('button');
            item.type = 'button';
            item.setAttribute('role', 'menuitem');
            item.setAttribute('data-href', href);
            item.textContent = rows[i].getAttribute('data-name') || 'Item';
            submenu.appendChild(item);
        }
        return submenu.children.length;
    }

    function hideMenu() {
        menu.hidden = true;
    }

    function putCreateBack() {
        holder.appendChild(form);
        form.hidden = true;
        nameInput.value = '';
    }

    function putRenameBack() {
        var host = renameForm.closest(HOST);
        if (host) {
            host.classList.remove('is-renaming');
        }
        renameHolder.appendChild(renameForm);
        renameForm.hidden = true;
        renameNameInput.value = '';
    }

    function showCreate(parentId, hostNode) {
        hideMenu();
        putRenameBack();
        parentInput.value = parentId || '';
        if (hostNode) {
            hostNode.classList.add('is-open');
            var hostToggle = hostNode.querySelector(
                    ':scope > .resource-tree-row .resource-tree-toggle:not(.is-leaf)');
            if (hostToggle) {
                hostToggle.setAttribute('aria-expanded', 'true');
            }
            tree.dispatchEvent(new Event('cse-tree-persist'));
            var children = hostNode.querySelector(':scope > .resource-tree-children');
            if (children) {
                children.insertBefore(form, children.firstChild);
            } else {
                hostNode.appendChild(form);
            }
        } else {
            var roots = tree.querySelector('.resource-tree-roots');
            if (roots) {
                roots.insertBefore(form, roots.firstChild);
            } else {
                tree.insertBefore(form, tree.firstChild);
            }
        }
        form.hidden = false;
        nameInput.focus();
    }

    function showRename(host) {
        if (!host || host.getAttribute('data-can-rename') !== 'true') {
            return;
        }
        hideMenu();
        putCreateBack();
        putRenameBack();
        renameKindInput.value = host.getAttribute('data-kind') || '';
        renameIdInput.value = host.getAttribute('data-id') || '';
        renameNameInput.value = host.getAttribute('data-name') || '';
        host.classList.add('is-renaming');
        var mount = host.classList.contains('resource-tree-row')
                ? host
                : (host.querySelector('.item-thumb-wrap') || host);
        mount.appendChild(renameForm);
        renameForm.hidden = false;
        renameNameInput.focus();
        renameNameInput.select();
    }

    function showMenu(x, y, options) {
        menuHost = options.host || null;
        menuParentId = options.parentId || '';
        menuCreateHref = options.createHref || '';
        menuCreatePageHref = options.createPageHref || '';
        menuCreatePageMenu = !!options.createPageMenu;
        menuCreateAlbumHref = options.createAlbumHref || '';
        menuCreateAlbumMenu = !!options.createAlbumMenu;
        menuEditHref = options.editHref || '';
        menuOpenHref = options.openHref || '';
        menuDownloadHref = options.downloadHref || '';
        renameItem.hidden = !options.canRename;
        createItem.hidden = !options.canCreate;
        createCategoryItem.hidden = !menuCreateHref;
        var pageChoices = menuCreatePageMenu ? fillHrefSubmenu(createPageSubmenu, 'data-create-page-href') : 0;
        createPageWrap.hidden = !menuCreatePageHref && pageChoices === 0;
        createPageWrap.classList.toggle('has-submenu', pageChoices > 0);
        createPageSubmenu.hidden = pageChoices === 0;
        var albumChoices = menuCreateAlbumMenu ? fillHrefSubmenu(createAlbumSubmenu, 'data-create-album-href') : 0;
        createAlbumWrap.hidden = !menuCreateAlbumHref && albumChoices === 0;
        createAlbumWrap.classList.toggle('has-submenu', albumChoices > 0);
        createAlbumSubmenu.hidden = albumChoices === 0;
        editItem.hidden = !menuEditHref;
        openItem.hidden = !menuOpenHref;
        downloadItem.hidden = !menuDownloadHref;
        activateItem.hidden = !options.canActivate;
        deleteItem.hidden = !options.canDelete;
        if (renameItem.hidden && createItem.hidden && createCategoryItem.hidden
                && createPageWrap.hidden && createAlbumWrap.hidden
                && editItem.hidden && openItem.hidden && downloadItem.hidden
                && activateItem.hidden && deleteItem.hidden) {
            hideMenu();
            return;
        }
        menu.hidden = false;
        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
    }

    function openHostMenu(e, host, fallbackRoot) {
        var options = host ? optionsFromHost(host) : optionsFromHost(null);
        if (host && !hasActions(options)) {
            hideMenu();
            return false;
        }
        if (!host && !fallbackRoot) {
            hideMenu();
            return false;
        }
        e.preventDefault();
        var x = e.clientX;
        var y = e.clientY;
        if (typeof x !== 'number' || (x === 0 && y === 0 && host)) {
            var box = host.getBoundingClientRect();
            x = box.left + 12;
            y = box.bottom;
        }
        showMenu(x, y, options);
        return true;
    }

    browser.addEventListener('contextmenu', function (e) {
        if (e.target.closest('#resource-tree-menu, .resource-tree-create, .resource-tree-rename')) {
            return;
        }
        if (e.target.closest('.resource-plus-tile')) {
            hideMenu();
            return;
        }
        var host = closestHost(e.target);
        if (host) {
            openHostMenu(e, host, false);
            return;
        }
        if (tree.contains(e.target) && tree.getAttribute('data-can-create-root') === 'true') {
            openHostMenu(e, null, true);
        }
    });

    tree.addEventListener('click', function (e) {
        var link = e.target.closest('.resource-tree-link');
        if (!link) {
            return;
        }
        var row = link.closest('.resource-tree-row');
        var node = row ? row.closest('.resource-tree-node') : null;
        if (!row || !node || !node.classList.contains('is-selected')) {
            return;
        }
        if (row.getAttribute('data-can-rename') !== 'true') {
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        showRename(row);
    });

    browser.addEventListener('keydown', function (e) {
        if (e.key === 'F2') {
            if (!renameForm.hidden) {
                return;
            }
            var renameHost = closestHost(e.target);
            if (!renameHost || renameHost.getAttribute('data-can-rename') !== 'true') {
                return;
            }
            e.preventDefault();
            showRename(renameHost);
            return;
        }
        if (e.key !== 'ContextMenu' && !(e.shiftKey && e.key === 'F10')) {
            return;
        }
        var host = closestHost(e.target);
        if (!host) {
            return;
        }
        var options = optionsFromHost(host);
        if (!hasActions(options)) {
            return;
        }
        e.preventDefault();
        var box = host.getBoundingClientRect();
        showMenu(box.left + 12, box.bottom, options);
    });

    menu.addEventListener('click', function (e) {
        var renameAction = e.target.closest('[data-action="rename"]');
        if (renameAction) {
            showRename(menuHost);
            return;
        }
        var editAction = e.target.closest('[data-action="edit"]');
        if (editAction && menuEditHref) {
            hideMenu();
            window.location.href = menuEditHref;
            return;
        }
        var openAction = e.target.closest('[data-action="open"]');
        if (openAction && menuOpenHref) {
            hideMenu();
            window.open(menuOpenHref, '_blank', 'noopener');
            return;
        }
        var downloadAction = e.target.closest('[data-action="download"]');
        if (downloadAction && menuDownloadHref) {
            hideMenu();
            window.location.href = menuDownloadHref;
            return;
        }
        var activateAction = e.target.closest('[data-action="activate"]');
        if (activateAction && menuHost && flag(menuHost, 'data-can-activate')) {
            hideMenu();
            catalogActivateTheme.value = menuHost.getAttribute('data-id') || '';
            catalogActivateReturn.value = currentReturnGroup(menuHost);
            catalogActivateForm.requestSubmit();
            return;
        }
        var deleteAction = e.target.closest('[data-action="delete"]');
        if (deleteAction && menuHost && flag(menuHost, 'data-can-delete')) {
            hideMenu();
            submitDelete(menuHost);
            return;
        }
        var createCategoryAction = e.target.closest('[data-action="new-category"]');
        if (createCategoryAction && menuCreateHref) {
            hideMenu();
            window.location.href = menuCreateHref;
            return;
        }
        var submenuPage = e.target.closest('#resource-tree-page-submenu [data-href]');
        if (submenuPage) {
            hideMenu();
            window.location.href = submenuPage.getAttribute('data-href');
            return;
        }
        var submenuAlbum = e.target.closest('#resource-tree-album-submenu [data-href]');
        if (submenuAlbum) {
            hideMenu();
            window.location.href = submenuAlbum.getAttribute('data-href');
            return;
        }
        var createPageAction = e.target.closest('[data-action="new-page"]');
        if (createPageAction && menuCreatePageHref) {
            hideMenu();
            window.location.href = menuCreatePageHref;
            return;
        }
        if (createPageAction && menuCreatePageMenu) {
            return;
        }
        var createAlbumAction = e.target.closest('[data-action="new-album"]');
        if (createAlbumAction && menuCreateAlbumHref) {
            hideMenu();
            window.location.href = menuCreateAlbumHref;
            return;
        }
        if (createAlbumAction && menuCreateAlbumMenu) {
            return;
        }
        var item = e.target.closest('[data-action="new-group"]');
        if (!item) {
            return;
        }
        var parentId = menuParentId;
        var createHost = parentId
                ? document.getElementById('group-' + parentId)
                : null;
        showCreate(parentId, createHost);
    });

    renameForm.addEventListener('submit', function (e) {
        var next = renameNameInput.value.trim();
        var host = renameForm.closest(HOST);
        var current = host ? (host.getAttribute('data-name') || '') : '';
        if (!renameKindInput.value || !renameIdInput.value || next === current) {
            e.preventDefault();
            putRenameBack();
        }
    });

    document.addEventListener('click', function (e) {
        if (!menu.hidden && !menu.contains(e.target)) {
            hideMenu();
        }
    });

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            hideMenu();
            if (!form.hidden) {
                putCreateBack();
            }
            if (!renameForm.hidden) {
                putRenameBack();
            }
        }
    });
})();

(function initEmbedDrop() {
    var areas = document.querySelectorAll('textarea.cse-embed-drop');
    if (!areas.length) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initEmbedDrop);
        }
        return;
    }

    function insertAtCaret(textarea, text) {
        var start = textarea.selectionStart;
        var end = textarea.selectionEnd;
        var value = textarea.value;
        var before = value.slice(0, start);
        var after = value.slice(end);
        var padBefore = before.length > 0 && !/\n$/.test(before) ? '\n' : '';
        var padAfter = after.length > 0 && !/^\n/.test(after) ? '\n' : '';
        var insert = padBefore + text + padAfter;
        textarea.value = before + insert + after;
        var pos = (before + insert).length;
        textarea.selectionStart = textarea.selectionEnd = pos;
        textarea.focus();
        textarea.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function snippetFrom(e) {
        var embed = (e.dataTransfer.getData('application/x-cse-embed') || '').trim();
        if (embed) {
            return embed;
        }
        var plain = (e.dataTransfer.getData('text/plain') || '').trim();
        return /^<cse-[a-z]+ id="[^"]+"><\/cse-[a-z]+>$/.test(plain) ? plain : '';
    }

    for (var i = 0; i < areas.length; i++) {
        areas[i].addEventListener('dragover', function (e) {
            if (snippetFrom(e) || (e.dataTransfer.types && (
                    Array.prototype.indexOf.call(e.dataTransfer.types, 'application/x-cse-embed') >= 0
                    || document.body.classList.contains('is-embed-drag')
                    || document.body.classList.contains('is-resource-drag')))) {
                e.preventDefault();
                e.dataTransfer.dropEffect = 'copy';
            }
        });
        areas[i].addEventListener('drop', function (e) {
            var snippet = snippetFrom(e);
            if (!snippet) {
                return;
            }
            e.preventDefault();
            e.stopPropagation();
            insertAtCaret(this, snippet);
        });
    }
})();
