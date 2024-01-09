package com.sst.sstdevicestream.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sst.sstdevicestream.R
import com.sst.sstdevicestream.databinding.FragmentHomeBinding
import com.sst.sstdevicestream.model.DeviceData
import com.sst.sstdevicestream.ui.framebyframe.CameraFragment
import com.sst.sstdevicestream.ui.framebyframe.CameraViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModel()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()

        setupObservers()

        savePrefs()

        viewModel.activeStreaming(true)

        viewModel.setupRabbitMq()

        checkInternetConnectionSpeed(requireContext())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val timer = Timer()

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    findNavController().navigate(R.id.navigation_permission)
                }
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    viewModel.deviceData = DeviceData(
                        viewModel.userUuid,
                        "Android Phone",
                        location?.latitude ?: 0.0,
                        location?.longitude ?: 0.0
                    )

                    viewModel.publishMessage(
                        viewModel.rabbitMqQueueStream, "message".toByteArray(), viewModel.deviceData
                    )

                    viewModel.producerKafka()
                }
            }

        }, 0, 1000L)
    }

    private fun savePrefs() {
        val sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPreferences?.getString(CameraFragment.USER_ID, "")
        if (userId.isNullOrEmpty()) {
            val uuid = UUID.randomUUID().toString()
            val prefEdit = sharedPreferences?.edit()
            prefEdit?.putString(CameraFragment.USER_ID, UUID.randomUUID().toString())
            prefEdit?.apply()

            viewModel.setUserId(uuid)
        } else {
            viewModel.setUserId(userId)
        }
    }

    private fun setupViews() {
        binding.btnSrtStream.setOnClickListener {
            findNavController().navigate(R.id.navigation_srt_stream)
        }

        binding.btnFrameByFrame.setOnClickListener {
            findNavController().navigate(R.id.navigation_camera)
        }
    }

    private fun setupObservers() {
        viewModel.error.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
        }

        viewModel.info.observe(viewLifecycleOwner) {
            //fragmentCameraBinding.info.text = it
        }
    }

    private fun checkInternetConnectionSpeed(context: Context) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities =
            connectivityManager.getNetworkCapabilities(network)

        binding.txtConnectionSpeed.text = if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    String.format(
                        getString(R.string.connection_speed),
                        capabilities.linkUpstreamBandwidthKbps
                    )
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    String.format(
                        getString(R.string.connection_speed),
                        capabilities.linkUpstreamBandwidthKbps
                    )
                }

                else -> {
                    getString(R.string.unidentified_connection)
                }
            }
        } else {
            getString(R.string.disconnected)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}