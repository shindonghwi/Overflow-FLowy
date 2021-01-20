package at.overflow.flowy.Util

import android.util.Log
import at.overflow.flowy.Renderer.FlowyRenderer
import at.overflow.flowy.View.GLTextureView

class TouchDataUtil {

    /** 사용자가 터치한 좌표값 (항상 갱신됨) */
    var touchAlwaysTouchPointX = 0.0
    var touchAlwaysTouchPointY = 0.0

    /** 사용자가 화면을 터치했을때 좌표를 항상 기록한다. */
    fun setAlwaysTouchPoint(x: Double, y: Double) {
        this.touchAlwaysTouchPointX = x
        this.touchAlwaysTouchPointY = y
    }

    /** 밝기, 대비 조절기능의 시크바 */
    var brightSeekbarProgress: Int = 50
    var contrastSeekbarProgress: Int = 50

    /**
     *  --------------------- 플로위 롱 클릭 모드 ---------------------
     *  */

    /** 사용자가 터치중인지 여부 판단함 */
    var isTouching: Boolean = false
    var isDoubleTapFirstTouched: Boolean = false

    /** 사용자가 플로위 롱 클릭 모드에서 터치한 좌표 값 ( 지속적으로 바뀜 ) */
    var touchPointX: Double = 0.0
    var touchPointY: Double = 0.0

    /** 사용자가 플로위 롱 클릭 모드에서 처음으로 터치한 좌표 값 ( 화면 여백을 클릭하는걸 방지하기 위함 ) */
    var touchFirstX: Double = 0.0
    var touchFirstY: Double = 0.0

    /** 사용자가 롱 클릭 모드에서 터치한 좌표값 설정 - FLowy Zoom 기능에 활용 */
    fun setTouchPoint(x: Double, y: Double) {
        this.touchPointX = x
        this.touchPointY = y
        Log.d("touch", "onViewCreated: ${this.touchPointX} , ${this.touchPointY}")
    }

    /** 사용자가 롱 클릭 모드에서 처음 터치한 좌표값 설정 - 화면 여백을 클릭하는걸 방지하기 위함 */
    fun setFirstTouchPoint(x: Double, y: Double) {
        this.touchFirstX = x
        this.touchFirstY = y
        Log.d("FragmentCamera1", "$this.touchFirstX : $this.touchFirstY")
    }

    /**
     * --------------------- 플로위 더블 탭 모드 ---------------------
     * */

    /** 사용자가 더블 탭모드에서 터치한 위치의 좌표 값 */
    var doubleTapPointX: Double = 0.0
    var doubleTapPointY: Double = 0.0
    var isScreenPointSave: Boolean = false

    /** camera mode 가 default 이고, 더블탭했을때 좌표값 설정 */
    fun setDoubleTapTouchPoint(x: Double, y: Double) {
        this.doubleTapPointX = x
        this.doubleTapPointY = y
    }

    /**
     * --------------------- 플로위 포커스 기능 ---------------------
     * */

    /** 포커스를 잡기 위해 터치한 좌표 값 */
    var touchFocusPointX: Float = 0f
    var touchFocusPointY: Float = 0f

    fun setFocusTouchPoint(glView: GLTextureView, x: Float, y: Float) {
        this.touchFocusPointX = x
        this.touchFocusPointY = y

        // 90도 회전하기때문에 x,y값을 바꾸어서 넣어준다.
        val rotateX = glView.width * this.touchFocusPointY / FlowyRenderer.adjustHeight
        val rotateY = FlowyRenderer.adjustHeight * this.touchFocusPointX / glView.width

        this.touchFocusPointX = rotateX
        this.touchFocusPointY = glView.height - rotateY

        Log.d("focusPoint", "${this.touchFocusPointX} : ${this.touchFocusPointY}")
    }

    /** 핀치줌 */
    var pinchFirstTouchX : Float = 0f
    var pinchFirstTouchY : Float = 0f
    var pinchSecondTouchX : Float = 0f
    var pinchSecondTouchY : Float = 0f
    var flowyPinchFlag : Boolean = false
}