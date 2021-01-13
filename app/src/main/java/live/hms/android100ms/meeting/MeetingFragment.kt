package live.hms.android100ms.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.android100ms.databinding.FragmentMeetingBinding
import live.hms.android100ms.util.viewLifecycle

class MeetingFragment : Fragment() {

    private var binding by viewLifecycle<FragmentMeetingBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingBinding.inflate(inflater, container, false)
        return binding.root
    }
}