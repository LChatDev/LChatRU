/* LChat animated emoji picker. Message text remains plain text in the database. */
(() => {
  'use strict';
  const composer = document.getElementById('composer');
  const input = document.getElementById('message-input');
  const messages = document.getElementById('message-items');
  if (!composer || !input || !messages) return;
  const scriptUrl = document.currentScript?.src || new URL('assets/animated-emoji.js', document.baseURI).href;
  const base = new URL('animated-emoji/', scriptUrl).href;
  const categories = {'All':'Все','Smileys':'Смайлы','People':'Люди','Animals and Nature':'Природа','Food and Drink':'Еда','Activity':'Занятия','Travel and Places':'Места','Objects':'Предметы','Symbols':'Символы','Flags':'Флаги'};
  let entries = [], byId = new Map(), category = 'All', loadPromise;
  const button = document.createElement('button');
  button.type = 'button'; button.className = 'icon-button lce-toggle';
  button.title = 'Анимированные эмодзи'; button.setAttribute('aria-label', button.title);
  button.setAttribute('aria-expanded', 'false'); button.textContent = '☺';
  input.before(button);
  const panel = document.createElement('section'); panel.id = 'lce-panel'; panel.className = 'lce-panel'; panel.hidden = true;
  button.setAttribute('aria-controls', panel.id);
  panel.setAttribute('aria-label', 'Анимированные эмодзи');
  const header = document.createElement('div'); header.className = 'lce-header';
  const title = document.createElement('strong'); title.textContent = 'Анимированные эмодзи';
  const close = document.createElement('button'); close.type = 'button'; close.className = 'lce-close'; close.textContent = '×'; close.setAttribute('aria-label', 'Закрыть');
  header.append(title, close);
  const search = document.createElement('input'); search.type = 'search'; search.placeholder = 'Поиск по названию'; search.setAttribute('aria-label', search.placeholder);
  const tabs = document.createElement('div'); tabs.className = 'lce-tabs'; tabs.setAttribute('aria-label','Категории');
  const grid = document.createElement('div'); grid.className = 'lce-grid';
  const status = document.createElement('div'); status.className = 'lce-status'; status.setAttribute('aria-live','polite');
  const credit = document.createElement('a'); credit.className = 'lce-credit'; credit.href = 'https://github.com/Tarikul-Islam-Anik/Telegram-Animated-Emojis'; credit.target = '_blank'; credit.rel = 'noopener noreferrer'; credit.textContent = 'Источник анимаций';
  panel.append(header, search, tabs, grid, status, credit); document.body.append(panel);
  function image(item) { const img = document.createElement('img'); img.loading = 'lazy'; img.decoding = 'async'; img.alt = item.name; img.title = item.name; img.draggable = false; img.src = base + item.id + '.webp'; return img; }
  // Keep the existing textarea as the application's plain-text model, while editing
  // a contenteditable view so animated emoji can be seen before sending.
  function setupEditor() {
    const editor = document.createElement('div');
    editor.className = 'lce-editor'; editor.contentEditable = 'true'; editor.setAttribute('role', 'textbox');
    editor.setAttribute('aria-multiline', 'true'); editor.setAttribute('aria-label', input.getAttribute('aria-label') || 'Текст сообщения');
    editor.dataset.placeholder = input.placeholder; input.before(editor); input.hidden = true;
    const valueProperty = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value');
    let savedRange = null;
    const tokenPattern = /\[lce:([a-f0-9]{16})\]/g;
    function fragmentFor(text) {
      const f = document.createDocumentFragment(); let cursor = 0, m;
      function addText(value) { const parts = value.split('\n'); parts.forEach((part, i) => { if (i) f.append(document.createElement('br')); f.append(document.createTextNode(part)); }); }
      tokenPattern.lastIndex = 0;
      while ((m = tokenPattern.exec(text))) {
        const item = byId.get(m[1]); if (!item) continue;
        addText(text.slice(cursor, m.index));
        const img = image(item); img.className = 'lce-editor-emoji'; img.dataset.token = m[0]; img.contentEditable = 'false';
        f.append(img); cursor = m.index + m[0].length;
      }
      addText(text.slice(cursor)); return f;
    }
    function serialize(node) {
      if (node.nodeType === Node.TEXT_NODE) return node.nodeValue.replace(/\u00a0/g, ' ');
      if (node.nodeType !== Node.ELEMENT_NODE && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE) return '';
      if (node.nodeType === Node.ELEMENT_NODE && node.matches('img.lce-editor-emoji')) return node.dataset.token || '';
      if (node.nodeType === Node.ELEMENT_NODE && node.tagName === 'BR') return '\n';
      let result = '';
      for (const child of node.childNodes) {
        if (node === editor && child === node.lastChild && child.nodeType === Node.ELEMENT_NODE && child.tagName === 'BR' && child.previousSibling?.nodeName === 'BR') continue;
        if (child.nodeType === Node.ELEMENT_NODE && /^(DIV|P)$/.test(child.tagName) && result && !result.endsWith('\n')) result += '\n';
        result += serialize(child);
      }
      return result;
    }
    function updateEmpty(value) { editor.dataset.empty = value.length ? 'false' : 'true'; }
    function sync() { const value = serialize(editor); valueProperty.set.call(input, value); updateEmpty(value); input.dispatchEvent(new Event('input', {bubbles:true})); }
    function render(value) { editor.replaceChildren(fragmentFor(value)); savedRange = null; updateEmpty(value); }
    function currentRange() {
      const sel = window.getSelection();
      if (sel.rangeCount && editor.contains(sel.getRangeAt(0).commonAncestorContainer)) return sel.getRangeAt(0).cloneRange();
      if (savedRange && editor.contains(savedRange.commonAncestorContainer)) return savedRange.cloneRange();
      const range = document.createRange(); range.selectNodeContents(editor); range.collapse(false); return range;
    }
    function insert(text) {
      const range = currentRange(); range.deleteContents(); const content = fragmentFor(text); const end = content.lastChild;
      range.insertNode(content); if (end) { if (end.nodeType === Node.TEXT_NODE) range.setStart(end, end.length); else range.setStartAfter(end); range.collapse(true); }
      const selection = window.getSelection(); selection.removeAllRanges(); selection.addRange(range); savedRange = range.cloneRange();
      editor.focus(); sync();
    }
    Object.defineProperty(input, 'value', {configurable:true, get() { return valueProperty.get.call(input); }, set(v) { valueProperty.set.call(input, v); render(valueProperty.get.call(input)); }});
    input.focus = () => editor.focus();
    input.setRangeText = text => insert(String(text));
    editor.addEventListener('input', sync);
    editor.addEventListener('keydown', e => {
      if (e.key !== 'Enter' || e.isComposing) return;
      e.preventDefault(); if (e.shiftKey) document.execCommand('insertLineBreak'); else composer.requestSubmit();
    });
    editor.addEventListener('beforeinput', e => { if (e.inputType === 'insertParagraph' && !e.isComposing) { e.preventDefault(); composer.requestSubmit(); } });
    editor.addEventListener('paste', e => { const text = e.clipboardData?.getData('text/plain'); if (text !== undefined) { e.preventDefault(); insert(text); } });
    editor.addEventListener('copy', e => { const selection = window.getSelection(); if (!selection.rangeCount || !editor.contains(selection.getRangeAt(0).commonAncestorContainer)) return; e.clipboardData.setData('text/plain', serialize(selection.getRangeAt(0).cloneContents())); e.preventDefault(); });
    document.addEventListener('selectionchange', () => { const selection = window.getSelection(); if (selection.rangeCount && editor.contains(selection.getRangeAt(0).commonAncestorContainer)) savedRange = selection.getRangeAt(0).cloneRange(); });
    new MutationObserver(() => { editor.dataset.placeholder = input.placeholder; }).observe(input, {attributes:true, attributeFilter:['placeholder']});
    render(valueProperty.get.call(input));
  }
  setupEditor();
  function position() {
    const anchor = button.getBoundingClientRect(); const width = Math.min(innerWidth >= 761 ? 440 : 380, innerWidth - 16);
    panel.style.width = width + 'px'; panel.style.left = Math.max(8, Math.min(anchor.left, innerWidth - width - 8)) + 'px';
    const height = panel.offsetHeight;
    const above = anchor.top - height - 8, below = anchor.bottom + 8;
    panel.style.top = Math.max(8, Math.min(above >= 8 ? above : below, innerHeight - height - 8)) + 'px';
  }
  function hide() { panel.hidden = true; button.setAttribute('aria-expanded','false'); }
  function buildTabs() {
    tabs.replaceChildren(); for (const [key, label] of Object.entries(categories)) {
      const tab = document.createElement('button'); tab.type = 'button'; tab.textContent = label; tab.className = key === category ? 'active' : '';
      tab.addEventListener('click', () => { category = key; buildTabs(); renderGrid(); }); tabs.append(tab);
    }
  }
  function renderGrid() {
    const q = search.value.trim().toLocaleLowerCase();
    const filtered = entries.filter(item => (category === 'All' || item.category === category) && (!q || (item.name + ' ' + (categories[item.category] || '')).toLocaleLowerCase().includes(q)));
    const fragment = document.createDocumentFragment();
    for (const item of filtered) {
      const option = document.createElement('button'); option.type = 'button'; option.title = item.name; option.setAttribute('aria-label', item.name); option.append(image(item));
      option.addEventListener('click', () => {
        const start = input.selectionStart, end = input.selectionEnd;
        input.setRangeText('[lce:' + item.id + ']', start, end, 'end');
        input.dispatchEvent(new Event('input', {bubbles:true})); input.focus(); hide();
      }); fragment.append(option);
    }
    grid.replaceChildren(fragment); grid.scrollTop = 0;
    status.textContent = filtered.length ? `Найдено: ${filtered.length}` : 'Эмодзи не найдены';
    position();
  }
  function replaceText(root) {
    if (!byId.size || !root?.isConnected) return;
    const textNodes = [], walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    while (walker.nextNode()) { const n = walker.currentNode; if (n.nodeValue.includes('[lce:') && !n.parentElement.closest('a')) textNodes.push(n); }
    for (const n of textNodes) {
      const value = n.nodeValue, regex = /\[lce:([a-f0-9]{16})\]/g; let match, cursor = 0, replaced = false;
      const fragment = document.createDocumentFragment();
      while ((match = regex.exec(value))) {
        const item = byId.get(match[1]); if (!item) continue;
        fragment.append(document.createTextNode(value.slice(cursor, match.index)));
        const img = image(item); img.className = 'lce-inline'; fragment.append(img); cursor = match.index + match[0].length; replaced = true;
      }
      if (replaced) { fragment.append(document.createTextNode(value.slice(cursor))); n.replaceWith(fragment); }
    }
    const only = root.textContent.trim() === '' && root.querySelectorAll('.lce-inline').length > 0 && root.querySelectorAll('.lce-inline').length <= 3;
    root.classList.toggle('lce-only', only);
  }
  function scan(node) {
    const el = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
    if (!el) return;
    const enclosing = el.closest('.message-text'); if (enclosing) replaceText(enclosing);
    else el.querySelectorAll?.('.message-text').forEach(replaceText);
  }
  function load() {
    return loadPromise ||= fetch(new URL('emoji-catalog.js?v=20261107-2', scriptUrl), {credentials:'same-origin'}).then(r => {
      if (!r.ok) throw Error('Не удалось загрузить каталог'); return r.json();
    }).then(data => {
      entries = data.filter(x => /^[a-f0-9]{16}$/.test(x.id) && typeof x.name === 'string' && Object.hasOwn(categories, x.category));
      byId = new Map(entries.map(x => [x.id, x])); messages.querySelectorAll('.message-text').forEach(replaceText); return entries;
    }).catch(err => { loadPromise = null; throw err; });
  }
  button.addEventListener('click', async () => {
    if (!panel.hidden) { hide(); return; }
    panel.hidden = false; button.setAttribute('aria-expanded','true'); status.textContent = 'Загружаем каталог…'; position();
    try { await load(); buildTabs(); renderGrid(); } catch { status.textContent = 'Не удалось загрузить каталог. Попробуйте снова.'; }
  });
  close.addEventListener('click', hide); search.addEventListener('input', renderGrid);
  document.addEventListener('pointerdown', e => { if (!panel.hidden && !panel.contains(e.target) && !button.contains(e.target)) hide(); });
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && !panel.hidden) { hide(); button.focus(); } });
  window.addEventListener('resize', () => { if (!panel.hidden) position(); });
  window.addEventListener('scroll', () => { if (!panel.hidden) position(); }, true);
  new MutationObserver(records => { for (const record of records) { if (record.type === 'characterData') scan(record.target); for (const node of record.addedNodes) scan(node); } }).observe(messages, {subtree:true, childList:true, characterData:true});
  load().catch(() => {});
})();
