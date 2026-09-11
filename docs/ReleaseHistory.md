# Release History

This document records the relationship between Git releases and Google Play
releases.

`versionName` is the user-visible application version and may be associated
with many different `versionCode` values.

`versionCode` is the monotonically increasing build number required by
Google Play. Once a versionCode has been uploaded to Google Play, it cannot
be reused for another uploaded bundle of any kind, regardless of the Google
Play track involved.

| versionCode | versionName | Git Tag | Play Track | Date | Notes |
|------------:|-------------|---------|------------|------|-------|
| 1 | 1.0 | v1.0 | Internal testing | 2026-07-09 | Initial internal testing release. |
| 2 | 1.0.0 | v1.0.0-rc1 | Internal testing | 2026-07-17 | Release candidate including Help screen rotation fix (#5), redesigned USB permission workflow (#6), status message fix (#11), updated User's Guide, and Play Store assets. |
| 3 | 1.0.0 | v1.0.0-rc2 | Internal testing | 2026-09-11 | Release candidate, #13, #14, #15 |

## Notes

- Git tags identify the exact source code used to build each release.

- Release contents are documented in the [change log](../CHANGELOG.md).

- User-facing Play Store release text is maintained in
[what's new](../play-store-assets/whats-new).