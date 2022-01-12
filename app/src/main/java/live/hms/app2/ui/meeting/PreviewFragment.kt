package live.hms.app2.ui.meeting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import kotlinx.coroutines.launch
import live.hms.app2.R
import live.hms.app2.databinding.FragmentPreviewBinding
import live.hms.app2.model.RoomDetails
import live.hms.app2.ui.home.HomeActivity
import live.hms.app2.util.*
import live.hms.video.error.HMSException
import live.hms.video.media.tracks.HMSLocalAudioTrack
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.media.tracks.HMSTrack
import live.hms.video.sdk.HMSPreviewListener
import live.hms.video.sdk.models.HMSRoom
import live.hms.video.utils.HMSCoroutineScope

class PreviewFragment : Fragment() {

  companion object {
    private const val TAG = "PreviewFragment"
  }

  private var binding by viewLifecycle<FragmentPreviewBinding>()

  private lateinit var roomDetails: RoomDetails

  private val meetingViewModel: MeetingViewModel by activityViewModels {
    MeetingViewModelFactory(
      requireActivity().application,
      requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
    )
  }

  private var alertDialog: AlertDialog? = null

  private lateinit var track: MeetingTrack

  private var isViewVisible = false

  override fun onResume() {
    super.onResume()
    isViewVisible = true
    bindVideo()
  }

  override fun onPause() {
    super.onPause()
    isViewVisible = false
    unbindVideo()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    roomDetails = requireActivity().intent!!.extras!![ROOM_DETAILS] as RoomDetails
  }

  private fun bindVideo() {
    if (this::track.isInitialized && track.video?.isMute == false) {
      SurfaceViewRendererUtil.bind(binding.previewView, track)
      binding.previewView.visibility = View.VISIBLE
    } else {
      binding.previewView.visibility = View.GONE
    }
  }

  private fun unbindVideo() {
    binding.previewView.visibility = View.GONE

    if (this::track.isInitialized) {
      SurfaceViewRendererUtil.unbind(binding.previewView, track)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPreviewBinding.inflate(inflater, container, false)

    initOnBackPress()
    initButtons()
    startPreview()

    requireActivity().invalidateOptionsMenu()
    return binding.root
  }

  private fun initButtons() {
    binding.buttonToggleVideo.apply {
      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonToggleVideo.onClick()")

        (track.video as HMSLocalVideoTrack?)?.let {
          if (it.isMute) {
            // Un-mute this track
            it.setMute(false)
            if (isViewVisible) {
              bindVideo()
            }
          } else {
            // Mute this track
            it.setMute(true)
            if (isViewVisible) {
              unbindVideo()
            }
          }

          setImageResource(
            if (it.isMute) R.drawable.ic_videocam_off_24
            else R.drawable.ic_videocam_24
          )
        }

      }
    }

    binding.buttonToggleAudio.apply {
      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonToggleAudio.onClick()")

        (track.audio as HMSLocalAudioTrack?)?.let {
          it.setMute(!it.isMute)

          setImageResource(
            if (it.isMute) R.drawable.ic_mic_off_24
            else R.drawable.ic_mic_24
          )
        }
      }
    }

    binding.buttonJoinMeeting.apply {
      setOnSingleClickListener(200L) {
        Log.v(TAG, "buttonJoinMeeting.onClick()")

        findNavController().navigate(
          PreviewFragmentDirections.actionPreviewFragmentToMeetingFragment()
        )
      }
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)

    menu.forEach { item ->
      if (item.itemId != R.id.action_flip_camera && item.itemId != R.id.action_volume) {
        item.isVisible = false
      }
    }
  }

  private fun updateActionVolumeMenuIcon(item: MenuItem) {
    item.apply {
      if (meetingViewModel.isPeerAudioEnabled()) {
        setIcon(R.drawable.ic_volume_up_24)
      } else {
        setIcon(R.drawable.ic_volume_off_24)
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_flip_camera -> {
        if (this::track.isInitialized) {
          HMSCoroutineScope.launch {
            (track.video as HMSLocalVideoTrack?)?.switchCamera()
          }
        }
      }
      R.id.action_volume -> {
        meetingViewModel.apply {
          toggleAudio()
          updateActionVolumeMenuIcon(item)
        }
      }
    }

    return false
  }

  private fun goToHomePage() {
    Intent(requireContext(), HomeActivity::class.java).apply {
      crashlyticsLog(
        TAG,
        "MeetingActivity.finish() -> going to HomeActivity :: $this"
      )
      startActivity(this)
    }
    requireActivity().finish()
  }

  private fun startPreview() {
    meetingViewModel.startPreview(object : HMSPreviewListener {
      override fun onError(error: HMSException) {
        requireActivity().runOnUiThread {
          if (error.isTerminal) {
            binding.buttonJoinMeeting.isEnabled = false
            AlertDialog.Builder(requireContext())
                    .setTitle(error.name)
                    .setMessage(error.toString())
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                      dialog.dismiss()
                      goToHomePage()
                    }
                    .setNeutralButton(R.string.bug_report) { _, _ ->
                      requireContext().startActivity(
                              EmailUtils.getNonFatalLogIntent(requireContext())
                      )
                      alertDialog = null
                    }
                    .create()
                    .show()
          } else {
            Toast.makeText(context, error.description, Toast.LENGTH_LONG).show()
          }
        }
      }

      override fun onPreview(room: HMSRoom, localTracks: Array<HMSTrack>) {
        // We assume  here that localTracks has at-most 2 tracks
        // containing one video & one audio track
        val activity = activity
        if (activity != null) {
          activity.runOnUiThread {
            binding.nameInitials.text = NameUtils.getInitials(room.localPeer!!.name)
            binding.buttonJoinMeeting.isEnabled = true

            track = MeetingTrack(room.localPeer!!, null, null)
            localTracks.forEach {
              when (it) {
                is HMSLocalAudioTrack -> {
                  track.audio = it
                }
                is HMSLocalVideoTrack -> {
                  track.video = it

                  if (isViewVisible) {
                    bindVideo()
                  }
                }
              }
            }

            // Disable buttons
            binding.buttonToggleVideo.apply {
              isEnabled = (track.video != null)

              track.video?.let {
                setImageResource(
                        if (it.isMute) R.drawable.ic_videocam_off_24
                        else R.drawable.ic_videocam_24
                )
              }
            }
            binding.buttonToggleAudio.apply {
              isEnabled = (track.audio != null)

              track.audio?.let {
                setImageResource(
                        if (it.isMute) R.drawable.ic_mic_off_24
                        else R.drawable.ic_mic_24
                )
              }
            }
          }
        } else {
          Log.e(TAG, "Attempted to show preview when activity was null")
        }
      }
    })
  }

  private fun initOnBackPress() {
    requireActivity().onBackPressedDispatcher.addCallback(
      viewLifecycleOwner,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          meetingViewModel.leaveMeeting()
          goToHomePage()
        }
      })
  }
}