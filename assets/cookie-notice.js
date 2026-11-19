(() => {
'use strict';
const key='lchat-cookie-notice-v1';
function saved(){try{return localStorage.getItem(key)==='accepted';}catch{return false;}}
if(saved())return;
const panel=document.createElement('aside');panel.className='cookie-notice';panel.setAttribute('aria-label','Использование cookie');
panel.innerHTML='<strong>Cookie и локальное хранилище</strong><p>Используем cookie для входа в аккаунт, а локальное хранилище — для настроек интерфейса. <a href="cookies.php">Подробнее</a></p><button type="button">Принимаю</button><span role="status"></span>';
panel.querySelector('button').addEventListener('click',()=>{try{localStorage.setItem(key,'accepted');}catch{}panel.remove();});
document.body.append(panel);
})();
