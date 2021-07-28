package live.hms.app2.ui.meeting.participants

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.R
import live.hms.app2.databinding.FragmentParticipantsBinding
import live.hms.app2.databinding.LayoutFragmentBottomSheetChangeRoleBinding
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import android.widget.Spinner




class BottomSheetRoleChangeFragment : BottomSheetDialogFragment(), AdapterView.OnItemSelectedListener {
    private val TAG = BottomSheetRoleChangeFragment::class.java.simpleName
    private val meetingViewModel: MeetingViewModel by activityViewModels()

    private var binding by viewLifecycle<LayoutFragmentBottomSheetChangeRoleBinding>()
    private val availableRoleStrings = listOf("a","b","c")
    private lateinit var popupSpinner : Spinner

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = LayoutFragmentBottomSheetChangeRoleBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        popupSpinner = view.findViewById(R.id.retroSpinner)
        initPopup()
        initListeners()
    }

    private fun initPopup() {
        ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_list_item_1,
            availableRoleStrings
        ).also { arrayAdapter ->
            popupSpinner.adapter = arrayAdapter
            popupSpinner.prompt = "Choose"
            popupSpinner.setSelection(0, true)
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            popupSpinner.onItemSelectedListener = this
        }
    }

    private fun initListeners() {
        with(binding) {
            cancel.setOnClickListener { findNavController().popBackStack() }
            forceChangeRole.setOnClickListener { spinnerDialog() }
        }
    }

    private fun spinnerDialog() {
        popupSpinner.performClick()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val stringRole = parent?.adapter?.getItem(position)
        Log.d(TAG, "Selected role: $stringRole")
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "Nothing selected")
    }
}
