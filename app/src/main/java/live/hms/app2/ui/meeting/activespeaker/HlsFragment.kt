package live.hms.app2.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import live.hms.app2.R
import live.hms.app2.databinding.HlsFragmentLayoutBinding
import live.hms.app2.ui.meeting.HlsPlayer
import live.hms.app2.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.HlsMetadataHandler
import live.hms.app2.util.viewLifecycle
import live.hms.stats.PlayerEventsCollector
import live.hms.stats.PlayerEventsListener
import live.hms.stats.Utils
import live.hms.stats.model.PlayerStats
import live.hms.video.utils.HMSLogger
import kotlin.math.absoluteValue


class HlsFragment : Fragment() {

    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    val playerUpdatesHandler = Handler()
    var runnable: Runnable? = null
    val TAG = "HlsFragment"
    var isStatsActive: Boolean = false
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val hlsPlayer: HlsPlayer by lazy {
        HlsPlayer()
    }
    var playerEventsManager: PlayerEventsCollector? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HlsFragmentLayoutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSeekLive.setOnClickListener {
            hlsPlayer.getPlayer()?.seekToDefaultPosition()
            hlsPlayer.getPlayer()?.play()
        }

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            hlsPlayer.mute(muted)
        }

        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        binding.chart.data = data
        binding.chart.description.isEnabled = false
        binding.chart.setScaleEnabled(false)
        binding.chart.legend.isEnabled = (false)
        binding.chart.setViewPortOffsets(0f,0f,0f,0f)

        val networkLineData = LineData()
        networkLineData.setValueTextColor(Color.WHITE)
        binding.networkActivityChart.data = networkLineData
        binding.networkActivityChart.description.isEnabled = false
        binding.networkActivityChart.setScaleEnabled(false)
        binding.networkActivityChart.legend.isEnabled = (false)
        binding.networkActivityChart.setViewPortOffsets(0f,0f,0f,0f)


        meetingViewModel.statsToggleData.observe(viewLifecycleOwner) {

            if (it) {
                binding.statsViewParent.visibility = View.VISIBLE
                playerEventsManager?.addListener(object : PlayerEventsListener {
                    @SuppressLint("SetTextI18n")
                    override fun onEventUpdate(playerStats: PlayerStats) {
                        updateStatsView(playerStats)
                    }
                })
                isStatsActive = true
            } else {
                playerEventsManager?.removeListener()
                isStatsActive = false
                binding.statsViewParent.visibility = View.GONE

            }

        }

        binding.btnTrackSelection.setOnClickListener {
            hlsPlayer.getPlayer()?.let {
                val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(it)
                trackSelectionBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    "trackSelectionBottomSheet"
                )
            }
        }

        hlsPlayer.getPlayer()?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                HMSLogger.i(TAG, " ~~ Exoplayer error: $error")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                HMSLogger.i(TAG, "Playback state change to $playbackState")
            }
        })


        runnable = Runnable {
            val distanceFromLive = ((hlsPlayer.getPlayer()?.duration?.minus(
                hlsPlayer.getPlayer()?.currentPosition ?: 0
            ))?.div(1000) ?: 0)

            HMSLogger.i(
                TAG,
                "duration : ${hlsPlayer.getPlayer()?.duration.toString()} current position ${hlsPlayer.getPlayer()?.currentPosition}"
            )
            HMSLogger.i(
                TAG,
                "buffered position : ${hlsPlayer.getPlayer()?.bufferedPosition}  total buffered duration : ${hlsPlayer.getPlayer()?.totalBufferedDuration} "
            )

            if (distanceFromLive >= 10) {
                binding.btnSeekLive.visibility = View.VISIBLE
            } else {
                binding.btnSeekLive.visibility = View.GONE
            }
            playerUpdatesHandler.postDelayed(runnable!!, 2000)
        }
    }

    fun statsToString(playerStats: PlayerStats): String {
        return "bitrate : ${Utils.humanReadableByteCount(playerStats.videoInfo.averageBitrate.toLong(),true,true)}/s \n" +
                "bufferedDuration  : ${playerStats.bufferedDuration.absoluteValue/1000} s \n" +
                "video width : ${playerStats.videoInfo.videoWidth} px \n" +
                "video height : ${playerStats.videoInfo.videoHeight} px \n" +
                "frame rate : ${playerStats.videoInfo.frameRate} fps \n" +
                "dropped frames : ${playerStats.frameInfo.droppedFrameCount} \n" +
                "distance from live edge : ${playerStats.distanceFromLive.div(1000)} s"
    }

    override fun onStart() {
        super.onStart()
        binding.hlsView.player = hlsPlayer.createPlayer(
            requireContext(),
            args.hlsStreamUrl,
            true
        )
        hlsPlayer.getPlayer()?.let {
            playerEventsManager = PlayerEventsCollector(it)
            val hlsMetadataHandler = HlsMetadataHandler(exoPlayer = it, { metaDataModel ->

            }, requireContext())
            hlsMetadataHandler.start()
        }
        runnable?.let {
            playerUpdatesHandler.postDelayed(it, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        playerEventsManager?.removeListener()
    }

    override fun onResume() {
        super.onResume()
        if (isStatsActive) {
            playerEventsManager?.removeListener()
            playerEventsManager?.addListener(object : PlayerEventsListener {
                override fun onEventUpdate(playerStats: PlayerStats) {
                        updateStatsView(playerStats)
                }
            })
        }
    }

    fun updateStatsView(playerStats: PlayerStats){
        addEntry(playerStats.bandwidth.bandWidthEstimate.toFloat(),binding.chart,"Bandwidth")
        binding.bandwidthEstimateTv.text = "${Utils.humanReadableByteCount(playerStats.bandwidth.bandWidthEstimate, si = true, isBits = true)}/s"

        addEntry(playerStats.bandwidth.totalBytesLoaded.toFloat(),binding.networkActivityChart,"Network Activity")
        binding.networkActivityTv.text = "${Utils.humanReadableByteCount(playerStats.bandwidth.totalBytesLoaded, si = true, isBits = true)}"

        binding.statsView.text = statsToString(playerStats)
    }

    override fun onStop() {
        super.onStop()
        hlsPlayer.releasePlayer()
        runnable?.let {
            playerUpdatesHandler.removeCallbacks(it)
        }
    }

    private fun addEntry(value: Float, lineChart: LineChart,label: String) {
        val data: LineData = lineChart.data
        var set = data.getDataSetByIndex(0)
        if (set == null) {
            set = createSet(label)
            data.addDataSet(set)
        }
        data.addEntry(Entry(set.entryCount.toFloat(), value), 0)
        data.notifyDataChanged()

        // let the chart know it's data has changed
        lineChart.notifyDataSetChanged()

        // limit the number of visible entries
        lineChart.setVisibleXRangeMaximum(15f)

        // move to the latest entry
        lineChart.moveViewToX(data.entryCount.toFloat())
    }

    private fun createSet(label : String): LineDataSet {
        val set = LineDataSet(null, label)
        set.axisDependency = AxisDependency.LEFT
        set.color = ContextCompat.getColor(requireContext(), R.color.primary_blue)
        set.setCircleColor(Color.WHITE)
        set.lineWidth = 1f
        set.circleRadius = 1f
        set.fillAlpha = 35
        set.fillColor = ColorTemplate.getHoloBlue()
        set.highLightColor = Color.rgb(244, 117, 117)
        set.valueTextColor = Color.WHITE
        set.valueTextSize = 9f
        set.setDrawValues(false)
        return set
    }
}