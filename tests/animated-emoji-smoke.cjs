// Run with Node.js and Playwright installed: node tests/animated-emoji-smoke.cjs
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..');
const manifest = JSON.parse(fs.readFileSync(path.join(root, 'assets/emoji-catalog.js'), 'utf8'));
const check = (value, message) => { if (!value) throw Error(message); };
(async () => {
  check(manifest.length === 631 && new Set(manifest.map(x => x.id)).size === 631, 'catalog');
  for (const entry of manifest) check(/^[a-f0-9]{16}$/.test(entry.id) && fs.existsSync(path.join(root, 'assets/animated-emoji', entry.id + '.webp')), 'asset ' + entry.id);
  const launch = { headless: true }; if (process.env.CHROME_PATH) launch.executablePath = process.env.CHROME_PATH;
  const browser = await chromium.launch(launch);
  try {
    for (const viewport of [{ width: 390, height: 780 }, { width: 1440, height: 900 }]) {
      const page = await browser.newPage({ viewport }); let imageRequests = 0;
      await page.route('https://lchat.test/**', route => {
        const url = new URL(route.request().url());
        if (url.pathname.endsWith('/emoji-catalog.js')) return route.fulfill({ json: manifest });
        if (/\/assets\/animated-emoji\/[a-f0-9]{16}\.webp$/.test(url.pathname)) {
          imageRequests++;
          return route.fulfill({ status: 200, contentType: 'image/webp', body: fs.readFileSync(path.join(root, 'assets/animated-emoji', manifest[0].id + '.webp')) });
        }
        return route.fulfill({ status: 200, contentType: 'text/html', body: '<div id="message-items"></div><form id="composer"><textarea id="message-input"></textarea><button type="submit">Send</button></form>' });
      });
      await page.goto('https://lchat.test/');
      await page.addStyleTag({ path: path.join(root, 'assets/style.css') });
      await page.addStyleTag({ path: path.join(root, 'assets/animated-emoji.css') });
      await page.addScriptTag({ path: path.join(root, 'assets/animated-emoji.js') });
      check(!await page.locator('#message-input').isVisible(), 'raw textarea hidden');
      await page.locator('.lce-editor').fill('Hello ');
      await page.locator('.lce-toggle').click();
      await page.locator('.lce-grid button').first().waitFor();
      check(await page.locator('.lce-grid button').count() === 631, 'full scrollable list');
      await page.waitForTimeout(300);
      check(imageRequests < 631, 'lazy image loading');
      await page.locator('.lce-grid button').last().scrollIntoViewIfNeeded();
      check(await page.locator('.lce-grid button').last().isVisible(), 'last item accessible');
      await page.locator('.lce-panel input').fill('Cat');
      check(await page.locator('.lce-grid button').count() > 0, 'search');
      await page.locator('.lce-grid button').first().click();
      const body = await page.locator('#message-input').inputValue();
      check(/^Hello \[lce:[a-f0-9]{16}\]$/.test(body), 'preserve text and token');
      check(await page.locator('.lce-editor img').count() === 1, 'emoji visible before sending');
      await page.locator('.lce-editor').press('Shift+Enter');
      await page.keyboard.type('Next');
      check((await page.locator('#message-input').inputValue()).endsWith('\nNext'), 'line break');
      await page.evaluate(() => document.getElementById('composer').addEventListener('submit', e => { e.preventDefault(); window.result = document.getElementById('message-input').value; }));
      await page.locator('.lce-editor').press('Enter');
      check((await page.evaluate(() => window.result)).endsWith('\nNext'), 'send');
      const token = body.match(/\[lce:[a-f0-9]{16}\]/)[0];
      await page.locator('#message-items').evaluate((el, value) => { const row = document.createElement('div'); row.className = 'message-text'; row.textContent = value; el.append(row); }, token);
      await page.locator('.message-text img.lce-inline').waitFor();
      await page.evaluate(() => { document.getElementById('message-input').value = ''; });
      check((await page.locator('.lce-editor').innerText()) === '', 'reset after sending');
      await page.close();
    }
    console.log('PASS 631 assets, mobile and desktop catalog, lazy loading, editor and message rendering');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
