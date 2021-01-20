package at.overflow.flowy.Util

import android.util.Log
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import at.overflow.flowy.Fragment.FragmentCamera
import at.overflow.flowy.Fragment.FragmentCamera.Companion.touchDataUtil
import at.overflow.flowy.Renderer.FlowyRenderer.Companion.camera
import at.overflow.flowy.View.FlowyGLTextureView
import java.util.concurrent.TimeUnit


class CameraUtil {

    /** 카메라 자동 포커스 잡기 */
    fun cameraAutoFocus(glTextureView: FlowyGLTextureView) {
        camera!!.cameraControl.cancelFocusAndMetering()
        val factory = SurfaceOrientedMeteringPointFactory(
            glTextureView.width.toFloat(),
            glTextureView.height.toFloat()
        )
        val autoFocusPoint = factory.createPoint(
            glTextureView.width.toFloat() / 2,
            glTextureView.height.toFloat() / 2
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
    fun cameraTapFocus(glTextureView: FlowyGLTextureView) {
//        camera!!.cameraControl.cancelFocusAndMetering()
        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
            glTextureView.width.toFloat(), glTextureView.height.toFloat()
        )

        val autoFocusPoint: MeteringPoint

        Log.d("widthHeight", "${glTextureView.width} : ${glTextureView.height}")

        autoFocusPoint = if (touchDataUtil.touchFocusPointX == 0f && touchDataUtil.touchFocusPointY == 0f) {
            factory.createPoint(
                (glTextureView.width.toDouble() / 2).toFloat(),
                (glTextureView.height.toDouble() / 2).toFloat()
            )
        } else {
            factory.createPoint(touchDataUtil.touchFocusPointX, touchDataUtil.touchFocusPointY)
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