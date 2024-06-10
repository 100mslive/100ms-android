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
import live.hms.roomkit.ui.theme.buttonStrokeEnabled
import live.hms.roomkit.util.switchCamera
import live.hms.roomkit.util.viewLifecycle

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PreCallCameraFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PreCallCameraFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


    private var binding by viewLifecycle<FragmentPreCallCameraBinding>()

    private val vm: DiagnosticViewModel by activityViewModels {
        DiagnosticViewModelFactory(
            requireActivity().application
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    vm.stopCameraCheck()
                    findNavController().popBackStack()
                }
            })

        vm.cameraTrackLiveData.observe(viewLifecycleOwner) { cameraTrack ->
            cameraTrack?.let {
                binding.videoView.addTrack(it)
            }
        }

        binding.videoView.setOnSingleClickListener {
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PreCallCameraFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) = PreCallCameraFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
            }
        }
    }
}