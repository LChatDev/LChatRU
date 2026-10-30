<?php if (!defined('LCHAT_INTERNAL')) { http_response_code(404); exit; } header('Cache-Control: no-store'); ?>
<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Установка LChat</title><link rel="stylesheet" href="assets/style.css"><link rel="stylesheet" href="assets/theme.css"><link rel="stylesheet" href="assets/weba-pages.css"></head>
<body class="install-body"><main class="install-card"><span class="brand-mark">L</span><h1>Установка LChat</h1><p>Таблицы будут созданы на сервере LChatHost.</p><p id="status" role="status">Проверяем сервер…</p><form id="setup" hidden><label>Ключ установки<input type="password" name="key" required autocomplete="off"></label><button class="primary">Создать таблицы</button></form><a href="index.php">Открыть мессенджер</a></main>
<script>
const status=document.getElementById('status'),form=document.getElementById('setup');
async function run(options){const r=await fetch('install.php?api=1',options),d=await r.json();if(!r.ok)throw Error(d.error||'Ошибка установки');status.textContent=d.done?'Таблицы готовы. Удалите установщик и очистите ключ на LChatHost.':'Введите ключ установки.';form.hidden=d.done;}
form.addEventListener('submit',async e=>{e.preventDefault();const b=form.querySelector('button');b.disabled=true;try{await run({method:'POST',body:new URLSearchParams(new FormData(form))});}catch(err){status.textContent=err.message;}finally{b.disabled=false;}});
run().catch(e=>{status.textContent=e.message;});
</script></body></html>
