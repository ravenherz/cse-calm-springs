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
                    category: { type: 'string' },
                    lane: { type: 'string' },
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
        },
        {
            name: 'pictures',
            access: 'member-write',
            schema: {
                type: 'object',
                properties: {
                    issueId: { type: 'string' },
                    name: { type: 'string', maxLength: 160 },
                    mime: { type: 'string', maxLength: 64 },
                    data: { type: 'string', maxLength: 240000 },
                    author: { type: 'string' }
                }
            }
        }
    ];
    var STATUSES = ['open', 'doing', 'done'];
    var MOVE = { open: 'doing', doing: 'done', done: 'open' };
    var TABS = ['work', 'backlog', 'archive', 'new-feature', 'new-project'];
    var KINDS = ['bug', 'feature', 'task'];
    var KIND_LABEL = { bug: 'Bug', feature: 'Feature', task: 'Task' };
    var KIND_SVG = {
        bug: '<svg viewBox="0 0 16 16" aria-hidden="true"><circle cx="8" cy="8" r="6.25" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M8 4.2v5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><circle cx="8" cy="11.6" r="1.05" fill="currentColor"/></svg>',
        feature: '<svg viewBox="0 0 16 16" aria-hidden="true"><circle cx="8" cy="8" r="6.25" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M6.15 6.15a1.85 1.85 0 1 1 2.55 1.7c-.55.36-.9.86-.9 1.55" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><circle cx="8" cy="12" r="1.05" fill="currentColor"/></svg>',
        task: '<svg viewBox="0 0 16 16" aria-hidden="true"><circle cx="3.4" cy="8" r="1.45" fill="currentColor"/><circle cx="8" cy="8" r="1.45" fill="currentColor"/><circle cx="12.6" cy="8" r="1.45" fill="currentColor"/></svg>'
    };
    var PICTURE_LIMIT = 8;
    var PICTURE_MAX_CHARS = 230000;
    var PICTURE_FALLBACK_CHARS = 120000;

    var projectsStore = CseAppData.connect({ table: 'projects' });
    var issuesStore = CseAppData.connect({ table: 'issues' });
    var commentsStore = CseAppData.connect({ table: 'comments' });
    var picturesStore = CseAppData.connect({ table: 'pictures' });

    var state = {
        site: null,
        schema: null,
        projects: [],
        issues: [],
        comments: [],
        pictures: [],
        pendingPictures: [],
        picked: {},
        pickedReady: false,
        mine: false,
        showArchived: false,
        tab: 'work',
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
        featureProject: document.getElementById('feature-project'),
        featureKindPicker: document.getElementById('feature-kind-picker'),
        featureBody: document.getElementById('feature-body'),
        featurePictures: document.getElementById('feature-pictures'),
        featurePicturePreviews: document.getElementById('feature-picture-previews'),
        issueTitle: document.getElementById('issue-title'),
        issuePriority: document.getElementById('issue-priority'),
        issueKeyHint: document.getElementById('issue-key-hint'),
        columns: document.getElementById('board-columns'),
        empty: document.getElementById('board-empty'),
        backlogList: document.getElementById('backlog-list'),
        backlogEmpty: document.getElementById('backlog-empty'),
        archiveList: document.getElementById('archive-list'),
        archiveEmpty: document.getElementById('archive-empty'),
        archiveLead: document.getElementById('archive-lead'),
        backdrop: document.getElementById('drawer-backdrop'),
        drawer: document.getElementById('drawer'),
        editTaskKey: document.getElementById('edit-task-key'),
        editTitle: document.getElementById('edit-title'),
        editKindPicker: document.getElementById('edit-kind-picker'),
        editStatus: document.getElementById('edit-status'),
        editPriority: document.getElementById('edit-priority'),
        editBody: document.getElementById('edit-body'),
        editPictures: document.getElementById('edit-pictures'),
        editPictureList: document.getElementById('edit-picture-list'),
        editMeta: document.getElementById('edit-meta'),
        issueLaneBtn: document.getElementById('btn-issue-lane'),
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

    function kindOf(row) {
        var kind = text(dataOf(row).category).toLowerCase();
        return KINDS.indexOf(kind) >= 0 ? kind : 'task';
    }

    function laneOf(row) {
        var lane = text(dataOf(row).lane).toLowerCase();
        if (lane === 'backlog' || lane === 'active' || lane === 'archived') {
            return lane;
        }
        return 'active';
    }

    function kindGlyph(kind) {
        var resolved = KINDS.indexOf(kind) >= 0 ? kind : 'task';
        var span = document.createElement('span');
        span.className = 'issue-kind issue-kind-' + resolved;
        span.setAttribute('title', KIND_LABEL[resolved]);
        span.setAttribute('aria-label', KIND_LABEL[resolved]);
        span.innerHTML = KIND_SVG[resolved];
        return span;
    }

    function selectedKind(name) {
        var el = document.querySelector('input[name="' + name + '"]:checked');
        var value = el ? el.value : '';
        return KINDS.indexOf(value) >= 0 ? value : 'feature';
    }

    function fillKindPicker(container, name, selected) {
        if (!container) {
            return;
        }
        container.textContent = '';
        KINDS.forEach(function (kind) {
            var label = document.createElement('label');
            label.className = 'kind-option' + (kind === selected ? ' is-selected' : '');
            var input = document.createElement('input');
            input.type = 'radio';
            input.name = name;
            input.value = kind;
            input.checked = kind === selected;
            label.appendChild(input);
            var face = document.createElement('span');
            face.className = 'kind-option-face';
            face.appendChild(kindGlyph(kind));
            var caption = document.createElement('span');
            caption.className = 'kind-option-label';
            caption.textContent = KIND_LABEL[kind];
            face.appendChild(caption);
            label.appendChild(face);
            container.appendChild(label);
        });
    }

    function markKindPicker(container) {
        if (!container) {
            return;
        }
        Array.prototype.forEach.call(container.querySelectorAll('.kind-option'), function (option) {
            var input = option.querySelector('input');
            option.classList.toggle('is-selected', !!(input && input.checked));
        });
    }

    function picturesFor(issueId) {
        return state.pictures.filter(function (row) {
            return dataOf(row).issueId === issueId;
        });
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
        var loading = name === 'load';
        if (els.load) {
            els.load.hidden = !loading;
            els.load.setAttribute('aria-busy', loading ? 'true' : 'false');
        }
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
        var tab = 'work';
        var i = 0;
        if (parts[0] === 'p' && parts[1]) {
            projectId = decodeURIComponent(parts[1]);
            i = 2;
        }
        if (parts[i] && TABS.indexOf(parts[i]) >= 0) {
            tab = parts[i];
            i += 1;
        }
        if (parts[i] === 'i' && parts[i + 1]) {
            issueId = decodeURIComponent(parts[i + 1]);
        }
        return { projectId: projectId, tab: tab, issueId: issueId };
    }

    function writeRoute(projectId, tab, issueId) {
        var nextTab = TABS.indexOf(tab) >= 0 ? tab : (state.tab || 'work');
        var next = '#' + nextTab;
        if (projectId) {
            next = '#p/' + encodeURIComponent(projectId) + '/' + nextTab;
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

    function ensurePicks() {
        if (state.pickedReady) {
            return;
        }
        visibleProjects().forEach(function (row) {
            state.picked[row.id] = true;
        });
        state.pickedReady = true;
    }

    function pickedProjectIds() {
        return state.projects.filter(function (row) {
            return !!state.picked[row.id];
        }).map(function (row) {
            return row.id;
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

    function showTab(tab) {
        TABS.forEach(function (name) {
            var panel = document.getElementById('tab-' + name);
            var btn = document.getElementById('tab-btn-' + name);
            var on = name === tab;
            if (panel) {
                panel.hidden = !on;
                panel.classList.toggle('is-active', on);
            }
            if (btn) {
                btn.classList.toggle('is-active', on);
                btn.setAttribute('aria-selected', on ? 'true' : 'false');
            }
        });
    }

    function renderProjects() {
        ensurePicks();
        els.projectList.textContent = '';
        visibleProjects().forEach(function (row) {
            var data = dataOf(row);
            var wrap = document.createElement('div');
            wrap.className = 'project-item-row';
            var pick = document.createElement('label');
            pick.className = 'project-item-pick';
            pick.title = 'Include in Backlog and Archive';
            var check = document.createElement('input');
            check.type = 'checkbox';
            check.checked = !!state.picked[row.id];
            check.addEventListener('click', function (event) {
                event.stopPropagation();
            });
            check.addEventListener('change', function () {
                if (check.checked) {
                    state.picked[row.id] = true;
                } else {
                    delete state.picked[row.id];
                }
                renderLists();
            });
            pick.appendChild(check);
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'project-item' + (row.id === state.projectId ? ' is-active' : '');
            button.addEventListener('click', function () {
                state.picked[row.id] = true;
                writeRoute(row.id, state.tab === 'new-project' ? 'work' : state.tab, null);
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
            wrap.appendChild(pick);
            wrap.appendChild(button);
            els.projectList.appendChild(wrap);
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
        els.columns.hidden = false;
        els.empty.hidden = true;
        document.getElementById('btn-archive').textContent = data.archived ? 'Restore project' : 'Archive project';
        STATUSES.forEach(function (status) {
            var col = els.columns.querySelector('[data-status="' + status + '"] .board-col-cards');
            col.textContent = '';
            issuesFor(project.id).filter(function (row) {
                return laneOf(row) === 'active' && statusOf(row) === status;
            }).forEach(function (row) {
                col.appendChild(issueCard(row, code, 'work'));
            });
        });
    }

    function listIssues(lane) {
        var allowed = pickedProjectIds();
        return state.issues.filter(function (row) {
            var projectId = dataOf(row).projectId;
            if (allowed.indexOf(projectId) < 0) {
                return false;
            }
            var project = byId(state.projects, projectId);
            if (!project) {
                return false;
            }
            if (!state.showArchived && dataOf(project).archived && project.id !== state.projectId) {
                return false;
            }
            if (lane === 'backlog') {
                return laneOf(row) !== 'archived';
            }
            return laneOf(row) === lane;
        }).sort(function (a, b) {
            return text(b.createdAt).localeCompare(text(a.createdAt));
        });
    }

    function renderLists() {
        var backlog = listIssues('backlog');
        els.backlogList.textContent = '';
        backlog.forEach(function (row) {
            var project = byId(state.projects, dataOf(row).projectId);
            els.backlogList.appendChild(issueCard(row, project && projectCodeOf(project), 'backlog'));
        });
        els.backlogEmpty.hidden = backlog.length > 0;
        if (!pickedProjectIds().length) {
            els.backlogEmpty.textContent = 'Check one or more projects on the left.';
        } else {
            els.backlogEmpty.textContent = 'Nothing in the backlog. Create a feature to add one.';
        }

        var archived = listIssues('archived');
        els.archiveList.textContent = '';
        archived.forEach(function (row) {
            var project = byId(state.projects, dataOf(row).projectId);
            els.archiveList.appendChild(issueCard(row, project && projectCodeOf(project), 'archive'));
        });
        els.archiveEmpty.hidden = archived.length > 0;
        var names = state.projects.filter(function (row) {
            return !!state.picked[row.id];
        }).map(function (row) {
            return text(dataOf(row).name) || projectCodeOf(row) || '(untitled)';
        });
        if (!names.length) {
            els.archiveLead.textContent = 'Check projects on the left to see their archived features.';
            els.archiveEmpty.textContent = 'Check one or more projects on the left.';
        } else {
            els.archiveLead.textContent = 'Archived features for ' + names.join(', ') + '.';
            els.archiveEmpty.textContent = 'No archived features for the checked projects.';
        }
    }

    function renderCreateFeature() {
        var select = els.featureProject;
        var current = select.value || state.projectId;
        select.textContent = '';
        var rows = state.projects.filter(function (row) {
            return !dataOf(row).archived || row.id === current;
        });
        if (!rows.length) {
            var empty = document.createElement('option');
            empty.value = '';
            empty.textContent = 'Create a project first';
            select.appendChild(empty);
            els.issueKeyHint.textContent = 'Create a project before adding features.';
            return;
        }
        rows.forEach(function (row) {
            var opt = document.createElement('option');
            opt.value = row.id;
            var code = projectCodeOf(row);
            opt.textContent = (text(dataOf(row).name) || '(untitled)') + (code ? ' (' + code + ')' : '');
            select.appendChild(opt);
        });
        if (current && byId(state.projects, current)) {
            select.value = current;
        }
        updateFeatureHint();
        renderPendingPictures();
    }

    function updateFeatureHint() {
        var project = byId(state.projects, els.featureProject.value);
        if (!project) {
            return;
        }
        var code = projectCodeOf(project);
        if (!validCode(code)) {
            els.issueKeyHint.textContent = 'This project needs a code before you can create features.';
            return;
        }
        var next = Math.max(Number(dataOf(project).issueSeq) || 0, maxTaskNumber(project.id)) + 1;
        els.issueKeyHint.textContent = 'Task id is assigned on create. Next: ' + code + '-' + next;
    }

    function issueCard(row, projectCode, mode) {
        var data = dataOf(row);
        var card = document.createElement('article');
        card.className = 'issue-card' + (data.priority === 'high' ? ' is-high' : '');
        var open = document.createElement('button');
        open.type = 'button';
        open.className = 'issue-card-open';
        open.addEventListener('click', function () {
            writeRoute(data.projectId || state.projectId, state.tab, row.id);
        });
        var top = document.createElement('div');
        top.className = 'issue-card-top';
        top.appendChild(kindGlyph(kindOf(row)));
        var key = taskKeyOf(row, projectCode);
        if (key) {
            var keyLine = document.createElement('span');
            keyLine.className = 'issue-card-key';
            keyLine.textContent = key;
            top.appendChild(keyLine);
        }
        open.appendChild(top);
        var title = document.createElement('span');
        title.className = 'issue-card-title';
        title.textContent = text(data.title) || '(untitled)';
        var meta = document.createElement('span');
        meta.className = 'issue-card-meta';
        var bits = [];
        bits.push(KIND_LABEL[kindOf(row)]);
        if (mode !== 'work') {
            var project = byId(state.projects, data.projectId);
            if (project) {
                bits.push(projectCodeOf(project) || text(dataOf(project).name) || '(project)');
            }
        }
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
        var pics = picturesFor(row.id);
        if (pics.length) {
            bits.push(pics.length === 1 ? '1 picture' : pics.length + ' pictures');
        }
        meta.textContent = bits.join(' · ');
        open.appendChild(title);
        open.appendChild(meta);
        if (pics.length) {
            var thumbs = document.createElement('div');
            thumbs.className = 'issue-card-thumbs';
            pics.slice(0, 3).forEach(function (pic) {
                var img = document.createElement('img');
                img.alt = text(dataOf(pic).name) || 'Picture';
                img.src = text(dataOf(pic).data);
                thumbs.appendChild(img);
            });
            open.appendChild(thumbs);
        }
        var move = document.createElement('div');
        move.className = 'issue-card-move';
        if (mode === 'backlog') {
            if (laneOf(row) === 'active') {
                var mark = document.createElement('span');
                mark.className = 'issue-card-meta';
                mark.textContent = 'In active work';
                move.appendChild(mark);
                if (statusOf(row) === 'done') {
                    move.appendChild(actionButton('Archive', function () {
                        patchIssue(row.id, { lane: 'archived' });
                    }));
                }
            } else {
                move.appendChild(actionButton('Active work', function () {
                    patchIssue(row.id, {
                        lane: 'active',
                        status: statusOf(row) === 'done' ? 'open' : statusOf(row)
                    });
                }));
            }
        } else if (mode === 'archive') {
            move.appendChild(actionButton('Restore', function () {
                patchIssue(row.id, { lane: 'backlog' });
            }));
        } else {
            var next = MOVE[statusOf(row)];
            move.appendChild(actionButton(
                next === 'doing' ? 'Doing' : next === 'done' ? 'Done' : 'Open',
                function () {
                    patchIssue(row.id, { status: next });
                }
            ));
            if (statusOf(row) === 'done') {
                move.appendChild(actionButton('Archive', function () {
                    patchIssue(row.id, { lane: 'archived' });
                }));
            }
        }
        card.appendChild(open);
        card.appendChild(move);
        return card;
    }

    function actionButton(label, onClick) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-secondary';
        btn.textContent = label;
        btn.addEventListener('click', function (event) {
            event.stopPropagation();
            onClick();
        });
        return btn;
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
        fillKindPicker(els.editKindPicker, 'edit-kind', kindOf(issue));
        els.editStatus.value = statusOf(issue);
        els.editPriority.value = data.priority === 'high' || data.priority === 'low' ? data.priority : 'normal';
        els.editBody.value = text(data.body);
        state.syncingDrawer = false;
        var meta = [];
        meta.push(KIND_LABEL[kindOf(issue)]);
        if (data.author) {
            meta.push(data.author);
        }
        if (issue.updatedAt) {
            meta.push(String(issue.updatedAt).slice(0, 19).replace('T', ' ') + ' UTC');
        }
        els.editMeta.textContent = meta.join(' · ');
        renderLaneButton(issue);
        renderEditPictures();
        renderComments();
    }

    function renderLaneButton(issue) {
        var lane = laneOf(issue);
        var status = statusOf(issue);
        if (lane === 'backlog') {
            els.issueLaneBtn.hidden = false;
            els.issueLaneBtn.textContent = 'Active work';
        } else if (lane === 'archived') {
            els.issueLaneBtn.hidden = false;
            els.issueLaneBtn.textContent = 'Restore';
        } else if (status === 'done') {
            els.issueLaneBtn.hidden = false;
            els.issueLaneBtn.textContent = 'Archive';
        } else {
            els.issueLaneBtn.hidden = true;
            els.issueLaneBtn.textContent = '';
        }
    }

    function dataUrlToBlob(dataUrl) {
        var parts = String(dataUrl || '').split(',');
        var header = parts[0] || '';
        var mime = (header.match(/data:([^;]+)/) || [])[1] || 'image/jpeg';
        var binary = atob(parts[1] || '');
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return new Blob([bytes], { type: mime });
    }

    function openPicture(src) {
        if (!src) {
            return;
        }
        var url = src;
        if (src.indexOf('data:') === 0) {
            try {
                url = URL.createObjectURL(dataUrlToBlob(src));
                setTimeout(function () {
                    URL.revokeObjectURL(url);
                }, 60000);
            } catch (err) {
                url = src;
            }
        }
        window.open(url, '_blank', 'noopener');
    }

    function pictureTile(src, name, onRemove) {
        var tile = document.createElement('div');
        tile.className = 'picture-tile';
        var img = document.createElement('img');
        img.alt = name || 'Picture';
        img.src = src;
        tile.appendChild(img);
        var view = document.createElement('button');
        view.type = 'button';
        view.className = 'btn btn-secondary picture-tile-view';
        view.textContent = 'View';
        view.title = 'Open in a new tab';
        view.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            openPicture(src);
        });
        tile.appendChild(view);
        if (onRemove) {
            var remove = document.createElement('button');
            remove.type = 'button';
            remove.className = 'btn btn-delete picture-tile-remove';
            remove.textContent = '×';
            remove.title = 'Remove picture';
            remove.addEventListener('click', function (event) {
                event.preventDefault();
                event.stopPropagation();
                onRemove();
            });
            tile.appendChild(remove);
        }
        return tile;
    }

    function renderPendingPictures() {
        els.featurePicturePreviews.textContent = '';
        state.pendingPictures.forEach(function (pic, index) {
            els.featurePicturePreviews.appendChild(pictureTile(pic.data, pic.name, function () {
                state.pendingPictures.splice(index, 1);
                renderPendingPictures();
            }));
        });
    }

    function renderEditPictures() {
        els.editPictureList.textContent = '';
        picturesFor(state.issueId).forEach(function (row) {
            var data = dataOf(row);
            els.editPictureList.appendChild(pictureTile(text(data.data), text(data.name), function () {
                if (window.confirm('Remove this picture?')) {
                    deletePicture(row.id);
                }
            }));
        });
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
            writeRoute(state.projectId, state.tab, null);
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
        showTab(state.tab);
        renderProjects();
        renderBoard();
        renderLists();
        renderCreateFeature();
        renderDrawer();
    }

    function applyRoute() {
        var route = parseRoute();
        state.tab = route.tab;
        state.projectId = route.projectId;
        state.issueId = route.issueId;
        if (!state.projectId && state.tab !== 'new-project' && state.tab !== 'new-feature' && state.tab !== 'backlog') {
            var first = visibleProjects()[0];
            if (first) {
                writeRoute(first.id, state.tab, null);
                return;
            }
        }
        paint();
        if (state.issueId) {
            issuesStore.get(state.issueId).then(function (row) {
                replaceRow(state.issues, row);
                renderBoard();
                renderLists();
                renderDrawer();
            }).catch(function (err) {
                if (err && err.status === 404) {
                    writeRoute(state.projectId, state.tab, null);
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
            listAll(commentsStore),
            listAll(picturesStore).catch(function (err) {
                if (err && err.status === 404) {
                    return [];
                }
                throw err;
            })
        ]);
        state.projects = loaded[0];
        state.issues = loaded[1];
        state.comments = loaded[2];
        state.pictures = loaded[3];
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
        fillKindPicker(els.featureKindPicker, 'feature-kind', 'feature');
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

    async function deletePicture(id) {
        await withBusy(async function () {
            try {
                await picturesStore.remove(id);
                removeRow(state.pictures, id);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    }

    async function removePicturesForIssues(issueIds) {
        for (var i = 0; i < state.pictures.length; i++) {
            var picture = state.pictures[i];
            if (issueIds.indexOf(dataOf(picture).issueId) >= 0) {
                await picturesStore.remove(picture.id);
            }
        }
        state.pictures = state.pictures.filter(function (row) {
            return issueIds.indexOf(dataOf(row).issueId) < 0;
        });
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

    function canvasJpeg(img, quality, maxEdge) {
        var w = img.naturalWidth || img.width;
        var h = img.naturalHeight || img.height;
        if (!w || !h) {
            return '';
        }
        if (w > maxEdge || h > maxEdge) {
            var scale = Math.min(maxEdge / w, maxEdge / h);
            w = Math.max(1, Math.round(w * scale));
            h = Math.max(1, Math.round(h * scale));
        }
        var canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        var ctx = canvas.getContext('2d');
        ctx.fillStyle = '#0b0c0f';
        ctx.fillRect(0, 0, w, h);
        ctx.drawImage(img, 0, 0, w, h);
        return canvas.toDataURL('image/jpeg', quality);
    }

    function looksLikeImage(file) {
        if (!file) {
            return false;
        }
        if (file.type && file.type.indexOf('image/') === 0) {
            return true;
        }
        return /\.(png|jpe?g|gif|webp|bmp)$/i.test(file.name || '');
    }

    function snapshotFiles(input) {
        var list = Array.prototype.slice.call((input && input.files) || []);
        if (input) {
            input.value = '';
        }
        return list;
    }

    async function shrinkToJpeg(raw, maxChars) {
        var img = await loadImage(raw);
        var edges = maxChars > 100000 ? [2048, 1600, 1280, 1024] : [1280, 1024, 800, 640];
        var qualities = maxChars > 100000 ? [0.92, 0.88, 0.82] : [0.82, 0.74, 0.64];
        for (var i = 0; i < edges.length; i++) {
            for (var q = 0; q < qualities.length; q++) {
                var next = canvasJpeg(img, qualities[q], edges[i]);
                if (next && next.length <= maxChars) {
                    return next;
                }
            }
        }
        throw new Error('Picture is too large after compression');
    }

    async function encodePicture(file, maxChars) {
        var cap = maxChars || PICTURE_MAX_CHARS;
        if (!looksLikeImage(file)) {
            throw new Error('Choose an image file');
        }
        var raw = await readFileAsDataUrl(file);
        if (raw.length <= cap && /^image\/(jpeg|png|gif|webp)$/.test(file.type || '')) {
            return { name: file.name || 'image', mime: file.type, data: raw };
        }
        return {
            name: (file.name || 'image').replace(/\.[^.]+$/, '') + '.jpg',
            mime: 'image/jpeg',
            data: await shrinkToJpeg(raw, cap)
        };
    }

    async function addPicturesFromFiles(files, already) {
        var room = PICTURE_LIMIT - already;
        if (room <= 0) {
            throw new Error('A feature can hold ' + PICTURE_LIMIT + ' pictures.');
        }
        var list = (files || []).slice(0, room);
        if (!list.length) {
            throw new Error('Choose an image file');
        }
        var added = [];
        for (var i = 0; i < list.length; i++) {
            added.push(await encodePicture(list[i]));
        }
        return added;
    }

    function pictureTableSpec() {
        for (var i = 0; i < TABLES.length; i++) {
            if (TABLES[i].name === 'pictures') {
                return TABLES[i];
            }
        }
        return { name: 'pictures', access: 'member-write', schema: { type: 'object' } };
    }

    async function insertPicture(payload) {
        try {
            return await picturesStore.insert(payload);
        } catch (err) {
            if (err && err.status === 404) {
                var spec = pictureTableSpec();
                await picturesStore.ensureTable('pictures', { access: spec.access, schema: spec.schema });
                return picturesStore.insert(payload);
            }
            if (err && err.status === 413 && payload.data && payload.data.length > PICTURE_FALLBACK_CHARS) {
                var smaller = await shrinkToJpeg(payload.data, PICTURE_FALLBACK_CHARS);
                payload = {
                    issueId: payload.issueId,
                    name: (payload.name || 'image').replace(/\.[^.]+$/, '') + '.jpg',
                    mime: 'image/jpeg',
                    data: smaller,
                    author: payload.author
                };
                return picturesStore.insert(payload);
            }
            throw err;
        }
    }

    async function savePictures(issueId, pictures) {
        for (var i = 0; i < pictures.length; i++) {
            var pic = pictures[i];
            var row = await insertPicture({
                issueId: issueId,
                name: pic.name,
                mime: pic.mime,
                data: pic.data,
                author: author()
            });
            state.pictures.push(row);
        }
    }

    document.querySelectorAll('.projects-tab-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            writeRoute(state.projectId, btn.getAttribute('data-tab'), null);
        });
    });

    els.formProject.addEventListener('submit', function (event) {
        event.preventDefault();
        withBusy(async function () {
            try {
                var code = normalizeCode(els.projectCode.value);
                if (!validCode(code)) {
                    banner('Project code must be 2–8 characters: a letter, then letters or digits (e.g. PROJ).');
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
                state.projects.push(row);
                state.picked[row.id] = true;
                writeRoute(row.id, 'work', null);
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
                    banner('Project code must be 2–8 characters: a letter, then letters or digits (e.g. PROJ).');
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

    els.featureProject.addEventListener('change', updateFeatureHint);

    els.featureKindPicker.addEventListener('change', function () {
        markKindPicker(els.featureKindPicker);
    });

    els.editKindPicker.addEventListener('change', function () {
        markKindPicker(els.editKindPicker);
        if (state.syncingDrawer || !state.issueId) {
            return;
        }
        patchIssue(state.issueId, { category: selectedKind('edit-kind') });
    });

    document.getElementById('btn-feature-pictures').addEventListener('click', function () {
        els.featurePictures.click();
    });

    els.featurePictures.addEventListener('change', function () {
        var files = snapshotFiles(els.featurePictures);
        withBusy(async function () {
            try {
                var added = await addPicturesFromFiles(files, state.pendingPictures.length);
                state.pendingPictures = state.pendingPictures.concat(added);
                renderPendingPictures();
                banner('');
            } catch (err) {
                fail(err);
            }
        });
    });

    document.getElementById('btn-edit-pictures').addEventListener('click', function () {
        els.editPictures.click();
    });

    els.editPictures.addEventListener('change', function () {
        if (!state.issueId) {
            return;
        }
        var files = snapshotFiles(els.editPictures);
        withBusy(async function () {
            try {
                var added = await addPicturesFromFiles(files, picturesFor(state.issueId).length);
                await savePictures(state.issueId, added);
                paint();
            } catch (err) {
                fail(err);
            }
        });
    });

    els.formIssue.addEventListener('submit', function (event) {
        event.preventDefault();
        var projectId = els.featureProject.value;
        if (!projectId) {
            return;
        }
        withBusy(async function () {
            try {
                var project = byId(state.projects, projectId);
                if (!project || !validCode(projectCodeOf(project))) {
                    banner('Set a project code before adding issues.');
                    return;
                }
                var task = await allocateTask(project);
                var row = await issuesStore.insert({
                    projectId: projectId,
                    title: els.issueTitle.value.trim(),
                    body: els.featureBody.value,
                    status: 'open',
                    priority: els.issuePriority.value,
                    category: selectedKind('feature-kind'),
                    lane: 'backlog',
                    author: author(),
                    taskNumber: task.number,
                    taskKey: task.key
                });
                await savePictures(row.id, state.pendingPictures);
                els.formIssue.reset();
                els.issuePriority.value = 'normal';
                fillKindPicker(els.featureKindPicker, 'feature-kind', 'feature');
                state.pendingPictures = [];
                state.issues.push(row);
                state.picked[projectId] = true;
                writeRoute(projectId, 'backlog', row.id);
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
            category: selectedKind('edit-kind'),
            body: els.editBody.value
        });
    });

    els.editStatus.addEventListener('change', function () {
        if (state.syncingDrawer || !state.issueId) {
            return;
        }
        patchIssue(state.issueId, { status: els.editStatus.value });
    });

    els.issueLaneBtn.addEventListener('click', function () {
        var issue = byId(state.issues, state.issueId);
        if (!issue) {
            return;
        }
        var lane = laneOf(issue);
        if (lane === 'backlog') {
            patchIssue(issue.id, { lane: 'active', status: statusOf(issue) === 'done' ? 'open' : statusOf(issue) });
        } else if (lane === 'archived') {
            patchIssue(issue.id, { lane: 'backlog' });
        } else if (statusOf(issue) === 'done') {
            patchIssue(issue.id, { lane: 'archived' });
        }
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
                await removePicturesForIssues(issueIds);
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
                delete state.picked[project.id];
                writeRoute(null, state.tab, null);
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
                await removePicturesForIssues([issueId]);
                var comments = commentsFor(issueId);
                for (var i = 0; i < comments.length; i++) {
                    await commentsStore.remove(comments[i].id);
                    removeRow(state.comments, comments[i].id);
                }
                await issuesStore.remove(issueId);
                removeRow(state.issues, issueId);
                writeRoute(state.projectId, state.tab, null);
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
                var opts = state.mine ? { mine: true } : {};
                state.issues = await listAll(issuesStore, opts);
                state.pictures = await listAll(picturesStore).catch(function (err) {
                    if (err && err.status === 404) {
                        return [];
                    }
                    throw err;
                });
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
