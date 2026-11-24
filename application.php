<?php
declare(strict_types=1);
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');

$versions = [
    [
        'version' => '2.1 (210)',
        'date' => '2026-11-24',
        'size' => '484.7 KB',
        'sha256' => '0f995e3e153f9034c59e74bc115108c152714590bd02b4f9850521dd6a8d18d1',
        'file' => 'https://data.lbs.su/lchat-apps/LChat.apk',
        'latest' => true,
        'changes' => ['Файл предоставлен владельцем и загружен без пересборки.','Подпись отличается от прежних сборок: установка поверх них невозможна.'],
    ],
];
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
        <h1>Приложение LChat</h1>
        <p>Актуальный APK LChat, предоставленный владельцем проекта. Старые сборки сняты с публикации.</p>
        <div class="requirements"><span>Android 7.0+</span><span>APK · 2.1 · code 210</span></div>
      </div>
    </section>

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
      <ol><li>Скачайте последнюю версию APK.</li><li>Откройте файл на телефоне.</li><li>Если Android попросит разрешение, разрешите установку из этого источника.</li><li>У этого APK другой ключ подписи. Сохраните нужные локальные данные и удалите старое приложение перед установкой; Android не позволяет обновление с другим ключом.</li></ol>
      <p>Опубликован только предоставленный APK. Старые сборки больше не доступны для скачивания.</p>
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
