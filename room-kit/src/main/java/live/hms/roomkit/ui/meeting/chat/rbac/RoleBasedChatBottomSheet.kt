package live.hms.roomkit.ui.meeting.chat.rbac

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import live.hms.roomkit.R
import live.hms.roomkit.databinding.LayoutRoleBasedChatBottomSheetSelectorBinding
import live.hms.roomkit.ui.meeting.AllowedToMessageParticipants
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.meeting.chat.ChatViewModel
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.HMSSDK

class RoleBasedChatBottomSheet(
    private val getSelectedRecipient : () -> Recipient,
    private val recipientSelected: (Recipient) -> Unit) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<LayoutRoleBasedChatBottomSheetSelectorBinding>()
    private val groupieAdapter = GroupieAdapter()

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    companion object {
        val TAG = "RoleBasedChatBottomSheet"
        fun launch(fm: FragmentManager, chatViewModel: ChatViewModel) {
            RoleBasedChatBottomSheet(
                { chatViewModel.currentlySelectedRecipient.value!! },
                { selectedRecipient ->
                    chatViewModel.updateSelectedRecipientChatBottomSheet(selectedRecipient)
                }
                ).show(fm, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = LayoutRoleBasedChatBottomSheetSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyTheme()
        binding.applyTheme()
        dialog?.let {
            val sheet = it as BottomSheetDialog
            sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


        binding.closeBtn.setOnClickListener {
            dismissAllowingStateLoss()
        }

        with(binding.optionsGrid) {
            adapter = groupieAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            val divider = DividerItemDecoration(context, RecyclerView.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.bottom_sheet_divider, null)!!)
            }
            addItemDecoration(divider)
        }

        val testing = true
        val allowedParticipants = meetingViewModel.availableRecipientsForChat()
        val initialRecipients = initialAddRecipients(allowedParticipants, testing)
        groupieAdapter.update(initialRecipients)
        if(allowedParticipants.peers || testing) {
            // Update all peers everytime the peers change
            meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
                groupieAdapter.update(
                    initialRecipients.plus(
                        getUpdatedPeersGroup(
                            meetingViewModel.hmsSDK, getSelectedRecipient()
                        )
                    )
                )
            }
        }
    }

    private fun onRecipientSelected(recipient: Recipient) {
        recipientSelected(recipient)
        dismissAllowingStateLoss()
    }

    private fun initialAddRecipients(allowedParticipants: AllowedToMessageParticipants,
                                     forTestingAllowAll : Boolean): List<Group> {
        val hmsSDK = meetingViewModel.hmsSDK
        val recipients = mutableListOf<Group>()
        // For testing, remove when not needed
        // Add `everyone` general chat
        val currentSelectedRecipient = getSelectedRecipient()
        if(allowedParticipants.everyone || forTestingAllowAll) {
            recipients.add(RecipientItem(Recipient.Everyone, currentSelectedRecipient, ::onRecipientSelected))
        }
        // Add roles
        if(allowedParticipants.roles.isNotEmpty() || forTestingAllowAll) {
            // There aren't many roles so we'll choose n^2 runtime
            val rolesToAdd = if(forTestingAllowAll) {
                hmsSDK.getRoles().map { RecipientItem(Recipient.Role(it), currentSelectedRecipient, ::onRecipientSelected) }
            } else {
                // Create a map of roles to their names
                val allRoles = hmsSDK.getRoles().associateBy { it.name }
                allowedParticipants.roles
                    .mapNotNull { allRoles[it] }
                    .map { RecipientItem(Recipient.Role(it), currentSelectedRecipient, ::onRecipientSelected) }
            }
            // Separate headers and roles
            val rolesGroup = ExpandableGroup(RecipientHeader("ROLES"), true)
                .apply {
                    addAll(rolesToAdd)
                }
            recipients.add(rolesGroup)

        }
        return recipients
    }

    private fun getUpdatedPeersGroup(hmsSDK : HMSSDK, currentSelectedRecipient: Recipient): ExpandableGroup {
        return ExpandableGroup(RecipientHeader("PARTICIPANTS"), true)
            .apply {
                addAll(hmsSDK.getPeers().map { RecipientItem(Recipient.Peer(it), currentSelectedRecipient, ::onRecipientSelected) })
            }
    }

    private fun applyTheme() {

        binding.title.setTextColor(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )

        binding.closeBtn.drawable.setTint(
            getColorOrDefault(
                HMSPrebuiltTheme.getColours()?.onSurfaceHigh,
                HMSPrebuiltTheme.getDefaults().onsurface_high_emp
            )
        )
    }

}