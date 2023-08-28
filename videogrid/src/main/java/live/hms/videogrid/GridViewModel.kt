package live.hms.videogrid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import live.hms.video.sdk.HMSAudioListener
import live.hms.video.sdk.HMSSDK
import live.hms.video.sdk.models.HMSSpeaker
import live.hms.video.sdk.reactive.MeetingTrack

class GridViewModel(
    application: Application
) : AndroidViewModel(application) {

    private var hmssdk: HMSSDK? = null
    val speakersLiveData = MutableLiveData<Array<HMSSpeaker>>()
    val updateRowAndColumnSpanForVideoPeerGrid = MutableLiveData<Pair<Int, Int>>()

    private val liveData by lazy { hmssdk!!.getMeetingTrackFlow().asLiveData() }

    companion object {
        const val TAG = "GridViewModel"
    }

    fun initHMSSDK(hmssdk: HMSSDK) {
        this.hmssdk = hmssdk
    }


    fun getTrackLiveData(): LiveData<ArrayList<MeetingTrack>> {
        if (hmssdk == null) throw Exception("HMSSDK not initialized")
        return liveData
    }





    val speakerUpdateLiveData by lazy {
        object : ActiveSpeakerLiveData() {
            private val speakerH = ActiveSpeakerHandler(
                true, 3 * 2
            ) { getTrackLiveData().value?: ArrayList() }


            init {
                initSpeakerCallBack()
                addSpeakerSource()

                // Add all tracks as they come in.
                addSource(getTrackLiveData()) { meetTracks: List<MeetingTrack> ->
                    //if remote peer and local peer is present inset mode
                    val excludeLocalTrackIfRemotePeerIsPreset = if (meetTracks.size > 1) {
                        meetTracks.filter { !it.isLocal }.toList()
                    } else meetTracks

                    val result = speakerH.trackUpdateTrigger(excludeLocalTrackIfRemotePeerIsPreset)
                    setValue(result)
                }

            }

            private fun initSpeakerCallBack() {
                hmssdk!!.addAudioObserver(object : HMSAudioListener {
                    override fun onAudioLevelUpdate(speakers: Array<HMSSpeaker>) {
                        speakersLiveData.postValue(speakers)
                    }
                })

            }


            override fun addSpeakerSource() {
                addSource(speakersLiveData) { speakers: Array<HMSSpeaker> ->

                    val excludeLocalTrackIfRemotePeerIsPreset: Array<HMSSpeaker> =
                        speakers.filter { it.peer?.isLocal == false }.toTypedArray()

                    val result = speakerH.speakerUpdate(excludeLocalTrackIfRemotePeerIsPreset)
                    setValue(result.first)
                }
            }

            override fun removeSpeakerSource() {
                removeSource(speakersLiveData)
            }

            //TODO can't be null
            fun refreshSpeaker() {
                // speakers.postValue(speakers.value)
            }

            override fun updateMaxActiveSpeaker(rowCount: Int, columnCount: Int) {
                speakerH.updateMaxActiveSpeaker(rowCount * columnCount)
                refreshSpeaker()
            }


        }
    }


}