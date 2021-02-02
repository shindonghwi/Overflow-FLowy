package at.overflow.flowy.Util

import android.util.Log
import com.google.gson.JsonArray
import com.neovisionaries.ws.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class WebSocketUtil {

    lateinit var ws : WebSocket

    init {

        Thread(kotlinx.coroutines.Runnable {

            try{
                val factory = WebSocketFactory().apply {
                    connectionTimeout = 5000
                }

                ws = factory.createSocket(OVERFLOW_WEB_SOCKET_URL)

                setWebSocketListener()

                ws.addHeader("X-Flowy-Service-ID", "012345678901") // 서비스 ID
                ws.addHeader("X-Flowy-User-Token", "abcdefghijklmn") // 사용자 토큰
                ws.addHeader("X-Flowy-Client-Type", "1") // 유형 1 - 제공자, 유형 2 - 소비자

                ws.connect()

                // 서버로 메세지 보내는 통신 테스트
                TEST_sendSDP()

            }
            catch ( e: Exception){
                Log.d("webSocketLog","webSocket Connection Error : $e")
            }


        }).start()
    }

    private fun setWebSocketListener(){
        ws.addListener(object:WebSocketAdapter() {

            override fun handleCallbackError(websocket: WebSocket?, cause: Throwable?) {
                super.handleCallbackError(websocket, cause)
                Log.d("webSocketLog","handleCallbackError : $cause")
            }

            override fun onBinaryFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onBinaryFrame(websocket, frame)
                Log.d("webSocketLog","onBinaryFrame : 바이너리 프레임 수신 : $frame")
            }

            override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray?) {
                super.onBinaryMessage(websocket, binary)
                Log.d("webSocketLog","onBinaryMessage : 바이너리 메세지 수신 : $binary")
            }

            override fun onCloseFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onCloseFrame(websocket, frame)
                Log.d("webSocketLog","onCloseFrame : 닫기 프레임 수신 : $frame")
            }

            override fun onConnected(
                websocket: WebSocket?,
                headers: MutableMap<String, MutableList<String>>?
            ) {
                super.onConnected(websocket, headers)
                Log.d("webSocketLog","onConnected  오프닝 핸드 셰이크 성공 후 호출됨")
            }

            override fun onConnectError(websocket: WebSocket?, exception: WebSocketException?) {
                super.onConnectError(websocket, exception)
                Log.d("webSocketLog","onConnectError : connectAsynchronously() 실패시 호출됨")
            }

            override fun onContinuationFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onContinuationFrame(websocket, frame)
                Log.d("webSocketLog","onContinuationFrame : 연속 프레임이 수신 될때 호출됨")
            }

            override fun onDisconnected(
                websocket: WebSocket?,
                serverCloseFrame: WebSocketFrame?,
                clientCloseFrame: WebSocketFrame?,
                closedByServer: Boolean
            ) {
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
                Log.d("webSocketLog","onDisconnected : webSocket 연결이 닫힌 후 호출")
            }

            override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                super.onError(websocket, cause)
                Log.d("webSocketLog","onError : 오류 발생시 호출")
            }

            override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrame(websocket, frame)
                Log.d("webSocketLog","onFrame : 프레임 수신시 호출")
            }

            override fun onFrameError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frame: WebSocketFrame?
            ) {
                super.onFrameError(websocket, cause, frame)
                Log.d("webSocketLog","onFrameError : 프레임 읽기 실패시 호출")
            }

            override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrameSent(websocket, frame)
                Log.d("webSocketLog","onFrameSent : 프레임 전송시 호출")
            }

            override fun onFrameUnsent(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrameUnsent(websocket, frame)
                Log.d("webSocketLog","onFrameUnsent : 프레임 전송이 안되었을때 호출")
            }

            override fun onMessageDecompressionError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                compressed: ByteArray?
            ) {
                super.onMessageDecompressionError(websocket, cause, compressed)
                Log.d("webSocketLog","onMessageDecompressionError : 메시지 압축 해제에 실패했을때 호출")
            }

            override fun onMessageError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frames: MutableList<WebSocketFrame>?
            ) {
                super.onMessageError(websocket, cause, frames)
                Log.d("webSocketLog","onMessageError : 메시지 생성에 실패했을때 호출")
            }

            override fun onPingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onPingFrame(websocket, frame)
                Log.d("webSocketLog","onPingFrame : 핑 프레임 수신 될때 호출")
            }

            override fun onPongFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onPongFrame(websocket, frame)
                Log.d("webSocketLog","onPongFrame : 퐁 프레임이 수신 될때 호출 호출")
            }

            override fun onSendError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frame: WebSocketFrame?
            ) {
                super.onSendError(websocket, cause, frame)
                Log.d("webSocketLog","onSendError : 프레임 전송 중 오류 발생하면 호출")
            }

            override fun onSendingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onSendingFrame(websocket, frame)
                Log.d("webSocketLog","onSendingFrame : 프레임 전송되기 전에 호출")
            }

            override fun onSendingHandshake(
                websocket: WebSocket?,
                requestLine: String?,
                headers: MutableList<Array<String>>?
            ) {
                super.onSendingHandshake(websocket, requestLine, headers)
                Log.d("webSocketLog","onSendingHandshake : 오프닝 핸드 셰이크를 보내기전에 호출")
            }

            override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {
                super.onStateChanged(websocket, newState)
                Log.d("webSocketLog","onStateChanged : WebSocket 상태 변경시 호출")
            }

            override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onTextFrame(websocket, frame)
                Log.d("webSocketLog","onTextFrame : 텍스트 프레임이 수신될때 호출")
            }

            override fun onTextMessage(websocket: WebSocket?, data: ByteArray?) {
                super.onTextMessage(websocket, data)
                Log.d("webSocketLog","onTextFrame : 바이트 배열 데이터가 수신될때 호출")
            }

            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                super.onTextMessage(websocket, text)
                Log.d("webSocketLog","onTextMessage : 문자 데이터가 수신될때 호출")
            }

            override fun onTextMessageError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                data: ByteArray?
            ) {
                super.onTextMessageError(websocket, cause, data)
                Log.d("webSocketLog","onTextMessageError : 스레드가 생성된후 호출")
            }

            override fun onThreadCreated(
                websocket: WebSocket?,
                threadType: ThreadType?,
                thread: Thread?
            ) {
                super.onThreadCreated(websocket, threadType, thread)
                Log.d("webSocketLog","onTextMessageError : 스레드가 생성된후 호출")
            }

            override fun onThreadStarted(
                websocket: WebSocket?,
                threadType: ThreadType?,
                thread: Thread?
            ) {
                super.onThreadStarted(websocket, threadType, thread)
                Log.d("webSocketLog","onThreadStarted : 스레드의 run()메서드가 시작될때 호출")
            }

            override fun onThreadStopping(
                websocket: WebSocket?,
                threadType: ThreadType?,
                thread: Thread?
            ) {
                super.onThreadStopping(websocket, threadType, thread)
                Log.d("webSocketLog","onThreadStopping : 스레드의 run()메서드의 끝에서 호출")
            }

            override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException?) {
                super.onUnexpectedError(websocket, cause)
                Log.d("webSocketLog","onUnexpectedError : ?? 무슨예외 : $cause")
            }
        })
    }

    private fun TEST_sendSDP(){
        var i = 0
        CoroutineScope(Dispatchers.Default).launch {
            while(i > 10){

                val ob = JSONObject()
                ob.put("msg_type", "1")
                ob.put("msg_code", "1")
                ob.put("str_val", "asdjasckjzxc")
                ws.sendText(ob.toString())
                delay(1000)
                i += 1
            }
        }
    }

}