package live.hms.app2.ui.meeting

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

        return getPhoneStateFlow(context)
            .distinctUntilChanged() // Will go from ringing to offhook so mute would be called twice, ignore second mute event.
            .onEach { phoneInterruptEvents ->
            when (phoneInterruptEvents) {

                PhoneCallEvents.MUTE_ALL -> {
                    // Store all the existing states of peer volume, local audio and video
                    //  when we restore it on unmuting it should get the original values back.
                    //  note we aren't taking into account any values that were modified in the interim.
                    prevLocalAudioState = localMc.isLocalAudioEnabled()
                    prevLocalVideoState = localMc.isLocalVideoEnabled()
                    prevPeerAudioState = peerMc.isPeerAudioEnabled()
                    localMc.setLocalAudioEnabled(false)
                    localMc.setLocalVideoEnabled(false)
                    peerMc.setPeerAudioEnabled(false)
                }

                PhoneCallEvents.UNMUTE_ALL -> {
                    // Restore the previous states of audio, video and peer volume
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