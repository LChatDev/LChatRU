<?php
declare(strict_types=1);
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');

$isCustom = ($_GET['branch'] ?? '') === 'userhosts';
$versions = [[
 'version'=>'2.4 (240)','date'=>'2026-11-28','size'=>'489.9 KB','sha256'=>'4e9fa5836b4c3f10fa13374f933983d862ef8c0d4447572de7cac804b0b55f68','file'=>'https://data.lbs.su/lchat-apps/LChat.apk?v=240','latest'=>true,'changes'=>['APK предоставлен владельцем и опубликован без пересборки.','Подпись сверена с предыдущей опубликованной версией.'],
]];
if ($isCustom) $versions = [[
 'version'=>'1.3-custom (2.4)','date'=>'2026-11-28','size'=>'500.1 KB','sha256'=>'62f11c73e9da30b5cef4112909025adfc640275d42e605bbe75014614fc3f45c','file'=>'https://data.lbs.su/lchat-apps/userhosts/LChatCustom.apk?v=13','latest'=>true,'changes'=>['Обновление версии для своего сервера, без пересборки.','Устанавливается отдельно от обычного LChat.'],
]];

?>
<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
  <meta name="theme-color" content="#212121">
  <meta name="description" content="Приложения LChat для Android и Windows">
  <title>Приложение LChat</title>
  <link rel="icon" href="assets/favicon.svg" type="image/svg+xml">
  <link rel="stylesheet" href="assets/application.css?v=20261101-apps1">
<script src="assets/lchatai-secondary-theme.js?v=20261111-1"></script><link rel="stylesheet" href="assets/lchatai-secondary.css?v=20261111-1"></head>
<body class="lchat-secondary lchat-apps">
  <header class="topbar">
    <a class="back" href="index.php" aria-label="Вернуться в LChat">←</a>
    <a class="brand" href="index.php"><span class="brand-mark">L</span><span>LChat<small>Android и Windows</small></span></a>
  </header>
  <main>
    <section class="hero">
      <div class="app-icon" aria-hidden="true"><span>L</span></div>
      <div>
        <span class="eyebrow">ОФИЦИАЛЬНЫЕ СБОРКИ LCHAT</span>
        <h1><?= $isCustom ? 'LChat Custom — свой сервер' : 'Приложение LChat' ?></h1>
        <p>Актуальный APK LChat, предоставленный владельцем проекта. Старые сборки сняты с публикации.</p>
        <div class="requirements"><span>Android 7.0+</span><span><?= $isCustom ? 'Custom · 1.3 · code 13' : 'LChat · 2.4 · code 240' ?></span></div>
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

<?php if (!$isCustom): ?><section class="versions" aria-labelledby="windows-title"><div class="section-heading"><h2 id="windows-title">LChat для Windows</h2></div><article class="version-card"><div class="version-main"><div class="version-number"><span>Windows · установщик</span><b>Обновление</b></div><p class="meta">2026-11-28 · 107.9 MB</p><p>Распакуйте ZIP и запустите установщик EXE.</p></div><div class="version-actions"><a class="download" href="https://data.lbs.su/lchat-apps/windows/LChat-Windows-Setup.zip?v=20261128" download>Скачать ZIP ↓</a><button class="checksum" type="button" data-hash="232527dc5439fb03bd90df724d27fb25d6f371b6c0ec3bc4e7197645e56c3320">SHA-256</button></div></article><article class="version-card"><div class="version-main"><div class="version-number"><span>Windows · без установки</span><b>Обновление</b></div><p class="meta">2026-11-28 · 97.9 MB</p><p>Распакуйте ZIP и запустите EXE из полученной папки.</p></div><div class="version-actions"><a class="download" href="https://data.lbs.su/lchat-apps/windows/LChat-Windows-Portable.zip?v=20261128" download>Скачать ZIP ↓</a><button class="checksum" type="button" data-hash="ab6e7f852df0cb76d761a385a469da96e3b0e4c76fee859483af0a43545a0833">SHA-256</button></div></article></section><?php endif; ?>
<?php if ($isCustom): ?><section class="versions" aria-labelledby="custom-windows-title"><div class="section-heading"><h2 id="custom-windows-title">LChat Custom для Windows</h2></div><article class="version-card"><div class="version-main"><div class="version-number"><span>Custom Windows · установщик</span><b>Новая версия</b></div><p class="meta">2026-11-28 · 107.9 MB</p><p>Распакуйте ZIP и запустите EXE. Адрес сервера выбирается при первом запуске; смена сервера доступна через трей.</p></div><div class="version-actions"><a class="download" href="https://data.lbs.su/lchat-apps/userhosts/windows/LChatCustom-Windows-Setup.zip?v=20261128" download>Скачать ZIP ↓</a><button class="checksum" type="button" data-hash="2a14f8d200b6ff4a881e43be2c54931aa588a8c5b145056fa2e6a9389f1970aa">SHA-256</button></div></article><article class="version-card"><div class="version-main"><div class="version-number"><span>Custom Windows · без установки</span><b>Новая версия</b></div><p class="meta">2026-11-28 · 97.9 MB</p><p>Распакуйте ZIP и запустите EXE. Адрес сервера выбирается при первом запуске; смена сервера доступна через трей.</p></div><div class="version-actions"><a class="download" href="https://data.lbs.su/lchat-apps/userhosts/windows/LChatCustom-Windows-Portable.zip?v=20261128" download>Скачать ZIP ↓</a><button class="checksum" type="button" data-hash="4d4d749cdbc5c2d18486ec90ec825cc8aa3825c10207e979511f22236a7bd0a1">SHA-256</button></div></article></section><?php endif; ?>
    <section class="install">
      <h2>Как установить</h2>
      <ol><li>Скачайте последнюю версию APK.</li><li>Откройте файл на телефоне.</li><li>Если Android попросит разрешение, разрешите установку из этого источника.</li><li><?php if ($isCustom): ?>Custom — отдельное Android-приложение. Обычный LChat удалять не нужно: они могут быть установлены одновременно.<?php else: ?>LChat 2.4 можно установить поверх предыдущей версии с той же подписью. Очень старые сборки с другой подписью могут потребовать удаления приложения; сначала сохраните нужные локальные данные.<?php endif; ?></li></ol>
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
