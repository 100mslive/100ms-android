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
import live.hms.roomkit.gone
import live.hms.roomkit.setOnSingleClickListener
import live.hms.roomkit.show
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModel
import live.hms.roomkit.ui.diagnostic.DiagnosticViewModelFactory
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.buttonEnabled
import live.hms.prebuilt_themes.buttonStrokeEnabled
import live.hms.roomkit.util.switchCamera
import live.hms.roomkit.util.viewLifecycle


/**
 * A simple [Fragment] subclass.
 * Use the [PreCallCameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreCallCameraFragment : Fragment() {

    private var binding by viewLifecycle<FragmentPreCallCameraBinding>()

    private val vm: DiagnosticViewModel by activityViewModels {
        DiagnosticViewModelFactory(
            requireActivity().application
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreCallCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.applyTheme()
        binding.yesButton.setOnSingleClickListener {
            vm.stopCameraCheck()
            findNavController().navigate(PreCallCameraFragmentDirections.actionPreCallCameraFragmentToPreCallMicFragment())
        }

        binding.noButton.setOnSingleClickListener {
            vm.stopCameraCheck()
            findNavController().navigate(PreCallCameraFragmentDirections.actionPreCallCameraFragmentToPreCallMicFragment())
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.stopCameraCheck()
                    findNavController().popBackStack()
                }
            })

        binding.buttonSwitchCamera.gone()
        vm.cameraTrackLiveData.observe(viewLifecycleOwner) { cameraTrack ->
            cameraTrack?.let {
                binding.buttonSwitchCamera.show()
                binding.videoView.addTrack(it)
            }
        }

        binding.buttonSwitchCamera.setOnSingleClickListener {
            vm.cameraTrackLiveData.value?.switchCamera()
        }
        //check if camera permission is granted
        if (requireContext().checkCallingPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            vm.cameraPermssionGranted()
        } else {
            // Request permission
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                )
            )
        }

        binding.yesButton.buttonEnabled()
//        binding.noButton.buttonStrokeEnabled()


    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {

        if (it.values.all { granted -> granted }) vm.cameraPermssionGranted()
        else {
            // Permission denied
            // Show a dialog explaining why the permission is needed
            // and how the user can grant the permission
            Toast.makeText(requireContext(), "Camera Permission denied", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

}