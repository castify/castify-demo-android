# castify-demo-android

[Castify SDK](https://doc.castify.jp/quickstart/install/Android.html) を利用してアプリかな以下の機能を利用するサンプルです。

 - カメラ／マイクから入力した映像を Castify プラットフォーム上に配信
 - Castify 上のライブ／アーカイブの再生

準備
----

Castify から取得した API トークンを [DemoApp.kt](./app/src/main/java/jp/castify/demo/DemoApp.kt) 下記の箇所に設定します。

```kotlin
private val token = ""
```
