package com.sst.sstdevicestream.ui.srt

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.sst.sstdevicestream.R
import com.sst.sstdevicestream.databinding.FragmentSrtStreamBinding
import com.sst.sstdevicestream.ui.permission.PermissionsFragment
import io.github.thibaultbee.streampack.data.VideoConfig
import io.github.thibaultbee.streampack.error.StreamPackError
import io.github.thibaultbee.streampack.ext.srt.streamers.CameraSrtLiveStreamer
import io.github.thibaultbee.streampack.listeners.OnConnectionListener
import io.github.thibaultbee.streampack.listeners.OnErrorListener
import io.github.thibaultbee.streampack.streamers.StreamerLifeCycleObserver
import io.github.thibaultbee.streampack.utils.TAG
import kotlinx.coroutines.launch

class SrtStreamFragment : Fragment() {

    private var _binding: FragmentSrtStreamBinding? = null

    private val binding
        get() = _binding!!

    private val errorListener = object : OnErrorListener {
        override fun onError(error: StreamPackError) {
            toast("An error occurred: $error")
        }
    }

    private val connectionListener = object : OnConnectionListener {
        override fun onFailed(message: String) {
            toast("Connection failed: $message")
        }

        override fun onLost(message: String) {
            toast("Connection lost: $message")
        }

        override fun onSuccess() {
            toast("Connected")
        }
    }

    private val streamer by lazy {
        CameraSrtLiveStreamer(
            requireContext(),
            false,
            initialOnErrorListener = errorListener,
            initialOnConnectionListener = connectionListener
        )
    }

    private val streamerLifeCycleObserver by lazy { StreamerLifeCycleObserver(streamer) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSrtStreamBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.liveButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                lifecycleScope.launch {
                    try {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        streamer.startStream(
                            "srt://"+ binding.edit.text.toString() +":9000?streamid=StreamPack&passphrase="
                        )
                    } catch (e: Exception) {
                        binding.liveButton.isChecked = false
                        Log.e(TAG, "Failed to connect", e)
                    }
                }
            } else {
                streamer.stopStream()
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        val sharedPref = requireContext().getSharedPreferences("srt_stream", Context.MODE_PRIVATE)

        binding.edit.setText(sharedPref.getString("ip", "44.195.107.125"))

        binding.btnSave.setOnClickListener {
            with (sharedPref.edit()) {
                putString("ip", binding.edit.text.toString())
                apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            findNavController().navigate(R.id.navigation_permission)
        } else {
            inflateStreamer()
        }
    }

    private fun inflateStreamer() {
        lifecycle.addObserver(streamerLifeCycleObserver)
        configureStreamer()
        binding.preview.streamer = streamer
    }

    private fun configureStreamer() {
        val videoConfig = VideoConfig(
            mimeType = MediaFormat.MIMETYPE_VIDEO_HEVC, resolution = Size(1280, 720), fps = 20
        )
        streamer.configure(videoConfig)
    }

    private fun toast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}