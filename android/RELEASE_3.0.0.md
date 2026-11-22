# LChat Android 3.0.0 — native calls rebuilt

## Confirmed crash cause

The old 2.2.2 APK was installed and exercised on an Android 12 / API 31 x86_64 emulator. It reproduced the reported SIGABRT after microphone startup. Logcat showed `SecurityException: ... android.permission.ACCESS_NETWORK_STATE` in WebRTC's `NetworkMonitor`, followed by the fatal JNI check in `jvm.cc`. The missing manifest permission was the confirmed cause of this reproduced abort. A Samsung-specific audio-effects workaround or a library version change alone did not address it.

The new manifest includes the normal `ACCESS_NETWORK_STATE` permission. It is granted at installation; no runtime settings change is required. The service checks its required permissions before entering JNI, and the build rejects a manifest missing the required call permissions.

## New architecture

- `CallService`: Android foreground lifecycle, audio permissions, notification controls and Binder snapshots.
- `CallEngine`: one serial native RTC thread; asynchronous callbacks belong to one session and cannot revive a stopped call.
- `CallTransport`: independent HTTP workers for admission/heartbeat and outgoing signaling. Network delays no longer block audio operations.
- `CallAudioRouter`: audio focus, earpiece/speaker/Bluetooth routing and wake-lock cleanup.
- `CallLifecycle`: explicit start, admission, connection, recovery and terminal states.
- `CallOutbox`: bounded, ordered signal batches with retry and server size/count limits.
- `CallLease`: atomic file journal with cross-process locking; no SharedPreferences cache as the source of call admission and no 15-second screen acknowledgement.
- `CallClient`: state subscription without automatic audio-process restart loops.

Native Java screens and WebRTC voice calls are retained. No WebView call page, Telecom account, new backend service or server configuration change is required. Pixel UI, chats, groups, channels, voice messages and files are retained.

## Verified

- Old APK abort reproduced on Android 12 and identified from real logcat.
- New APK installed on the same API 31 emulator with the existing signing certificate.
- Native Android ↔ Chromium audio RTP in both directions through the existing server's forced TURN relay policy.
- Microphone mute/unmute, speaker/earpiece controls, duplicate acceptance, background and screen-off call continuity.
- ICE restart, hangup, return to the app and another call through the incoming-accept intent.
- Journal removal and immediate server account state `idle` after hangup.
- Lifecycle/outbox host-JVM regression tests, negative missing-permission build test, APK signatures, alignment, ZIP/DEX integrity and all four ABIs.

Hardware-independent tests ran on x86_64 emulation. Samsung hardware audio quality, Bluetooth headsets, deep Doze/battery restrictions, physical ARM device installation and Android versions other than API 31 were not device-tested. The emulator verifies an actual native call and RTP flow, not a claim about microphone/speaker acoustics on every phone.

## Install

Install `LChat-3.0.0-universal.apk` over the previous official version. Package `su.lbs.lchat`, version code 30, Android 8.0+ (API 26), ARM64/ARMv7/x86/x86_64. Keep microphone and notification permissions enabled. Incoming notifications use server polling rather than FCM; Android can delay them in deep sleep. The active call uses a visible microphone foreground service.

---

# LChat Android 3.0.0 — звонки переработаны

Причина воспроизведённого вылета найдена на Android 12: в старом APK отсутствовало разрешение `ACCESS_NETWORK_STATE`. Монитор сети WebRTC получал исключение и аварийно завершал процесс. Разрешение добавлено; сборка и служба теперь проверяют обязательные разрешения.

Заново разделены служба Android, нативное аудио, HTTP-сигнализация и маршрутизация звука. Убраны ожидание подтверждения от экрана и циклы перезапуска аудиопроцесса. Подключение записывается в атомарный журнал, который согласован между процессами. Завершение и поздние ответы сервера не запускают старый звонок снова.

Проверены реальные аудиопакеты в обе стороны через TURN, микрофон, динамик, повторное принятие, фон, погашенный экран, восстановление ICE, завершение и повторный звонок. После выхода сервер сразу показывал аккаунт свободным. Проверки выполнены на эмуляторе Android 12; физический Samsung в этой среде недоступен. Подпись сохранена, устанавливайте поверх предыдущего APK.
