/* Apply the visitor's existing messenger appearance before the page is painted. */
try {
  const saved = JSON.parse(localStorage.getItem('lchat-appearance') || '{}');
  if (saved.theme === 'light' || saved.theme === 'dark') {
    document.documentElement.dataset.lchatTheme = saved.theme;
  }
} catch (_) { /* The page keeps its default theme if storage is unavailable. */ }
