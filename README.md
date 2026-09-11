# Holfuy Upgrader

![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

Holfuy Upgrader is an Android application for updating the firmware
of [Holfuy](https://holfuy.com/) weather stations over USB.

The application communicates directly with the station's built-in Nuvoton ISP
(In-System Programming) bootloader and allows firmware updates to be performed
from an Android phone or tablet using a USB OTG connection.

Copyright © 2026 John R. Wolfe

Licensed under the Apache License, Version 2.0. See the LICENSE file for details.

## Screenshots

| | | |
|:-:|:-:|:-:|
| <img src="play-store-assets/screenshots/phone/02-Attached.jpg" width="250"> | <img src="play-store-assets/screenshots/phone/03-USB_PermissionRequest.jpg" width="250"> | <img src="play-store-assets/screenshots/phone/05-FirmwareSelected.jpg" width="250"> |
| **Station Detected** | **USB Permission** | **Ready to Update** |

## Features

* Firmware updates over USB
* Modern Jetpack Compose user interface
* Automatic detection of supported Holfuy weather stations
* Compatible with Android 8.0 (API 26) and later
* Tested on Android 8.1, 12, 15, and 16

## Requirements

- Android device with USB OTG support
- USB OTG adapter (if required by your device)
- USB cable with a USB Micro-B connector for the weather station
- Firmware image supplied by Holfuy

## Building a Development APK

This project is built with Android Studio using Kotlin and Jetpack Compose.
Clone the repository and open it with a recent version of Android Studio.
The project uses the Gradle Wrapper, so no separate Gradle installation is required.

## Building a Release Bundle

To build a signed Android App Bundle for Google Play:

```bash
./gradlew clean bundleRelease
```

The bundle will be written to:

```
app/build/outputs/bundle/release/
```

## User Guide

Detailed operating instructions are available in the project's
[User Guide](app/src/main/assets/UserGuide.md).

The same guide is also included in the application and can be accessed by tapping **Help** on the main screen.

## Acknowledgements

This application communicates with the Nuvoton ISP (In-System Programming)
bootloader used by Holfuy weather stations.

The implementation in `protocol/HEXTool.kt`, `protocol/ISPCommands.kt`, and
`protocol/ISPManager.kt` originated from the
[`OpenNuvoton/NuISPTool-Android`](https://github.com/OpenNuvoton/NuISPTool-Android)
project and has been substantially modified and extended during development of
this application.

The upstream project is licensed under the Apache License 2.0.

Special thanks to Holfuy for their assistance during development and testing.

Development of this project was substantially assisted by OpenAI ChatGPT (GPT-5.5).
The author reviewed, tested, and integrated all generated code.

## Contributing

Please include the Android version, device model, and application log when reporting problems.


Pull requests are welcome.
Please open an issue before beginning work on significant new features or architectural
changes so that we can discuss the proposed approach.

## Privacy Policy

Holfuy Upgrader does not collect, store, transmit, or share personal information.

For complete details, see [PrivacyPolicy.md](PrivacyPolicy.md).

## Disclaimer

Firmware updates modify the software stored on the weather station.

Ensure that you are using firmware supplied for your specific Holfuy device.

The authors and contributors are not responsible for damage resulting from
the installation of incorrect firmware or interruption of the firmware update process.