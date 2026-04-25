# メータープリセット仕様

## 対象

組み込みプリセットと、その保守ルールを定義する。

## 組み込みプリセット一覧

現在の built-in profiles:

- `BackUp-CT`
- `Mitsubishi-ME110SSR-MB`
- `DTSU666-HW`
- `YADA-YDS60-80`
- `Wave Energy-PWM-72`
- `DRPR-72/DVRR-72`

デフォルトプリセット:

- `BackUp-CT`

## 共通構造

各 `MeterProfile` は以下を持つ。

- `modelId`
- `displayName`
- `slaveId`
- `baudRate`
- `dataBits`
- `parity`
- `stopBits`
- `functionCode`
- `points`

各 `MeterPoint` は以下を持つ。

- `SignalType` による固定信号種別
- 表示名
- 開始アドレス
- レジスタ数
- gain
- data type
- word/byte order
- 初期 raw 値

## 通信設定

### BackUp-CT

- Function code
  - `0x03`
- Serial
  - `9600 / 8N1`

### Mitsubishi-ME110SSR-MB

- Function code
  - `0x03`
- Serial
  - `19200 / 8E1`

### DTSU666-HW

- Function code
  - `0x03`
- Serial
  - `9600 / 8N1`

### YADA-YDS60-80

- Function code
  - `0x03`
- Serial
  - `9600 / 8N1`

### Wave Energy-PWM-72

- Function code
  - `0x03`
- Serial
  - `9600 / 8N1`

### DRPR-72/DVRR-72

- Function code
  - `0x03`
- Serial
  - `9600 / 8N1`

## 編集ポリシー

- 組み込みプリセットはアプリ内で read-only
- ユーザー追加メーターは `Edit Meter` から編集可能
- 標準 22 項目テンプレートは固定名・固定 `SignalType` を持つ
- 未使用の標準項目はアドレス空欄で無効化する
- テンプレート外レジスタは `Add Register` で追加できる

## 標準 22 信号

標準テンプレートでは以下の固定信号種別を扱う。

- A相電圧
- B相電圧
- C相電圧
- A-B線電圧
- B-C線電圧
- C-A線電圧
- A相電流
- B相電流
- C相電流
- 有効電力
- A相有効電力
- B相有効電力
- C相有効電力
- 無効電力
- 力率
- 皮相電力
- 合計有効電力量
- 合計無効電力量
- 正方向合計有効電力量
- 正方向合計無効電力量
- 負方向合計有効電力量
- 負方向合計無効電力量

## 保守ルール

プリセット変更時は以下を守る。

- 検証済みの通信設定は、意図的に再検証する場合を除き維持する
- SmartLogger 互換性に関わる初期値は慎重に変更する
- ブロック読取互換のための欠番埋めは、可能ならダミーポイント追加ではなく「有効範囲内 `0x0000` 埋め」仕様で吸収する
- シミュレーションに関わるため、`SignalType` は意図した信号種別と一致させる

## 検証観点

主な検証ツール:

- SmartLogger
- QModMaster

主な確認項目:

- 通信条件が一致するか
- ブロック読取で `NA` や例外が出ないか
- マスター側表示値とアプリ側表示値が一致するか
- 有効範囲内ギャップが `0x0000` で埋まるか

## 関連コード

- `app/src/main/java/com/example/meterdemo/meter/profile/*`
- `app/src/main/java/com/example/meterdemo/meter/model/SignalType.kt`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
