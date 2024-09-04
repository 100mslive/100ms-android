package live.hms.roomkit

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.media.tracks.HMSTrackType
import live.hms.video.plugin.video.HMSVideoPlugin
import live.hms.video.sdk.HMSActionResultListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.HMSUpdateListener
import live.hms.video.sdk.models.HMSConfig
import live.hms.video.sdk.models.HMSMessage
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSRoleChangeRequest
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.video.sdk.models.enums.HMSRoomUpdate
import live.hms.video.sdk.models.enums.HMSTrackUpdate
import live.hms.video.sdk.models.trackchangerequest.HMSChangeTrackStateRequest
import live.hms.video.signal.init.HMSTokenListener
import live.hms.video.signal.init.TokenRequest
import live.hms.video.signal.init.TokenRequestOptions
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resumeWithException

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

@OptIn(InternalCoroutinesApi::class)
suspend fun HMSSDK.tokens(roomcode : String) :String {
    return suspendCancellableCoroutine { continuation ->

        Log.d("LeakTest", "token")
        getAuthTokenByRoomCode(
            TokenRequest(roomcode,  UUID.randomUUID().toString()),
            TokenRequestOptions(null), object: HMSTokenListener {
                override fun onError(error: HMSException) {
                    Log.e("LeakTest", "fetch token un-successful")
                    continuation.resumeWithException(error)
                }

                override fun onTokenSuccess(string: String) {
                    Log.d("LeakTest", "fetch token successful")
                    continuation.resume(string, {})
                }
            })
    }
}


@OptIn(InternalCoroutinesApi::class)
suspend fun HMSSDK.joins(
    config: HMSConfig,
    liveTrack: MutableLiveData<Pair<HMSTrack, HMSTrackUpdate>>
) {
    return suspendCancellableCoroutine { continuation ->
        join(config,object : HMSUpdateListener {
            override fun onChangeTrackStateRequest(details: HMSChangeTrackStateRequest) {
            }

            override fun onError(error: HMSException) {
                Log.d("LeakTest", "Join error $error")
                continuation.tryResumeWithException(error)
            }

            override fun onJoin(room: HMSRoom) {
                continuation.resume(Unit, {})
            }

            override fun onMessageReceived(message: HMSMessage) {
            }

            override fun onPeerUpdate(type: HMSPeerUpdate, peer: HMSPeer) {
            }

            override fun onRoleChangeRequest(request: HMSRoleChangeRequest) {
            }

            override fun onRoomUpdate(type: HMSRoomUpdate, hmsRoom: HMSRoom) {
            }

            override fun onTrackUpdate(type: HMSTrackUpdate, track: HMSTrack, peer: HMSPeer) {
                if (track.type == HMSTrackType.VIDEO)
                liveTrack.postValue(Pair(track, type))
            }

        })
    }
}

@OptIn(InternalCoroutinesApi::class)
suspend fun HMSSDK.leaves() {
    return suspendCancellableCoroutine { continuation ->
        leave(object : HMSActionResultListener {
            override fun onError(error: HMSException) {
                Log.d("LeakTest", "leave error $error")
                continuation.tryResumeWithException(error)
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