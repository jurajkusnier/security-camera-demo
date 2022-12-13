package com.juraj.securitycamera

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoRenderer
import org.webrtc.VideoTrack
import java.net.URISyntaxException

open class WebRtcHelper(serverUrl: String) {

    val rootEglBase = EglBase.create()

    private var peerConnection: PeerConnection? = null

    private var isInitiator = false
    private var isChannelReady = false
    private var isStarted = false
    private val socket = IO.socket(serverUrl)

    private lateinit var factory: PeerConnectionFactory
    private var videoTrackFromCamera: VideoTrack? = null

    fun start(context: Context, view: SurfaceViewRenderer, isBroadcasting: Boolean) {
        connectToSignallingServer()
        initializePeerConnectionFactory(context)

        if (isBroadcasting)
            createVideoTrackFromCameraAndShowIt(context, view)

        initializePeerConnections(view, !isBroadcasting)
        startStreamingVideo(isBroadcasting)
    }

    private fun initializePeerConnections(view: SurfaceViewRenderer, receiveVideo: Boolean) {
        peerConnection = createPeerConnection(view, factory, receiveVideo)
    }

    private fun createPeerConnection(
        view: SurfaceViewRenderer, factory: PeerConnectionFactory,
        receiveVideo: Boolean
    ): PeerConnection {
        val iceServers = ArrayList<PeerConnection.IceServer>()
        val url = "stun:stun.l.google.com:19302"
        iceServers.add(PeerConnection.IceServer(url))
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        val pcConstraints = MediaConstraints()
        val pcObserver: PeerConnection.Observer = object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
                Log.d(TAG, "onSignalingChange: ")
            }

            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ")
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: ")
            }

            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                Log.d(TAG, "onIceCandidate: ")
                val message = JSONObject()
                try {
                    message.put("type", "candidate")
                    message.put("label", iceCandidate.sdpMLineIndex)
                    message.put("id", iceCandidate.sdpMid)
                    message.put("candidate", iceCandidate.sdp)
                    Log.d(TAG, "onIceCandidate: sending candidate $message")
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                Log.d(TAG, "onIceCandidatesRemoved: ")
            }

            override fun onAddStream(mediaStream: MediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size)
                if (receiveVideo) {
                    val remoteVideoTrack = mediaStream.videoTracks[0]
                    remoteVideoTrack.setEnabled(true)
                    remoteVideoTrack.addRenderer(VideoRenderer(view))
                }
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                Log.d(TAG, "onRemoveStream: ")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                Log.d(TAG, "onDataChannel: ")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ")
            }
        }
        return factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver)
    }

    private fun createVideoCapturer(context: Context): VideoCapturer? {
        return createCameraCapturer(Camera2Enumerator(context))
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun createVideoTrackFromCameraAndShowIt(context: Context, view: SurfaceViewRenderer) {
        val videoCapturer = createVideoCapturer(context)
        val videoSource = factory.createVideoSource(videoCapturer)
        videoCapturer?.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS)
        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        videoTrackFromCamera?.setEnabled(true)
        videoTrackFromCamera?.addRenderer(VideoRenderer(view))
    }

    private fun initializePeerConnectionFactory(context: Context) {
        PeerConnectionFactory.initializeAndroidGlobals(context, true, true, true)
        factory = PeerConnectionFactory(null).apply {
            setVideoHwAccelerationOptions(
                rootEglBase.eglBaseContext,
                rootEglBase.eglBaseContext
            )
        }
    }

    private fun startStreamingVideo(isBroadcasting: Boolean) {
        if (isBroadcasting) {
            val mediaStream = factory.createLocalMediaStream("ARDAMS")
            mediaStream.addTrack(videoTrackFromCamera)
            peerConnection?.addStream(mediaStream)
        }
        sendMessage("got user media")
    }

    private fun maybeStart() {
        Log.d(TAG, "maybeStart: $isStarted $isChannelReady")
        if (!isStarted && isChannelReady) {
            isStarted = true
            if (isInitiator) {
                doCall()
            }
        }
    }

    private fun doCall() {
        val sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
        )
        sdpMediaConstraints.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
        )
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "onCreateSuccess: ")
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("type", "offer")
                    message.put("sdp", sessionDescription.description)
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, sdpMediaConstraints)
    }

    private fun doAnswer() {
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("type", "answer")
                    message.put("sdp", sessionDescription.description)
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, MediaConstraints())
    }

    fun sendMessage(message: Any) {
        socket.emit("message", message)
    }

    private fun connectToSignallingServer() {
        try {
            socket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "connectToSignallingServer: connect")
                socket.emit("create or join", "foo")
            }.on("ipaddr") {
                Log.d(TAG, "connectToSignallingServer: ipaddr")
            }.on("created") {
                Log.d(TAG, "connectToSignallingServer: created")
                isInitiator = true
            }.on("full") {
                Log.d(TAG, "connectToSignallingServer: full")
            }.on("join") {
                Log.d(TAG, "connectToSignallingServer: join")
                isChannelReady = true
            }.on("joined") {
                Log.d(TAG, "connectToSignallingServer: joined")
                isChannelReady = true
            }.on("log") { args ->
                for (arg in args) {
                    Log.d(TAG, "connectToSignallingServer: $arg")
                }
            }.on("message") {
                Log.d(TAG, "connectToSignallingServer: got a message")
            }.on("message") { args ->
                try {
                    if (args[0] is String) {
                        val message = args[0] as String
                        if (message == "got user media") {
                            maybeStart()
                        }
                    } else {
                        val message = args[0] as JSONObject
                        Log.d(TAG, "connectToSignallingServer: got message $message")
                        if (message.getString("type") == "offer") {
                            Log.d(
                                TAG,
                                "connectToSignallingServer: received an offer $isInitiator $isStarted"
                            )
                            if (!isInitiator && !isStarted) {
                                maybeStart()
                            }
                            peerConnection?.setRemoteDescription(
                                SimpleSdpObserver(),
                                SessionDescription(
                                    SessionDescription.Type.OFFER,
                                    message.getString("sdp")
                                )
                            )
                            doAnswer()
                        } else if (message.getString("type") == "answer" && isStarted) {
                            peerConnection?.setRemoteDescription(
                                SimpleSdpObserver(),
                                SessionDescription(
                                    SessionDescription.Type.ANSWER,
                                    message.getString("sdp")
                                )
                            )
                        } else if (message.getString("type") == "candidate" && isStarted) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates")
                            val candidate = IceCandidate(
                                message.getString("id"),
                                message.getInt("label"),
                                message.getString("candidate")
                            )
                            peerConnection?.addIceCandidate(candidate)
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "connectToSignallingServer: disconnect")
            }
            socket.connect()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "Helper"
        private const val VIDEO_TRACK_ID = "ARDAMSv0"
        private const val VIDEO_RESOLUTION_WIDTH = 720
        private const val VIDEO_RESOLUTION_HEIGHT = 1280
        private const val FPS = 30
    }
}