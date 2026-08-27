# Firmware Repository Test Data

This directory is a toolbox of reusable firmware-repository test fixtures. It is intentionally **not organized by test case**. Test cases compose these building blocks as needed.

## Disposition fixtures

- `Dispositions/Current/Repository/current.bin` — manifest checksum matches the physical file, establishing **Current**.
- `Dispositions/Outdated/Repository/outdated.bin` — existing fixture, retained byte-for-byte. A manifest checksum mismatch plus an unavailable download establishes **Outdated**.
- `Dispositions/Custom/Repository/custom.bin` — intentionally absent from the manifest, establishing **Custom**.
- `Dispositions/SelectedMissing/Repository/vanish.bin` — manifest checksum initially matches. Select it, delete it, and resume the app to exercise a selected file becoming **Missing**.
- `Unavailable/` — deliberately contains no downloadable firmware files; use it for manifest URLs where a download must fail.

## Combined disposition manifest

`Manifests/Dispositions/all.json` combines the manifest entries needed for TC-021. It describes Current, Outdated, Missing, and the initially-Current `vanish.bin`; `custom.bin` is intentionally omitted.

The manifest uses `<DEVELOPMENT-BRANCH>` in the GitHub URL so the fixture can be tested on a branch of the development fork. For the authoritative test suite, replace the host/branch component with the upstream repository and a versioned test-data tag.

All manifest download URLs point into `Unavailable/`. This is intentional: the physical repository files establish the initial states, while failed downloads preserve Outdated/Missing and prevent `vanish.bin` from being silently restored after deletion.

## SHA-256

| File | Bytes | SHA-256 |
|---|---:|---|
| `Dispositions/Current/Repository/current.bin` | 35 | `97f7606d6c6849ec4c4c140dc25951465ab36fb1e667fec4ffef1437e0c00039` |
| `Dispositions/Outdated/Repository/outdated.bin` | 39 | `73907bd77f938f6b1446954d4f3363993455c9d19b337720c9c42c23700b6ae5` |
| `Dispositions/Custom/Repository/custom.bin` | 34 | `c3b833f3106125306364e533bf4a8fd15caf4b37bdf21f1d3131b69dcccb896e` |
| `Dispositions/SelectedMissing/Repository/vanish.bin` | 34 | `40609b10cf6b28fc631956ed4df974989a7d0f362a13be37b26630a6b3cde78c` |
