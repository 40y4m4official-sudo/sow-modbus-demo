# Update Specification

## Scope

Document the APK self-update flow implemented without Google Play in-app update APIs.

## Update Source Model

The app uses public HTTP resources:
- update metadata JSON
- public GitHub Release APK asset

No Play Store in-app update API is used.

## Metadata URL

Build-time constant:
- `BuildConfig.APP_UPDATE_JSON_URL`

Current intended public URL:
- `https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`

## Remote JSON Schema

Expected fields:

```json
{
  "versionCode": 7,
  "versionName": "0.0.7",
  "apkUrl": "https://github.com/40y4m4official-sudo/sow-modbus-demo/releases/download/v0.0.7/SOW-Modbus-Demo-v0.0.7-Release.apk"
}
```

Rules:
- `versionCode` must be an integer
- `versionName` should match the release tag name without the leading `v`
- `apkUrl` must be the direct asset URL, not the GitHub release page URL

## Version Comparison Rules

Installed version source:
- Android package manager

Comparison rule:
- if remote `versionCode` is greater than installed `versionCode`, update is available
- otherwise app reports that the current version is already latest

## UI Behavior

Settings screen shows:
- current version
- status message
- latest version if known
- download progress if applicable
- one action button that changes role:
  - `Check for Update`
  - `Download and Install Update`
  - `Checking...`
  - `Downloading...`

## Download Behavior

- Uses `HttpURLConnection`
- HTTP GET for both JSON and APK
- APK target path:
  - app-private files directory under `files/updates/meterdemo-update.apk`
- Download progress uses `contentLength` when available
- Progress percent is stored in UI state when calculable

## Installer Launch Flow

After a successful download:
1. app checks `PackageManager.canRequestPackageInstalls()`
2. if install permission is missing:
   - launch `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`
   - show guidance message to retry after enabling
3. if permission is available:
   - create content URI via `FileProvider`
   - launch installer with MIME type `application/vnd.android.package-archive`

## Android Requirements

Manifest requirements:
- `android.permission.INTERNET`
- `android.permission.REQUEST_INSTALL_PACKAGES`
- configured `FileProvider`

## Operational Assumptions

- update hosting is public
- APK is signed with the same signing key lineage required for successful update install
- repository versioning is kept aligned with:
  - `app/build.gradle.kts`
  - `app-update.json`
  - git tag
  - release APK filename

## Failure States

Handled user-visible failures include:
- failed metadata retrieval
- failed APK download
- missing unknown-sources permission

The update flow does not currently include:
- delta updates
- signature verification beyond normal Android package install checks
- background download service
- resumable downloads

## Related Code

- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/ui/SettingsScreen.kt`
- `app-update.json`
- `docs/APP_UPDATE_RELEASE_FLOW.md`
- `AGENTS.md`
