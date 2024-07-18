package live.hms.roomkit.ui.diagnostic.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentMeetingBinding
import live.hms.roomkit.databinding.FragmentPreCallRegionSelectionBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModel
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModelFactory
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.MeetingViewModelFactory
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.buttonEnabled
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.roomkit.util.viewLifecycle

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PreCallRegionSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreCallRegionSelectionFragment : Fragment() {
    private var binding by viewLifecycle<FragmentPreCallRegionSelectionBinding>()

    private val vm: DiagnosticViewModel by activityViewModels {
        DiagnosticViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPreCallRegionSelectionBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
        //add code to create chip group and programmatically add chips

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        binding.regionChipGroup.setOnCheckedStateChangeListener(ChipGroup.OnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@OnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedIds.first())
            vm.setRegionPreference(chip.text.toString())
            //add code to get the selected region
        })
        binding.regionChipGroup.isSingleSelection = true

        vm.getRegionList().forEachIndexed { index, pair ->
            val chip = Chip(requireContext())
            chip.text = pair.second
            chip.setTextColor(
                getColorOrDefault(
                    HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                    HMSPrebuiltTheme.getDefaults().onsurface_high_emp
                )
            )
            chip.isCheckable = true
            if (index == 0) chip.isChecked = true
            binding.regionChipGroup.addView(chip)
        }

        binding.continueButton.buttonEnabled()

        binding.continueButton.setOnSingleClickListener {
            //add code to navigate to next fragment
            findNavController().navigate(PreCallRegionSelectionFragmentDirections.actionPreCallRegionSelectionFragmentToPreCallCameraFragment())
        }
    }

}