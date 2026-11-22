# Android 3.0.0 verification — 2026-11-22

Environment: Google Android 12 / API 31 ATD x86_64, Windows WHPX; signed universal production APK; isolated synthetic account on the existing PHP server; headless Chrome with fake audio. The actual join response enforced TURN (`ice_transport_policy=relay`).

## Reproduced original defect

The unmodified 2.2.2 APK aborted immediately after microphone startup. Real logcat:

```text
java.lang.SecurityException: ConnectivityService: ... android.permission.ACCESS_NETWORK_STATE
org.webrtc.NetworkMonitorAutoDetect$ConnectivityManagerDelegate.getNetworkState
org.webrtc.NetworkMonitor.startMonitoring
Fatal error in ... sdk/android/src/jni/jvm.cc, line 81
Fatal signal 6 (SIGABRT) ... network_thread
```

The complete raw log is retained locally; it is not distributed. The production manifest now requests ACCESS_NETWORK_STATE. The build rejects a copy of the manifest with that permission removed, and the service validates required permissions before initializing native media.

## Final production APK scenario

[Actual instrumentation output](tests/results/api31-3.0.0.txt)

- Dedicated QA session authenticated and a native foreground call was admitted.
- Bidirectional RTP audio counters exceeded ten packets in each direction.
- Duplicate acceptance retained the same server participant.
- Microphone mute/unmute and speaker/earpiece state checks passed.
- The Activity was confirmed paused in the background; RTP continued.
- The screen was confirmed non-interactive for eight seconds; RTP continued and wake-up succeeded.
- Explicit ICE restart preserved incoming RTP.
- Hangup removed the atomic lease and the API immediately returned account state idle.
- Returning to the app and a second call through the incoming-accept intent passed.
- Cycle 1: 1000 RTP packets sent / 999 received; cycle 2: 241 sent / 295 received.
- No native abort of the LChat app/call process occurred. During the rapid cancellation test, the ATD emulator vendor audio HAL logged an `Assertion failed: !mSource` in `/vendor/bin/hw/android.hardware.audio.service`; the app remained alive and the scenario completed successfully. This emulator-specific vendor event is retained in the local test record.
- Cancellation during admission also returned the server account to `idle`.

## Build and regression checks

Host-JVM lifecycle/outbox checks passed: duplicate start, cancelled join, late callbacks after hangup, reconnect clock, idempotent stop, FIFO retries, packet-size/batch-count/capacity bounds. The negative missing-permission build check passed. APK signatures (including Android 12/12L), alignment, ZIP CRC, DEX checksums and four native ABI packages passed. The signing certificate matches the previous official APK.

APK SHA-256: `e2590a74c8f70e2b3e6354cdc28c9c9384d0a2948183009bf23da0c3e015e158`.

Limits: automated functional emulator tests are not Samsung hardware, acoustic-quality, headset, deep-Doze or visual-layout certification. ARM libraries were verified as packaged artifacts; ARM phones and other API levels were not device-tested. No real user chats or call contacts were used.

---

Проверка выполнена на настоящей среде Android 12 в эмуляторе: старый вылет воспроизведён, причина установлена по logcat. Новый APK успешно обменивался аудиопакетами через TURN, продолжал разговор в фоне и с выключенным экраном, переключал микрофон/динамик, восстанавливал ICE и освобождал серверное состояние. Повторный вход и принятие через входящий intent прошли. Подробный вывод теста приложен выше. Физический Samsung, акустика и глубокий Doze отдельно не проверялись.
