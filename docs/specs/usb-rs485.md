# USB-RS485 仕様

## 対象

USB シリアル接続と RS485 通信処理の仕様を定義する。

## 対応アダプタ前提

主対象:

- FTDI 互換 USB シリアルアダプタ
- 代表例
  - DSD TECH SH-U11C

デバイス検出には `usb-serial-for-android` の標準 probing を利用する。

## 検出フロー

Settings 画面では以下を提供する。

- 一般 USB デバイス再検出
- USB serial デバイス再検出

画面へ表示する情報:

- デバイスラベル
- デバイスパス / 名前
- VID:PID
- permission 状態

## permission フロー

- 選択した serial device ごとに USB permission を要求する
- 結果は `UsbSerialConnectionManager.Listener` 経由で受け取る
- permission 状態は UI とログへ反映する

## 接続フロー

ユーザーが `Connect` を押した時:

1. 現在プロファイルの通信条件を読む
2. 対象 serial port を開く
3. 以下のパラメータを設定する
   - baud rate
   - 8 data bits
   - parity
   - stop bits
4. DTR / RTS を true にする
5. `SerialInputOutputManager` を開始する

切断が起こる契機:

- ユーザー操作
- 再接続前処理
- I/O エラー
- ViewModel の clear

## 通信条件の供給元

現在の通信条件はアクティブなメータープロファイルから読む。

- `baudRate`
- `parity`
- `stopBits`

UI で扱う baud rate:

- `1200`
- `2400`
- `4800`
- `9600`
- `19200`
- `115200`

parity:

- None
- Odd
- Even

stop bits:

- 1
- 2

## フレーム再構成

USB 読取は分割受信やノイズ混入を含みうる。

`UsbRequestFrameAssembler` の役割:

- 受信バイト列をバッファへ蓄積する
- 最初に見つかる有効な 8 バイト Modbus RTU 要求を探索する
- 以下を検証する
  - slave ID
  - function code
  - CRC
  - parser 成功
- 完成フレームを emit する
- 先頭ノイズを必要に応じて破棄する
- バッファ末尾の有望な開始バイトは次回のため保持する

再構成対象 function code:

- `0x03`
- `0x04`

## ViewModel での処理順

受信 chunk ごとの流れ:

1. 生バイト列を `USB / RX` としてログ化
2. frame assembler に追加
3. 破棄したノイズがあれば USB ログへ記録
4. 完成フレームを `ModbusRtuSlaveEngine` に渡す
5. 応答があれば USB へ書き戻し、`USB / TX` としてログ化
6. 処理不能な要求ならエラーログを残す

## エラー処理

主なエラー:

- permission denied
- device not found
- failed to open USB device
- no serial port found
- connect failed
- write failed
- I/O stopped
- request not handled

## 制約

- 常駐 Android Service でのバックグラウンド通信は未対応
- 複数デバイス同時セッションは未対応
- 現状は read-only の Modbus スレーブ動作を前提とする

## 関連コード

- `app/src/main/java/com/example/meterdemo/usb/*`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/example/meterdemo/modbus/*`

## 通信解析モードでの挙動

通信解析モードは現場向けの受動監視モードである。

要件:

- 受信した USB シリアルデータはログ化と解析のみに使う
- Modbus 応答は生成しても USB へ書き戻さない
- 現場機器への影響を避けるため、RS-485 ラインに対する送信は行わない

ViewModel での処理:

1. 生バイト列を `USB / RX` として記録する
2. 受信データを解析トラッカーへ渡す
3. スレーブ別状態、異常、通信品質を更新する
4. `UsbSerialConnectionManager.write(...)` は呼ばない

通常のメーターデモモードとの差分:

- メーターデモモード: 03H / 04H 要求を組み立てて必要に応じて応答する
- 通信解析モード: 受信専用で、応答や Comm Test を行わない
## モード切替時の安全動作

`メーターデモ` と `通信解析` を切り替える際は、操作ミスで旧モードのまま通信を継続しないよう、一度 USB シリアル接続を切断する。

- 切替前に接続中なら `disconnect()` を実行する
- 切替後は必要に応じてオペレーターが再接続する
- 通信解析モードへの切替時は、切替直後に応答送信が残らないことを優先する