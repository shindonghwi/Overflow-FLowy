package com.overflow.flowy.Util

import android.util.Log
import androidx.camera.core.*
import androidx.camera.core.FocusMeteringAction.MeteringMode
import androidx.core.content.ContextCompat
import com.overflow.flowy.Fragment.FragmentCamera
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchAlwaysTouchPointX
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchAlwaysTouchPointY
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchFocusPointX
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchFocusPointY
import com.overflow.flowy.Renderer.FlowyRenderer.Companion.camera
import com.overflow.flowy.View.FlowyGLSurfaceView
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.TimeUnit


class CameraUtil {

    /** 카메라 자동 포커스 잡기 */
    fun cameraAutoFocus(glSurfaceView: FlowyGLSurfaceView) {
        camera!!.cameraControl.cancelFocusAndMetering()
        val factory = SurfaceOrientedMeteringPointFactory(
            glSurfaceView.width.toFloat(),
            glSurfaceView.height.toFloat()
        )
        val autoFocusPoint = factory.createPoint(
            glSurfaceView.width.toFloat() / 2,
            glSurfaceView.height.toFloat() / 2
        )
        camera!!.cameraControl.startFocusAndMetering(
            FocusMeteringAction.Builder(
                autoFocusPoint,
                FocusMeteringAction.FLAG_AF
            ).apply {
                //auto-focus every 1 seconds
                setAutoCancelDuration(1, TimeUnit.SECONDS)
            }.build()
        )
    }

    /** 사용자가 카메라에 탭했을때 포커스 잡기 */
    fun cameraTapFocus(glSurfaceView: FlowyGLSurfaceView) {
//        camera!!.cameraControl.cancelFocusAndMetering()
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            glSurfaceView.width.toFloat(), glSurfaceView.height.toFloat()
        )

        val autoFocusPoint: MeteringPoint

        Log.d("widthHeight", "${glSurfaceView.width} : ${glSurfaceView.height}")

        autoFocusPoint = if (touchFocusPointX == 0f && touchFocusPointY == 0f) {
            factory.createPoint(
                (glSurfaceView.width.toDouble() / 2).toFloat(),
                (glSurfaceView.height.toDouble() / 2).toFloat()
            )
        } else {
            factory.createPoint(touchFocusPointX, touchFocusPointY)
        }

        val action = FocusMeteringAction.Builder(autoFocusPoint, FocusMeteringAction.FLAG_AF)
            .apply { disableAutoCancel() }.build()

        val future = camera!!.cameraControl.startFocusAndMetering(action)
        future.addListener(Runnable {
            try {
                val result = future.get()
                Log.d("successFail", result.isFocusSuccessful.toString())
            } catch (e: Exception) {
            }
        }, ContextCompat.getMainExecutor(THIS_CONTEXT))
    }
}