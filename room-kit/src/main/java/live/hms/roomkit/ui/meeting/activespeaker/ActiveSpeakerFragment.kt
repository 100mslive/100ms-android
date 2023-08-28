package live.hms.roomkit.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentActiveSpeakerBinding
import live.hms.roomkit.util.*

class ActiveSpeakerFragment : Fragment()  {

  companion object {
    private const val TAG = "ActiveSpeakerFragment"
  }

  private var binding by viewLifecycle<FragmentActiveSpeakerBinding>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentActiveSpeakerBinding.inflate(inflater, container, false)

    return binding.root
  }


}