package com.diatomicsoft.agoravideo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.diatomicsoft.agoravideo.databinding.FragmentVideoBinding
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas


class VideoFragment : Fragment() {

    var mRtcEngine: RtcEngine? = null
    private var mCallEnd = false
    private var mMuted = false
    private lateinit var binding: FragmentVideoBinding



    var agoraHelper: AgoraHelper? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            agoraHelper?.isJoined = true
            showMessage("Joined Channel $channel")
            Log.d(TAG, "onJoinChannelSuccess: ")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")
            Log.d(TAG, "onUserJoined: ")
            requireActivity().runOnUiThread { agoraHelper?.setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            requireActivity().runOnUiThread {
                agoraHelper?.remoteSurfaceView?.visibility = View.GONE
            }
            Log.d(TAG, "onUserOffline: ")
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUESTED_PERMISSIONS,
                PERMISSION_REQ_ID
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoBinding.inflate(layoutInflater, container, false)

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val defaultHeight = displayMetrics.heightPixels
        val defaultWidth = displayMetrics.widthPixels


        agoraHelper = AgoraHelper(requireContext(), binding)

        //init agora engine
        agoraHelper?.setupVideoSDKEngine(mRtcEventHandler)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    fun init() {
        agoraHelper = AgoraHelper(requireContext(),binding)
        agoraHelper?.setupVideoSDKEngine(mRtcEventHandler)
        mRtcEngine = agoraHelper?.agoraEngine
        initListeners()
    }

    private fun initListeners() {
        binding.btnMute.setOnClickListener {
            mMuted = !mMuted
            mRtcEngine?.muteLocalAudioStream(mMuted)
            val res = if (mMuted) R.drawable.btn_mute else R.drawable.btn_unmute
            binding.btnMute.setImageResource(res)
        }

        binding.btnSwitchCamera.setOnClickListener { mRtcEngine?.switchCamera() }

        binding.btnCall.setOnClickListener {
            if (mCallEnd) {
                startCall()
                mCallEnd = false
                binding.btnCall.setImageResource(R.drawable.btn_endcall)
            } else {
                endCall()
                mCallEnd = true
                binding.btnCall.setImageResource(R.drawable.btn_startcall)
            }

            showButtons(!mCallEnd)
        }

        binding.localVideoViewContainer.setOnClickListener {
        /*    switchView(mLocalVideo!!)
            switchView(mRemoteVideo!!)*/
        }
    }

    private fun startCall() {
        agoraHelper?.apply {
            setupLocalVideo()
            joinChannel()
        }

    }

    private fun endCall() {
        /* removeFromParent(mLocalVideo)
         mLocalVideo = null
         removeFromParent(mRemoteVideo)
         mRemoteVideo = null
         leaveChannel()*/
        agoraHelper?.apply {
            leaveChannel()
        }
    }

    private fun showButtons(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.btnMute.visibility = visibility
        binding.btnSwitchCamera.visibility = visibility
    }

    private fun removeFromParent(canvas: VideoCanvas?): ViewGroup? {
        if (canvas != null) {
            val parent = canvas.view.parent
            if (parent != null) {
                val group = parent as ViewGroup
                group.removeView(canvas.view)
                return group
            }
        }
        return null
    }

    private fun switchView(canvas: VideoCanvas) {
        val parent = removeFromParent(canvas)
        if (parent === binding.localVideoViewContainer) {
            if (canvas.view is SurfaceView) {
                (canvas.view as SurfaceView).setZOrderMediaOverlay(false)
            }
            binding.remoteVideoViewContainer.addView(canvas.view)
        } else if (parent === binding.remoteVideoViewContainer) {
            if (canvas.view is SurfaceView) {
                (canvas.view as SurfaceView).setZOrderMediaOverlay(true)
            }
            binding.localVideoViewContainer.addView(canvas.view)
        }
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            requireContext(),
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }






    override fun onDestroy() {
        super.onDestroy()
        agoraHelper?.agoraEngine?.stopPreview()
        agoraHelper?.agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraHelper?.agoraEngine = null
        }.start()

    }



    companion object {
        private const val TAG = "AgoraVideoCallFragment"
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS =
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

        @JvmStatic
        fun newInstance() = VideoFragment()
    }


    fun showMessage(message: String?) {
        requireActivity().runOnUiThread(Runnable {
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        })
    }

}