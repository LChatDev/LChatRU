(() => {
  'use strict';
  if (!window.lottie || !('IntersectionObserver' in window) || matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const seen=new Set(),visible=new Set(),active=new Map(),cache=new Map();
  function stop(el) {
    const entry=active.get(el);if(!entry)return;
    active.delete(el);entry.abort.abort();if(entry.player)entry.player.destroy();entry.layer.remove();
    const preview=el.querySelector('.tgs-preview');if(preview)preview.style.visibility='';
  }
  async function start(el) {
    const id=el.dataset.tgs;if(!/^gift_[0-9]{19}$/.test(id||''))return;
    el.querySelectorAll('.tgs-layer').forEach(layer=>layer.remove());
    const preview=el.querySelector('.tgs-preview');if(preview)preview.style.visibility='';
    const layer=document.createElement('span');layer.className='tgs-layer';layer.setAttribute('aria-hidden','true');el.append(layer);
    const entry={abort:new AbortController(),layer,player:null};active.set(el,entry);
    try {
      let raw=cache.get(id);
      if(!raw){const response=await fetch('gift-animation.php?id='+encodeURIComponent(id),{signal:entry.abort.signal,credentials:'same-origin'});if(!response.ok)throw Error('Animation unavailable');raw=await response.text();cache.set(id,raw);if(cache.size>20)cache.delete(cache.keys().next().value);}
      if(active.get(el)!==entry||!visible.has(el)||!el.isConnected)return;
      const player=window.lottie.loadAnimation({container:layer,renderer:'svg',loop:true,autoplay:!document.hidden,animationData:JSON.parse(raw),rendererSettings:{preserveAspectRatio:'xMidYMid meet'}});entry.player=player;
      player.addEventListener('DOMLoaded',()=>{if(active.get(el)===entry&&preview)preview.style.visibility='hidden';});
      player.addEventListener('data_failed',()=>{stop(el);visible.delete(el);drain();});
    } catch(error) {if(error.name!=='AbortError'&&active.get(el)===entry){stop(el);visible.delete(el);drain();}}
  }
  function drain(){
    const center=innerHeight/2;
    const candidates=[...visible].filter(el=>el.isConnected).sort((a,b)=>{
      const score=el=>{const r=el.getBoundingClientRect();return Math.abs((r.top+r.bottom)/2-center)-(el.closest('dialog[open],#celebration')?100000:0);};
      return score(a)-score(b);
    });
    const desired=new Set(candidates);
    for(const el of active.keys())if(!desired.has(el))stop(el);
    for(const el of candidates)if(!active.has(el))start(el);
  }
  const observer=new IntersectionObserver(changes=>{for(const change of changes){const el=change.target;if(change.isIntersecting)visible.add(el);else{visible.delete(el);stop(el);}}drain();});
  function scan(root){if(root.nodeType!==1)return;if(root.matches('.gift-tgs[data-tgs]')&&!seen.has(root)){seen.add(root);observer.observe(root);}root.querySelectorAll?.('.gift-tgs[data-tgs]').forEach(scan);}
  document.querySelectorAll('.gift-tgs[data-tgs]').forEach(scan);
  new MutationObserver(changes=>{for(const change of changes)for(const node of change.addedNodes)scan(node);for(const el of seen)if(!el.isConnected){observer.unobserve(el);seen.delete(el);visible.delete(el);stop(el);}drain();}).observe(document.body,{childList:true,subtree:true});
  document.addEventListener('visibilitychange',()=>{for(const entry of active.values())if(entry.player){if(document.hidden)entry.player.pause();else entry.player.play();}});
})();
