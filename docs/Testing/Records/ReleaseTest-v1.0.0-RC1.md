# Holfuy Upgrader Release Test Record

**Release:** 1.0.0

**Date Tested:** 2026-07-17

**Tester:** John R. Wolfe

**Android Studio Version:** Android Studio Panda 4 | 2025.3.4 Patch 1

---

# Test Environment

## Test Devices

- After installing app to be tested, turn off USB Debugging and disable Developer Mode

| Device                | Android Version | Result | Notes |
| --------------------- | --------------- | ------ | ----- |
| Samsung Galaxy S21    | 15              | Pass   | All test cases executed on primary test device.  #11 discovered and retested. |
| Samsung Galaxy S24    | 16              | Pass   | All compatibility test cases executed. |
| Samsung Galaxy Tab A7 | 12              | Pass   | All compatibility test cases executed. |
| Air3                  | 8.1             | Pass   | All compatibility test cases executed.  Platform-specific Android 8.1 behavior. See issue [#7](https://github.com/MaileTechnical/HolfuyConfigTool-Android/issues/7). |

---

## Test Equipment

| Item                   | Notes |
| ---------------------- | ----- |
| Holfuy weather station | ID: 1380 |
| USB OTG adapter        | Not used since all test devices have native OTG capability. Basesailor USB C to USB Adapter used extensively during early development, but only with devices that have native OTG capability. |
| Firmware image         | V10.06, V11.04 |

---

# Test Results

| Test Case | Result | Notes |
| --------- | ------ | ----- |
| TC-001    | Pass   |       |
| TC-002    | Pass   |       |
| TC-003    | Pass   |       |
| TC-004    | Pass   |       |
| TC-005    | Pass   |       |
| TC-006    |        | Deferred until receiving confirmation this is a valid test. |
| TC-007    | Pass   |       |
| TC-008    | Pass   |       |
| TC-009    | Pass   |       |
| TC-010    | Pass   |       |
| TC-011    | Pass   |       |
| TC-012    | Pass   |       |
| TC-013    | Pass   |       |
| TC-014    | Pass   |       |
| TC-015    | Pass   |       |
| TC-016    | Pass   |       |

---

# Issues Found

- [11](https://github.com/MaileTechnical/HolfuyConfigTool-Android/issues/11)

---

# Release Decision

Approved for release 

---

# Notes

TC-006 exercises interruption during flash programming. It was deferred pending confirmation that intentionally interrupting
programming does not risk hardware damage or corruption of existing firmware on the station.
Discovering #11 this late in the release cycle was a surprise, and it resulted in the addition of two test cases, TC-014, and TC-015.
Noticing that the Help screen had escaped initial test planning and coverage analysis was another surprise that produced TC-016.