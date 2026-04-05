# APK Update Release Flow

This app uses a public JSON file plus public GitHub Release assets for free APK updates.

## Public JSON

The app reads update metadata from:

`https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`

Expected format:

```json
{
  "versionCode": 3,
  "versionName": "1.2.0",
  "apkUrl": "https://github.com/40y4m4official-sudo/sow-modbus-demo/releases/download/v1.2.0/SOW-Modbus-Demo-v1.2.0-Release.apk"
}
```

## Release asset

Upload the signed APK to a public GitHub Release.

Recommended asset name:

`SOW-Modbus-Demo-v<versionName>-Release.apk`

## Update process

1. Build a signed release APK.
2. Create a public GitHub Release and upload the APK asset.
3. Update `app-update.json` in the main branch with the new `versionCode`, `versionName`, and `apkUrl`.
4. Commit and push `app-update.json`.

## Notes

- The repository must stay public for this direct update flow.
- The APK asset URL must be reachable without authentication.
- `versionCode` must increase for the app to detect an update.

