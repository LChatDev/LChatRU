'use strict';
const { app, BrowserWindow, Menu, Tray, nativeImage, dialog, shell, session } = require('electron');
const path = require('node:path');
const { ORIGIN, trusted, external, permissionAllowed } = require('./policy.cjs');
let window, tray, quitting = false;
const smoke = process.argv.includes('--smoke-test');
const smokeOutput = process.env.LCHAT_SMOKE_OUTPUT;
if (smoke) app.setPath('userData', path.join(app.getPath('temp'), 'lchat-desktop-smoke'));
if (!app.requestSingleInstanceLock()) app.quit();
else {
app.on('second-instance', () => show());
app.whenReady().then(async () => {
  const ses = session.fromPartition(smoke ? 'lchat-test' : 'persist:lchat');
  ses.setPermissionCheckHandler((wc, permission, origin, details) => {
    if (!wc || !trusted(wc.getURL()) || !trusted(origin)) return false;
    if (permission === 'media') return ['audio', 'video'].includes(details.mediaType);
    return ['notifications', 'fullscreen'].includes(permission);
  });
  ses.setPermissionRequestHandler(async (wc, permission, callback, details) => {
    if (!wc || !trusted(wc.getURL()) || !permissionAllowed(details.requestingUrl, permission, details)) return callback(false);
    const names = permission === 'media' ? details.mediaTypes.map(x => x === 'audio' ? 'микрофон' : 'камеру').join(' и ') : permission === 'notifications' ? 'уведомления' : 'полноэкранный режим';
    const answer = await dialog.showMessageBox(window, { type: 'question', title: 'Разрешение LChat', message: `Разрешить ${names}?`, buttons: ['Запретить', 'Разрешить'], defaultId: 0, cancelId: 0 });
    callback(answer.response === 1);
  });
  window = new BrowserWindow({ width: 1180, height: 820, minWidth: 360, minHeight: 500, show: !smoke, title: 'LChat', backgroundColor: '#111827', icon: path.join(__dirname, 'icon.png'),
    webPreferences: { session: ses, nodeIntegration: false, contextIsolation: true, sandbox: true, webSecurity: true, allowRunningInsecureContent: false, backgroundThrottling: false, spellcheck: true } });
  window.webContents.setWindowOpenHandler(({ url }) => { if (trusted(url)) window.loadURL(url).catch(showError); else openExternal(url); return { action: 'deny' }; });
  window.webContents.on('will-navigate', (event, url) => { if (!trusted(url)) { event.preventDefault(); openExternal(url); } });
  window.webContents.on('will-redirect', (event, url) => { if (!trusted(url)) { event.preventDefault(); showError(new Error('Сервер перенаправил на другой адрес.')); } });
  window.webContents.on('will-attach-webview', event => event.preventDefault());
  window.webContents.on('did-fail-load', (_e, code, _desc, _url, mainFrame) => { if (mainFrame && code !== -3 && !smoke) showError(new Error('Нет соединения с LChat. Проверьте интернет и нажмите Ctrl+R.')); });
  window.on('close', event => { if (!quitting && !smoke) { event.preventDefault(); window.hide(); } });
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    { label: 'LChat', submenu: [{ label: 'Открыть', click: show }, { label: 'Обновить', accelerator: 'CmdOrCtrl+R', click: () => window.reload() }, { label: 'Открыть в браузере', click: () => openExternal(ORIGIN) }, { type: 'separator' }, { label: 'Выйти', click: quit }] },
    { label: 'Правка', submenu: [{ role: 'undo' }, { role: 'redo' }, { type: 'separator' }, { role: 'cut' }, { role: 'copy' }, { role: 'paste' }, { role: 'selectAll' }] },
    { label: 'Вид', submenu: [{ role: 'resetZoom' }, { role: 'zoomIn' }, { role: 'zoomOut' }, { role: 'togglefullscreen' }] }
  ]));
  if (!smoke) {
    tray = new Tray(nativeImage.createFromPath(path.join(__dirname, 'icon.png')));
    tray.setToolTip('LChat · работает в фоне');
    tray.setContextMenu(Menu.buildFromTemplate([{ label: 'Открыть LChat', click: show }, { label: 'Выход (завершить звонок)', click: quit }]));
    tray.on('double-click', show);
  }
  try {
    await window.loadURL(ORIGIN);
    if (smoke) {
      await new Promise(resolve => setTimeout(resolve, 1200));
      const state = await window.webContents.executeJavaScript('({title:document.title,url:location.href,ready:document.readyState,body:document.body.innerText.slice(0,1000),node:typeof require})');
      const fs = require('node:fs');
      fs.mkdirSync(smokeOutput, { recursive: true });
      fs.writeFileSync(path.join(smokeOutput, 'desktop.json'), JSON.stringify(state, null, 2));
      fs.writeFileSync(path.join(smokeOutput, 'desktop.png'), (await window.webContents.capturePage()).toPNG());
      quit();
    }
  } catch (error) { if (smoke) { console.error(error.message); app.exit(1); } else showError(error); }
});
app.on('before-quit', () => { quitting = true; });
}
function show() { if (window) { if (window.isMinimized()) window.restore(); window.show(); window.focus(); } }
function quit() { quitting = true; app.quit(); }
function showError(error) { if (!smoke && window && !window.isDestroyed()) dialog.showMessageBox(window, { type: 'error', title: 'LChat', message: error.message }); }
async function openExternal(url) {
  if (!external(url)) return;
  const result = await dialog.showMessageBox(window, { type: 'question', title: 'Открыть ссылку', message: 'Открыть ссылку в браузере?', detail: url, buttons: ['Отмена', 'Открыть'], defaultId: 0, cancelId: 0 });
  if (result.response === 1) shell.openExternal(url).catch(showError);
}
