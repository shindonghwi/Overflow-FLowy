package com.overflow.flowy.View

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.SurfaceHolder
import com.overflow.flowy.Renderer.FlowyRenderer

class FlowyGLSurfaceView(context: Context, attributeSet: AttributeSet) :
    GLSurfaceView(context, attributeSet){

    private var mRenderer: FlowyRenderer =
        FlowyRenderer(this)

    init{
        setEGLContextClientVersion(2)
        setRenderer(mRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        super.surfaceChanged(holder, format, w, h)
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