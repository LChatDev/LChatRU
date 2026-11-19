'use strict';
(() => {
  const sidebar = document.querySelector('#messenger > .sidebar');
  if (!sidebar || sidebar.querySelector('.mobile-nav')) return;
  const edition = document.currentScript.dataset.edition || 'full';
  const mobile = matchMedia('(max-width: 760px)');
  const english = document.documentElement.lang === 'en';
  const labels = {
    "Основная навигация": "Main navigation",
    "Чаты": "Chats",
    "Контакты": "Contacts",
    "Настройки": "Settings",
    "Профиль": "Profile",
    "Назад к чатам": "Back to chats",
    "Войдите в аккаунт.": "Sign in to your account.",
    "Найти человека": "Find someone",
    "По логину": "By username",
    "Пригласить друзей": "Invite friends",
    "Ссылка на LChat": "LChat link",
    "Ссылка скопирована": "Link copied",
    "Здесь появятся люди из ваших личных чатов.": "People from your direct chats will appear here.",
    "Чат не найден": "Chat not found",
    "В звонке": "In a call",
    "Не удалось загрузить контакты.": "Could not load contacts.",
    "Аккаунт и оформление": "Account and appearance",
    "Имя, описание, тема и цвет": "Name, bio, theme and color",
    "Звуки уведомлений": "Notification sounds",
    "Переключить звуки сообщений и звонков": "Toggle sounds for messages and calls",
    "Мои боты": "My bots",
    "Управление ботами": "Manage bots",
    "Платёжная система пока не подключена": "Payments are unavailable",
    "Подарки и тикеты": "Gifts and tickets",
    "Коллекция и отправка подарков": "Collect and send gifts",
    "Приложение": "App",
    "Версии для Android": "Android releases",
    "Поддержка": "Support",
    "Написать в поддержку": "Contact support",
    "Политика конфиденциальности": "Privacy policy",
    "Пользовательское соглашение": "Terms of use",
    "Документация Bot API": "Bot API documentation",
    "Выбрать фото": "Choose photo",
    "Изменить": "Edit",
    "Имя пользователя": "Username",
    "Подарки 🎁": "Gifts 🎁",
    "Загружаем подарки…": "Loading gifts…",
    "Открыть подарки и тикеты": "Open gifts and tickets",
    "Не удалось загрузить профиль.": "Could not load the profile.",
    "О себе": "Bio",
    "Показаны последние 100 подарков. Остальные — в коллекции.": "Showing the latest 100 gifts. See the collection for more.",
    "Не удалось загрузить подарки.": "Could not load gifts.",
    "непрочитанных": "unread",
    "У вас пока нет подарков.": "No gifts yet.",
  };
  const t = label => english ? (labels[label] ?? label) : label;
  const icons = {
    back: '<path d="M19 12H5m7-7-7 7 7 7"/>',
    chats: '<path d="M4 5.5A3.5 3.5 0 0 1 7.5 2h9A3.5 3.5 0 0 1 20 5.5v7a3.5 3.5 0 0 1-3.5 3.5H10l-4.5 3v-3.2A3.5 3.5 0 0 1 4 12.5z"/>',
    contacts: '<circle cx="12" cy="8" r="3.2"/><path d="M5.5 19.5c.2-3.5 2.7-5.5 6.5-5.5s6.3 2 6.5 5.5"/><circle cx="12" cy="12" r="10"/>',
    settings: '<path d="M9.8 2.8h4.4l.5 2a8 8 0 0 1 1.5.9l2-.6 2.2 3.8-1.5 1.5a8 8 0 0 1 0 1.8l1.5 1.5-2.2 3.8-2-.6a8 8 0 0 1-1.5.9l-.5 2H9.8l-.5-2a8 8 0 0 1-1.5-.9l-2 .6-2.2-3.8 1.5-1.5a8 8 0 0 1 0-1.8L3.6 8.9l2.2-3.8 2 .6a8 8 0 0 1 1.5-.9z"/><circle cx="12" cy="11.3" r="2.8"/>',
    profile: '<circle cx="12" cy="8" r="3.5"/><path d="M5.5 20c.2-4 2.8-6 6.5-6s6.3 2 6.5 6"/>'
  };
  const nav = document.createElement('nav'); nav.className = 'mobile-nav'; nav.setAttribute('aria-label', t('Основная навигация'));
  const buttons = {};
  for (const [id, label] of [['chats',t('Чаты')],['contacts',t('Контакты')],['settings',t('Настройки')],['profile',t('Профиль')]]) {
    const button=document.createElement('button'); button.type='button'; button.className='mobile-nav-item'; button.dataset.nav=id;
    const glyph=document.createElement('span'); glyph.className='mobile-nav-icon';
    glyph.innerHTML=`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons[id]}</svg>`;
    button.append(glyph,document.createTextNode(label)); nav.append(button); buttons[id]=button;
  }
  buttons.chats.classList.add('active'); buttons.chats.setAttribute('aria-current','page');
  const badge=document.createElement('span'); badge.className='mobile-nav-badge'; badge.hidden=true; buttons.chats.querySelector('.mobile-nav-icon').append(badge);
  const updateUnread=()=>{let count=0; document.querySelectorAll('#chat-list .unread').forEach(el=>{const n=parseInt(el.textContent,10); if(Number.isFinite(n))count+=n;}); badge.textContent=count>99?'99+':String(count);badge.hidden=!count; buttons.chats.setAttribute('aria-label',count?`${t('Чаты')}, ${t('непрочитанных')}: ${count>99?'99+':count}`:t('Чаты'));};
  const list=document.getElementById('chat-list'); if(list)new MutationObserver(updateUnread).observe(list,{childList:true,subtree:true,characterData:true}); updateUnread();
  const profile=document.getElementById('profile-button');
  const updateProfile=()=>{const slot=buttons.profile.querySelector('.mobile-nav-icon');slot.querySelector('img')?.remove();const photo=profile?.querySelector('img');if(photo){const image=photo.cloneNode();image.alt='';image.onerror=()=>image.remove();slot.append(image);}};
  if(profile)new MutationObserver(updateProfile).observe(profile,{childList:true,subtree:true,attributes:true,attributeFilter:['src']});updateProfile();
  const screen=document.createElement('section');screen.className='mobile-screen';screen.hidden=true;sidebar.append(screen);
  let active = 'chats', generation = 0;
  const covered = Array.from(sidebar.children).filter(child => child !== screen);
  function syncScreen() {
    const showing = mobile.matches && !screen.hidden;
    for (const child of covered) child.inert = showing;
  }
  mobile.addEventListener('change', syncScreen);
  function showChats(restoreFocus = false) {
    generation++;
    select('chats');
    screen.hidden = true;
    syncScreen();
    if (restoreFocus) buttons.chats.focus({preventScroll: true});
  }
  const el=(tag,cls,text)=>{const n=document.createElement(tag);if(cls)n.className=cls;if(text!=null)n.textContent=text;return n;};
  // Decorative icons share one stroke style; labels remain real text for assistive technology.
  const rowIcons={
    '👤':'<circle cx="12" cy="8" r="3.5"/><path d="M5 20c.2-4.2 2.8-6.2 7-6.2s6.8 2 7 6.2"/>',
    '🔔':'<path d="M18 8a6 6 0 0 0-12 0c0 7-2 7-2 8.5h16C20 15 18 15 18 8zM10 20h4"/>',
    '🤖':'<rect x="3" y="6" width="18" height="14" rx="4"/><path d="M12 6V3m-3 9h.01M15 12h.01M9 16h6"/>',
    '✦':'<path d="m12 2 2.7 6.1L21 11l-6.3 2.9L12 20l-2.7-6.1L3 11l6.3-2.9z"/>',
    '🎁':'<rect x="4" y="10" width="16" height="11" rx="2"/><path d="M3 7h18v3H3zm9 0v14M12 7C8 7 7 6 7 4.7a2 2 0 0 1 4-1L12 7zm0 0c4 0 5-1 5-2.3a2 2 0 0 0-4-1L12 7z"/>',
    '📱':'<rect x="6" y="2" width="12" height="20" rx="2.5"/><path d="M10 5h4M11 19h2"/>',
    '💬':'<path d="M4 5.5A3.5 3.5 0 0 1 7.5 2h9A3.5 3.5 0 0 1 20 5.5v7a3.5 3.5 0 0 1-3.5 3.5H10l-4.5 3v-3.2A3.5 3.5 0 0 1 4 12.5z"/>',
    '🔒':'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 1 1 8 0v3M12 14v3"/>',
    '📄':'<path d="M6 3h8l4 4v14H6zM14 3v5h4M9 12h6M9 16h6"/>',
    '⌘':'<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 7h8M8 11h8M8 15h5"/>',
    '＋':'<circle cx="11" cy="11" r="6"/><path d="m16 16 4 4M11 8v6M8 11h6"/>',
    '↗':'<path d="M12 16V3m-4 4 4-4 4 4M5 14v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5"/>',
    '📷':'<path d="M4 7h3l2-3h6l2 3h3v12H4z"/><circle cx="12" cy="13" r="3.2"/>',
    '✎':'<path d="m4 20 4-.8L19 8.2 15.8 5 4.8 16zM14.5 6.3l3.2 3.2"/>',
    '⚙':'<path d="M9.8 2.8h4.4l.5 2a8 8 0 0 1 1.5.9l2-.6 2.2 3.8-1.5 1.5a8 8 0 0 1 0 1.8l1.5 1.5-2.2 3.8-2-.6a8 8 0 0 1-1.5.9l-.5 2H9.8l-.5-2a8 8 0 0 1-1.5-.9l-2 .6-2.2-3.8 1.5-1.5a8 8 0 0 1 0-1.8L3.6 8.9l2.2-3.8 2 .6a8 8 0 0 1 1.5-.9z"/><circle cx="12" cy="11.3" r="2.8"/>'
  };
  function rowIcon(symbol){const icon=el('span','mobile-row-icon');if(rowIcons[symbol]){icon.innerHTML=`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${rowIcons[symbol]}</svg>`;}else icon.textContent=symbol;return icon;}
  function select(id){active=id;for(const [name,button] of Object.entries(buttons)){button.classList.toggle('active',name===id);if(name===id)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');}}
  function row(symbol,title,subtitle,action){const b=el('button','mobile-screen-row');b.type='button';const icon=rowIcon(symbol),info=el('span','mobile-row-info');info.append(el('strong','',title));if(subtitle)info.append(el('small','',subtitle));b.append(icon,info,el('span','mobile-row-chevron','›'));b.addEventListener('click',action);return b;}
  function card(){return el('div','mobile-screen-card');}
  function navigate(url){location.href=url;}
  function modalTarget(id){document.getElementById(id)?.click();}
  async function showScreen(id){
    const serial = ++generation;
    select(id);
    screen.hidden = false;
    screen.dataset.screen = id;
    screen.replaceChildren();
    screen.scrollTop = 0;
    syncScreen();

    const heading = el('header', 'mobile-screen-heading');
    const back = el('button', 'mobile-screen-back');
    back.type = 'button';
    back.setAttribute('aria-label', t('Назад к чатам'));
    back.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${icons.back}</svg>`;
    back.addEventListener('click', () => showChats(true));
    const title = el('h2', 'mobile-screen-title', id === 'contacts' ? t('Контакты') : id === 'settings' ? t('Настройки') : t('Профиль'));
    title.id = 'mobile-screen-title';
    screen.setAttribute('aria-labelledby', title.id);
    heading.append(back, title);
    const contentBody = el('div', 'mobile-screen-enter');
    screen.append(heading, contentBody);
    const app=window.LChat,user=app?.getUser();if(!app||!user){contentBody.append(el('p','mobile-screen-note',t('Войдите в аккаунт.')));return;}
    if(id==='contacts'){
      const shortcuts=card();shortcuts.append(row('＋',t('Найти человека'),t('По логину'),()=>modalTarget('new-chat')),row('↗',t('Пригласить друзей'),t('Ссылка на LChat'),async()=>{try{await navigator.clipboard.writeText(location.origin+'/');app.toast(t('Ссылка скопирована'));}catch{app.modal(t('Ссылка на LChat'),body=>{const input=el('input');input.value=location.origin+'/';input.readOnly=true;body.append(input);});}}));contentBody.append(shortcuts);
      const content=card();content.append(el('h3','mobile-card-title',t('Контакты')));contentBody.append(content);
      try {const data=await app.api('contacts');if(serial!==generation)return;const contacts=data.contacts||[];if(!contacts.length)content.append(el('p','mobile-screen-note',t('Здесь появятся люди из ваших личных чатов.')));for(const person of contacts){const entry=row('','', '',async()=>{try{await app.refreshChats();const chat=app.getChats().find(c=>Number(c.id)===Number(person.chat_id));if(!chat)throw new Error(t('Чат не найден'));showChats();await app.selectChat(chat);}catch(e){app.report(e);}});entry.querySelector('.mobile-row-icon').replaceWith(app.avatar(person.name,person.id));const info=entry.querySelector('.mobile-row-info');info.replaceChildren(el('strong','',person.name),el('small','',person.in_call?t('В звонке'):'@'+person.username));content.append(entry);}}catch(e){if(serial===generation)content.append(el('p','mobile-screen-note',e.message||t('Не удалось загрузить контакты.')));}return;
    }
    const hero=el('div','mobile-screen-hero');hero.append(app.avatar(user.name,user.id),el('h2','',user.name),el('p','mobile-screen-note','@'+user.username));contentBody.append(hero);
    if(id==='settings'){
      const settings=card();settings.append(row('👤',t('Аккаунт и оформление'),t('Имя, описание, тема и цвет'),()=>modalTarget('settings-button')),row('🔔',t('Звуки уведомлений'),t('Переключить звуки сообщений и звонков'),()=>modalTarget('notification-button')),row('🤖',t('Мои боты'),t('Управление ботами'),()=>modalTarget('bots-button')));contentBody.append(settings);
      const extras = card();
      if (edition === 'full') extras.append(row('✦','LChat Premium',t('Платёжная система пока не подключена'),()=>navigate('premium.php')));
      if (edition !== 'web') extras.append(row('🎁',t('Подарки и тикеты'),t('Коллекция и отправка подарков'),()=>navigate('rewards.php')));
      if (edition === 'full') extras.append(row('📱',t('Приложение'),t('Версии для Android'),()=>navigate('application.php')));
      if (extras.childElementCount) contentBody.append(extras);
      const help = card();
      if (edition === 'full') help.append(row('💬',t('Поддержка'),t('Написать в поддержку'),()=>navigate('support.php')),row('🔒',t('Политика конфиденциальности'),'',()=>navigate('privacy.php')),row('📄',t('Пользовательское соглашение'),'',()=>navigate('terms.php')));
      help.append(row('⌘',t('Документация Bot API'),'',()=>navigate('docs.php')));
      contentBody.append(help);
      return;
    }
    const actions=el('div','mobile-profile-actions');for(const [symbol,label,target] of [['📷',t('Выбрать фото'),'profile-button'],['✎',t('Изменить'),'settings-button'],['⚙',t('Настройки'),'settings-button']]){const b=el('button','mobile-profile-action');b.type='button';b.append(rowIcon(symbol),el('small','',label));b.addEventListener('click',()=>modalTarget(target));actions.append(b);}contentBody.append(actions);
    const info=card();info.append(el('div','mobile-profile-value','@'+user.username),el('small','mobile-profile-label',t('Имя пользователя')));contentBody.append(info);
    if (edition === 'web') return;
    const gifts=card();gifts.append(el('h3','mobile-card-title',t('Подарки 🎁')),el('p','mobile-screen-note',t('Загружаем подарки…')));contentBody.append(gifts);
    const open=el('button','mobile-gifts-open',t('Открыть подарки и тикеты'));open.type='button';open.onclick=()=>navigate('rewards.php');contentBody.append(open);
    try{const url=new URL('user-profile.php',location.href);url.searchParams.set('id',user.id);const response=await fetch(url,{credentials:'same-origin',cache:'no-store'});const data=await response.json();if(!response.ok)throw new Error(data.error||t('Не удалось загрузить профиль.'));if(serial!==generation)return;const p=data.profile;if(p.bio){info.prepend(el('small','mobile-profile-label',t('О себе')));info.prepend(el('div','mobile-profile-value',p.bio));}if(Number(p.verified))hero.querySelector('h2').append(el('span','mobile-profile-verified',' ✓'));gifts.replaceChildren(el('h3','mobile-card-title',t('Подарки 🎁')));if(!data.gifts?.length)gifts.append(el('p','mobile-screen-note',t('У вас пока нет подарков.')));else{const grid=el('div','mobile-gift-grid');for(const gift of data.gifts){const item=el('div','mobile-gift-item'),art=el('span','',gift.emoji);if(/^gift_[0-9]{19}$/.test(gift.tgs||'')){art.textContent='';art.classList.add('gift-tgs');art.dataset.tgs=gift.tgs;const image=el('img','tgs-preview');image.src='/assets/lchat-gifts/webp/'+gift.tgs+'.webp';image.alt=gift.name;image.loading='lazy';art.append(image);}else if(Number.isInteger(gift.sprite)&&gift.sprite>=0&&gift.sprite<9){art.textContent='';art.classList.add('profile-sprite');art.dataset.sprite=gift.sprite;art.dataset.variant=gift.variant;}item.append(art,el('strong','',gift.name),el('small','',gift.fingerprint?'#'+gift.fingerprint.slice(0,12):'× '+gift.count));if(gift.fingerprint)window.LChatGiftArt?.decorate(item,gift.fingerprint);grid.append(item);}gifts.append(grid);if(data.has_more)gifts.append(el('p','mobile-screen-note',t('Показаны последние 100 подарков. Остальные — в коллекции.')));}}catch(e){if(serial===generation)gifts.lastElementChild.textContent=e.message||t('Не удалось загрузить подарки.');}
  }
  buttons.chats.addEventListener('click',()=>{showChats();const modal=document.getElementById('modal');if(modal?.open)modal.close();const panel=document.querySelector('.community-panel[open]');if(panel?.close)panel.close();});
  for(const id of ['contacts','settings','profile'])buttons[id].addEventListener('click',()=>showScreen(id));
  document.getElementById('modal')?.addEventListener('close',()=>{if(active!=='chats')showScreen(active);});
  sidebar.append(nav);
})();
