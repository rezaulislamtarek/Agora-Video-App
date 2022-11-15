package com.diatomicsoft.agoravideo

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import com.diatomicsoft.agoravideo.databinding.FragmentVideoBinding
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas


class AgoraHelper(private val context: Context, private val binding: FragmentVideoBinding) {
    val TAG = "AgoraHelper"
    private val appId = "e1285fe24acd42c49563a2efb1d88114"
    private val channelName = "DT123"
    private val token =
        "007eJxTYLhXueHijH9LmEWUovP6tSo7JvgbKKaut7m/4daCa6sZ6k0UGFINjSxM01KNTBKTU0yMkk0sTc2ME41S05IMUywsDA1NVmcVJzcEMjJsk5zEysgAgSA+K4NLiKGRMQMDAGoCHrU="
    var agoraEngine: RtcEngine? = null
    var remoteSurfaceView: SurfaceView? = null
    var localSurfaceView: SurfaceView? = null
    private val uid = 0
    var isJoined = false
    lateinit var containerRemote: FrameLayout
    lateinit var containerLocal: FrameLayout

    fun setupVideoSDKEngine(mRtcEventHandler: IRtcEngineEventHandler) {
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine?.enableVideo()
        } catch (e: Exception) {
            Log.d(TAG, "setupVideoSDKEngine: $TAG")
        }
    }


    fun setupRemoteVideo(uid: Int) {
        containerRemote = binding.remoteVideoViewContainer
        remoteSurfaceView = SurfaceView(context)
        //remoteSurfaceView?.setZOrderMediaOverlay(true)
        ///////
        containerRemote.addView(remoteSurfaceView)


        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        // Display RemoteSurfaceView.
        remoteSurfaceView?.visibility = View.VISIBLE
    }


    fun setupLocalVideo(flag: Boolean? = false) {
        //val container: FrameLayout = findViewById(R.id.local_video_view_container)
        containerLocal = binding.localVideoViewContainer
        // Create a SurfaceView object and add it as a child to the FrameLayout.
        localSurfaceView = SurfaceView(context)
        localSurfaceView?.setZOrderMediaOverlay(true)
        containerLocal.addView(localSurfaceView)
        // Pass the SurfaceView object to Agora so that it renders the local video.

        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }


    fun joinChannel() {
        val options = ChannelMediaOptions()
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
        // Set the client role as BROADCASTER or AUDIENCE according to the scenario.
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // Display LocalSurfaceView.
        setupLocalVideo()
        localSurfaceView?.visibility = View.VISIBLE
        // Start local preview.
        agoraEngine?.startPreview()
        // Join the channel with a temp token.
        // You need to specify the user ID yourself, and ensure that it is unique in the channel.
        agoraEngine?.joinChannel(token, channelName, uid, options)

        //mute option
        agoraEngine?.adjustAudioMixingVolume(0)
        agoraEngine?.adjustPlaybackSignalVolume(0);
    }


    fun leaveChannel(): String {
        if (!isJoined) {
            return "Join a channel first"
        } else {
            agoraEngine!!.leaveChannel()
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
            return "You left the channel"
        }
    }

}





