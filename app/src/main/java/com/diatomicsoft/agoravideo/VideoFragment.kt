package com.diatomicsoft.agoravideo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.diatomicsoft.agoravideo.databinding.FragmentVideoBinding
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine


class VideoFragment : Fragment() {
    val TAG = "VideoFragment"
    lateinit var agoraHelper: AgoraHelper
    private lateinit var binding: FragmentVideoBinding
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )


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

/*        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mainBinding.saveView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                frameHeight = mainBinding.image.measuredHeight
                frameWidth = mainBinding.image.measuredWidth
                if (frameHeight == 0 || frameHeight < 0) frameHeight = 500
                if (frameWidth == 0 || frameWidth < 0) frameWidth = 400
                defaultHeight = frameHeight
                defaultWidth = frameWidth
                if (intent.getStringExtra("quoteFromEdit") != null) {
                    val split = intent.getStringExtra("quoteFromEdit")!!
                        .split("^")
                    quoteFromEdit = if (split.size == 1) {
                        intent.getStringExtra("quoteFromEdit").toString()
                    } else "${split[0]}${split[1]}"
                    addTextSticker(quoteFromEdit)
                } else if (isFromDesign) {
                    if (template.heightRatio != 0.0) resizeWindow(
                        template.widthRatio,
                        template.heightRatio
                    )
                    if (template.fonts != null) {
                        for (font in template.fonts!!) addStickerFromDesign(
                            font,
                            frameHeight,
                            frameWidth
                        )
                    }
                } else addTextSticker("Double Tap to Edit")
            } *//*  public boolean onPreDraw() {

            }*//*
        })*/

        agoraHelper = AgoraHelper(requireContext(), binding)

        //init agora engine
        agoraHelper.setupVideoSDKEngine(mRtcEventHandler)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.JoinButton.setOnClickListener {
            if (checkSelfPermission()) {
                agoraHelper.joinChannel()
            } else {
                Toast.makeText(requireContext(), "Permission needed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.LeaveButton.setOnClickListener { agoraHelper.leaveChannel() }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Listen for the remote host joining the channel to get the uid of the host.
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")

            Log.d(TAG, "onUserJoined: ")

            // Set the remote video view
            requireActivity().runOnUiThread(Runnable { agoraHelper.setupRemoteVideo(uid) })
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            agoraHelper.isJoined = true
            showMessage("Joined Channel $channel")
            Log.d(TAG, "onJoinChannelSuccess: ")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            requireActivity().runOnUiThread(Runnable {
                agoraHelper.remoteSurfaceView!!.visibility = View.GONE
            })
            Log.d(TAG, "onUserOffline: ")
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

    fun showMessage(message: String?) {
        requireActivity().runOnUiThread(Runnable {
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraHelper.agoraEngine?.stopPreview()
        agoraHelper.agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraHelper.agoraEngine = null
        }.start()

    }

}