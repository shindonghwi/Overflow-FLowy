package at.overflow.flowy.View

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.SurfaceHolder
import at.overflow.flowy.Renderer.FlowyRenderer

class FlowyGLTextureView(context: Context, attributeSet: AttributeSet) :
    GLTextureView(context, attributeSet){

    private var mRenderer: FlowyRenderer =
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
}