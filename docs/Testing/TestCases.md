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

| Section            | Purpose                                        |
| ------------------ | ---------------------------------------------- |
| ID                 | Unique test identifier                         |
| Title              | Short descriptive name                         |
| Reference Workflow | Typically WF-001                               |
| Classification     | Smoke, Regression, Compatibility, Exploratory  |
| Preconditions      | Conditions required before executing the test  |
| Variation          | Interruption Point and Variation Specification |
| Expected Results   | Observable system behavior                     |
| Regression History | GitHub issue(s), if applicable                 |

---

# Reference Workflows

## WF-001 — Update Firmware

This is the normal firmware update workflow described in the User Guide.

**Procedure**

Execute the firmware update procedure exactly as described in the User Guide.

**Expected Results**

* A firmware image can be selected before connecting to the weather station.
* USB permission is granted.
* Connection succeeds.
* Firmware update completes successfully.
* Progress indication reaches completion.
* Application remains responsive.
* No crash or "Application Not Responding" (ANR) occurs.
* The station boots the newly installed firmware.

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

- Application satisfies the VS-REPO-CONFIG expected behavioral property.

---

# Application Session States

Application Session State identifies the state of Holfuy Upgrader when a test begins.

| State      | Definition                                                                                                                                                                                  |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Fresh      | The application has been newly installed, or has been stopped with its application data cleared. No firmware selection is retained and the firmware repository has not yet been configured. |
| Repository Configured | A firmware repository is configured, but no firmware selection exists. |
| Firmware Selected | A firmware repository is configured and a firmware selection is retained. |

## Default Assumptions

Unless otherwise specified, a test may begin in any Application Session State.

A test case specifies an Application Session state only when the expected
behavior differs between the states.

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
device to the station, making IP-3 redundant with IP-1, hence the retirement of IP-3.

| ID    | Workflow | Description                                                                            |
| ----- | -------- | -------------------------------------------------------------------------------------- |
| IP-1  | WF-001   | Firmware selection available, before opening Select Firmware                           |
| IP-2  | WF-001   | Select Firmware screen displayed                                                       |
| IP-3  | WF-001   | Retired                                                                                |
| IP-4  | WF-001   | Station powered on and connected via USB to Android device, before tapping **Connect** |
| IP-5  | WF-001   | Waiting for USB permission                                                             |
| IP-6  | WF-001   | Connected to weather station                                                           |
| IP-7  | WF-001   | Firmware update in progress                                                            |
| IP-8  | WF-001   | Firmware update completed                                                              |
| IP-9  | WF-003   | Repository configuration instructions displayed                                        |
| IP-10 | WF-003   | Repository directory picker displayed                                                  |

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

* The selected repository firmware becomes the active firmware selection.
* If a firmware selection already exists, it is replaced by the newly selected
  repository firmware.

---

### VS-FW-BROWSE-SELECT

Select a firmware file using **Browse** and the Android document picker.

Expected behavioral property:

* The selected firmware becomes the active firmware selection.
* If a firmware selection already exists, it is replaced by the newly selected
  firmware.

---

### VS-REPO-CONFIG

Configure the firmware repository using the application's repository
configuration workflow.

Expected behavioral property:

* The firmware repository directory is configured successfully.
* The application proceeds to normal operation.
* The repository configuration is retained for subsequent application use.
* The user is not required to configure the repository again on subsequent
  application invocations.
* The repository configuration survives upgrades of the app.

---

### VS-REPO-CURRENT

A repository firmware file exists and its checksum matches the checksum
specified by the manifest.

Expected behavioral property:

* The firmware file is presented as **Current**.
* The firmware file is available for selection.

---

### VS-REPO-OUTDATED

A repository firmware file exists but its checksum does not match the checksum
specified by the manifest.

Expected behavioral property:

* The firmware file is presented as **Outdated**.
* The firmware file is available for selection.

---

### VS-REPO-MISSING

A firmware file specified by the manifest is not present in the repository.

