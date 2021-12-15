package live.hms.app2.ui.meeting.activespeaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.app2.databinding.HlsFragmentLayoutBinding
import live.hms.app2.ui.meeting.HlsPlayer
import live.hms.app2.util.viewLifecycle

class HlsFragment : Fragment() {

    private var binding by viewLifecycle<HlsFragmentLayoutBinding>()
    private val hlsPlayer : HlsPlayer by lazy{
        HlsPlayer()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HlsFragmentLayoutBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.hlsView.player = hlsPlayer.getPlayer(requireContext())
    }

    override fun onStop() {
        super.onStop()
        hlsPlayer.releasePlayer()
    }
}