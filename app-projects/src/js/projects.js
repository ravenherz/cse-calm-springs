(function () {
    var TABLES = [
        {
            name: 'projects',
            access: 'member-write',
            schema: {
                type: 'object',
                properties: {
                    name: { type: 'string', maxLength: 80 },
                    description: { type: 'string', maxLength: 4000 },
                    archived: { type: 'boolean' },
                    author: { type: 'string' },
                    code: { type: 'string', maxLength: 8 },
                    issueSeq: { type: 'integer' }
                }
            }
        },
        {
            name: 'issues',
            access: 'member-write',
            schema: {
                type: 'object',
                properties: {
                    projectId: { type: 'string' },
                    title: { type: 'string', maxLength: 160 },
                    body: { type: 'string', maxLength: 8000 },
                    status: { type: 'string' },
                    priority: { type: 'string' },
                    author: { type: 'string' },
                    taskNumber: { type: 'integer' },
                    taskKey: { type: 'string' }
                }
            }
        },
        {
            name: 'comments',
            access: 'member-write',
            schema: {
                type: 'object',
                properties: {
                    issueId: { type: 'string' },
                    body: { type: 'string', maxLength: 4000 },
                    author: { type: 'string' }
                }
            }
        }
    ];
    var STATUSES = ['open', 'doing', 'done'];
    var MOVE = { open: 'doing', doing: 'done', done: 'open' };

    var projectsStore = CseAppData.connect({ table: 'projects' });
    var issuesStore = CseAppData.connect({ table: 'issues' });
    var commentsStore = CseAppData.connect({ table: 'comments' });

    var state = {
        site: null,
        schema: null,
        projects: [],
        issues: [],
        comments: [],
        mine: false,
        showArchived: false,
        projectId: null,
        issueId: null,
        busy: false,
        syncingDrawer: false
    };

    var els = {
        banner: document.getElementById('banner'),
        who: document.getElementById('who'),
        load: document.getElementById('panel-load'),
        guest: document.getElementById('panel-guest'),
        store: document.getElementById('panel-store'),
        app: document.getElementById('panel-app'),
        projectList: document.getElementById('project-list'),
        formProject: document.getElementById('form-project'),
        projectName: document.getElementById('project-name'),
        projectCode: document.getElementById('project-code'),
        projectDescription: document.getElementById('project-description'),
        showArchived: document.getElementById('show-archived'),
        boardTitle: document.getElementById('board-title'),
        boardCode: document.getElementById('board-code'),
        boardLead: document.getElementById('board-lead'),
        boardActions: document.getElementById('board-actions'),
        mineOnly: document.getElementById('mine-only'),
        formProjectCode: document.getElementById('form-project-code'),
        existingProjectCode: document.getElementById('existing-project-code'),
        formIssue: document.getElementById('form-issue'),
        issueTitle: document.getElementById('issue-title'),
        issuePriority: document.getElementById('issue-priority'),
        issueKeyHint: document.getElementById('issue-key-hint'),
        columns: document.getElementById('board-columns'),
        empty: document.getElementById('board-empty'),
        backdrop: document.getElementById('drawer-backdrop'),
        drawer: document.getElementById('drawer'),
        editTaskKey: document.getElementById('edit-task-key'),
        editTitle: document.getElementById('edit-title'),
        editStatus: document.getElementById('edit-status'),
        editPriority: document.getElementById('edit-priority'),
        editBody: document.getElementById('edit-body'),
        editMeta: document.getElementById('edit-meta'),
        commentList: document.getElementById('comment-list'),
        commentBody: document.getElementById('comment-body')
    };

    function base() {
        return projectsStore.base || '';
    }

    function siteHref() {
        return base() ? base() + '/' : '/';
    }

    function dataOf(row) {
        return row && row.data ? row.data : {};
    }

    function text(value) {
        return value == null ? '' : String(value);
    }

    function normalizeCode(value) {
        return text(value).trim().toUpperCase();
    }

    function validCode(code) {
        return /^[A-Z][A-Z0-9]{1,7}$/.test(code);
    }

    function projectCodeOf(row) {
        return normalizeCode(dataOf(row).code);
    }

    function codeTaken(code, exceptId) {
        var needle = normalizeCode(code);
        return state.projects.some(function (row) {
            return row.id !== exceptId && projectCodeOf(row) === needle;
        });
    }

    function taskNumberOf(row) {
        var n = Number(dataOf(row).taskNumber);
        return Number.isFinite(n) && n > 0 ? n : 0;
    }

    function taskKeyOf(row, fallbackCode) {
        var key = text(dataOf(row).taskKey).trim().toUpperCase();
        if (key) {
            return key;
        }
        var n = taskNumberOf(row);
        var code = normalizeCode(fallbackCode);
        return n && validCode(code) ? code + '-' + n : '';
    }

    function maxTaskNumber(projectId) {
        var max = 0;
        issuesFor(projectId).forEach(function (row) {
            var n = taskNumberOf(row);
            if (n > max) {
                max = n;
            }
        });
        return max;
    }

    function bindUppercase(input) {
        if (!input) {
            return;
        }
        input.addEventListener('input', function () {
            var start = input.selectionStart;
            var end = input.selectionEnd;
            var next = input.value.toUpperCase();
            if (input.value !== next) {
                input.value = next;
                if (start != null) {
                    input.setSelectionRange(start, end);
                }
            }
        });
    }

    async function assignMissingKeys(project, code) {
        var issues = issuesFor(project.id).slice().sort(function (a, b) {
            return text(a.createdAt).localeCompare(text(b.createdAt));
        });
        var max = maxTaskNumber(project.id);
        for (var i = 0; i < issues.length; i++) {
            var row = issues[i];
            if (taskKeyOf(row, code)) {
                continue;
            }
            max += 1;
            var updated = await issuesStore.patch(row.id, {
                taskNumber: max,
                taskKey: code + '-' + max
            });
            replaceRow(state.issues, updated);
        }
        return max;
    }

    async function allocateTask(project) {
        var fresh = await projectsStore.get(project.id);
        var data = dataOf(fresh);
        var code = normalizeCode(data.code);
        if (!validCode(code)) {
            throw new Error('Set a project code before adding issues');
        }
        var seq = Number(data.issueSeq);
        if (!Number.isFinite(seq) || seq < 0) {
            seq = 0;
        }
        var next = Math.max(seq, maxTaskNumber(project.id)) + 1;
        var updated = await projectsStore.patch(project.id, { issueSeq: next });
        replaceRow(state.projects, updated);
        return { code: code, number: next, key: code + '-' + next };
    }

    function banner(message) {
        if (!message) {
            els.banner.hidden = true;
            els.banner.textContent = '';
            return;
        }
        els.banner.hidden = false;
        els.banner.textContent = message;
    }

    function showPanel(name) {
        els.load.hidden = name !== 'load';
        els.guest.hidden = name !== 'guest';
        els.store.hidden = name !== 'store';
        els.app.hidden = name !== 'app';
    }

    function signedIn() {
        return !!(state.site && state.site.authenticated);
    }

    function author() {
        return text(state.site && state.site.username);
    }

    function listAll(store, opts) {
        var items = [];
        function page(after) {
            var query = { limit: 100 };
            if (opts) {
                Object.keys(opts).forEach(function (key) {
                    query[key] = opts[key];
                });
            }
            if (after) {
                query.after = after;
            }
            return store.list(query).then(function (res) {
                (res.items || []).forEach(function (item) {
                    items.push(item);
                });
                if (res.after) {
                    return page(res.after);
                }
                return items;
            });
        }
        return page(null);
    }

    function tableNames(schema) {
        var names = {};
        ((schema && schema.tables) || []).forEach(function (table) {
            if (table && table.name) {
                names[table.name] = true;
            }
        });
        return names;
    }

    function byId(rows, id) {
        for (var i = 0; i < rows.length; i++) {
            if (rows[i].id === id) {
                return rows[i];
            }
        }
        return null;
    }

    function replaceRow(rows, row) {
        for (var i = 0; i < rows.length; i++) {
            if (rows[i].id === row.id) {
                rows[i] = row;
                return;
            }
        }
        rows.push(row);
    }

    function removeRow(rows, id) {
        for (var i = rows.length - 1; i >= 0; i--) {
            if (rows[i].id === id) {
                rows.splice(i, 1);
            }
        }
    }

    function parseRoute() {
        var hash = (window.location.hash || '').replace(/^#/, '');
        var parts = hash.split('/').filter(Boolean);
        var projectId = null;
        var issueId = null;
        if (parts[0] === 'p' && parts[1]) {
            projectId = decodeURIComponent(parts[1]);
            if (parts[2] === 'i' && parts[3]) {
                issueId = decodeURIComponent(parts[3]);
            }
        }
        return { projectId: projectId, issueId: issueId };
    }

    function writeRoute(projectId, issueId) {
        var next = '#';
        if (projectId) {
            next = '#p/' + encodeURIComponent(projectId);
            if (issueId) {
                next += '/i/' + encodeURIComponent(issueId);
            }
        }
        if ((window.location.hash || '#') !== next) {
            window.location.hash = next;
        }
    }

    function visibleProjects() {
        return state.projects.filter(function (row) {
            var archived = !!dataOf(row).archived;
            return state.showArchived || !archived || row.id === state.projectId;
        });
    }

    function issuesFor(projectId) {
        return state.issues.filter(function (row) {
            return dataOf(row).projectId === projectId;
        });
    }

    function commentsFor(issueId) {
        return state.comments.filter(function (row) {
            return dataOf(row).issueId === issueId;
        });
    }

    function statusOf(row) {
        var status = text(dataOf(row).status);
        return STATUSES.indexOf(status) >= 0 ? status : 'open';
    }

    function setBusy(on) {
        state.busy = !!on;
    }

    async function withBusy(work) {
        if (state.busy) {
            return;
        }
        setBusy(true);
        try {
            await work();
        } finally {
            setBusy(false);
        }
    }

    function fail(err) {
        if (err && err.status === 401) {
            showPanel('guest');
            closeDrawer();
            banner('');
            return;
        }
        banner((err && err.message) || 'Request failed');
    }

    function loginHref() {
        return (base() ? base() : '') + '/apps/login/?next='
            + encodeURIComponent(window.location.pathname + window.location.search);
    }

    function renderWho() {
        var name = author();
        els.who.hidden = !name;
        els.who.textContent = name;
        var href = siteHref();
        ['link-site', 'link-site-brand'].forEach(function (id) {
            var link = document.getElementById(id);
            if (link) {
                link.setAttribute('href', href);
            }
        });
        var signin = document.getElementById('link-signin');
        if (signin) {
            signin.setAttribute('href', loginHref());
        }
    }

    function renderProjects() {
        els.projectList.textContent = '';
        visibleProjects().forEach(function (row) {
            var data = dataOf(row);
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'project-item' + (row.id === state.projectId ? ' is-active' : '');
            button.addEventListener('click', function () {
                writeRoute(row.id, null);
            });
            var name = document.createElement('span');
            name.className = 'project-item-name';
            name.textContent = text(data.name) || '(untitled)';
            var meta = document.createElement('span');
            meta.className = 'project-item-meta';
            var count = issuesFor(row.id).length;
            var code = projectCodeOf(row);
            var bits = [];
            if (code) {
                bits.push(code);
            }
            if (data.archived) {
                bits.push('archived');
            }
            bits.push(count + (count === 1 ? ' issue' : ' issues'));
            meta.textContent = bits.join(' · ');
            button.appendChild(name);
            button.appendChild(meta);
            els.projectList.appendChild(button);
        });
    }

    function renderBoard() {
        var project = byId(state.projects, state.projectId);
        if (!project) {
            els.boardTitle.textContent = state.projects.length ? 'Select a project' : 'Projects';
            els.boardCode.hidden = true;
            els.boardCode.textContent = '';
            els.boardLead.textContent = state.projects.length
                ? ''
                : 'Create a project to start tracking issues.';
            els.boardActions.hidden = true;
            els.formProjectCode.hidden = true;
            els.formIssue.hidden = true;
            els.columns.hidden = true;
            els.empty.hidden = false;
            els.empty.textContent = state.projects.length
                ? 'Choose a project on the left.'
                : 'Create a project to start tracking issues.';
            return;
        }
        var data = dataOf(project);
        var code = projectCodeOf(project);
        var hasCode = validCode(code);
        els.boardTitle.textContent = text(data.name) || '(untitled)';
        els.boardCode.hidden = !hasCode;
        els.boardCode.textContent = hasCode ? code : '';
        els.boardLead.textContent = text(data.description);
        els.boardActions.hidden = false;
        els.formProjectCode.hidden = hasCode;
        els.formIssue.hidden = !hasCode;
        els.columns.hidden = false;
        els.empty.hidden = true;
        if (hasCode) {
            var next = Math.max(Number(data.issueSeq) || 0, maxTaskNumber(project.id)) + 1;
            els.issueKeyHint.textContent = 'Task id is assigned on create. Next: ' + code + '-' + next;
        }
        document.getElementById('btn-archive').textContent = data.archived ? 'Restore' : 'Archive';
        STATUSES.forEach(function (status) {
            var col = els.columns.querySelector('[data-status="' + status + '"] .board-col-cards');
            col.textContent = '';
            issuesFor(project.id).filter(function (row) {
                return statusOf(row) === status;
            }).forEach(function (row) {
                col.appendChild(issueCard(row, code));
            });
        });
    }

    function issueCard(row, projectCode) {
        var data = dataOf(row);
        var card = document.createElement('article');
        card.className = 'issue-card' + (data.priority === 'high' ? ' is-high' : '');
        var open = document.createElement('button');
        open.type = 'button';
        open.className = 'issue-card-open';
        open.addEventListener('click', function () {
            writeRoute(state.projectId, row.id);
        });
        var key = taskKeyOf(row, projectCode);
        if (key) {
            var keyLine = document.createElement('span');
            keyLine.className = 'issue-card-key';
            keyLine.textContent = key;
            open.appendChild(keyLine);
        }
        var title = document.createElement('span');
        title.className = 'issue-card-title';
        title.textContent = text(data.title) || '(untitled)';
        var meta = document.createElement('span');
        meta.className = 'issue-card-meta';
        var bits = [];
        if (data.priority && data.priority !== 'normal') {
            bits.push(data.priority);
        }
        if (data.author) {
            bits.push(data.author);
        }
        if (row.createdAt) {
            bits.push(String(row.createdAt).slice(0, 10));
        }
        var n = commentsFor(row.id).length;
        if (n) {
            bits.push(n === 1 ? '1 comment' : n + ' comments');
        }
        meta.textContent = bits.join(' · ');
        open.appendChild(title);
        open.appendChild(meta);
        var move = document.createElement('div');
        move.className = 'issue-card-move';
        var next = MOVE[statusOf(row)];
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-secondary';
        btn.textContent = next === 'doing' ? 'Doing' : next === 'done' ? 'Done' : 'Open';
        btn.addEventListener('click', function (event) {
            event.stopPropagation();
            patchIssue(row.id, { status: next });
        });
        move.appendChild(btn);
        card.appendChild(open);
        card.appendChild(move);
        return card;
    }

    function renderDrawer() {
        var issue = byId(state.issues, state.issueId);
        if (!issue) {
            els.drawer.hidden = true;
            els.backdrop.hidden = true;
            return;
        }
        var data = dataOf(issue);
        var project = byId(state.projects, data.projectId || state.projectId);
        els.drawer.hidden = false;
        els.backdrop.hidden = false;
        state.syncingDrawer = true;
        var key = taskKeyOf(issue, project && projectCodeOf(project));
        els.editTaskKey.hidden = !key;
        els.editTaskKey.textContent = key;
        els.editTitle.value = text(data.title);
        els.editStatus.value = statusOf(issue);
        els.editPriority.value = data.priority === 'high' || data.priority === 'low' ? data.priority : 'normal';
        els.editBody.value = text(data.body);
        state.syncingDrawer = false;
        var meta = [];
        if (data.author) {
            meta.push(data.author);
        }
        if (issue.updatedAt) {
            meta.push(String(issue.updatedAt).slice(0, 19).replace('T', ' ') + ' UTC');
        }
        els.editMeta.textContent = meta.join(' · ');
        renderComments();
    }

    function renderComments() {
        els.commentList.textContent = '';
        commentsFor(state.issueId).forEach(function (row) {
            var data = dataOf(row);
            var item = document.createElement('article');
            item.className = 'comment-item';
            var meta = document.createElement('div');
            meta.className = 'comment-item-meta';
            meta.textContent = [data.author || 'member', row.createdAt ? String(row.createdAt).slice(0, 16).replace('T', ' ') : '']
                .filter(Boolean).join(' · ');
            var body = document.createElement('p');
            body.className = 'comment-item-body';
            body.textContent = text(data.body);
            item.appendChild(meta);
            item.appendChild(body);
            if (row.ownerId) {
                var del = document.createElement('button');
                del.type = 'button';
                del.className = 'btn-delete';
                del.textContent = 'Delete';
                del.addEventListener('click', function () {
                    if (window.confirm('Delete this comment?')) {
                        deleteComment(row.id);
                    }
                });
                item.appendChild(del);
            }
            els.commentList.appendChild(item);
        });
    }

    function closeDrawer() {
        if (state.issueId) {
            writeRoute(state.projectId, null);
        } else {
            els.drawer.hidden = true;
            els.backdrop.hidden = true;
        }
    }

    function paint() {
        renderWho();
        if (!signedIn()) {
            showPanel('guest');
            return;
        }
        if (!state.schema) {
            showPanel('store');
            return;
        }
        showPanel('app');
        renderProjects();
        renderBoard();
        renderDrawer();
    }

    function applyRoute() {
        var route = parseRoute();
        state.projectId = route.projectId;
        state.issueId = route.issueId;
        if (!state.projectId) {
            var first = visibleProjects()[0];
            if (first) {
                writeRoute(first.id, null);
                return;
            }
        }
        paint();
        if (state.issueId) {
            issuesStore.get(state.issueId).then(function (row) {
                replaceRow(state.issues, row);
                renderBoard();
                renderDrawer();
            }).catch(function (err) {
                if (err && err.status === 404) {
                    writeRoute(state.projectId, null);
                    return;
                }
                fail(err);
            });
        }
    }

    async function loadRows() {
        var issueOpts = state.mine ? { mine: true } : {};
        var loaded = await Promise.all([
            listAll(projectsStore),
            listAll(issuesStore, issueOpts),
            listAll(commentsStore)
        ]);
        state.projects = loaded[0];
        state.issues = loaded[1];
        state.comments = loaded[2];
    }

    async function ensureTables() {
        var have = tableNames(state.schema);
        var missing = TABLES.filter(function (table) {
            return !have[table.name];
        });
        if (!missing.length) {
            return;
        }
        if (!state.schema.storeOpen) {
            banner('Tables are missing from the allowlist. Re-upload the Projects pack.');
            return;
        }
        for (var i = 0; i < missing.length; i++) {
            var table = missing[i];
            await CseAppData.connect({ table: table.name }).ensureTable(table.name, {
                access: table.access,
                schema: table.schema
            });
        }
        state.schema = await projectsStore.schema();
    }

    async function boot() {
        showPanel('load');
        try {
            var siteRes = await fetch(base() + '/rest/site', {
                credentials: 'same-origin',
                headers: { Accept: 'application/json' }
            });
            state.site = await siteRes.json();
            renderWho();
            if (!signedIn()) {
                showPanel('guest');
                return;
            }
            try {
                state.schema = await projectsStore.schema();
            } catch (err) {
                if (err && err.status === 403) {
                    showPanel('store');
                    return;
                }
                throw err;
            }
            await ensureTables();
            await loadRows();
            applyRoute();
        } catch (err) {
            showPanel('guest');
            fail(err);
        }
    }

    async function patchIssue(id, patch) {
        await withBusy(async function () {
            try {
                var row = await issuesStore.patch(id, patch);
                replaceRow(state.issues, row);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    }

    async function deleteComment(id) {
        await withBusy(async function () {
            try {
                await commentsStore.remove(id);
                removeRow(state.comments, id);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    }

    document.getElementById('btn-new-project').addEventListener('click', function () {
        els.formProject.hidden = !els.formProject.hidden;
        if (!els.formProject.hidden) {
            els.projectName.focus();
        }
    });

    els.formProject.addEventListener('submit', function (event) {
        event.preventDefault();
        withBusy(async function () {
            try {
                var code = normalizeCode(els.projectCode.value);
                if (!validCode(code)) {
                    banner('Project code must be 2–8 characters: a letter, then letters or digits (e.g. FLAB).');
                    return;
                }
                if (codeTaken(code)) {
                    banner('Project code ' + code + ' is already in use.');
                    return;
                }
                var row = await projectsStore.insert({
                    name: els.projectName.value.trim(),
                    code: code,
                    issueSeq: 0,
                    description: els.projectDescription.value.trim(),
                    archived: false,
                    author: author()
                });
                els.formProject.reset();
                els.formProject.hidden = true;
                state.projects.push(row);
                writeRoute(row.id, null);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    els.formProjectCode.addEventListener('submit', function (event) {
        event.preventDefault();
        var project = byId(state.projects, state.projectId);
        if (!project) {
            return;
        }
        withBusy(async function () {
            try {
                if (validCode(projectCodeOf(project))) {
                    banner('Project code cannot be changed.');
                    return;
                }
                var code = normalizeCode(els.existingProjectCode.value);
                if (!validCode(code)) {
                    banner('Project code must be 2–8 characters: a letter, then letters or digits (e.g. FLAB).');
                    return;
                }
                if (codeTaken(code, project.id)) {
                    banner('Project code ' + code + ' is already in use.');
                    return;
                }
                var seq = Number(dataOf(project).issueSeq);
                if (!Number.isFinite(seq) || seq < 0) {
                    seq = 0;
                }
                var numbered = await assignMissingKeys(project, code);
                var row = await projectsStore.patch(project.id, {
                    code: code,
                    issueSeq: Math.max(seq, numbered)
                });
                els.formProjectCode.reset();
                replaceRow(state.projects, row);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    els.formIssue.addEventListener('submit', function (event) {
        event.preventDefault();
        if (!state.projectId) {
            return;
        }
        withBusy(async function () {
            try {
                var project = byId(state.projects, state.projectId);
                if (!project || !validCode(projectCodeOf(project))) {
                    banner('Set a project code before adding issues.');
                    return;
                }
                var task = await allocateTask(project);
                var row = await issuesStore.insert({
                    projectId: state.projectId,
                    title: els.issueTitle.value.trim(),
                    body: '',
                    status: 'open',
                    priority: els.issuePriority.value,
                    author: author(),
                    taskNumber: task.number,
                    taskKey: task.key
                });
                els.formIssue.reset();
                els.issuePriority.value = 'normal';
                state.issues.push(row);
                writeRoute(state.projectId, row.id);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    document.getElementById('form-issue-edit').addEventListener('submit', function (event) {
        event.preventDefault();
        if (!state.issueId) {
            return;
        }
        patchIssue(state.issueId, {
            title: els.editTitle.value.trim(),
            status: els.editStatus.value,
            priority: els.editPriority.value,
            body: els.editBody.value
        });
    });

    els.editStatus.addEventListener('change', function () {
        if (state.syncingDrawer || !state.issueId) {
            return;
        }
        patchIssue(state.issueId, { status: els.editStatus.value });
    });

    document.getElementById('form-comment').addEventListener('submit', function (event) {
        event.preventDefault();
        if (!state.issueId) {
            return;
        }
        withBusy(async function () {
            try {
                var row = await commentsStore.insert({
                    issueId: state.issueId,
                    body: els.commentBody.value.trim(),
                    author: author()
                });
                els.commentBody.value = '';
                state.comments.push(row);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    document.getElementById('btn-archive').addEventListener('click', function () {
        var project = byId(state.projects, state.projectId);
        if (!project) {
            return;
        }
        withBusy(async function () {
            try {
                var row = await projectsStore.patch(project.id, { archived: !dataOf(project).archived });
                replaceRow(state.projects, row);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    document.getElementById('btn-delete-project').addEventListener('click', function () {
        var project = byId(state.projects, state.projectId);
        if (!project) {
            return;
        }
        if (!window.confirm('Delete this project and all of its issues?')) {
            return;
        }
        withBusy(async function () {
            try {
                var issueIds = issuesFor(project.id).map(function (row) {
                    return row.id;
                });
                for (var i = 0; i < state.comments.length; i++) {
                    var comment = state.comments[i];
                    if (issueIds.indexOf(dataOf(comment).issueId) >= 0) {
                        await commentsStore.remove(comment.id);
                    }
                }
                for (var j = 0; j < issueIds.length; j++) {
                    await issuesStore.remove(issueIds[j]);
                }
                await projectsStore.remove(project.id);
                state.comments = state.comments.filter(function (row) {
                    return issueIds.indexOf(dataOf(row).issueId) < 0;
                });
                state.issues = state.issues.filter(function (row) {
                    return dataOf(row).projectId !== project.id;
                });
                removeRow(state.projects, project.id);
                writeRoute(null, null);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    document.getElementById('btn-delete-issue').addEventListener('click', function () {
        if (!state.issueId) {
            return;
        }
        if (!window.confirm('Delete this issue and its comments?')) {
            return;
        }
        var issueId = state.issueId;
        withBusy(async function () {
            try {
                var comments = commentsFor(issueId);
                for (var i = 0; i < comments.length; i++) {
                    await commentsStore.remove(comments[i].id);
                    removeRow(state.comments, comments[i].id);
                }
                await issuesStore.remove(issueId);
                removeRow(state.issues, issueId);
                writeRoute(state.projectId, null);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    els.showArchived.addEventListener('change', function () {
        state.showArchived = els.showArchived.checked;
        paint();
    });

    els.mineOnly.addEventListener('change', function () {
        state.mine = els.mineOnly.checked;
        withBusy(async function () {
            try {
                state.issues = await listAll(issuesStore, state.mine ? { mine: true } : {});
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    document.getElementById('btn-close-drawer').addEventListener('click', closeDrawer);
    els.backdrop.addEventListener('click', closeDrawer);
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && !els.drawer.hidden) {
            closeDrawer();
        }
    });

    bindUppercase(els.projectCode);
    bindUppercase(els.existingProjectCode);
    window.addEventListener('hashchange', applyRoute);
    boot();
})();
