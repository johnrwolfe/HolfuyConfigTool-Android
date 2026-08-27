# Holfuy Upgrader Test Cases

This document defines the executable manual test cases for Holfuy Upgrader.

Unlike many manual test suites, only one test case contains a complete user
procedure. All remaining test cases are defined as intentional variations of
that reference procedure.

This organization minimizes duplicated procedures while providing systematic
coverage of the application's behavior.

---

# Test Case Format

Each test case consists of the following sections.

| Section | Purpose |
|----------|---------|
| ID | Unique test identifier |
| Title | Short descriptive name |
| Reference Workflow | Typically WF-001 |
| Classification | Smoke, Regression, Compatibility, Exploratory |
| Preconditions | Conditions required before executing the test |
| Variation | Interruption Point and Variation Specification |
| Expected Results | Observable system behavior |
| Regression History | GitHub issue(s), if applicable |

---

# Reference Workflows

## WF-001 — Update Firmware

This is the normal firmware update workflow described in the User Guide.

**Procedure**

Execute the firmware update procedure exactly as described in the User Guide.

**Expected Results**

- A firmware image can be selected before connecting to the weather station.
- USB permission is granted.
- Connection succeeds.
- Firmware update completes successfully.
- Progress indication reaches completion.
- Application remains responsive.
- No crash or "Application Not Responding" (ANR) occurs.
- The station boots the newly installed firmware.

---

## WF-003 — Configure Firmware Repository

This workflow is executed during the first invocation of the app after
installation, when no firmware repository directory has yet been configured.

**Procedure**

1. Launch the app.
2. When prompted, select a directory to contain the firmware repository.
3. If necessary, create the selected directory.
4. Complete the repository configuration.

The application suggests `Download/HolfuyFirmware` as the repository location,
but the user may select any directory permitted by the Android framework.

**Expected Results**

- The firmware repository directory is configured successfully.
- The application proceeds to normal operation.
- The repository configuration is retained for subsequent application use.
- The user is not required to configure the repository again on subsequent
  application invocations.

The repository configuration is expected to survive upgrades of the app.

---

# Application Session States

Application Session State identifies the persistent state of Holfuy Upgrader
when a test begins.

| State | Definition |
|-------|------------|
| Fresh | The application has been newly installed, or has been stopped with its application data cleared. No firmware selection is retained and the firmware repository has not yet been configured. |
| Persistent | The application has previously been used and retains its configured firmware repository and, if one was previously selected, its firmware selection. |

## Default Assumptions

Unless otherwise specified, a test may begin in either Application Session State.

A test case specifies an Application Session state only when the expected
behavior differs between the `Fresh` and `Persistent` states.

Where the retained firmware selection affects the behavior being tested, the
required Application Session State is specified explicitly as a precondition.

---

# Interruption Points

Interruption Points identify locations within the reference workflows where one
or more Variation Specifications may be introduced.

Each Interruption Point has a unique identifier across all reference workflows.
The numbering does not imply ordering or equivalence between workflows.

In the original workflow, firmware selection could not occur until after an
ISP connection was established between the Android device and the station, hence
IP-1 and IP-3 were independent and meaningful interruption points.
The current workflow prescribes firmware selection before connecting the Android
device to the station, making IP-3 redundant with IP-1, hence the retirment of IP-3.

| ID | Workflow | Description |
|----|----------|-------------|
| IP-1 | WF-001 | Firmware selection available, before opening Select Firmware |
| IP-2 | WF-001 | Select Firmware screen displayed |
| IP-3 | WF-001 | Retired |
| IP-4 | WF-001 | Station powered on and connected via USB to Android device, before tapping **Connect** |
| IP-5 | WF-001 | Waiting for USB permission |
| IP-6 | WF-001 | Connected to weather station |
| IP-7 | WF-001 | Firmware update in progress |
| IP-8 | WF-001 | Firmware update completed |
| IP-9 | WF-003 | Repository configuration instructions displayed |
| IP-10 | WF-003 | Repository directory picker displayed |

---

# Variation Specifications

Variation Specifications define reusable behavioral variations that may be
applied at one or more Interruption Points.

A Variation Specification represents a single behavioral property. Multiple
representative actions are grouped only when they are expected to produce
substantially the same observable behavior.

## Notation

```
IP-n: VS-name [ × count ]
```

Examples:

```
IP-2: VS-LIFE

IP-6: VS-LIFE ×3

IP-4: VS-FW-CANCEL

IP-5: VS-STATION-LOST
```

