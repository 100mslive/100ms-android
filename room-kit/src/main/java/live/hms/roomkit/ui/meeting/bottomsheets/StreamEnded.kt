package live.hms.roomkit.ui.meeting.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import live.hms.roomkit.R
import live.hms.roomkit.databinding.StreamEndedBinding
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.util.viewLifecycle

class StreamEnded: Fragment()  {
    companion object {
        const val TAG = "StreamEndedFragment"
        fun launch(fm: FragmentManager) {
            fm
                .beginTransaction()
                .add(R.id.fragment_container, StreamEnded())
                .commit()
        }
    }

    private var binding by viewLifecycle<StreamEndedBinding>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = StreamEndedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()

    }
}