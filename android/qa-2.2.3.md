# QA 2.2.3 (APKMaster)

| Уровень | Результат |
| --- | --- |
| Compile | OK (javac --release 8, 3 deprecation warnings) |
| Package | OK universal APK (arm64-v8a, armeabi-v7a, x86, x86_64) |
| Sign | OK apksigner v2/v3 |
| Align | OK zipalign -P 16 |
| Version | 2.2.3 / code 25 |
| Device install | Не прогонялось в этой сессии (нет эмулятора/устройства в проверке) |

Визуальные изменения проверены по исходникам: pixel palette, wallpaper, icon, радиусы.
