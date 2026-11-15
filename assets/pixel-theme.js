'use strict';
(() => {
  const root = document.documentElement;
  const header = document.querySelector('#messenger .sidebar-header');
  const search = document.getElementById('chat-filter');
  if (!header || !search) return;

  const english = root.lang === 'en';
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

  const menu = document.getElementById('weba-menu');
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
