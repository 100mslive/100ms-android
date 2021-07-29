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
import androidx.navigation.fragment.navArgs


class BottomSheetRoleChangeFragment : BottomSheetDialogFragment(), AdapterView.OnItemSelectedListener {
    private val TAG = BottomSheetRoleChangeFragment::class.java.simpleName
    private val meetingViewModel: MeetingViewModel by activityViewModels()
    private val args : BottomSheetRoleChangeFragmentArgs by navArgs()
    private var isForce : Boolean? = null

    private var binding by viewLifecycle<LayoutFragmentBottomSheetChangeRoleBinding>()
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
            args.availableRoles
        ).also { arrayAdapter ->
            popupSpinner.adapter = arrayAdapter
            popupSpinner.prompt = "Role Change Request"
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            popupSpinner.post { popupSpinner.onItemSelectedListener = this }
        }
    }

    private fun initListeners() {
        with(binding) {
            cancel.setOnClickListener { findNavController().popBackStack() }
            forceChangeRole.setOnClickListener {
                isForce = true
                spinnerDialog()
            }
            promptChangeRole.setOnClickListener {
                isForce = false
                spinnerDialog()
            }
        }
    }

    private fun spinnerDialog() {
        popupSpinner.performClick()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val stringRole = parent?.adapter?.getItem(position) as String
        Log.d(TAG, "Selected role: $stringRole")
        meetingViewModel.changeRole(args.remotePeerId, stringRole, isForce!!)
        findNavController().popBackStack()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(TAG, "Nothing selected")
        findNavController().popBackStack()
    }
}
