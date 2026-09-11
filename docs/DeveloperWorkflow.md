# Developer Workflow

This document describes the standard development and release workflow for
Holfuy Upgrader.

## Forks and Branching

Development is never performed directly on the [upstream repository](https://github.com/MaileTechnical/HolfuyConfigTool-Android).
Instead, each developer forks the upstream repository and operates on that fork.

Each logical change is developed on its own branch within the developer's fork.

Examples:

- `feature/...`
- `issue/...`
- `doc/...`
- `release/...`

Whenever possible, each new branch is created from a local `master` branch that has been synchronized with the current upstream `master` branch.

## Development

1. Open an [issue](https://github.com/MaileTechnical/HolfuyConfigTool-Android/issues) if necessary.  Each feature or defect must be covered by an issue.
2. Ensure the `master` branch of the developer fork is synchronized with the `master` branch in the upstream repository.
3. Update the local `master` branch from the `master` branch of the developer fork.
4. Create a new (local) development branch using the naming convention shown above, and check it out.  If an issue is associated with the branch, include the issue number in the branch name.
5. Implement and test the change, making semantically cohesive commits and pushing those commits to the developer fork.
6. Update the associated issue, storing a link to the branch along with an explanation of the work.
7. Open a pull request to pull the development branch into the upstream `master` branch.

## Servicing Pull Requests (maintainers)

1. Review the changes.
2. Request changes or clarification if necessary.
3. Merge the pull request.
4. Update the associated issue if there is one.

## Synchronizing Repositories

After merging:

1. Synchronize the `master` branch of the developer fork with the `master` branch of the upstream repository.
2. Pull the `master` branch of the developer fork into the local `master` branch.


## Documentation

Documentation changes are committed with the feature or bug fix they
describe whenever practical.

The repository maintains:

- README
- User's Guide
- Privacy Policy
- Test Plan
- CHANGELOG
- Release History
- Google Play assets

## Pre-release Process

1. Optimize imports.  Let Android Studio remove unnecessary imports and sort the import list.  
    - Select "app" in the left panel of the IDE.
    - Select "Optimize imports" on the CME
2. Inspect code.  Let Android Studio inspect the entire project.
    - Select "app" in the left panel of the IDE.
    - Select "Analyze > Inspect code" in the CME
3. Build release-candidate code and then:
    - Smoke-test on primary test device
    - Make new screenshots for Play Store and README, storing them in repo and README

## Release Process

1. Create a release branch from `master`.
2. Merge each branch (feature, issue, documentation, etc.) targeted for this release into the newly created release-candidate branch.
3. Update:
   - `versionName`
   - `versionCode`
   - README
   - CHANGELOG
   - Release History
   - Google Play assets, as required
   - Test record, creating a blank one for the release
4. Merge release-candidate branch into the upstream master.
5. Create a Git tag on the upstream master identifying the source from which the release is built (e.g., v1.0.0-rc1, v1.0.0, etc.) annotating it with `versionCode` and Play track.
6. Note that some test cases require an override of the URL for the manifest.  These test cases must be executed using a debug APK.  All other test cases must be executed with a release APK.
6. Build debug APK from the upstream master:
   ```bash
   ./gradlew clean assembleDebug
   ```
7. Build release APK from the upstream master:
   ```bash
   ./gradlew clean assembleRelease
   ```
8. Install the debug APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
9. Execute all test cases that require an override of the manifest URL.
10. Install release APK:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```   
11. Execute all test cases that do not require an override of the manifest URL.
12. Build a signed Android App Bundle:
   ```bash
   ./gradlew clean bundleRelease
   ```
13. Update test record and commit it to master on the upstream repo.
14. Upload the bundle to the Google Play Console.
15. Upload updated Play Store assets, screenshots, and "What's New" text if necessary.
16. Submit the release for review by Play.
17. After approval from Play, install the app from the appropriate Play testing track and perform a brief acceptance test.
18. Promote the release to the next Play track if appropriate.

## Google Play Console

Google Play Console treats **Internal Testing releases** and **Google Play Store listing/metadata changes** as separate workflows.

* **Internal Testing:** The **Internal Testing** page is the authoritative source for the status of the uploaded app release. Use it to verify that the release has been processed and is available to testers.
* **Store listing and metadata:** Changes to the Play Store listing, such as descriptions, graphics, or other store metadata, are tracked separately through **Publishing Overview**. A pending change shown there does not necessarily indicate that the Internal Testing release is still pending.
* **Release verification:** Do not use **Publishing Overview** as the authoritative indication that an Internal Testing APK/AAB has been published. Check the **Internal Testing** page directly.
* **Store-listing verification:** Conversely, use **Publishing Overview** to determine whether store listing or other metadata changes remain pending publication.

## Guiding Principles

- One logical change per branch.
- One authoritative copy of every artifact.
- Keep `master` releasable.
- Verify every release using the documented test plan.