Expected behavioral property:

* The firmware file is not presented as selectable repository firmware.
* If the file was previously selected, its disposition is **Missing** and
  Firmware Update is unavailable.

---

### VS-REPO-CUSTOM

A firmware file exists in the repository but is not specified by the manifest.

Expected behavioral property:

* The firmware file is presented as **Custom**.
* The firmware file is available for selection.

---

### VS-REPO-MANIFEST-FAIL

The application cannot retrieve the firmware repository manifest during
repository synchronization.

Expected behavioral property:

* The existing repository contents remain usable.
* An existing firmware selection remains selected and usable.
* The disposition of firmware within the repository is unchanged.
* Repository synchronization resumes normally when manifest retrieval becomes
  available again.

---

### VS-REPO-INVALID-MANIFEST

The firmware repository manifest is successfully retrieved but contains
malformed or unexpected content.

Expected behavioral property:

* The application handles the invalid manifest without crashing or becoming
  unusable.
* Existing repository contents remain usable.
* An existing firmware selection remains selected and usable.
* The disposition of firmware within the repository is unchanged.

---

### VS-HELP

* Tap **Help**.
* Scroll Help screen.
* Tap **Close Help**.
* Tap **Help**.
* Rotate device.
* Tap **Back**.

Expected behavioral property:

* Help screen is displayed, scrolls, and responds appropriately to rotation.
* **Back** and **Close Help** both close Help screen and return to Main screen.

---

## TC-001 — Update Firmware (Nominal)

**Reference Workflow:** WF-001

**Classification:** Smoke, Regression, Compatibility

**Preconditions**

* Application Session: Fresh, Repository Configured, Firmware Selected -- execute the test once in each state.

---

# Workflow Variation Test Cases

Each test case below specifies expected results in addition to these:

* Application remains responsive.
* No crash or (ANR) occurs.

## TC-002 — USB Permission Denied

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-5: Deny USB permission.

**Expected Results**

* Connection is not established.
* Firmware Update remains unavailable.
* User may retry by tapping **Connect**.

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

* No change to firmware selection.
* Firmware Update remains disabled.
* User may reopen the file picker.

---

## TC-005 — Select Repository-Resident Firmware

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-2, IP-6: VS-FW-REPO-SELECT

**Expected Results**

* Application satisfies the VS-FW-REPO-SELECT expected behavioral property.

---

## TC-006 — Unexpected Station Loss During Firmware Update

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-7: VS-STATION-LOST

**Expected Results**

**Application**

* Detects loss of communication.
* Terminates the firmware update cleanly.
* Returns to the disconnected state.
* Remains responsive.

**Weather Station**

* Boots the previously installed firmware.

---

## TC-007 — Unsupported USB Device

**Reference Workflow:** WF-001

**Classification:** Regression

**Preconditions**

* Attach an unsupported USB device.
* The Holfuy weather station is not connected.

**Variation:** IP-1: VS-USB-UNSUPPORTED

**Expected Results**

* Connect remains unavailable.
* Firmware selection remains unavailable.
* Firmware Update remains unavailable.

---

## TC-008 — Android Lifecycle Interruption While Waiting for Permission

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-5: VS-LIFE

**Expected Results**

* Application resumes with Connect enabled.
* Tapping Connect presents USB permission dialog.
* USB permission workflow completes normally.
* Firmware update may be completed successfully.

---

## TC-009 — Android Lifecycle Interruption During Firmware Update

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-7: VS-LIFE

**Expected Results**

* Application resumes in an equivalent state.
* Firmware update continues uninterrupted.
* Progress indication remains correct.
* Firmware update completes successfully.

---

## TC-010 — Retain Firmware Selection

**Reference Workflow:** WF-001

**Classification:** Historical

**Application Session:** Firmware Selected

**Variation:** IP-1: Retain the existing firmware selection.

**Expected Results**

* Previously selected firmware remains selected.
* Firmware Update is enabled after connection.

