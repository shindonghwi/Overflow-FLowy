package com.overflow.flowy.Renderer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.overflow.flowy.Fragment.FragmentCamera.Companion.doubleTapPointX
import com.overflow.flowy.Fragment.FragmentCamera.Companion.doubleTapPointY
import com.overflow.flowy.Fragment.FragmentCamera.Companion.isDoubleTapFirstTouched
import com.overflow.flowy.Fragment.FragmentCamera.Companion.isTouching
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchFirstX
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchFirstY
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchPointX
import com.overflow.flowy.Fragment.FragmentCamera.Companion.touchPointY
import com.overflow.flowy.MainActivity.Companion.initImage
import com.overflow.flowy.MainActivity.Companion.iv
import com.overflow.flowy.Provider.SurfaceTextureProvider
import com.overflow.flowy.Util.*
import com.overflow.flowy.View.FlowyGLSurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ExecutionException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FlowyRenderer(private val flowyGLSurfaceView: FlowyGLSurfaceView) : GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {

    private var pVertex: FloatBuffer =
        ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var pTexCoord: FloatBuffer =
        ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private var sfTexture: SurfaceTexture? = null
    private var program = 0
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var surfaceTextureProvider: SurfaceTextureProvider
    private lateinit var preview: Preview
    private var mGLInit = false
    private var mUpdateST = false
    private lateinit var camera: Camera

    private lateinit var OPENGL_VERTICE: FloatArray
    private var cameraXAspectRatio: Int = -1

    private var adjustWidth: Int = 0
    private var adjustHeight: Int = 0
    private var varNDC: FloatArray = NDC_VERTICE

    /** 셰이더 코드 설정 */
    private lateinit var vertexType: String
    private lateinit var fragmentType: String
    private lateinit var vertexCode: String
    private lateinit var fragmentCode: String

    /** 카메라의 방향을 셋팅하는 함수 : 전면 - 0, 후면 - 1 */
    private fun cameraDirectionSetting(cameraLensMode: Int) {
        if (cameraLensMode == 0) OPENGL_VERTICE = FRONT_OPENGL_VERTICE // 카메라 렌즈 전면
        else if (cameraLensMode == 1) OPENGL_VERTICE = BACK_OPENGL_VERTICE // 카메라 렌즈 후면
    }

    var x = 0.0
    var y = 0.0
    var screenLeft = 0.0f
    var screenRight = 0.0f
    var screenBottom = 0.0f
    var screenTop = 0.0f

    /** NDC 및 OPENGL 좌표계 설정 */
    private fun setNDCandOPENGL(cameraMode: String) {

        // 화면 중간에 빨간색 점
        CoroutineScope(Dispatchers.Main).launch {
            initImage.bringToFront()
            initImage.x = (screenWidth / 2).toFloat()
            initImage.y = (screenHeight / 2).toFloat()
        }

        if (cameraMode == "default"){
            varNDC = NDC_VERTICE
        }
        else if (cameraSubMode == "longClick") {
            modeFlowy()
        }
        else if (cameraSubMode == "flowyDoubleTap"){
            modeDoubleTap()
        }


        /** NDC 좌표계 설정 */
        pVertex.put(varNDC)
        pVertex.position(0)

        /** OPENGL 좌표계 설정 */
        pTexCoord.put(OPENGL_VERTICE)
        pTexCoord.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!mGLInit) return
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 텍스처가 업데이트 가능 할 때 업데이트한다.
        textureUpdate()

        // 사용자 기기에 따른 화면 설정
        setScreenWindow()

        setNDCandOPENGL(cameraMode = cameraMode)

        // 프로그램 연결
        GLES20.glUseProgram(program)

        // 프래그먼트 쉐이더 조절
        if (fragmentType == "default") FShaderControlDefault()
        else if (fragmentType == "luminance") FShaderControlLuminance()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        // 화면의 사이즈 저장
        screenWidth = flowyGLSurfaceView.width
        screenHeight = flowyGLSurfaceView.height
        Log.d("screenSize", "onSurfaceChanged: width : $width / height : $height")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        initTex()
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        cameraLensMode = 1 // 카메라 렌즈 모드 설정 ( 전면 : 0, 후면 : 1)

        // 카메라 방향 셋팅
        cameraDirectionSetting(cameraLensMode = cameraLensMode)

        // 좌표계 설정
        setNDCandOPENGL(cameraMode = cameraMode)

        cameraXAspectRatio = screenSetAspectRatio() // 16:9 -> 1 [] 4:3 -> 0

        // 카메라 프리뷰 설정
        setUpCameraPreview(cameraLensMode = cameraLensMode, aspectRatio = screenSetAspectRatio())

        // 셰이터 프로그램 타입 설정
        setProgramCode(vertexType = "default", fragmentType = "default")

        // 셰이더 프로그램 생성
        program = createProgram(vertexCode = vertexCode, fragmentCode = fragmentCode)

//        flowyGLSurfaceView.display.getRealSize(Point())
        mGLInit = true

    }

    private fun screenSetAspectRatio(): Int {

        val metrics = DisplayMetrics().also { flowyGLSurfaceView.display.getRealMetrics(it) }
        val previewRatio = max(metrics.widthPixels, metrics.heightPixels).toDouble() / min(
            metrics.widthPixels,
            metrics.heightPixels
        )
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        mUpdateST = true
        flowyGLSurfaceView.requestRender()
    }

    private fun setScreenWindow() {
        if (cameraXAspectRatio == 0) adjustHeight = (screenWidth * 3) / 4 // 4:3 인 경우
        else adjustHeight = (screenWidth * 16) / 9 // 16:9 인 경우

        Log.d("adjustScreenSize", "adjust width : $screenWidth / height : $adjustHeight")
        GLES20.glViewport(0, (screenHeight - adjustHeight) / 2, screenWidth, adjustHeight)
    }

    private fun initTex() {
        GLES20.glGenTextures(textureArray[0], textureArray, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureArray[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
    }

    fun onResume() {
        mUpdateST = true
    }

    fun onPause() {
        mGLInit = false
        mUpdateST = false
    }

    /** 텍스처가 업데이트 가능 할 때 업데이트 한다. */
    private fun textureUpdate() {
//        synchronized(this) {
//            if (mUpdateST) {
        try {
            sfTexture?.updateTexImage()
        } catch (e: Exception) {
            sfTexture?.attachToGLContext(textureArray[0])
        }
        mUpdateST = false
//            }
//        }
    }

    /** 셰이더 프로그램 타입 설정 */
    private fun setProgramCode(vertexType: String, fragmentType: String) {
        this.vertexType = vertexType
        this.fragmentType = fragmentType
        this.vertexCode = LoadFile().shaderCodeRead(rawFile = "vertex", type = vertexType)
        this.fragmentCode = LoadFile().shaderCodeRead(rawFile = "fragment", type = fragmentType)
    }

    /** 셰이더 프로그램 생성 */
    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        var vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vshader, vertexCode)
        GLES20.glCompileShader(vshader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile vshader")
            Log.v(
                "Shader",
                "Could not compile vshader:" + GLES20.glGetShaderInfoLog(vshader)
            )
            GLES20.glDeleteShader(vshader)
            vshader = 0
        }
        var fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fshader, fragmentCode)
        GLES20.glCompileShader(fshader)
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile fshader")
            Log.v(
                "Shader",
                "Could not compile fshader:" + GLES20.glGetShaderInfoLog(fshader)
            )
            GLES20.glDeleteShader(fshader)
            fshader = 0
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vshader)
        GLES20.glAttachShader(program, fshader)
        GLES20.glLinkProgram(program)
        return program
    }

    /** 카메라 프리뷰 설정 */
    private fun setUpCameraPreview(cameraLensMode: Int, aspectRatio: Int) {

        // 카메라 수명주기를 LifecycleOwner에 바인딩 할 수 있는 클래스임.
        // CameraX 클래스가 ProcessCameraProvider 객체로 대체됨.
        // ProcessCameraProvider를 통해서 CameraProvider 객체를 얻을 수 있다고함.
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(THIS_CONTEXT!!)
        cameraProviderFuture.addListener(
            Runnable {
                try {
                    cameraProvider = cameraProviderFuture.get()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                cameraSelector = CameraSelector.Builder().requireLensFacing(cameraLensMode).build()
                surfaceTextureProvider = SurfaceTextureProvider()
                val builder = Preview.Builder()
//                builder.setTargetResolution(resolution)
                builder.setTargetAspectRatio(aspectRatio)
                preview = builder.build()
                preview.setSurfaceProvider(
                    surfaceTextureProvider.createSurfaceTextureProvider(
                        object : SurfaceTextureProvider.SurfaceTextureCallback {
                            override fun onSurfaceTextureReady(
                                surfaceTexture: SurfaceTexture,
                                resolution: Size
                            ) {
                                sfTexture = surfaceTexture
                                sfTexture?.setOnFrameAvailableListener(this@FlowyRenderer)
                                Log.d("onSurfaceTextureReady", "onSurfaceTextureReady")
                            }

                            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {}
                        }
                    )
                )
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(createLifeCycle(), cameraSelector, preview)
            }
            , ContextCompat.getMainExecutor(THIS_CONTEXT)
        )
    }

    /** 카메라 수명주기  생성*/
    private fun createLifeCycle(): CustomLifecycle {
        val customLifecycle = CustomLifecycle()
        customLifecycle.doOnResume()
        customLifecycle.doOnStart()
        return customLifecycle
    }

    /** --------------------------------------------- */
    /** 카메라 모드 ( FLowy, DoubleTab ) 등 */
    private fun modeFlowy() {
        val yMin = ((screenHeight - adjustHeight) / 2).toDouble()
        val yMax = (((screenHeight - adjustHeight) / 2) + adjustHeight).toDouble()

        // 터치를 안했을때는 기본 모드로 보여준다.
        if (!isTouching) {
            varNDC = NDC_VERTICE // 기본 모드로 설정한다.
        }
        // 화면 바깥을 터치했을때는, FLowy Zoom 모드가 아니라 기본모드로 보여준다.
        else if (touchFirstY <= yMin || touchFirstY >= yMax) {
            varNDC = NDC_VERTICE // 기본 모드로 설정한다.
        }
        // 카메라 미리보기가 보이는 부분을 터치했을때는 Flowy줌을 시작한다.
        else {

            // 화면 여백을 클릭했을때는 y값에 제한걸기 ( 여백으로 뻗어나가는걸 막는다. )
            if (touchPointY <= yMin) touchPointY = yMin
            if (touchPointY >= yMax) touchPointY = yMax

            // 스케일 설정 : 2f -> 1배, 4f -> 2배, 6f -> 3배
            val scale = 4.0f

            // 화면 비율 : 조정된 크기에 대한 전체 화면의 크기 : 1이상의 값이 나온다.
            var ratio = (screenHeight.toDouble() / adjustHeight.toDouble())
            Log.d("ratio", "ratio : $ratio // ")

            // 사용자가 터치한곳의 NDK 좌표를 구한다. ( -1 ~ 1 사이값임 )
            var x = (touchPointX / screenWidth) * scale - (scale / 2.0)
            var y = ((touchPointY / screenHeight) * scale - (scale / 2.0)) * ratio

            Log.d("ndkPoint", "ndkPoint : x : $x // y : $y // ")

            // x의 좌표값을 뒤집는다.
            x *= -1

            screenLeft = (x.toFloat() - scale + scale / 2.0).toFloat()
            screenRight = (x.toFloat() + scale - scale / 2.0).toFloat()
            screenBottom = (y.toFloat() - scale + scale / 2.0).toFloat()
            screenTop = (y.toFloat() + scale - scale / 2.0).toFloat()

            // 화면 바깥으로 안나가게 막는다.
            if (screenRight <= 1) {
                screenRight = 1f; screenLeft = -scale + screenRight
            }
            if (screenLeft >= -1) {
                screenLeft = -1f; screenRight = scale + screenLeft
            }
            if (screenTop <= 1) {
                screenTop = 1f; screenBottom = -scale + screenTop
            }
            if (screenBottom >= -1) {
                screenBottom = -1f; screenTop = scale + screenBottom
            }

            varNDC = floatArrayOf(
                screenLeft, screenBottom, // left, bottom
                screenRight, screenBottom, // right, bottom
                screenLeft, screenTop, // left, top
                screenRight, screenTop // right, top
            )

            // 사용자가 터치한 부분을 점 찍는다.
            CoroutineScope(Dispatchers.Main).launch {
                iv.bringToFront()
                iv.x = touchPointX.toFloat()
                iv.y = touchPointY.toFloat()
                Log.d("userTouchPoint", iv.x.toString() + " :: " + iv.y.toString())
            }
            GLES20.glViewport(0, 0, screenWidth, screenHeight)
        }
    }

    private fun modeDoubleTap(){
        val yMin = ((screenHeight - adjustHeight) / 2).toDouble()
        val yMax = (((screenHeight - adjustHeight) / 2) + adjustHeight).toDouble()

        // 화면 바깥을 터치했을때는, FLowy Zoom 모드가 아니라 기본모드로 보여준다.
        if (doubleTapPointY <= yMin || doubleTapPointY >= yMax) {
            varNDC = NDC_VERTICE // 기본 모드로 설정한다.
        }
        else{
            // 스케일 설정 : 2f -> 1배, 4f -> 2배, 6f -> 3배
            val scale = 6.0f

            // 화면 비율 : 조정된 크기에 대한 전체 화면의 크기 : 1이상의 값이 나온다.
            var ratio = (screenHeight.toDouble() / adjustHeight.toDouble())
            Log.d("ratio", "ratio : $ratio // ")

            /** 더블 탭을 처음 클릭한 경우에, 클릭한 지점을 확대해준다. */
            if ( isDoubleTapFirstTouched ){
                isDoubleTapFirstTouched = false // 더블탭 터치가 끝났다는걸 알린다.

                // 사용자가 터치한곳의 NDK 좌표를 구한다. ( -1 ~ 1 사이값임 )
                x = (doubleTapPointX / screenWidth) * scale - (scale / 2.0)
                y = ((doubleTapPointY / screenHeight) * scale - (scale / 2.0)) * ratio

                x *= -1

                // 설정한 scale만큼 이미지를 확대한다.
                screenLeft = (x.toFloat() - scale + scale / 2.0).toFloat()
                screenRight = (x.toFloat() + scale - scale / 2.0).toFloat()
                screenBottom = (y.toFloat() - scale + scale / 2.0).toFloat()
                screenTop = (y.toFloat() + scale - scale / 2.0).toFloat()

                // 화면 바깥으로 안나가게 막는다.
                if (screenRight <= 1) {
                    screenRight = 1f; screenLeft = -scale + screenRight
                }
                if (screenLeft >= -1) {
                    screenLeft = -1f; screenRight = scale + screenLeft
                }
                if (screenTop <= 1) {
                    screenTop = 1f; screenBottom = -scale + screenTop
                }
                if (screenBottom >= -1) {
                    screenBottom = -1f; screenTop = scale + screenBottom
                }

            }
            /** 더블 탭을 하여, 확대된 이미지가 보이는 상태이다.
             * 여기서는 사용자의 터치포인터를 인식하여 사용자가 움직이는 곳으로 화면을 이동시켜줘야한다. */
            else{
                val scrollSpeed = 20 // 값을 올릴수록 스크롤 속도가 느려진다.

                if (touchPointX != 0.0 && touchPointY != 0.0 && touchFirstX != 0.0 && touchFirstY != 0.0){

                    // 사용자가 손가락으로 화면을 이동하는데, 얼마나 이동했는지 구하는 식이다.
                    var moveX = touchPointX - touchFirstX
                    moveX = (moveX / screenWidth) * scale / scrollSpeed
                    var moveY = touchPointY - touchFirstY
                    moveY = (moveY / screenHeight) * scale / scrollSpeed

                    Log.d("touchPointXY",   "c : $touchPointX : $touchFirstX")

                    // 설정한 scale만큼 이미지를 확대한다.
                    screenLeft = (x.toFloat() - scale + scale / 2.0).toFloat() - moveX.toFloat()
                    screenRight = (x.toFloat() + scale - scale / 2.0).toFloat() - moveX.toFloat()
                    screenBottom = (y.toFloat() - scale + scale / 2.0).toFloat() + moveY.toFloat()
                    screenTop = (y.toFloat() + scale - scale / 2.0).toFloat() + moveY.toFloat()

                    // 화면 바깥으로 안나가게 막는다.
                    if (screenRight <= 1) {
                        screenRight = 1f; screenLeft = -scale + screenRight
                    }
                    if (screenLeft >= -1) {
                        screenLeft = -1f; screenRight = scale + screenLeft
                    }
                    if (screenTop <= 1) {
                        screenTop = 1f; screenBottom = -scale + screenTop
                    }
                    if (screenBottom >= -1) {
                        screenBottom = -1f; screenTop = scale + screenBottom
                    }

                    // 사용자가 움직인 곳으로 화면을 이동시켜준다.
                    x = ((screenLeft + screenRight) / 2).toDouble()
                    y = ((screenBottom + screenTop) / 2).toDouble()
                }
            }

            varNDC = floatArrayOf(
                screenLeft, screenBottom, // left, bottom
                screenRight, screenBottom, // right, bottom
                screenLeft, screenTop, // left, top
                screenRight, screenTop // right, top
            )
            GLES20.glViewport(0, 0, screenWidth, screenHeight)
        }
    }

    /** --------------------------------------------- */


    /** fragment Shader default : rgb 조작 */
    private fun FShaderControlDefault() {
        val ph = GLES20.glGetAttribLocation(program, "vPosition") // vertex shader
        val tch = GLES20.glGetAttribLocation(program, "vTexCoord") // vertex shader

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex)
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord)
        GLES20.glEnableVertexAttribArray(ph)
        GLES20.glEnableVertexAttribArray(tch)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFlush()
    }

    /** fragment Shader default : rgb 조작 */
    private fun FShaderControlLuminance() {
        val ph = GLES20.glGetAttribLocation(program, "vPosition") // vertex shader
        val tch = GLES20.glGetAttribLocation(program, "vTexCoord") // vertex shader

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex)
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord)
        GLES20.glEnableVertexAttribArray(ph)
        GLES20.glEnableVertexAttribArray(tch)

        val testColorHandle1 = GLES20.glGetUniformLocation(program, "vColor1")
        val testColorHandle2 = GLES20.glGetUniformLocation(program, "vColor2")
        val color1 = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
        val color2 = floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f)
        GLES20.glUniform4fv(testColorHandle1, 1, color1, 0)
        GLES20.glUniform4fv(testColorHandle2, 1, color2, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFlush()
    }

    companion object {
        var screenWidth: Int = 0
        var screenHeight: Int = 0
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}