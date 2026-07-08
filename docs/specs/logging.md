# ログ仕様

## 対象

ログ保持、エクスポート、集計画面の仕様を記述する。

## ログモデル

各ログエントリは以下を持つ。

- category
- direction
- 必要に応じた HEX payload
- note / message
- timestamp

## カテゴリ

現在のカテゴリ:

- `SYSTEM`
- `MODBUS`
- `USB`

## 方向

現在の方向:

- `RX`
- `TX`
- `INFO`
- `ERROR`

## 保持ポリシー

- ログは `CommLogger` にメモリ保持する
- 最大保持件数
  - `5000`
- 新しいログを先頭へ追加する
- 容量超過時は古いログから破棄する

## 主なログ発生源

- Comm Test の手動要求 / 応答
- USB RX / TX 通信
- USB permission / connect / disconnect イベント
- `Dropped USB noise` のような再同期通知
- シミュレーション開始 / 停止
- プリセット変更
- slave ID 更新
- 値の適用 / リセット
- 更新確認 / ダウンロード状態

## エクスポート仕様

- 保存先
  - app external files dir を優先し、無ければ app files dir
  - `log_exports/` 配下
- ファイル名
  - `meter_demo_logs_yyyyMMdd_HHmmss.txt`
- 共有方法
  - `FileProvider` を使って共有シートを開く

### 1行の出力形式

各行には以下を含む。

- timestamp
- category
- direction
- 任意の HEX payload
- 任意の note

例:

- `[2026-03-27 12:54:02.351] USB / RX / 02 03 90 F9 00 04 B9 0B | USB RX`

## 集計仕様

集計画面は USB RX ログのみを対象にする。

集計キー:

- slave ID
- function code
- start address
- quantity

集計器の挙動:

- 分割された USB RX バイト列を再解析する
- 有効な 8 バイトの Modbus RTU 読取要求を再構成する
- 要求シグネチャ単位にグループ化する
- 件数、最終検出時刻、サンプル HEX を表示する

この集計の主な用途:

- SmartLogger が実際にどの開始アドレスを読んでいるか把握する
- 特定の要求パターンの頻度を確認する
- 期待アドレスが本当に読まれているかを確認する

## 解釈ルール

- 生の USB chunk はそのまま RTU 完全フレームとは限らない
- 高頻度ログでは目視より集計結果の方が信頼できる
- `Dropped USB noise` はフレーム再同期時に捨てられた先頭バイトを示す

## 現在の制約

- ログは手動エクスポートしない限りアプリ再起動後に残らない
- フィルタ機能は画面単位の簡易なものに留まる
- 集計は現在 8 バイトの読取要求を中心に扱う

## 関連コード

- `app/src/main/java/com/example/meterdemo/logging/*`
- `app/src/main/java/com/example/meterdemo/ui/LogsScreen.kt`
- `app/src/main/java/com/example/meterdemo/ui/LogSummaryScreen.kt`
- `app/src/main/java/com/example/meterdemo/usb/UsbRequestFrameAssembler.kt`

## 通信解析ビュー

通信解析モードでは既存ログを元に受信フレームを見やすく再集計する。

表示対象:

- 全ログビュー
- スレーブアドレス別サマリー
- 異常ログ一覧
- 通信品質サマリー

主な集計項目:

- 総フレーム数
- CRC エラー数
- フレーム途中切れ数
- タイムアウト数
- 応答時間の最小 / 平均 / 最大
- スレーブごとの成功 / Timeout / CRC / 例外件数

異常ログに含める代表例:

- Timeout
- CRC Error
- Exception 応答
- Frame truncated
- 想定外応答

注意:

- 通信解析は受動監視であり、解析結果は `USB / RX` を基準に構築する
- 通信解析モードでは `USB / TX` を新規送出しない