---

## TC-011 — Update Firmware (Alternative)

**Reference Workflow:** WF-002

**Classification:** Historical

**Preconditions**

* Application Session: Fresh and Firmware Selected -- Execute test once in each state.

---

## TC-012 — Unexpected Station Loss Before Firmware Update

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-6: VS-STATION-LOST

**Expected Results**

* Android reports USB device removal.
* Application indicates that the station is no longer connected.
* Firmware update is no longer available.
* No error dialog is displayed.
* After the weather station is restored, the user can reconnect and continue the normal workflow.

---

## TC-013 — Android Lifecycle Interruption on Select Firmware Screen

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-2: VS-LIFE

**Expected Results**

* Application resumes in an equivalent state.
* Firmware selection can continue normally.
* Firmware update can be completed successfully.

---

## TC-014 — Clear Status Message When Station Detaches

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-8: VS-STATION-LOST

**Expected Results**

* Android reports USB device removal.
* Application indicates that the station is no longer connected.
* "Firmware update complete" message is not present on the main screen.

---

## TC-015 — Clear Status Message When Firmware Selection Begins

**Reference Workflow:** WF-001

**Classification:** Historical

**Variation:** IP-8: Tap **Select Firmware**.

**Expected Results**

* "Firmware update complete" message is not present on the main screen.

---

## TC-016 — Help Screen

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-1, 4, 6, 7, 8: VS-HELP

**Expected Results**

* Application satisfies the VS-HELP expected behavioral property.

---

## TC-017 — Select Firmware via Browse

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Variation:** IP-2, IP-6: VS-FW-BROWSE-SELECT

**Expected Results**

* Application satisfies the VS-FW-BROWSE-SELECT expected behavioral property.

---

## TC-018 — Configure Firmware Repository

**Reference Workflow:** WF-003

**Classification:** Regression, Compatibility

**Variation:** IP-9, 10: VS-LIFE, VS-REPO-CONFIG

**Expected Results**

### Expected Results

- Application satisfies the VS-REPO-CONFIG and VS-LIFE expected behavioral properties.

---

## TC-019 — Cancel Firmware Repository Configuration

**Reference Workflow:** WF-003

**Classification:** Regression, Compatibility

**Variation:** IP-10: Cancel repository directory selection.

**Expected Results**

* Application returns to the firmware repository configuration screen.
* Repository configuration remains incomplete.

---

## TC-020 — Manifest Retrieval Failure

**Reference Workflow:** WF-001

**Classification:** Regression, Compatibility

**Application Session:** Firmware Selected

### Purpose

Verify that failure to retrieve the firmware repository manifest does not make an existing repository or firmware selection unusable.

### Preconditions

1. The selected firmware is available and usable.
2. The Android device has an Internet connection.

### Procedure

1. Open the application.
2. Tap **Select Firmware**.
3. Verify that the selected firmware is listed with its expected disposition.
4. Return to the main screen.
5. Verify that the Selected Firmware card identifies the selected firmware and its expected disposition.
6. Disable Internet connectivity on the Android device.
7. Cause the application to resume, initiating a repository refresh.
8. Wait for the refresh to complete.
9. Verify that the application reports that the manifest could not be retrieved.
10. Tap **Select Firmware**.
11. Verify that the existing repository firmware remains available for selection.
12. Return to the main screen.
13. Verify that the previously selected firmware remains selected and usable.
14. Verify that the application reports the date and time of the last successful check for firmware.
15. Verify that the application reports the date and time it was unable to check for firmware.
16. Restore Internet connectivity on the Android device.
17. Cause the application to resume, initiating a repository refresh.
18. Wait for the refresh to complete.
19. Verify that normal repository synchronization resumes.

### Expected Results

- Application satisfies the VS-REPO-MANIFEST-FAIL expected behavioral property.

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

1. Application Session: Fresh
2. The manifest URL override is set as specified above.
3. The test repository contains:

   * `current.bin`
   * `outdated.bin`
   * `custom.bin`
   * `vanish.bin`
