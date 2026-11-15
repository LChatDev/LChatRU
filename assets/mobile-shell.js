'use strict';
(() => {
  const messenger = document.getElementById('messenger');
  const back = document.getElementById('mobile-back');
  const messages = document.getElementById('messages');
  const list = document.getElementById('chat-list');
  if (!messenger || !back || !messages || !list) return;

  const mobile = matchMedia('(max-width: 760px)');
  const viewport = window.visualViewport;
  const inputSelector = 'textarea,input:not([type]),input[type="text"],input[type="search"],[contenteditable="true"]';
  back.setAttribute('aria-label', document.documentElement.lang === 'en' ? 'Back to chats' : 'Назад к чатам');

  function syncViewport() {
    const editing = mobile.matches && (document.activeElement?.matches(inputSelector) || false);
    const height = viewport?.height ?? window.innerHeight;
    const keyboard = editing && (!viewport || viewport.scale === 1) && document.documentElement.clientHeight - height > 100;
    const pinned = messages.scrollHeight - messages.scrollTop - messages.clientHeight < 48;

    messenger.classList.toggle('mobile-keyboard', keyboard);
    if (keyboard) messenger.style.setProperty('--mobile-viewport-height', `${Math.round(height)}px`);
    else messenger.style.removeProperty('--mobile-viewport-height');

    if (mobile.matches && pinned && messenger.classList.contains('chat-open')) {
      messages.scrollTo({top: messages.scrollHeight, behavior: 'instant'});
    }
  }

  let frame = 0;
  function scheduleViewport() {
    if (frame) return;
    frame = requestAnimationFrame(() => { frame = 0; syncViewport(); });
  }

  viewport?.addEventListener('resize', scheduleViewport);
  window.addEventListener('resize', scheduleViewport);
  document.addEventListener('focusin', scheduleViewport);
  document.addEventListener('focusout', scheduleViewport);
  mobile.addEventListener('change', scheduleViewport);

  let lastRow = -1;
  list.addEventListener('click', event => {
    const row = event.target.closest('.chat-row');
    lastRow = row ? Array.from(list.querySelectorAll('.chat-row')).indexOf(row) : -1;
  }, {capture: true});

  let open = messenger.classList.contains('chat-open');
  new MutationObserver(() => {
    const next = messenger.classList.contains('chat-open');
    if (open === next) return;
    open = next;
    messenger.classList.toggle('mobile-returning', mobile.matches && !open);
    if (!mobile.matches || messenger.hidden) return;
    if (open) back.focus({preventScroll: true});
    else (list.querySelectorAll('.chat-row')[lastRow] ?? document.getElementById('chat-filter'))?.focus({preventScroll: true});
    scheduleViewport();
  }).observe(messenger, {attributes: true, attributeFilter: ['class']});
})();
