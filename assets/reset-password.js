(() => {
  'use strict';
  function openReset() {
    const token = location.hash.slice(1);
    if (!/^[a-f0-9]{64}$/.test(token)) return;
    const form = document.getElementById('reset-form');
    if (!form) { location.replace(location.pathname + '?open=' + Date.now() + '#' + token); return; }
    document.getElementById('reset-token').value = token;
    document.getElementById('request-form').hidden = true;
    document.getElementById('request-description').hidden = true;
    document.getElementById('reset-description').hidden = false;
    form.hidden = false;
    history.replaceState(null, '', location.pathname + location.search);
  }
  window.addEventListener('hashchange', openReset);
  openReset();
})();
