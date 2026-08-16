## Requirements

To use this tool, you will need:

* An Android phone or tablet with USB OTG (On-The-Go) capability.  OTG enables a phone or tablet to act as a host computer when connected to a weather station.
* A USB OTG adapter if your device does not have native USB host capability available through its connector.
* A USB cable with a USB-C connector for the Holfuy station (or micro-B connector for V5.X and older main board) and a connector compatible with your Android device (USB-A or USB-C).

## Preparation

While your Android device has an Internet connection, follow the relevant procedure below.

### First-time use

1. Open the app.
1. Follow the instructions to configure the firmware repository.
1. Wait 10 seconds for the firmware repository to be populated.

## Firmware Update Procedure

1. Tap **Select Firmware** and select an appropriate firmware for your weather station from the list or tap **Browse** and navigate to and select a firmware file residing outside the firmware repository.
1. Turn off the Holfuy station.
1. Disconnect the solar panel from the station main board.
1. Connect the station to the Android device using the USB cable and OTG adapter if required.
1. Turn on the station, and within 45 seconds execute each of these steps:
   - Tap **Connect**.
   - When Android requests permission to access the USB device, grant permission.
   - Tap **Update Firmware**.
1. Wait for the update to complete. Do not disconnect the Android device from the station during the update process.
1. Turn off the station.
1. Disconnect the USB cable.
1. Reconnect the solar panel to the station.
1. Turn on the station.

The station should now be running the new firmware.

## Firmware Repository

The firmware repository contains firmware files downloaded from Holfuy.
You may also put custom firmware files into the firmware repository.

The firmware repository is configured once, during the first use of the app after installation.
The app suggests an appropriate folder to contain the repository, but you may choose any folder to which you have sufficient permissions.
For example, the Android framework typically will not allow you to choose a system folder like `Download`, but it will allow you to 
create one within that folder, such as `Download/HolfuyFirmware`.

If you need to re-configure the firmware repository, clear the app's data and then open the app again.

Each time the app is opened, it attempts to update the firmware repository, and this requires an Internet connection.
Typically, this process takes only a few seconds.
A message indicates when the repository was last successfully checked.
If a failure has occurred since that last successful check, a separate message indicates when the failure occurred.
Typically, a failure to update the repository is caused by a network issue, so if one occurs, ensure your Android 
device has an Internet connection.

After each successful check of the repository, the disposition of each file in the repository is updated:

- Current:  The file in the repository is the most recent one provided by Holfuy.
- Outdated:  The file in the repository was Current when it was downloaded, but a newer one is available from Holfuy but was not successfully downloaded.
- Missing:  Holfuy supplies this file, but it is not present in the repository and was not successfully downloaded during the most recent attempt.
- Custom:  The file exists in the repository, but it is not one Holfuy supplies to general users.

Since the app attempts to update the repository every time it is opened, any file marked as **Outdated** or **Missing** will be replaced with
a **Current** version whenever the app is opened when the Android device has an Internet connection.

## Troubleshooting Firmware Update

First, ensure the station is connected through a USB OTG-capable connection.
Some Android devices with native OTG capability require a change in the settings to enable OTG.

This app communicates with the bootloader.

When the station is powered on while connected to an Android device via USB, the bootloader starts a timer.
After roughly one minute of inactivity from the app, the bootloader disconnects from the Android
device and boots the existing application firmware.

If the LEDs on the main board are flashing, the bootloader is no longer running.

If the **Connect** or **Update Firmware** buttons are not enabled when you
expect them to be enabled, the most likely cause is that the bootloader timer expired, and the
simplest thing to try is:

1. Turn off the station.
1. Wait two seconds.
1. Turn on the station.  
1. Continue with the process described above, starting with tapping **Connect**.

### Firmware update fails

Verify that:

* The selected firmware file is intended for your station.
* The USB cable is securely connected.
* The station remains connected to the Android device throughout the update process.