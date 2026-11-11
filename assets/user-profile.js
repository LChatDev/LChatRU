'use strict';
(() => {
  const app=window.LChat, dialog=document.getElementById('modal');
  if (!app || !dialog) return;
  const {node,button,avatar,modal,report}=app;
  let requestId=0;
  dialog.addEventListener('close',()=>{requestId++;dialog.classList.remove('profile-view');});
  document.addEventListener('click',event=>{
    if (!event.target.closest('#chat-details')) return;
    const chat=app.getSelected();
    if (!chat || chat.kind!=='direct' || !chat.peer?.id) return;
    event.stopImmediatePropagation(); event.preventDefault();
    openProfile(chat).catch(report);
  },true);
  async function openProfile(chat){
    const serial=++requestId;dialog.classList.add('profile-view');
    modal('Профиль',body=>body.append(node('p','profile-loading','Загружаем профиль…')));
    try {
      const url=new URL('user-profile.php',location.href);url.searchParams.set('id',chat.peer.id);url.searchParams.set('chat_id',chat.id);
      const response=await fetch(url,{credentials:'same-origin',cache:'no-store'});const data=await response.json();
      if(!response.ok)throw new Error(data.error||'Не удалось загрузить профиль.');
      if(serial!==requestId || !dialog.open)return;
      const p=data.profile;
      modal('Профиль',body=>{
        const hero=node('div','profile-hero'),photo=avatar(p.name,p.id),title=node('h2','profile-name',p.name);
        if(Number(p.verified)){const check=node('span','profile-check',' ✓');check.title='Подтверждённый аккаунт';title.append(check);}
        const status=window.LChatRelativePresence?.(p.last_seen,chat.peer.in_call)||'Не был в сети';
        hero.append(photo,title,node('p','profile-status',status));body.append(hero);
        const actions=node('div','profile-actions');
        const action=(symbol,label,fn)=>{const b=button('','profile-action',fn);b.append(node('span','profile-action-icon',symbol),node('span','',label));actions.append(b);};
        action('●','Чат',()=>dialog.close());
        action('☎','Звонок',()=>{dialog.close();document.getElementById('audio-call')?.click();});
        action('▣','Видео',()=>{dialog.close();document.getElementById('video-call')?.click();});
        action('🎁','Подарок',()=>openGiftPicker(chat,p,data.catalog||[]));
        body.append(actions);
        const info=node('section','profile-card');info.append(node('div','profile-card-label','Имя пользователя'),node('p','profile-username','@'+p.username));
        if(p.bio){info.append(node('div','profile-card-label','О себе'),node('p','profile-bio',p.bio));}body.append(info);
        const gifts=node('section','profile-gifts');gifts.append(node('h3','','Подарки'));
        if(!data.gifts?.length)gifts.append(node('p','profile-empty','Пока нет подарков'));
        else {const grid=node('div','profile-gift-grid');const append=items=>{for(const gift of items){const card=node('div','profile-gift'),art=giftArt(gift);card.append(art,node('strong','',gift.name),node('small','',gift.fingerprint?'#'+gift.fingerprint.slice(0,12):'× '+gift.count));if(gift.fingerprint)window.LChatGiftArt?.decorate(card,gift.fingerprint);grid.append(card);}};append(data.gifts);gifts.append(grid);if(data.has_more){let offset=data.next_offset;const more=button('Показать ещё','profile-more',async()=>{more.disabled=true;try{const u=new URL('user-profile.php',location.href);u.searchParams.set('id',p.id);u.searchParams.set('chat_id',chat.id);u.searchParams.set('offset',offset);const r=await fetch(u,{credentials:'same-origin',cache:'no-store'});const next=await r.json();if(!r.ok)throw new Error(next.error||'Не удалось загрузить подарки.');append(next.gifts);offset=next.next_offset;if(!next.has_more)more.remove();}catch(e){report(e);}finally{more.disabled=false;}});gifts.append(more);}}
        body.append(gifts);
        const send=button('🎁  Отправить подарок','profile-send-gift',()=>openGiftPicker(chat,p,data.catalog||[]));body.append(send);
      });
    } catch(error){if(serial===requestId && dialog.open){dialog.classList.remove('profile-view');modal('Профиль',body=>body.append(node('p','form-error',error.message||'Не удалось загрузить профиль.')));}}
  }
  function giftArt(gift){const art=node('span','profile-gift-emoji',gift.emoji);if(Number.isInteger(gift.sprite)&&gift.sprite>=0&&gift.sprite<(gift.sheet?16:9)){art.textContent='';art.classList.add('profile-sprite');art.dataset.sprite=gift.sprite;art.dataset.variant=gift.variant;if(['one','two'].includes(gift.sheet)){art.dataset.sheet=gift.sheet;art.style.setProperty('--sprite-x',(gift.sprite%4)*100/3+'%');art.style.setProperty('--sprite-y',Math.floor(gift.sprite/4)*100/3+'%');}}return art;}
  function openGiftPicker(chat,person,catalog){
    let pending=null,selected=null;
    modal('Подарок для @'+person.username,body=>{
      const hint=node('p','profile-picker-hint','Выберите подарок. Его уникальный узор появится после отправки.');
      const balance=node('p','profile-picker-balance','Загружаем каталог…'),grid=node('div','profile-gift-grid profile-picker-grid'),message=node('p','profile-picker-message');
      const confirm=button('Выберите подарок','profile-send-gift',async()=>{
        if(!selected||!pending)return;
        confirm.disabled=true;message.textContent='Отправляем…';
        try{
          const me=await app.api('me');
          const response=await fetch('gift-purchase.php',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json','X-CSRF-Token':me.csrf},body:JSON.stringify({gift:selected.id,recipient:person.username,nonce:pending})});
          const result=await response.json();if(!response.ok)throw new Error(result.error||'Не удалось отправить подарок.');
          body.replaceChildren();const success=node('div','profile-gift-success'),card=node('div','profile-gift profile-success-art');card.append(giftArt(selected));window.LChatGiftArt?.decorate(card,result.fingerprint);success.append(card,node('h2','','Подарок отправлен!'),node('p','',`@${person.username} · осталось ${result.balance} тикетов`),node('p','profile-gift-serial','#'+result.fingerprint),button('Готово','profile-send-gift',()=>openProfile(chat)));body.append(success);
        }catch(e){message.textContent=e.message||'Не удалось отправить подарок.';confirm.disabled=false;}
      });
      confirm.disabled=true;
      for(const gift of catalog){const card=button('','profile-gift profile-pick',()=>{if(!globalThis.crypto?.getRandomValues){message.textContent='Для отправки нужен современный браузер.';return;}const bytes=crypto.getRandomValues(new Uint8Array(24));pending=Array.from(bytes,b=>b.toString(16).padStart(2,'0')).join('');selected=gift;grid.querySelectorAll('.profile-pick').forEach(item=>item.classList.remove('selected'));card.classList.add('selected');confirm.disabled=false;confirm.textContent='Подарить за '+gift.price+' ✦';message.textContent='';});card.append(giftArt(gift),node('strong','',gift.name),node('small','',gift.price+' ✦'));grid.append(card);}
      balance.textContent=catalog.length?'Выберите вариант и подтвердите отправку.':'Подарки пока недоступны.';body.append(hint,balance,grid,message,confirm);
    });
  }
})();
