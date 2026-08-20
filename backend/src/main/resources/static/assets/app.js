/*
 * Ameya Production Tool & Consumable Inventory - web client.
 *
 * Phase 3 scope: sign in, and browse the master data the backend exposes.
 * The transaction ledger, issue/return screens and reports arrive in later
 * phases; this is deliberately read-only over the existing REST API.
 */
(function () {
    'use strict';

    var TOKEN_KEY = 'ameya.auth';

    var el = function (id) { return document.getElementById(id); };

    // ------------------------------------------------------------------
    // session
    // ------------------------------------------------------------------

    var session = null;

    function loadSession() {
        try {
            var raw = sessionStorage.getItem(TOKEN_KEY);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function saveSession(s) {
        session = s;
        sessionStorage.setItem(TOKEN_KEY, JSON.stringify(s));
    }

    function clearSession() {
        session = null;
        sessionStorage.removeItem(TOKEN_KEY);
    }

    // ------------------------------------------------------------------
    // api
    // ------------------------------------------------------------------

    function api(path, options) {
        var opts = options || {};
        var headers = { 'Accept': 'application/json' };
        if (opts.body) headers['Content-Type'] = 'application/json';
        if (session && session.token) headers['Authorization'] = 'Bearer ' + session.token;

        return fetch(path, {
            method: opts.method || 'GET',
            headers: headers,
            body: opts.body ? JSON.stringify(opts.body) : undefined
        }).then(function (res) {
            if (res.status === 401 && session) {
                // Token expired or revoked mid-session.
                showLogin('Your session has expired. Please sign in again.');
                throw new Error('unauthorized');
            }
            if (res.status === 204 || res.status === 205) return null;

            return res.text().then(function (text) {
                var data = null;
                if (text) {
                    try { data = JSON.parse(text); } catch (e) { data = null; }
                }
                if (!res.ok) {
                    var msg = (data && (data.message || data.error)) || ('Request failed (' + res.status + ')');
                    var err = new Error(msg);
                    err.status = res.status;
                    err.fieldErrors = data && data.fieldErrors;
                    throw err;
                }
                return data;
            });
        });
    }

    /** Backend list endpoints return either a bare array or a Spring Page. */
    function unwrap(data) {
        if (Array.isArray(data)) return { rows: data, total: data.length, paged: false };
        if (data && Array.isArray(data.content)) {
            return { rows: data.content, total: data.totalElements, paged: true };
        }
        return { rows: [], total: 0, paged: false };
    }

    // ------------------------------------------------------------------
    // small render helpers
    // ------------------------------------------------------------------

    function esc(v) {
        if (v === null || v === undefined || v === '') return '';
        return String(v)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function dash(v) {
        return (v === null || v === undefined || v === '') ? '<span class="muted">&mdash;</span>' : esc(v);
    }

    function boolPill(v) {
        return v ? '<span class="pill yes">Active</span>' : '<span class="pill no">Inactive</span>';
    }

    function num(v) {
        if (v === null || v === undefined || v === '') return '<span class="muted">&mdash;</span>';
        return esc(v);
    }

    function table(columns, rows) {
        if (!rows.length) {
            return '<div class="card"><div class="empty">Nothing recorded yet.</div></div>';
        }
        var head = columns.map(function (c) { return '<th>' + esc(c.label) + '</th>'; }).join('');
        var body = rows.map(function (r) {
            return '<tr>' + columns.map(function (c) { return '<td>' + c.cell(r) + '</td>'; }).join('') + '</tr>';
        }).join('');
        return '<div class="card"><div class="table-scroll"><table>' +
            '<thead><tr>' + head + '</tr></thead>' +
            '<tbody>' + body + '</tbody>' +
            '</table></div></div>';
    }

    function renderList(path, columns, note) {
        el('content').innerHTML = '<p class="section-head">Loading&hellip;</p>';
        api(path).then(function (data) {
            var out = unwrap(data);
            var html = table(columns, out.rows);
            if (out.rows.length) {
                html += '<p class="count-note">' + out.rows.length + ' of ' + out.total + ' record'
                    + (out.total === 1 ? '' : 's') + (note ? ' &middot; ' + note : '') + '</p>';
            }
            el('content').innerHTML = html;
        }).catch(function (err) {
            if (err.message === 'unauthorized') return;
            el('content').innerHTML = '<div class="alert">' + esc(err.message) + '</div>';
        });
    }

    // ------------------------------------------------------------------
    // pages
    // ------------------------------------------------------------------

    var PAGES = {
        dashboard: {
            label: 'Dashboard',
            render: function () {
                var sources = [
                    { key: 'Items', path: '/api/items?size=1' },
                    { key: 'Item categories', path: '/api/item-categories' },
                    { key: 'Machines', path: '/api/machines?size=1' },
                    { key: 'Employees', path: '/api/employees?size=1' },
                    { key: 'Departments', path: '/api/departments' },
                    { key: 'Suppliers', path: '/api/suppliers?size=1' },
                    { key: 'Manufacturers', path: '/api/manufacturers' },
                    { key: 'Units of measure', path: '/api/units-of-measure' }
                ];

                el('content').innerHTML =
                    '<p class="section-head">Master data currently in the system.</p>' +
                    '<div class="tiles">' + sources.map(function (s, i) {
                        return '<div class="tile"><div class="n" id="tile-' + i + '">&hellip;</div>' +
                            '<div class="k">' + esc(s.key) + '</div></div>';
                    }).join('') + '</div>' +
                    '<p class="count-note">Stock levels and transactions appear here once the ' +
                    'inventory ledger (Phase 4) is built.</p>';

                sources.forEach(function (s, i) {
                    api(s.path).then(function (data) {
                        var node = el('tile-' + i);
                        if (node) node.textContent = unwrap(data).total;
                    }).catch(function () {
                        var node = el('tile-' + i);
                        if (node) node.textContent = '—';
                    });
                });
            }
        },

        items: {
            label: 'Items',
            render: function () {
                renderList('/api/items?size=200&sort=itemCode', [
                    { label: 'Code', cell: function (r) { return '<strong>' + dash(r.itemCode) + '</strong>'; } },
                    { label: 'Name', cell: function (r) { return dash(r.name); } },
                    { label: 'Category', cell: function (r) { return dash(r.categoryName); } },
                    { label: 'Manufacturer', cell: function (r) { return dash(r.manufacturerName); } },
                    { label: 'Part no.', cell: function (r) { return dash(r.partNumber); } },
                    { label: 'UOM', cell: function (r) { return dash(r.uomCode); } },
                    { label: 'Safe stock', cell: function (r) { return num(r.safeStock); } },
                    { label: 'Unit cost', cell: function (r) { return num(r.currentUnitCost); } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ], 'showing first 200');
            }
        },

        categories: {
            label: 'Item categories',
            render: function () {
                renderList('/api/item-categories', [
                    { label: 'Name', cell: function (r) { return '<strong>' + dash(r.name) + '</strong>'; } },
                    { label: 'Parent', cell: function (r) { return dash(r.parentCategoryName); } },
                    {
                        label: 'Custom attributes', cell: function (r) {
                            if (!r.attributes || !r.attributes.length) return '<span class="muted">&mdash;</span>';
                            return esc(r.attributes.map(function (a) { return a.name; }).join(', '));
                        }
                    },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ]);
            }
        },

        machines: {
            label: 'Machines',
            render: function () {
                renderList('/api/machines?size=200&sort=machineCode', [
                    { label: 'Code', cell: function (r) { return '<strong>' + dash(r.machineCode) + '</strong>'; } },
                    { label: 'Name', cell: function (r) { return dash(r.machineName); } },
                    { label: 'Type', cell: function (r) { return dash(r.machineType); } },
                    { label: 'Department', cell: function (r) { return dash(r.departmentName); } },
                    { label: 'Location', cell: function (r) { return dash(r.location); } },
                    { label: 'Model', cell: function (r) { return dash(r.model); } },
                    { label: 'Condition', cell: function (r) { return dash(r.status); } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ], 'showing first 200');
            }
        },

        employees: {
            label: 'Employees',
            render: function () {
                renderList('/api/employees?size=200&sort=employeeCode', [
                    { label: 'Code', cell: function (r) { return '<strong>' + dash(r.employeeCode) + '</strong>'; } },
                    { label: 'Name', cell: function (r) { return dash(r.name); } },
                    { label: 'Department', cell: function (r) { return dash(r.departmentName); } },
                    { label: 'Designation', cell: function (r) { return dash(r.designation); } },
                    { label: 'Contact', cell: function (r) { return dash(r.contact); } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ], 'showing first 200');
            }
        },

        departments: {
            label: 'Departments',
            render: function () {
                renderList('/api/departments', [
                    { label: 'Name', cell: function (r) { return '<strong>' + dash(r.name) + '</strong>'; } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ]);
            }
        },

        suppliers: {
            label: 'Suppliers',
            render: function () {
                renderList('/api/suppliers?size=200&sort=name', [
                    { label: 'Name', cell: function (r) { return '<strong>' + dash(r.name) + '</strong>'; } },
                    { label: 'Contact person', cell: function (r) { return dash(r.contactPerson); } },
                    { label: 'Phone', cell: function (r) { return dash(r.phone); } },
                    { label: 'Email', cell: function (r) { return dash(r.email); } },
                    { label: 'GST no.', cell: function (r) { return dash(r.gstNumber); } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ], 'showing first 200');
            }
        },

        manufacturers: {
            label: 'Manufacturers',
            render: function () {
                renderList('/api/manufacturers', [
                    { label: 'Name', cell: function (r) { return '<strong>' + dash(r.name) + '</strong>'; } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ]);
            }
        },

        uoms: {
            label: 'Units of measure',
            render: function () {
                renderList('/api/units-of-measure', [
                    { label: 'Code', cell: function (r) { return '<strong>' + dash(r.code) + '</strong>'; } },
                    { label: 'Name', cell: function (r) { return dash(r.name); } },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ]);
            }
        },

        users: {
            label: 'Users',
            adminOnly: true,
            render: function () {
                renderList('/api/users?size=200&sort=username', [
                    { label: 'Username', cell: function (r) { return '<strong>' + dash(r.username) + '</strong>'; } },
                    { label: 'Employee', cell: function (r) { return dash(r.employeeName); } },
                    { label: 'Role', cell: function (r) { return '<span class="role-chip">' + dash(r.roleName) + '</span>'; } },
                    {
                        label: 'Last login', cell: function (r) {
                            if (!r.lastLoginAt) return '<span class="muted">never</span>';
                            return esc(new Date(r.lastLoginAt).toLocaleString());
                        }
                    },
                    { label: 'Status', cell: function (r) { return boolPill(r.active); } }
                ], 'showing first 200');
            }
        }
    };

    var currentPage = null;

    function buildNav() {
        var isAdmin = session && session.role && session.role.indexOf('ADMIN') !== -1;
        var nav = el('nav');
        nav.innerHTML = '';

        Object.keys(PAGES).forEach(function (key) {
            var page = PAGES[key];
            if (page.adminOnly && !isAdmin) return;

            var btn = document.createElement('button');
            btn.type = 'button';
            btn.textContent = page.label;
            btn.dataset.page = key;
            btn.addEventListener('click', function () { go(key); });
            nav.appendChild(btn);
        });
    }

    function go(key) {
        var page = PAGES[key];
        if (!page) return;
        currentPage = key;

        Array.prototype.forEach.call(el('nav').children, function (btn) {
            btn.classList.toggle('active', btn.dataset.page === key);
        });

        el('page-title').textContent = page.label;
        page.render();
    }

    // ------------------------------------------------------------------
    // views
    // ------------------------------------------------------------------

    function showLogin(message) {
        clearSession();
        el('app-view').classList.add('hidden');
        el('login-view').classList.remove('hidden');
        el('pw-modal').classList.add('hidden');

        var box = el('login-error');
        if (message) {
            box.textContent = message;
            box.classList.remove('hidden');
        } else {
            box.classList.add('hidden');
        }
        el('password').value = '';
        el('username').focus();
    }

    function showApp() {
        el('login-view').classList.add('hidden');
        el('app-view').classList.remove('hidden');
        el('who-user').textContent = session.username;
        el('who-role').textContent = (session.role || '').replace(/^ROLE_/, '');
        buildNav();
        go(currentPage && PAGES[currentPage] ? currentPage : 'dashboard');
    }

    // ------------------------------------------------------------------
    // wiring
    // ------------------------------------------------------------------

    el('login-form').addEventListener('submit', function (e) {
        e.preventDefault();
        var btn = el('login-btn');
        var box = el('login-error');

        box.classList.add('hidden');
        btn.disabled = true;
        btn.textContent = 'Signing in…';

        api('/api/auth/login', {
            method: 'POST',
            body: { username: el('username').value.trim(), password: el('password').value }
        }).then(function (res) {
            saveSession({ token: res.token, username: res.username, role: res.role, userId: res.userId });
            el('password').value = '';
            showApp();
        }).catch(function (err) {
            box.textContent = err.message || 'Sign in failed.';
            box.classList.remove('hidden');
            el('password').value = '';
            el('password').focus();
        }).finally(function () {
            btn.disabled = false;
            btn.textContent = 'Sign in';
        });
    });

    el('logout-btn').addEventListener('click', function () {
        currentPage = null;
        showLogin();
    });

    el('change-pw-btn').addEventListener('click', function () {
        el('pw-form').reset();
        el('pw-msg').classList.add('hidden');
        el('pw-modal').classList.remove('hidden');
        el('pw-current').focus();
    });

    el('pw-cancel').addEventListener('click', function () {
        el('pw-modal').classList.add('hidden');
    });

    el('pw-form').addEventListener('submit', function (e) {
        e.preventDefault();
        var msg = el('pw-msg');
        msg.classList.add('hidden');
        msg.classList.remove('ok');

        if (el('pw-new').value !== el('pw-confirm').value) {
            msg.textContent = 'The new passwords do not match.';
            msg.classList.remove('hidden');
            return;
        }

        api('/api/users/me/change-password', {
            method: 'POST',
            body: { currentPassword: el('pw-current').value, newPassword: el('pw-new').value }
        }).then(function () {
            msg.textContent = 'Password updated. Sign in again with your new password.';
            msg.classList.add('ok');
            msg.classList.remove('hidden');
            setTimeout(function () {
                currentPage = null;
                showLogin('Password changed. Please sign in again.');
            }, 1800);
        }).catch(function (err) {
            if (err.message === 'unauthorized') return;
            msg.textContent = err.message || 'Could not change the password.';
            msg.classList.remove('hidden');
        });
    });

    // ------------------------------------------------------------------
    // boot
    // ------------------------------------------------------------------

    session = loadSession();
    if (session && session.token) {
        showApp();
    } else {
        showLogin();
    }
})();
