# Исходники Telegram Web A в LChat

LChat — самостоятельный PHP-мессенджер. Он не подключается к аккаунтам и серверам Telegram.

Следующие компоненты адаптированы из [официальных исходников Telegram Web A](https://github.com/TelegramOrg/Telegram-web-z), коммит `f67a40879a7d78c2683ec5a296cf55bc22417d56`:

- `assets/weba-icons.woff2` скопирован из `src/styles/icons.woff2`.
- `assets/weba-pattern.svg` скопирован из `src/assets/pattern.svg`.
- `assets/weba-fonts/` содержит CSS Roboto и подмножества WOFF2 из `src/assets/fonts/`.
- Коды иконок в `assets/app.js` и `assets/weba-ui.js` основаны на `src/styles/icons.scss`.
- Цвета, заголовок 64 px, область сообщений 760 px, ширина сообщения до 464 px, поле ввода 48 px, радиус сообщения 15 px и времена анимаций в `assets/weba.css` основаны на `src/styles/index.scss`, `src/styles/_variables.scss`, `src/styles/_composer-primitives.scss` и стилях компонентов. Они адаптированы к интерфейсу и функциям LChat.

Telegram Web A распространяется под GNU General Public License версии 3; текст лицензии приведён в [LICENSE](LICENSE). Эти исходники LChat распространяются под GPL-3.0. Названия и логотипы Telegram не используются в оформлении LChat.
