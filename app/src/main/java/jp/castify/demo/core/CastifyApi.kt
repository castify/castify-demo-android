package jp.castify.demo.core

import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.Call
import retrofit2.http.*

interface CastifyApi {

  @GET("broadcasts.less")
  fun listBroadcast(@Query("offset") offset: Long = 0, @Query("limit") limit: Long = 30): Call<Page<Broadcast>>
}

data class Page<E>(
  @JsonProperty("values")
  val items: List<E>,

  @JsonProperty("total")
  val total: Int
)

data class Broadcast(
  @JsonProperty("broadcastId")
  val broadcastId: String,

  @JsonProperty("name")
  val name: String? = null,

  @JsonProperty("link")
  val link: String? = null,

  @JsonProperty("startedAt")
  val startedAt: Number,

  @JsonProperty("stoppedAt")
  val stoppedAt: Number? = null
)
