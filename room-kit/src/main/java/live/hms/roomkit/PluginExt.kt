package live.hms.roomkit

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import live.hms.video.error.HMSException
import live.hms.video.plugin.video.HMSVideoPlugin
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.HMSSDK
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

object HMSPluginScope : CoroutineScope {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()
    override val coroutineContext: CoroutineContext
        get() = dispatcher
}
internal suspend fun HMSSDK.addPlugin(plugin : HMSVideoPlugin): Unit {
    return suspendCancellableCoroutine { continuation ->
        if (getPlugins().orEmpty().isEmpty().not()){
            continuation.resume(Unit, {})
            return@suspendCancellableCoroutine
        }
        addPlugin(plugin, object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                Log.d("HmsPluginError","$error")
                continuation.resume(Unit, {})
            }
            override fun onSuccess() {
                continuation.resume(Unit, {})
            }
        })
    }
}

internal suspend fun HMSSDK.removePlugin(): Unit {
    return suspendCancellableCoroutine { continuation ->
        for (plugin in this.getPlugins().orEmpty()) {
            removePlugin(plugin, object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    continuation.resume(Unit, {})
                }
                override fun onSuccess() {
                    continuation.resume(Unit, {})
                }
            })
        }
    }
}