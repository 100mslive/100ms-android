package live.hms.app2.ui.meeting.participants

import android.R
import android.content.Context
import android.util.AttributeSet
import android.widget.AdapterView
import android.widget.ArrayAdapter

class RoleSpinner @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : androidx.appcompat.widget.AppCompatSpinner(context, attrs) {
    private var actualRoles: List<String>? = null

    fun initAdapters(
        spinnerRoles: List<String>,
        givenPrompt: String,
        listener: AdapterView.OnItemSelectedListener
    ) {
        this.actualRoles = spinnerRoles

        ArrayAdapter(
            context,
            R.layout.simple_list_item_1,
            spinnerRoles // add an extra element at the end for cancel
        ).also { arrayAdapter ->
            adapter = arrayAdapter
            prompt = givenPrompt
            setSelection(
                spinnerRoles.size - 1,
                false
            ) // Set cancel selected by default. this also prevents it from being selected actually.
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            post { onItemSelectedListener = listener }
        }
    }


//    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        val stringRole = parent?.adapter?.getItem(position) as String
//        Log.d(TAG, "Selected role: $stringRole")
//        meetingViewModel.changeRole(args.remotePeerId, stringRole, isForce!!)
//        findNavController().popBackStack()
//    }
//
//    override fun onNothingSelected(parent: AdapterView<*>?) {
//        Log.d(TAG, "Nothing selected")
//        findNavController().popBackStack()
//    }
}