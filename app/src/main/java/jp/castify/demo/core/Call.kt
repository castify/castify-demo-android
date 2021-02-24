package jp.castify.demo.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call

suspend fun <T> Call<T>.load(): T {
  return withContext(Dispatchers.IO) { execute().body()!! }
}
