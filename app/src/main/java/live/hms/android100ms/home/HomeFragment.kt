package live.hms.android100ms.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.android100ms.databinding.FragmentHomeBinding
import live.hms.android100ms.util.viewLifecycle

class HomeFragment : Fragment() {

    private var binding by viewLifecycle<FragmentHomeBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
}