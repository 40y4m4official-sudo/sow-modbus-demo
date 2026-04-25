# Modbus 仕様

## 対象

Android アプリが Modbus RTU スレーブとして返す挙動を定義する。

## 対応要求

- 読取要求のみ対応する
- 使用可能な function code はアクティブなプロファイルに依存する
  - `0x03` Read Holding Registers
  - `0x04` Read Input Registers
- 1つのプロファイルが同時に持つ function code は 1 種類のみ
- 受信した function code が現在プロファイルと一致しない場合:
  - `Illegal Function (0x01)` を返す

## 要求検証ルール

### フレーム無視 (`null` 応答)

以下の場合は応答を返さない。

- フレーム長が 8 バイト未満
- CRC 不正
- slave ID が現在設定中の slave ID と一致しない

### 例外応答

以下の場合は例外応答を返す。

- function code が現在プロファイルと一致しない
  - `Illegal Function (0x01)`
- quantity が 0 以下
  - `Illegal Data Value (0x03)`
- 指定アドレス / 数量が現在プロファイルのルールで満たせない
  - `Illegal Data Address (0x02)`

## アドレス解決ルール

### 定義済みアドレス

- 対応する `MeterPoint` の値を返す

### 有効アドレス範囲内の未定義アドレス

- `0x0000` を返す

有効アドレス範囲は以下で自動算出する。

- `min(startAddress)`
- `max(startAddress + registerCount - 1)`

### 有効アドレス範囲外

- `Illegal Data Address (0x02)`

## 連続読取

- quantity が 1 でも複数でも処理する
- quantity に応じて必要なレジスタ列を順番に埋める
- ブロック読取時に途中の未定義アドレスがあっても、有効範囲内なら `0x0000` で埋めて応答を継続する

この仕様は以下のようなマスター互換のために重要である。

- SmartLogger のブロック読取
- QModMaster の連続読取確認

## データ型とレジスタ数

現在の UI 上のデータ型:

- `INT`
- `FLOAT`

解釈ルール:

- `INT`
  - register count に応じて 1〜4 ワード整数として扱う
- `FLOAT`
  - 現状は `FLOAT32` 相当
  - register count は 2 を前提とする

## 並び順

サポートする word/byte order:

- `MSB+MSB`
- `LSB+LSB`
- `MSB+LSB`
- `LSB+MSB`

エンジンは自然順バイト列を生成した後、指定 order を適用する。

## raw 値と表示値

- repository には raw 値を保持する
- UI 表示時は gain と型に応じて表示値へ変換する
- 手動入力時は表示値から raw 値へ戻して repository に反映する

## 例外応答の意味

- `0x01 Illegal Function`
  - プロファイルと異なる function code が来た
- `0x02 Illegal Data Address`
  - 有効範囲外、または要求数量と定義の組み合わせが成立しない
- `0x03 Illegal Data Value`
  - quantity など要求値自体が不正

## USB 受信との関係

- USB 側での分割受信 / ノイズ除去は `UsbRequestFrameAssembler` が担当する
- Modbus パースは、再構成済みの完全フレームに対して行う

## 関連コード

- `app/src/main/java/com/example/meterdemo/modbus/*`
- `app/src/main/java/com/example/meterdemo/meter/repository/*`
- `app/src/main/java/com/example/meterdemo/usb/UsbRequestFrameAssembler.kt`
