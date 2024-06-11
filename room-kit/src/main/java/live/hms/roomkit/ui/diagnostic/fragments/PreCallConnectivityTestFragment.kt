package live.hms.roomkit.ui.diagnostic.fragments

import android.os.Bundle
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
import live.hms.roomkit.show
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModel
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModelFactory
import live.hms.roomkit.ui.diagnostic.item.DiagnosticDetail
import live.hms.roomkit.ui.diagnostic.item.ExpandableHeader
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.buttonEnabled
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.diagnostics.models.ConnectivityCheckResult
import live.hms.video.diagnostics.models.ConnectivityState


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
            } else if (it.errors.isEmpty().not()) {
                //failed
                binding.uiFailedGroup.show()
                binding.uiLoadingGroup.gone()
                binding.uiSuccessGroup.gone()
            }
        })

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

        val signalingHeader = ExpandableGroup(
            ExpandableHeader(
                "Signalling server connection test",
                if (model.signallingReport.isConnected) "Connected" else "Not Connected",
                if (model.signallingReport.isConnected) R.drawable.ic_correct_tick_big else R.drawable.ic_cross_big
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


        val mediaReportHeader = ExpandableGroup(
            ExpandableHeader(
                "Media server connection test", "Connected", R.drawable.ic_correct_tick_big
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
                    model.mediaServerReport.connectionQualityScore.toString(),
                    R.drawable.ic_correct_tick_small
                )
            )
        }






        connectivityListAdapter.apply {
            add(signalingHeader)
            add(mediaReportHeader)
        }
    }

}