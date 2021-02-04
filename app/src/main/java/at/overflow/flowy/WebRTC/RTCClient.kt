package at.overflow.flowy.WebRTC

import android.content.Context
import android.util.Base64
import android.util.Log
import com.neovisionaries.ws.client.WebSocket
import org.json.JSONObject
import org.webrtc.*
import java.nio.charset.Charset

class RTCClient(
    private val ws : WebSocket,
    private val applicationContext : Context
) {

    val TAG : String by lazy { "webRTCLog" }
    lateinit var peerConnection : PeerConnection
    lateinit var factory: PeerConnectionFactory

    val iceServers by lazy { listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()) }

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

    private fun initializePeerConnectionFactory(){
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
            .builder(applicationContext)
            .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
    }

    private fun initializePeerConnections(){

        /** Local peer Connection Create */
        peerConnection = createPeerConnection(factory = factory)

        connectToOtherPeer()

    }

    private fun createPeerConnection(factory: PeerConnectionFactory) : PeerConnection {

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        val pcObserver = object : PeerConnection.Observer{
            override fun onIceCandidate(p0: IceCandidate?) {
                Log.d(TAG, "createPeerConnection : onIceCandidate");
                peerConnection.addIceCandidate(p0);
                sendIceCandidateExchange(p0)
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d(TAG, "createPeerConnection : dataChannel");
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                Log.d(TAG, "createPeerConnection : onIceConnectionReceivingChange: $p0");
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "createPeerConnection : onIceConnectionChange: $p0");
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "createPeerConnection : onIceGatheringChange: $p0");
            }

            override fun onAddStream(p0: MediaStream?) {
                Log.d(TAG, "createPeerConnection : onAddStream: $p0");
            }

            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                Log.d(TAG, "createPeerConnection : onSignalingChange: $p0");
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                Log.d(TAG, "createPeerConnection : onIceCandidatesRemoved: $p0");
            }

            override fun onRemoveStream(p0: MediaStream?) {
                Log.d(TAG, "createPeerConnection : onRemoveStream: $p0");
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "createPeerConnection : onRenegotiationNeeded: ");
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                Log.d(TAG, "createPeerConnection : onAddTrack: $p0 / $p1");
            }
        }

        return factory.createPeerConnection(rtcConfig, pcObserver)!!
    }

    private fun connectToOtherPeer(){
        val sdpMediaConstraints = MediaConstraints()
        peerConnection.createOffer(object :AppSdpObserver(){

            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)

                peerConnection.setLocalDescription(AppSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, p0!!.description))
                try{
                    sendSdpExchange(p0)
                    Log.d(TAG, "connectToOtherPeer createOffer send / type = ${p0.type} ");
                }
                catch (e : Exception){
                    Log.d(TAG, "connectToOtherPeer createOffer error : ${e.message} ");
                }
            }

        },sdpMediaConstraints)

        peerConnection.createAnswer(object :AppSdpObserver(){
            override fun onCreateSuccess(p0: SessionDescription?) {
                super.onCreateSuccess(p0)
                peerConnection.setRemoteDescription(AppSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, p0!!.description))

                try{
                    sendSdpExchange(p0)
                    Log.d(TAG, "connectToOtherPeer createAnswer send ");
                }
                catch (e : Exception){
                    Log.d(TAG, "connectToOtherPeer createAnswer error : ${e.message} ");
                }
            }
        }, MediaConstraints())
    }


    fun sendSdpExchange(sdp : SessionDescription?){
        Log.d(TAG, "sendSdpExchange / type : ${sdp!!.type}");
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 1)
        jsonSDP.put("str_val", strToBase64(sdp.description))
        ws.sendText(jsonSDP.toString())
    }
    fun requestRemoteSdp(){
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 3)
        ws.sendText(jsonSDP.toString())
    }


    fun sendIceCandidateExchange(ice : IceCandidate?){
        Log.d(TAG, "sendIceCandidateExchange / iceSdp : ${ice!!.sdp}");
        val jsonSDP = JSONObject()
        jsonSDP.put("msg_type", 1)
        jsonSDP.put("msg_code", 1)
        jsonSDP.put("str_val", strToBase64(ice.sdp))
        ws.sendText(jsonSDP.toString())
    }

    private fun strToBase64(str : String): String? {
        val base64 = str.toByteArray(Charset.forName("UTF-8"))
        return Base64.encodeToString(base64, Base64.DEFAULT)
    }

}