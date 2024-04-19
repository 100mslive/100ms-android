package live.hms.roomkit.ui.meeting

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import live.hms.roomkit.R
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.transcripts.HmsTranscript
import live.hms.video.sdk.transcripts.HmsTranscripts
import java.util.UUID

class TranscriptionUseCase(
    val getNameForPeerId : (String) -> String?,
) {
    private val TAG = "TranscriptionUseCase"
    val captions : MutableLiveData<List<TranscriptViewHolder>> = MutableLiveData(null)
    var receivedOneCaption = false
        private set
    private val CLEAR_AFTER_SILENCE_MILLIS = 5000L
    private val EXTRA_SUBTITLE_DELETION_TIME = 20_000L
    private var singleCancelJob : Job? = null
    private val peerTranscriptList = LinkedHashMap<String, HmsTranscript>()
    private val peerToNameMap = HashMap<String,String>()
    private val cancelJobs = HashMap<String, Job>()

    private val editLock = Mutex()

    fun onPeerNameChanged(peer : HMSPeer) {
        peerToNameMap[peer.peerID] = peer.name
    }

    suspend fun newCaption(transcripts: HmsTranscripts) : Unit = editLock.withLock {
        if(!receivedOneCaption) {
            receivedOneCaption = true
        }
//        Log.d(TAG,"processing started")
        clearAllTranscriptionAfterSilence()
        // update peer names into the map
        updatePeerNamesIntoTheMap(transcripts)

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
        filterCancelJobs(newItemsOriginal)

        // Add the new transcripts to the queue (now this might move the thing)
        peerTranscriptList.putAll(newItemsOriginal)

        updateHolders(peerTranscriptList)
        // Schedule removals
//        if(removeItems)
        scheduleRemovals(newItemsOriginal)

//        Log.d(TAG,"processing complete")
    }

    private fun clearAllTranscriptionAfterSilence() {
        singleCancelJob?.cancel()
        singleCancelJob = CoroutineScope(Dispatchers.Default).launch {
            delay(CLEAR_AFTER_SILENCE_MILLIS)
            editLock.withLock {
                peerTranscriptList.clear()
                updateHolders(peerTranscriptList)
            }
        }
    }

    private fun scheduleRemovals(newItemsOriginal: Map<String, HmsTranscript>) {
        newItemsOriginal.map { (id, transcript) ->
            cancelJobs[id] = CoroutineScope(Dispatchers.Default).launch {
                val delay = (transcript.end - transcript.start).toLong() + EXTRA_SUBTITLE_DELETION_TIME
//                Log.d("CaptionRemoval","$delay")
                delay(delay)
                editLock.withLock {
                    peerTranscriptList.remove(id)
                    updateHolders(peerTranscriptList)
                }
            }
        }
    }

    private fun filterCancelJobs(newItemsOriginal: Map<String, HmsTranscript>) {
        // When an a transcript is extended, such as with a longer sentence, we'll get the same key
        //  again. In this case the duration is also extended. But a cancel job has already been
        //  scheduled for the text. Which would remove the same key.
        // So the older remove job has to be cancelled.
        val commonItems = newItemsOriginal.keys.intersect(peerTranscriptList.keys)
        commonItems.forEach {
            cancelJobs[it]?.cancel()
        }
    }

    private fun updatePeerNamesIntoTheMap(transcripts: HmsTranscripts) {
        val newPeerIds = transcripts.transcripts.map { it.peerId }.toSet() - peerToNameMap.keys
        newPeerIds.forEach {
            // Add missing peer names
            peerToNameMap[it] = getNameForPeerId(it) ?: "Participant"
        }
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
                previousPeerTranscript._text += ' '+hmsTranscript.transcript
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
    fun getSubtitle() : AnnotatedString {
        return buildAnnotatedString {
            withStyle(style = SpanStyle(fontFamily = FontFamily(Font(R.font.inter_bold)))) {
                append(peerName)
                append(": ")
            }
            withStyle(style = SpanStyle(fontFamily = FontFamily(Font(R.font.inter_regular)))) {
                append(text)
            }
        }
    }
}