const { test } = require('node:test');
const assert = require('node:assert/strict');
const { trusted, external, permissionAllowed } = require('./policy.cjs');
test('only the exact LChat HTTPS origin receives in-app navigation and permissions', () => {
  assert(trusted('https://chat.lbs.su/index.php'));
  for (const url of ['http://chat.lbs.su', 'https://chat.lbs.su.evil.test', 'https://user:pass@chat.lbs.su', 'file:///C:/Windows', 'javascript:alert(1)', 'https://chat.lbs.su:8443']) assert(!trusted(url), url);
});
test('untrusted origins and sensitive unrelated permissions are refused', () => {
  assert(permissionAllowed('https://chat.lbs.su/', 'media', { mediaTypes: ['audio'] }));
  assert(!permissionAllowed('https://evil.test/', 'media', { mediaTypes: ['audio'] }));
  assert(!permissionAllowed('https://chat.lbs.su/', 'media', { mediaTypes: ['display'] }));
  assert(!permissionAllowed('https://chat.lbs.su/', 'geolocation'));
  assert(!external('file:///C:/secret')); assert(!external('cmd.exe')); assert(external('https://example.com'));
});
