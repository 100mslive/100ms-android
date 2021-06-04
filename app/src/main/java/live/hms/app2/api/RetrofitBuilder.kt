package live.hms.app2.api

import live.hms.app2.util.crashlyticsLog
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "RetrofitBuilder"

private val okHttpClient = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor { crashlyticsLog(TAG, it) }.apply {
    level = HttpLoggingInterceptor.Level.BODY
  })
  .callTimeout(10, TimeUnit.SECONDS)
  .readTimeout(5, TimeUnit.SECONDS)
  .writeTimeout(5, TimeUnit.SECONDS)
  .build()

private fun getRetrofit(url: String) = Retrofit.Builder()
  .baseUrl(url)
  .addConverterFactory(GsonConverterFactory.create())
  .client(okHttpClient)
  .build()

fun makeTokenService(endpoint: String): TokenService = getRetrofit(endpoint)
  .create(TokenService::class.java)