/* LChat · темы «Аврора», «Ретро-терминал», «Минимал» (жидкое стекло), «Windows XP», «ВК 2010», «Сакура», «Без CSS»
   и выдвижное меню как в Telegram. Только оформление: логика приложения не меняется,
   все пункты меню вызывают уже существующие кнопки и ссылки. */
'use strict';
(() => {
  const root = document.documentElement;
  const KEY = 'lchat-lx-theme';
  const THEMES = [
    ['aurora', 'Аврора', '#0b0f2e'],
    ['retro', 'Ретро', '#050805'],
    ['minimal', 'Стекло', '#F4F2FB'],
    ['xp', 'Windows XP', '#0058EE'],
    ['vk2010', 'ВК 2010', '#45688E'],
    ['sakura', 'Сакура', '#F48FB1'],
    ['nocss', 'Без CSS', '#FFFFFF'],
    ['aqua', 'Aqua', '#D7D7D7'],
    ['terminal', 'Терминал', '#000000'],
    ['icq', 'ICQ 2003', '#D9E1EC'],
    ['pixel', 'Пиксельный', null],
    ['telegram', 'LChatClassic', null]
  ];
  const ORIGINAL = ['pixel', 'telegram'];
  /* «Пиксельный» и «LChatClassic» — родные скины сайта (ключ lchat-skin, атрибут data-pixel-theme) */
  const skin = () => { try { return localStorage.getItem('lchat-skin') === 'telegram' ? 'telegram' : 'pixel'; } catch (_) { return 'pixel'; } };
  const get = () => {
    let v = 'pixel';
    try {
      v = localStorage.getItem(KEY);
      if (!v) v = localStorage.getItem('lchat-skin') === 'telegram' ? 'telegram' : 'pixel';
    } catch (_) {}
    return v === 'classic' ? skin() : v;
  };
  const menu = document.getElementById('weba-menu');
  const trigger = document.getElementById('main-menu');
  const header = document.querySelector('#messenger .sidebar-header');
  let picker;

  function apply(theme) {
    const def = THEMES.find(t => t[0] === theme) || THEMES[0];
    theme = def[0];
    if (ORIGINAL.includes(theme)) {
      root.removeAttribute('data-lx');
      try { localStorage.setItem('lchat-skin', theme); } catch (_) {}
      root.toggleAttribute('data-pixel-theme', theme === 'pixel');
      if (menu && header && menu.parentNode !== header) header.insertBefore(menu, header.querySelector('.pixel-search-toggle'));
    } else {
      root.setAttribute('data-lx', theme);
      root.removeAttribute('data-pixel-theme');
      if (menu && menu.parentNode !== document.body) document.body.append(menu);
    }
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta && def[2]) meta.setAttribute('content', def[2]);
    if (picker) for (const b of picker.querySelectorAll('button')) b.setAttribute('aria-pressed', String(b.dataset.lx === theme));
    syncScrim();
    if (typeof xpEnsureChrome === 'function') xpEnsureChrome(theme === 'xp');
    if (typeof vkEnsureChrome === 'function') vkEnsureChrome(theme === 'vk2010');
    if (LOG_THEMES.includes(theme)) {
      ensureMsgObserver();
      polishMessages();
    }
    if (typeof relabelClassic === 'function') relabelClassic();
  }
  function set(theme) {
    try { localStorage.setItem(KEY, theme); } catch (_) {}
    root.classList.add('lx-switching');
    apply(theme);
    setTimeout(() => root.classList.remove('lx-switching'), 450);
  }

  /* ---------- выдвижное меню (ЛК) ---------- */
  const scrim = document.createElement('div');
  scrim.className = 'lx-scrim';
  scrim.setAttribute('aria-hidden', 'true');
  document.body.append(scrim);
  function isOpen() { return menu && !menu.hidden; }
  function close() {
    if (!isOpen()) return;
    menu.hidden = true;
    trigger?.setAttribute('aria-expanded', 'false');
  }
  function open() {
    if (!menu || isOpen()) return;
    trigger ? trigger.click() : (menu.hidden = false);
  }
  function syncScrim() { scrim.classList.toggle('lx-on', Boolean(root.hasAttribute('data-lx') && isOpen())); }
  scrim.addEventListener('click', close);

  if (menu) {
    const head = document.createElement('div');
    head.className = 'lx-drawer-head';
    head.innerHTML = '<button type="button" class="lx-me" aria-label="Мой профиль"><span class="lx-me-avatar avatar"></span><span class="lx-me-text"><b class="lx-me-name">LChat</b><small class="lx-me-sub">Сообщения и звонки</small></span><span class="lx-me-chev" aria-hidden="true">›</span></button>';
    const meBtn = head.querySelector('.lx-me');
    meBtn.addEventListener('click', () => { close(); document.getElementById('profile-button')?.click(); });

    picker = document.createElement('div');
    picker.className = 'lx-themes';
    picker.setAttribute('role', 'group');
    picker.setAttribute('aria-label', 'Тема оформления');
    const title = document.createElement('div');
    title.className = 'lx-themes-title';
    title.textContent = 'Тема оформления';
    const row = document.createElement('div');
    row.className = 'lx-themes-row';
    for (const [id, label] of THEMES) {
      const b = document.createElement('button');
      b.type = 'button';
      b.dataset.lx = id;
      b.innerHTML = '<i class="lx-swatch lx-swatch-' + id + '" aria-hidden="true"></i><span></span>';
      b.querySelector('span').textContent = label;
      b.addEventListener('click', event => { event.stopPropagation(); set(id); });
      row.append(b);
    }
    picker.append(title, row);
    head.append(picker);
    menu.prepend(head);

    const fillUser = user => {
      if (!user) return;
      head.querySelector('.lx-me-name').textContent = user.name || 'LChat';
      const sub = [user.username ? '@' + user.username : '', user.phone || ''].filter(Boolean).join(' · ');
      head.querySelector('.lx-me-sub').textContent = sub || 'Мой профиль';
      const av = head.querySelector('.lx-me-avatar');
      av.className = 'lx-me-avatar avatar ' + ['violet', 'mint', 'peach', 'blue'][Number(user.id) % 4];
      av.textContent = (user.name || '?').trim().slice(0, 1).toUpperCase();
      if (Number(user.id) > 0) {
        const img = document.createElement('img');
        img.alt = '';
        img.src = 'avatar.php?id=' + encodeURIComponent(user.id);
        img.onerror = () => img.remove();
        av.append(img);
      }
    };
    document.addEventListener('lchat-user-ready', e => fillUser(e.detail?.user));
    const pb = document.getElementById('profile-button');
    if (pb && pb.title && pb.title !== 'Профиль') fillUser({name: pb.title});

    new MutationObserver(() => {
      syncScrim();
      if (isOpen() && root.hasAttribute('data-lx')) menu.scrollTop = 0;
    }).observe(menu, {attributes: true, attributeFilter: ['hidden']});
    const xpMenuObs = new MutationObserver(() => {
      if (xpMenuBusy || root.getAttribute('data-lx') !== 'xp') return;
      xpArrangeMenu(true);
    });
    xpMenuObs.observe(menu, {childList: true});
    menu.addEventListener('click', event => {
      if (!root.hasAttribute('data-lx')) return;
      const item = event.target.closest('a[href], .pixel-menu-action');
      if (item) setTimeout(close, 0);
    });
    document.addEventListener('keydown', event => { if (event.key === 'Escape' && root.hasAttribute('data-lx')) close(); });

    /* свайп от левого края — открыть, свайп влево по меню — закрыть */
    let sx = 0, sy = 0, mode = '';
    document.addEventListener('touchstart', event => {
      if (!root.hasAttribute('data-lx') || event.touches.length !== 1) return;
      const t = event.touches[0];
      sx = t.clientX; sy = t.clientY; mode = '';
      const messenger = document.getElementById('messenger');
      const chatOpen = messenger?.classList.contains('chat-open') && innerWidth <= 760;
      if (isOpen() && (menu.contains(event.target) || event.target === scrim)) mode = 'close';
      else if (!isOpen() && sx < 28 && !chatOpen && messenger && !messenger.hidden) mode = 'open';
    }, {passive: true});
    document.addEventListener('touchend', event => {
      if (!mode) return;
      const t = event.changedTouches[0];
      const dx = t.clientX - sx, dy = Math.abs(t.clientY - sy);
      if (mode === 'open' && dx > 60 && dy < 60) open();
      if (mode === 'close' && dx < -60 && dy < 60) close();
      mode = '';
    }, {passive: true});
  }

  /* мягкое «нажатие» для кнопок отправки */
  document.getElementById('composer')?.addEventListener('submit', () => {
    const send = document.querySelector('#composer .send-button');
    if (!send || !root.hasAttribute('data-lx')) return;
    send.classList.remove('lx-sent'); void send.offsetWidth; send.classList.add('lx-sent');
  });

  /* родной переключатель «Пиксель / LChatClassic» в меню сайта остаётся рабочим и синхронизируется с нашим */
  new MutationObserver(() => {
    if (root.hasAttribute('data-lx')) return;
    const now = root.hasAttribute('data-pixel-theme') ? 'pixel' : 'telegram';
    try { localStorage.setItem(KEY, now); } catch (_) {}
    if (picker) for (const b of picker.querySelectorAll('button')) b.setAttribute('aria-pressed', String(b.dataset.lx === now));
  }).observe(root, {attributes: true, attributeFilter: ['data-pixel-theme']});


  /* ---------- Windows XP chrome (только при data-lx=xp, снимается при смене темы) ---------- */
  const XP_RIGHT = /settings-button|notification-button|rewards\.php|pay\.php|privacy\.php|terms\.php|rules\.php|application\.php|docs\.php|github|Конфиденциальность|Соглашение|Правила|Настройки|звуки|Premium|Документация|Исходный|Приложение|Appearance|Sounds/i;
  let xpClockTimer = 0;
  let xpMenuBusy = false;
  const flagHtml = '<i class="lx-xp-flag" aria-hidden="true"><span></span><span></span><span></span><span></span></i>';

  function xpCaption(handlers) {
    const wrap = document.createElement('div');
    wrap.className = 'lx-xp-caption';
    const mk = (cls, label, fn) => {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'lx-xp-cap ' + cls;
      b.title = label;
      b.setAttribute('aria-label', label);
      b.addEventListener('click', event => { event.preventDefault(); event.stopPropagation(); fn && fn(event); });
      return b;
    };
    wrap.append(mk('lx-xp-min', 'Свернуть', handlers.min), mk('lx-xp-max', 'Развернуть', handlers.max), mk('lx-xp-close', 'Закрыть', handlers.close));
    return wrap;
  }
  function xpLogout() {
    close();
    const pb = document.getElementById('profile-button');
    pb && pb.click();
    const tryClick = () => {
      const modal = document.getElementById('modal');
      if (!modal) return false;
      const b = [...modal.querySelectorAll('button')].find(x => /Выйти из аккаунта|Log out|Выход/i.test(x.textContent || ''));
      if (b) { b.click(); return true; }
      return false;
    };
    if (tryClick()) return;
    const obs = new MutationObserver(() => { if (tryClick()) obs.disconnect(); });
    obs.observe(document.body, { childList: true, subtree: true });
    setTimeout(() => obs.disconnect(), 2000);
  }
  function xpIsRight(el) {
    const t = [el.getAttribute('data-proxy-target') || '', el.getAttribute('href') || '', el.textContent || ''].join(' ');
    return XP_RIGHT.test(t);
  }
  function xpArrangeMenu(on) {
    const menuEl = document.getElementById('weba-menu');
    if (!menuEl || xpMenuBusy) return;
    const head = menuEl.querySelector('.lx-drawer-head');
    const picker = menuEl.querySelector('.lx-themes');
    const orange = menuEl.querySelector('.lx-xp-orange');
    const cols = menuEl.querySelector('.lx-xp-cols');
    const left = menuEl.querySelector('.lx-xp-col-left');
    const right = menuEl.querySelector('.lx-xp-col-right');
    const foot = menuEl.querySelector('.lx-xp-foot');
    const restore = () => {
      xpMenuBusy = true;
      if (picker && head && picker.parentNode !== head) head.append(picker);
      if (left) while (left.firstChild) menuEl.insertBefore(left.firstChild, cols || foot || null);
      if (right) while (right.firstChild) menuEl.insertBefore(right.firstChild, cols || foot || null);
      cols && cols.remove();
      orange && orange.remove();
      foot && foot.remove();
      setTimeout(() => { xpMenuBusy = false; }, 0);
    };
    if (!on) { restore(); return; }
    xpMenuBusy = true;
    let or = orange, cs = cols, lf = left, rt = right, ft = foot;
    if (!or) { or = document.createElement('div'); or.className = 'lx-xp-orange'; or.setAttribute('aria-hidden', 'true'); }
    if (!cs) {
      cs = document.createElement('div'); cs.className = 'lx-xp-cols';
      lf = document.createElement('div'); lf.className = 'lx-xp-col-left';
      rt = document.createElement('div'); rt.className = 'lx-xp-col-right';
      cs.append(lf, rt);
    }
    if (!ft) {
      ft = document.createElement('div'); ft.className = 'lx-xp-foot';
      const exit = document.createElement('button');
      exit.type = 'button';
      exit.className = 'lx-xp-logoff';
      exit.innerHTML = '<i aria-hidden="true"></i><span>Выход</span>';
      exit.addEventListener('click', event => { event.stopPropagation(); xpLogout(); });
      ft.append(exit);
    }
    if (head) head.after(or); else menuEl.prepend(or);
    or.after(cs);
    menuEl.append(ft);
    if (picker) rt.prepend(picker);
    const hold = new Set([head, or, cs, ft, lf, rt, picker]);
    [...menuEl.children].forEach(el => {
      if (hold.has(el) || el.classList.contains('weba-menu-brand') || el.classList.contains('pixel-appearance') || el.classList.contains('lx-drawer-head')) return;
      (xpIsRight(el) ? rt : lf).append(el);
    });
    setTimeout(() => { xpMenuBusy = false; }, 0);
  }
  function xpMoveSearch(on) {
    const box = document.querySelector('#messenger .search-box');
    const head = document.querySelector('#messenger .sidebar-header');
    const side = document.querySelector('#messenger .sidebar');
    if (!box || !head || !side) return;
    if (on) {
      const tabs = side.querySelector('.sidebar-tabs, .sidebar-quick');
      if (box.parentNode === head || box.previousElementSibling === head || box.parentNode !== side) {
        if (tabs) side.insertBefore(box, tabs);
        else head.after(box);
      }
    }
    /* при уходе с темы не возвращаем поиск: weba-ui сам кладёт его в шапку; вернём рядом с меню */
    if (!on && box.parentNode === side && head) {
      const menuBtn = document.getElementById('main-menu');
      if (menuBtn) menuBtn.after(box);
    }
  }
  function xpEnsureChrome(on) {
    if (!on) {
      document.querySelectorAll('.lx-xp-caption, .lx-xp-taskbar, .lx-xp-start-mob').forEach(el => el.remove());
      if (xpClockTimer) { clearInterval(xpClockTimer); xpClockTimer = 0; }
      document.getElementById('messenger')?.classList.remove('lx-xp-max');
      xpArrangeMenu(false);
      xpMoveSearch(false);
      return;
    }
    const sideHead = document.querySelector('#messenger .sidebar-header');
    if (sideHead && !sideHead.querySelector('.lx-xp-caption')) {
      sideHead.append(xpCaption({ min: () => {}, max: () => {}, close: () => {} }));
    }
    const chatHead = document.querySelector('.chat-header');
    if (chatHead && !chatHead.querySelector('.lx-xp-caption')) {
      chatHead.append(xpCaption({
        min: () => document.getElementById('mobile-back')?.click(),
        max: () => document.getElementById('messenger')?.classList.toggle('lx-xp-max'),
        close: () => document.getElementById('mobile-back')?.click()
      }));
    }
    if (!document.querySelector('.lx-xp-taskbar')) {
      const bar = document.createElement('div');
      bar.className = 'lx-xp-taskbar';
      bar.innerHTML = '<button type="button" class="lx-xp-start">' + flagHtml + '<span>Пуск</span></button><button type="button" class="lx-xp-task">LChat</button><div class="lx-xp-tray"><span class="lx-xp-clock"></span></div>';
      document.body.append(bar);
      bar.querySelector('.lx-xp-start').addEventListener('click', event => {
        event.preventDefault();
        event.stopPropagation();
        isOpen() ? close() : open();
      });
      const clock = bar.querySelector('.lx-xp-clock');
      const tick = () => {
        try { clock.textContent = new Date().toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }); }
        catch (_) { clock.textContent = ''; }
      };
      tick();
      xpClockTimer = setInterval(tick, 15000);
    }
    const nav = document.querySelector('.mobile-nav');
    if (nav && !nav.querySelector('.lx-xp-start-mob')) {
      const b = document.createElement('button');
      b.type = 'button';
      b.className = 'mobile-nav-item lx-xp-start-mob';
      b.innerHTML = flagHtml + 'Пуск';
      b.addEventListener('click', event => {
        event.preventDefault();
        event.stopPropagation();
        isOpen() ? close() : open();
      });
      nav.prepend(b);
    }
    xpMoveSearch(true);
    xpArrangeMenu(true);
  }


  /* ---------- ВК 2010: подписи, шапка, строки сообщений ---------- */
  const VK_LABELS = {
    'Мой профиль': 'Моя Страница',
    'Новый разговор': 'Написать сообщение',
    'Контакты': 'Мои Друзья',
    'Мои боты': 'Мои Боты',
    'Настройки': 'Настройки',
    'Переключить звуки': 'Настройки уведомлений',
    'Поддержка': 'Помощь',
    'Подарки и тикеты': 'Подарки',
    'Тикеты и Premium': 'Premium',
    'Конфиденциальность': 'Приватность',
    'Соглашение': 'Соглашение',
    'Правила и возврат': 'Правила',
    'Приложение': 'Приложение',
    'Документация Bot API': 'Документация',
    'Исходный код': 'Исходный код'
  };
  const AV_TONES = ['violet','mint','peach','blue'];
  let msgObs = null;
  /* темы, где сообщения — «документ/лог»: имя автора над/перед текстом (только оформление) */
  const LOG_THEMES = ['vk2010', 'nocss', 'terminal', 'icq'];

  function labelKey(el) {
    return (el.textContent || '').replace(/^[^\wА-Яа-яЁё]+/, '').replace(/\s+/g, ' ').trim();
  }
  function hashName(name) {
    let h = 0;
    for (let i = 0; i < (name || '').length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0;
    return h;
  }
  function toneFor(name) { return AV_TONES[hashName(name) % AV_TONES.length]; }
  function meName() {
    return document.querySelector('.lx-me-name')?.textContent?.trim()
      || document.getElementById('profile-button')?.title
      || 'Я';
  }
  function peerName() {
    return document.getElementById('chat-title')?.textContent?.trim() || 'Собеседник';
  }
  function authorOf(row) {
    if (row.classList.contains('mine')) return meName();
    const existing = row.querySelector('.message-author');
    if (existing && existing.textContent.trim()) return existing.textContent.trim();
    return peerName();
  }
  function icqHeader(on) {
    const h = document.querySelector('#active-chat .chat-header, .chat-header');
    if (!h) return;
    if (!on) {
      if (h.hasAttribute('data-lx-online')) h.removeAttribute('data-lx-online');
      if (h.hasAttribute('data-lx-kind')) h.removeAttribute('data-lx-kind');
      return;
    }
    const sub = (document.getElementById('chat-subtitle')?.textContent || '').toLowerCase();
    const online = /в сети|online|печата|онлайн/.test(sub) ? '1' : '0';
    const kind = document.querySelector('.chat-row.selected')?.dataset.kind || 'direct';
    if (h.getAttribute('data-lx-online') !== online) h.setAttribute('data-lx-online', online);
    if (h.getAttribute('data-lx-kind') !== kind) h.setAttribute('data-lx-kind', kind);
  }
  function cleanupMessages() {
    icqHeader(false);
    document.querySelectorAll('.lx-msg-avatar, .lx-msg-author').forEach(el => el.remove());
    document.querySelectorAll('.lx-dc-follow, .lx-vk-unread').forEach(el => {
      el.classList.remove('lx-dc-follow', 'lx-vk-unread');
      el.style.removeProperty('--lx-dc-name');
    });
  }
  function polishMessages() {
    const theme = root.getAttribute('data-lx');
    if (!LOG_THEMES.includes(theme)) { cleanupMessages(); return; }
    icqHeader(theme === 'icq');
    const rows = [...document.querySelectorAll('#messages .message, #message-items .message, .messages .message')];
    rows.forEach(row => {
      const mine = row.classList.contains('mine');
      const name = theme === 'icq' && mine ? 'Я' : authorOf(row);
      const bubble = row.querySelector('.message-bubble');
      const own = row.querySelector('.lx-msg-author');
      if (own && mine && own.textContent !== name) own.textContent = name;
      if (bubble && !row.querySelector('.message-author')) {
        const a = document.createElement('div');
        a.className = 'message-author lx-msg-author';
        a.textContent = name;
        bubble.prepend(a);
      }
      if (theme === 'vk2010' && !row.querySelector(':scope > .avatar, :scope > .lx-msg-avatar')) {
        const av = document.createElement('span');
        av.className = 'avatar lx-msg-avatar ' + toneFor(name);
        av.textContent = (name || '?').trim().slice(0, 1).toUpperCase();
        row.prepend(av);
      }
    });
    if (theme === 'vk2010') {
      const badge = document.querySelector('.chat-row.selected .unread');
      const n = badge ? parseInt(badge.textContent, 10) || 0 : 0;
      const incoming = rows.filter(r => !r.classList.contains('mine'));
      incoming.forEach((el, i) => el.classList.toggle('lx-vk-unread', n > 0 && i >= incoming.length - n));
    }
    if (theme === 'nocss') {
      document.querySelectorAll('.avatar img').forEach(img => {
        const drop = () => img.remove();
        img.addEventListener('error', drop, {once: true});
        if (img.complete && img.naturalWidth === 0) drop();
      });
    }
  }
  function ensureMsgObserver() {
    if (msgObs) return;
    msgObs = new MutationObserver(() => polishMessages());
    const host = document.getElementById('messenger') || document.body;
    msgObs.observe(host, {childList: true, subtree: true});
  }

  function vkSetLabels(on) {
    const menuEl = document.getElementById('weba-menu');
    if (!menuEl) return;
    menuEl.querySelectorAll(':scope > button, :scope > a').forEach(el => {
      if (el.closest('.lx-drawer-head')) return;
      if (!on) {
        if (el.dataset.lxVkOrig != null) { el.innerHTML = el.dataset.lxVkOrig; delete el.dataset.lxVkOrig; }
        return;
      }
      if (el.dataset.lxVkOrig == null) el.dataset.lxVkOrig = el.innerHTML;
      const raw = (el.textContent || '').replace(/\s+/g, ' ').trim();
      const key = labelKey(el);
      const mapped = VK_LABELS[key] || VK_LABELS[raw] || null;
      if (mapped) el.textContent = mapped;
    });
  }
  function vkTitles(on) {
    const h1 = document.querySelector('.sidebar-header h1');
    if (h1) {
      if (on) {
        if (h1.dataset.lxOrig == null) h1.dataset.lxOrig = h1.textContent;
        h1.textContent = 'Мои сообщения';
      } else if (h1.dataset.lxOrig != null) {
        h1.textContent = h1.dataset.lxOrig;
        delete h1.dataset.lxOrig;
      }
    }
  }
  function vkIsMobile() { return window.matchMedia && window.matchMedia('(max-width:760px)').matches; }
  function vkMoveSearch(on) {
    const box = document.querySelector('#messenger .search-box');
    const head = document.querySelector('#messenger .sidebar-header');
    const slot = document.querySelector('.lx-vk-search-slot');
    const side = document.querySelector('#messenger .sidebar');
    if (!box || !head) return;
    const backToHead = () => {
      const menuBtn = document.getElementById('main-menu');
      if (menuBtn) menuBtn.after(box);
      else head.append(box);
    };
    if (!on) {
      if (box.closest('.lx-vk-topbar') || (side && box.parentNode === side)) backToHead();
      return;
    }
    if (vkIsMobile()) {
      const tabs = side && side.querySelector('.sidebar-tabs, .sidebar-quick');
      if (side && tabs) side.insertBefore(box, tabs);
      else if (side) head.after(box);
      else backToHead();
    } else if (slot) {
      slot.append(box);
    }
  }
  window.addEventListener('resize', () => {
    if (root.getAttribute('data-lx') === 'vk2010') vkMoveSearch(true);
  });
  function vkEnsureChrome(on) {
    ensureMsgObserver();
    if (!on) {
      vkSetLabels(false);
      vkTitles(false);
      vkMoveSearch(false);
      document.querySelector('.lx-vk-topbar')?.remove();
      polishMessages();
      return;
    }
    vkTitles(true);
    vkSetLabels(true);
    if (!document.querySelector('.lx-vk-topbar')) {
      const bar = document.createElement('div');
      bar.className = 'lx-vk-topbar';
      bar.innerHTML = '<button type="button" class="lx-vk-burger" aria-label="Меню"><span></span><span></span><span></span></button><span class="lx-vk-logo">LChat</span><div class="lx-vk-search-slot"></div><button type="button" class="lx-vk-who"></button>';
      document.body.append(bar);
      bar.querySelector('.lx-vk-burger').addEventListener('click', event => {
        event.preventDefault();
        event.stopPropagation();
        isOpen() ? close() : open();
      });
      const who = bar.querySelector('.lx-vk-who');
      const syncWho = () => { who.textContent = meName(); };
      syncWho();
      document.addEventListener('lchat-user-ready', syncWho);
      who.addEventListener('click', event => {
        event.preventDefault();
        close();
        document.getElementById('profile-button')?.click();
      });
    } else {
      const who = document.querySelector('.lx-vk-who');
      if (who) who.textContent = meName();
    }
    vkMoveSearch(true);
    polishMessages();
  }

  function relabelClassic() {
    document.querySelectorAll('button[data-skin="telegram"]').forEach(b => {
      if ((b.textContent || '').trim() !== 'LChatClassic') b.textContent = 'LChatClassic';
    });
  }
  if (menu) new MutationObserver(relabelClassic).observe(menu, {childList: true, subtree: true});

  apply(get());
  relabelClassic();
  window.LChatThemes = {set, get, list: THEMES.map(t => t[0])};
})();
