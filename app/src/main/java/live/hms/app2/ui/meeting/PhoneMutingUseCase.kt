package live.hms.app2.ui.meeting

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import live.hms.app2.helpers.PhoneCallEvents
import live.hms.app2.helpers.getPhoneStateFlow

/**
 * Logic that takes mute events emitted
 *  uses them to actually call the mute methods on the sdk.
 */
class PhoneMutingUseCase {
    /* Stores whether the user had their local audio/video on
     * this should be restored when the call ends.
     */
    private var prevLocalAudioState : Boolean? = null
    private var prevLocalVideoState : Boolean? = null
    private var prevPeerAudioState : Boolean? = null

    fun execute(context: Context,
                localMc: ILocalMediaControl,
                peerMc: IPeerMediaControl): Flow<PhoneCallEvents> {

        return getPhoneStateFlow(context).onEach { phoneInterruptEvents ->
            when (phoneInterruptEvents) {

                PhoneCallEvents.MUTE_ALL -> {
                    prevLocalAudioState = localMc.isLocalAudioEnabled()
                    prevLocalVideoState = localMc.isLocalVideoEnabled()
                    prevPeerAudioState = peerMc.isPeerAudioEnabled()
                    Log.d("PhoneMutingUseCase","Muting: $prevLocalVideoState $prevLocalAudioState $prevPeerAudioState")
                    localMc.setLocalAudioEnabled(false)
                    localMc.setLocalVideoEnabled(false)
                    peerMc.setPeerAudioEnabled(false)
                }

                PhoneCallEvents.UNMUTE_ALL -> {
                    Log.d("PhoneMutingUseCase","Un: $prevLocalVideoState $prevLocalAudioState $prevPeerAudioState")
                    // Restore the previous states
                    prevLocalAudioState?.let {
                        localMc.setLocalAudioEnabled(it)
                    }
                    prevLocalVideoState?.let{
                        localMc.setLocalVideoEnabled(it)
                    }
                    prevPeerAudioState?.let {
                        peerMc.setPeerAudioEnabled(it)
                    }
                }
            }
        }
    }
}