package at.overflow.flowy.WebRTC

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import at.overflow.flowy.Fragment.FragmentCamera.Companion.glTextureView
import at.overflow.flowy.Fragment.FragmentCamera.Companion.webSocketUtil
import at.overflow.flowy.Util.StringUtil
import at.overflow.flowy.Util.cameraLensMode
import com.neovisionaries.ws.client.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer


class RTCClient(
    private val ws: WebSocket,
    private val applicationContext: Context
) : PeerConnection.Observer{

    val TAG: String by lazy { "webRTCLog" }
    lateinit var peerConnection: PeerConnection
    lateinit var dataChannel: DataChannel
    lateinit var factory: PeerConnectionFactory
    private val iceServers by lazy {
        listOf(
//            IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            IceServer.builder("turn:st.flowy.kr:3478")
                .setUsername("test")
                .setPassword("test")
                .createIceServer()
        )
    }

    var eglBaseContext: EglBase.Context? = null

    init {

        /** peerConnectionFactory Init */
        initializePeerConnectionFactory()

        /** Local Peer Init */
        initializePeerConnections()

    }

    @SuppressLint("RestrictedApi")
    private fun initializePeerConnectionFactory() {
        Log.d(TAG, "initializePeerConnectionFactory")

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(applicationContext)
                .createInitializationOptions()
        )



        val options = PeerConnectionFactory.Options()
            .apply {
                disableNetworkMonitor = true
            }
        val defaultVideoEncoderFactory =
            DefaultVideoEncoderFactory(eglBaseContext, true, true)
        val defaultVideoDecoderFactory =
            DefaultVideoDecoderFactory(eglBaseContext)

        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()
    }

    private fun initializePeerConnections() {

        Log.d(TAG, "initializePeerConnections")

        /** Local peer Connection Create */
        peerConnection = createPeerConnection(factory = factory)

        setDataChannel()
    }

    private fun setDataChannel() {
        dataChannel = peerConnection.createDataChannel("sendDataChannel", DataChannel.Init())
        dataChannel.registerObserver(object : DataChannel.Observer {
            override fun onMessage(p0: DataChannel.Buffer?) {
                Log.d(TAG, "datachannel : onMessage")
            }

            override fun onBufferedAmountChange(p0: Long) {
                Log.d(TAG, "datachannel : onBufferedAmountChange + ${p0}")
            }

            override fun onStateChange() {
                Log.d(TAG, "datachannel : onStateChange : ${dataChannel.state().toString()}")
            }

        })
    }

    private fun createPeerConnection(factory: PeerConnectionFactory): PeerConnection {

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        val fac = factory.createPeerConnection(rtcConfig, this)
        Log.d(TAG,"rtcConfig $rtcConfig")
        Log.d(TAG,"facfg $fac")

        return factory.createPeerConnection(rtcConfig, this)!!
    }

    fun doCall(){
        val sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        sdpMediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        sdpMediaConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        peerConnection.createOffer(object :AppSdpObserver(){
            override fun onCreateFailure(p0: String?) {
                super.onCreateFailure(p0)
                Log.d("SSTSTS", "createOffer onCreateFailure : ${p0}")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setLocalDescription(AppSdpObserver(), p0)
                sendLocalSdp(p0!!.description)
                Log.d("SSTSTS", "제공자 콜 시도 및 sdp 정보 전달")
                Log.d("SSTSTS", "제공자 local 저장 : ${peerConnection.localDescription}")
            }
        }, sdpMediaConstraints)
    }

    fun doAnswer(description: String){
        Log.d("SSTSTS", "Answer 함수 들어옴 ")
        peerConnection.setRemoteDescription(AppSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, description))
        Log.d("SSTSTS", "제공자 remote을 저장 : ${peerConnection.remoteDescription}")
        val sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"))
        sdpMediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"))
        sdpMediaConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        peerConnection.createAnswer(object : AppSdpObserver(){
            override fun onCreateFailure(p0: String?) {
                super.onCreateFailure(p0)
                Log.d("SSTSTS", "createAnswer onCreateFailure : ${p0}")
            }

            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setLocalDescription(AppSdpObserver(), p0)
                sendLocalSdp(p0!!.description)
                Log.d("SSTSTS", "수신자 local 저장 : ${peerConnection.remoteDescription}")
            }
        }, sdpMediaConstraints)
    }

    fun sendLocalSdp(sdpJson: String) {
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 1)
        jsonSDP.put("task_id", "sendLocalSdp")
        jsonSDP.put("str_val", StringUtil().base64Encoding(sdpJson))
        ws.sendText(jsonSDP.toString())
    }

    fun sendIceCandidate(iceInfo : String) {
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 2)
        jsonSDP.put("str_val", StringUtil().base64Encoding(iceInfo))
        jsonSDP.put("task_id", "sendLocalIce")
        ws.sendText(jsonSDP.toString())
    }

    fun requestRemoteSdp() {
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 3)
        jsonSDP.put("task_id", "requestRemoteSdp")
        ws.sendText(jsonSDP.toString())
    }

    fun requestRemoteIce() {
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 4)
        jsonSDP.put("task_id", "requestRemoteIce")
        ws.sendText(jsonSDP.toString())
    }

    override fun onIceCandidate(p0: IceCandidate?) {

        val ob = JSONObject()
        ob.put("sdpMid", p0!!.sdpMid.toString())
        ob.put("sdpMLineIndex", p0.sdpMLineIndex.toString())
        ob.put("sdp", p0.sdp.toString())
        ob.put("serverUrl", p0.serverUrl.toString())
        ob.put("adapterType", p0.adapterType.toString())

        sendIceCandidate(ob.toString())
//        Log.d("SSTSTS", "나의 onIceCandidate ice 저장 : ${p0.toString()}")
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        dataChannel!!.registerObserver(object : DataChannel.Observer {
            override fun onMessage(p0: DataChannel.Buffer?) {
                Log.d(TAG, "onDataChannel : onMessage data : ${p0!!.data}")
            }

            override fun onBufferedAmountChange(p0: Long) {
                Log.d(TAG, "onDataChannel : onBufferedAmountChange : ${p0}")
            }

            override fun onStateChange() {
                Log.d(TAG, "onDataChannel : onStateChange")
            }

        })
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        super.onTrack(transceiver)
        Log.d(TAG, "createPeerConnection : onTrack: $transceiver")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.d(TAG, "createPeerConnection : onIceConnectionReceivingChange: $p0")
    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.d(TAG, "createPeerConnection : onIceConnectionChange: $p0")
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.d(TAG, "createPeerConnection : onIceGatheringChange: $p0")
    }

    override fun onAddStream(p0: MediaStream?) {
        Log.d(TAG, "createPeerConnection : onAddStream: $p0")
    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.d(TAG, "createPeerConnection : onSignalingChange: $p0")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.d(TAG, "createPeerConnection : onIceCandidatesRemoved: $p0")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.d(TAG, "createPeerConnection : onRemoveStream: $p0")
    }

    override fun onRenegotiationNeeded() {
        Log.d(TAG, "createPeerConnection : onRenegotiationNeeded: ")
    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.d(TAG, "createPeerConnection : onAddTrack: $p0 / ${p1}")
    }

}