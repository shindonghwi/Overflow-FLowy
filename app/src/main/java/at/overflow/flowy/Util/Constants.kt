@file:JvmName("Constants")

package at.overflow.flowy.Util

import android.content.Context
import androidx.camera.core.CameraSelector
import at.overflow.flowy.DTO.ContrastData
import at.overflow.flowy.R

@JvmField val REQUEST_PERMISSION_CODE = 1
@JvmField val APP_UPDATE_PERMISSION_CODE = 999
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
@JvmField var fragmentType : String = "default"
@JvmField var binaryFlag : Boolean = true
@JvmField var inverseFlag : Boolean = false
@JvmField var vertexType : String = "default"

/** 부가기능 : 플래시, 포커스, 프리즈 기능 등 */
@JvmField var freezeMode : Boolean = false
@JvmField var autoFocusMode : Boolean = true
@JvmField var castMode : Boolean = false

@JvmField var deviceRotationValue : Int = 0

@JvmField val contrastInitData = arrayListOf<ContrastData>(
    ContrastData(
        null,
        (R.color.black),
        (R.color.white),
        "흑/백",
        null
    ),
    ContrastData(
        null,
        (R.color.black),
        (R.color.yellow),
        "흑/황",
        null
    ),
    ContrastData(
        null,
        (R.color.blue),
        (R.color.white),
        "청/백",
        null
    ),
    ContrastData(
        null,
        (R.color.blue),
        (R.color.yellow),
        "청/황",
        null
    )
)


/** 서버와 통신하는데 필요한 정보 */
@JvmField val API_KEY : String = "f077d9dedeb7d1de5a12449ed3aa56b9b0855829c087de33da9d942ed42248a25711a9b8ef8964530b37773e8ed322843b916a2de7594b4830f3f6c7aa3408fd"
@JvmField var USER_UUID : String = ""
@JvmField var OVERFLOW_TEST_API_BASE_URL : String = "https://at.flowy.kr/"
@JvmField var OVERFLOW_TEST_API_IMAGE_UPLOAD : String = "http://121.161.228.253:12000/"
@JvmField var OVERFLOW_WEB_SOCKET_URL : String = "http://st.flowy.kr:16000/openControl"
//@JvmField var OVERFLOW_TEST_API_IMAGE_UPLOAD : String = "http://faitest.flowy.kr:12000/"
