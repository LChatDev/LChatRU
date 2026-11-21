# LChat Android 2.2.3

Пиксельный визуальный релиз нативного клиента.

## Изменения
- Палитра Pixel: фон `#070c26`, поверхности `#0a1030`, акцент `#638dff`, исходящие пузыри `#0638c8`
- Пиксельные радиусы (6–8 dp) вместо «телеграмных» скруглений
- Пиксельные обои чата (клетчатый паттерн)
- Новая векторная иконка приложения
- Версия: `2.2.3` / `versionCode 25`

## Сборка
```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1 `
  -JavaHome "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot" `
  -BuildTools "C:\LChat\android\sdk\build-tools\android-15" `
  -AndroidJar "C:\LChat\android\sdk\platform\android-35\android.jar" `
  -WebRtcDir "C:\LChat\android\sdk\webrtc-150.7871.08"
```

Артефакты: `LChat.apk`, `LChat-2.2.3-universal.apk`, `.sha256`
