package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPrebuiltBinding
import live.hms.roomkit.model.RoomDetails
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.*
import live.hms.video.error.HMSException
import live.hms.video.sdk.HMSActionResultListener

class HMSRoomFragment : Fragment() {

    companion object {
        private const val TAG = "PrebuiltFragment"

        fun newInstance(roomCode: String, options: HMSPrebuiltOptions? = null): HMSRoomFragment {
            return HMSRoomFragment().apply {
                arguments = Bundle().apply {
                    putString(ROOM_CODE, roomCode)
                    putParcelable(ROOM_PREBUILT, options)
                }
            }
        }
    }

    private var binding by viewLifecycle<FragmentPrebuiltBinding>()

    private var hmsPrebuiltOptions: HMSPrebuiltOptions? = null
    private var roomCode: String? = null
    private lateinit var settings: SettingsStore


    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application,
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomCode = arguments?.getString(ROOM_CODE)
        hmsPrebuiltOptions = arguments?.getParcelable(ROOM_PREBUILT)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().invalidateOptionsMenu()
        setHasOptionsMenu(true)
        settings = SettingsStore(requireContext())

        meetingViewModel.initSdk(
            roomCode.orEmpty(),
            hmsPrebuiltOptions, object : HMSActionResultListener {
                override fun onError(error: HMSException) {
                    contextSafe { context, activity ->
                        activity.runOnUiThread {
                            Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onSuccess() {
                    contextSafe { context, activity ->
                        activity.runOnUiThread {
                            val navHostFragment =
                                parentFragmentManager.findFragmentById(R.id.nav_host_fragment_prebuilt) as NavHostFragment
                            val navController = navHostFragment.navController
                            val topFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
                            if (settings.showPreviewBeforeJoin == true && (topFragment is MeetingFragment).not()) {
                                navController?.setGraph(R.navigation.preview_nav_graph)
                            } else {
                                navController?.setGraph(R.navigation.meeting_nav_graph)
                            }
                        }
                    }
                }
            })



    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPrebuiltBinding.inflate(inflater, container, false)
        return binding.root
    }


}