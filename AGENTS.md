# Repository Notes

## Versioning Policy

- Keep these four values aligned for every release:
  - `app/build.gradle.kts` -> `versionCode`
  - `app/build.gradle.kts` -> `versionName`
  - `app-update.json` -> `versionCode`
  - `app-update.json` -> `versionName`
- Release Git tag must match `versionName` using the format:
  - `vX.Y.Z`
- APK asset name must match `versionName` using the format:
  - `SOW-Modbus-Demo-vX.Y.Z-Release.apk`
- `app-update.json` must point to the matching public GitHub Release asset URL.

## Release Flow

When preparing a new release:

1. Update `versionCode` in `app/build.gradle.kts`
2. Update `versionName` in `app/build.gradle.kts`
3. Update `app-update.json`
   - `versionCode`
   - `versionName`
   - `apkUrl`
4. Build and verify the signed release APK
5. Commit the version bump
6. Create the Git tag `vX.Y.Z`
7. Push `main`
8. Push the tag
9. Create the GitHub Release
10. Upload the APK asset with the expected filename

## Documentation Update Rule

- When implementation changes are made, update the corresponding documentation in the same work whenever practical.
- At minimum, check whether the change affects:
  - `docs/specs/*.md`
  - `docs/c4/*.md`
  - `docs/README.md`
- If behavior, architecture, screen flow, release/update flow, or operational rules changed, reflect that change in the matching docs before closing the task.

## Update Distribution

- The app uses direct APK update distribution without Google Play in-app updates.
- The update metadata URL is public and should stay aligned with the current repository name.
- Current repository remote should use:
  - `https://github.com/40y4m4official-sudo/sow-modbus-demo.git`
- Public update JSON should be available at:
  - `https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`

## Security Notes

- Never commit:
  - `keystore.properties`
  - `release-keystore.jks`
  - release APK artifacts
- Keep signing credentials local only.

## UI / App Naming

- Launcher label should stay short:
  - `SMD`
- Release APK filename should stay branded:
  - `SOW-Modbus-Demo-vX.Y.Z-Release.apk`

## File Encoding Rule

- All text files in this repository must use `UTF-8 without BOM`.
- When creating or editing files, preserve or rewrite them as `UTF-8 without BOM`.
- Do not save source, resource, markdown, Gradle, JSON, XML, or config files in Shift_JIS, UTF-8 with BOM, UTF-16, or ANSI code pages.
- If mojibake or encoding ambiguity appears, fix the affected file and keep the repaired file in `UTF-8 without BOM`.
