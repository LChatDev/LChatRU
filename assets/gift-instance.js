'use strict';
(() => {
  const ns='http://www.w3.org/2000/svg';
  function decorate(card,id){
    if(!/^[a-f0-9]{48}$/i.test(String(id)))return;
    card.classList.add('unique-gift');card.style.setProperty('--gift-hue',String(parseInt(id.slice(0,4),16)%360));
    const svg=document.createElementNS(ns,'svg');svg.setAttribute('viewBox','0 0 160 120');svg.setAttribute('class','gift-fingerprint');svg.setAttribute('aria-hidden','true');
    let i=0;for(const hex of id.toLowerCase()){const value=parseInt(hex,16);for(let bit=3;bit>=0;bit--,i++){if(!(value&(1<<bit)))continue;const dot=document.createElementNS(ns,'circle');dot.setAttribute('cx',String((i%16)*10+5));dot.setAttribute('cy',String(Math.floor(i/16)*10+5));dot.setAttribute('r','2');svg.append(dot);}}
    card.prepend(svg);card.title='Уникальный идентификатор: '+id;
  }
  window.LChatGiftArt={decorate};
  document.querySelectorAll('[data-gift-instance]').forEach(el=>decorate(el,el.dataset.giftInstance));
})();
