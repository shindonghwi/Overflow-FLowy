package at.overflow.flowy.View

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.TextureView
import at.overflow.flowy.MainActivity
import at.overflow.flowy.Renderer.FlowyRenderer
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.VideoFrame
import org.webrtc.VideoFrame.I420Buffer
import org.webrtc.VideoSink
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


class FlowyGLTextureView(context: Context, attributeSet: AttributeSet) :
    GLTextureView(context, attributeSet), VideoSink{

    var mRenderer: FlowyRenderer =
        FlowyRenderer(this)

    init{
        setEGLContextClientVersion(2)
        setRenderer(mRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()
        mRenderer.onResume()
    }

    override fun onPause() {
        mRenderer.onPause()
        super.onPause()
    }

    override fun onFrame(p0: VideoFrame?) {
        Log.d("GlTextureView","$p0")
    }
}