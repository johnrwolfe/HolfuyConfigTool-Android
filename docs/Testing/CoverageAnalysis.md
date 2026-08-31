# Coverage Analysis

This document explains the rationale behind the Holfuy Upgrader manual test
suite.

Rather than attempting to exercise every possible sequence of user actions, the
suite identifies behavioral variations that may occur during each supported
workflow and demonstrates that each distinct observable behavior is verified by
at least one test case.

Behaviorally equivalent situations are intentionally represented by a single
canonical test.

---

# Scope

This analysis applies to:

* Workflow: WF-001
* Workflow: WF-003
* Application Session: Either, unless otherwise specified.

Additional workflows and application session states should be analyzed separately
as the application evolves.

---

# Behavioral Variations

Each behavioral variation is analyzed independently.

Matrix entries use the following notation.

| Entry   | Meaning                                 |
| ------- | --------------------------------------- |
| TC-nnn  | Canonical test exercising this behavior |
| =TC-nnn | Behavior equivalent to TC-nnn           |
| N       | Not meaningful                          |
| TBD     | Analysis not yet complete               |

Matrices show only Interruption Points at which the behavioral variation is
meaningful. Columns containing only `N` are omitted.

IP-3 has been retired as an Interruption Point. Its number is intentionally
unused so that the remaining Interruption Point numbers do not need to be
renumbered. It therefore does not appear in the coverage matrices.

=TC-xxx indicates that the referenced test case directly exercises a behaviorally
representative interruption point. The equivalence is based on the nature of the 
application state and the expected lifecycle behavior, rather than requiring the 
variation to be executed at every interruption point.

---

# WF-001 — Firmware Update

## Unexpected Station Loss

Representative actions:

* Disconnect USB cable.
* Power off weather station.
* Allow bootloader timeout to expire.

Behavioral property:

The weather station unexpectedly becomes unavailable while the firmware update
workflow is in progress.

| Interruption Point |   IP-4  |   IP-5  |   IP-6  |  IP-7  |
| ------------------ | :-----: | :-----: | :-----: | :----: |
| Coverage           | =TC-012 | =TC-012 | =TC-012 | TC-006 |

**Rationale**

Prior to firmware programming, disconnecting the USB cable, powering off the
station, and allowing the bootloader timeout to expire produce the same
observable application behavior. Android reports USB device removal, the
application transitions to the disconnected state, and the workflow may be
restarted after the station is restored.

Firmware programming is analyzed separately because interruption during flash
programming has unique consequences.

---

## Android Lifecycle

Representative actions:

* Rotate device.
* Home / Resume.
* Recents / Resume.
* Lock / Unlock.
* Screen timeout.
* Back.

| Interruption Point |   IP-1  |  IP-2  |   IP-4  |  IP-5  |  IP-6  |   IP-7  |   IP-8  | IP-9   | IP-10  |
| ------------------ | :-----: | :----: | :-----: | :-----: | :----: | :----: | :-----: | :----: | :----: |
| Coverage           | =TC-009 | TC-013 | =TC-009 | TC-008 | TC-009 | =TC-009 | =TC-009 | TC-018 | TC-018 |

**Rationale**

Interruption points are grouped according to which component owns the active
user interface.

IPs 1, 4, 6, 7, and 8 are application-controlled UI states and are therefore
covered by TC-009.

IP-2 is the application-controlled Select Firmware screen and is covered by
TC-013.

IP-5 is the Android USB permission dialog and is covered by TC-008.

IP-9 is the application-controlled Repository Configuration screen, while
IP-10 is the Android-controlled directory picker. These are distinct lifecycle
contexts and require additional coverage.

---

## Help

| Interruption Point |  IP-1  |  IP-4  |  IP-6  |  IP-7  |  IP-8  |
| ------------------ | :----: | :----: | :----: | :----: | :----: |
| Coverage           | TC-016 | TC-016 | TC-016 | TC-016 | TC-016 |

**Rationale**

Help is available from the application-controlled states represented by these
Interruption Points. TC-016 is the canonical test for the VS-HELP variation and
explicitly exercises all applicable IPs.

Help is not directly available from the Android USB permission dialog at IP-5.
Tapping Help at that point dismisses the permission request; the Help screen
must then be opened by tapping Help again.

---

## Firmware Selection

### Cancel Firmware Selection

| Interruption Point |  IP-2  |
| ------------------ | :----: |
| Coverage           | TC-004 |

---

### Select Repository-Resident Firmware

| Interruption Point |  IP-2  |   IP-4  |  IP-6  |
| ------------------ | :----: | :-----: | :----: |
| Coverage           | TC-005 | =TC-005 | TC-005 |

---

### Select Firmware via Browse

| Interruption Point |  IP-2  |   IP-4  |  IP-6  |
| ------------------ | :----: | :-----: | :----: |
| Coverage           | TC-017 | =TC-017 | TC-017 |

