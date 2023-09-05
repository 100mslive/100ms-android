package live.hms.roomkit.ui.meeting.chat.combined

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.participants.ParticipantsFragment


class ChatParticipantAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int).

        return if (position == 0)
            CombinedChatFragmentTab()
        else
            ParticipantsFragment()
    }
}
const val OPEN_TO_PARTICIPANTS: String= "CHAT_COMBINED_OPEN_PARTICIPANTS"
class ChatParticipantCombinedFragment : BottomSheetDialogFragment() {
    private lateinit var binding : LayoutChatParticipantCombinedBinding//by viewLifecycle<LayoutChatParticipantCombinedBinding>()
    lateinit var pagerAdapter : ChatParticipantAdapter//by lazy { PagerAdapter(meetingViewmodel, chatViewModel, chatAdapter, viewLifecycleOwner) }
    val meetingViewModel : MeetingViewModel by activityViewModels()
//    private val args: ChatParticipantCombinedFragmentArgs by navArgs()
companion object {
    val TAG: String = "CombinedChatFragmentTag"
}

    private fun getShowParticipants() : Boolean =
        arguments?.getBoolean(OPEN_TO_PARTICIPANTS, false) == true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheet = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val view = View.inflate(context, R.layout.layout_chat_participant_combined, null)

        binding = LayoutChatParticipantCombinedBinding.bind(view)
        bottomSheet.setContentView(view)

        pagerAdapter = ChatParticipantAdapter(this)
        binding.pager.adapter = pagerAdapter
        val tabLayout = binding.tabLayout
        TabLayoutMediator(tabLayout, binding.pager) { tab, position ->
            tab.text = if(position == 0 ) "Chat" else "Participants"
        }.attach()

        binding.closeCombinedTabButton.setOnSingleClickListener {
            findNavController().popBackStack()
        }

        if(getShowParticipants())
            binding.pager.post {
                binding.pager.setCurrentItem(1, true)
            }
        bottomSheet.setOnShowListener {
            BottomSheetBehavior.from(view.parent as View).apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return bottomSheet
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initOnBackPress()
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().popBackStack()
                }
            })
    }
}