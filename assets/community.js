'use strict';
(() => {
  const app = window.LChat;
  if (!app) return;
  const {api, node, button, avatar, toast, report, modal} = app;
  const ru = document.documentElement.lang === 'ru';
  const tr = (a, b) => ru ? a : b;
  const nonce = () => Array.from(crypto.getRandomValues(new Uint8Array(16)), b=>b.toString(16).padStart(2,'0')).join('');
  const modalElement = document.getElementById('modal');
  let panelVersion = 0;
  modalElement.addEventListener('close', () => { panelVersion++; modalElement.classList.remove('community-panel'); });
  async function run(control, task) {
    control.disabled = true;
    try { await task(); } catch (e) { report(e); } finally { control.disabled = false; }
  }
  function action(text, task, style = 'community-action') {
    const control = button(text, style, () => run(control, task));
    return control;
  }
  async function copy(text) {
    try { await navigator.clipboard.writeText(text); toast(tr('Ссылка скопирована', 'Link copied')); }
    catch { modal(tr('Ссылка', 'Link'), body => { const input = node('input'); input.value = text; input.readOnly = true; body.append(input); input.select(); }); }
  }
  function link(chat) {
    return new URL(chat.kind === 'channel' ? encodeURIComponent(chat.channel_slug) : 'index.php?invite=' + encodeURIComponent(chat.invite_token), location.href).href;
  }
  async function details() {
    const c = app.getSelected();
    if (!c) return;
    const version = ++panelVersion;
    modal(tr('Информация', 'Chat info'), body => body.append(node('p', 'muted', tr('Загрузка…', 'Loading…'))));
    modalElement.classList.add('community-panel');
    try {
      const data = await api('chat.info', {chat_id:c.id});
      if (version !== panelVersion || !modalElement.open || app.getSelected()?.id !== c.id) return;
      const body = document.getElementById('modal-body'); body.replaceChildren();
      const owner = c.role === 'owner';
      const hero = node('div', 'community-hero');
      hero.append(avatar(c.title, c.peer?.id || c.id, c.kind !== 'direct'), node('h2', '', c.title));
      hero.append(node('p', 'muted', c.kind === 'direct' ? '@' + (c.peer?.username || '') : (data.members?.length || c.member_count || 0) + ' ' + tr(c.kind === 'channel' ? 'подписчиков' : 'участников', c.kind === 'channel' ? 'subscribers' : 'members')));
      body.append(hero);
      if (c.kind !== 'direct') {
        const share = node('div', 'community-link');
        share.append(node('small','muted',tr('Ссылка-приглашение','Invite link')), node('span','',link(c)), action(tr('Скопировать ссылку','Copy link'), () => copy(link(c))));
        body.append(share);
        body.append(action(tr('Пригласить контакт','Invite a contact'),async()=>{
          const contacts=await api('contacts');panelVersion++;modalElement.classList.remove('community-panel');
          modal(tr('Пригласить контакт','Invite a contact'),body=>{
            for(const person of contacts.contacts||[]){const invitationNonce=nonce();body.append(action(person.name+' · @'+person.username,async()=>{
              if(c.kind==='channel')await api('message.send',{chat_id:person.chat_id,body:link(c),nonce:invitationNonce},true);
              else await api('invite.send',{kind:'group',token:c.invite_token,user_ids:[Number(person.id)],nonce:invitationNonce},true);
              toast(tr('Приглашение отправлено','Invitation sent'));modalElement.close();
            }));}
            if(!contacts.contacts?.length)body.append(node('p','muted',tr('Сначала начните личный чат с контактом.','Start a direct chat with a contact first.')));
          });
        }));
        if (owner) {
          const form = node('form','community-edit'), label = node('label','',tr('Название','Name')), title = node('input');
          title.value = c.title; title.required = true; title.minLength = 2; title.maxLength = 80; label.append(title); form.append(label);
          const save = button(tr('Сохранить','Save'),'primary',()=>{}); save.type = 'submit'; form.append(save);
          form.onsubmit = e => {e.preventDefault();run(save, async()=>{await api('chat.update',{chat_id:c.id,title:title.value},true);await app.refreshChats();await details();toast(tr('Сохранено','Saved'));});}; body.append(form);
          if(c.kind === 'channel') {
            const form = node('form','community-edit'), label=node('label','',tr('Адрес канала','Channel address')), slug=node('input');
            slug.value=c.channel_slug;slug.required=true;label.append(slug);form.append(label);
            const save=button(tr('Изменить адрес','Change address'),'button',()=>{});save.type='submit';form.append(save);
            form.onsubmit=e=>{e.preventDefault();run(save,async()=>{await api('channel.update',{chat_id:c.id,slug:slug.value},true);await app.refreshChats();await details();});};body.append(form);
          } else body.append(action(tr('Отозвать старую ссылку','Revoke invite link'),async()=>{
            if(!confirm(tr('Старая ссылка перестанет работать. Продолжить?','The old link will stop working. Continue?')))return;
            await api('group.invite-reset',{chat_id:c.id},true);await app.refreshChats();await details();
          }));
        } else body.append(action(tr(c.kind==='channel'?'Отписаться':'Выйти из группы',c.kind==='channel'?'Unsubscribe':'Leave group'), async()=>{
          if(!confirm(tr('Выйти из этого чата?','Leave this chat?')))return;
          await api(c.kind==='channel'?'channel.leave':'group.leave',{chat_id:c.id},true);modalElement.close();document.getElementById('mobile-back').click();await app.refreshChats();
        },'community-action danger-text'));
      }
      const tabs=node('div','community-tabs'); const members=node('section','community-members'), files=node('section','community-files'); files.hidden=true;
      const membersButton=button(tr('Участники','Members'),'active',()=>{members.hidden=false;files.hidden=true;membersButton.classList.add('active');filesButton.classList.remove('active');});
      const filesButton=button(tr('Файлы','Files'),'',()=>{members.hidden=true;files.hidden=false;filesButton.classList.add('active');membersButton.classList.remove('active');});tabs.append(membersButton,filesButton);body.append(tabs,members,files);
      const find=node('input');find.placeholder=tr('Поиск участников','Search members');find.setAttribute('aria-label',find.placeholder);members.append(find);
      const rows=node('div');members.append(rows);
      const render=()=>{rows.replaceChildren();for(const p of data.members||[]){if(!(p.name+' '+p.username).toLowerCase().includes(find.value.toLowerCase()))continue;
        const row=node('div','person-row');row.append(avatar(p.name,p.id));const info=node('div');info.append(node('b','',p.name),node('small','muted','@'+p.username+(p.role==='owner'?tr(' · владелец',' · owner'):'')));row.append(info);
        if(owner&&Number(p.id)!==Number(app.getUser().id)&&c.kind!=='direct') row.append(action(tr('Удалить','Remove'),async()=>{if(!confirm(tr('Исключить участника?','Remove this member?')))return;await api(c.kind==='channel'?'channel.remove':'group.remove',{chat_id:c.id,user_id:p.id},true);await app.refreshChats();await details();},'remove-button'));
        rows.append(row);
      }};find.addEventListener('input',render);render();
      files.append(node('p','muted small',tr('Файлы из загруженных сообщений','Files from loaded messages')));
      for(const m of app.getMessages().filter(m=>m.file_name)){const a=node('a','community-file',m.file_name);a.href='download.php?id='+encodeURIComponent(m.id);a.append(node('small','muted',((m.file_size||0)/1048576).toFixed(2)+' MB'));files.append(a);}
      if(files.children.length===1)files.append(node('p','muted',tr('Файлов пока нет','No files yet')));
    } catch(e){if(version===panelVersion)report(e);}
  }
  for(const id of ['chat-info','chat-details']) document.getElementById(id)?.addEventListener('click',e=>{e.stopImmediatePropagation();details();},true);
  const search=button('','icon-button',()=>{
    if(!app.getSelected())return;
    panelVersion++;modalElement.classList.remove('community-panel');
    modal(tr('Поиск в чате','Search chat'),body=>{
      const input=node('input','community-search-input'),results=node('div');input.placeholder=tr('Поиск в загруженных сообщениях','Search loaded messages');input.setAttribute('aria-label',input.placeholder);
      body.append(input,node('p','small muted',tr('Для поиска по старой истории сначала загрузите предыдущие сообщения.','Load older messages first to search earlier history.')),results);
      input.oninput=()=>{results.replaceChildren();const q=input.value.trim().toLocaleLowerCase();if(!q)return;const found=app.getMessages().filter(m=>m.body?.toLocaleLowerCase().includes(q)).slice(0,30);
        for(const m of found)results.append(button((m.name||'')+' · '+m.body.slice(0,120),'community-search-result',()=>{modalElement.close();const row=document.querySelector('[data-message-id="'+Number(m.id)+'"]');row?.scrollIntoView({block:'center',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});row?.classList.add('community-highlight');setTimeout(()=>row?.classList.remove('community-highlight'),2000);}));
        if(!found.length)results.append(node('p','muted',tr('Ничего не найдено','No results')));
      };input.focus();
    });
  });search.title=tr('Поиск в чате','Search chat');search.setAttribute('aria-label',search.title);
  search.innerHTML='<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="10" cy="10" r="7"/><path d="m15 15 6 6"/></svg>';
  document.querySelector('.chat-actions')?.prepend(search);
  const box=document.getElementById('messages');
  const down=button('↓','community-scroll-bottom',()=>box.scrollTo({top:box.scrollHeight,behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'}));down.hidden=true;down.setAttribute('aria-label',tr('Последние сообщения','Latest messages'));document.getElementById('active-chat')?.append(down);
  box.addEventListener('scroll',()=>{down.hidden=box.scrollHeight-box.clientHeight-box.scrollTop<250;},{passive:true});
})();