---

### VS-LIFE

Exercise Android lifecycle behavior without terminating the application
process.

Representative actions:

* Rotate the device.
* Press Home and resume the application.
* Open Recents and resume the application.
* Lock and unlock the device.
* Allow the screen to time out and wake the device.
* Press Back.

Expected behavioral property:

* The application remains in a consistent state.
* The current workflow may be continued.

---

### VS-STATION-LOST

The weather station unexpectedly becomes unavailable while the firmware update
workflow is in progress.

Representative actions:

* Disconnect the USB cable.
* Turn the weather station off.
* Allow the weather station bootloader timeout to expire.

Expected behavioral property:

* Android reports USB device removal.
* The application returns to the disconnected state.
* The user is able to recover by restoring the weather station connection to
the Android device and restarting the workflow.

---

### VS-USB-UNSUPPORTED

Attach an unsupported USB device.

Expected behavioral property:

* Unsupported devices are ignored.
* The application remains responsive.

---

### VS-FW-CANCEL

Dismiss the Android file picker without selecting a firmware image.

Expected behavioral property:

* No change to the firmware selection state.

---

### VS-FW-REPO-SELECT

Select a firmware file from the repository's firmware list.

Expected behavioral property:

- The selected repository firmware becomes the active firmware selection.
- If a firmware selection already exists, it is replaced by the newly selected
  repository firmware.

---

### VS-FW-BROWSE-SELECT

Select a firmware file using **Browse** and the Android document picker.

Expected behavioral property:

- The selected firmware becomes the active firmware selection.
- If a firmware selection already exists, it is replaced by the newly selected
  firmware.
  
---

### VS-HELP

- Tap **Help**.
- Scroll Help screen.
- Tap **Close Help**.
- Tap **Help**.
- Rotate device.
- Tap **Back**.

Expected behavioral property:

* Help screen is displayed, scrolls, and responds appropriately to rotation.
* **Back** and **Close Help** both close Help screen and return to Main screen. 

---

## TC-001 — Update Firmware (Nominal)

**Reference Workflow:** WF-001

**Classification:** Smoke, Regression, Compatibility

**Preconditions**

- Application Session: Fresh and Persistent

---

# Workflow Variation Test Cases

Each test case below specifies expected results in addition to these:

- Application remains responsive.
- No crash or (ANR) occurs.


## TC-002 — USB Permission Denied

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-5: Deny USB permission.

**Expected Results**

- Connection is not established.
- Firmware Update remains unavailable.
- User may retry by tapping **Connect**.

---

## TC-003 — Bootloader Timeout

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-4, IP-5: VS-STATION-LOST

**Expected Results**

* Station exits ISP mode after bootloader timeout.
* Android reports USB device removal.
* The application indicates that the weather station is no longer connected.
* No error dialog is displayed.
* The user can restore the station and continue the workflow as appropriate to the interruption point.

---

## TC-004 — Cancel Firmware Selection

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-2: VS-FW-CANCEL

**Expected Results**

- No change to firmware selection.
- Firmware Update remains disabled.
- User may reopen the file picker.

---

## TC-005 — Select Repository-Resident Firmware

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-2, IP-6: VS-FW-REPO-SELECT

**Expected Results**

- Application satisfies the VS-FW-REPO-SELECT expected behavioral property.

---

## TC-006 — Unexpected Station Loss During Firmware Update

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-7: VS-STATION-LOST

**Expected Results**

**Application**

- Detects loss of communication.
- Terminates the firmware update cleanly.
- Returns to the disconnected state.
- Remains responsive.

**Weather Station**

- Boots the previously installed firmware.

---

## TC-007 — Unsupported USB Device

**Reference Workflow:** WF-001

**Classification:** Regression

**Preconditions**

- Attach an unsupported USB device.

**Variation:** IP-4: VS-USB-UNSUPPORTED

**Expected Results**

- Connect remains unavailable.
- Firmware selection remains unavailable.
- Firmware Update remains unavailable.

---

## TC-008 — Android Lifecycle Interruption While Waiting for Permission

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-5: VS-LIFE

**Expected Results**

- Application resumes with Connect enabled.
- Tapping Connect presents USB permission dialog.
- USB permission workflow completes normally.
- Firmware update may be completed successfully.

---

## TC-009 — Android Lifecycle Interruption During Firmware Update

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-7: VS-LIFE

