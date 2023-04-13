package live.hms.app2.ui.meeting.activespeaker

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.snackbar.Snackbar
import live.hms.app2.R
import live.hms.app2.databinding.HlsFragmentLayoutBinding
import live.hms.app2.ui.meeting.HlsVideoQualitySelectorBottomSheet
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import live.hms.hls_player.*
import live.hms.stats.PlayerStatsListener
import live.hms.stats.Utils
import live.hms.stats.model.PlayerStatsModel
import kotlin.math.absoluteValue


class HlsFragment : Fragment() {

    private val args: HlsFragmentArgs by navArgs()
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    val TAG = "HlsFragment"
    var isStatsActive: Boolean = false
    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    val player by lazy{ HlsPlayer(requireContext()) }
    var distanceFromLive : Long = 0

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
            player.seekToLivePosition()
        }

        meetingViewModel.showAudioMuted.observe(viewLifecycleOwner) { muted ->
            player.mute(muted)
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
            isStatsActive = it
            setPlayerStats(it)
        }

        binding.btnTrackSelection.setOnClickListener {
            binding.hlsView.let {
                val trackSelectionBottomSheet = HlsVideoQualitySelectorBottomSheet(player)
                trackSelectionBottomSheet.show(
                    requireActivity().supportFragmentManager,
                    "trackSelectionBottomSheet"
                )
            }
        }

        // TODO enable
//        runnable = Runnable {
//            val distanceFromLive = ((hlsPlayer?.getPlayer()?.duration?.minus(
//                hlsPlayer?.getPlayer()?.currentPosition ?: 0
//            ))?.div(1000) ?: 0)
//
//            HMSLogger.i(
//                TAG,
//                "duration : ${hlsPlayer?.getPlayer()?.duration.toString()} current position ${hlsPlayer?.getPlayer()?.currentPosition}"
//            )
//            HMSLogger.i(
//                TAG,
//                "buffered position : ${hlsPlayer?.getPlayer()?.bufferedPosition}  total buffered duration : ${hlsPlayer?.getPlayer()?.totalBufferedDuration} "
//            )
//
//            if (distanceFromLive >= 10) {
//                binding.btnSeekLive.visibility = View.VISIBLE
//            } else {
//                binding.btnSeekLive.visibility = View.GONE
//            }
//            playerUpdatesHandler.postDelayed(runnable!!, 2000)
//        }
    }

    private fun statsToString(playerStats: PlayerStatsModel): String {
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
        resumePlay()
        player.play(args.hlsStreamUrl)
    }

    fun resumePlay() {

        binding.hlsView.player = player.getNativePlayer()

        player.addPlayerEventListener(object : HmsHlsPlaybackEvents {

            override fun isLive(live : Boolean) {
                binding.btnSeekLive.visibility = if(!live && distanceFromLive > 10000) View.VISIBLE else View.GONE
            }

            override fun onPlaybackFailure(error : HmsHlsException) {
                Log.d("HMSHLSPLAYER","From App, error: $error")
            }

            override fun onPlaybackStateChanged(p1 : HmsHlsPlaybackState){
                Log.d("HMSHLSPLAYER","From App, playback state: $p1")
            }

            override fun onCue(hlsCue : HmsHlsCue) {
                val duration = if(hlsCue.endDate?.time == null){
                    Snackbar.LENGTH_INDEFINITE
                }
                else {
                    ((hlsCue.endDate?.time ?: 0) - System.currentTimeMillis()).toInt()
                }
                if (duration > 0 || duration == Snackbar.LENGTH_INDEFINITE) {
                    Log.d("HMSHLSPLAYER","From App, metadata: $duration s/ $hlsCue")
                    Snackbar.make(
                        this@HlsFragment.requireContext(),
                        binding.networkActivityTv,
                        hlsCue.payloadval ?: "empty",
                        duration
                    ).show()
                }

            }
        })

    }

    override fun onPause() {
        super.onPause()
        setPlayerStats(false)
    }

    override fun onResume() {
        super.onResume()
        if (isStatsActive) {
            setPlayerStats(true)
        }
    }

    private fun setPlayerStats(enable : Boolean) {
        if(enable) {
            binding.statsViewParent.visibility = View.VISIBLE
            player.setStatsMonitor(object : PlayerStatsListener {
                @SuppressLint("SetTextI18n")
                override fun onEventUpdate(playerStats: PlayerStatsModel) {
                    updateStatsView(playerStats)
                }
            })
        } else {
            player.setStatsMonitor(null)
            binding.statsViewParent.visibility = View.GONE
        }
    }

    fun updateStatsView(playerStats: PlayerStatsModel){
        addEntry(playerStats.bandwidth.bandWidthEstimate.toFloat(),binding.chart,"Bandwidth")
        binding.bandwidthEstimateTv.text = "${Utils.humanReadableByteCount(playerStats.bandwidth.bandWidthEstimate, si = true, isBits = true)}/s"

        addEntry(playerStats.bandwidth.totalBytesLoaded.toFloat(),binding.networkActivityChart,"Network Activity")
        binding.networkActivityTv.text = "${Utils.humanReadableByteCount(playerStats.bandwidth.totalBytesLoaded, si = true, isBits = true)}"
        distanceFromLive = playerStats.distanceFromLive
        binding.statsView.text = statsToString(playerStats)
    }

    override fun onStop() {
        super.onStop()
        player.stop()
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