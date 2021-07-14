package live.hms.app2.ui.home.permission

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import live.hms.app2.R
import live.hms.app2.databinding.FragmentPermissionBinding
import live.hms.app2.util.viewLifecycle
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class PermissionFragment : Fragment(), EasyPermissions.PermissionCallbacks {

  private var binding by viewLifecycle<FragmentPermissionBinding>()

  companion object {
    private const val TAG = "PermissionFragment"

    private const val RC_CALL = 111
    private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkAlreadyGrantedPermissions()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPermissionBinding.inflate(inflater, container, false)
    initButtons()
    return binding.root
  }

  private fun checkAlreadyGrantedPermissions() {
    if (EasyPermissions.hasPermissions(requireContext(), *PERMISSIONS)) {
      findNavController().navigate(
        PermissionFragmentDirections.actionPermissionFragmentToHomeFragment()
      )
    }
  }

  private fun initButtons() {
    binding.buttonGrantPermission.setOnClickListener { gotoHomePage() }

    binding.buttonDoItLater.setOnClickListener {
      // TODO: Integrate no-camera permission flow
    }
  }

  @AfterPermissionGranted(RC_CALL)
  private fun gotoHomePage() {
    if (EasyPermissions.hasPermissions(requireContext(), *PERMISSIONS)) {
      Log.v(TAG, "Permission=$PERMISSIONS granted, moving to HomeFragment")
      findNavController().navigate(
        PermissionFragmentDirections.actionPermissionFragmentToHomeFragment()
      )
    } else {
      EasyPermissions.requestPermissions(
        this,
        resources.getString(R.string.permission_description),
        RC_CALL,
        *PERMISSIONS
      )
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
  }

  // Permission Callbacks
  override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    Log.v(TAG, "onPermissionsGranted($requestCode, $perms)")
  }

  override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    Log.v(TAG, "onPermissionsDenied($requestCode, $perms)")
  }
}
