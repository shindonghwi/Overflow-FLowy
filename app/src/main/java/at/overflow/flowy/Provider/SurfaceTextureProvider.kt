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
import at.overflow.flowy.Util.textureArray

/**
 * CameraX를 통해 나온 카메라의 정보를 Surface에 표시하기 위해서 필요한 Provider
 * 사용할 텍스처를 하나 만들고 -> textureArray[0]]
 * surfaceTexture를 생성한다. -> val surfaceTexture = SurfaceTexture(textureArray[0])
 * 생성된 surfaceTexture를 기반으로 surface를 생성한다.
 * surface는 카메라로 부터 전달받은 정보를 표시하기 위해 필요하다.
 */
class SurfaceTextureProvider : SurfaceTexture.OnFrameAvailableListener{

    @SuppressLint("RestrictedApi")
    fun createSurfaceTextureProvider(
        surfaceTextureCallback: SurfaceTextureCallback
    ): SurfaceProvider {
        return SurfaceProvider { surfaceRequest: SurfaceRequest ->
            val surfaceTexture = SurfaceTexture(textureArray[0])
            surfaceTexture.setDefaultBufferSize(
                surfaceRequest.resolution.width,
                surfaceRequest.resolution.height
            )
            surfaceTexture.detachFromGLContext()
            surfaceTextureCallback.onSurfaceTextureReady(
                surfaceTexture,
                surfaceRequest.resolution
            )
            val surface = Surface(surfaceTexture)
            surfaceRequest.provideSurface(surface,
                CameraXExecutors.directExecutor(),
                Consumer {
                    surface.release()
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

}