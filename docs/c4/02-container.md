# コンテナ図

## 目的

システムを構成する実行単位 / 配布単位を高レベルで整理する。

## コンテナ

### 1. Android アプリ

- 技術要素
  - Kotlin
  - Jetpack Compose
  - Android USB Host API
  - `usb-serial-for-android`
- 責務
  - UI 表示
  - プリセット / ユーザーメーター管理
  - 現在 raw 値保持
  - Modbus RTU スレーブ応答
  - USB シリアル接続管理
  - 値変動シミュレーション
  - ローカル保存
  - ログエクスポート
  - 更新確認 / ダウンロード / インストール起動

### 2. ローカルアプリストレージ

- 技術要素
  - app-private files
  - SharedPreferences / ローカル保存領域
- 責務
  - ユーザープロファイル保存
  - 選択中プロファイル / slave ID / raw 値 / 表示モード保存
  - ダウンロード済み APK の一時保存
  - ログエクスポートファイル保存

### 3. 公開更新メタデータ

- 技術要素
  - GitHub raw 上の静的 JSON
- 責務
  - 最新 `versionCode` 公開
  - 最新 `versionName` 公開
  - APK 直リンク公開

### 4. 公開 APK asset 配布

- 技術要素
  - GitHub Releases
- 責務
  - 署名済み APK を直接ダウンロード可能な形で配布する

### 5. 外部マスターシステム

- 代表例
  - SmartLogger
  - QModMaster
- 責務
  - Modbus RTU 読取要求を送る
  - 返却値を解釈する
  - プリセット互換性を検証する

### 6. USB-RS485 ハードウェア経路

- 技術要素
  - Android USB Host
  - FTDI 対応アダプタ
  - RS485 配線
- 責務
  - Android アプリと外部マスター間でシリアルデータを伝送する

## データフロー概要

1. オペレーターがアプリを起動し、設定を行う
2. 外部マスターが RS485 経由で Modbus RTU 読取要求を送る
3. USB シリアル層がバイト列を受信し、完全なフレームへ再構成する
4. Modbus エンジンが要求を検証し、リポジトリ内の値からレスポンスを生成する
5. レスポンスを USB シリアルへ返送する
6. ログは保存・集計され、必要に応じてエクスポートされる
7. 更新時は公開 JSON を取得し、バージョン比較後に APK をダウンロードし、インストーラを起動する

## コンテナ境界

- 直接コード管理している実行コンテナは Android アプリのみ
- GitHub raw JSON と GitHub Releases は受動的な配布コンテナ
- 外部マスター機器と USB ハードウェアは統合先であり、本プロジェクト所有のコンテナではない

## 運用メモ

- release APK の命名規則
  - `SOW-Modbus-Demo-vX.Y.Z-Release.apk`
- 更新 JSON の想定 URL
  - `https://raw.githubusercontent.com/40y4m4official-sudo/sow-modbus-demo/main/app-update.json`
- Release asset は同じ public リポジトリの GitHub Releases に配置する
