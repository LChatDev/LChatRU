# LChat for Windows

Windows 10/11 x64 desktop client for https://chat.lbs.su, built with Electron. It displays the current web interface. The Android client uses separate native Java screens.

Download `LChat-1.0.0-Windows-x64.exe` from the [release](https://github.com/LChatDev/LChat/releases/tag/clients-2026.11.20). This is a portable executable; Node.js is not required. The EXE has no publisher signing certificate. Sessions and preferences stay in the Windows user profile. Internet access is required.

Closing the window keeps LChat in the system tray. Double-click its icon to reopen it. Use the tray's Exit command to stop the app and any active call. Microphone, camera and notification access require confirmation. No automatic startup is installed. External links require confirmation and open in the browser. Screen sharing is not implemented in this desktop wrapper yet.

## Build

Install Node.js with npm. From `desktop`:

```powershell
npm ci
npm test
npm run build
```

Output: `dist/LChat-1.0.0-Windows-x64.exe`. Dependencies are pinned in `package-lock.json`. Remote content has no Node.js integration and runs with sandbox and context isolation. No server configuration or credentials are packaged.

## Release checks

Navigation/permission policy tests, packaged application startup, live login page loading, Node.js isolation and screenshot capture were checked. Signed-in messaging, two-device calling, camera and prolonged Windows sleep were not tested here. Server UI changes appear after reloading; Electron runtime updates require a new build.
