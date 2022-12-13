package com.juraj.securitycamera

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

internal open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(s: String) {}
    override fun onSetFailure(s: String) {}
}