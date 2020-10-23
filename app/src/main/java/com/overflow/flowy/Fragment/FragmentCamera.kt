package com.overflow.flowy.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.overflow.flowy.R
import com.overflow.flowy.Util.THIS_CONTEXT
import com.overflow.flowy.Util.cameraMode
import com.overflow.flowy.Util.flowyMode
import com.overflow.flowy.View.FlowyGLSurfaceView


class FragmentCamera : Fragment(), View.OnClickListener {

    private lateinit var glSurfaceView: FlowyGLSurfaceView
    private lateinit var flowyZoomBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        THIS_CONTEXT = context
        glSurfaceView = view.findViewById(R.id.glSurfaceView)
        flowyZoomBtn = view.findViewById(R.id.flowyZoomBtn)

        setClickListener() // 클릭 리스너 설정
        screenTouchListener() // 터치 리스너 설정

    }

    fun setTouchPoint(x: Double, y: Double) {
        touchPointX = x
        touchPointY = y
        Log.d("touch", "onViewCreated: $touchPointX , $touchPointY")
    }

    fun setFirstTouchPoint(x: Double, y: Double) {
        touchFirstX = x
        touchFirstY = y
    }

    /** 클릭 리스너 관리 */
    private fun setClickListener() {
        flowyZoomBtn.setOnClickListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {

        glSurfaceView.setOnTouchListener(object: View.OnTouchListener {
            private val gestureDetector = GestureDetector(context, object:GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e:MotionEvent):Boolean {

                    if (cameraMode == "default"){
                        Log.d("TEST", "onDoubleTap")
                    }
                    return super.onDoubleTap(e)
                }
                // 여기에 필요한 콜백 메서드를 추가 할 수 있다. override...

            })
            override fun onTouch(v:View, event:MotionEvent):Boolean {

                // 플로위 모드일때
                if (flowyMode && event.action == MotionEvent.ACTION_MOVE) {

                    // 첫번째로 터치한 좌표를 받아 firstTouchX , Y 에 할당한다. 화면 여백을 클릭하는걸 방지 하기 위함.
                    if ( touchFirstX == 0.0 && touchFirstY == 0.0 )
                        setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 터치한 포인트를 기록한다.
                    setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 현재 상태를 터치중으로 변경한다.
                    isTouching = true
                }

                // 플로위 모드가 아닐때
                else {
                    // 터치한 포인트를 0,0 으로 초기화한다.
                    setTouchPoint(0.0, 0.0)
                    // 첫번째로 터치한 포인트를 0,0으로 초기화한다.
                    setFirstTouchPoint(0.0, 0.0)
                    // 현재 상태를 터치중 아님으로 변경한다.
                    isTouching = false
                }
                gestureDetector.onTouchEvent(event)
                return true
            }
        })
    }

    /** 클릭 이벤트 처리 */
    override fun onClick(v: View) {

        when (v.id) {
            // 플로위 줌을 사용 여부를 변경한다.
            R.id.flowyZoomBtn -> {
                if (!flowyMode) {
                    flowyMode = !flowyMode
                    cameraMode = "flowy"
                } else {
                    flowyMode = !flowyMode
                    cameraMode = "default"
                }
            }
        }
    }

    companion object {
        var touchPointX: Double = 0.0
        var touchPointY: Double = 0.0
        var isTouching: Boolean = false
        var touchFirstX : Double = 0.0
        var touchFirstY : Double = 0.0

    }
}