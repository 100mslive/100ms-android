package live.hms.roomkit.ui.meeting.chat

import android.content.DialogInterface
import android.content.DialogInterface.OnMultiChoiceClickListener
import android.util.Log
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import live.hms.video.sdk.models.role.HMSRole

class RoleChangeViewModel(private val getRoles :() -> List<HMSRole>, private val bulkRoleChange : (toRole : HMSRole, rolesToChange : List<HMSRole>) -> Unit) : ViewModel(), AdapterView.OnItemSelectedListener, OnMultiChoiceClickListener {
    private val selectedRole = MutableLiveData<RolePresenter>()
    private val _selectedRolesToChange : MutableSet<CharSequence> = mutableSetOf()
    val selectedRolesToChange = MutableLiveData<String>()

    val rolesList by lazy {
        getRoles()
    }

    fun changeRoles() {
        Log.d("BulkRoleChange", "Being called")
        bulkRoleChange(selectedRole.value?.hmsRole!!,
            getRoles().filter { it.name in _selectedRolesToChange })
        Log.d("BulkRoleChange", "Complete")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedRole.postValue(parent?.getItemAtPosition(position) as RolePresenter)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
        if(isChecked)
            _selectedRolesToChange.add(rolesList[which].name)
        else
            _selectedRolesToChange.remove(rolesList[which].name)
    }

    fun rolesToChangeSelected() {
        selectedRolesToChange.postValue(_selectedRolesToChange.joinToString(", "))
    }
}