'use strict';
(() => {
  const root = document.documentElement;
  const english = root.lang === 'en';
  try {
    if (localStorage.getItem('lchat-skin') === 'telegram') root.removeAttribute('data-pixel-theme');
  } catch (_) { /* keep the markup default */ }

  const menu = document.getElementById('weba-menu');
  if (menu && !menu.querySelector('.pixel-appearance')) {
    const box = document.createElement('div');
    box.className = 'pixel-appearance';
    const skinLabel = document.createElement('div');
    skinLabel.className = 'pixel-appearance-label';
    skinLabel.textContent = english ? 'Look' : 'Оформление';
    const skinRow = document.createElement('div');
    skinRow.className = 'pixel-appearance-row';
    skinRow.setAttribute('role', 'group');
    skinRow.setAttribute('aria-label', skinLabel.textContent);
    const modeLabel = document.createElement('div');
    modeLabel.className = 'pixel-appearance-label';
    modeLabel.textContent = english ? 'Theme' : 'Тема';
    const modeRow = document.createElement('div');
    modeRow.className = 'pixel-appearance-row';
    modeRow.setAttribute('role', 'group');
    modeRow.setAttribute('aria-label', modeLabel.textContent);

    function paint() {
      const pixel = root.hasAttribute('data-pixel-theme');
      const light = root.dataset.lchatTheme === 'light';
      for (const button of skinRow.children) button.setAttribute('aria-pressed', String(button.dataset.skin === (pixel ? 'pixel' : 'telegram')));
      for (const button of modeRow.children) button.setAttribute('aria-pressed', String(button.dataset.mode === (light ? 'light' : 'dark')));
    }
    function setSkin(skin) {
      const pixel = skin !== 'telegram';
      root.toggleAttribute('data-pixel-theme', pixel);
      try { localStorage.setItem('lchat-skin', pixel ? 'pixel' : 'telegram'); } catch (_) {}
      paint();
    }
    function setMode(mode) {
      const light = mode === 'light';
      root.dataset.lchatTheme = light ? 'light' : 'dark';
      const darkCss = document.getElementById('dark-theme-style');
      const darkDetails = document.getElementById('dark-details-style');
      if (darkCss) darkCss.disabled = light;
      if (darkDetails) darkDetails.disabled = light;
      try {
        const saved = JSON.parse(localStorage.getItem('lchat-appearance') || '{}');
        saved.theme = light ? 'light' : 'dark';
        localStorage.setItem('lchat-appearance', JSON.stringify(saved));
      } catch (_) {}
      paint();
    }
    for (const [skin, label] of [['pixel', english ? 'Pixel' : 'Пиксель'], ['telegram', 'Telegram']]) {
      const button = document.createElement('button');
      button.type = 'button';
      button.dataset.skin = skin;
      button.textContent = label;
      button.addEventListener('click', () => setSkin(skin));
      skinRow.append(button);
    }
    for (const [mode, label] of [['dark', english ? 'Dark' : 'Тёмная'], ['light', english ? 'Light' : 'Светлая']]) {
      const button = document.createElement('button');
      button.type = 'button';
      button.dataset.mode = mode;
      button.textContent = label;
      button.addEventListener('click', () => setMode(mode));
      modeRow.append(button);
    }
    box.append(skinLabel, skinRow, modeLabel, modeRow);
    menu.prepend(box);
    paint();
    new MutationObserver(paint).observe(root, {attributes: true, attributeFilter: ['data-lchat-theme', 'data-pixel-theme']});
  }

  const header = document.querySelector('#messenger .sidebar-header');
  const search = document.getElementById('chat-filter');
  if (!header || !search) return;

  const mobile = matchMedia('(max-width: 760px)');
  const toggle = document.createElement('button');
  toggle.type = 'button';
  toggle.className = 'icon-button pixel-search-toggle';
  toggle.setAttribute('aria-controls', search.id);
  toggle.setAttribute('aria-expanded', 'false');
  header.append(toggle);
  header.classList.add('pixel-header');

  function setSearch(open, moveFocus = true) {
    header.classList.toggle('pixel-search-open', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? (english ? 'Close search' : 'Закрыть поиск') : (english ? 'Search chats' : 'Поиск чатов'));
    toggle.innerHTML = open
      ? '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" fill="none" stroke="currentColor" stroke-width="2"/></svg>'
      : '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6" fill="none" stroke="currentColor" stroke-width="2"/><path d="m15 15 5 5" stroke="currentColor" stroke-width="2"/></svg>';
    if (moveFocus) (open ? search : toggle).focus({preventScroll: true});
  }

  function closeSearch() {
    if (search.value) {
      search.value = '';
      search.dispatchEvent(new Event('input', {bubbles: true}));
    }
    setSearch(false);
  }

  setSearch(mobile.matches && Boolean(search.value), false);
  toggle.addEventListener('click', () => {
    if (header.classList.contains('pixel-search-open')) closeSearch();
    else setSearch(true);
  });
  search.addEventListener('keydown', event => {
    if (event.key === 'Escape' && mobile.matches) closeSearch();
  });
  mobile.addEventListener('change', () => {
    if (mobile.matches && (search.value || document.activeElement === search)) setSearch(true, false);
  });

  const menuButton = document.getElementById('main-menu');
  const strip = document.querySelector('.sidebar-quick');
  const newChatItem = menu?.querySelector('[data-proxy-target="new-chat"]');
  const contactsItem = menu?.querySelector('[data-proxy-target="contacts-button"]');
  if (!strip || !menuButton || !newChatItem || !contactsItem || strip.children.length < 3) return;
  const actions = Array.from(strip.querySelectorAll('button')).slice(2);
  for (const action of actions.reverse()) {
    action.classList.remove('quick-action');
    action.classList.add('pixel-menu-action');
    action.addEventListener('click', () => {
      menu.hidden = true;
      menuButton.setAttribute('aria-expanded', 'false');
    });
    newChatItem.after(action);
  }
  strip.classList.add('pixel-shortcuts-in-menu');
})();
