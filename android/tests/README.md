# Native call regression tests

The distributed APK contains no instrumentation runner or test credentials. Use an isolated test account and a private test room on a server you control.

## Pure Java checks

Compile the actual production `CallLifecycle.java`, `CallOutbox.java` and `tests/CallCoreTest.java` with a JDK, then run `su.lbs.lchat.CallCoreTest`. Checks cover duplicate admission, cancellation during join, terminal callbacks, reconnect clock, idempotent hangup and ordered/bounded signaling retries.

```powershell
New-Item -ItemType Directory build/core-tests -Force
javac -encoding UTF-8 -d build/core-tests app/src/main/java/su/lbs/lchat/CallLifecycle.java app/src/main/java/su/lbs/lchat/CallOutbox.java tests/CallCoreTest.java
java -cp build/core-tests su.lbs.lchat.CallCoreTest
```

## Actual Android ↔ browser scenario

1. Build and install the app on a private API 31 emulator. The test runner uses the same signing key as that app. `setup-emulator.py` optionally downloads checksum-verified Google emulator tools and an API 31 ATD image into `sdk/`; configure an AVD using these packages, or use Android Studio. ATD is a headless functional test image; it does not replace visual or hardware QA.
2. Obtain a session for a dedicated QA user and create a public test call room. Keep its 48-character room token and cookie private. Write `tests/lchat-call-scenario.json` with `base`, `cookie` and `token`. This file is ignored by Git. The browser participant joins as a guest in that public room.
3. In `tests/qa`, run `npm ci`, then `node browser-peer.cjs`. It uses a fresh headless Chrome profile and fake microphone input. This does not access your real browser account or microphone.
4. Build `tests/build-instrumentation.ps1 -JavaHome <JDK>`. It uses the latest `build/work-*` app JAR; pass `-AppWork` to choose explicitly. The test APK is written to `build/instrumentation/tests.apk` unless `-QaOutput` is supplied.
5. Install it on the test emulator, grant the target app `RECORD_AUDIO`, and push the JSON configuration to `/data/local/tmp/lchat-call-scenario.json`.
6. Run `adb shell am instrument -w -r su.lbs.lchat.tests/su.lbs.lchat.CallScenarioRunner`. Expected result: `PASS: real Android native call scenario`, `INSTRUMENTATION_CODE: -1` (Android's successful Activity result).
7. Place `tests/qa.stop` to stop the browser peer. Remove the temporary QA account/room, local session file and emulator-side JSON after testing.

The scenario asserts bidirectional audio RTP counters, mute/unmute, speaker routes, duplicate acceptance without a new server participant, background/screen-off continuity, ICE restart, lease removal, server `idle`, repeat entry and the incoming-accept Activity intent. It does not prove acoustic quality or delivery of incoming polling notifications through Android deep Doze.

## APK checks

```powershell
node tests/apk-integrity.cjs LChat-3.0.0-universal.apk
```

`build.ps1` verifies signatures and alignment, including Android 12/12L. It rejects missing native-call permissions before packaging. Keep the signing directory private.