**Expected Results**

- Application resumes in an equivalent state.
- Firmware update continues uninterrupted.
- Progress indication remains correct.
- Firmware update completes successfully.

---

## TC-010 — Retain Firmware Selection

**Reference Workflow:** WF-001

**Classification:** Regression

**Application Session:** Persistent

**Variation:** IP-1: Retain the existing firmware selection.

**Expected Results**

- Previously selected firmware remains selected.
- Firmware Update is enabled after connection.

---

## TC-011 — Update Firmware (Alternative)

**Reference Workflow:** WF-002

**Classification:** Historical

**Preconditions**

- Application Session: Fresh and Persistent

---

## TC-012 — Unexpected Station Loss Before Firmware Update

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-6: VS-STATION-LOST

**Expected Results**

- Android reports USB device removal.
- Application indicates that the station is no longer connected.
- Firmware update is no longer available.
- No error dialog is displayed.
- After the weather station is restored, the user can reconnect and continue the normal workflow.

---

## TC-013 — Android Lifecycle Interruption on Select Firmware Screen

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-2: VS-LIFE

**Expected Results**

- Application resumes in an equivalent state.
- Firmware selection can continue normally.
- Firmware update can be completed successfully.

---

## TC-014 — Clear Status Message When Station Detaches

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-8: VS-STATION-LOST

**Expected Results**

- Android reports USB device removal.
- Application indicates that the station is no longer connected.
- "Firmware update complete" message is not present on the main screen.

---

## TC-015 — Clear Status Message When Firmware Selection Begins

**Reference Workflow:** WF-001

**Classification:** Historical

**Variation:** IP-7: VS-FW-REPLACE

**Expected Results**

- "Firmware update complete" message is not present on the main screen.

---

## TC-016 — Help Screen

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-1, 3, 4, 6, 7, 8: VS-HELP

**Expected Results**

- Application satisfies the VS-HELP expected behavioral property.

---

## TC-017 — Select Firmware via Browse

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-2, IP-6: VS-FW-BROWSE-SELECT

**Expected Results**

- Application satisfies the VS-FW-BROWSE-SELECT expected behavioral property.

---

## TC-018 — Configure Firmware Repository

**Reference Workflow:** WF-003

**Classification:** Regression, Compatibility

**Variation:** IP-9: Configure the firmware repository.

**Expected Results**

- Application satisfies the repository configuration expected behavioral property.

---

## TC-019 — Cancel Firmware Repository Configuration

**Reference Workflow:** WF-003

**Classification:** Regression, Compatibility

**Variation:** IP-10: Cancel repository directory selection.

**Expected Results**

- Application returns to the firmware repository configuration screen.
- Repository configuration remains incomplete.

---

## TC-020 — Android Lifecycle Interruption During Repository Directory Selection

**Reference Workflow:** WF-003

**Classification:** Regression, Compatibility

**Variation:** IP-10: VS-LIFE

**Expected Results**

- Application resumes in an equivalent state.
- Repository directory selection can continue normally.
- Repository configuration can be completed successfully.

---

## TC-021 — Firmware Repository Dispositions and Selection Availability

**Reference Workflow:** WF-001

**Classification:** Regression

**Variations:** VS-REPO-CURRENT, VS-REPO-OUTDATED, VS-REPO-MISSING, VS-REPO-CUSTOM

### Purpose

Verify that the application correctly recognizes and presents Current, Outdated, Missing, 
and Custom firmware files; that all existing selectable dispositions can be used to 
initiate Firmware Update; and that a Missing selected firmware file cannot be used to 
initiate Firmware Update.

### Test Data

**Development manifest URL:**

```text
https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/Manifests/Dispositions/all.json
```

**Manifest URL override:**

From the project repository root:

```bash
adb shell am start \
  -n com.holfuy.configtool/.DebugManifestActivity \
  -a com.holfuy.configtool.debug.SET_MANIFEST_URL \
  --es url "https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/Manifests/Dispositions/all.json"
```

**Test repository on device:**

```text
/sdcard/Download/HolfuyTest-Dispositions
```

Create the repository if necessary:

```bash
adb shell mkdir -p /sdcard/Download/HolfuyTest-Dispositions
```

From the project repository root, populate it with:

```bash
adb push \
  docs/Testing/TestData/FirmwareRepository/Dispositions/Current/Repository/current.bin \
  /sdcard/Download/HolfuyTest-Dispositions/
```

