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

    fun init(context: Context,
             getLocalAudioEnabled : () -> Boolean?,
             getLocalVideoEnabled : () -> Boolean?,
             getPeerAudioEnabled : () -> Boolean?,
             setLocalAudio : (Boolean) -> Unit,
             setLocalVideo : (Boolean) -> Unit,
             setPeerAudio : (Boolean) -> Unit ): Flow<PhoneCallEvents> {

        return getPhoneStateFlow(context).onEach { phoneInterruptEvents ->
            when (phoneInterruptEvents) {

                PhoneCallEvents.MUTE_ALL -> {
                    prevLocalAudioState = getLocalAudioEnabled()
                    prevLocalVideoState = getLocalVideoEnabled()
                    prevPeerAudioState = getPeerAudioEnabled()
                    Log.d("PhoneMutingUseCase","Muting: $prevLocalVideoState $prevLocalAudioState $prevPeerAudioState")
                    setLocalAudio(false)
                    setLocalVideo(false)
                    setPeerAudio(false)
                }

                PhoneCallEvents.UNMUTE_ALL -> {
                    Log.d("PhoneMutingUseCase","Un: $prevLocalVideoState $prevLocalAudioState $prevPeerAudioState")
                    setLocalAudio(true)
                    setLocalVideo(true)
                    setPeerAudio(true)
                    // TODO we should really restore people's state, not blank turn on.
//                    prevLocalAudioState?.let {
//                        setLocalAudio(it)
//                    }
//                    prevLocalVideoState?.let{
//                        setLocalVideo(it)
//                    }
//                    prevPeerAudioState?.let {
//                        setPeerAudio(it)
//                    }
                }
            }
        }
    }
}