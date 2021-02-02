package at.overflow.flowy.Fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.overflow.flowy.Adapter.AdapterBrightShadeControl
import at.overflow.flowy.DTO.ContrastData
import at.overflow.flowy.Interface.RetrofitAPI
import at.overflow.flowy.MainActivity
import at.overflow.flowy.MainActivity.Companion.pref
import at.overflow.flowy.MainActivity.Companion.prefEditor
import at.overflow.flowy.R
import at.overflow.flowy.Renderer.FlowyRenderer.Companion.camera
import at.overflow.flowy.Renderer.FlowyRenderer.Companion.sfTexture
import at.overflow.flowy.Renderer.VideoEncoderThread
import at.overflow.flowy.Util.*
import at.overflow.flowy.View.FlowyGLTextureView
import at.overflow.flowy.WebRTC.AppSdpObserver
import at.overflow.flowy.WebRTC.PeerConnectionObserver
import at.overflow.flowy.WebRTC.RTCClient
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.gson.JsonParser
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import me.amryousef.webrtc_demo.SignallingClient
import me.amryousef.webrtc_demo.SignallingClientListener
import org.json.JSONObject
import org.webrtc.*
import tech.thdev.mediacodecexample.video.VideoDecodeThread
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class FragmentCamera : Fragment(), View.OnClickListener{

    /** TEST START*/

    private lateinit var webSocketConnectBtn : Button

    /** TEST END */

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var alertToast: Toast

    private lateinit var glTextureView: FlowyGLTextureView // 카메라 미리보기가 나올 화면
    private var flowyZoomLongClickEvent: Boolean = false // 롱클릭 이벤트 콜백을 위한 변수, 이벤트 발생시 플로위 줌 시작
    private var pinchZoomFlag: Boolean = true
    private var pinchZoomFinishCallback: Boolean = false

    private lateinit var tm_layout: View
    private lateinit var top_f_layout: FrameLayout
    private lateinit var bottom_f_layout: FrameLayout
    private lateinit var bm_layout: View
    private lateinit var ps_layout: View
    private lateinit var bs_layout: View

    /** 메뉴 버튼 */
    private lateinit var focusToggleBtn: ToggleButton
    private lateinit var flashToggleBtn: ToggleButton
    private lateinit var lensChangeToggleBtn: ToggleButton
    private lateinit var flowyZoomToggleBtn: ToggleButton
    private lateinit var mirroringToggleBtn: ToggleButton
    private lateinit var menuToggleBtn: ToggleButton

    private lateinit var flowyCastToggleBtn: ToggleButton
    private lateinit var freezeToggleBtn: ToggleButton
    private lateinit var controlToggleBtn: ToggleButton
    private lateinit var controlChildToggleBtn: ToggleButton
    private lateinit var shareImgBtn: ImageButton
    private lateinit var defaultColorImgView: ImageButton
    private lateinit var binaryToggleBtn: ToggleButton
    private lateinit var inverseToggleBtn: ToggleButton

    /** 시크바 */
    private lateinit var pinchZoomSeekbar: SeekBar
    private lateinit var pinchZoomMinusImgBtn: ImageButton
    private lateinit var pinchZoomPlusImgBtn: ImageButton
    private lateinit var brightSeekbar: SeekBar
    private lateinit var contrastSeekbar: SeekBar

    /** 광고 */
    private lateinit var bannerAdView: AdView
    private lateinit var bannerVersaAD: ImageView

    private lateinit var contrastItemRecyclerView: RecyclerView
    private lateinit var brightShadeAdapter: AdapterBrightShadeControl

    private var deviceSensorDirection = 0f
    private var threePointClickFlag: Boolean = true

    private lateinit var videoDecoder: VideoDecodeThread
    var videoFlag: Boolean = false

    /** 프래그먼트 인스턴스 */
    fun newInstance(): FragmentCamera {
        Log.d("newInstance", "카메라 인스턴스 생성")
        return FragmentCamera()
    }

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
        alertToast = Toast(context)

        idInit(view = view)
        setClickListener() // 클릭 리스너 설정
        screenTouchListener() // 터치 리스너 설정
        pinchZoomListener() // 핀치줌 리스너 설정
        loadLuminanceData() // 사용자의 대비 데이터 가져오기

        // 리니어 레이아웃 매니저 설정 및 어댑터 연결
        contrastItemRecyclerView.layoutManager =
            LinearLayoutManager(THIS_CONTEXT, LinearLayoutManager.HORIZONTAL, false)
        contrastItemRecyclerView.adapter = brightShadeAdapter

    }

    /** layout id 초기화하는 공간 */
    private fun idInit(view: View) {

        /** TEST START */
        webSocketConnectBtn = view.findViewById(R.id.webSocketConnectBtn)
        /** TEST END */

        pref = THIS_CONTEXT!!.getSharedPreferences("flowyToggleBtnStatus", Context.MODE_PRIVATE)
        prefEditor = pref.edit()

        // 부모 레이아웃
        rootLayout = view.findViewById(R.id.rootLayout)
        top_f_layout = view.findViewById(R.id.top_f_layout)
        bottom_f_layout = view.findViewById(R.id.bottom_f_layout)
        tm_layout = view.findViewById(R.id.tm_layout)
        bm_layout = view.findViewById(R.id.bm_layout)
        bs_layout = view.findViewById(R.id.bs_layout)
        ps_layout = view.findViewById(R.id.ps_layout)

        glTextureView = view.findViewById(R.id.glSurfaceView)
        blackWindowScreen = view.findViewById(R.id.blackScreen)

        /** 메뉴 버튼 */
        focusToggleBtn = view.findViewById(R.id.focusToggleBtn)
        flashToggleBtn = view.findViewById(R.id.flashToggleBtn)
        lensChangeToggleBtn = view.findViewById(R.id.lensChangeToggleBtn)
        flowyZoomToggleBtn = view.findViewById(R.id.flowyZoomToggleBtn)
        mirroringToggleBtn = view.findViewById(R.id.mirroringToggleBtn)
        menuToggleBtn = view.findViewById(R.id.menuToggleBtn)
        flowyCastToggleBtn = view.findViewById(R.id.flowyCastToggleBtn)
        freezeToggleBtn = view.findViewById(R.id.freezeToggleBtn)
        luminanceToggleBtn = view.findViewById(R.id.luminanceToggleBtn)
        controlToggleBtn = view.findViewById(R.id.controlToggleBtn)
        controlChildToggleBtn = view.findViewById(R.id.controlChildToggleBtn)
        defaultColorImgView = view.findViewById(R.id.defaultColorImgView)
        shareImgBtn = view.findViewById(R.id.shareImgBtn)
        defaultColorImgView = view.findViewById(R.id.defaultColorImgView)
        binaryToggleBtn = view.findViewById(R.id.binaryToggleBtn)
        inverseToggleBtn = view.findViewById(R.id.inverseToggleBtn)

        /** 시크바 */
        pinchZoomSeekbar = view.findViewById(R.id.pinchZoomSeekbar)
        pinchZoomMinusImgBtn = view.findViewById(R.id.pinchZoomMinusImgBtn)
        pinchZoomPlusImgBtn = view.findViewById(R.id.pinchZoomPlusImgBtn)
        brightSeekbar = view.findViewById(R.id.brightSeekbar)
        contrastSeekbar = view.findViewById(R.id.contrastSeekbar)
        brightSeekbarListener(brightSeekbar)
        contrastSeekbarListener(contrastSeekbar)

        /** 광고 */
        bannerAdView = view.findViewById(R.id.bannerAdView)
        bannerVersaAD = view.findViewById(R.id.bannerVersaAD)

        contrastItemRecyclerView = view.findViewById(R.id.contrastItemRecyclerView)
        brightShadeAdapter = AdapterBrightShadeControl(THIS_CONTEXT!!)

        /** 인코딩 테스트 */
        val videoBtn: Button = view.findViewById(R.id.videoBtn)
        videoBtn.setOnClickListener {


            alertToast = Toast.makeText(THIS_CONTEXT, "인코딩 테스트 시작", Toast.LENGTH_SHORT)
            alertToast.show()

//            encode = VideoEncoderThread(
//                glTextureView.bitmap!!.width, glTextureView.bitmap!!.height,
//                8000*1000, 30
//            )

//            val webRTCUtil = WebRTCUtil(THIS_CONTEXT!!)
            onCameraPermissionGranted()
            flowyCastFlag = true

        }
        /** 인코딩 해제 테스트 */
        val videoReleaseBtn: Button = view.findViewById(R.id.videoReleaseBtn)
        videoReleaseBtn.setOnClickListener {

            flowyCastFlag = false

            alertToast = Toast.makeText(THIS_CONTEXT, "인코딩 해제", Toast.LENGTH_SHORT)
            alertToast.show()

//            encode.releaseMediaCodec()
        }
    }

    /** 클릭 리스너 관리 */
    private fun setClickListener() {
        focusToggleBtn.setOnClickListener(this)
        flashToggleBtn.setOnClickListener(this)
        lensChangeToggleBtn.setOnClickListener(this)
        flowyZoomToggleBtn.setOnClickListener(this)
        mirroringToggleBtn.setOnClickListener(this)
        menuToggleBtn.setOnClickListener(this)
        flowyCastToggleBtn.setOnClickListener(this)
        freezeToggleBtn.setOnClickListener(this)
        luminanceToggleBtn.setOnClickListener(this)
        controlToggleBtn.setOnClickListener(this)
        bannerVersaAD.setOnClickListener(this)
        shareImgBtn.setOnClickListener(this)
        controlChildToggleBtn.setOnClickListener(this)
        defaultColorImgView.setOnClickListener(this)
        binaryToggleBtn.setOnClickListener(this)
        inverseToggleBtn.setOnClickListener(this)

        /** TEST START */
        webSocketConnectBtn.setOnClickListener(this)
        /** TEST END */
    }

    override fun onResume() {
        super.onResume()

        /** 화면 방향 체크 */
        deviceRotationCheck()

        /** 광고 불러오기 */
        loadAdMob()

        /** 버튼 상태 불러오기 */
        togBtnStatusLoad()

        /** 카메라 사용시작 로그를 서버에 보낸다. */
        sendFlowyDataToServer(OVERFLOW_TEST_API_BASE_URL, 1)
    }


    /** 기기의 방향 체크 - 카메라 프래그먼트에서 화면 방향에 따라서 UI 버튼도 회전이 되어야한다. */
    private fun deviceRotationCheck() {
        val orientationEventListener =
            object : OrientationEventListener(THIS_CONTEXT, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    deviceRotationValue = orientation
                    Log.d("orientation", "orientation = $deviceRotationValue")
                    uiDirectionChange()
                }
            }
        orientationEventListener.enable()
    }

    /** 메뉴 버튼 방향 변경 */
    private fun uiDirectionChange() {
        // 정방향 이미지
        if (deviceRotationValue > 315 || deviceRotationValue <= 45) {
            deviceSensorDirection = 0f
            seekBarAnimation(pinchZoomSeekbar, 0f)
        }
        // 버튼 왼쪽으로 90도 회전
        else if (deviceRotationValue in 46..135) {
            deviceSensorDirection = -90f
            seekBarAnimation(pinchZoomSeekbar, 180f)
        }
        // 역방향 이미지
        else if (deviceRotationValue in 136..225) {
            deviceSensorDirection = -180f
            seekBarAnimation(pinchZoomSeekbar, 180f)
        }

        // 버튼 오른쪽으로 90도 회전
        else if (deviceRotationValue in 226..315) {
            deviceSensorDirection = 90f
            seekBarAnimation(pinchZoomSeekbar, 0f)
        }
        imgButtonAnimation(shareImgBtn, deviceSensorDirection)
        menuButtonAnimation(focusToggleBtn, deviceSensorDirection)
        menuButtonAnimation(flashToggleBtn, deviceSensorDirection)
        menuButtonAnimation(lensChangeToggleBtn, deviceSensorDirection)
        menuButtonAnimation(flowyZoomToggleBtn, deviceSensorDirection)
        menuButtonAnimation(mirroringToggleBtn, deviceSensorDirection)
        menuButtonAnimation(menuToggleBtn, deviceSensorDirection)
        menuButtonAnimation(flowyCastToggleBtn, deviceSensorDirection)
        menuButtonAnimation(freezeToggleBtn, deviceSensorDirection)
        menuButtonAnimation(freezeToggleBtn, deviceSensorDirection)
        menuButtonAnimation(luminanceToggleBtn, deviceSensorDirection)
        menuButtonAnimation(controlToggleBtn, deviceSensorDirection)
    }

    /** 이미지 버튼에 애니매이션 적용하기 : 공유버튼 : 기기의 방향에 따라서 버튼이 회전함  */
    private fun imgButtonAnimation(imgBtn: ImageButton, rotation: Float) {
        imgBtn.animate().apply { this.duration = 100; this.rotation(rotation) }.start()
    }

    /** 메뉴 버튼 ( 모든 토글버튼 ) 에 애니매이션 적용하기 : 기기의 방향에 따라서 버튼이 회전함 */
    private fun menuButtonAnimation(toggleBtn: ToggleButton, rotation: Float) {
        toggleBtn.animate().apply { this.duration = 100; this.rotation(rotation) }.start()
    }

    /** 시크바에 애니매이션 적용하기 : 기기의 방향에 따라서 시크바의 방향이 회전함  */
    private fun seekBarAnimation(seekBar: SeekBar, rotation: Float) {
        seekBar.animate().apply { this.duration = 0; this.rotation(rotation) }.start()
        if (rotation == 180f) {
            pinchZoomMinusImgBtn.setImageResource(R.drawable.pinchzoom_plus)
            pinchZoomPlusImgBtn.setImageResource(R.drawable.pinchzoom_minus)
            pinchZoomMinusImgBtn.animate().apply { this.rotation(180f) }.start()
            pinchZoomPlusImgBtn.animate().apply { this.rotation(180f) }.start()
        } else {
            pinchZoomMinusImgBtn.setImageResource(R.drawable.pinchzoom_minus)
            pinchZoomPlusImgBtn.setImageResource(R.drawable.pinchzoom_plus)
            pinchZoomMinusImgBtn.animate().apply { this.duration = 0; this.rotation(0f) }.start()
            pinchZoomPlusImgBtn.animate().apply { this.duration = 0; this.rotation(0f) }.start()
        }
    }

    /** 사용자의 대비 데이터 가져오기 */
    private fun loadLuminanceData() {
        val contrastPref =
            THIS_CONTEXT!!.getSharedPreferences("userLuminance", Context.MODE_PRIVATE)
        val contrastPrefEditor = contrastPref.edit()

        val userLuminancePref = contrastPref.getBoolean("userLuminanceInitFlag", true)
        // 처음 사용하는 사람이라면, luminance 데이터를 초기화 시켜준다.
        if (userLuminancePref) {
            contrastPrefEditor.putBoolean("userLuminanceInitFlag", false)
            contrastPrefEditor.commit()
            SharedPreferenceUtil().saveArrayListData<ContrastData>(
                contrastPref,
                "userLumincanceData",
                contrastInitData
            )
            userContrastData.addAll(contrastInitData)
            Log.d("lumiinit", "처음")
            Log.d("lumiinit", userContrastData.toString())
        }
        // 처음 사용하는 사람이 아니라면, 이전에 사용하던 luminance 데이터를 가져온다.
        else {
            val contrastMutableListData =
                SharedPreferenceUtil().loadArrayListData<ContrastData>(
                    contrastPref,
                    "userLumincanceData"
                )
            userContrastData = contrastMutableListData
            Log.d("lumiinit", "처음아님")
            Log.d("lumiinit", userContrastData.toString())
        }
    }

    override fun onClick(v: View) {

        when (v.id) {

            /** 포커스 기능 */
            R.id.focusToggleBtn -> {
                if (!focusToggleBtn.isChecked) CameraUtil().cameraAutoFocus(glTextureView)
                else CameraUtil().cameraTapFocus(glTextureView) // 포커스 기능을 누르면 오토 포커스 잡힌 곳으로 이동해야하는데 안됨.

            }

            /** 플래시 기능 */
            R.id.flashToggleBtn -> {

                try {
                    if (camera != null) {
                        if (flashToggleBtn.isChecked) {
                            camera!!.cameraControl.enableTorch(true)
                        } else {
                            camera!!.cameraControl.enableTorch(false)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "잠시후에 실행해주세요", Toast.LENGTH_SHORT).show()
                }
            }

            /** 화면 전환 기능 (전면, 후면) */
            R.id.lensChangeToggleBtn -> {

                CoroutineScope(Dispatchers.Main).launch {
                    blackWindowScreen.visibility = View.VISIBLE
                }

                // 0 : 전면
                // 1 : 후면

                // 화면 렌즈 모드바꾸기 전면 , 후면
                lensChangeFlag = true
                cameraLensMode = if (cameraLensMode == 0) 1 else 0

                // 화면이 전면으로 바뀌게 되면 플래시 기능을 비활성화 시킨다.
                if (camera != null) {
                    if (flashToggleBtn.isChecked) {
                        flashToggleBtn.isChecked = false
                        camera!!.cameraControl.enableTorch(false)
                    }
                }
                flashToggleBtn.isEnabled = cameraLensMode != 0

                // 화면이 전환 되면 포커스 기능을 끄고 자동포커스 키능을 켠다..
                focusToggleBtn.isChecked = false
                CameraUtil().cameraAutoFocus(glTextureView)

            }

            /** 플로위줌 기능 완료 */
            R.id.flowyZoomToggleBtn -> {
                // 플로위 줌 버튼을 눌렀을때 카메라 모드가 기본값이면, 카메라 모드는 flowy로 카메라 서브 모드는 longClick으로 변경한다.
                if (flowyZoomToggleBtn.isChecked) {
                    cameraMode = "flowy"
                    cameraSubMode = "longClick"

                    // 서버에 플로위 줌 시작 api를 보낸다.
                    sendFlowyDataToServer(OVERFLOW_TEST_API_BASE_URL, 3)
                    ps_layout.visibility = View.GONE
                }

                // 카메라 모드가 플로위 모드라면, 카메라모드를 기본값으로, 카메라 서브값도 기본값으로 변경한다.
                else {
                    cameraMode = "default"
                    cameraSubMode = "default"

                    // 서버에 플로위 줌 종료 api를 보낸다.
                    sendFlowyDataToServer(OVERFLOW_TEST_API_BASE_URL, 4)
                    ps_layout.visibility = View.VISIBLE
                }
            }

            /** 메뉴 기능 */
            R.id.menuToggleBtn -> {
                (activity as MainActivity).replaceFragment("add", FragmentMenu().newInstance())
            }

            R.id.flowyCastToggleBtn -> {

                alertToast =
                    Toast.makeText(THIS_CONTEXT, "mode : ${textureMode}", Toast.LENGTH_SHORT)
                alertToast.show()

                // 만약, 내장 카메라 화면 데이터가 렌더링 중이였다면
                if (textureMode == 0) {
                    val videoSurface = Surface(videoTexture)
                    sfTexture = videoTexture
                    videoDecoder = VideoDecodeThread()
                    videoFlag = videoDecoder.init(
                        videoSurface,
                        resources.openRawResourceFd(R.raw.h264video)
                    )
                    if (videoFlag) {
                        videoDecoder.start()
                    }
                    textureMode = 1
                }
                // mp4 파일 스트림 데이터가 렌더링 중이였다면
                else {
                    sfTexture = cameraTexture
                    videoDecoder.close()
                    videoDecoder.decoderStop()
                    videoDecoder.decoderRelease()
                    videoDecoder.extractorRelease()
                    textureMode = 0
                }

            }

            /** 화면 멈춤 기능 완료 */
            R.id.freezeToggleBtn -> {
                // 눌렀는데 체크가 되어있다면
                freezeMode = freezeToggleBtn.isChecked

                CoroutineScope(Dispatchers.Main).launch {
                    if (freezeMode) {
                        shareImgBtn.visibility = View.VISIBLE
                        tm_layout.visibility = View.GONE
                        cameraMode = "flowy"
                        cameraSubMode = "longClick"
                    } else {
                        shareImgBtn.visibility = View.GONE
                        tm_layout.visibility = View.VISIBLE

                        if (flowyZoomToggleBtn.isChecked) {
                            cameraMode = "flowy"
                            cameraSubMode = "longClick"
                        } else {
                            cameraMode = "default"
                            cameraSubMode = "default"
                        }


                    }
                }
            }

            /** 고대비 기능 완료 */
            R.id.luminanceToggleBtn -> {

                // fragment Shader에서 프로그램을 한번만 만들기 위한 플래그
                // 프로그램을 여러번 만들면 메모리릭이 발생한다.
                luminanceFlag = true

                // 고대비를 활성화 시킨다.
                // arraylist에 등록된 고대비 색상의 갯수보다 내가 선택한 횟수가 많다면 기본색상을 보여줘야한다.
                if (luminanceIndex >= userContrastData.size) {
                    luminanceIndex = 0
                    if (userContrastData.size == 0) {
                        if (alertToast != null) alertToast.cancel()
                        alertToast =
                            Toast.makeText(context, "[메뉴 - 대비] 값을 설정해주세요", Toast.LENGTH_SHORT)
                        alertToast.show()
                    }
                } else {
                    luminanceIndex += 1
                }

                if (luminanceIndex == 0) {
                    fragmentType = "default"
                    luminanceToggleBtn.isChecked = false
                } else {
                    fragmentType = "luminance"
                    luminanceToggleBtn.isChecked = true
                }
            }

            /** 공유하기 버튼 */
            R.id.shareImgBtn -> {

                val filePath = Environment.getExternalStorageDirectory().toString()
                val folderName = "Flowy"
                val fileName = "shareImage.jpeg"
                val dirs = File(filePath, folderName)

                // Flowy 폴더가 없으면 만든다.
                if (!dirs.exists()) {
                    dirs.mkdirs()
                }

                try {
                    CoroutineScope(Dispatchers.Default).launch {
                        val b = glTextureView.bitmap
                        b!!.compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            FileOutputStream(
                                "$filePath/$folderName/$fileName"
                            )
                        )
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

                val shareFile = File(filePath, "$folderName/$fileName")

                val intent = Intent(Intent.ACTION_SEND)
                intent.type = "image/*"
                intent.flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                val uri = FileProvider.getUriForFile(
                    THIS_CONTEXT!!,
                    "at.overflow.flowy.fileprovider",
                    shareFile
                )
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(intent)
            }

            /** 밝기 대비 조절 버튼 */
            R.id.controlToggleBtn -> {

                CoroutineScope(Dispatchers.Main).launch {
                    bm_layout.visibility = View.GONE
                    bs_layout.visibility = View.VISIBLE
                    brightShadeAdapter.notifyDataSetChanged()
                }
            }

            /** 밝기 대비 조절 버튼 끄기 */
            R.id.controlChildToggleBtn -> {
                CoroutineScope(Dispatchers.Main).launch {
                    bm_layout.visibility = View.VISIBLE
                    bs_layout.visibility = View.GONE
                }
            }

            /** 기본색상으로 초기화하는 버튼 */
            R.id.defaultColorImgView -> {
                luminanceIndex = 0
                luminanceFlag = true
                fragmentType = "default"
                luminanceToggleBtn.isChecked = false
                brightShadeAdapter.notifyDataSetChanged()
                if (alertToast != null) alertToast.cancel()
                alertToast = Toast.makeText(context, "기본색상으로 변경되었습니다", Toast.LENGTH_SHORT)
                alertToast.show()
            }

            R.id.binaryToggleBtn -> {
                binaryFlag = binaryToggleBtn.isChecked
            }
            R.id.inverseToggleBtn -> {
                inverseFlag = inverseToggleBtn.isChecked
            }

            /** 회사 광고를 누르면 회사페이지로 ~ */
            R.id.bannerVersaAD -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://atoverflow.com/")))
            }

            /** TEST START */

            R.id.webSocketConnectBtn -> {
                WebSocketUtil()
            }

            /** TEST END */
        }
    }

    /** 메뉴바에 있는 토글버튼 상태 가져오기 */
    private fun togBtnStatusLoad() {

        // 플로위줌 버튼 상태 및
        flowyZoomToggleBtn.isChecked = pref.getBoolean("flowyZoomToggleBtn", false)

        // 고대비 모드 설정
        luminanceIndex = pref.getInt("luminanceIndex", 0)
        if (luminanceIndex == 0) {
            luminanceToggleBtn.isChecked = false
            fragmentType = "default"
        } else {
            luminanceToggleBtn.isChecked = true
            fragmentType = "luminance"
        }

        // 카메라 렌즈 방향
        lensChangeToggleBtn.isChecked = pref.getBoolean("lensChangeToggleBtn", false)
        cameraLensMode = if (lensChangeToggleBtn.isChecked) 0 else 1
    }


    /** 메뉴바에 있는 토글버튼 상태 저장하기 */
    private fun togBtnStatusSave() {
        prefEditor.putBoolean("flowyZoomToggleBtn", flowyZoomToggleBtn.isChecked)
        prefEditor.putBoolean("lensChangeToggleBtn", lensChangeToggleBtn.isChecked)
        prefEditor.putInt("luminanceIndex", luminanceIndex)
        prefEditor.commit()
    }

    override fun onPause() {
        Log.d("lifeCycle", "onPause")
        super.onPause()
        togBtnStatusSave() // 버튼의 상태 저장

        /** 카메라 사용종료 로그를 서버에 보낸다. */
        sendFlowyDataToServer(OVERFLOW_TEST_API_BASE_URL, 2)
    }

    /** 광고 로드 */
    private fun loadAdMob() {
        MobileAds.initialize(THIS_CONTEXT, getString(R.string.admob_app_id))
        val adRequest = AdRequest.Builder().build()
        bannerAdView.loadAd(adRequest)
        bannerAdView.bringToFront()

        bannerAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerAdView.visibility = View.VISIBLE
                bannerVersaAD.visibility = View.GONE

                // 광고가 문제 없이 로드시 출력
                Log.d("@@@", "onAdLoaded")
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // 광고 로드에 문제가 있을시에 플로위 광고를 보여준다.
                Log.d("@@@", "onAdFailedToLoad $errorCode")
                if (errorCode == 3) {
                    Glide.with(THIS_CONTEXT!!)
                        .asGif()
                        .load(R.raw.versa_banner)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(bannerVersaAD)
                    bannerVersaAD.bringToFront()
                    bannerAdView.visibility = View.GONE
                }
            }

            override fun onAdOpened() {
            }

            override fun onAdClicked() {
            }

            override fun onAdLeftApplication() {
            }

            override fun onAdClosed() {
            }
        }
    }

    /** 밝기 조절기능 */
    private fun brightSeekbarListener(brightSeekbar: SeekBar) {
        brightSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                touchDataUtil.brightSeekbarProgress = progress
                if (progress == 0) brightSeekbar.progress = 1
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    /** 대비 조절기능 */
    private fun contrastSeekbarListener(contrastSeekbar: SeekBar) {
        contrastSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                touchDataUtil.contrastSeekbarProgress = progress
                if (progress == 0) contrastSeekbar.progress = 1
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun pinchZoomListener() {
        pinchZoomSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                // 핀치줌을 사용하지 않을때 카메라 확대
                if (!pinchZoomFlag && cameraMode != "flowy") {
                    camera!!.cameraControl.setLinearZoom(progress / 100.toFloat())
                    Log.d("setLinearZoom", (progress).toString())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (cameraMode != "flowy")
                    pinchZoomFlag = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (cameraMode != "flowy")
                    pinchZoomFlag = true
            }

        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun screenTouchListener() {
        glTextureView.setOnTouchListener(object : View.OnTouchListener {

            private val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        /** 터치를 하면 해당 위치에 포커스가 맞추어 지는 기능 */
                        focusToggleBtn.isChecked = true // 포커스 버튼을 활성화 시킨다.
                        autoFocusMode = false // 자동 포커스 기능을 해제한다.
                        return super.onSingleTapUp(e)
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {

                        // 더블 탭 모드로 변경
                        if (cameraSubMode == "longClick") {
                            cameraSubMode = "flowyDoubleTap"
                            touchDataUtil.setDoubleTapTouchPoint(e.x.toDouble(), e.y.toDouble())
                            Log.d("doubleTapPoint", "${e.x} : ${e.y} ")
                            touchDataUtil.isDoubleTapFirstTouched = true
                            touchDataUtil.setFirstTouchPoint(e.x.toDouble(), e.y.toDouble())
                        }

                        // 롱 클릭 모드로 변경
                        else {
                            cameraSubMode = "longClick"
                            touchDataUtil.setDoubleTapTouchPoint(0.0, 0.0)
                            touchDataUtil.isDoubleTapFirstTouched = false
                        }
                        return super.onDoubleTap(e)
                    }

                    override fun onShowPress(e: MotionEvent?) {
                        super.onShowPress(e)
                        Log.d("onShowPress", "onShowPress")
                        flowyZoomLongClickEvent = true // 롱클릭 이벤트가 발생했을때, 플로위 줌을 시작한다.
                    }
                })

            /** 핀치줌 기능 : 카메라 모드가 플로위 모드일떄랑 아닐떄랑 나뉜다.
             * 플로위 기능을 사용할떄는 플로위 확대 - 터치한 포인트를 기준으로 확대를 시켜주고
             * 플로위 기능을 사용 안할때는 일반 카메라 확대를 해준다. */
            private val pinchZoomGesture =
                ScaleGestureDetector(
                    context,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            Log.d("onScale", cameraMode)

                            // zoomRatio의 범위는 1~8배 까지이다.
                            var currentZoomRatio: Float =
                                camera!!.cameraInfo.zoomState.value?.zoomRatio ?: 0F
                            var currentZoomLinear: Float =
                                camera!!.cameraInfo.zoomState.value?.linearZoom ?: 0F
                            val delta = detector.scaleFactor
                            var scale = currentZoomRatio * delta
                            Log.d(
                                "scaleValue123",
                                "$currentZoomRatio : $currentZoomLinear : $scale"
                            )

                            // 일반 확대를 사용하는 경우
                            if (cameraMode != "flowy") {
                                camera!!.cameraControl.setZoomRatio(scale)

                                scale -= 1
                                if (scale <= 0) scale = 0f
                                else if (scale >= 7) scale = 7f

                                if (currentZoomLinear <= 0) currentZoomLinear = 0f
                                else if (currentZoomLinear >= 1) currentZoomLinear = 1f

                                if (pinchZoomFlag) {
                                    Log.d(
                                        "scaleValue",
                                        " 1: $currentZoomRatio : $scale : ${pinchZoomSeekbar.progress}"
                                    )
                                    pinchZoomSeekbar.progress =
                                        (currentZoomLinear * 100.toDouble()).toInt()
                                    Log.d(
                                        "scaleValue",
                                        " 2: $currentZoomRatio : $scale : ${pinchZoomSeekbar.progress}"
                                    )
                                }
                            }
                            return true
                        }

                        // 핀치 줌이 끝나면 오토 포커스 모드로 들어간다.
                        override fun onScaleEnd(detector: ScaleGestureDetector?) {
                            // 일반 확대를 사용하는 경우
                            if (cameraMode != "flowy") {
                                CameraUtil().cameraAutoFocus(glTextureView)
                                pinchZoomFinishCallback = false
                                focusToggleBtn.isChecked = false
                            }
                            super.onScaleEnd(detector)
                        }

                        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                            // 일반 확대를 사용하는 경우
                            if (cameraMode != "flowy") {
                                pinchZoomFinishCallback = true
                            }
                            return super.onScaleBegin(detector)
                        }
                    })

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                Log.d("onTouch", "${event.x} : ${event.y} ")

                // 사용자가 찎은 좌표를 항상 기록한다.
                touchDataUtil.setAlwaysTouchPoint(event.x.toDouble(), event.y.toDouble())

                // 롱클릭 이벤트를 받아, 플로위 롱클릭 모드에서 움직이는 경우
                if (flowyZoomLongClickEvent && cameraMode == "flowy" && (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN)) {
                    Log.d("motion", "onTouch: ${event.action}")
                    // 첫번째로 터치한 좌표를 받아 firstTouchX , Y 에 할당한다. 화면 여백을 클릭하는걸 방지 하기 위함.
                    if (touchDataUtil.touchFirstX == 0.0 && touchDataUtil.touchFirstY == 0.0)
                        touchDataUtil.setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 터치한 포인트를 기록한다.
                    touchDataUtil.setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    // 현재 상태를 터치중으로 변경한다.
                    touchDataUtil.isTouching = true
                } else if (cameraSubMode == "flowyDoubleTap") {

                    if (event.action == MotionEvent.ACTION_DOWN) {
                        touchDataUtil.setFirstTouchPoint(event.x.toDouble(), event.y.toDouble())
                        touchDataUtil.isScreenPointSave = false

                    } else {
                        touchDataUtil.setTouchPoint(event.x.toDouble(), event.y.toDouble())
                    }
                }

                // 사용자가 화면에서 손을 땠을때
                if (event.action == MotionEvent.ACTION_UP) {
                    touchDataUtil.setTouchPoint(0.0, 0.0) // 터치한 포인트를 0,0 으로 초기화한다.
                    touchDataUtil.setFirstTouchPoint(0.0, 0.0) // 첫번째로 터치한 포인트를 0,0으로 초기화한다.
                    touchDataUtil.setFocusTouchPoint(glTextureView, event.x, event.y)
                    touchDataUtil.isScreenPointSave = true // 더블탭 모드에 사용
                    touchDataUtil.flowyPinchFlag = false // 핀치줌
                    touchDataUtil.pinchFirstDistanceFlag = false

                    Log.d("ClickEvent", "action up")
                    touchDataUtil.isTouching = false // 현재 상태를 터치중 아님으로 변경한다.
                    flowyZoomLongClickEvent = false // 플로위줌을 사용하기 다시 사용하기 위해 롱클릭 이벤트를 false로 만듦
                    CameraUtil().cameraTapFocus(glTextureView)
                }

                // 손가락 2개로 터치를 했을때, 메뉴를 보이게 / 안보이게 처리를 한다.
                if (event.action == MotionEvent.ACTION_POINTER_3_DOWN && event.pointerCount == 3) {
                    threePointClick()
                }

                if (event.action == MotionEvent.ACTION_MOVE) {
                    /** flowy pinch zoom 포인트 */
                    if (event.pointerCount == 1) {
                        touchDataUtil.pinchFirstTouchX = event.getX(0)
                        touchDataUtil.pinchFirstTouchY = event.getY(0)
                        touchDataUtil.flowyPinchFlag = false
                    } else if (event.pointerCount == 2) {
                        touchDataUtil.pinchSecondTouchX = event.getX(1)
                        touchDataUtil.pinchSecondTouchY = event.getY(1)
                        touchDataUtil.flowyPinchFlag = true
                        if (!touchDataUtil.pinchFirstDistanceFlag) {
                            touchDataUtil.pinchFirstDistance = Math.sqrt(
                                Math.pow(
                                    ((touchDataUtil.pinchSecondTouchX - touchDataUtil.pinchFirstTouchX).toDouble()),
                                    2.0
                                ) + Math.pow(
                                    ((touchDataUtil.pinchSecondTouchY - touchDataUtil.pinchFirstTouchY).toDouble()),
                                    2.0
                                )
                            ).toFloat()
                            touchDataUtil.pinchFirstDistanceFlag = true
                        }
                    }

                    Log.d(
                        "asdasdadszxczvcxlkm",
                        "pinchFirstTouchX : ${touchDataUtil.pinchFirstTouchX} / " +
                                "pinchFirstTouchY : ${touchDataUtil.pinchFirstTouchY} / " +
                                "pinchSecondTouchX : ${touchDataUtil.pinchSecondTouchX} / " +
                                "pinchSecondTouchY : ${touchDataUtil.pinchSecondTouchY} / "
                    )
                }

                gestureDetector.onTouchEvent(event)
                pinchZoomGesture.onTouchEvent(event)
                return true
            }
        })
    }

    /** 화면을 손가락 3개 이상으로 터치했을때, 화면 상단, 하단에 있는 메뉴를 보임, 숨김 처리한다. */
    fun threePointClick() {
        if (threePointClickFlag) {

            CoroutineScope(Dispatchers.Main).launch {
                top_f_layout.visibility = View.GONE
                bottom_f_layout.visibility = View.GONE
            }

        } else {
            CoroutineScope(Dispatchers.Main).launch {
                top_f_layout.visibility = View.VISIBLE
                bottom_f_layout.visibility = View.VISIBLE
            }
        }

        threePointClickFlag = !threePointClickFlag
    }

    /** 서버에 플로위 줌 신호를 보낸다. - 사용시작 : 1, 사용정지 : 2, FlowyZoom 시작 : 3, FlowyZoom 종료 : 4 */
    private fun sendFlowyDataToServer(baseURL: String, userAction: Int) {

        try {
            val sendLogData: HashMap<String, Any> = HashMap()
            sendLogData["api_key"] = API_KEY
            sendLogData["device_id"] = USER_UUID
            sendLogData["action_code"] = userAction

            val retrofit =
                Retrofit2Util().getRetrofit2Builder(baseURL).create(RetrofitAPI::class.java)
            retrofit.postFlowyZoomLogData(sendLogData)
        } catch (e: Exception) {
            Log.d("retrofitError", "error : " + e.message.toString())
        }

    }

    companion object {
        lateinit var blackWindowScreen : View
        var lensChangeFlag = false

        /** 고대비 색상과 인덱스 */
        var luminanceIndex: Int = 0
        var luminanceFlag: Boolean = false
        var userContrastData = arrayListOf<ContrastData>()

        lateinit var luminanceToggleBtn: ToggleButton

        var touchDataUtil: TouchDataUtil = TouchDataUtil()

        lateinit var encode: VideoEncoderThread
    }

    //    /** 서버에 이미지를 올린다. */
    private fun imageUpload(encodeBitmap: String, baseURL: String) {

        val sendLogData: HashMap<String, Any> = HashMap()
        sendLogData["image_data"] = encodeBitmap

        val retrofit = Retrofit2Util().getRetrofit2Builder(baseURL).create(RetrofitAPI::class.java)

        /** 동기 */
        val responseData = retrofit.uploadImage(sendLogData).execute()

        val jsonData = JsonParser().parse(responseData.body().toString())
        val code = jsonData.asJsonObject["code"]
        val result = jsonData.asJsonObject["result"]

        Log.d("uploadImageTest", "jsonData: $jsonData")
        Log.d("uploadImageTest", "code: $code")

        /** 아래는 비동기 */
//        retrofit.uploadImage(sendLogData).enqueue(object : Callback<Any> {
//            override fun onFailure(call: Call<Any>, t: Throwable) {
//                Log.d("uploadImageTest", "onFailure: ${t.message}")
//                val end = System.currentTimeMillis()
//                Log.d("timer", ((end - start) / 1000).toString())
//            }
//
//            override fun onResponse(call: Call<Any>, response: Response<Any>) {
//                val jsonData = JSONObject(response.body().toString())
//
//                try {
//
//                    val code = jsonData.getString("code");
//                    Log.d("uploadImageTest", "onResponse: $code")
//
//                    CoroutineScope(Dispatchers.Main).launch {
//                        busTextView.text = ""
//                    }
//
//                    if (code == "0.0") {
//                        Log.d("uploadImageTest", "그대로 출력 : $jsonData")
//                        val result = jsonData.getJSONArray("result");
//                        Log.d("uploadImageTest", "onResponse: ${result.toString()}")
//                        for (i in 0 until result.length()) {
//                            val jsonObject = result.getJSONObject(i)
//                            val busNumber = jsonObject.getString("num")
//                            val busAc = jsonObject.getString("conf")
//                            val busRect = jsonObject.getString("rect")
//                            Log.d("uploadImageTest", "width: ${glTextureView.width}")
//                            Log.d("uploadImageTest", "height: ${glTextureView.height}")
//                            Log.d("uploadImageTest", "busNumber: $busNumber")
//                            Log.d("uploadImageTest", "busAc: $busAc")
//                            Log.d("uploadImageTest", "busRect: $busRect")
//
//                            CoroutineScope(Dispatchers.Main).launch {
//                                busTextView.append("버스번호 : $busNumber\n")
//                            }
//                        }
//                    } else {
//                        CoroutineScope(Dispatchers.Main).launch {
//                            busTextView.text = "No Detect"
//                        }
//                        Log.d("uploadImageTest", jsonData.toString())
//                    }
//                } catch (e: Exception) {
//
//                }
//            }
//
//        })
    }


    /** ------------------------------ */
    private lateinit var signallingClient: SignallingClient
    private lateinit var rtcClient: RTCClient

    @KtorExperimentalAPI
    @ExperimentalCoroutinesApi
    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            signallingClient.send(p0)
        }
    }

    private fun onCameraPermissionGranted() {
        rtcClient = RTCClient(
            activity!!.application,
            object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    signallingClient.send(p0)
                    rtcClient.addIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    p0?.videoTracks?.get(0)?.addSink(remote_view)
                }
            }
        )
        rtcClient.initSurfaceView(remote_view)
        rtcClient.initSurfaceView(local_view)
        rtcClient.startLocalVideoCapture(local_view)
        signallingClient = SignallingClient(createSignallingClientListener())
        call_button.setOnClickListener { rtcClient.call(sdpObserver) }
    }

    private fun createSignallingClientListener() = object : SignallingClientListener {
        override fun onConnectionEstablished() {
            call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver)
//            remote_view_loading.isGone = true
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
//            remote_view_loading.isGone = true
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            rtcClient.addIceCandidate(iceCandidate)
        }
    }

}