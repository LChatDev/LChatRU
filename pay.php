<?php
declare(strict_types=1);
header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
$packages = [
    ['tickets' => 10, 'rubles' => 5],
    ['tickets' => 50, 'rubles' => 25],
    ['tickets' => 100, 'rubles' => 50],
    ['tickets' => 500, 'rubles' => 250],
    ['tickets' => 1000, 'rubles' => 500],
];
$premiumPrice = 199;
?><!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <meta name="theme-color" content="#10101d">
  <meta name="description" content="Тикеты и Premium в LChat: цены и возможности.">
  <title>Тикеты и Premium · LChat</title>
  <link rel="icon" href="assets/favicon.svg" type="image/svg+xml">
  <link rel="stylesheet" href="assets/pay.css?v=20261107-2">
  <script src="assets/pay.js?v=20261107-2" defer></script>
<script src="assets/lchatai-secondary-theme.js?v=20261111-1"></script><link rel="stylesheet" href="assets/lchatai-secondary.css?v=20261111-1"></head>
<body class="lchat-secondary lchat-pay">
  <div class="background-glow glow-one" aria-hidden="true"></div>
  <div class="background-glow glow-two" aria-hidden="true"></div>
  <header class="topbar">
    <a class="brand" href="index.php" aria-label="Вернуться в LChat"><span class="brand-icon">L</span><span>LChat</span></a>
    <nav aria-label="Разделы страницы"><a href="#tickets">Тикеты</a><a href="#premium">Premium</a></nav>
    <a class="back" href="index.php">В мессенджер <span aria-hidden="true">↗</span></a>
  </header>
  <main class="shell">
    <section class="hero">
      <div class="hero-copy">
        <span class="eyebrow"><span class="eyebrow-dot"></span> БОЛЬШЕ ВОЗМОЖНОСТЕЙ В LCHAT</span>
        <h1>Маленькие знаки.<br><span>Большие эмоции.</span></h1>
        <p>Выбирайте подарки за тикеты и откройте для себя будущие возможности LChat Premium.</p>
        <div class="hero-actions"><a class="primary-link" href="#tickets">Посмотреть тикеты <span aria-hidden="true">↗</span></a><a class="ghost-link" href="#premium">Узнать о Premium <span aria-hidden="true">↓</span></a></div>
      </div>
      <div class="hero-art" aria-hidden="true"><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="hero-star">✦</div><span class="spark spark-one">✧</span><span class="spark spark-two">✦</span><span class="spark spark-three">✧</span><span class="floating-chip chip-one">✦ <strong>Тикеты</strong></span><span class="floating-chip chip-two">✧ <strong>Premium</strong></span></div>
    </section>

    <div class="availability" id="payment-status" role="status"><span class="availability-icon" aria-hidden="true">i</span><div><strong>Платёжная система не подключена</strong><p>Купить тикеты и оформить Premium пока нельзя. Тикеты сейчас выдаёт администрация.</p></div></div>

    <section id="tickets" class="section tickets-section">
      <div class="section-heading"><div><span class="eyebrow">ДАРИТЬ ПРИЯТНО</span><h2>Тикеты <span>✦</span></h2><p>Внутренняя валюта LChat для покупки подарков.</p></div><a class="text-link" href="rewards.php">Открыть подарки <span aria-hidden="true">↗</span></a></div>
      <form id="ticket-calculator" class="ticket-calculator">
        <div class="custom-copy"><span class="custom-icon" aria-hidden="true">✦</span><div><h3>Своё количество</h3><p>Выберите, сколько тикетов хотите купить.</p></div></div>
        <div class="custom-actions"><div class="quantity-control"><button type="button" id="ticket-minus" aria-label="Уменьшить количество">−</button><label><span class="sr-only">Количество тикетов</span><input type="number" id="ticket-quantity" min="1" max="100000" step="1" value="10" inputmode="numeric" required></label><button type="button" id="ticket-plus" aria-label="Увеличить количество">+</button></div><div class="custom-price"><span>К оплате</span><output id="ticket-total" aria-live="polite" for="ticket-quantity">5 ₽</output></div><button type="submit" class="custom-buy">Купить <span aria-hidden="true">↗</span></button></div>
      </form>
      <div class="ticket-grid">
        <?php foreach ($packages as $package): ?>
        <article class="ticket-card">
          <div class="ticket-symbol" aria-hidden="true">✦</div><span class="card-label">ТИКЕТЫ LCHAT</span>
          <h3><?=number_format($package['tickets'], 0, '.', ' ')?> <small>тикетов</small></h3>
          <p class="ticket-note">Для подарков в LChat</p>
          <button type="button" class="ticket-buy" data-payment-unavailable aria-label="Купить <?= $package['tickets'] ?> тикетов за <?= $package['rubles'] ?> рублей"><?=number_format($package['rubles'], 0, '.', ' ')?> ₽ <span aria-hidden="true">↗</span></button>
        </article>
        <?php endforeach; ?>
      </div>
      <p class="rate-note">10 тикетов = 5 ₽. Цена остальных наборов рассчитана по тому же курсу.</p>
    </section>

    <section id="premium" class="section premium-section">
      <div class="premium-intro"><div><span class="eyebrow">ЧУТЬ БОЛЬШЕ ВАШЕГО СТИЛЯ</span><h2>LChat <span>Premium</span></h2><p>Идеи для подписки, которую мы готовим. Возможности пока разрабатываются и могут измениться до запуска.</p></div><div class="premium-emblem" aria-hidden="true">✦</div></div>
      <div class="premium-grid">
        <article class="benefit"><span class="benefit-icon violet" aria-hidden="true">✦</span><h3>Значок Premium</h3><p>Особая звезда рядом с вашим именем и в профиле.</p><span class="planned">Планируется</span></article>
        <article class="benefit"><span class="benefit-icon rose" aria-hidden="true">◈</span><h3>Оформление профиля</h3><p>Дополнительные цвета и красивые акценты для профиля.</p><span class="planned">Планируется</span></article>
        <article class="benefit"><span class="benefit-icon blue" aria-hidden="true">◌</span><h3>Темы чатов</h3><p>Подборка эксклюзивных фонов для ваших разговоров.</p><span class="planned">Планируется</span></article>
        <article class="benefit"><span class="benefit-icon gold" aria-hidden="true">✧</span><h3>Статус с эмодзи</h3><p>Анимированный эмодзи рядом с именем по вашему выбору.</p><span class="planned">Планируется</span></article>
      </div>
      <div class="subscription"><div><span class="card-label">ПОДПИСКА</span><p><strong><?=$premiumPrice?> ₽</strong> <span>/ месяц</span></p><small>Оформление станет доступно после подключения оплаты.</small></div><div class="subscription-actions"><label>Срок <select id="premium-duration" data-monthly-price="<?=$premiumPrice?>" aria-label="Срок подписки"><option value="1">1 месяц</option><option value="3">3 месяца</option><option value="6">6 месяцев</option><option value="12">12 месяцев</option></select></label><div class="premium-total">Итого: <output id="premium-total" aria-live="polite" for="premium-duration"><?=$premiumPrice?> ₽</output></div><button type="button" class="premium-buy" data-payment-unavailable>Оформить Premium <span aria-hidden="true">✦</span></button></div></div>
    </section>
    <section class="faq"><h2>Что нужно знать</h2><div class="faq-grid"><article><h3>Как потратить тикеты?</h3><p>Выберите подарок в каталоге и отправьте его себе или другому пользователю.</p></article><article><h3>Можно оплатить сейчас?</h3><p>Пока нет. После подключения платёжной системы здесь появится возможность оплаты.</p></article><article><h3>Когда заработает Premium?</h3><p>Подписка и её возможности находятся в разработке. О запуске сообщим отдельно.</p></article></div></section>
  </main>
  <footer class="footer"><a class="footer-brand" href="index.php">LChat <span>✦</span></a><div><a href="privacy.php">Конфиденциальность</a><a href="terms.php">Соглашение</a><a href="rules.php#refund">Правила и возврат</a><a href="mailto:admin@example.com">Поддержка</a></div></footer>
  <dialog id="payment-dialog" aria-labelledby="payment-title" aria-describedby="payment-description"><div class="dialog-inner"><button type="button" class="dialog-close" data-close-dialog aria-label="Закрыть">×</button><div class="dialog-icon" aria-hidden="true">✦</div><h2 id="payment-title">Платёжная система<br>не подключена!</h2><p id="payment-description">Покупка пока недоступна. Вернитесь позже, когда мы подключим оплату.</p><button type="button" class="dialog-ok" data-close-dialog>Понятно</button></div></dialog>
  <noscript><style>[data-payment-unavailable],.ticket-calculator,.subscription-actions{display:none}</style><p class="noscript">Оплата пока недоступна: платёжная система не подключена.</p></noscript>
</body>
</html>
