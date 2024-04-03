package live.hms.roomkit.ui.meeting

import androidx.lifecycle.MutableLiveData
import live.hms.video.sdk.transcripts.HmsTranscripts

class TranscriptionUseCase {
    val captions : MutableLiveData<String?> = MutableLiveData(null)
    // Actually you have to keep a per peer queue of text until the final comes in.
    // Also keep a mapping of peerid to name.

    fun newCaption(transcripts: HmsTranscripts) {
        var isFinal = false
        val text = transcripts.transcripts.fold("") { acc, hmsTranscript ->
            isFinal = isFinal || hmsTranscript.isFinal
            "$acc ${hmsTranscript.transcript}"
        }
        // Actually you have to keep a per peer queue of text until the final comes in.
        if(text.isNotBlank() || isFinal){
            captions.postValue(text)
        }
    }
}