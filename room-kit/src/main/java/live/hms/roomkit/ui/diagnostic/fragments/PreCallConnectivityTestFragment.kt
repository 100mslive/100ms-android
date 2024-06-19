package live.hms.roomkit.ui.diagnostic.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.GroupieAdapter
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPreCallConnectivityTestBinding
import live.hms.roomkit.gone
import live.hms.roomkit.orZeroIfNullOrNaN
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.show
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModel
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModelFactory
import live.hms.roomkit.ui.diagnostic.item.DiagnosticDetail
import live.hms.roomkit.ui.diagnostic.item.ExpandableHeader
import live.hms.roomkit.ui.diagnostic.item.Padding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.buttonEnabled
import live.hms.roomkit.util.viewLifecycle
import live.hms.stats.Utils
import live.hms.video.diagnostics.models.ConnectivityCheckResult
import live.hms.video.diagnostics.models.ConnectivityState
import kotlin.math.round


/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class PreCallConnectivityTestFragment : Fragment() {

    private var binding by viewLifecycle<FragmentPreCallConnectivityTestBinding>()

    private val connectivityListAdapter by lazy { GroupieAdapter() }
    private val vm: DiagnosticViewModel by activityViewModels {
        DiagnosticViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreCallConnectivityTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        binding.yesButton.buttonEnabled()
        vm.startConnectivityTest()

        binding.connectivtyList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = connectivityListAdapter
            setHasFixedSize(false)
        }

        vm.connectivityStateLiveData.observe(viewLifecycleOwner, Observer {
            binding.subHeaderConnection.text = it?.name.orEmpty()
        })

        vm.connectivityLiveData.observe(viewLifecycleOwner, Observer {
            //in progress

            if (it == null || it.connectivityState == ConnectivityState.STARTING) {
                binding.uiFailedGroup.gone()
                binding.uiLoadingGroup.show()
                binding.uiSuccessGroup.gone()
                return@Observer
            }

            //success
            if (it.connectivityState == ConnectivityState.COMPLETED) {
                mapToUi(it)
                binding.uiFailedGroup.gone()
                binding.uiLoadingGroup.gone()
                binding.uiSuccessGroup.show()
            }

        })

        binding.yesButton.setOnSingleClickListener {
            vm.startConnectivityTest()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.stopConnectivityTest()
                    findNavController().popBackStack()
                }
            })

    }

    private fun mapToUi(model: ConnectivityCheckResult) {

        connectivityListAdapter.clear()
        val signalingReport = ExpandableGroup(
            ExpandableHeader(
                "Signalling server connection test",
                if (model.signallingReport.isConnected) "Connected" else "Not Connected",
                if (model.signallingReport.isConnected) R.drawable.ic_correct_tick_big else R.drawable.ic_cross_big,
                onExpand = ::onExpand
            )
        ).apply {
            add(
                DiagnosticDetail(
                    "Signalling Gateway",
                    if (model.signallingReport.isInitConnected) "Reachable" else "Not Reachable",
                    if (model.signallingReport.isInitConnected) R.drawable.ic_correct_tick_small else R.drawable.ic_cross_small
                )
            )
            if (model.signallingReport.websocketUrl.isNullOrEmpty().not()) add(
                DiagnosticDetail(
                    "Websocket URL",
                    model.signallingReport.websocketUrl.toString(),
                    R.drawable.link_2
                )
            )
        }
        val isVideoAudioPublished =
            model.mediaServerReport.isSubcribeICEConnected && model.mediaServerReport.isPublishICEConnected && model.mediaServerReport.stats != null && model.mediaServerReport.stats?.video != null && model.mediaServerReport.stats?.audio != null


        val mediaReport = ExpandableGroup(
            ExpandableHeader(
                "Media server connection test",
                if (isVideoAudioPublished) "Connected" else "Not Connected",
                if (isVideoAudioPublished) R.drawable.ic_correct_tick_big else R.drawable.ic_cross_big,
                onExpand = ::onExpand
            )

        ).apply {
            add(
                DiagnosticDetail(
                    "Media Captured",
                    if (vm.isMediaCaptured) "Yes" else "No",
                    if (vm.isMediaCaptured) R.drawable.ic_correct_tick_small else R.drawable.ic_cross_small
                )
            )
            add(
                DiagnosticDetail(
                    "Media Published",
                    if (vm.isMediaPublished) "Yes" else "No",
                    if (vm.isMediaPublished) R.drawable.ic_correct_tick_small else R.drawable.ic_cross_small
                )
            )
            add(
                DiagnosticDetail(
                    "CQS",
                    "${model.mediaServerReport.connectionQualityScore ?: 0} / 5",
                    if((model.mediaServerReport.connectionQualityScore ?: 0) == 0) R.drawable.ic_cross_small else R.drawable.ic_correct_tick_small
                )
            )
        }



        val videoStats = model.mediaServerReport.stats?.video
        val audioStats = model.mediaServerReport.stats?.audio
        val videoReport = ExpandableGroup(
            ExpandableHeader(
                "Video",
                if (isVideoAudioPublished) "Received/Sent" else "Not Received/Sent",
                if (isVideoAudioPublished) R.drawable.ic_correct_tick_big else R.drawable.ic_cross_big,
                hideViewMoreUI = isVideoAudioPublished.not(),
                onExpand = ::onExpand
            )
        ).apply {
            if (isVideoAudioPublished) {
                add(
                    DiagnosticDetail(
                        "Bytes Received",
                        Utils.humanReadableByteCount(videoStats?.bytesReceived ?: 0, true, false),
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Packets Lost",
                        "${videoStats?.packetsLost.orZeroIfNullOrNaN()}",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Packets Received",
                        "${videoStats?.packetsReceived.orZeroIfNullOrNaN()}",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Bitrate Sent",
                        "${round(videoStats?.bitrateSent.orZeroIfNullOrNaN().toDouble())} kbps",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Bitrate Received",
                        "${round(videoStats?.bitrateReceived.orZeroIfNullOrNaN().toDouble())} kbps",
                        R.drawable.ic_correct_tick_small
                    )
                )

                add(
                    DiagnosticDetail(
                        "Round-Trip Time (RTT)",
                        "${(videoStats?.roundTripTime.orZeroIfNullOrNaN().toDouble() * 1000).toInt()} ms",
                        R.drawable.ic_correct_tick_small
                    )
                )
            }
        }


        val audioReport = ExpandableGroup(
            ExpandableHeader(
                "Audio",
                if (isVideoAudioPublished) "Received/Sent" else "Not Received/Sent",
                if (isVideoAudioPublished) R.drawable.ic_correct_tick_big else R.drawable.ic_cross_big,
                hideViewMoreUI = isVideoAudioPublished.not(),
                onExpand = ::onExpand
            )
        ).apply {
            if (isVideoAudioPublished) {
                add(
                    DiagnosticDetail(
                        "Bytes Received",
                        Utils.humanReadableByteCount(audioStats?.bytesReceived.orZeroIfNullOrNaN().toLong(), true, false),
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Packets Lost",
                        "${audioStats?.packetsLost.orZeroIfNullOrNaN()}",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Packets Received",
                        "${audioStats?.packetsReceived.orZeroIfNullOrNaN()}",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Bitrate Sent",
                        "${round(audioStats?.bitrateSent.orZeroIfNullOrNaN().toDouble())} kbps",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Bitrate Received",
                        "${round(audioStats?.bitrateReceived.orZeroIfNullOrNaN().toDouble())} kbps",
                        R.drawable.ic_correct_tick_small
                    )
                )
                add(
                    DiagnosticDetail(
                        "Round-Trip Time (RTT)",
                        "${(audioStats?.roundTripTime.orZeroIfNullOrNaN().toDouble() * 1000).toInt()} ms",
                        R.drawable.ic_correct_tick_small
                    )
                )

            }
        }







        connectivityListAdapter.apply {
            add(signalingReport)
            add(Padding())
            add(mediaReport)
            add(Padding())
            add(videoReport)
            add(Padding())
            add(audioReport)
            add(Padding())
        }
    }

    val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private fun onExpand() {
        mainHandler.postDelayed({
            binding?.root?.invalidate()
            binding?.connectivtyList?.invalidate()
        }, 500)
    }

}