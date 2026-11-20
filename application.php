<?php
declare(strict_types=1);
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');

$versions = [
    [
        'version' => '2.2.2',
        'date' => '2026-11-20',
        'size' => '22.7 MB',
        'sha256' => '13db5337dde2e54ce4f1d39883feb3fafb0324a9d417090bd1098b61bdf7ff81',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.2.2/LChat-2.2.2-universal.apk',
        'latest' => true,
        'changes' => ['WebRTC 150.7871.08 и обновлённое подключение аудио.','Расширенная диагностика. Сборка для проверки звонков на устройстве.'],
    ],
    [
        'version' => '2.2.1',
        'date' => '2026-11-20',
        'size' => '22.7 MB',
        'sha256' => '1467890018f2fe83009a36410b95fd01d10d80af26411806ddf5265e188c105f',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/clients-2026.11.20/LChat-2.2.1-universal.apk',
        'latest' => false,
        'changes' => ['Принятие соглашения и инструкция подтверждения email.','Восстановление пароля с экрана входа.'],
    ],
    [
        'version' => '2.2.0',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => 'aafbe3a5ab22bc02b7fbd0be15a37c679c2b9e549aff2ad58cae3b354e5bb6fb',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.2.0/LChat-2.2.0-universal.apk',
        'latest' => false,
        'changes' => [
            'Нативный интерфейс чатов, групп и каналов в стиле LChat.',
            'Отдельный сворачиваемый экран звонка и список участников.',
            'Ответы, реакции, редактирование сообщений и управление сообществами.',
            'Обновление временных TURN-данных и улучшенное восстановление соединения.',
        ],
    ],
    [
        'version' => '2.1.0',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => '40a40acb686705a8be0de96b26fa0872cc68a2a94f28ef4ac491d4e0ac4af5c4',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.1.0/LChat-2.1.0-universal.apk',
        'changes' => ['Текстовые сообщения до 10 МиБ.', 'Постраничное открытие очень длинных сообщений.', 'Диагностика аудиопроцесса звонков.'],
    ],
    [
        'version' => '2.0.9',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => 'a137828c7a920392d0e29de39adfdfa0fe0137effdccb375ffa1901ef68e0259',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.0.9/LChat-2.0.9-universal.apk',
        'changes' => ['Изменена обработка аудиоэффектов на устройствах Samsung.', 'Расширена диагностика нативных сбоев WebRTC.'],
    ],
    [
        'version' => '2.0.8',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => 'e6e7dd123cd069a95a60dc58dc68b7aa88034e7be730474f18f50ccf4b28040a',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.0.8/LChat-2.0.8-universal.apk',
        'changes' => ['Добавлена расшифровка аварийного завершения аудиопроцесса.', 'Улучшен диагностический отчёт звонка.'],
    ],
    [
        'version' => '2.0.7',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => 'bfdf0d8cef94fe4082a3d5e31421d2ef1f6b789a56a8d70e6a0c556cba6cb529',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.0.7/LChat-2.0.7-universal.apk',
        'changes' => ['Стабилизация отдельного аудиопроцесса.', 'Исправление очистки состояния после завершения звонка.'],
    ],
    [
        'version' => '2.0.6',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => '5344c9c5c1c2c65c580e4b6bd3f3cefdd3bee4a0c372877eb036d15122657615',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.0.6/LChat-2.0.6-universal.apk',
        'changes' => ['Улучшен запуск фоновой аудиослужбы.', 'Добавлено восстановление состояния звонка после перезапуска приложения.'],
    ],
    [
        'version' => '2.0.5',
        'date' => '30 октября 2026',
        'size' => '22,7 МБ',
        'sha256' => 'a479780d64839680bd42f7a891ef293ad0fc6e673b849fa10aea677bfa6d9b85',
        'file' => 'https://github.com/LChatDev/LChat/releases/download/android-v2.0.5/LChat-2.0.5-universal.apk',
        'changes' => ['Ранняя версия нативного Android-клиента.', 'Чаты, каналы, голосовые сообщения и интернет-звонки.'],
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
        <p>Нативные чаты, группы, каналы, голосовые сообщения и интернет-звонки. Выберите версию и скачайте APK прямо с этого сайта.</p>
        <div class="requirements"><span>Android 8.0+</span><span>ARM64 · ARMv7 · x86 · x86_64</span></div>
      </div>
    </section>

<section class="install"><h2>LChat для Windows</h2><p>Windows 10/11 x64. Закрытие сворачивает в трей; полный выход завершает звонок. Без подписи издателя.</p><a class="download" href="https://github.com/LChatDev/LChat/releases/download/clients-2026.11.20/LChat-1.0.0-Windows-x64.exe">Скачать EXE</a></section>
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
      <ol><li>Скачайте последнюю версию APK.</li><li>Откройте файл на телефоне.</li><li>Если Android попросит разрешение, разрешите установку из этого источника.</li><li>Установите приложение поверх старой версии — аккаунт и настройки сохранятся.</li></ol>
      <p>Старые версии оставлены для диагностики. Для обычного использования выбирайте сборку с отметкой «Последняя».</p>
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
