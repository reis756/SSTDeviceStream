package com.sst.sstdevicestream.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.sst.sstdevicestream.R

private val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 */
class PermissionsFragment : Fragment() {

    private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) &&
            permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {
                navigateToCamera()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) &&
                    permissions.getOrDefault(Manifest.permission.CAMERA, false) -> {
                navigateToCamera()
            } else -> {
                Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            hasPermissions(requireContext()) -> {
                navigateToCamera()
            }
            else -> {
                requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
            }
        }
    }

    private fun navigateToCamera() {
        lifecycleScope.launchWhenStarted {
            findNavController().navigate(R.id.navigation_camera)
        }
    }

    companion object {

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}