```bash
adb push \
  docs/Testing/TestData/FirmwareRepository/Dispositions/Outdated/Repository/outdated.bin \
  /sdcard/Download/HolfuyTest-Dispositions/
```

```bash
adb push \
  docs/Testing/TestData/FirmwareRepository/Dispositions/Custom/Repository/custom.bin \
  /sdcard/Download/HolfuyTest-Dispositions/
```

```bash
adb push \
  docs/Testing/TestData/FirmwareRepository/Dispositions/SelectedMissing/Repository/vanish.bin \
  /sdcard/Download/HolfuyTest-Dispositions/
```

Do not place `missing.bin` in the repository.

### Preconditions

Establish the preconditions in the order specified below.
The manifest URL override is stored in the app's private data, so the app's data must
be cleared before setting the manifest URL override.

1. The app's data has been cleared so it presents the repository-configuration screen when opened.
2. The manifest URL override is set to the development manifest URL specified above.
3. The test repository contains:
   - `current.bin`
   - `outdated.bin`
   - `custom.bin`
   - `vanish.bin`
4. `missing.bin` is absent from the repository.
5. The firmware repository is configured to:
   `/sdcard/Download/HolfuyTest-Dispositions`
6. The station is not required for the initial repository-disposition checks.

### Procedure

Note that with this configuration, each repository-refresh cycle takes several seconds
because the app must exhaust its retry count for a file before declaring it **Missing**
or **Outdated**.

1. Open the application and allow the repository refresh to complete.
2. Tap **Select Firmware**.
3. Verify that the Select Firmware screen lists:
   - `current.bin`, with disposition **Current**.
   - `outdated.bin`, with disposition **Outdated**.
   - `custom.bin`, with disposition **Custom**.
   - `vanish.bin`, with disposition **Current**.

   Verify that `missing.bin` is not listed.

4. Select `current.bin`.
5. Return to the main screen.
6. Verify that the Selected Firmware card identifies `current.bin` as **Current**.
7. Connect the station to the Android device and turn on the station.
8. Tap **Connect** and grant USB permission if requested.
9. Verify that **Update Firmware** is enabled.
10. Do not perform the update. Turn off the station.
11. Return to **Select Firmware**.
12. Select `outdated.bin`.
13. Return to the main screen.
14. Verify that the Selected Firmware card identifies `outdated.bin` as **Outdated**.
15. Connect the station to the Android device and turn on the station.
16. Tap **Connect** and grant USB permission if requested.
17. Verify that **Update Firmware** is enabled.
18. Do not perform the update. Turn off the station.
19. Return to **Select Firmware**.
20. Select `custom.bin`.
21. Return to the main screen.
22. Verify that the Selected Firmware card identifies `custom.bin` as **Custom**.
23. Connect the station to the Android device and turn on the station.
24. Tap **Connect** and grant USB permission if requested.
25. Verify that **Update Firmware** is enabled.
26. Do not perform the update. Turn off the station.
27. Return to **Select Firmware**.
28. Select `vanish.bin`.
29. Return to the main screen.
30. Verify that the Selected Firmware card identifies `vanish.bin` as **Current**.
31. Delete `vanish.bin` from the test repository on the Android device (using a file-management app).
32. Cause the application to resume, for example by navigating to another application and returning to Holfuy Upgrader.
33. Verify that the Selected Firmware card now identifies `vanish.bin` as **Missing**.
34. Connect the station to the Android device and turn on the station.
35. Tap **Connect** and grant USB permission if requested.
36. Verify that **Update Firmware** is disabled.
37. Return to **Select Firmware**.
38. Verify that `vanish.bin` is no longer listed.

### Expected Results

- The Select Firmware screen lists all existing selectable repository files except Missing files.
- `current.bin` is presented as **Current**.
- `outdated.bin` is presented as **Outdated**.
- `custom.bin` is presented as **Custom**.
- `vanish.bin` is initially presented as **Current**.
- `missing.bin` is not presented on the Select Firmware screen.
- Current, Outdated, and Custom firmware selections are reflected with the corresponding disposition on the Selected Firmware card.
- When a selected Current, Outdated, or Custom firmware file exists, the **Update Firmware** button is enabled after the station is connected.
- When the selected repository file is deleted, its disposition changes to **Missing** when the application resumes.
- A selected Missing firmware file cannot be used to initiate Firmware Update; **Update Firmware** is disabled.
- A Missing firmware file is not listed on the Select Firmware screen.
