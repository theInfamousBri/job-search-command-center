(() => {
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
