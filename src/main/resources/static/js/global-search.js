(() => {
    const dialog = document.querySelector('[data-global-search-dialog]');
    const input = document.querySelector('[data-global-search-input]');
    const results = document.querySelector('[data-global-search-results]');
    const empty = document.querySelector('[data-global-search-empty]');
    const trigger = document.querySelector('[data-global-search-trigger]');
    const triggerForm = document.querySelector('[data-global-search-trigger-form]');
    const shortcut = document.querySelector('[data-search-shortcut]');

    if (!dialog || !input || !results || !empty || !trigger) return;

    // Keep the full-screen overlay out of the sticky topbar's backdrop-filter
    // stacking context so the page dims uniformly behind the palette.
    if (dialog.parentElement !== document.body) {
        document.body.append(dialog);
    }

    const isMac = /Mac|iPhone|iPad|iPod/.test(navigator.platform || navigator.userAgent);
    if (shortcut) shortcut.textContent = isMac ? '⌘ K' : 'Ctrl K';

    let activeIndex = -1;
    let debounceTimer = null;
    let request = null;
    let lastFocused = null;
    let suppressTriggerOpen = false;

    const resultItems = () => Array.from(results.querySelectorAll('[data-search-result]'));

    function openPalette(seed = '') {
        if (!dialog.hidden) return;
        lastFocused = document.activeElement;
        dialog.hidden = false;
        document.body.classList.add('command-palette-open');
        input.value = seed || '';
        resetSelection();
        window.setTimeout(() => {
            input.focus();
            input.select();
        }, 0);
        if (input.value.trim()) search(input.value);
        else showPrompt();
    }

    function closePalette({restoreFocus = true} = {}) {
        if (dialog.hidden) return;
        if (request) request.abort();
        clearTimeout(debounceTimer);
        dialog.hidden = true;
        document.body.classList.remove('command-palette-open');
        results.replaceChildren();
        showPrompt();
        if (restoreFocus && lastFocused && typeof lastFocused.focus === 'function') {
            suppressTriggerOpen = true;
            lastFocused.focus();
            window.setTimeout(() => { suppressTriggerOpen = false; }, 0);
        }
    }


    function scrollToHash(hash, behavior = 'smooth') {
        if (!hash) return false;
        const targetElement = document.getElementById(decodeURIComponent(hash.slice(1)));
        if (!targetElement) return false;
        targetElement.scrollIntoView({block: 'center', behavior});
        return true;
    }

    function navigateToResult(url) {
        const target = new URL(url, window.location.href);
        const current = new URL(window.location.href);
        const sameDocument = target.origin === current.origin
            && target.pathname === current.pathname
            && target.search === current.search;

        closePalette({restoreFocus: false});

        if (sameDocument) {
            if (target.hash && target.hash !== current.hash) {
                history.pushState(null, '', target.href);
            }
            scrollToHash(target.hash);
            return;
        }

        if (target.hash) {
            sessionStorage.setItem('globalSearchScrollTarget', JSON.stringify({
                path: target.pathname + target.search,
                hash: target.hash
            }));
            // Let the destination page own the final scroll. Native fragment scrolling can
            // race with scroll restoration/layout changes and leave the viewport at the top.
            target.hash = '';
        }
        window.location.assign(target.href);
    }

    function showPrompt() {
        results.hidden = true;
        empty.hidden = false;
        empty.classList.remove('is-error');
        empty.querySelector('strong').textContent = 'Search your command center';
        empty.querySelector('p').textContent = 'Find an application, company, person, or exact requisition / job ID.';
        resetSelection();
    }

    function showMessage(title, message, error = false) {
        results.hidden = true;
        empty.hidden = false;
        empty.classList.toggle('is-error', error);
        empty.querySelector('strong').textContent = title;
        empty.querySelector('p').textContent = message;
        resetSelection();
    }

    function resetSelection() {
        activeIndex = -1;
        input.removeAttribute('aria-activedescendant');
    }

    function setActive(nextIndex) {
        const items = resultItems();
        if (!items.length) {
            resetSelection();
            return;
        }
        activeIndex = ((nextIndex % items.length) + items.length) % items.length;
        items.forEach((item, index) => {
            const active = index === activeIndex;
            item.classList.toggle('is-active', active);
            item.setAttribute('aria-selected', active ? 'true' : 'false');
            if (active) {
                input.setAttribute('aria-activedescendant', item.id);
                item.scrollIntoView({block: 'nearest'});
            }
        });
    }

    function resultElement(item, index) {
        const anchor = document.createElement('a');
        anchor.className = `command-result command-result-${item.type}`;
        anchor.href = item.url;
        anchor.id = `global-search-result-${index}`;
        anchor.dataset.searchResult = '';
        anchor.setAttribute('role', 'option');
        anchor.setAttribute('aria-selected', 'false');
        if (item.exactMatch) anchor.classList.add('is-exact');

        const avatar = document.createElement('span');
        avatar.className = 'command-result-avatar';
        avatar.textContent = item.initials || '•';
        avatar.setAttribute('aria-hidden', 'true');

        const copy = document.createElement('span');
        copy.className = 'command-result-copy';

        const titleRow = document.createElement('span');
        titleRow.className = 'command-result-title-row';
        const title = document.createElement('strong');
        title.textContent = item.title || 'Untitled';
        titleRow.append(title);
        if (item.badge) {
            const badge = document.createElement('small');
            badge.className = 'command-result-badge';
            badge.textContent = item.badge;
            titleRow.append(badge);
        }
        copy.append(titleRow);

        if (item.subtitle) {
            const subtitle = document.createElement('span');
            subtitle.className = 'command-result-subtitle';
            subtitle.textContent = item.subtitle;
            copy.append(subtitle);
        }
        if (item.meta) {
            const meta = document.createElement('span');
            meta.className = 'command-result-meta';
            meta.textContent = item.meta;
            copy.append(meta);
        }

        const arrow = document.createElement('span');
        arrow.className = 'command-result-arrow';
        arrow.textContent = '→';
        arrow.setAttribute('aria-hidden', 'true');

        anchor.append(avatar, copy, arrow);
        anchor.addEventListener('mousemove', () => {
            const items = resultItems();
            const current = items.indexOf(anchor);
            if (current >= 0 && current !== activeIndex) setActive(current);
        });
        anchor.addEventListener('click', event => {
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            navigateToResult(anchor.href);
        });
        return anchor;
    }

    function render(data) {
        results.replaceChildren();
        resetSelection();

        if (!data || !data.totalResults) {
            showMessage('No matches found', `Nothing matched “${input.value.trim()}”. Try a company, role, person, or requisition ID.`);
            return;
        }

        let resultIndex = 0;
        for (const group of data.groups || []) {
            if (!group.results || !group.results.length) continue;

            const section = document.createElement('section');
            section.className = 'command-result-group';

            const heading = document.createElement('div');
            heading.className = 'command-result-group-heading';
            const label = document.createElement('span');
            label.textContent = group.label;
            const count = document.createElement('small');
            count.textContent = group.results.length;
            heading.append(label, count);
            section.append(heading);

            for (const item of group.results) {
                section.append(resultElement(item, resultIndex++));
            }
            results.append(section);
        }

        empty.hidden = true;
        results.hidden = false;
        setActive(0);
    }

    async function search(value) {
        const query = value.trim();
        if (!query) {
            if (request) request.abort();
            showPrompt();
            return;
        }

        if (request) request.abort();
        request = new AbortController();
        showMessage('Searching…', 'Looking across applications, companies, and people.');

        try {
            const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`, {
                headers: {'Accept': 'application/json'},
                credentials: 'same-origin',
                signal: request.signal
            });
            if (!response.ok) throw new Error(`Search failed with ${response.status}`);
            render(await response.json());
        } catch (error) {
            if (error.name === 'AbortError') return;
            showMessage('Search unavailable', 'Something went wrong while searching. You can still press Enter in the top search to search applications.', true);
        }
    }

    trigger.addEventListener('focus', () => {
        if (!suppressTriggerOpen) openPalette(trigger.value);
    });
    trigger.addEventListener('click', event => {
        event.preventDefault();
        openPalette(trigger.value);
    });
    if (triggerForm) {
        triggerForm.addEventListener('submit', event => {
            event.preventDefault();
            openPalette(trigger.value);
        });
    }

    input.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = window.setTimeout(() => search(input.value), 120);
    });

    input.addEventListener('keydown', event => {
        const items = resultItems();
        if (event.key === 'ArrowDown') {
            event.preventDefault();
            setActive(activeIndex + 1);
        } else if (event.key === 'ArrowUp') {
            event.preventDefault();
            setActive(activeIndex <= 0 ? items.length - 1 : activeIndex - 1);
        } else if (event.key === 'Enter') {
            event.preventDefault();
            if (activeIndex >= 0 && items[activeIndex]) {
                items[activeIndex].click();
            } else if (input.value.trim()) {
                window.location.assign(`/applications?q=${encodeURIComponent(input.value.trim())}`);
            }
        } else if (event.key === 'Escape') {
            event.preventDefault();
            closePalette();
        }
    });

    dialog.addEventListener('mousedown', event => {
        if (event.target === dialog) closePalette();
    });

    document.addEventListener('keydown', event => {
        if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
            event.preventDefault();
            if (dialog.hidden) openPalette();
            else closePalette();
        } else if (event.key === 'Escape' && !dialog.hidden) {
            event.preventDefault();
            closePalette();
        }
    });

    const pendingScrollRaw = sessionStorage.getItem('globalSearchScrollTarget');
    if (pendingScrollRaw) {
        sessionStorage.removeItem('globalSearchScrollTarget');

        let pendingScroll;
        try {
            pendingScroll = JSON.parse(pendingScrollRaw);
        } catch (error) {
            // Backward-compatible with the previous version that stored only the hash.
            pendingScroll = {path: window.location.pathname + window.location.search, hash: pendingScrollRaw};
        }

        const currentPath = window.location.pathname + window.location.search;
        if (pendingScroll && pendingScroll.hash && pendingScroll.path === currentPath) {
            let completed = false;

            const finishPendingScroll = () => {
                if (completed) return true;
                const scrolled = scrollToHash(pendingScroll.hash, 'auto');
                if (!scrolled) return false;

                const destination = new URL(window.location.href);
                destination.hash = pendingScroll.hash;
                history.replaceState(null, '', destination.href);
                completed = true;
                return true;
            };

            // Defer until the destination is fully laid out, then retry briefly in case
            // fonts/images or browser scroll restoration adjust the viewport afterward.
            const schedulePendingScroll = () => {
                if (finishPendingScroll()) {
                    window.setTimeout(() => scrollToHash(pendingScroll.hash, 'auto'), 80);
                    window.setTimeout(() => scrollToHash(pendingScroll.hash, 'auto'), 220);
                } else {
                    window.setTimeout(finishPendingScroll, 80);
                    window.setTimeout(finishPendingScroll, 220);
                    window.setTimeout(finishPendingScroll, 500);
                }
            };

            if (document.readyState === 'complete') schedulePendingScroll();
            else window.addEventListener('load', schedulePendingScroll, {once: true});
        }
    }
})();
