# 更新機能仕様

## 対象

Google Play In-App Update API を使わない APK 直接更新フローを定義する。

## 更新元モデル

アプリは public な HTTP リソースを利用する。

- 更新メタデータ JSON
- public GitHub Release の APK asset

Play Store の in-app update API は使わない。

## メタデータ URL

ビルド時定数:

- `BuildConfig.APP_UPDATE_JSON_URL`

現在の想定 URL:

- `https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`

## JSON スキーマ

期待するフィールド:

```json
{
  "versionCode": 8,
  "versionName": "0.1.0",
  "apkUrl": "https://github.com/40y4m4official-sudo/sow-modbus-demo/releases/download/v0.1.0/SOW-Modbus-Demo-v0.1.0-Release.apk"
}
```

ルール:

- `versionCode` は整数
- `versionName` は Git tag の `v` を外した値と一致させる
- `apkUrl` は Release ページではなく asset 直リンクを使う

## バージョン比較

インストール済みバージョンの取得元:

- Android PackageManager

判定:

- remote `versionCode` が現在より大きければ更新あり
- それ以外は最新版とみなす

## UI 挙動

Settings 画面の更新セクションには以下を表示する。

- 現在バージョン
- 状態メッセージ
- 既知なら最新バージョン
- 必要ならダウンロード進捗
- 状態に応じて役割が変わる単一ボタン
  - `Check for Update`
  - `Download and Install Update`
  - `Checking...`
  - `Downloading...`

## ダウンロード仕様

- `HttpURLConnection` を使う
- JSON も APK も HTTP GET
- APK 保存先
  - app-private files dir の `files/updates/meterdemo-update.apk`
- `contentLength` が取得できれば進捗率を計算する
- 計算可能なときのみ進捗 percent を UI 状態へ持つ

## インストール起動フロー

ダウンロード成功後:

1. `PackageManager.canRequestPackageInstalls()` を確認する
2. 権限が無ければ
   - `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` を開く
   - 許可後に再試行するよう案内する
3. 権限があれば
   - `FileProvider` で content URI を作る
   - MIME type `application/vnd.android.package-archive` でインストーラを起動する

## Android 要件

Manifest 上必要なもの:

- `android.permission.INTERNET`
- `android.permission.REQUEST_INSTALL_PACKAGES`
- `FileProvider` 設定

## 運用前提

- 更新配布は public URL で行う
- APK は同一署名系統で署名されている必要がある
- 次のバージョン情報は常に揃える
  - `app/build.gradle.kts`
  - `app-update.json`
  - Git tag
  - release APK ファイル名

## 失敗状態

ユーザーに見える主な失敗:

- 更新メタデータ取得失敗
- APK ダウンロード失敗
- 未知のアプリ権限不足

現時点で未対応:

- 差分更新
- Android 標準署名確認以上の追加署名検証
- バックグラウンドサービスによる継続ダウンロード
- 再開可能ダウンロード

## 関連コード

- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/ui/SettingsScreen.kt`
- `app-update.json`
- `docs/APP_UPDATE_RELEASE_FLOW.md`
- `AGENTS.md`
