package live.hms.roomkit.ui.diagnostic.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import live.hms.roomkit.R
import live.hms.roomkit.databinding.FragmentPreCallCameraBinding
import live.hms.roomkit.databinding.FragmentPreCallMicBinding
import live.hms.roomkit.databinding.FragmentPreCallRegionSelectionBinding
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModel
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.roomkit.ui.theme.buttonEnabled
import live.hms.roomkit.util.switchCamera
import live.hms.roomkit.util.viewLifecycle

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PreCallMicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreCallMicFragment : Fragment() {

    private var binding by viewLifecycle<FragmentPreCallMicBinding>()

    private val vm: DiagnosticViewModel by activityViewModels {
        DiagnosticViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreCallMicBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        binding.yesButton.setOnSingleClickListener {
            vm.stopCameraCheck()
            findNavController().navigate(PreCallMicFragmentDirections.actionPreCallMicFragmentToPreCallConnectivityTestFragment())
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.stopMicCheck()
                    findNavController().popBackStack()
                }
            })


        //check if camera permission is granted
        if (requireContext().checkCallingPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted

        } else {
            // Request permission
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                )
            )
        }

        binding.yesButton.buttonEnabled()


        binding.btnRecord.setOnSingleClickListener {
            if (vm.isRecording) {
                vm.stopMicCheck()
                binding.btnRecord.text = "Start recording"
                return@setOnSingleClickListener
            }
            vm.startMicRecording()
            binding.btnRecord.text = "Stop recording"
        }




    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {

        if (it.values.all { granted -> granted }) {}
        else {
            // Permission denied
            // Show a dialog explaining why the permission is needed
            // and how the user can grant the permission
            Toast.makeText(requireContext(), "Mic Permission denied", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }




}