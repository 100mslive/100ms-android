package live.hms.roomkit.ui.meeting.chat.combined

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutChatParticipantCombinedBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.participants.ParticipantsFragment
import live.hms.roomkit.util.viewLifecycle
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
class ChatParticipantCombinedFragment : Fragment() {
    private var binding by viewLifecycle<LayoutChatParticipantCombinedBinding>()
    lateinit var pagerAdapter : ChatParticipantAdapter//by lazy { PagerAdapter(meetingViewmodel, chatViewModel, chatAdapter, viewLifecycleOwner) }
    val meetingViewModel : MeetingViewModel by activityViewModels()
    private val args: ChatParticipantCombinedFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = LayoutChatParticipantCombinedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagerAdapter = ChatParticipantAdapter(this)
        binding.pager.adapter = pagerAdapter
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, binding.pager) { tab, position ->
            tab.text = if(position == 0 ) "CHAT" else "PARTICIPANTS"
        }.attach()
        initOnBackPress()
        binding.closeCombinedTabButton.setOnSingleClickListener {
            findNavController().popBackStack()
        }
        if(args.showParticipants)
            binding.pager.post {
                binding.pager.setCurrentItem(1, true)
            }
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