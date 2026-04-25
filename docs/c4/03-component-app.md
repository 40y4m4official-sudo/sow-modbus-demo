# コンポーネント図 - Android アプリ

## 目的

Android アプリ内部の主要コンポーネントと責務を整理する。

## 主要コンポーネント

### MainActivity

- ファイル
  - `app/src/main/java/com/example/meterdemo/MainActivity.kt`
- 責務
  - Android エントリーポイント
  - Compose content の起動
  - 保存済み言語の適用開始点

### MeterDemoApp

- ファイル
  - `app/src/main/java/com/example/meterdemo/ui/MeterDemoApp.kt`
- 責務
  - 画面全体の構成と遷移
  - 各画面コールバックを `MainViewModel` へ接続

### UI Screens

- ファイル
  - `app/src/main/java/com/example/meterdemo/ui/*`
- 責務
  - Main / Settings / Logs / Summary / Edit Meter / Add Meter を描画
  - `MainUiState` を表示へ変換
  - ユーザー操作を受け取り ViewModel の action を呼ぶ

### MainViewModel

- ファイル
  - `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- 責務
  - UI 全体の主要状態管理
  - repository / Modbus / USB / simulation / persistence / logging / update / language の統合制御
  - 表示値入力を raw 値へ変換
  - 起動時の保存状態復元
  - 更新チェック / ダウンロード / インストール起動

### MeterRepository

- ファイル
  - `app/src/main/java/com/example/meterdemo/meter/repository/MeterRepository.kt`
- 責務
  - 現在のアクティブメータープロファイル保持
  - レジスタ開始アドレスごとの raw 値保持
  - UI 用 snapshot 生成
  - 定義済みポイントから有効アドレス範囲を算出
  - 有効範囲内の未定義アドレスに対する `0x0000` 埋め可否判定

### ModbusRtuSlaveEngine

- ファイル
  - `app/src/main/java/com/example/meterdemo/modbus/ModbusRtuSlaveEngine.kt`
- 責務
  - RTU 要求フレーム検証
  - slave ID / function code の整合確認
  - 正常レスポンス / 例外レスポンス生成
  - register count と word/byte order を考慮したエンコード
  - 有効範囲内未定義アドレスの `0x0000` 埋め

### USB Serial Integration

- ファイル
  - `app/src/main/java/com/example/meterdemo/usb/*`
- 責務
  - USB / USB-serial デバイス検出
  - USB permission 要求
  - FTDI 系シリアルポート接続
  - 分割受信バイト列の収集
  - 完全な RTU 要求フレームへの再構成
  - シリアルポートへのレスポンス書き戻し

### MeterSimulationEngine

- ファイル
  - `app/src/main/java/com/example/meterdemo/meter/simulation/MeterSimulationEngine.kt`
- 責務
  - 表示値より細かい内部浮動小数点状態の保持
  - 電圧 / 電流 / 力率 / 電力 / 積算値の自動更新
  - `V × I × PF` のような物理関係の維持
  - 三相電流を共通基準値でまとまりのある挙動にする

### MeterPersistence

- ファイル
  - `app/src/main/java/com/example/meterdemo/persistence/MeterPersistence.kt`
- 責務
  - ユーザープロファイル保存
  - 選択中プロファイル / slave ID / raw 値 / Main 表示モードの保存

### Localization

- ファイル
  - `app/src/main/java/com/example/meterdemo/localization/*`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ja/strings.xml`
- 責務
  - 現在の UI 言語保存
  - 起動時の保存済み locale 適用
  - Settings 画面での言語切替
  - 対応言語のリソース提供

### CommLogger / Logging Components

- ファイル
  - `app/src/main/java/com/example/meterdemo/logging/*`
- 責務
  - カテゴリ付きログをメモリ保持
  - ログファイル出力
  - USB RX 要求を開始アドレス・数量単位で集計

### App Update Flow

- 主な実装場所
  - `MainViewModel`
- 補助要素
  - `FileProvider`
  - `app-update.json`
- 責務
  - 更新メタデータ JSON 取得
  - バージョン比較
  - APK ダウンロード
  - インストーラ起動または未知のアプリ権限画面遷移

## 主な関係

- 各 UI 画面は `MainViewModel` の `MainUiState` を表示する
- `MainViewModel` は以下へ依存する
  - `MeterRepository`
  - `ModbusRtuSlaveEngine`
  - `UsbSerialConnectionManager`
  - `UsbRequestFrameAssembler`
  - `MeterSimulationEngine`
  - `MeterPersistence`
  - `CommLogger`
  - `AppLanguageManager`
- `ModbusRtuSlaveEngine` は値取得を `MeterRepository` に委譲する
- USB 層は完成した要求フレームを `MainViewModel` 経由で `ModbusRtuSlaveEngine` に渡す
- シミュレーション結果は ViewModel を通じて `MeterRepository` に反映される

## 設計メモ

- 現状の主要オーケストレータは `MainViewModel`
- 更新機能は Play In-App Update API を使わず自前実装
- ログはまずメモリ保持し、必要時だけエクスポートする
- プリセット互換調整は profile 定義と repository / modbus 挙動で吸収する
