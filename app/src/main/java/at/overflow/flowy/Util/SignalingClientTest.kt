package at.overflow.flowy.Util

import android.util.Base64
import android.util.Log
import com.neovisionaries.ws.client.*
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URI
import java.nio.charset.Charset

class SignalingClientTest{

    private val factory = WebSocketFactory().setConnectionTimeout(10000)
    private val ws: WebSocket = factory.createSocket(URI(OVERFLOW_WEB_SOCKET))
    private lateinit var signalingClient : SignalingClientTest

    init {

        webSocketListener()
        setHeader()
        connection()

    }

    fun get(): SignalingClientTest {
        if (signalingClient != null)
            signalingClient = SignalingClientTest()
        return signalingClient
    }

    fun setHeader(){
        ws.addHeader("X-Flowy-Service-ID", "012345678901")
        ws.addHeader("X-Flowy-User-Token", "123456789")
        ws.addHeader("X-Flowy-Client-Type", "1")
    }

    fun connection(){
        try {
            // 서버에 연결 한 후에, 핸드 셰이크 수행
            ws.connect()
        }catch (e : OpeningHandshakeException){
            // 핸드세이크 오픈 중에 웹 소켓 프로토콜 위반 에러
            Log.d("webSocket","OpeningHandshakeException : ${e.headers} / ${e.body} / ${e.message}")
        }catch ( e : HostnameUnverifiedException)
        {
            // 인증서와 호스트 네임 불일치 에러
            Log.d("webSocket","HostnameUnverifiedException : ${e.hostname} / ${e.message}")
        }
        catch (e : WebSocketException)
        {
            // WebSocket 연결을 설정하지 못했습니다.
            Log.d("webSocket","WebSocketException : ${e.error} / ${e.message}")
        }
    }

    fun sendIceCandidate(iceCandidate: IceCandidate) {
        val ob = JSONObject()
        try {
            ob.put("msg_type", 1)
            ob.put("msg_code", 1)
            ob.put("str_val", strBase64Encoding(iceCandidate.sdp))
            ws.sendText(ob.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendSessionDescription(sdp: SessionDescription) {
        val ob = JSONObject()
        try {
            ob.put("msg_type", 1)
            ob.put("msg_code", 1)
            ob.put("str_val", strBase64Encoding(sdp.description))
            ws.sendText(ob.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun webSocketListener(){
        ws.addListener(object : WebSocketAdapter() {
            override fun onConnected(
                websocket: WebSocket?,
                headers: MutableMap<String, MutableList<String>>?
            ) {
                super.onConnected(websocket, headers)
                Log.d("webSocket","onConnected : Opening 핸드 셰이크 성공 후 호출완료 // ${headers.toString()}")
            }

            override fun onDisconnected(
                websocket: WebSocket?,
                serverCloseFrame: WebSocketFrame?,
                clientCloseFrame: WebSocketFrame?,
                closedByServer: Boolean
            ) {
                super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer)
                Log.d("webSocket","onDisconnected : WebSocket 연결이 닫힌 후에 호출 // ${serverCloseFrame.toString()} // ${clientCloseFrame.toString()} // ${closedByServer.toString()}")
            }

            override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                super.onError(websocket, cause)
                Log.d("webSocket","onError : 오류 발생 : $cause")
            }

            override fun onConnectError(websocket: WebSocket?, exception: WebSocketException) {
                super.onConnectError(websocket, exception)
                Log.d("webSocket","onConnectError : 오류 발생 : ${exception.error} / ${exception.message}")
            }

            override fun onStateChanged(websocket: WebSocket?, newState: WebSocketState?) {
                super.onStateChanged(websocket, newState)
                Log.d("webSocket","onStateChanged : WebSocket 상태 변경 : $newState")
            }

            override fun onSendingHandshake(
                websocket: WebSocket?,
                requestLine: String?,
                headers: MutableList<Array<String>>?
            ) {
                super.onSendingHandshake(websocket, requestLine, headers)
                Log.d("webSocket","onSendingHandshake : handShake 보내기 전 호출")
            }

            override fun onSendError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frame: WebSocketFrame?
            ) {
                super.onSendError(websocket, cause, frame)
                Log.d("webSocket","onSendError : 프레임 전송중 에러 $cause")
            }

            override fun onFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrame(websocket, frame)
                Log.d("webSocket","onFrame : 프레임이 수신 될때 호출 }")
            }

            override fun onFrameError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                frame: WebSocketFrame?
            ) {
                super.onFrameError(websocket, cause, frame)
                Log.d("webSocket","onFrameError : 프레임을 읽기에 실패 했을때 호출")
            }

            override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrameSent(websocket, frame)
                Log.d("webSocket","onFrameSent : 프레임이 전송 될 때 호출")
            }

            override fun onFrameUnsent(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onFrameUnsent(websocket, frame)
                Log.d("webSocket","onFrameUnsent : 프레임이 전송 되지 않았을때 호출")
            }

            override fun onSendingFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onSendingFrame(websocket, frame)
                Log.d("webSocket","onSendingFrame : 프레임이 전송되기 전 호출")
            }

            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                super.onTextMessage(websocket, text)
                Log.d("webSocket","onTextMessage : text 전달 받음 : $text")
            }

            override fun onTextMessage(websocket: WebSocket?, data: ByteArray?) {
                super.onTextMessage(websocket, data)
                Log.d("webSocket","onTextMessage : byteArray 전달 받음 : $data")
            }

            override fun onTextFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onTextFrame(websocket, frame)
                Log.d("webSocket","onTextFrame : 텍스트 프레임 수신 : $frame")
            }

            override fun onTextMessageError(
                websocket: WebSocket?,
                cause: WebSocketException?,
                data: ByteArray?
            ) {
                super.onTextMessageError(websocket, cause, data)
                Log.d("webSocket","onTextMessageError : 메세지 생성 실패  : $cause / $data")
            }
        })
    }

    fun strBase64Encoding(text : String): String? {
        val data: ByteArray = text.toByteArray(Charset.forName("UTF-8"))
        return Base64.encodeToString(data, Base64.DEFAULT)
    }
}