# Changelog

<!---
https://keepachangelog.com/
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
--->

## [v1.9.2] - 2025-03-31

### Changed

- Update build environ
    - Android Studio Meerkat (Gradle 8.13, AGP 8.9.1)
    - Java 17
    - Google Play Location Services 21.3.0


## [v1.8.0] - 2023-05-26

### Added

- Add support for some new sensor types
- Add support for runtime permission checks in collective way

### Changed

- Integrate runtime permission handling tasks as `PermissionHandler`.
- Expand sensor interval timer resolution: 1 sec -> 100 ms
- GpsService: Handle cases for a location change event is being delivered in batch.
- FlpService: Adapt changes from the release of play-services-location (v21.0.0).
- Update build environ


## [v1.6.0] - 2021-12-22

### Added

- Support automatic location update for the output JSON data.

### Changed

- build.gradle: Use MavenCentral instead of jCenter
- build.gradle: Use JDK 11 instead of JDK 8, from Android Studio Arctic Fox.

- misc/Dockerfile: Update `openjdk`, `Command line tools` and `SDK Build Tools`.


## [v1.5.2] for Android - 2021-05-20

### Changed

- build.gradle: Update build environ for the Android Studio 4.1.2.


## [v1.4.0] - 2020-10-08

### Added

- Initial release

