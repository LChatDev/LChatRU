# Fork appearance / Оформление своей сборки

Each frontend loads assets/custom.css after its standard styles. The file contains a disabled example, so the default look stays intact until you uncomment the example. Edit that file in your fork and refresh the browser.

Каждая веб-версия загружает assets/custom.css после основных стилей. Раскомментируйте пример и задайте свои значения. Можно менять акцент кнопок, ширину списка чатов на ПК, размер текста, скругления сообщений и фон переписки. В примере отдельно задан фон светлой темы. Свой фон можно положить рядом с CSS и подключить через `url("./wallpaper.webp")`.

On split deployments, edit the CSS in LChatWeb. Site name and server options belong in LChatHost. Verify the theme in light and dark modes on desktop and mobile. Use the bundled Codex skill at .agents/skills/lchat-maintainer/SKILL.md to guide changes.

При раздельном размещении стили меняются в LChatWeb. Название сайта и параметры сервера настраиваются на стороне LChatHost. Проверьте читаемость в обеих темах. Для работы через Codex откройте репозиторий и вызовите `$lchat-maintainer`.
