package at.overflow.flowy.Util

import android.content.Context
import android.util.Log
import at.overflow.flowy.Fragment.FragmentCamera
import at.overflow.flowy.WebRTC.AppSdpObserver
import at.overflow.flowy.WebRTC.RTCClient
import com.neovisionaries.ws.client.*
import org.json.JSONObject
import org.webrtc.SessionDescription

class WebSocketUtil(
    val applicationContext : Context
){

    lateinit var ws : WebSocket

    val TAG : String by lazy { "webSocketLog" }
    val userIsCP : Boolean by lazy { false }

    init {

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
            }


        }).start()
    }

    private fun setWebSocketListener(){
        ws.addListener(object:WebSocketAdapter() {

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

                rtcClient = RTCClient(ws = FragmentCamera.webSocketUtil.ws, applicationContext = applicationContext)

                /**
                 * CC 인 경우에는 웹 소켓 연결 후에 제공 받을 수 있는 원격 SDP가 있는지 확인하는 과정을 거쳐야한다.
                 * CP가 먼저 연결을 하고 SDP 정보를 서버에 제공을 하였다면, 늦게 소켓 연결을 한 CC 입장에서는 SDP 정보를 받을 수 없기 때문이다.
                 * 그러기에 원격 SDP 정보가 있는지 없는지 확인하는 과정이 필요하다. [ protocol2 문서 참고 ]
                 * */

                if (userIsCP){
                }
                else{
                    Log.d(TAG,"requestRemoteSdp")
                    rtcClient.requestRemoteSdp()
                }

            }

            override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
                super.onConnectError(websocket, exception)
                Log.d(TAG,"onConnectError : connectAsynchronously() 실패시 호출됨")
            }

            override fun onContinuationFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onContinuationFrame(websocket, frame)
                Log.d(TAG,"onContinuationFrame : 연속 프레임이 수신 될때 호출됨")
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
                Log.d(TAG,"onFrame : 프레임 수신시 호출")
            }

            override fun onFrameError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frame: WebSocketFrame?
            ) {
                super.onFrameError(websocket, cause, frame)
                Log.d(TAG,"onFrameError : 프레임 읽기 실패시 호출")
            }

            override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrameSent(websocket, frame)
                Log.d(TAG,"onFrameSent : 프레임 전송시 호출")
            }

            override fun onFrameUnsent(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrameUnsent(websocket, frame)
                Log.d(TAG,"onFrameUnsent : 프레임 전송이 안되었을때 호출")
            }

            override fun onMessageDecompressionError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                compressed: ByteArray?
            ) {
                super.onMessageDecompressionError(websocket, cause, compressed)
                Log.d(TAG,"onMessageDecompressionError : 메시지 압축 해제에 실패했을때 호출")
            }

            override fun onMessageError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frames: MutableList<WebSocketFrame>?
            ) {
                super.onMessageError(websocket, cause, frames)
                Log.d(TAG,"onMessageError : 메시지 생성에 실패했을때 호출")
            }

            override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onPingFrame(websocket, frame)
                Log.d(TAG,"onPingFrame : 핑 프레임 수신 될때 호출")
            }

            override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onPongFrame(websocket, frame)
                Log.d(TAG,"onPongFrame : 퐁 프레임이 수신 될때 호출 호출")
            }

            override fun onSendError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frame: WebSocketFrame?
            ) {
                super.onSendError(websocket, cause, frame)
                Log.d(TAG,"onSendError : 프레임 전송 중 오류 발생하면 호출")
            }

            override fun onSendingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onSendingFrame(websocket, frame)
                Log.d(TAG,"onSendingFrame : 프레임 전송되기 전에 호출")
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
                Log.d(TAG,"onTextFrame : 텍스트 프레임이 수신될때 호출")
            }

            override fun onTextMessage(websocket: WebSocket?, data: ByteArray?) {
                super.onTextMessage(websocket, data)
                Log.d(TAG,"onTextFrame : 바이트 배열 데이터가 수신될때 호출 $data")
            }

            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                super.onTextMessage(websocket, text)
                Log.d(TAG,"onTextMessage : 문자 데이터가 수신될때 호출 : $text")

                val responseData = messageModelParser(text.toString())

                val msg_type = responseData["msg_type"]
                val msg_code = responseData["msg_code"]
                val str_val = responseData["str_val"]

                when(msg_type){
                    // 서버에서 알림이 왔다
                    "2" -> {
                        // 메세지 코드 확인 [ 1:제어/작업요청 , 2:알림, 3:요청 응답 ]
                        when(msg_code){
                            "1" ->{
                                if (userIsCP){
                                    // 상대방 클라이언트의 SDP 정보를 저장

                                    val otherClientSdp =  SessionDescription(SessionDescription.Type.ANSWER, str_val)
                                    rtcClient.peerConnection.setRemoteDescription(AppSdpObserver(), otherClientSdp)
                                }
                            }
                            "2" ->{

                            }
                            "3" ->{

                            }
                        }
                    }
                }

            }

            override fun onTextMessageError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                data: ByteArray?
            ) {
                super.onTextMessageError(websocket, cause, data)
                Log.d(TAG,"onTextMessageError : 스레드가 생성된후 호출")
            }

            override fun onThreadCreated(
                websocket: WebSocket?,
                threadType: ThreadType?,
                thread: Thread?
            ) {
                super.onThreadCreated(websocket, threadType, thread)
                Log.d(TAG,"onTextMessageError : 스레드가 생성된후 호출")
            }

            override fun onThreadStarted(
                websocket: WebSocket?,
                threadType: ThreadType?,
                thread: Thread?
            ) {
                super.onThreadStarted(websocket, threadType, thread)
                Log.d(TAG,"onThreadStarted : 스레드의 run()메서드가 시작될때 호출")
            }

            override fun onThreadStopping(
                websocket: WebSocket?,
                threadType: ThreadType?,
                thread: Thread?
            ) {
                super.onThreadStopping(websocket, threadType, thread)
                Log.d(TAG,"onThreadStopping : 스레드의 run()메서드의 끝에서 호출")
            }

            override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException?) {
                super.onUnexpectedError(websocket, cause)
                Log.d(TAG,"onUnexpectedError : ?? 무슨예외 : $cause")
            }
        })
    }

    fun close(){
        ws.socket.close()
    }

    fun messageModelParser(json : String): HashMap<String, String> {

        val jsonObject = JSONObject(json)
        val responseHashMap = HashMap<String, String>()

        responseHashMap["msg_type"] = jsonObject.getString("msg_type")
        responseHashMap["msg_code"] = jsonObject.getString("msg_code")
        responseHashMap["task_id"] = jsonObject.getString("task_id")
        responseHashMap["str_val"] = jsonObject.getString("str_val")
        responseHashMap["str_array"] = jsonObject.getString("str_array")
        responseHashMap["int_val"] = jsonObject.getString("int_val")
        responseHashMap["int_array"] = jsonObject.getString("int_array")
        responseHashMap["bool_val"] = jsonObject.getString("bool_val")
        responseHashMap["bool_array"] = jsonObject.getString("bool_array")
        responseHashMap["object_val"] = jsonObject.getString("object_val")

        return responseHashMap
    }

    companion object{
        lateinit var rtcClient: RTCClient
    }
}