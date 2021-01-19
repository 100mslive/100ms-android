package live.hms.android100ms.api

import live.hms.android100ms.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val okHttpClient = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  })
  .callTimeout(10, TimeUnit.SECONDS)
  .readTimeout(5, TimeUnit.SECONDS)
  .writeTimeout(5, TimeUnit.SECONDS)
  .build()


private fun getRetrofit() = Retrofit.Builder()
  .baseUrl(BuildConfig.TOKEN_ENDPOINT)
  .addConverterFactory(GsonConverterFactory.create())
  .client(okHttpClient)
  .build()

val TokenApiService: TokenService by lazy { getRetrofit().create(TokenService::class.java) }