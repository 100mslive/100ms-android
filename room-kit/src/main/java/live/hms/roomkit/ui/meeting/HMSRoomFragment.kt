package live.hms.roomkit.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPrebuiltBinding
import live.hms.roomkit.model.RoomDetails
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.util.ROOM_DETAILS
import live.hms.roomkit.util.viewLifecycle

class HMSRoomFragment : Fragment() {

    companion object {
        private const val TAG = "PrebuiltFragment"
        private const val HMS_ROOM_CODE = "HMS_ROOM_CODE"
        private const val HMS_ROOM_OPTIONS = "HMS_ROOM_OPTIONS"

        fun newInstance(roomCode: String, options: HMSPrebuiltOptions? = null): HMSRoomFragment {
            return HMSRoomFragment().apply {
                arguments = Bundle().apply {
                    putString(HMS_ROOM_CODE, roomCode)
                    putParcelable(HMS_ROOM_OPTIONS, options)
                }
            }
        }
    }

    private var binding by viewLifecycle<FragmentPrebuiltBinding>()

    private lateinit var roomDetails: RoomDetails
    private lateinit var settings: SettingsStore


    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application,
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().invalidateOptionsMenu()
        setHasOptionsMenu(true)
        settings = SettingsStore(requireContext())


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