package live.hms.roomkit.ui.meeting.videogrid

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentGridVideoBinding
import live.hms.roomkit.ui.inset.makeInset
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.setIconDisabled
import live.hms.roomkit.ui.theme.setIconEnabled
import live.hms.roomkit.util.NameUtils
import live.hms.roomkit.util.viewLifecycle

class VideoGridFragment : Fragment() {
    companion object {
        private const val TAG = "VideoGridFragment"
    }

    private var binding by viewLifecycle<FragmentGridVideoBinding>()
    private lateinit var settings: SettingsStore

    private lateinit var clipboard: ClipboardManager

    private val meetingViewModel: MeetingViewModel by activityViewModels {
        MeetingViewModelFactory(
            requireActivity().application
        )
    }

    private lateinit var adapter: VideoGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentGridVideoBinding.inflate(inflater, container, false)
        settings = SettingsStore(requireContext())

        initVideoGrid()
        initViewModels()
        return binding.root
    }

    private fun initVideoGrid() {
        adapter = VideoGridAdapter(this@VideoGridFragment) /* { video ->
      Log.v(TAG, "onVideoItemClick: $video")

      Snackbar.make(
          binding.root,
          "Name: ${video.peer.userName} (${video.peer.role}) \nId: ${video.peer.customerUserId}",
          Snackbar.LENGTH_LONG,
      ).setAction("Copy") {
        val clip = ClipData.newPlainText("Customer Id", video.peer.customerUserId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(
            requireContext(),
            "Copied customer id of ${video.peer.userName} to clipboard",
            Toast.LENGTH_SHORT
        ).show()
      }.show()
    } */

        binding.viewPagerVideoGrid.apply {
            offscreenPageLimit = 1
            adapter = this@VideoGridFragment.adapter

            TabLayoutMediator(binding.tabLayoutDots, this) { _, _ ->
                // No text to be shown
            }.attach()
        }

        binding.applyTheme()
        binding.insetPill.makeInset()
        binding.localHmsVideoView?.setZOrderOnTop(true)
        binding.localHmsVideoView?.setZOrderMediaOverlay(true)
        var isMinimized = false


        meetingViewModel.tracks.observe(viewLifecycleOwner) {
            val localMeeting = it.filter { it.isLocal }.firstOrNull()

            //show or hide inset
            if (it.size == 1 && localMeeting != null) {
                binding.insetPill.visibility = View.GONE
            } else if (it.size > 1 && localMeeting != null) {
                binding.insetPill.visibility = View.VISIBLE
            } else if (localMeeting == null) {
                binding.insetPill.visibility = View.GONE
            }

            localMeeting?.let {
                //audio mute icon toggle
                if (it.audio?.isMute == true) {
                    if (isMinimized) {
                        binding.minimizedIconAudioOff.visibility = View.VISIBLE
                        binding.iconAudioOff.visibility = View.GONE
                        binding.minimizedIconAudioOff.setIconDisabled(R.drawable.avd_mic_on_to_off)
                    } else {
                        binding.iconAudioOff.visibility = View.VISIBLE
                    }
                } else {
                    if (isMinimized) {
                        binding.minimizedIconAudioOff.visibility = View.VISIBLE
                        binding.iconAudioOff.visibility = View.GONE
                        binding.minimizedIconAudioOff.setIconDisabled(R.drawable.avd_mic_off_to_on)
                    } else {
                        binding.iconAudioOff.visibility = View.INVISIBLE
                    }

                }

                if (it.video?.isMute == true) {
                    binding.nameInitials.text = NameUtils.getInitials(it.peer.name.orEmpty())

                    binding.localHmsVideoView.visibility =
                        if (isMinimized) View.VISIBLE else View.INVISIBLE

                    binding.nameInitials.visibility = if (isMinimized) View.GONE else View.VISIBLE
                } else {
                    binding.nameInitials.visibility = View.INVISIBLE
                    binding.localHmsVideoView?.visibility = View.VISIBLE
                    binding.localHmsVideoView?.addTrack(it.video!!)
                }

            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModels() {
        meetingViewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            val itemsPerPage = settings.videoGridRows * settings.videoGridColumns
            adapter.totalPages = (tracks.size + itemsPerPage - 1) / itemsPerPage
        }

        if (settings.detectDominantSpeaker) {
            meetingViewModel.pinnedTrackUiUseCase.observe(viewLifecycleOwner) { meetingTrack ->
                if (meetingTrack == null) {
                    binding.dominantSpeakerName.setText(R.string.no_one_speaking)
                } else {
                    binding.dominantSpeakerName.text = "Dominant Speaker: ${meetingTrack.peer.name}"
                }
            }
        } else {
            binding.containerDominantSpeaker.visibility = View.GONE
        }
        binding.containerNetworkInfo.visibility = View.GONE
    }
}