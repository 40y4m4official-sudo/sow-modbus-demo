# ドキュメント一覧

このディレクトリには、本プロジェクトの構造資料と機能仕様書を配置します。

## C4 アーキテクチャ

- [01 システムコンテキスト](/f:/app-android/docs/c4/01-system-context.md)
- [02 コンテナ図](/f:/app-android/docs/c4/02-container.md)
- [03 コンポーネント図 - Android アプリ](/f:/app-android/docs/c4/03-component-app.md)
- [04 コードマップ](/f:/app-android/docs/c4/04-code-map.md)

## 機能仕様

- [Modbus 仕様](/f:/app-android/docs/specs/modbus.md)
- [メータープリセット仕様](/f:/app-android/docs/specs/presets.md)
- [値変動シミュレーション仕様](/f:/app-android/docs/specs/simulation.md)
- [USB-RS485 仕様](/f:/app-android/docs/specs/usb-rs485.md)
- [ログ仕様](/f:/app-android/docs/specs/logging.md)
- [UI 仕様](/f:/app-android/docs/specs/ui.md)
- [更新機能仕様](/f:/app-android/docs/specs/update.md)

## リリース / 運用

- [APK 更新リリース手順](/f:/app-android/docs/APP_UPDATE_RELEASE_FLOW.md)
- [運用ルール / バージョン管理](/f:/app-android/AGENTS.md)

## 実装言語と主な担当機能

### Kotlin

アプリ本体の主要機能は Kotlin で実装しています。

- Android エントリーポイント
- 画面状態管理
- Modbus RTU スレーブ処理
- USB-RS485 接続処理
- メータープリセット管理
- 値変動シミュレーション
- ログ収集 / 集計 / エクスポート
- APK 更新チェック / ダウンロード / インストール起動
- 多言語切替制御

主な配置:

- `app/src/main/java/...`

### Jetpack Compose（Kotlin）

UI は Jetpack Compose を使って Kotlin で実装しています。

- Main
- Settings
- Edit Meter
- Add Meter
- Logs
- Log Summary
- 共通ヘッダー
- ダイアログ / ボタン / 一覧表示

主な配置:

- `app/src/main/java/com/example/meterdemo/ui/*`

### Android XML

Android 固有の宣言やリソースは XML で管理しています。

- AndroidManifest
- 文字列リソース
- FileProvider 定義
- 一部 drawable / icon 定義

主な配置:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/**/*.xml`

### Gradle Kotlin DSL

ビルド設定は Kotlin DSL で管理しています。

- `versionCode` / `versionName`
- APK ファイル名
- 依存関係
- signingConfig
- release buildType
- BuildConfig 定数

主な配置:

- `app/build.gradle.kts`
- `settings.gradle.kts`

### JSON

更新配布メタデータは JSON で管理しています。

- 最新 `versionCode`
- 最新 `versionName`
- APK 直リンク

主な配置:

- `app-update.json`

### Markdown

設計資料、仕様書、運用ルールは Markdown で記述しています。

- C4 ドキュメント
- 各機能仕様
- リリース手順
- 運用ルール

主な配置:

- `docs/**/*.md`
- `AGENTS.md`

## 記述ルール

- `docs/c4/` は構造、責務、関係性を中心に記述する
- `docs/specs/` は画面挙動、機能仕様、運用条件を中心に記述する
- 実装を変更した場合は、対応する仕様書と必要に応じて C4 側も更新する
- `docs/` 配下の文書は原則として日本語で記述する
- 識別子、クラス名、API 名、プロトコル名、製品名などは必要に応じて英語表記を維持する
