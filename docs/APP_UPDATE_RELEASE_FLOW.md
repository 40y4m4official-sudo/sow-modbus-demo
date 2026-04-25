# APK 更新リリース手順

このアプリは、公開 JSON と公開 GitHub Release asset を使って、無料で APK 更新を配布する。

## 公開 JSON

更新メタデータは次の URL から取得する。

`https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`

想定フォーマット:

```json
{
  "versionCode": 8,
  "versionName": "0.1.0",
  "apkUrl": "https://github.com/40y4m4official-sudo/sow-modbus-demo/releases/download/v0.1.0/SOW-Modbus-Demo-v0.1.0-Release.apk"
}
```

## Release asset

署名済み release APK を public GitHub Release にアップロードする。

推奨 asset 名:

`SOW-Modbus-Demo-v<versionName>-Release.apk`

## 更新手順

1. 署名付き release APK をビルドする
2. public GitHub Release を作成し、APK asset をアップロードする
3. `main` ブランチ上の `app-update.json` を更新する
   - `versionCode`
   - `versionName`
   - `apkUrl`
4. `app-update.json` をコミットして push する

## 注意事項

- この直接更新フローを使うため、リポジトリは public のまま維持する
- APK asset の URL は認証なしで取得できる必要がある
- アプリが更新を検知するため、`versionCode` は必ず増加させる
