<?php
declare(strict_types=1);
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');

$isCustom = ($_GET['branch'] ?? '') === 'userhosts';
$versions = [
    [
        'version' => '2.2 (220)',
        'date' => '2026-11-26',
        'size' => '486.0 KB',
        'sha256' => '10bc1cda1585160d771b6808810371ae800182c46504ce7884ba505e5fb8e5f3',
        'file' => 'https://data.lbs.su/lchat-apps/LChat.apk?v=220',
        'latest' => true,
        'changes' => ['Файл предоставлен владельцем и загружен без пересборки.','Обновляется поверх LChat 2.1 с той же подписью.'],
    ],
];
if ($isCustom) $versions = [[
    'version' => '1.1-custom (2.2)',
    'date' => '2026-11-26',
    'size' => '496.1 KB',
    'sha256' => '5f2a06eb6f076587f00d0d8338eef6b82a18efb12ed194c2db77e41cb2545c15',
    'file' => 'https://data.lbs.su/lchat-apps/userhosts/LChatCustom.apk?v=11',
    'latest' => true,
    'changes' => ['APK для пользовательских серверов LChat, предоставленный владельцем.','Отдельное приложение: можно установить рядом с обычным LChat.'],
]];

?>
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
  <meta name="theme-color" content="#212121">
  <meta name="description" content="Версии приложения LChat для Android">
  <title>Приложение LChat</title>
  <link rel="icon" href="assets/favicon.svg" type="image/svg+xml">
  <link rel="stylesheet" href="assets/application.css?v=20261101-apps1">
<script src="assets/lchatai-secondary-theme.js?v=20261111-1"></script><link rel="stylesheet" href="assets/lchatai-secondary.css?v=20261111-1"></head>
<body class="lchat-secondary lchat-apps">
  <header class="topbar">
    <a class="back" href="index.php" aria-label="Вернуться в LChat">←</a>
    <a class="brand" href="index.php"><span class="brand-mark">L</span><span>LChat<small>Приложение для Android</small></span></a>
  </header>
  <main>
    <section class="hero">
      <div class="app-icon" aria-hidden="true"><span>L</span></div>
      <div>
        <span class="eyebrow">ОФИЦИАЛЬНЫЕ СБОРКИ LCHAT</span>
        <h1><?= $isCustom ? 'LChat Custom — свой сервер' : 'Приложение LChat' ?></h1>
        <p>Актуальный APK LChat, предоставленный владельцем проекта. Старые сборки сняты с публикации.</p>
        <div class="requirements"><span>Android 7.0+</span><span><?= $isCustom ? 'Custom · 1.1 · code 11' : 'LChat · 2.2 · code 220' ?></span></div>
      </div>
    </section>

<nav aria-label="Варианты приложения" style="display:flex;flex-wrap:wrap;gap:12px;margin:24px 0"><a class="download" href="application.php" <?= !$isCustom ? 'aria-current="page"' : '' ?>>Обычная версия</a><a class="download" href="application.php?branch=userhosts" <?= $isCustom ? 'aria-current="page"' : '' ?>>Свой сервер</a></nav>
<section class="versions" aria-labelledby="versions-title">
      <div class="section-heading"><div><span class="eyebrow">ИСТОРИЯ ВЕРСИЙ</span><h2 id="versions-title">Доступные сборки</h2></div><span><?= count($versions) ?> версий</span></div>
      <?php foreach ($versions as $index => $item): ?>
        <article class="version-card<?= !empty($item['latest']) ? ' latest' : '' ?>">
          <div class="version-main">
            <div class="version-number"><span><?= htmlspecialchars($item['version']) ?></span><?php if (!empty($item['latest'])): ?><b>Последняя</b><?php else: ?><b class="archive">Архив</b><?php endif; ?></div>
            <p class="meta"><?= htmlspecialchars($item['date']) ?> · <?= htmlspecialchars($item['size']) ?></p>
            <ul><?php foreach ($item['changes'] as $change): ?><li><?= htmlspecialchars($change) ?></li><?php endforeach; ?></ul>
          </div>
          <div class="version-actions">
            <a class="download" href="<?= htmlspecialchars($item['file']) ?>" download>Скачать APK <span>↓</span></a>
            <button class="checksum" type="button" data-hash="<?= htmlspecialchars($item['sha256']) ?>" aria-label="Скопировать SHA-256 версии <?= htmlspecialchars($item['version']) ?>">SHA-256</button>
          </div>
        </article>
      <?php endforeach; ?>
    </section>

    <section class="install">
      <h2>Как установить</h2>
      <ol><li>Скачайте последнюю версию APK.</li><li>Откройте файл на телефоне.</li><li>Если Android попросит разрешение, разрешите установку из этого источника.</li><li><?php if ($isCustom): ?>Custom — отдельное Android-приложение. Обычный LChat удалять не нужно: они могут быть установлены одновременно.<?php else: ?>LChat 2.2 можно установить поверх версии 2.1. Очень старые сборки с другой подписью могут потребовать удаления приложения; сначала сохраните нужные локальные данные.<?php endif; ?></li></ol>
      <p>Доступны обычный LChat и отдельная версия Custom для своих серверов. Старые сборки сняты с публикации.</p>
    </section>
  </main>
  <div id="toast" role="status" aria-live="polite" hidden>SHA-256 скопирован</div>
  <script>
  document.querySelectorAll('.checksum').forEach(button => button.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(button.dataset.hash);
      const toast = document.getElementById('toast'); toast.hidden = false;
      clearTimeout(window.lchatToastTimer); window.lchatToastTimer = setTimeout(() => toast.hidden = true, 1800);
    } catch { window.prompt('SHA-256', button.dataset.hash); }
  }));
  </script>
</body>
</html>
