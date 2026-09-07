# Holfuy Upgrader Testing

This directory contains the testing documentation for Holfuy Upgrader.

The test suite is organized around **user
workflows** rather than individual software components or application screens.
The objective is to verify the behavior of the complete system, including the
Android device, the USB connection, the Holfuy weather station, and the Holfuy
Upgrader application.

The testing strategy is intended to minimize redundant procedures while
providing systematic coverage of both expected operation and important
variations of normal use.

---

# Scope

Holfuy Upgrader updates firmware on Holfuy weather stations using the Nuvoton
ISP bootloader over USB.

Testing encompasses the complete system:

- Android operating system
- Android device hardware
- USB subsystem
- Internet connection
- Holfuy Upgrader
- Holfuy weather station
- User interaction

Because these components operate concurrently, testing focuses on observable
system behavior rather than individual software modules.

---

# Testing Philosophy

Testing begins with the procedures documented in the User Guide.

Each documented procedure defines a **nominal workflow** describing the expected
sequence of user actions required to complete a task.

For version 1.0.0, there are two nominal workflows:

- Configure Repository
- Update Firmware

Additional application features will introduce additional nominal workflows.

Rather than creating independent procedures for every scenario, the test suite
treats every non-nominal test as a **variation** of one of these documented
workflows.

Typical variations include:

- Delayed user actions
- Android lifecycle events
- USB connection changes
- Internet connection changes
- Weather station state changes
- Firmware-selection variations

Each variation intentionally changes one aspect of the nominal workflow while
leaving the remainder of the procedure unchanged.

This approach minimizes duplicated procedures while providing systematic
behavioral coverage.

Unless a test explicitly verifies successful firmware update, the preferred outcome
of most workflow variations is graceful recovery rather than automatic completion.
A successful test is one in which the application preserves user data where appropriate
and allows the user to resume or restart the workflow without restarting the application.

---

# Test Documentation

The testing documentation consists of the following documents.

| Document | Purpose |
|----------|---------|
| **TestCases.md** | Executable manual test procedures |
| **CoverageAnalysis.md** | Functional coverage of test procedures |

# Test Derivation

Manual test cases are developed using the following process.

1. Document a nominal user workflow in the User Guide.
2. Identify significant interruption points within that workflow.
3. Identify meaningful variations using:
   - Android lifecycle behavior,
   - USB events,
   - Network activity,
   - weather station behavior, and
   - user interaction.
4. Create one variation test for each meaningful deviation.
5. Classify each completed test according to its intended purpose.

This process provides broad behavioral coverage while minimizing duplicated test
procedures.

---

# Test Classification

Each test case may belong to one or more test suites.

## Smoke

Quick confidence testing following development changes.

## Regression

Permanent tests intended to prevent previously corrected defects from
reappearing.

Executed before every release candidate.

## Compatibility

Tests executed on multiple Android versions and hardware platforms.

## Exploratory

Unscripted testing intended to discover previously unknown defects.

Exploratory testing supplements, but never replaces, the scripted test suite.

## Historical

A test case retained because it was executed in a previous release cycle 
but which no longer applies to the current application behavior. 
Historical test cases are not included in current release test requirements 
unless explicitly reinstated.

---

# Maintaining the Test Suite

When new functionality is added:

1. Update the User Guide.
2. Identify new workflows.
3. Identify new workflow variations.
4. Update the coverage analysis to reflect new workflows and variations.
5. Add or modify manual test cases to fill gaps in the updated coverage analysis.
6. Add permanent regression tests for any corrected defects.

When correcting a software defect, at least one permanent regression test should
normally be added before the issue is considered complete.

---

# Future Evolution

The testing methodology described here is intended to remain stable throughout
the lifetime of the project.

As Holfuy Upgrader gains new capabilities, new nominal workflows, workflow
variations, and regression tests will be added without changing the overall
organization of the test suite.