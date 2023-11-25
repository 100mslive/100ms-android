package live.hms.roomkit.ui.meeting

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.Group
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import live.hms.roomkit.R
import live.hms.roomkit.databinding.BottomSheetMessageOptionsBinding
import live.hms.roomkit.databinding.BottomSheetOptionBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.GridOptionItem
import live.hms.roomkit.ui.HMSPrebuiltOptions
import live.hms.roomkit.ui.meeting.chat.ChatMessage
import live.hms.roomkit.ui.theme.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.getColorOrDefault
import live.hms.roomkit.ui.theme.getShape
import live.hms.roomkit.util.ROOM_PREBUILT
import live.hms.roomkit.util.viewLifecycle
import live.hms.video.sdk.models.enums.HMSRecordingState
const val CHAT_MESSAGE_OPTIONS_EXTRA = "ChatMessageOptionsExtra"
class MessageOptionsBottomSheet(val chatMessage: ChatMessage): BottomSheetDialogFragment() {

    private var binding by viewLifecycle<BottomSheetMessageOptionsBinding>()
//    private val chatMessage: ChatMessage? = arguments?.getParcelable(CHAT_MESSAGE_OPTIONS_EXTRA) as ChatMessage?

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetMessageOptionsBinding.inflate(inflater, container, false)
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

        if(meetingViewModel.isAllowedToBlockFromChat()) {
            binding.optionBlockFromChat.visibility = View.VISIBLE
        } else {
            binding.optionBlockFromChat.visibility = View.GONE
        }

        if(meetingViewModel.isAllowedToPinMessages()){
            binding.optionPinMessage.visibility = View.VISIBLE
        } else {
            binding.optionPinMessage.visibility = View.GONE
        }

        if(chatMessage == null) {
            Log.d("MessageOptions","No chat message received, closing")
            dismissAllowingStateLoss()
        }
        else {
            with(binding) {
                optionPinMessage.setOnSingleClickListener {
                    meetingViewModel.pinMessage(chatMessage)
                    dismiss()
                }

                optionBlockFromChat.setOnSingleClickListener {
                    meetingViewModel.blockUser(chatMessage)
                    dismiss()
                }
            }
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

//    override fun getTheme(): Int {
//        return R.style.AppBottomSheetDialogTheme
//    }
}