package live.hms.app2.ui.meeting.participants

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.LayoutRtmpRecordingBinding
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.ui.settings.SettingsStore
import live.hms.app2.util.viewLifecycle
import live.hms.video.media.settings.HMSRtmpVideoResolution
import java.net.URI
import java.net.URISyntaxException


class RtmpRecordBottomSheet(val startClickListener: ()->Unit) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<LayoutRtmpRecordingBinding>()
    private lateinit var settings: SettingsStore

    private val meetingViewModel: MeetingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = LayoutRtmpRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsStore(requireContext())
        // Get a listener on the page for the urls.

        binding.addRtmpUrlButton.setOnClickListener { addItem() }
        binding.startButton.setOnClickListener { startClicked() }
        enableDisable()
        addItem()
    }

    private fun enableDisable() {
        with(binding) {
            enableDisableRtmp()
            addRtmpUrlButton.isEnabled = meetingViewModel.isAllowedToRtmpStream()
            shouldRecord.isEnabled = meetingViewModel.isAllowedToBrowserRecord()
        }
    }

    private fun enableDisableRtmp() {
        // addRtmpUrlButton,rtmpUrlContainer,existingRtmpUrlsText,rtmpUrls
        val enabled = meetingViewModel.isAllowedToRtmpStream()
        with(binding) {
            addRtmpUrlButton.isVisible = enabled
        }
    }

    private fun addItem() {

        val layout = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_rtmp_add_view, null, false)
        val expansionToggle = layout.findViewById<ImageView>(R.id.expansion_toggle)
        expansionToggle.setOnClickListener {
            val streamingUrlView = layout.findViewById<EditText>(R.id.streaming_key)
            val streamingTitleView = layout.findViewById<TextView>(R.id.streaming_url_title)
            val rtmpUrlView = layout.findViewById<EditText>(R.id.rtmp_url)
            val rtmpUrlTitleView = layout.findViewById<TextView>(R.id.rtmp_url_title)

            if (streamingUrlView.isVisible) {
                streamingUrlView.visibility = View.GONE
                streamingTitleView.visibility = View.GONE
                rtmpUrlView.visibility = View.GONE
                rtmpUrlTitleView.visibility = View.GONE
                expansionToggle.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_keyboard_arrow_down_24
                    )
                )
            } else {
                streamingUrlView.visibility = View.VISIBLE
                streamingTitleView.visibility = View.VISIBLE
                rtmpUrlView.visibility = View.VISIBLE
                rtmpUrlTitleView.visibility = View.VISIBLE
                expansionToggle.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_keyboard_arrow_up_24
                    )
                )
            }
        }

        layout.findViewById<ImageView>(R.id.vertical_menu).setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext()).create()
            val dialogView: View? = requireActivity().layoutInflater.inflate(
                R.layout.dialog_stream_edit,
                null
            )
            dialog.setView(dialogView)
            // Coordinates relative to parent
            val exitBtn = layout.findViewById<ImageView>(R.id.vertical_menu)

            val location = IntArray(2)
            exitBtn.getLocationOnScreen(location)
            val x = location[0]
            val y = location[1]

            val params = WindowManager.LayoutParams()
            params.y = y - exitBtn.height
            params.x = x
            params.gravity = Gravity.TOP or Gravity.LEFT
            dialog.window!!.attributes = params
            dialog.window?.setDimAmount(0f)
            dialog.window?.setBackgroundDrawableResource(R.color.primary_bg)

            dialog.window?.attributes?.flags =
                dialog.window?.attributes?.flags?.and((WindowManager.LayoutParams.FLAG_DIM_BEHIND).inv())

            dialog.show()

            dialog.findViewById<TextView>(R.id.btn_end_session)?.setOnClickListener{
                if (binding.streamUrlList.childCount > 0){
                    binding.streamUrlList.removeView(layout)
                }
                dialog.dismiss()
                validate()

            }
        }
        binding.streamUrlList.addView(layout)
        validate()
    }

    fun validate(){
        if (binding.streamUrlList.childCount >= 3) {
            binding.addRtmpUrlButton.visibility = View.GONE
        }else{
            binding.addRtmpUrlButton.visibility = View.VISIBLE
        }
    }

    private fun checkValidUrl(url: String): Boolean = try {
        val uri = URI(url)
        uri.scheme == "rtmp"
    } catch (e: URISyntaxException) {
        false
    }

    private fun checkInputWidthHeight(width: Int?, height: Int?): HMSRtmpVideoResolution? {
        if (width == null || height == null) {
            return null
        }
        return HMSRtmpVideoResolution(width, height)
    }

    private fun startClicked() {
        // Create a config and start
        val newList = arrayListOf<String>()
        binding.streamUrlList.forEach {
            val rtmpUrl = it.findViewById<EditText>(R.id.rtmp_url).text.toString()
            val streamKey = it.findViewById<EditText>(R.id.streaming_key).text.toString()
            if (rtmpUrl.isNullOrEmpty().not() && checkValidUrl(rtmpUrl) && streamKey.isNullOrEmpty()
                    .not()
            ) {
                newList.add("$rtmpUrl$streamKey")
            }
        }
        settings.rtmpUrlsList = newList.toSet()

        val isRecording = binding.shouldRecord.isChecked

        val isRtmp = meetingViewModel.isAllowedToRtmpStream()

        val inputWidthHeight: HMSRtmpVideoResolution? =
            checkInputWidthHeight(
                binding.rtmpWidth.text.toString().toIntOrNull(),
                binding.rtmpHeight.text.toString().toIntOrNull()
            )

        val rtmpUrls = settings.rtmpUrlsList.toList()
        if (isRecording || isRtmp && (inputWidthHeight?.height ?: 0) > 0 && (inputWidthHeight?.width ?: 0) > 0) {
            startClickListener.invoke()
            meetingViewModel.recordMeeting(
                isRecording,
                rtmpUrls,
                inputWidthHeight
            )
            dismiss()
        }else{
            Toast.makeText(requireContext(),"Enter required fields before starting RTMP.",Toast.LENGTH_LONG).show()
        }
    }
}

// this is only required for hms beam bots, your beam auth mechanism will likely be different.
fun String.meetingToHlsUrl() =
    "${this.replace("/meeting/", "/preview/")}?skip_preview=true"