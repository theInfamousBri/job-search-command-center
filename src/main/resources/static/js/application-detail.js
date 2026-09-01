(() => {
    const openEventEditor = targetId => {
        const editor = document.getElementById(targetId);
        if (!editor) return false;

        document.querySelectorAll('.timeline-inline-editor.is-open').forEach(openEditor => {
            if (openEditor !== editor) openEditor.classList.remove('is-open');
        });

        editor.classList.add('is-open');

        requestAnimationFrame(() => {
            editor.scrollIntoView({block: 'center', behavior: 'auto'});
            const url = new URL(window.location.href);
            url.hash = targetId;
            history.replaceState(null, '', url.href);
        });
        return true;
    };

    document.querySelectorAll('[data-event-editor-target]').forEach(toggle => {
        toggle.addEventListener('click', event => {
            const targetId = toggle.dataset.eventEditorTarget;
            if (!targetId || !openEventEditor(targetId)) return;
            event.preventDefault();
        });
    });

    document.querySelectorAll('[data-event-editor-close]').forEach(button => {
        button.addEventListener('click', () => {
            const targetId = button.dataset.eventEditorClose;
            const editor = targetId ? document.getElementById(targetId) : null;
            if (editor) editor.classList.remove('is-open');

            const returnId = button.dataset.eventReturn;
            const returnTarget = returnId ? document.getElementById(returnId) : null;
            if (returnTarget) {
                returnTarget.scrollIntoView({block: 'center', behavior: 'auto'});
                const url = new URL(window.location.href);
                url.searchParams.delete('editEvent');
                url.hash = returnId;
                history.replaceState(null, '', url.href);
            }
        });
    });

    // If the server rendered edit mode as a progressive-enhancement fallback,
    // make sure the selected inline editor is visible after layout settles.
    const serverOpenEditor = document.querySelector('.timeline-inline-editor.is-open');
    if (serverOpenEditor && window.location.hash === `#${serverOpenEditor.id}`) {
        requestAnimationFrame(() => serverOpenEditor.scrollIntoView({block: 'center', behavior: 'auto'}));
    }

    // Dashboard attention actions can deep-link directly into the Add activity composer.
    const activityComposer = document.getElementById('activity-composer');
    if (activityComposer && window.location.hash === '#activity-composer') {
        activityComposer.open = true;
        requestAnimationFrame(() => activityComposer.scrollIntoView({block: 'center', behavior: 'auto'}));
    }

    const manager = document.getElementById('manage-materials');
    if (!manager) return;

    const RETURN_KEY = 'jobsearch.applicationMaterialsReturn';
    const RETURN_TTL_MS = 30_000;

    // Remember the user's working position before any material-management POST.
    // This is intentionally client-owned instead of relying only on the redirect
    // fragment, because browsers can restore POST/redirect navigation positions
    // inconsistently across otherwise identical material actions.
    manager.addEventListener('submit', event => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        if ((form.method || 'get').toLowerCase() !== 'post') return;
        if (event.defaultPrevented) return;

        try {
            sessionStorage.setItem(RETURN_KEY, JSON.stringify({
                path: window.location.pathname,
                savedAt: Date.now()
            }));
        } catch (_) {
            // sessionStorage can be unavailable in hardened/private contexts.
            // The redirect hash + flash-state fallback below still works there.
        }
    });

    let storedReturn = null;
    try {
        const raw = sessionStorage.getItem(RETURN_KEY);
        if (raw) {
            const parsed = JSON.parse(raw);
            const fresh = Number.isFinite(parsed.savedAt) && Date.now() - parsed.savedAt <= RETURN_TTL_MS;
            if (parsed.path === window.location.pathname && fresh) {
                storedReturn = parsed;
            }
            sessionStorage.removeItem(RETURN_KEY);
        }
    } catch (_) {
        // Ignore unavailable/corrupt sessionStorage and fall back to server state.
    }

    const serverReturn = manager.dataset.returnActive === 'true';
    const hashReturn = window.location.hash === '#manage-materials';
    if (!storedReturn && !serverReturn && !hashReturn) return;

    manager.open = true;

    const returnToManager = () => {
        manager.scrollIntoView({block: 'start', behavior: 'auto'});
        const url = new URL(window.location.href);
        url.hash = 'manage-materials';
        history.replaceState(null, '', url.href);
    };

    const restore = () => {
        requestAnimationFrame(() => {
            returnToManager();
            window.setTimeout(returnToManager, 80);
            window.setTimeout(returnToManager, 220);
        });
    };

    if (document.readyState === 'complete') {
        restore();
    } else {
        window.addEventListener('load', restore, {once: true});
    }
})();
