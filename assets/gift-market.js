'use strict';
(() => {
  const filters=[...document.querySelectorAll('.filter')], search=document.getElementById('gift-search');
  const cards=[...document.querySelectorAll('.gift-card')],empty=document.getElementById('market-empty');
  let filter='all';
  const refresh=()=>{const term=search.value.trim().toLocaleLowerCase('ru');let shown=0;for(const card of cards){const visible=(filter==='all'||card.dataset.category===filter)&&(!term||card.dataset.name.includes(term));card.hidden=!visible;if(visible)shown++;}empty.hidden=shown!==0;};
  filters.forEach(button=>button.addEventListener('click',()=>{filter=button.dataset.filter;for(const item of filters){const active=item===button;item.classList.toggle('active',active);item.setAttribute('aria-pressed',String(active));}refresh();}));
  search.addEventListener('input',refresh);
  const dialog=document.getElementById('gift-dialog'),confirm=document.getElementById('gift-confirm'),preview=document.getElementById('gift-preview');
  const celebration=document.getElementById('celebration');if(celebration){for(const b of celebration.querySelectorAll('button'))b.addEventListener('click',()=>celebration.remove());celebration.addEventListener('click',e=>{if(e.target===celebration)celebration.remove();});}
  if(!dialog?.showModal)return;
  for(const card of cards)card.addEventListener('submit',event=>{event.preventDefault();confirm.elements.gift.value=card.elements.gift.value;confirm.elements.nonce.value=card.elements.nonce.value;document.getElementById('gift-title').textContent=card.dataset.title;document.getElementById('gift-cost').textContent=card.dataset.price;preview.replaceChildren(card.querySelector('.gift-art').cloneNode(true));dialog.showModal();});
  dialog.querySelector('.dialog-close').addEventListener('click',()=>dialog.close());
  dialog.addEventListener('click',event=>{if(event.target===dialog)dialog.close();});
  confirm.addEventListener('submit',()=>{const b=confirm.querySelector('.confirm-gift');b.disabled=true;b.textContent='Отправляем…';});
})();
