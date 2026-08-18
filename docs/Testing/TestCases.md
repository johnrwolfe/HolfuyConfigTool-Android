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

| ID | Workflow | Description |
|----|----------|-------------|
| IP-1 | WF-001 | Firmware selection available, before opening Select Firmware |
| IP-2 | WF-001 | Select Firmware screen displayed |
| IP-3 | WF-001 | Firmware selected, before connecting the weather station |
| IP-4 | WF-001 | Station powered on and connected via USB to Android device, before tapping **Connect** |
| IP-5 | WF-001 | Waiting for USB permission |
| IP-6 | WF-001 | Connected to weather station |
| IP-7 | WF-001 | Firmware update in progress |
| IP-8 | WF-001 | Firmware update completed |
| IP-9 | WF-003 | Repository configuration instructions displayed |
| IP-10 | WF-003 | Repository directory picker displayed |
| IP-11 | WF-003 | Repository directory selected, before completing configuration |
| IP-12 | WF-003 | Repository configuration completed |

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

IP-3: VS-STATION-LOST
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

### VS-FW-REPLACE

Replace the currently selected firmware image.

Expected behavioral property:

* The previous firmware selection is replaced.

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

## TC-003 — Bootloader Timeout While Waiting for USB Permission

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-5: VS-STATION-LOST

**Expected Results**

* Android reports USB device removal.
* The application indicates that the weather station is no longer connected.
* No error dialog is displayed.
* The user may restore the weather station and restart the firmware update procedure.

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

## TC-005 — Replace Selected Firmware

**Reference Workflow:** WF-001

**Classification:** Regression

**Variation:** IP-3: VS-FW-REPLACE

**Expected Results**

- Previous firmware selection is replaced.
- Newly selected firmware becomes the active selection.
- Firmware Update remains enabled.

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

**Variation:** IP-3: Retain the existing firmware selection.

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

**Variation:** IP-1, 3, 4, 6, 7, 8, 12: VS-HELP

**Expected Results**

- Application satisfies the VS-HELP expected behavioral property.