4. `missing.bin` is absent from the repository.

### Procedure

Note that with this configuration, each repository-refresh cycle takes several seconds
because the app must exhaust its retry count for a file before declaring it **Missing**
or **Outdated**.

1. Open the application.
2. Configure the firmware repository to:
   `/sdcard/Download/HolfuyTest-Dispositions`
3. Tap **Select Firmware**.
4. Verify that the Select Firmware screen lists:
   - `current.bin`, with disposition **Current**.
   - `outdated.bin`, with disposition **Outdated**.
   - `custom.bin`, with disposition **Custom**.
   - `vanish.bin`, with disposition **Current**.
   Verify that `missing.bin` is not listed.
5. Select `current.bin`.
6. Return to the main screen.
7. Verify that the Selected Firmware card identifies `current.bin` as **Current**.
8. Connect the station to the Android device and turn on the station.
9. Tap **Connect** and grant USB permission if requested.
10. Verify that **Update Firmware** is enabled.
11. Do not perform the update. Turn off the station.
12. Tap **Select Firmware**.
13. Select `outdated.bin`.
14. Return to the main screen.
15. Verify that the Selected Firmware card identifies `outdated.bin` as **Outdated**.
16. Connect the station to the Android device and turn on the station.
17. Tap **Connect** and grant USB permission if requested.
18. Verify that **Update Firmware** is enabled.
19. Do not perform the update. Turn off the station.
20. Tap **Select Firmware**.
21. Select `custom.bin`.
22. Return to the main screen.
23. Verify that the Selected Firmware card identifies `custom.bin` as **Custom**.
24. Connect the station to the Android device and turn on the station.
25. Tap **Connect** and grant USB permission if requested.
26. Verify that **Update Firmware** is enabled.
27. Do not perform the update. Turn off the station.
28. Tap **Select Firmware**.
29. Select `vanish.bin`.
30. Return to the main screen.
31. Verify that the Selected Firmware card identifies `vanish.bin` as **Current**.
32. Delete `vanish.bin` from the test repository on the Android device (using a file-management app).
33. Cause the application to resume, for example by navigating to another application and returning to Holfuy Upgrader.
34. Verify that the Selected Firmware card now identifies `vanish.bin` as **Missing**.
35. Connect the station to the Android device and turn on the station.
36. Tap **Connect** and grant USB permission if requested.
37. Verify that **Update Firmware** is disabled.
38. Tap **Select Firmware**.
39. Verify that the Select Firmware screen lists:
   - `current.bin`, with disposition **Current**.
   - `outdated.bin`, with disposition **Outdated**.
   - `custom.bin`, with disposition **Custom**.
   Verify that `missing.bin` and `vanish.bin` are no longer listed.

### Expected Results

- Application satisfies the VS-REPO-CURRENT, VS-REPO-OUTDATED, VS-REPO-MISSING, and VS-REPO-CUSTOM expected behavioral properties.

---

## TC-022 — Replace Outdated Firmware

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** VS-REPO-OUTDATED, VS-REPO-CURRENT

### Purpose

Verify that a successful repository refresh replaces an Outdated firmware file with the Current
version identified by the manifest, and that the Selected Firmware card reflects the transition
from Outdated to Current.

### Test Data

**Manifest A URL:**

```text
https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/Dispositions/Replacement/manifest-outdated.json
```

Manifest A identifies `replacement.bin` with a checksum that does not match the copy initially
placed in the repository and specifies an unavailable URL for the replacement file.

**Manifest B URL:**

```text
https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/Dispositions/Replacement/manifest-current.json
```

Manifest B identifies `replacement.bin` with the checksum of the replacement file and
specifies the URL from which the replacement file can be downloaded.

The replacement file SHA-256 is:

```text
f3ad11014246cc9c67d2b3a5fda4bdb1ea8bc5ce6d0268de3487accdb32e6a77
```

