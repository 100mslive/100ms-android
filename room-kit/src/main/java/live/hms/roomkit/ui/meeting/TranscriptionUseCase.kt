package live.hms.roomkit.ui.meeting

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import live.hms.video.sdk.transcripts.HmsTranscript
import live.hms.video.sdk.transcripts.HmsTranscripts
import java.util.UUID

class TranscriptionUseCase(
    val getNameForPeerId : (String) -> String?
) {
    private val TAG = "TranscriptionUseCase"
    val captions : MutableLiveData<List<TranscriptViewHolder>> = MutableLiveData(null)
    // Actually you have to keep a per peer queue of text until the final comes in.
    // Also keep a mapping of peerid to name.

    private val peerTranscriptList = LinkedHashMap<String, HmsTranscript>()
    private val peerToNameMap = HashMap<String,String>()
    private val cancelJobs = HashMap<String, Job>()

    private val editLock = Mutex()


    suspend fun newCaption(transcripts: HmsTranscripts) : Unit = editLock.withLock {
//        Log.d(TAG,"processing started")

        // update peer names into the map
        val newPeerIds = transcripts.transcripts.map { it.peerId }.toSet() - peerToNameMap.keys
        newPeerIds.forEach {
            // Add missing peer names
            peerToNameMap[it] = getNameForPeerId(it) ?: "Participant"
        }

        // Whenever a new list comes in, send it to be appended to the queue.
        // The end time for older events in the queue must always be before the end time for
        //  events later in the queue.
        // When we add something to the queue, we also schedule its removal.
        val newItemsOriginal : Map<String, HmsTranscript> = transcripts.transcripts
            // We skip adding blanks to the queue.
            .filter { it.transcript.isNotBlank() }
            // The text and the end time for a transcript can be updated if you speak a longer sentence.
            // However the newer transcript will contain the older text.
            // So the entire transcript object will need to be replaced.
            // This will happen automatically since we're using a map.
            .associateBy { it.peerId + it.start }

        // filter out the cancel jobs.
        // When an a transcript is extended, such as with a longer sentence, we'll get the same key
        //  again. In this case the duration is also extended. But a cancel job has already been
        //  scheduled for the text. Which would remove the same key.
        // So the older remove job has to be cancelled.
        val commonItems = newItemsOriginal.keys.intersect(peerTranscriptList.keys)
        commonItems.forEach {
            cancelJobs[it]?.cancel()
        }

        // Add the new transcripts to the queue (now this might move the thing)
        peerTranscriptList.putAll(newItemsOriginal)

        updateHolders(peerTranscriptList)
        // Schedule removals
        newItemsOriginal.map { (id, transcript) ->
            cancelJobs[id] = CoroutineScope(Dispatchers.Default).launch {
                val delay = (transcript.end - transcript.start).toLong()
//                Log.d("CaptionRemoval","$delay")
                delay(delay)
                editLock.withLock {
                    peerTranscriptList.remove(id)
                    updateHolders(peerTranscriptList)
                }
            }
        }
//        Log.d(TAG,"processing complete")
    }

    private fun updateHolders(peerTranscriptList: LinkedHashMap<String, HmsTranscript>) {
        // convert transcript map to transcript view holder
        // For each item in the list, group gather everything into a group
        //  as long a peers info is consecutive and the break into new strings when it changes.
        val captions = mutableListOf<TranscriptViewHolder>()

        peerTranscriptList.forEach {  (_, hmsTranscript: HmsTranscript) ->
            // Pull the last one or create a new one
            val previousPeerTranscript = captions.lastOrNull()
            if(previousPeerTranscript == null || previousPeerTranscript.peerId != hmsTranscript.peerId) {
                captions.add(TranscriptViewHolder(
                    peerName = peerToNameMap[hmsTranscript.peerId]!!,
                    _text = hmsTranscript.transcript,
                    peerId = hmsTranscript.peerId
                ))
            } else {
                previousPeerTranscript._text += '\n'+hmsTranscript.transcript
            }
        }
        this.captions.postValue(captions)
    }
}

data class TranscriptViewHolder(
    val peerName: String,
    internal var _text: String,
    internal var peerId : String,
    val id: String = UUID.randomUUID().toString()
) {
    val text: String
        get() {
            return _text
        }
}