package at.overflow.flowy.WebRTC

import android.util.Log
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * WebRTC 프로토콜을 사용하기 위한 과정 중
 * SDP 정보를 교환하기 위해서 필요한 class
 * */

open class AppSdpObserver : SdpObserver {

    val TAG : String by lazy { "AppSdpObserver" }

    override fun onSetFailure(p0: String?) {
        Log.d(TAG, "onSetFailure: $p0")
    }

    override fun onSetSuccess() {
        Log.d(TAG, "onSetSuccess")
    }

    override fun onCreateSuccess(p0: SessionDescription?) {
        Log.d(TAG, "onCreateSuccess: $p0")
    }

    override fun onCreateFailure(p0: String?) {
        Log.d(TAG, "onCreateFailure: $p0")
    }
}