**Test repository on device:**

```text
/sdcard/Download/HolfuyTest-Replacement
```

Create the repository if necessary:

```bash
adb shell mkdir -p /sdcard/Download/HolfuyTest-Replacement
```

From the project repository root, populate it with the older repository copy:

```bash
adb push \
  docs/Testing/TestData/FirmwareRepository/Dispositions/Replacement/Repository/replacement.bin \
  /sdcard/Download/HolfuyTest-Replacement/
```

Verify the initial file:

```bash
adb shell sha256sum /sdcard/Download/HolfuyTest-Replacement/replacement.bin
```

The initial SHA-256 should be:

```text
73907bd77f938f6b1446954d4f3363993455c9d19b337720c9c42c23700b6ae5
```

**Manifest URL override:**

From the project repository root:

```bash
adb shell am start \
  -n com.holfuy.configtool/.DebugManifestActivity \
  -a com.holfuy.configtool.debug.SET_MANIFEST_URL \
  --es url "https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/Dispositions/Replacement/manifest-outdated.json"
```

### Preconditions

1. Clear the application's data.
2. Set the manifest URL override to Manifest A.
3. The repository contains the older `replacement.bin`.
4. The Android device has an Internet connection.

### Procedure

1. Open the application.
2. Configure the firmware repository as:
   `/sdcard/Download/HolfuyTest-Replacement`
3. Tap **Select Firmware**.
4. Verify that `replacement.bin` is listed with disposition **Outdated**.
5. Select `replacement.bin`.
6. Return to the main screen.
7. Verify that the Selected Firmware card identifies `replacement.bin` as **Outdated**.
8. Set the manifest URL override to Manifest B:

   ```bash
   adb shell am start \
     -n com.holfuy.configtool/.DebugManifestActivity \
     -a com.holfuy.configtool.debug.SET_MANIFEST_URL \
     --es url "https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/Dispositions/Replacement/manifest-current.json"
   ```
9. Cause the application to resume, initiating a repository refresh.
10. Wait for the refresh to complete.
11. Verify that the Selected Firmware card identifies `replacement.bin` as **Current**.
12. Verify that the SHA-256 of the repository file is the replacement-image SHA-256:

    ```bash
    adb shell sha256sum /sdcard/Download/HolfuyTest-Replacement/replacement.bin
    ```

    Expected SHA-256:

    ```text
    f3ad11014246cc9c67d2b3a5fda4bdb1ea8bc5ce6d0268de3487accdb32e6a77
    ```
13. Tap **Select Firmware**.
14. Verify that `replacement.bin` is listed with disposition **Current**.
15. Verify that `replacement.bin` remains selected.

### Expected Results

* Application satisfies the VS-REPO-OUTDATED and VS-REPO-CURRENT expected behavioral properties.
* After Manifest B is selected and the repository is successfully refreshed, the selected firmware transitions from **Outdated** to **Current**.
* The repository file after replacement has the SHA-256 specified by Manifest B.
* The firmware selection is retained after replacement.


---

## TC-023 — Firmware Selection File Constraints

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-2: Firmware file size and extension constraints

### Purpose

Verify that firmware files larger than 200 kB or having an extension other than `.bin` are not presented as selectable repository firmware, and that attempting to select such files through **Browse** produces the appropriate validation error.

### Test Data

The `SizeExtension` test fixture contains:

* `valid_exactly_200kB.bin` — exactly 200 kB and has a `.bin` extension.
* `too_large.bin` — larger than 200 kB and has a `.bin` extension.
* `wrong_extension.txt` — no larger than 200 kB and has an extension other than `.bin`.
* `wrong_extension_too_large.txt` — larger than 200 kB and has an extension other than `.bin`.

The test repository on the Android device contains all four files.

### Preconditions

1. The firmware repository is configured to the test repository containing the four test files.
2. The Android device has an Internet connection.
3. The application has completed any repository refresh required to populate the repository state.
4. The station is not required for this test.

