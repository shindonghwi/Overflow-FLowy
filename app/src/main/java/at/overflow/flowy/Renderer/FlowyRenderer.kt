package at.overflow.flowy.Renderer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import at.overflow.flowy.Fragment.FragmentCamera.Companion.blackScreen
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceFlag
import at.overflow.flowy.Fragment.FragmentCamera.Companion.luminanceIndex
import at.overflow.flowy.Fragment.FragmentCamera.Companion.lensChangeFlag
import at.overflow.flowy.Fragment.FragmentCamera.Companion.touchDataUtil
import at.overflow.flowy.Fragment.FragmentCamera.Companion.userContrastData
import at.overflow.flowy.Provider.SurfaceTextureProvider
import at.overflow.flowy.Util.*
import at.overflow.flowy.View.FlowyGLTextureView
import at.overflow.flowy.View.GLTextureView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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


class FlowyRenderer(private val flowyGLTextureView: FlowyGLTextureView) : GLTextureView.Renderer,
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

    private lateinit var OPENGL_VERTICE: FloatArray
    private var cameraXAspectRatio: Int = -1
    private var varNDC: FloatArray = NDC_VERTICE

    /** 셰이더 코드 설정 */
    private lateinit var vertexCode: String
    private lateinit var fragmentCode: String

    /** 카메라의 방향을 셋팅하는 함수 : 전면 - 0, 후면 - 1 */
    private fun cameraDirectionSetting(cameraLensMode: Int) {
        if (cameraLensMode == 0) OPENGL_VERTICE = FRONT_OPENGL_VERTICE // 카메라 렌즈 전면
        else if (cameraLensMode == 1) OPENGL_VERTICE = BACK_OPENGL_VERTICE // 카메라 렌즈 후면
    }

    /** 화면의 중심점 및 왼, 오, 위, 아래 값 : 중심점 기준으로 사각형이 생긴다고 생각하면됨. */
    private var centerPointX = 0.0
    private var centerPointY = 0.0
    private var screenLeft = 0.0f
    private var screenRight = 0.0f
    private var screenBottom = 0.0f
    private var screenTop = 0.0f

    private var finalLeft = 0.0f
    private var finalRight = 0.0f
    private var finalBottom = 0.0f
    private var finalTop = 0.0f

    /** NDC 및 OPENGL 좌표계 설정 */
    private fun setNDCandOPENGL(cameraMode: String) {

//        Log.d("cameraMode", cameraMode)
//        Log.d("cameraSubMode", cameraSubMode)

        when {
            cameraMode == "default" -> {
                varNDC = NDC_VERTICE
            }
            cameraSubMode == "longClick" -> {
                modeFlowy()
            }
            cameraSubMode == "flowyDoubleTap" -> {
                modeDoubleTap()
            }
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

        // 카메라 전환시에는 텍스처 업데이트를 중단한다.
        if (lensChangeFlag) {

            // 텍스처가 업데이트 가능 할 때 업데이트한다.
            setCamera()
            lensChangeFlag = false
            mUpdateST = false
            return
        }
        if (!mUpdateST) {
            Log.d("updateAvailable", "$mUpdateST")
        } else {
            // 텍스처가 업데이트 가능 할 때 업데이트한다.
            textureUpdate()

            // 사용자 기기에 따른 화면 설정
            setScreenWindow()

            setNDCandOPENGL(cameraMode = cameraMode)

            // 프래그먼트 쉐이더 값 변경
            if (fragmentType == "default") {
                if (luminanceFlag) {
                    program = createProgram()
                    luminanceFlag = false
                }
                fShaderControlDefault()
            } else if (fragmentType == "luminance") {
                if (luminanceFlag) {
                    program = createProgram()
                    luminanceFlag = false
                }
                fShaderControlLuminance()
            }
            GLES20.glUseProgram(program)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        // 화면의 사이즈 저장
        screenWidth = flowyGLTextureView.width
        screenHeight = flowyGLTextureView.height
        Log.d("screenSize", "onSurfaceChanged: width : $width / height : $height")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        cameraLifecycle = createLifeCycle()

//        initTex()
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 카메라 셋팅
        setCamera()

        // 좌표계 설정
        setNDCandOPENGL(cameraMode = cameraMode)

        // 셰이더 프로그램 생성
        program = createProgram()

//        flowyGLTextureView.display.getRealSize(Point())
        mGLInit = true

    }

    override fun onSurfaceDestroyed(gl: GL10?) {

    }

    private fun setCamera() {
//         카메라 방향 셋팅
        cameraDirectionSetting(cameraLensMode = cameraLensMode)
        cameraXAspectRatio = screenSetAspectRatio() // 16:9 -> 1 [] 4:3 -> 0
        // 카메라 프리뷰 설정
        setUpCameraPreview(cameraLensMode = cameraLensMode, aspectRatio = cameraXAspectRatio)
    }

    private fun screenSetAspectRatio(): Int {

        val metrics = DisplayMetrics().also { flowyGLTextureView.display.getRealMetrics(it) }
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
        flowyGLTextureView.requestRender()
    }

    private fun setScreenWindow() {
        if (cameraXAspectRatio == 0) adjustHeight = (screenWidth * 3) / 4 // 4:3 인 경우
        else adjustHeight = (screenWidth * 16) / 9 // 16:9 인 경우

//        Log.d("adjustScreenSize", "adjust width : $screenWidth / height : $adjustHeight")
        GLES20.glViewport(
            0, (screenHeight - adjustHeight) / 2,
            screenWidth,
            adjustHeight
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
        synchronized(this) {
            if (freezeMode) {
                Log.d("freezeMode", "$freezeMode")
                // RenderMode를 Dirty로 설정했기에 requestRender를 요청하지 않으면 렌더링이 중단된다.
//                flowyGLTextureView.requestRender()
                return
            } else {
                try {
                    sfTexture?.updateTexImage()
                } catch (e: Exception) {
                    sfTexture?.attachToGLContext(textureArray[0])
                }
            }
        }
    }

    /** 셰이더 프로그램 생성 */
    private fun createProgram(): Int {
        this.vertexCode = LoadFile().shaderCodeRead(rawFile = "vertex", type = vertexType)
        this.fragmentCode = LoadFile().shaderCodeRead(rawFile = "fragment", type = fragmentType)

        var vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vshader, vertexCode)
        GLES20.glCompileShader(vshader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile vshader")
            Log.d(
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
            Log.d(
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
                surfaceTextureProvider =
                    SurfaceTextureProvider()
                previewBuilder = Preview.Builder()
                previewBuilder.setTargetAspectRatio(aspectRatio)
//                previewBuilder.setTargetResolution(Size(480,640))
                preview = previewBuilder.build()
                preview.setSurfaceProvider(
                    surfaceTextureProvider.createSurfaceTextureProvider(
                        object : SurfaceTextureProvider.SurfaceTextureCallback {
                            override fun onSurfaceTextureReady(
                                surfaceTexture: SurfaceTexture,
                                resolution: Size
                            ) {
                                sfTexture = surfaceTexture
                                sfTexture?.setOnFrameAvailableListener(this@FlowyRenderer)

                                Log.d("onSurfaceTextureReady", "$resolution")
                            }

                            override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                                Log.d("onSafeToRelease", "onSafeToRelease")
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(900)
                                    blackScreen.visibility = View.GONE
                                }
                            }
                        }
                    )
                )
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    cameraLifecycle, cameraSelector, preview
                )
            }
            , ContextCompat.getMainExecutor(THIS_CONTEXT)
        )

    }


    /** 카메라 수명주기  생성*/
    private fun createLifeCycle(): CustomLifecycle {
        Log.d("tetetest", "라이프 사이클 생성")
        val customLifecycle = CustomLifecycle()
        customLifecycle.doOnResume()
        customLifecycle.doOnStarted()
        return customLifecycle
    }

    /** --------------------------------------------- */
    /** 카메라 모드 ( FLowy, DoubleTab ) 등 */
    private fun modeFlowy() {
        val yMin = ((screenHeight - adjustHeight) / 2).toDouble()
        val yMax = (((screenHeight - adjustHeight) / 2) + adjustHeight).toDouble()

        // 터치를 안했을때는 기본 모드로 보여준다.
        if (!touchDataUtil.isTouching) {
            varNDC = NDC_VERTICE // 기본 모드로 설정한다.
        }
        // 화면 바깥을 터치했을때는, FLowy Zoom 모드가 아니라 기본모드로 보여준다.
        else if (touchDataUtil.touchFirstY <= yMin || touchDataUtil.touchFirstY >= yMax) {
            varNDC = NDC_VERTICE // 기본 모드로 설정한다.
        }
        // 카메라 미리보기가 보이는 부분을 터치했을때는 Flowy줌을 시작한다.
        else {

            // 화면 여백을 클릭했을때는 y값에 제한걸기 ( 여백으로 뻗어나가는걸 막는다. )
            if (touchDataUtil.touchPointY <= yMin) touchDataUtil.touchPointY = yMin
            if (touchDataUtil.touchPointY >= yMax) touchDataUtil.touchPointY = yMax

            // 스케일 설정 : 2f -> 1배, 4f -> 2배, 6f -> 3배
            val scale = 8.0f

            // 화면 비율 : 조정된 크기에 대한 전체 화면의 크기 : 1이상의 값이 나온다.
            var ratio = (screenHeight.toDouble() / adjustHeight.toDouble())
            Log.d("ratio", "ratio : $ratio // ")

            // 사용자가 터치한곳의 NDK 좌표를 구한다. ( -1 ~ 1 사이값임 )
            centerPointX = (touchDataUtil.touchPointX / screenWidth) * scale - (scale / 2.0)
            centerPointY =
                ((touchDataUtil.touchPointY / screenHeight) * scale - (scale / 2.0)) * ratio

            Log.d(
                "ndkPoint",
                "ndkPoint : centerPointX : $centerPointX // centerPointY : $centerPointY // "
            )

            // x의 좌표값을 뒤집는다.
            centerPointX *= -1

            screenLeft = (centerPointX.toFloat() - scale + scale / 2.0).toFloat()
            screenRight = (centerPointX.toFloat() + scale - scale / 2.0).toFloat()
            screenBottom = (centerPointY.toFloat() - scale + scale / 2.0).toFloat()
            screenTop = (centerPointY.toFloat() + scale - scale / 2.0).toFloat()

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

            finalLeft = screenLeft
            finalRight = screenRight
            finalBottom = screenBottom
            finalTop = screenTop

            varNDC = floatArrayOf(
                finalLeft, finalBottom, // left, bottom
                finalRight, finalBottom, // right, bottom
                finalLeft, finalTop, // left, top
                finalRight, finalTop // right, top
            )

            GLES20.glViewport(
                0, 0,
                screenWidth,
                screenHeight
            )
        }
    }

    var dragLeftMax: Double = 0.0
    var dragRightMax: Double = 0.0
    var dragBottomMax: Double = 0.0
    var dragTopMax: Double = 0.0
    var newScreenLeft: Float = 0.0f
    var newScreenRight: Float = 0.0f
    var newScreenBottom: Float = 0.0f
    var newScreenTop: Float = 0.0f

    private fun modeDoubleTap() {

        val yMin = ((screenHeight - adjustHeight) / 2).toDouble()
        val yMax = (((screenHeight - adjustHeight) / 2) + adjustHeight).toDouble()

        // 화면 바깥을 터치했을때는, FLowy Zoom 모드가 아니라 기본모드로 보여준다.
        if (touchDataUtil.doubleTapPointY <= yMin || touchDataUtil.doubleTapPointY >= yMax) {
            Log.d("adsasdaszxzxc", "1")
            varNDC = NDC_VERTICE // 기본 모드로 설정한다.
        } else {
            // 스케일 설정 : 2f -> 1배, 4f -> 2배, 6f -> 3배
            val scale = 8.0f

            // 화면 비율 : 조정된 크기에 대한 전체 화면의 크기 : 1이상의 값이 나온다.
            var ratio = (screenHeight.toDouble() / adjustHeight.toDouble())
            Log.d("ratio", "ratio : $ratio // ")

            /** 더블 탭을 처음 클릭한 경우에, 클릭한 지점을 확대해준다. */
            if (touchDataUtil.isDoubleTapFirstTouched) {
                Log.d("adsasdaszxzxc", "3")

                touchDataUtil.isDoubleTapFirstTouched = false // 더블탭 터치가 끝났다는걸 알린다.

                // 사용자가 터치한곳의 NDK 좌표를 구한다. ( -1 ~ 1 사이값임 )
                centerPointX = (touchDataUtil.doubleTapPointX / screenWidth) * scale - (scale / 2.0)
                centerPointY =
                    ((touchDataUtil.doubleTapPointY / screenHeight) * scale - (scale / 2.0)) * ratio

                centerPointX *= -1

                // 설정한 scale만큼 이미지를 확대한다.
                screenLeft = (centerPointX.toFloat() - scale + scale / 2.0).toFloat()
                screenRight = (centerPointX.toFloat() + scale - scale / 2.0).toFloat()
                screenBottom = (centerPointY.toFloat() - scale + scale / 2.0).toFloat()
                screenTop = (centerPointY.toFloat() + scale - scale / 2.0).toFloat()

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

                dragLeftMax = screenLeft.toDouble()
                dragRightMax = screenRight.toDouble()
                dragBottomMax = screenBottom.toDouble()
                dragTopMax = screenTop.toDouble()

                finalLeft = screenLeft
                finalRight = screenRight
                finalBottom = screenBottom
                finalTop = screenTop

                varNDC = floatArrayOf(
                    finalLeft, finalBottom, // left, bottom
                    finalRight, finalBottom, // right, bottom
                    finalLeft, finalTop, // left, top
                    finalRight, finalTop // right, top
                )

                return
            }
            /** 더블 탭을 하여, 확대된 이미지가 보이는 상태이다.
             * 여기서는 사용자의 터치포인터를 인식하여 사용자가 움직이는 곳으로 화면을 이동시켜줘야한다. */
            else {
                if (touchDataUtil.touchPointX != 0.0 && touchDataUtil.touchPointY != 0.0 && touchDataUtil.touchFirstX != 0.0 && touchDataUtil.touchFirstY != 0.0) {

                    if (!touchDataUtil.isScreenPointSave){
                        dragLeftMax = newScreenLeft.toDouble()
                        dragRightMax = newScreenRight.toDouble()
                        dragBottomMax = newScreenBottom.toDouble()
                        dragTopMax = newScreenTop.toDouble()
                        touchDataUtil.isScreenPointSave = !touchDataUtil.isScreenPointSave
                    }

                    val xDistance =
                        (touchDataUtil.touchFirstX - touchDataUtil.touchPointX) / screenWidth * scale / 2
                    val yDistance =
                        -(touchDataUtil.touchFirstY - touchDataUtil.touchPointY) / screenHeight * scale / 2

                    Log.d(
                        "bsdds", "" +
                                "touchFirstX : ${touchDataUtil.touchFirstX}  /  " +
                                "touchFirstY : ${touchDataUtil.touchFirstY}  /  " +
                                "touchPointX : ${touchDataUtil.touchPointX}  /  " +
                                "touchPointY : ${touchDataUtil.touchPointY}  /  "  +
                                "xDistance : ${xDistance}  /  " +
                                "yDistance : ${yDistance}  /  "
                    )

                    Log.d(
                        "bsdzxcxcbds", "" +
                                "dragLeftMax : ${dragLeftMax}  /  " +
                                "dragRightMax : ${dragRightMax}  /  " +
                                "dragBottomMax : ${dragBottomMax}  /  " +
                                "dragTopMax : ${dragTopMax}  /  "
                    )

                    newScreenLeft = (screenLeft + xDistance).toFloat()
                    newScreenRight = (screenRight + xDistance).toFloat()
                    newScreenBottom = (screenBottom + yDistance).toFloat()
                    newScreenTop = (screenTop + yDistance).toFloat()

                    if (newScreenLeft <= dragLeftMax + xDistance) {
                        newScreenLeft = (dragLeftMax + xDistance).toFloat()
                        newScreenRight = scale - newScreenLeft
                    }
                    if (newScreenRight >= dragRightMax + xDistance) {
                        newScreenRight = (dragRightMax + xDistance).toFloat()
                        newScreenLeft = -(scale - newScreenRight)
                    }
                    if (newScreenBottom <= dragBottomMax + yDistance) {
                        newScreenBottom = (dragBottomMax + yDistance).toFloat()
                        newScreenTop = scale - newScreenBottom
                    }
                    if (newScreenTop >= dragTopMax + yDistance) {
                        newScreenTop = (dragTopMax + yDistance).toFloat()
                        newScreenBottom = -(scale - newScreenTop)
                    }

                    Log.d(
                        "bsdzxcds", "" +
                                "newScreenLeft : ${newScreenLeft}  /  " +
                                "newScreenRight : ${newScreenRight}  /  " +
                                "newScreenBottom : ${newScreenBottom}  /  " +
                                "newScreenTop : ${newScreenTop}  /  "
                    )

                    Log.d(
                        "sdfklzzxczxccnsafd", "" +
                                "screenLeft : ${screenLeft}  /  " +
                                "screenRight : ${screenRight}  /  " +
                                "screenBottom : ${screenBottom}  /  " +
                                "screenTop : ${screenTop}  /  "
                    )

                    // 화면 바깥으로 안나가게 막는다.
                    if (newScreenRight <= 1) {
                        newScreenRight = 1f; newScreenLeft = -scale + newScreenRight
                    }
                    if (newScreenLeft >= -1) {
                        newScreenLeft = -1f; newScreenRight = scale + newScreenLeft
                    }
                    if (newScreenTop <= 1) {
                        newScreenTop = 1f; newScreenBottom = -scale + newScreenTop
                    }
                    if (newScreenBottom >= -1) {
                        newScreenBottom = -1f; newScreenTop = scale + newScreenBottom
                    }

                    Log.d(
                        "bszxczxczxcdzxcds", "" +
                                "newScreenLeft : ${newScreenLeft}  /  " +
                                "newScreenRight : ${newScreenRight}  /  " +
                                "newScreenBottom : ${newScreenBottom}  /  " +
                                "newScreenTop : ${newScreenTop}  /  "
                    )

                    finalLeft = newScreenLeft
                    finalRight = newScreenRight
                    finalBottom = newScreenBottom
                    finalTop = newScreenTop

                    varNDC = floatArrayOf(
                        finalLeft, finalBottom, // left, bottom
                        finalRight, finalBottom, // right, bottom
                        finalLeft, finalTop, // left, top
                        finalRight, finalTop // right, top
                    )
                    return
                }
            }
            GLES20.glViewport(
                0, (screenHeight - adjustHeight) / 2,
                screenWidth,
                adjustHeight
            )
        }
    }
    /** --------------------------------------------- */

    /** fragment Shader default : rgb 조작 */
    private fun fShaderControlDefault() {
        val ph = GLES20.glGetAttribLocation(program, "vPosition") // vertex shader
        val tch = GLES20.glGetAttribLocation(program, "vTexCoord") // vertex shader

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex)
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord)
        GLES20.glEnableVertexAttribArray(ph)
        GLES20.glEnableVertexAttribArray(tch)

        // 밝기 : 0.5 ~ 2
        val brightHandle = GLES20.glGetUniformLocation(program, "bright")
        val conversionBright =
            (0.5 + 1.5 * touchDataUtil.brightSeekbarProgress.toDouble() / 100.toDouble()).toFloat()
        GLES20.glUniform1f(brightHandle, conversionBright)

        // 대비 : 0 ~ 2
        val contrastHandle = GLES20.glGetUniformLocation(program, "contrast")
        val conversionContrast =
            (2 * touchDataUtil.contrastSeekbarProgress.toDouble() / 100.toDouble()).toFloat()
        GLES20.glUniform1f(contrastHandle, conversionContrast)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFlush()
    }

    /** fragment Shader default : rgb 조작 */
    private fun fShaderControlLuminance() {
        val ph = GLES20.glGetAttribLocation(program, "vPosition") // vertex shader
        val tch = GLES20.glGetAttribLocation(program, "vTexCoord") // vertex shader

        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, pVertex)
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord)
        GLES20.glEnableVertexAttribArray(ph)
        GLES20.glEnableVertexAttribArray(tch)

        val testColorHandle1 = GLES20.glGetUniformLocation(program, "reversalColor1")
        val testColorHandle2 = GLES20.glGetUniformLocation(program, "reversalColor2")

        var reversalColor1: FloatArray
        var reversalColor2: FloatArray

        // 사용자가 고대비 버튼을 너무  빨리누를경우 인덱스 에러가 난다. 예외처리
        try {
            reversalColor1 =
                colorIntToFloatArray(userContrastData[luminanceIndex - 1].contrastLeftImage!!)
            reversalColor2 =
                colorIntToFloatArray(userContrastData[luminanceIndex - 1].contrastRightImage!!)
        } catch (e: ArrayIndexOutOfBoundsException) {
            reversalColor1 = colorIntToFloatArray(userContrastData[0].contrastLeftImage!!)
            reversalColor2 = colorIntToFloatArray(userContrastData[0].contrastRightImage!!)
        }

        GLES20.glUniform4fv(testColorHandle1, 1, reversalColor1, 0)
        GLES20.glUniform4fv(testColorHandle2, 1, reversalColor2, 0)

        // 밝기 : 0.5 ~ 2
        val brightHandle = GLES20.glGetUniformLocation(program, "bright")
        val conversionBright =
            (0.5 + 1.5 * touchDataUtil.brightSeekbarProgress.toDouble() / 100.toDouble()).toFloat()
        GLES20.glUniform1f(brightHandle, conversionBright)

        // 대비 : 0 ~ 2
        val contrastHandle = GLES20.glGetUniformLocation(program, "contrast")
        val conversionContrast =
            (2 * touchDataUtil.contrastSeekbarProgress.toDouble() / 100.toDouble()).toFloat()
        GLES20.glUniform1f(contrastHandle, conversionContrast)

        // binaryType 타입 전달 : 1.0 - On / 0.0 - Off
        val binaryFlagHandle = GLES20.glGetUniformLocation(program, "binaryType")
        val binaryType = if (binaryFlag) 1.0f else 0.0f
        GLES20.glUniform1f(binaryFlagHandle, binaryType)

        // inverseType 타입 전달 : 1.0 - On / 0.0 - Off
        val inverseFlagHandle = GLES20.glGetUniformLocation(program, "inverseType")
        val inverseType = if (inverseFlag) 1.0f else 0.0f
        GLES20.glUniform1f(inverseFlagHandle, inverseType)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sTexture"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glFlush()
    }

    /** Color 값은 Int인데 fragment에 적용하기 위해 floatArray로 바꾸는 함수 */
    private fun colorIntToFloatArray(color: Int): FloatArray {

        val red =
            (((ContextCompat.getColor(THIS_CONTEXT!!, color) shr 16) and 0xff).toDouble()).toFloat()
        val green =
            (((ContextCompat.getColor(THIS_CONTEXT!!, color) shr 8) and 0xff).toDouble()).toFloat()
        val blue = ((ContextCompat.getColor(THIS_CONTEXT!!, color) and 0xff).toDouble()).toFloat()

        return floatArrayOf(red, green, blue, 1.0f)
    }

    companion object {
        var screenWidth: Int = 0
        var screenHeight: Int = 0
        var adjustWidth: Int = 0
        var adjustHeight: Int = 0
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        var camera: Camera? = null
        lateinit var previewBuilder: Preview.Builder
        lateinit var cameraLifecycle: CustomLifecycle
        var mUpdateST = false
    }

}
