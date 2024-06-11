package live.hms.roomkit.ui.diagnostic.fragments

import android.content.res.Resources.Theme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import live.hms.roomkit.databinding.FragmentPreCallConnectivityTestBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle


/**
 * A simple [Fragment] subclass.
 * create an instance of this fragment.
 */
class PreCallConnectivityTestFragment : Fragment() {

    private var binding by viewLifecycle<FragmentPreCallConnectivityTestBinding>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreCallConnectivityTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
    }

}