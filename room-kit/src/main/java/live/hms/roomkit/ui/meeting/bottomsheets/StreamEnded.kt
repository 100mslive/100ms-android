package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import live.hms.roomkit.R
import live.hms.roomkit.databinding.StreamEndedBinding
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.logS
import live.hms.roomkit.util.viewLifecycle

enum class StreamState {
    ENDED,
    STARTED
}
class StreamEnded: Fragment()  {
    val meetingViewModel : MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }
    companion object {
        const val TAG = "StreamEndedFragment"
        fun launch(fm: FragmentManager) {
            fm
                .beginTransaction()
                .add(R.id.fragment_container, StreamEnded())
                .commit()
        }
    }

    private var binding by viewLifecycle<StreamEndedBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = StreamEndedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        initOnBackPress()
        // now we watch something to dismiss the fragment if it needs to be.
        lifecycleScope.launch {
            meetingViewModel.hlsStreamEndedFlow.collect {
                if(it == StreamState.ENDED) {
                    logS("Removing the fragment")
                    parentFragmentManager
                        .beginTransaction()
                        .remove(this@StreamEnded)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    meetingViewModel.leaveMeeting()
                }
            })
    }
}