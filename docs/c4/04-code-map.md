# コードマップ

## 目的

概念上のコンポーネントと、実際のファイル配置を対応付けます。

## 実装言語ごとの役割

### Kotlin

主な担当:

- アプリ本体ロジック
- Modbus RTU スレーブ応答
- USB-RS485 通信
- 状態管理
- シミュレーション
- 更新機能
- ログ機能
- 多言語切替制御

主な配置:

- `app/src/main/java/**/*`

### Jetpack Compose（Kotlin）

主な担当:

- 各画面 UI
- 画面ヘッダー
- アイコン付き操作
- ダイアログ
- 一覧 / カード表示

主な配置:

- `app/src/main/java/com/example/meterdemo/ui/*`

### XML

主な担当:

- Android Manifest
- 文字列リソース
- アイコン / FileProvider / drawable 定義

主な配置:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/**/*.xml`

### Gradle Kotlin DSL

主な担当:

- ビルド設定
- release 設定
- 署名設定
- バージョン情報
- APK 出力名
- BuildConfig 定数

主な配置:

- `app/build.gradle.kts`
- `settings.gradle.kts`

### JSON

主な担当:

- 更新配布用メタデータ

主な配置:

- `app-update.json`

### Markdown

主な担当:

- C4 資料
- 機能仕様
- リリース / 運用ドキュメント

主な配置:

- `docs/**/*.md`
- `AGENTS.md`

## 対応表

### エントリーポイント

- `app/src/main/java/com/example/meterdemo/MainActivity.kt`

### アプリ構成 / 画面遷移

- `app/src/main/java/com/example/meterdemo/ui/MeterDemoApp.kt`

### メイン状態 / オーケストレーション

- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewMode.kt`

### UI 画面

- `app/src/main/java/com/example/meterdemo/ui/MeterValuesScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/SettingsScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/LogsScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/LogSummaryScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/EditMeterScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/AddMeterScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/ScreenHeader.kt`

### 多言語対応

- `app/src/main/java/com/example/meterdemo/localization/AppLanguage.kt`
- `app/src/main/java/com/example/meterdemo/localization/AppLanguageManager.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ja/strings.xml`

### メータードメインモデル

- `app/src/main/java/com/example/meterdemo/meter/model/SignalType.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/MeterPoint.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/MeterProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/StandardSignalTemplate.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/WordByteOrder.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/SerialParity.kt`
- `app/src/main/java/com/example/meterdemo/meter/model/DataType.kt`

### 組み込みプリセット

- `app/src/main/java/com/example/meterdemo/meter/profile/BackUpCtProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/MitsubishiMe110SsrMbProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/Dtsu666HwProfile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/YadaYds60_80Profile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/WaveEnergyPwm72Profile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/Drpr72Dvrr72Profile.kt`
- `app/src/main/java/com/example/meterdemo/meter/profile/MeterProfiles.kt`

### Repository / 値処理

- `app/src/main/java/com/example/meterdemo/meter/repository/MeterRepository.kt`

### シミュレーション

- `app/src/main/java/com/example/meterdemo/meter/simulation/MeterSimulationEngine.kt`

### Modbus

- `app/src/main/java/com/example/meterdemo/modbus/ModbusRtuSlaveEngine.kt`
- `app/src/main/java/com/example/meterdemo/modbus/ModbusFrameParser.kt`
- `app/src/main/java/com/example/meterdemo/modbus/ModbusCrc.kt`

### USB / シリアル

- `app/src/main/java/com/example/meterdemo/usb/UsbDeviceScanner.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbSerialScanner.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbSerialConnectionManager.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbRequestFrameAssembler.kt`

### ログ

- `app/src/main/java/com/example/meterdemo/logging/CommLog.kt`
- `app/src/main/java/com/example/meterdemo/logging/CommLogger.kt`
- `app/src/main/java/com/example/meterdemo/logging/LogExporter.kt`
- `app/src/main/java/com/example/meterdemo/logging/LogAddressSummaryAnalyzer.kt`

### 永続化

- `app/src/main/java/com/example/meterdemo/persistence/MeterPersistence.kt`

### ビルド / リリース / 更新

- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app-update.json`
- `docs/APP_UPDATE_RELEASE_FLOW.md`
- `AGENTS.md`

### テスト

- `app/src/test/java/com/example/meterdemo/modbus/*`
- `app/src/test/java/com/example/meterdemo/meter/repository/*`
- `app/src/test/java/com/example/meterdemo/usb/*`
- `app/src/test/java/com/example/meterdemo/logging/*`

## トレーサビリティルール

機能変更時は以下をセットで更新します。

- 実装ファイル
- 対応する `docs/specs/*.md`
- 構造や責務が変わる場合は `docs/c4/*.md`