---


| Repository Content         | Coverage |
| -------------------------- | -------- |
| Manifest-provided firmware | =TC-017  |
| Custom repository firmware | =TC-017  |

---

## USB Permission

| Interruption Point | IP-5 |
|--------------------|:---:|
| Coverage | TC-002 |

**Rationale**

The Android USB permission dialog presents several ways to decline or dismiss
the permission request. Explicitly tapping **Deny**, tapping **Back**, tapping
**Home** or **Recents**, or tapping the dialog's **Cancel** button all leave
the application without USB permission and with **Connect** enabled. These
actions therefore represent one behavioral class.

TC-002 covers explicit denial. The dismissal actions are additional executions
of TC-002 rather than separate test cases.

Android lifecycle interruption while the permission dialog is displayed is
covered separately by TC-008.

If the station exits ISP mode while permission is pending, the resulting USB
device removal is covered by TC-003.

---

## Bootloader Timeout

The station bootloader timeout can occur while the station is in ISP mode and
there has been insufficient activity on the USB connection.

| Interruption Point |  IP-4  |  IP-5  |  IP-6   |  IP-8   |
| ------------------ | :----: | :----: | :-----: | :-----: |
| Coverage           | TC-003 | TC-003 | =TC-003 | =TC-003 |

**Rationale**

When the bootloader timeout occurs, the station exits ISP mode and drops the
USB connection. Android reports the USB detachment to the application. The app
updates its state, disables the Connect button, and displays the disconnected
state. The user must power-cycle the station to restart the workflow.

---

## Unsupported USB Device

| Interruption Point | IP-1 |
|--------------------|:---:|
| Coverage | TC-007 |

**Rationale**

An unsupported USB device is recognized and ignored. The application remains
responsive, and the firmware-update workflow remains unavailable.

No additional interruption points or behavioral variations are introduced by
the current application changes, so TC-007 provides sufficient coverage.

---

# WF-003 — Firmware Repository Configuration

| Interruption Point              |   IP-9  |    IP-10   |
| ------------------------------- | :-----: | :--------: |
| Android Lifecycle               | TC-018  |   TC-018   |
| Configure Repository            |    N    |   TC-018   |
| Cancel Repository Configuration |    N    |   TC-019   |


Rationale

Selecting a permitted directory is the canonical repository-configuration test.
Because the application imposes no additional constraints on the directory 
selected by Android, different permitted directories do not represent distinct
behavioral cases.

Cancelling the directory picker is tested separately because it returns the 
application to the repository-configuration screen rather than completing the 
workflow.

---

# Firmware Repository Synchronization

Whenever the application is opened, it attempts to retrieve the manifest and
download the latest firmware files identified by the manifest.

Failure to retrieve firmware files must not prevent normal operation using
firmware already present in the repository.

## Manifest Retrieval

| Behavior                                                  | Coverage |
| --------------------------------------------------------- | -------- |
| Manifest successfully retrieved                           | TC-021   |
| Manifest retrieval fails                                  | TC-020   |
| Existing repository remains usable after manifest failure | TC-020   |

---

## Firmware Download

| Behavior                                                   | Coverage |
| ---------------------------------------------------------- | -------- |
| Latest firmware file downloads successfully                | TC-022   |
| Individual firmware download fails                         | TC-021   |
| Existing firmware remains available after download failure | TC-021   |
| Downloaded firmware becomes available for selection        | TC-022   |

---

## Manifest-Defined Modem Types

The manifest is the interface between the application and Holfuy's firmware
repository. The application uses the `path` attribute to locate firmware files.

| Behavior                                                  | Coverage |
| --------------------------------------------------------- | -------- |
| Multiple modem types listed in manifest                   | TC-001   |
| Each listed firmware file is downloaded                   | TC-001   |
| New modem type can be added without an application update | TC-021   |
| Malformed or unexpected manifest content                  | TC-024   |

---

## Repository Contents

The repository can contain both firmware downloaded by the application and
custom firmware files placed there by the user.

| Behavior                                                       | Coverage |
| -------------------------------------------------------------- | -------- |
| Automatically downloaded firmware is retained                  | TC-021   |
| Custom firmware is retained                                    | TC-021   |
| Both types appear in Select Firmware                           | TC-021   |
| Existing firmware remains usable after synchronization failure | TC-021   |

---

# Application Session State

Coverage by Application Session State is evaluated using the session-state definitions established in the test suite.

The matrix identifies the Application Session States from which each behavioral area is explicitly exercised. 


| Behavioral Area            |  Fresh | Repository Configured | Firmware Selected |
| -------------------------- | :----: | :-------------------: | :---------------: |
| Repository Configuration   | TC-018 |           —           |         —         |
| Firmware Selection         | TC-001 |         TC-001        |       TC-001      |
| Repository Synchronization | TC-001 |         TC-001        |       TC-001      |
