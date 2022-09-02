package live.hms.app2.ui.meeting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import live.hms.app2.databinding.SettingsBottomSheetDialogBinding
import live.hms.app2.util.setOnSingleClickListener
import live.hms.app2.util.viewLifecycle


class SettingsBottomSheet(
    private val meetingViewModel: MeetingViewModel,
) : BottomSheetDialogFragment() {

    private var binding by viewLifecycle<SettingsBottomSheetDialogBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsBottomSheetDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backBtn.setOnClickListener {
            dismiss()
        }

        binding.btnBrb.apply {

            if (meetingViewModel.isBRBOn()){
                binding.tvBrbStatus.text = "Disable BRB"
            } else{
                binding.tvBrbStatus.text = "Enable BRB"
            }

            setOnSingleClickListener(350) {
                meetingViewModel.toggleBRB()
                dismiss()
            }
        }

        binding.btnChangeName.apply {
            setOnSingleClickListener(350) {
                ChangeNameDialogFragment().show(
                    childFragmentManager,
                    ChangeNameDialogFragment.TAG
                )
            }
        }

        binding.btnCameraSwitch.apply {
            setOnSingleClickListener(350) {
                meetingViewModel.flipCamera()
                dismiss()
            }
        }

        binding.btnShowStats.apply {
            if (meetingViewModel.statsToggleLiveData.value == true){
                binding.tvStats.text = "Hide Stats"
            }else{
                binding.tvStats.text = "Show Stats"
            }
            setOnSingleClickListener(350) {

                meetingViewModel.statsToggleData.postValue(meetingViewModel.statsToggleLiveData.value?.not())
                dismiss()
            }
        }

    }
}