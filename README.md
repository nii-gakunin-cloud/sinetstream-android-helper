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
--->

# SINETStreamHelper

## 概要

IoTアプリケーションの一つとして、Android端末が備える様々なセンサー情報を  
収集して[SINETStream](https://nii-gakunin-cloud.github.io/sinetstream)に
送出する「パプリッシャ」機能が考えられる。  
ユーザアプリケーションが既存の「SINETStream Android」プラグインを直接  
利用してメッセージを送出しても何ら問題ない。  
しかしながら、センサー装置群の細かな動作制御やネットワークに負荷をかけない  
（= 電力を浪費しない）送出制御など、現実的には実装上の難易度が高いものと  
なってしまう。

そこで、ユーザアプリケーションと「SINETStream Android」の中間階層として、  
センサー制御およびMQTTプロトコルメッセージ送出を制御する補助ライブラリを  
「SINETStreamHelper」として用意する。

```
      #----------------------------------+
      | Sensor Publisher Application     |---[ config file ]
      +----------------------------------+
    ======================================== API functions
      +----------------------------------+
      | SINETStreamHelper                |
      +----------------------------------+
        +------------+  +-------------+
        | Sensor     |  | SINETStream |
        | Manager    |  | Android     |
        +------------+  +-------------+
             A                  |
             |                  V
       [ Sensor data ]  [ MQTT Message ]
```

Android端末のセンサー値をSINETStreamに流したいユーザアプリケーション  
開発者は、本ライブラリが提供するAPI関数や非同期通知を「妥当な順序」で  
用いることで、センサー情報のIoTパブリッシャ機能を簡易に実装できる。


## 導入方法

### ライブラリの追加

アプリケーションレベルの `build.gradle` を編集して SINETStreamHelper  
を依存ライブラリに追加する。追加する内容を以下に示す。

```
repositories {
    maven { url "https://niidp.pages.vcp-handson.org/SINETStreamHelper/" }
}

dependencies {
    implementation 'jp.ad.sinet.stream.android.helper:libhelper:1.5.0'
}
```


## 処理の流れ

SINETStreamHelperを使うアプリケーションの主処理の流れは以下の通りである。  
いずれも非同期操作となるため、操作要求に対する応答、あるいは非同期通知が  
あることを前提にユーザアプリケーションを実装する必要がある。

```
1) サービスに結合
2) センサー種別一覧取得
3) センサー種別群の登録
4) センサー種別群の削除
5) サービスと切断
```

上記3)により、指定されたセンサー群が有効となる。  
システムから随時通知されるセンサー値はいったん「SINETStreamHelper」内部で  
蓄積され、送信契機が一定の頻度を越えないよう制御しながらネットワークに送出  
される。この動作は、上記4)により当該センサー群が登録削除されるまで続く。  
もちろんアプリケーションの生存期間中に3)と4)を繰り替えして構わない。


## JSONデータ形式

Android端末上で収集したセンサー情報は、下記に示すJSON形式に変換され、  
MQTTメッセージのペイロード部に渡される。  
ここでは人間が見やすいよう整形してコメントも追記しているが、実際は改行無しで  
詰めた形式で出力される。
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
        # 位置情報（利用者が設定）
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

