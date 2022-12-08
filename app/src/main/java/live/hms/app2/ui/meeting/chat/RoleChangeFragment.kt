package live.hms.app2.ui.meeting.chat

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import live.hms.app2.R
import live.hms.app2.databinding.BulkRoleChangeFragmentBinding
import live.hms.app2.ui.meeting.MeetingViewModel
import live.hms.app2.util.viewLifecycle
import live.hms.video.sdk.models.role.HMSRole


class RoleChangeFragment : Fragment() {

    val TAG = "RoleChangeFragment"
    private var binding by viewLifecycle<BulkRoleChangeFragmentBinding>()
    private val vm by viewModels<RoleChangeViewModel>(factoryProducer = {
        object : ViewModelProvider.Factory{
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val meetingViewModel: MeetingViewModel by activityViewModels()
                return RoleChangeViewModel(meetingViewModel::getAvailableRoles,meetingViewModel::bulkRoleChange) as T
            }
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BulkRoleChangeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRoleToChangeToSpinner(vm.rolesList.map{ RolePresenter(it) })
        binding.btnChangeRoles.setOnClickListener {
            vm.changeRoles()
            findNavController().navigate(RoleChangeFragmentDirections.actionRoleChangeFragmentToMeetingFragment())
        }
        binding.fromRolesTextView.setOnClickListener {
            showRoleSelectDialog(vm.rolesList)
        }
        vm.selectedRolesToChange.observe(viewLifecycleOwner){
            binding.fromRolesTextView.text = it
        }
        binding.closeBtn.setOnClickListener {
            findNavController().navigate(RoleChangeFragmentDirections.actionRoleChangeFragmentToMeetingFragment())
        }
    }

    private fun showRoleSelectDialog(rolesList : List<HMSRole>) {
        with(AlertDialog.Builder(requireContext())) {
            val items = rolesList.map{ it.name }.toTypedArray<CharSequence>()
            setMultiChoiceItems(items, BooleanArray(items.size),vm)
            setPositiveButton("Ok") {_,_ -> vm.rolesToChangeSelected()}
            setTitle("Roles to be changed")
            setCancelable(false)
            show()
        }
    }

    fun initRoleToChangeToSpinner(roles : List<RolePresenter>) {
        ArrayAdapter(requireContext(), R.layout.layout_chat_recipient_selector_item, roles)
            .also { recipientsAdapter ->
                binding.toRoleSpinner.onItemSelectedListener = vm
                binding.toRoleSpinner.adapter = recipientsAdapter
                recipientsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

}

data class RolePresenter(val hmsRole : HMSRole) {
    override fun toString(): String =
        hmsRole.name
}

