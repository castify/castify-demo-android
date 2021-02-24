package jp.castify.demo.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import jp.castify.api.CastifyAppConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory


object CastifyApiFactory {

  fun create(token: String, baseUrl: String = CastifyAppConfig.production.restAPI): CastifyApi =
    Retrofit.Builder()
      .apply {
        baseUrl(baseUrl.replace(Regex("[/\\\\]*$"), "/"))
        addConverterFactory(
          JacksonConverterFactory
            .create(ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
        )
        client(
          OkHttpClient.Builder()
            .addInterceptor {
              it.proceed(it.request().newBuilder().addHeader("X-Castify-API-Token", token).build())
            }
            .build()
        )
      }
      .build()
      .create(CastifyApi::class.java)
}
