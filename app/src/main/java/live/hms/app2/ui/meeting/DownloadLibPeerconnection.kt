package live.hms.app2.ui.meeting

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DownloadLibPeerconnection {
    private val x64 = "https://github.com/AniketSK/webrtc/raw/main/arm64-v8a/libjingle_peerconnection_so.so"

    suspend fun getLib(context : Context) = withContext(Dispatchers.IO){
        val f : Call<ResponseBody> = Retrofit.Builder().baseUrl("https://www.example.com")
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(SoDownload::class.java).getFile()
        saveFile(f.execute().body(), File("${context.filesDir.absolutePath}/libjingle_peerconnection_so.so").absolutePath)

    }

    private fun saveFile(body: ResponseBody?, pathWhereYouWantToSaveFile: String):String{
        if (body==null)
            return ""
        var input: InputStream? = null
        try {
            input = body.byteStream()
            //val file = File(getCacheDir(), "cacheFileAppeal.srl")
            val fos = FileOutputStream(pathWhereYouWantToSaveFile)
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            return pathWhereYouWantToSaveFile
        }catch (e:Exception){
            Log.e("saveFile",e.toString())
        }
        finally {
            input?.close()
        }
        return ""
    }
}

interface SoDownload {
    @Streaming
    @GET("https://github.com/AniketSK/webrtc/raw/main/arm64-v8a/libjingle_peerconnection_so.so")
    fun getFile() : Call<ResponseBody>
}
