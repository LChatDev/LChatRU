'use strict';
(() => {
  const app=window.LChat;if(!app)return;
  const {api,node,button,report,toast}=app;
  const dialog=document.createElement('dialog');dialog.className='discussion-dialog';
  dialog.setAttribute('aria-label','Комментарии');document.body.append(dialog);
  let current=null,timer=null,busy=false,requestVersion=0;
  const safe=task=>Promise.resolve().then(task).catch(report);
  dialog.addEventListener('close',()=>{current=null;requestVersion++;clearInterval(timer);});
  dialog.addEventListener('cancel',()=>dialog.close());
  async function open(channel,post){
    clearInterval(timer);current={channel:Number(channel),post:Number(post)};requestVersion++;
    dialog.replaceChildren();
    const header=node('header','discussion-header'),title=node('h2','','Комментарии'),close=button('×','icon-button',()=>dialog.close());close.setAttribute('aria-label','Закрыть комментарии');header.append(title,close);
    const status=node('p','muted','Загрузка…'),list=node('div','discussion-list'),older=button('Показать предыдущие','button',()=>safe(()=>load(true))),form=node('form','discussion-composer'),input=node('textarea');
    older.hidden=true;input.placeholder='Написать комментарий…';input.setAttribute('aria-label','Текст комментария');input.maxLength=10485760;input.required=true;
    const send=button('Отправить','primary',()=>{});send.type='submit';form.append(input,send);
    dialog.append(header,status,older,list,form);
    if(!dialog.open)dialog.showModal();
    let oldest=0,loading=false,pending=null,expanded=false;
    function render(m){const row=node('article','discussion-comment');row.dataset.commentId=m.id;row.append(node('b','',m.name),node('p','',m.body),node('small','muted',m.created_at+(m.edited_at?' · изменено':'')));
      if(m.can_delete)row.append(button('Удалить','discussion-delete',()=>safe(async()=>{if(!confirm('Удалить комментарий?'))return;await api('discussion.delete',{chat_id:channel,post_id:post,message_id:m.id},true);await load();await app.refreshChats();})));return row;}
    async function load(previous=false){
      if(loading||!current||current.channel!==Number(channel)||current.post!==Number(post))return;
      loading=true;if(previous)expanded=true;const version=requestVersion;
      try{const data=await api('discussion.messages',{chat_id:channel,post_id:post,...(previous?{before:oldest}:{})});
        if(version!==requestVersion||!dialog.open)return;
        status.textContent=data.discussion.title+' · '+data.total+' комментариев. Отправляя комментарий, вы вступаете в эту группу.';
        if(!previous){list.replaceChildren();oldest=0;}
        const fragment=document.createDocumentFragment();for(const m of data.messages)fragment.append(render(m));
        if(previous)list.prepend(fragment);else list.append(fragment);
        oldest=Number(list.firstElementChild?.dataset.commentId||0);older.hidden=!data.has_more;
        if(!data.messages.length&&!previous)list.append(node('p','muted','Пока нет комментариев. Начните обсуждение.'));
      }finally{loading=false;}
    }
    form.onsubmit=e=>{e.preventDefault();if(busy||!input.value.trim())return;busy=true;send.disabled=true;const text=input.value;if(!pending||pending.text!==text)pending={text,nonce:crypto.randomUUID()};const nonce=pending.nonce;safe(async()=>{await api('discussion.send',{chat_id:channel,post_id:post,body:text,nonce},true);pending=null;pending=null;if(input.value===text)input.value='';await load();await app.refreshChats();}).finally(()=>{busy=false;send.disabled=false;});};
    input.onkeydown=e=>{if(e.key==='Enter'&&!e.shiftKey&&!e.isComposing){e.preventDefault();form.requestSubmit();}};
    try{await load();}catch(e){status.textContent=e.message||'Не удалось открыть обсуждение.';form.hidden=true;report(e);}
    // Pause automatic replacement while browsing older comments or typing.
    timer=setInterval(()=>{if(dialog.open&&!document.hidden&&!expanded&&!input.value&&list.scrollTop+list.clientHeight>=list.scrollHeight-80)safe(()=>load());},5000);
  }
  function settings(body,c,link){
    const section=node('section','discussion-settings');section.append(node('h3','','Обсуждение'));
    if(link)section.append(node('p','muted','Группа: '+link.title));else section.append(node('p','muted','Подключите группу, чтобы подписчики могли комментировать публикации.'));
    const run=async(data,control)=>{control.disabled=true;try{await api('discussion.configure',{chat_id:c.id,...data},true);toast('Настройки обсуждения сохранены');document.getElementById('modal').close();await app.refreshChats();}catch(e){report(e);}finally{control.disabled=false;}};
    if(c.role==='owner'){
      section.append(node('p','small muted','Комментарии доступны подписчикам канала. Авторы комментариев вступают в группу; существующая переписка группы остаётся доступна её участникам.'));
      const select=node('select');select.setAttribute('aria-label','Группа обсуждения');const empty=node('option','','Выберите свою группу');empty.value='';select.append(empty);
      for(const g of app.getChats().filter(g=>g.kind==='group'&&g.role==='owner')){const opt=node('option','',g.title);opt.value=g.id;select.append(opt);}section.append(select);
      const connect=button('Подключить группу','button',()=>{if(!select.value)return toast('Выберите группу');safe(()=>run({group_id:Number(select.value)},connect));});section.append(connect);
      if(!link){const create=button('Создать группу обсуждений','primary',()=>safe(()=>run({create:true},create)));section.append(create);}
      else{const detach=button('Отключить обсуждение','button',()=>{if(confirm('Отключить комментарии? Сообщения сохранятся в группе.'))safe(()=>run({group_id:0},detach));});section.append(detach);}
    }
    if(link)section.append(button('Открыть группу','button',()=>safe(async()=>{await app.refreshChats();const group=app.getChats().find(g=>Number(g.id)===Number(link.group_id));if(!group)return toast('После первого комментария группа появится в чатах');document.getElementById('modal').close();await app.selectChat(group);})));
    body.append(section);
  }
  window.LChatDiscussions={open,settings};
})();
