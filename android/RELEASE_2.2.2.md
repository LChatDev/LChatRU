# Android 2.2.2 — call-engine candidate / Сборка для проверки звонков

The supplied Samsung SM-N975F / Android 12 report shows the audio process exiting with `reason=2 status=6` after `MIC_RUNNING`. Room admission and foreground-service startup succeeded. Signal 6 indicates SIGABRT; the report does not identify the exact native assertion. A TURN configuration change alone cannot be assumed to fix this process abort.

Changes:
- Upgrade the bundled WebRTC Java classes and all four JNI libraries together from 150.7871.01 to 150.7871.08.
- Use unified-plan `addTrack` on both negotiation sides instead of manually accessing/mutating JNI transceiver wrappers when answering.
- Log peer creation, audio attachment, local/remote SDP stages, peer connection states and poll exceptions without SDP, cookies, participant IDs or TURN credentials.
- Attempt native trace extraction for signaled SIGABRT exits as well as native-crash exits.

Upstream release: https://github.com/webrtc-sdk/android/releases/tag/v150.7871.08 . It includes an audio media-engine lifetime correction: https://github.com/livekit/libwebrtc/pull/333 . This is relevant maintenance, not proof that the reported Samsung abort has the same cause.

Build: run `python android/setup-tools.py`, then `android/build.ps1` with `-WebRtcDir android/sdk/webrtc-150.7871.08` and the existing JDK/platform/build-tools parameters. Android package `su.lbs.lchat`, version code 24, minimum API 26. Signing certificate retained. Checks: compilation, D8, signatures, alignment, ZIP/DEX integrity, four native ABIs. No attached ADB device was available. A successful call on the affected phone is still required before calling this crash resolved.

## Проверка на телефоне

Установите 2.2.2 поверх 2.2.1. Проверьте исходящий и входящий звонок, звук в обе стороны, сворачивание приложения и завершение разговора. Если снова выйдет из звонка, отправьте новый отчёт: он покажет, успел ли движок создать соединение, прикрепить микрофон и обработать SDP. Существующие записи диагностики могут оставаться в отчёте — ориентируйтесь на события после `APP_OPEN_2.2.2`.

Обновлены WebRTC и способ подключения аудиодорожки. Это проверяемая попытка устранить нативный сбой; работа звонков на вашем устройстве пока не подтверждена.
