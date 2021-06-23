package live.hms.app2.ui.meeting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import live.hms.app2.databinding.FragmentPreviewBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.ROOM_DETAILS
import live.hms.app2.util.setOnSingleClickListener
import live.hms.app2.util.viewLifecycle

class PreviewFragment : Fragment() {

    companion object {
        private const val TAG = "PreviewFragment"
    }

    private var binding by viewLifecycle<FragmentPreviewBinding>()

    private lateinit var roomDetails: RoomDetails

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application,
            requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
        )
    }

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreviewBinding.inflate(inflater, container, false)

        initButtons()
        return binding.root
    }

    private fun initButtons() {
        binding.buttonToggleVideo.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonToggleVideo.onClick()")
                // Toggle HMStrackSettings initial for video state
            }
        }

        binding.buttonToggleAudio.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonToggleAudio.onClick()")
                // Toggle HMStrackSettings initial for audio state
            }
        }

        binding.buttonJoinMeeting.apply {
            setOnSingleClickListener(200L) {
                Log.v(TAG, "buttonJoinMeeting.onClick()")
                // Start meeting
            }
        }
    }


}