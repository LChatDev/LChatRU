(() => {
  'use strict';
  const dialog = document.getElementById('payment-dialog');
  function showUnavailable() {
    if (typeof dialog.showModal === 'function') dialog.showModal();
    else document.getElementById('payment-status').scrollIntoView({behavior:'smooth', block:'center'});
  }
  document.querySelectorAll('[data-payment-unavailable]').forEach(button => {
    button.addEventListener('click', showUnavailable);
  });
  const form = document.getElementById('ticket-calculator');
  const quantity = document.getElementById('ticket-quantity');
  const total = document.getElementById('ticket-total');
  const whole = new Intl.NumberFormat('ru-RU', {maximumFractionDigits: 0});
  const kopecks = new Intl.NumberFormat('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2});
  function amount(rubles) { return (Number.isInteger(rubles) ? whole : kopecks).format(rubles) + ' ₽'; }
  function updateTickets() {
    const n = Number(quantity.value);
    total.textContent = quantity.value !== '' && quantity.validity.valid && Number.isSafeInteger(n) && n >= 1 && n <= 100000 ? amount(n / 2) : 'Укажите от 1 до 100 000';
  }
  quantity.addEventListener('input', updateTickets);
  for (const [id, change] of [['ticket-minus', -1], ['ticket-plus', 1]]) {
    document.getElementById(id).addEventListener('click', () => {
      const n = Number(quantity.value);
      quantity.value = Math.max(1, Math.min(100000, (Number.isSafeInteger(n) && n >= 1 && n <= 100000 ? n : 10) + change));
      updateTickets();
    });
  }
  form.addEventListener('submit', event => { event.preventDefault(); if (form.reportValidity()) showUnavailable(); });
  const duration = document.getElementById('premium-duration');
  const premiumTotal = document.getElementById('premium-total');
  duration.addEventListener('change', () => { premiumTotal.textContent = amount(Number(duration.value) * Number(duration.dataset.monthlyPrice)); });
  updateTickets();
  dialog.querySelectorAll('[data-close-dialog]').forEach(button => button.addEventListener('click', () => dialog.close()));
  dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
})();