### Procedure

1. Open the application.
2. Tap **Select Firmware**.
3. Verify that `valid_exactly_200kB.bin` is listed.  Note that other valid files will also be listed.
4. Verify that `too_large.bin` is not listed.
5. Verify that `wrong_extension.txt` is not listed.
6. Verify that `wrong_extension_too_large.txt` is not listed.
7. Tap **Browse**.
8. Navigate to the test files and select `too_large.bin`.
9. Verify that the application rejects the file and displays the error message indicating that the file is too large.
10. Tap **Browse** again.
11. Navigate to the test files and select `wrong_extension.txt`.
12. Verify that the application rejects the file and displays the error message indicating that the file must have a `.bin` extension.
13. Tap **Browse** again.
14. Navigate to the test files and select `wrong_extension_too_large.txt`.
15. Verify that the application rejects the file and displays the error message indicating that the file is both too large and has an invalid extension.

### Expected Results

* `valid_exactly_200kB.bin` is listed on the Select Firmware screen.
* `too_large.bin` is not listed on the Select Firmware screen.
* `wrong_extension.txt` is not listed on the Select Firmware screen.
* `wrong_extension_too_large.txt` is not listed on the Select Firmware screen.
* A Browse selection of `too_large.bin` is rejected with the appropriate size error.
* A Browse selection of `wrong_extension.txt` is rejected with the appropriate extension error.
* A Browse selection of `wrong_extension_too_large.txt` is rejected with the appropriate combined error.
* An invalid Browse selection does not become the selected firmware.

---

## TC-024 — Invalid Firmware Manifest

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** VS-REPO-INVALID-MANIFEST

### Purpose

Verify that an invalid firmware manifest does not alter the existing firmware 
repository or selected firmware, and that the application clearly indicates the manifest is invalid.

### Test Data

**Manifest URL overrides:**

`invalid_json.json`:

    https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/InvalidManifest/invalid_json.json

`missing_sha256.json`:

    https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/InvalidManifest/missing_sha256.json

### Preconditions

1. Application Session: Firmware Selected.
2. The firmware repository is configured and contains at least one Current firmware file.
3. A Current firmware file is selected.
4. The Android device has an Internet connection.

### Procedure

1. Open the application.
2. Verify that the Selected Firmware card identifies the selected firmware as **Current**.
3. Set the manifest URL override to `invalid_json.json`:

       adb shell am start \
         -n com.holfuyconfigtool/.DebugManifestActivity \
         -a com.holfuyconfigtool.debug.SET_MANIFEST_URL \
         --es url "https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/InvalidManifest/invalid_json.json"

4. Cause the application to resume, initiating a repository refresh.
5. Wait for the refresh to complete.
6. Verify that the application displays an invalid-manifest error indication.
7. Verify that the Selected Firmware card still identifies the selected firmware as **Current**.
8. Tap **Select Firmware**.
9. Verify that the previously selected firmware is still listed with disposition **Current**.
10. Return to the main screen.
11. Set the manifest URL override to `missing_sha256.json`:

       adb shell am start \
         -n com.holfuyconfigtool/.DebugManifestActivity \
         -a com.holfuyconfigtool.debug.SET_MANIFEST_URL \
         --es url "https://raw.githubusercontent.com/johnrwolfe/HolfuyConfigTool-Android/issue/13-Download_Firmware_Files/docs/Testing/TestData/FirmwareRepository/InvalidManifest/missing_sha256.json"

12. Cause the application to resume, initiating a repository refresh.
13. Wait for the refresh to complete.
14. Verify that the application displays an invalid-manifest error indication.
15. Verify that the Selected Firmware card still identifies the selected firmware as **Current**.
16. Tap **Select Firmware**.
17. Verify that the previously selected firmware is still listed with disposition **Current**.
18. Return to the main screen.

### Expected Results

- Application satisfies the VS-REPO-INVALID-MANIFEST expected behavioral property.
