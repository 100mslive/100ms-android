package live.hms.android100ms.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.android100ms.databinding.FragmentChatBinding
import live.hms.android100ms.util.viewLifecycle

class ChatFragment : Fragment() {

    private var binding by viewLifecycle<FragmentChatBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }
}