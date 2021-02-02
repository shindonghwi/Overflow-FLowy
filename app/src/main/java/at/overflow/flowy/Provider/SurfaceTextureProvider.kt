package at.overflow.flowy.Provider

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview.SurfaceProvider
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.core.util.Consumer
import at.overflow.flowy.Util.cameraTexture
import at.overflow.flowy.Util.textureArray
import at.overflow.flowy.Util.textureMode

/**
 * SurfaceTextureProvider는 미리 구성된 표면을 제공하는 Preview SurfaceProvider의 구현을 생성한다.
 */
class SurfaceTextureProvider : SurfaceTexture.OnFrameAvailableListener{

    @SuppressLint("RestrictedApi")
    fun createSurfaceTextureProvider(
        surfaceTextureCallback: SurfaceTextureCallback
    ): SurfaceProvider {
        return SurfaceProvider { surfaceRequest: SurfaceRequest ->
            textureMode = 0 // 내장 카메라 사용
            val surfaceTexture = SurfaceTexture(textureArray[0])
            cameraTexture = surfaceTexture
            surfaceTexture.setDefaultBufferSize(
                surfaceRequest.resolution.width,
                surfaceRequest.resolution.height
            )
            surfaceTexture.detachFromGLContext()
            surfaceTextureCallback.onSurfaceTextureReady(
                surfaceTexture,
                surfaceRequest.resolution
            )
            screenSurface = Surface(surfaceTexture)
//            val surface = Surface(surfaceTexture)
            surfaceRequest.provideSurface(screenSurface,
                CameraXExecutors.directExecutor(),
                Consumer {
                    screenSurface.release()
                    surfaceTextureCallback.onSafeToRelease(surfaceTexture)
                }
            )
        }
    }


    interface SurfaceTextureCallback {

        fun onSurfaceTextureReady(
            surfaceTexture: SurfaceTexture,
            resolution: Size
        )

        fun onSafeToRelease(surfaceTexture: SurfaceTexture){

        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        Log.d("tetetest", "onFrameAvailable")
    }

    companion object{
        lateinit var screenSurface : Surface
    }

}