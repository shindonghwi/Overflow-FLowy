@file:JvmName("Constants")

package com.overflow.flowy.Util

import android.content.Context
import androidx.camera.core.CameraSelector
import java.util.ArrayList

@JvmField val REQUEST_PERMISSION_CODE = 1
@JvmField val MY_LOG = "MY_LOG"

// OpenGL 에서 vertex 좌표 ( NDC라는 디바이스 좌표계를 사용한다고 한다. ) - 중앙에 원점이 위치함.
// android의 OpenGL 좌표는 왼쪽 아래가 0,0

@JvmField val NDC_VERTICE = floatArrayOf(
    -1.0f, -1.0f,
    1.0f, -1.0f,
    -1.0f, 1.0f,
    1.0f, 1.0f
)

/** NDC 좌표를 후면 카메라로 매핑 */
@JvmField val BACK_OPENGL_VERTICE = floatArrayOf(
    1.0f, 1.0f,
    1.0f, 0.0f,
    0.0f, 1.0f,
    0.0f, 0.0f
)

/** NDC 좌표를 전면 카메라로 매핑 */
@JvmField val FRONT_OPENGL_VERTICE = floatArrayOf(
    0.0f, 1.0f,
    0.0f, 0.0f,
    1.0f, 1.0f,
    1.0f, 0.0f
)


@JvmField var THIS_CONTEXT : Context? = null
@JvmField var textureArray = IntArray(5)
@JvmField var cameraLensMode : Int = CameraSelector.LENS_FACING_BACK // 카메라의 렌즈방향 - 전면, 후면
@JvmField var cameraMode : String = "default" // 카메라 모드 - default, flowy 등
@JvmField var cameraSubMode : String = "longClick" // 카메라 서브 모드 - 기본은 longClick 모드 , flowyDoubleTap 등
