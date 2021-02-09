package at.overflow.flowy.WebRTC

import android.content.Context
import android.util.Base64
import android.util.Log
import com.bumptech.glide.load.resource.bitmap.VideoDecoder.byteBuffer
import com.neovisionaries.ws.client.WebSocket
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


class RTCClient(
    private val ws: WebSocket,
    private val applicationContext: Context
) : PeerConnection.Observer {

    val TAG: String by lazy { "webRTCLog" }
    lateinit var peerConnection: PeerConnection
    lateinit var dataChannel: DataChannel
    lateinit var factory: PeerConnectionFactory
    private val iceServers by lazy {
        listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
    }

    init {

        /**
         *  WebSocket - WebRTC 를 사용한 1 : 1 통신 과정
         *
         * [ Web Socket 연결 ]
         * 1. WebSocket 을 사용하여 URL 을 Signalling Server 로 지정한다.
         *    ( Signalling Server 는 단지 WebRTC 통신을 위한 정보 교환을 하기 위함이다. )
         * 2. WebSocket 을 Signalling Server 에 연결하기 전에 Header 데이터를 추가한다.
         * 3. webSocket 을 연결한다.
         *
         * [ WebRTC 초기화 ]
         * 4. A,B 클라이언트에서는 WebRTC를 사용하기 위해 PeerConnectionFactory 를 초기화 한다.
         *    ( PeerConnectionFactory 초기화는 WebRTC 를 사용하기 위한 기본 설정이다. )
         * 5. 초기화가 완료가 되었다면, A,B 클라이언트 에서는 Peer Connection 을 생성한다.
         *    ( 이제 A, B 클라이언트 들은 Signalling Server 통해 WebRTC 정보 ( SDP, Ice Candidate 등 ) 를 교환 할 수 있다. )
         *
         * [ SDP 정보 교환 ]
         * 6. A 클라이언트에서 SDP 정보를 Signalling Server 로 보내고, SDP 정보를 Local Description 에 저장한다.
         * 7. B 클라이언트에서는 WebSocket 으로 부터 전달받은 A 클라이언트의 SDP 정보를 Remote Description 에 저장한다.
         * 8. B 클라이언트가 Remote Description 을 저장했으면, B 클라이언트에서는 A 클라이언트에게 SDP 정보를 전달해주고, SDP 정보를 Local Description 에 저장한다.
         * 9. A 클라이언트에서 B 클라이언트의 SDP 정보를 수신 받게되면, A 클라이언트의 Remote Description 에 B 클라이언트의 SDP 정보를 저장하는 것으로 SDP 교환이 끝이난다.
         *
         * [ Ice Candidate 정보 교환 ]
         *
         * */

        /** peerConnectionFactory Init */
        initializePeerConnectionFactory()

        /** Local Peer Init */
        initializePeerConnections()
    }

    private fun initializePeerConnectionFactory() {
        Log.d(TAG, "initializePeerConnectionFactory")

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(applicationContext)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    private fun initializePeerConnections() {

        Log.d(TAG, "initializePeerConnections")

        /** Local peer Connection Create */
        peerConnection = createPeerConnection(factory = factory)

        setDataChannel()

//        connectToOtherPeer()
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
        return factory.createPeerConnection(rtcConfig, this)!!
    }

    fun doCall(){
        val sdpMediaConstraints = MediaConstraints()
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
        jsonSDP.put("str_val", strToBase64(sdpJson))
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

    fun sendIceCandidate(iceInfo : String) {
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 2)
        jsonSDP.put("str_val", strToBase64(iceInfo))
        jsonSDP.put("task_id", "sendLocalIce")
        ws.sendText(jsonSDP.toString())
    }

    fun strToBase64(str: String): String? {
        val base64 = str.toByteArray(Charset.forName("UTF-8"))
        return Base64.encodeToString(base64, Base64.DEFAULT)
    }

    override fun onIceCandidate(p0: IceCandidate?) {

        val ob = JSONObject()
        ob.put("sdpMid", p0!!.sdpMid.toString())
        ob.put("sdpMLineIndex", p0.sdpMLineIndex.toString())
        ob.put("sdp", p0.sdp.toString())
        ob.put("serverUrl", p0.serverUrl.toString())
        ob.put("adapterType", p0.adapterType.toString())

        sendIceCandidate(ob.toString())
        Log.d("SSTSTS", "나의 onIceCandidate ice 저장 : ${p0.toString()}")
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
        Log.d(TAG, "createPeerConnection : onAddTrack: $p0 / $p1")
    }

}