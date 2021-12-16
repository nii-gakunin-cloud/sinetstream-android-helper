<!--
Copyright (C) 2020-2021 National Institute of Informatics

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# SINETStreamHelper

## 概要

SINETStreamHelperライブラリ（以降、本ライブラリと記述）は、Android端末
の具備する各種センサーデバイスの読み取り値をJSON形式に整形して出力する
ものである。

以下に示すように、本ライブラリはセンサー制御部分と位置情報の追跡部分と
いう2つの機能モジュールが独立に並存する形で構成される。
後者の利用は任意であり、Android端末の移動に伴い位置情報を継続的に追跡
取得したい場合に起動されることを想定する。

```
        #--------------------------------------+
        |           User Application           |
        +--------------------------------------+
             |   A                  |   A
    =========|===|==================|===|================= API functions
             |   |                  |   |
      +------|-- |------------------|---|---------------------+
      |      V   | [JSON]           V   | [Location]          |
      | +----------------+    +------------------+            |
      | | Sensor Handler |    | Location Handler |            |
      | +----------------+    +------------------+            |
      |      |   A                  |   A                     |
      |      |   |                  |   |   SINETStreamHelper |
      +------|---|------------------|---|---------------------+
             |   |                  |   |
    =========|===|==================|===|================ Android System
            [ ... ]                [ ... ]
    =========|===|==================|===|======================= Devices
             |   |                  |   |
             V   | [Raw Data]       V   |  [Raw Data]
         +----------+           +----------+
         | Sensor   |+          | Location |+
         | Device   ||+         | Source   ||+
         +----------+||         +----------+||
          +----------+|          +----------+|
           +----------+           +----------+
```
〈凡例〉
* Sensor Handler
    * Android端末の具備するセンサーデバイスの読み取り値を収集する機能モジュール
* Location Handler
    * GPSなどの情報源経由でAndroid端末の位置情報を追跡取得する機能モジュール

## センサー情報の取得

本ライブラリを使うアプリケーションは、以下の要領でセンサー情報を取得する。
いずれも非同期操作となるため、操作要求に対する応答、あるいは非同期通知が
あることを前提にユーザアプリケーションを実装する必要がある。

```
1) AndroidシステムのSensorサービスに結合
2) センサー種別一覧取得
3) センサー種別群の登録
4) センサー情報の非同期通知
5) センサー種別群の削除
6) AndroidシステムのSensorサービスと切断
```

上記3)により、指定されたセンサー群が有効となる。
システムから随時通知されるセンサー値はいったん「SINETStreamHelper」内部で
蓄積され、送信契機が一定の頻度を越えないよう制御しながらコールバック関数
でJSONデータがユーザに通知される。
この動作は、上記5)により当該センサー群が登録削除されるまで続く。
もちろんアプリケーションの生存期間中に3)と5)を繰り返して構わない。

## 位置情報の追跡

本ライブラリを使うアプリケーションは、以下の要領で位置情報を追跡取得する。
いずれも非同期操作となるため、操作要求に対する応答、あるいは非同期通知が
あることを前提にユーザアプリケーションを実装する必要がある。

```
1) 実行権限の検査
2) AndroidシステムのLocationManager経由で位置情報源に結合
3) 位置情報の非同期通知
4) AndroidシステムのLocationManager経由で位置情報源と切断
```

上記2)で位置情報の更新要求が受け付けられると、最新の位置情報が継続的に
コールバック関数でユーザに通知される。
位置情報源としてGPSを使う場合は「移動の変位量が閾値を超えた場合」、
融合された位置予測プロバイダ（FLP: Fused Location Provider）を使う場合は
「移動の有無に関わらず周期的に」通知される。
この動作は上記4)で位置情報取得がキャンセルされるまで続く。

## JSONデータ形式

Android端末上で収集したセンサー情報は、下記に示すJSON形式に変換され、  
コールバック関数でユーザに通知される。
ここでは人間が見やすいよう整形し、（本来JSONで許されない）コメントも
追記しているが、実際は改行無しで詰めた形式で出力される。

```
{
    # デバイス情報
    "device": {
        # システム情報（自動取得）
        "sysinfo": {
            "android": "8.0.0",
            "model": "Android SDK built for x86",
            "manufacturer": "Google"
        },
        # ユーザ情報（利用者が設定）
        "userinfo": {
            "note": "Say something",
            "publisher": "hello@world"
        },
        # 位置情報（利用者が設定、または自動更新）
        "location": {
            "latitude": "3.400000",
            "longitude": "1.200000"
        }
    },
    # センサー情報（自動取得）
    "sensors": [
        # ベクター値の例：加速度計
        {
            "type": "accelerometer",
            "name": "Goldfish 3-axis Accelerometer",
            "timestamp": "20200521T150130.866+0900",
            "values": [
                -1.0407600402832031,
                9.667730331420898,
                1.2991900444030762
            ]
        },
        # スカラー値の例：照度計
        {
            "type": "light",
            "name": "Goldfish Light sensor",
            "timestamp": "20200521T150130.865+0900",
            "value": 9894.7001953125
        },
        ...
    ]
}
```

## 本ライブラリの利用方法

### ビルド環境設定：参照先リポジトリおよび依存関係

ユーザアプリケーション開発者は、自身のビルド制御ファイル
「$(TOP)/app/build.gradle」
に以下の内容を追記すること。

```build.gradle
repositories {
    maven { url "https://niidp.pages.vcp-handson.org/SINETStreamHelper/" }
}

dependencies {
    implementation 'jp.ad.sinet.stream.android.helper:libhelper:1.6.0'
}
```

### ビルド環境設定：マニフェストファイル

本ライブラリを用いるユーザアプリケーションは、マニフェストファイルに
生体情報を取得するセンサー、および位置情報の自動追跡に必要となる権限
を記述する。

```xml
<manifest>
    <!-- 歩数計センサー（Step Detector/Counter）の利用 -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />

    <!-- 位置情報の自動追跡 -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
</manifest>
```

