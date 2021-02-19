package at.overflow.flowy.Disable//package at.overflow.flowy.Renderer
//
//import android.R.attr.mimeType
//import android.media.MediaCodec
//import android.media.MediaCodecInfo
//import android.media.MediaCodecInfo.CodecCapabilities
//import android.media.MediaFormat
//import android.util.Log
//import at.overflow.flowy.Fragment.FragmentCamera.Companion.webSocketUtil
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import org.webrtc.DataChannel
//import java.nio.ByteBuffer
//import java.nio.charset.Charset
//
//
//class VideoEncoderThread(
//    private val videoW: Int, // glTextureView width
//    private val videoH: Int, // glTextureView height
//    private val videoBitrate: Int,
//    private val videoFrameRate: Int
//) {
//    val CHUNK_SIZE : Int by lazy { 64000 }
//    private var codec: MediaCodec? = null
//    private var bufferCount: Int = 0
//    private fun initMediaCodec() {
//        bufferCount = 0
//        try {
//            codec = MediaCodec.createEncoderByType(MIME)
//            val format = MediaFormat.createVideoFormat(MIME, videoW, videoH)
//            format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
//            format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate)
//            format.setInteger(
//                MediaFormat.KEY_COLOR_FORMAT,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
////                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
//            )
//            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
//            codec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
//            codec!!.start()
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    fun encoderYUV420(input: ByteArray) {
//        CoroutineScope(Dispatchers.Default).launch {
//            try {
//                val inputBufferIndex = codec!!.dequeueInputBuffer(-1)
//
//                if (inputBufferIndex >= 0) {
//                    val inputBuffer = codec!!.getInputBuffer(inputBufferIndex)
//                    inputBuffer!!.clear()
//                    inputBuffer.put(input)
//                    codec!!.queueInputBuffer(
//                        inputBufferIndex,
//                        0,
//                        input.size,
//                        System.currentTimeMillis(),
//                        0
//                    )
//                }
//                val bufferInfo = MediaCodec.BufferInfo()
//                var outputBufferIndex = codec!!.dequeueOutputBuffer(bufferInfo, 0)
//                Log.d("sdffdsdffd","outputBufferIndex : ${outputBufferIndex}")
//
//                while (outputBufferIndex >= 0) {
//                    val outputBuffer = codec!!.getOutputBuffer(outputBufferIndex)
//
//                    var buf : ByteBuffer
//
//                    if (outputBuffer != null){
//                        buf = ByteBuffer.allocate(outputBuffer.limit())
//                        buf.put(outputBuffer)
//                        buf.flip()
//                        Log.d(TAG, "Wrote " + outputBuffer.limit() + " bytes. : ${buf.array().size}");
//                        Log.d(TAG, "buf : ${buf} / buf.array() : ${buf.array()} / offset : ${outputBuffer.limit()}");
////                        webSocketUtil!!.rtcClient.dataChannel.send(DataChannel.Buffer(buf, false))
//                    }
//
//
////                    val outData = ByteArray(outputBuffer!!.remaining())
////                    outputBuffer[outData, 0, outData.size] as ByteBuffer
////                    codec!!.releaseOutputBuffer(outputBufferIndex, false)
////                    outputBufferIndex = codec!!.dequeueOutputBuffer(bufferInfo, 0)
////                    Log.d("sdffdsdffda","outputBuffer0 $outputBuffer")
////                    Log.d("sdffdsdffd","outputBuffer.isDirect : ${outputBuffer.isDirect}")
////                    Log.d("sdffdsdffd","outputBuffer.isReadOnly : ${outputBuffer.isReadOnly}")
////                    CoroutineScope(Dispatchers.Default).launch {
////                        if (webSocketUtil!!.rtcClient.dataChannel.state() == DataChannel.State.OPEN){
////                            val numberOfChunks: Int = outData.size / CHUNK_SIZE
////
////                            for (i in 0 until numberOfChunks) {
////                                val wrap = ByteBuffer.wrap(outData, i * CHUNK_SIZE,  CHUNK_SIZE )
////                                webSocketUtil!!.rtcClient.dataChannel.send(DataChannel.Buffer(wrap, false))
////                            }
////                            val remainder: Int = outData.size % CHUNK_SIZE
////                            if (remainder > 0) {
////                                val wrap = ByteBuffer.wrap(  outData, numberOfChunks * CHUNK_SIZE,  remainder  )
////                                webSocketUtil!!.rtcClient.dataChannel.send(DataChannel.Buffer(wrap, false))
////                            }
////                        }
////                    }
//
//
//                }
//
//                Log.d("sdffdsdffd","outputBuffers : ${codec!!.outputBuffers.size}")
////                Log.d("sdffdsdffd","dataChannel State : ${webSocketUtil!!.rtcClient.dataChannel.state()}")
//
//
////                for (i in 0 until codec!!.outputBuffers.size){
////                    Log.d("sdffdsdffd","outputBuffers : ${codec!!.outputBuffers[i]}")
////                }
//
////                Log.d("sdffdsdffd","outputBuffers.size : ${codec!!.outputBuffers.size}")
////                Log.d("sdffdsdffd","정상적인 outputBuffers가 생김 ${codec!!.outputBuffers}")
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.d("sdffdsdffd","e.message : ${e.message}")
//            }
//        }
//    }
//
//    private fun stringToByteBuffer(
//        msg: String,
//        charset: Charset
//    ): ByteBuffer? {
//        return ByteBuffer.wrap(msg.toByteArray(charset))
//    }
//
//    fun releaseMediaCodec() {
//        if (codec != null) {
//            codec!!.stop()
//            codec!!.release()
//            codec = null
//        }
////        gchannel.close()
////        lFileOutputStream.close()
//    }
//
//    companion object {
//        private const val TAG = "Encode"
//        private const val MIME = "Video/AVC"
//    }
//
//    init {
//        initMediaCodec()
//    }
//}
