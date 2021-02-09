package at.overflow.flowy.Util

import android.content.Context
import android.util.Base64
import android.util.Log
import android.widget.Toast
import at.overflow.flowy.Fragment.FragmentCamera
import at.overflow.flowy.WebRTC.AppSdpObserver
import at.overflow.flowy.WebRTC.RTCClient
import com.neovisionaries.ws.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.nio.charset.Charset

class WebSocketUtil(
    val context : Context
){

    lateinit var ws : WebSocket
    lateinit var rtcClient: RTCClient

    val TAG : String by lazy { "webSocketLog" }
    val userIsCP : Boolean by lazy { true }

    init {

        /** 웹 소켓 셋팅 및 연결
         * -- 웹 소켓은 Main Thread 에서 연결을 허용 하지 않기에 back ground 환경에서 작업한다.
         * */
        Thread(kotlinx.coroutines.Runnable {

            try{
                val factory = WebSocketFactory().apply {
                    connectionTimeout = 5000
                }

                ws = factory.createSocket(OVERFLOW_WEB_SOCKET_URL)

                setWebSocketListener()


                /**
                 * 서비스 ID = [ CP - 서버 - CC(여러대) ] 간에 사용할 고유 서비스 ID다
                 * - 우리 회사 상품으로 기본 최대 5연결 까지 지원 하기로 하였는데, 1연결당 1개의 서비스 ID가 부여된다
                 *
                 * 사용자 토큰 및 유형
                 * 유형 1 - CP 컴퓨터
                 * 유형 2 - CC 컴퓨터
                 * */

                // 헤더 추가
                ws.addHeader("X-Flowy-Service-ID", "112233445566") // 서비스 ID

                if (userIsCP){
                    ws.addHeader("X-Flowy-User-Token", "t100tokenxxxx") // 사용자 토큰
                    ws.addHeader("X-Flowy-Client-Type", "1") // 유형 1 - 제공자, 유형 2 - 소비자
                }
                else{
                    ws.addHeader("X-Flowy-User-Token", "t200tokenyyyy") // 사용자 토큰
                    ws.addHeader("X-Flowy-Client-Type", "2") // 유형 1 - 제공자, 유형 2 - 소비자
                }

                ws.isMissingCloseFrameAllowed = true

                // 웹 소켓 연결시작
                ws.connect()
            }
            catch ( e: Exception){
                Log.d(TAG,"webSocket Connection Error : $e")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Socket Connect Fail",Toast.LENGTH_SHORT).show()
                }
            }

        }).start()
    }

    private fun setWebSocketListener(){

        /** with는 Non-Nullable 객체이여야하고, 결과가 필요하지 않을때 사용 */
        with(ws){
            this.addListener(object:WebSocketAdapter() {

                override fun handleCallbackError(websocket: WebSocket?, cause: Throwable?) {
                    super.handleCallbackError(websocket, cause)
                    Log.d(TAG,"handleCallbackError : $cause")
                }

                override fun onBinaryFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onBinaryFrame(websocket, frame)
                    Log.d(TAG,"onBinaryFrame : 바이너리 프레임 수신 : $frame")
                }

                override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
                    super.onBinaryMessage(websocket, binary)
                    Log.d(TAG,"onBinaryMessage : 바이너리 메세지 수신 : $binary")
                }

                override fun onCloseFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onCloseFrame(websocket, frame)
                    Log.d(TAG,"onCloseFrame : 닫기 프레임 수신 : $frame")
                }

                override fun onConnected(
                    websocket: WebSocket?,
                    headers: MutableMap<String, MutableList<String>>?
                ) {
                    super.onConnected(websocket, headers)
                    Log.d(TAG,"onConnected  오프닝 핸드 셰이크 성공 후 호출됨 ${headers.toString()}")

                    rtcClient = RTCClient(ws = FragmentCamera.webSocketUtil!!.ws, applicationContext = context)

                    rtcClient.requestRemoteSdp()
                    rtcClient.requestRemoteIce()
                    if (userIsCP){
                        rtcClient.doCall()
                    }
                }

                override fun onTextMessage(websocket: WebSocket?, text: String?) {
                    super.onTextMessage(websocket, text)
                    Log.d(TAG,"onTextMessage : 문자 데이터가 수신될때 호출 : $text")
                    Log.d("SSTSTS","onTextMessage : 문자 데이터가 수신될때 호출 : $text")

                    val responseData = messageModelParser(text.toString())

                    val msg_type = responseData["msg_type"]
                    val msg_code = responseData["msg_code"]
                    val task_id = responseData["task_id"]
                    var str_val : String = ""
                    var str_array : JSONArray = JSONArray()

                    try{
                        str_val = base64Decoding(responseData["str_val"])
                        Log.d("SSTSTS", "str_val 파싱 : ${str_val}")
                    }catch (e : Exception){}

                    try{
                        str_array = JSONArray(responseData["str_array"])
                        Log.d("SSTSTS", "str_val str_array : ${str_array}")

                    } catch (e : Exception){}

                    when(msg_type){
                        /** 제어/ 작업 요청 */
                        "3"->{
                            when(msg_code){
                                /** 0 : 성공, 1 : 서비스를 찾을 수 없음, 2 : 잘못된 사용자 토큰, 3 : 내부 오류 */
                                "0"->{

                                    if (task_id == "requestRemoteSdp" && str_val != ""){
                                        if (userIsCP){
                                            rtcClient.peerConnection.setRemoteDescription(AppSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, base64Decoding(str_val)))
                                            Log.d("SSTSTS", "최종 수신자 remote을 저장 : ${rtcClient.peerConnection.remoteDescription}")
                                            Log.d("SSTSTS", "SDP 교환 완료")
                                        }
                                        else{
                                            Log.d("SSTSTS", "제어 / 작업 요청 Answer 시작 ")
                                            rtcClient.doAnswer(str_val)
                                        }
                                    }
                                    else if (task_id == "requestRemoteIce"){
                                        for ( i in 0 until str_array.length()){
                                            val iceInfo = base64Decoding(str_array[i].toString())

                                            val jsonObject = JSONObject(iceInfo)

                                            val iceMid = jsonObject.optString("sdpMid")
                                            val iceIndex = jsonObject.optString("sdpMLineIndex").toInt()
                                            val iceSdp = jsonObject.optString("sdp")
                                            val serverUrl = jsonObject.optString("serverUrl")
                                            val adapterType = jsonObject.optString("adapterType")

                                            Log.d("SSTSTS", "제어 요청 작업 ICE 정보 : " +
                                                    "mid : ${iceMid}" + "\n" +
                                                    "iceIndex : ${iceIndex}" + "\n" +
                                                    "iceSdp : ${iceSdp}" + "\n" +
                                                    "serverUrl : ${serverUrl}" + "\n" +
                                                    "adapterType : ${adapterType}" + "\n"
                                            )
                                            var iceInstance : IceCandidate = IceCandidate(iceMid, iceIndex, iceSdp)
                                            Log.d("SSTSTS", "제어 요청 작업 ICE 정보 : $iceInstance")
                                            rtcClient.peerConnection.addIceCandidate(iceInstance)
                                        }

                                        CoroutineScope(Dispatchers.Default).launch {
                                            var i = 0
                                            while (i < 100){
                                                Log.d("SSTSTS", "ICE Connect 상태 : ${rtcClient.peerConnection.iceConnectionState()}")
                                                Log.d("SSTSTS", "ICE Gather 상태 : ${rtcClient.peerConnection.iceGatheringState()}")
                                                delay(1000)
                                                i++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        /** 서버 알림 */
                        "2"->{
                            when(msg_code){
                                /** 1 : 상대방 SDP 수신됨 , 2 : 상대방 ICE candidate 수신됨 */
                                "1"->{
                                    if (userIsCP){
                                        rtcClient.peerConnection.setRemoteDescription(AppSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, str_val))
                                        Log.d("SSTSTS", "수신자 remote을 저장 : ${rtcClient.peerConnection.remoteDescription}")
                                        Log.d("SSTSTS", "SDP 교환 완료")
                                    }
                                    else{
                                        Log.d("SSTSTS", "알림 Answer 시작 ")
                                        rtcClient.doAnswer(str_val)
                                    }
                                }
                                "2"->{

                                    val jsonObject = JSONObject(str_val)

                                    val iceMid = jsonObject.optString("sdpMid")
                                    val iceIndex = jsonObject.optString("sdpMLineIndex").toInt()
                                    val iceSdp = jsonObject.optString("sdp")
                                    val serverUrl = jsonObject.optString("serverUrl")
                                    val adapterType = jsonObject.optString("adapterType")

                                    Log.d("SSTSTS", "알림 ICE 정보 : " +
                                            "mid : ${iceMid}" + "\n" +
                                            "iceIndex : ${iceIndex}" + "\n" +
                                            "iceSdp : ${iceSdp}" + "\n" +
                                            "serverUrl : ${serverUrl}" + "\n" +
                                            "adapterType : ${adapterType}" + "\n"
                                    )
                                    var iceInstance : IceCandidate = IceCandidate(iceMid, iceIndex, iceSdp)
                                    Log.d("SSTSTS", "제어 요청 작업 ICE 정보 : $iceInstance")
                                    rtcClient.peerConnection.addIceCandidate(iceInstance)

                                    CoroutineScope(Dispatchers.Default).launch {
                                        var i = 0
                                        while (i < 10){
                                            Log.d("SSTSTS", "ICE Connect 상태 : ${rtcClient.peerConnection.iceConnectionState()}")
                                            Log.d("SSTSTS", "ICE Gather 상태 : ${rtcClient.peerConnection.iceGatheringState()}")
                                            delay(1000)
                                            i++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
                    super.onConnectError(websocket, exception)
//                    Log.d(TAG,"onConnectError : connectAsynchronously() 실패시 호출됨")
                }

                override fun onContinuationFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onContinuationFrame(websocket, frame)
//                    Log.d(TAG,"onContinuationFrame : 연속 프레임이 수신 될때 호출됨")
                }

                override fun onDisconnected(
                    websocket: WebSocket?,
                    serverCloseFrame: WebSocketFrame?,
                    clientCloseFrame: WebSocketFrame?,
                    closedByServer: Boolean
                ) {
                    super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
                    Log.d(TAG,"onDisconnected : webSocket 연결이 닫힌 후 호출")
                }

                override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                    super.onError(websocket, cause)
                    Log.d(TAG,"onError : 오류 발생시 호출")
                }

                override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onFrame(websocket, frame)
//                    Log.d(TAG,"onFrame : 프레임 수신시 호출")
                }

                override fun onFrameError(
                    websocket: WebSocket?,
                    cause: WebSocketException?,
                    frame: WebSocketFrame?
                ) {
                    super.onFrameError(websocket, cause, frame)
//                    Log.d(TAG,"onFrameError : 프레임 읽기 실패시 호출")
                }

                override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onFrameSent(websocket, frame)
//                    Log.d(TAG,"onFrameSent : 프레임 전송시 호출")
                }

                override fun onFrameUnsent(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onFrameUnsent(websocket, frame)
//                    Log.d(TAG,"onFrameUnsent : 프레임 전송이 안되었을때 호출")
                }

                override fun onMessageDecompressionError(
                    websocket: WebSocket?,
                    cause: WebSocketException?,
                    compressed: ByteArray?
                ) {
                    super.onMessageDecompressionError(websocket, cause, compressed)
//                    Log.d(TAG,"onMessageDecompressionError : 메시지 압축 해제에 실패했을때 호출")
                }

                override fun onMessageError(
                    websocket: WebSocket?,
                    cause: WebSocketException?,
                    frames: MutableList<WebSocketFrame>?
                ) {
                    super.onMessageError(websocket, cause, frames)
//                    Log.d(TAG,"onMessageError : 메시지 생성에 실패했을때 호출")
                }

                override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onPingFrame(websocket, frame)
//                    Log.d(TAG,"onPingFrame : 핑 프레임 수신 될때 호출")
                }

                override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onPongFrame(websocket, frame)
//                    Log.d(TAG,"onPongFrame : 퐁 프레임이 수신 될때 호출 호출")
                }

                override fun onSendError(
                    websocket: WebSocket?,
                    cause: WebSocketException?,
                    frame: WebSocketFrame?
                ) {
                    super.onSendError(websocket, cause, frame)
//                    Log.d(TAG,"onSendError : 프레임 전송 중 오류 발생하면 호출")
                }

                override fun onSendingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onSendingFrame(websocket, frame)
//                    Log.d(TAG,"onSendingFrame : 프레임 전송되기 전에 호출")
                }

                override fun onSendingHandshake(
                    websocket: WebSocket?,
                    requestLine: String?,
                    headers: MutableList<Array<String>>?
                ) {
                    super.onSendingHandshake(websocket, requestLine, headers)
                    Log.d(TAG,"onSendingHandshake : 오프닝 핸드 셰이크를 보내기전에 호출 header : ${headers.toString()}")
                }

                override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {
                    super.onStateChanged(websocket, newState)
                    Log.d(TAG,"onStateChanged : WebSocket 상태 변경시 호출")
                }

                override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                    super.onTextFrame(websocket, frame)
//                    Log.d(TAG,"onTextFrame : 텍스트 프레임이 수신될때 호출")
                }

                override fun onTextMessage(websocket: WebSocket?, data: ByteArray?) {
                    super.onTextMessage(websocket, data)
//                    Log.d(TAG,"onTextFrame : 바이트 배열 데이터가 수신될때 호출 $data")
                }

                override fun onTextMessageError(
                    websocket: WebSocket?,
                    cause: WebSocketException?,
                    data: ByteArray?
                ) {
                    super.onTextMessageError(websocket, cause, data)
//                    Log.d(TAG,"onTextMessageError : ${cause!!.message}")
                }

                override fun onThreadCreated(
                    websocket: WebSocket?,
                    threadType: ThreadType?,
                    thread: Thread?
                ) {
                    super.onThreadCreated(websocket, threadType, thread)
                }

                override fun onThreadStarted(
                    websocket: WebSocket?,
                    threadType: ThreadType?,
                    thread: Thread?
                ) {
                    super.onThreadStarted(websocket, threadType, thread)
//                    Log.d(TAG,"onThreadStarted : 스레드의 run()메서드가 시작될때 호출")
                }

                override fun onThreadStopping(
                    websocket: WebSocket?,
                    threadType: ThreadType?,
                    thread: Thread?
                ) {
                    super.onThreadStopping(websocket, threadType, thread)
//                    Log.d(TAG,"onThreadStopping : 스레드의 run()메서드의 끝에서 호출")
                }

                override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException?) {
                    super.onUnexpectedError(websocket, cause)
                    Log.d(TAG,"onUnexpectedError : ?? 무슨예외 : $cause")
                }
            })
        }
    }

    fun messageModelParser(json : String): HashMap<String, String> {

        val jsonObject = JSONObject(json)

        val responseHashMap = HashMap<String, String>()

        responseHashMap["msg_type"] = jsonObject.optString("msg_type")
        responseHashMap["msg_code"] = jsonObject.optString("msg_code")
        responseHashMap["task_id"] = jsonObject.optString("task_id")
        responseHashMap["str_val"] = jsonObject.optString("str_val")
        responseHashMap["str_array"] = jsonObject.optString("str_array")
        responseHashMap["int_val"] = jsonObject.optString("int_val")
        responseHashMap["int_array"] = jsonObject.optString("int_array")
        responseHashMap["bool_val"] = jsonObject.optString("bool_val")
        responseHashMap["bool_array"] = jsonObject.optString("bool_array")
        responseHashMap["object_val"] = jsonObject.optString("object_val")

        return responseHashMap
    }

    fun base64Decoding(str: String?): String {
        return String(Base64.decode(str, Base64.DEFAULT), Charset.forName("UTF-8"))
    }

}