package jp.castify.demo

import android.app.Application
import android.content.Context
import jp.castify.api.CastifyApp
import jp.castify.demo.core.CastifyApiFactory

class DemoApp : Application() {

  private val token = ""

  val castifyApp = CastifyApp(token, this)

  val castifyApi = CastifyApiFactory.create(token)
}

val Context.app: DemoApp get() = applicationContext as DemoApp
