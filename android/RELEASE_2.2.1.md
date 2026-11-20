# LChat Android 2.2.1

Native Java interface. Android 8.0+; ARMv7, ARM64, x86 and x86_64. Package `su.lbs.lchat`, version code 23. Default server: https://chat.lbs.su/.

Registration now submits explicit agreement acceptance and displays the email verification step. A password reset link is available on the login screen. Existing native chats, groups, channels, files, voice messages and call implementation are retained. This update does not claim to fix every device-specific WebRTC crash.

Built using JDK 21, Google Android Platform 35 / Build Tools 35.0.0 and WebRTC 150.7871.01. The APK has the same signing certificate as 2.2.0, so it can update the previous official installation. Verified: package/version/min SDK, APK signatures including API 31–32, ZIP alignment, ZIP CRC, DEX checksums and all four ABI libraries. No phone was attached: installation, email registration completion and live/background calls were not device-tested.

To build, run `python android/setup-tools.py`, then `android/build.ps1` with your JDK, extracted SDK paths and WebRTC directory. Keep your own signing key private. The SDK downloader uses Google/Maven checksums; do not include `sdk/`, `build/` or `signing/` in Git.

---

Нативный клиент Java для Android 8.0+. Добавлены принятие соглашения, понятный шаг подтверждения email и ссылка восстановления пароля. Подпись сохранена: APK устанавливается поверх 2.2.0. Проверены подпись, структура APK, DEX и четыре архитектуры. Физическая установка и звонки на телефоне не проверялись. Изменения не заявлены как исправление всех аппаратных сбоев WebRTC.
