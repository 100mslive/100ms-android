package live.hms.roomkit.ui.meeting.chat.rbac

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
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
import live.hms.roomkit.ui.meeting.participants.MessageHeaderItemDecoration
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.HMSPeer

class RoleBasedChatBottomSheet(
    private val getSelectedRecipient: () -> Recipient?,
    private val recipientSelected: (Recipient) -> Unit
) : BottomSheetDialogFragment() {

    private var initialRecipients : List<Group> = emptyList()
    private var allowedParticipants : AllowedToMessageParticipants? = null
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
            RoleBasedChatBottomSheet({ chatViewModel.currentlySelectedRecipientRbac.value },
                { selectedRecipient ->
                    chatViewModel.updateSelectedRecipientChatBottomSheet(selectedRecipient)
                }).show(fm, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
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
            addItemDecoration(
                MessageHeaderItemDecoration(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.borderBright,
                        HMSPrebuiltTheme.getDefaults().border_bright
                    ), 0, R.layout.layout_role_based_chat_message_bottom_sheet_item_header
                )
            )
        }


        // This would break many things if it were called when no participants were available.
        // crash early to point it out.
        updateInitialRecipients()
        groupieAdapter.update(initialRecipients)

        // There's no need for role change to emit the first time this runs.
        meetingViewModel.roleChange.observe(viewLifecycleOwner) {
            // When the role changes, the allowed participants might have changed.
            updateInitialRecipients()
            updateListWithPeers()
        }
        // Update all peers everytime the peers change
        meetingViewModel.participantPeerUpdate.observe(viewLifecycleOwner) {
            updateListWithPeers()
        }

    }

    private fun updateListWithPeers() {
        val peers = getPeerGroup()
        val list = if(peers == null) {
            initialRecipients
        } else {
            initialRecipients.plus(peers)
        }
        groupieAdapter.update(list)
        // Toggle empty view
        binding.emptyView.visibility = if (list.isEmpty())
            View.VISIBLE
        else
            View.GONE

    }

    private fun updateInitialRecipients() {
        allowedParticipants = meetingViewModel.availableRecipientsForChat()
        initialRecipients = initialAddRecipients(getAllowedParticipants())
    }

    private fun getAllowedParticipants() = allowedParticipants!!
    private fun getInitialRecipients() = initialRecipients

    private fun getPeerGroup(): ExpandableGroup? {
        return if (getAllowedParticipants().peers) {
            val peers = meetingViewModel.hmsSDK.getRemotePeers()
            // Remove the "participants" option if there are no others.
            if (peers.isEmpty())
                null
            else
                getUpdatedPeersGroup(
                    peers, getSelectedRecipient()
                )
        } else {
            null
        }
    }


    private fun onRecipientSelected(recipient: Recipient) {
        recipientSelected(recipient)
        dismissAllowingStateLoss()
    }

    private fun initialAddRecipients(
        allowedParticipants: AllowedToMessageParticipants
    ): List<Group> {
        val hmsSDK = meetingViewModel.hmsSDK
        val recipients = mutableListOf<Group>()
        // For testing, remove when not needed
        // Add `everyone` general chat
        val currentSelectedRecipient = getSelectedRecipient()
        if (allowedParticipants.everyone) {
            recipients.add(
                RecipientItem(
                    Recipient.Everyone,
                    currentSelectedRecipient,
                    ::onRecipientSelected
                )
            )
        }
        // Add roles
        if (allowedParticipants.roles.isNotEmpty()) {
            // There aren't many roles so we'll choose n^2 runtime
            val allRoles = hmsSDK.getRoles().associateBy { it.name }
            val rolesToAdd = allowedParticipants.roles.mapNotNull { allRoles[it] }.map {
                RecipientItem(
                    Recipient.Role(it),
                    currentSelectedRecipient,
                    ::onRecipientSelected
                )
            }
            // Separate headers and roles
            val rolesGroup = ExpandableGroup(RecipientHeader(RECIPIENT_ROLES), true).apply {
                    addAll(rolesToAdd)
                }
            recipients.add(rolesGroup)

        }
        return recipients
    }

    private fun getUpdatedPeersGroup(
        peers : List<HMSPeer>,
        currentSelectedRecipient: Recipient?
    ): ExpandableGroup {
        return ExpandableGroup(RecipientHeader(RECIPIENT_PEERS), true).apply {
                addAll(
                    peers.map {
                        RecipientItem(
                            Recipient.Peer(it),
                            currentSelectedRecipient,
                            ::onRecipientSelected
                        )
                    })
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

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }

}