<?php if (!defined('LCHAT_INTERNAL')) { http_response_code(404); exit; } header('Cache-Control: no-store'); ?>
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>LChat setup</title><link rel="stylesheet" href="assets/style.css"><link rel="stylesheet" href="assets/theme.css"><link rel="stylesheet" href="assets/weba-pages.css"></head>
<body class="install-body"><main class="install-card"><span class="brand-mark">L</span><h1>LChat setup</h1><p>Tables will be created on the backend server.</p><p id="status" role="status">Checking the server…</p><form id="setup" hidden><label>Installation key<input type="password" name="key" required autocomplete="off"></label><button class="primary">Create tables</button></form><a href="index.php">Open messenger</a></main>
<script>
const status=document.getElementById('status'),form=document.getElementById('setup');
async function run(options){const r=await fetch('install.php?api=1',options),d=await r.json();if(!r.ok)throw Error(d.error||'Setup failed');status.textContent=d.done?'Tables are ready. Remove the installer and clear the backend installation key.':'Enter the installation key.';form.hidden=d.done;}
form.addEventListener('submit',async e=>{e.preventDefault();const b=form.querySelector('button');b.disabled=true;try{await run({method:'POST',body:new URLSearchParams(new FormData(form))});}catch(err){status.textContent=err.message;}finally{b.disabled=false;}});
run().catch(e=>{status.textContent=e.message;});
</script></body></html>
