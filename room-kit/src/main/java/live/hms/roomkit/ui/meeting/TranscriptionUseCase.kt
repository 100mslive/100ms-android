package live.hms.roomkit.ui.meeting

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import live.hms.video.sdk.transcripts.HmsTranscript
import live.hms.video.sdk.transcripts.HmsTranscripts
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

class TranscriptionUseCase(
    val getNameForPeerId : (String) -> String?
) {
    val captions : MutableLiveData<List<TranscriptViewHolder>> = MutableLiveData(null)
    // Actually you have to keep a per peer queue of text until the final comes in.
    // Also keep a mapping of peerid to name.

    // So we could just keep a list of peerId -> transcriptions.
//    val concurrentLinkedDeque = ConcurrentLinkedDeque<HmsTranscript>()
    // What we really have is a map of peerid to concurrent lists.
    private val peerTranscriptMap = ConcurrentHashMap<String, ConcurrentLinkedDeque<HmsTranscript>>()
    private val peerToNameMap = HashMap<String,String>()

    private val editLock = Mutex()

    suspend fun newCaption(transcripts: HmsTranscripts) : Unit = editLock.withLock {
        Log.d("Caption","processing started")

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
        val newItemsOriginal = transcripts.transcripts
            // We skip adding blanks to the queue.
            .filter { it.transcript.isNotBlank() }

        val newItems : Map<String, List<HmsTranscript>> = newItemsOriginal.groupBy { it.peerId }

        // Add the new transcripts to the queue
        newItems.forEach{ (key, value) ->
            if(peerTranscriptMap.contains(key)) {
                // TODO remove mutation, might cause errors with compose
                peerTranscriptMap[key]!!.addAll(value)
            } else {
                peerTranscriptMap[key] = ConcurrentLinkedDeque(value)
            }
        }


        updateHolders(peerTranscriptMap)
        // Schedule removals
        newItemsOriginal.filter { it.isFinal }.map { transcript ->
            CoroutineScope(Dispatchers.Default).launch {
                val delay = (transcript.end - transcript.start).toLong()
                Log.d("CaptionRemoval","$delay")
                delay(delay)
                editLock.withLock {
                    peerTranscriptMap[transcript.peerId]?.remove(transcript)
                    updateHolders(peerTranscriptMap)
                }
            }
        }
        Log.d("Caption","processing complete")
    }

    private fun updateHolders(peerTranscriptMap: ConcurrentHashMap<String, ConcurrentLinkedDeque<HmsTranscript>>) {
        // convert transcript map to transcript view holder
        val holders = peerTranscriptMap.mapNotNull { (peerId, transcript) ->
            if(transcript.size == 0) {
                null
            } else {
                val text = transcript.fold("") { acc, hmsTranscript ->
                    "$acc\n${hmsTranscript.transcript}"
                }
                TranscriptViewHolder(peerToNameMap[peerId]!!, text)
            }
        }
        captions.postValue(holders)
    }
}

data class TranscriptViewHolder(
    val peerName : String,
    val text : String,
    val id : String = UUID.randomUUID().toString()
)