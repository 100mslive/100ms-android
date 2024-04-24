package live.hms.roomkit.ui.meeting.chat.combined

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.participants.ParticipantsTabFragment
import live.hms.roomkit.ui.theme.applyTheme


class ChatOnlyAdapter(val fragment: BottomSheetDialogFragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 1

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int).

        return CombinedChatFragmentTab().apply {
            this.dismissAllowingStateLoss = fragment::dismissAllowingStateLoss
        }
    }
}
class ChatParticipantAdapter(val fragment: BottomSheetDialogFragment) : FragmentStateAdapter(fragment) {
    val partFragment = ParticipantsTabFragment().apply {
        this.dismissFragment = fragment::dismiss
    }
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // Return a NEW fragment instance in createFragment(int).

        return if (position == 0)
            CombinedChatFragmentTab().apply {
                this.dismissAllowingStateLoss = fragment::dismissAllowingStateLoss
            }
        else
            partFragment
    }
}
const val OPEN_TO_PARTICIPANTS: String= "CHAT_COMBINED_OPEN_PARTICIPANTS"
const val OPEN_TO_CHAT_ALONE: String= "CHAT_TAB_ONLY"
const val CHAT_TAB_TITLE : String = "CHAT_TITLE_TEXT"
class ChatParticipantCombinedFragment : BottomSheetDialogFragment() {
    private lateinit var binding : LayoutChatParticipantCombinedBinding//by viewLifecycle<LayoutChatParticipantCombinedBinding>()
    lateinit var pagerAdapter : FragmentStateAdapter//by lazy { PagerAdapter(meetingViewmodel, chatViewModel, chatAdapter, viewLifecycleOwner) }
//    val meetingViewModel by activityViewModels<MeetingViewModel>()
//    private val args: ChatParticipantCombinedFragmentArgs by navArgs()
companion object {
    val TAG: String = "CombinedChatFragmentTag"
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.RoundedTabDialogTheme);
    }

    private fun getCustomChatTitle() = arguments?.getString(CHAT_TAB_TITLE)
    private fun hideParticipantTab() : Boolean =
        arguments?.getBoolean(OPEN_TO_CHAT_ALONE, false) == true
    private fun jumpToParticipantsTab() : Boolean =
        arguments?.getBoolean(OPEN_TO_PARTICIPANTS, false) == true

    private fun addObservers() {
        initOnBackPress()
//        meetingViewModel.peerCount.observe(viewLifecycleOwner) {
//
//            binding.tabLayout.getTabAt(1)?.setText("$it")
//        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheet = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        val view = View.inflate(context, R.layout.layout_chat_participant_combined, null)

        binding = LayoutChatParticipantCombinedBinding.bind(view)
        bottomSheet.setContentView(view)

        pagerAdapter = if(hideParticipantTab())
            ChatOnlyAdapter(this)
        else
            ChatParticipantAdapter(this)

        binding.pager.adapter = pagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.pager) { tab, position ->
            tab.text = if(position == 0 ) getCustomChatTitle() else "Participants"
        }.apply {
            attach()
        }

        binding.closeCombinedTabButton.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }

        if(jumpToParticipantsTab() && !hideParticipantTab())
            binding.pager.post {
                binding.pager.setCurrentItem(1, true)
            }
        bottomSheet.setOnShowListener {
            BottomSheetBehavior.from(view.parent as View).apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        binding.applyTheme(hideParticipantTab())
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