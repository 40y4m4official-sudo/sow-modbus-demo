# 値変動シミュレーション仕様

## 対象

自動値変動機能の挙動と前提を定義する。

## 基本方針

- シミュレーションは Main 画面から ON/OFF する
- 手動入力と自動変動は同じ repository に反映される
- シミュレーション内部では、表示値より細かい浮動小数点状態を保持する
- 画面表示時にのみ丸める
- 可能な限り物理関係が崩れないようにする

## tick と時間

- 目標 tick 間隔
  - `1000 ms`
- 積算値の計算は固定 1 秒ではなく、実際の経過時間を使う
- 長時間停止後の暴走を防ぐため、経過秒数には上限を設ける

## 再初期化ルール

以下のタイミングで、内部シミュレーション状態を現在表示値から再シードする。

- シミュレーション開始時
- 選択プリセット / プロファイル変更時
- 手動で値を入力して適用した時
- 値のリセット時

つまり:

- 手動入力値は新しい内部初期値になる
- 自動運転は、その時点の値から継続する

## 電圧

対象信号:

- 各相電圧
- 線間電圧

挙動:

- 初期表示値を中心に random walk
- 変動範囲
  - 初期値の ±10%
- 毎 tick 少しずつ揺れる
- 範囲外へは clamp する

## 電流

対象信号:

- A相電流
- B相電流
- C相電流

### 共通基準値方式

三相で共通の符号付き基準電流を持つ。

内部状態:

- `baseValue`
- `targetBaseValue`
- `ticksUntilNextEvent`
- `transitionTicksRemaining`

符号は `baseValue` 自体に含める。

### 通常時

- `baseValue` 自体は変えない
- 各相は毎 tick 以下の範囲で個別に揺れる
  - `baseValue ±5%`

この方式により:

- 三相の傾向は揃う
- ただし完全一致にはならない

### 大変動

- イベント発生間隔
  - ランダム `60..200 tick`
- イベント開始時
  - 新しい符号付き `targetBaseValue` を決める
- 遷移時間
  - ランダム `1..10 tick`
- 遷移中
  - `baseValue` を `targetBaseValue` へ寄せる
  - 同時に各相はその時点の `baseValue ±5%` で揺れる
- 到達後
  - 次のイベントまでの tick 数を再生成する

## 力率

対象信号:

- 力率

挙動:

- 通常時は基準値付近を維持する
- たまに burst 的に低下する
- 次イベントまで
  - `15..35 tick`
- burst 継続時間
  - `3..7 tick`
- burst 中の値
  - 基準値の約 `45%..85%`
- 値は `-0.999..0.999` に clamp する

## 電力導出

有効電力は直接アニメーションさせず、電圧・電流・力率から導出する。

### 使用電圧

- 相電圧が定義されている場合
  - 各相電圧をそのまま使う
- 線間電圧しかない場合
  - `平均線間電圧 / sqrt(3)` を相電圧相当として使う

### 皮相電力

各相:

- `S_phase = V_phase × |I_phase| / 1000`

合計:

- `S_total = sum(S_phase)`

### 有効電力

各相:

- `P_phase = S_phase × PF × sign(I_phase)`

合計:

- `P_total = sum(P_phase)`

### 無効電力

各相の大きさ:

- `Q_phase_mag = S_phase × sqrt(1 - PF^2)`

符号:

- `P_phase` の符号に追従

合計:

- `Q_total = sum(Q_phase)`

## 積算値

積算値は電力と経過時間から導出する。

### 有効電力量

- `delta_kWh = P_total × elapsedHours`
- `P_total >= 0`
  - 正方向有効電力量へ加算
- `P_total < 0`
  - 負方向有効電力量へ加算
- 合計有効電力量が存在する場合
  - `forward + reverse`

### 無効電力量

- `delta_kVarh = |Q_total| × elapsedHours`
- 合計無効電力量へ絶対値で加算する

## 表示値と raw 値

- シミュレーション内部では表示値ドメインの浮動小数点を扱う
- `MainViewModel` が gain と型に応じて raw 値へ戻す
- `INT` は四捨五入して repository に書く
- `FLOAT` は 32-bit float bits として保持する

## 期待される見え方

- 電圧はゆっくり漂う
- 三相電流はおおむねまとまりつつ少し差が出る
- 電流は通常時に細かく揺れ、たまにまとまって大きく変動する
- 有効電力は `V × I × PF` の結果として自然に変動する
- 積算値は時間に応じて滑らかに増減する

## 関連コード

- `app/src/main/java/com/example/meterdemo/meter/simulation/MeterSimulationEngine.kt`
- `app/src/main/java/com/example/meterdemo/viewmodel/MainViewModel.kt`
