package live.hms.roomkit.ui.meeting.chat.combined

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.participants.ParticipantsTabFragment
import live.hms.roomkit.ui.theme.applyTheme


class ChatParticipantAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int).

        return if (position == 0)
            CombinedChatFragmentTab()
        else
            ParticipantsTabFragment()
    }
}
const val OPEN_TO_PARTICIPANTS: String= "CHAT_COMBINED_OPEN_PARTICIPANTS"
class ChatParticipantCombinedFragment : BottomSheetDialogFragment() {
    private lateinit var binding : LayoutChatParticipantCombinedBinding//by viewLifecycle<LayoutChatParticipantCombinedBinding>()
    lateinit var pagerAdapter : ChatParticipantAdapter//by lazy { PagerAdapter(meetingViewmodel, chatViewModel, chatAdapter, viewLifecycleOwner) }
    val meetingViewModel by activityViewModels<MeetingViewModel>()
//    private val args: ChatParticipantCombinedFragmentArgs by navArgs()
companion object {
    val TAG: String = "CombinedChatFragmentTag"
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.RoundedTabDialogTheme);
    }

    private fun getShowParticipants() : Boolean =
        arguments?.getBoolean(OPEN_TO_PARTICIPANTS, false) == true

    private fun addObservers() {
        Log.d("asdadasf","Updating ")
        initOnBackPress()
        meetingViewModel.peerCount.observe(viewLifecycleOwner) {

            binding.tabLayout.getTabAt(1)?.setText("$it")
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheet = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val view = View.inflate(context, R.layout.layout_chat_participant_combined, null)

        binding = LayoutChatParticipantCombinedBinding.bind(view)
        bottomSheet.setContentView(view)

        pagerAdapter = ChatParticipantAdapter(this)
        binding.pager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = if(position == 0 ) "Chat" else "Participants"
        }.apply {
            attach()
        }

        binding.closeCombinedTabButton.setOnSingleClickListener {
            dismissAllowingStateLoss()
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
        binding.applyTheme()
        return bottomSheet
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        addObservers()
    }

    private fun initOnBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    dismissAllowingStateLoss()
                }
            })
    }
}