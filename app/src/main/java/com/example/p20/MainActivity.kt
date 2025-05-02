package com.example.p20

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.view.animation.AlphaAnimation
import androidx.fragment.app.DialogFragment
import android.graphics.Color
import android.view.Gravity
import android.animation.ObjectAnimator
import android.os.Looper
import androidx.core.graphics.drawable.DrawableCompat

class MainActivity : AppCompatActivity() {

    lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
    private lateinit var assetTextView: TextView // 최상단 자산 표시
    lateinit var stockViewModel: StockViewModel
    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언
    private lateinit var realEstateViewModel: RealEstateViewModel // 부동산 뷰모델 추가
    private lateinit var albaViewModel: AlbaViewModel // 알바 뷰모델 추가
    private lateinit var globalRemainingTimeTextView: TextView // 전역 남은 시간 표시 텍스트뷰
    private lateinit var viewModelFactory: ViewModelFactory // viewModelFactory 클래스 변수로 선언
    
    // 컨트롤러 인스턴스
    private lateinit var soundController: SoundController
    private lateinit var animationController: AnimationController
    
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialGravity: Int = 0

    // 기본 UI 요소
    private lateinit var contentContainer: FrameLayout
    private lateinit var timeWarningEffect: View
    private lateinit var flashEffect: View
    private lateinit var visionNarrowingEffect: View
    
    // UI 요소에 대한 변수
    private lateinit var slidePanel: LinearLayout

    // 효과음 설정 변경 리시버 등록
    private lateinit var soundSettingsReceiver: android.content.BroadcastReceiver
    
    // 클래스 멤버 변수로 핸들러들을 추적하기 위한 목록 추가
    private val activeHandlers = mutableListOf<android.os.Handler>()
    private val activeRunnables = mutableMapOf<android.os.Handler, Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 컨트롤러 초기화
        soundController = SoundController.getInstance(this)
        animationController = AnimationController.getInstance(this)
        
        // 컨트롤러 연결
        animationController.setSoundController(soundController)
        
        // 설정만 초기화하고 배경음악은 아직 재생하지 않음
        initializeSoundSettings()
        
        // SplashActivity에서 넘어온 경우 음악 시작
        if (intent.getBooleanExtra("startMusic", false)) {
            android.util.Log.d("MainActivity", "SplashActivity에서 넘어옴 - 배경음악 시작")
            startMusicFromSplash()
        }

        // 효과음 설정 변경을 수신하는 BroadcastReceiver 등록
        registerSoundSettingsReceiver()

        // UI 요소 초기화
        contentContainer = findViewById(R.id.contentContainer)
        timeWarningEffect = findViewById(R.id.timeWarningEffect)
        flashEffect = findViewById(R.id.flashEffect)
        visionNarrowingEffect = findViewById(R.id.visionNarrowingEffect)
        
        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        val timeInfo: TextView = findViewById(R.id.timeInfo)

        // 메시지 관리자 초기화
        val messageContainer = findViewById<LinearLayout>(R.id.messageContainer)
        MessageManager.initialize(messageContainer)

        // 시간 표시 설정
        globalRemainingTimeTextView = findViewById(R.id.timeInfo)
        assetTextView = findViewById(R.id.assetInfo)

        // StockViewModel 초기화
        stockViewModel = ViewModelProvider(this).get(StockViewModel::class.java)

        // RealEstateViewModel 초기화
        realEstateViewModel = ViewModelProvider(this).get(RealEstateViewModel::class.java)

        // AlbaViewModel 초기화
        albaViewModel = ViewModelProvider(this).get(AlbaViewModel::class.java)

        // TimeViewModel 초기화 (수정)
        timeViewModel = ViewModelProvider(this).get(TimeViewModel::class.java)

        // AnimationController에 남은 시간 LiveData 설정
        animationController.setRemainingTimeLiveData(timeViewModel.remainingTime)

        // LiveData 감시하여 UI 업데이트
        timeViewModel.time.observe(this) { newTime ->
            timeInfo.text = "게임시간: $newTime"
        }

        // 전역 남은 시간 UI 업데이트 추가
        globalRemainingTimeTextView = findViewById(R.id.globalRemainingTimeInfo) // 텍스트뷰 참조

        // 남은 시간 표시를 드래그로 이동할 수 있도록 설정
        setupDraggableTimeView()

        timeViewModel.remainingTime.observe(this) { remainingSeconds ->
            // 텍스트 업데이트 (초 단위) - 둥근 배경에 맞게 포맷 수정
            val timeText = if (remainingSeconds > 60) {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                "남은 시간\n${minutes}분 ${seconds}초"
            } else {
                "남은 시간\n${remainingSeconds}초"
            }
            globalRemainingTimeTextView.text = timeText

            // 임계값에 따른 배경색 변경
            when {
                remainingSeconds <= Constants.CRITICAL_TIME_THRESHOLD -> {
                    // 5초 이하 - 위험 상태 (빨간색 배경)
                    globalRemainingTimeTextView.setBackgroundResource(R.drawable.round_time_background_critical)
                }
                remainingSeconds <= Constants.WARNING_TIME_THRESHOLD -> {
                    // 15초 이하 - 경고 상태 (노란색 배경)
                    globalRemainingTimeTextView.setBackgroundResource(R.drawable.round_time_background_warning)
                }
                else -> {
                    // 정상 상태 (기본 배경)
                    globalRemainingTimeTextView.setBackgroundResource(R.drawable.round_time_background)
                }
            }

            // 시간 임계값에 따른 효과 적용
            if (remainingSeconds <= Constants.WARNING_TIME_THRESHOLD) {
                // 텍스트 깜빡임 애니메이션
                animationController.setupTimeBlinkAnimation(globalRemainingTimeTextView)

                // 위급 상황 효과 적용
                animationController.setupEmergencyEffects(
                    remainingSeconds.toLong(),
                    contentContainer,
                    timeWarningEffect,
                    flashEffect
                )
            } else {
                // 임계값 이상일 때는 모든 효과 제거
                stopAllAnimations()
            }
        }

        // 다시 시작 요청 처리 로직
        timeViewModel.restartRequested.observe(this) { requested ->
            if (requested) {
                // 실제 데이터 리셋 (ViewModel의 resetTimer에서 restartRequested를 false로 돌림)
                timeViewModel.resetTimer()
                assetViewModel.resetAssets()
                stockViewModel.resetStocks()
                albaViewModel.resetAlba()
                realEstateViewModel.resetRealEstatePrices()

                // ViewModelFactory 초기화 후 TimingAlbaViewModel 리셋 추가
                if (::viewModelFactory.isInitialized) {
                    // 타이밍 알바 뷰모델 제거
                    // 게임 리셋 시 알바 뷰모델만 리셋
                    albaViewModel.resetAlba()
                }

                // 게임 리셋 이벤트 발생
                timeViewModel.triggerGameResetEvent()

                // 기존에 표시 중인 모든 프래그먼트 제거
                for (fragment in supportFragmentManager.fragments) {
                    if (fragment.isVisible && fragment !is DialogFragment) {
                        supportFragmentManager.beginTransaction().remove(fragment).commit()
                    }
                }
                
                // supportFragmentManager가 업데이트될 수 있도록 실행
                supportFragmentManager.executePendingTransactions()

                // ExplanationFragment 표시
                val explanationTag = "ExplanationFragment"
                if (supportFragmentManager.findFragmentByTag(explanationTag) == null) {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.contentFrame, ExplanationFragment(), explanationTag)
                        .commit()
                }
            }
        }

        // 게임 오버 처리 로직 (DialogFragment 사용)
        timeViewModel.isGameOver.observe(this) { isGameOver ->
            val dialogTag = "GameOverDialog"
            val existingDialog = supportFragmentManager.findFragmentByTag(dialogTag) as? DialogFragment

            if (isGameOver) {
                // 모든 시각적 효과 중지
                stopAllAnimations()
                
                // 다이얼로그가 이미 떠 있지 않다면 새로 띄움
                if (existingDialog == null) {
                    GameOverDialogFragment().show(supportFragmentManager, dialogTag)
                }
                
                // 게임 오버 시 모든 기능 버튼 비활성화
                setAllButtonsEnabled(false)
            } else {
                // 게임 오버 상태가 아니라면 다이얼로그 닫기
                existingDialog?.dismiss()
                
                // 게임 진행 중일 때는 모든 버튼 활성화
                setAllButtonsEnabled(true)
            }
        }

        // TimeViewModel 초기화 후 명시적으로 게임 타이머 시작 호출
        timeViewModel.startGameTimer()

        // ViewModelFactory 초기화
        viewModelFactory = ViewModelFactory(application)

        // AssetViewModel과 기타 ViewModel 초기화
        assetViewModel = ViewModelProvider(this, viewModelFactory).get(AssetViewModel::class.java)

        // 알바 관련 ViewModel 초기화
        ViewModelProvider(this, viewModelFactory).get(AlbaViewModel::class.java)

        assetTextView = findViewById(R.id.assetInfo)

        // 자산 초기 표시
        assetViewModel.asset.observe(this) { newAsset ->
            assetTextView.text = assetViewModel.getAssetText()

            // 자산이 마이너스일 경우 색상 변경
            if (newAsset < 0) {
                assetTextView.setTextColor(android.graphics.Color.RED)
            } else {
                assetTextView.setTextColor(android.graphics.Color.WHITE)
            }
        }

        /////////////////////////////버튼///////////////////////////////
        val buttonAlba = findViewById<Button>(R.id.buttonAlba)
        val buttonStock = findViewById<Button>(R.id.buttonStock)
        val buttonRealEstate = findViewById<Button>(R.id.buttonRealEstate)
        val buttonEarnMoney = findViewById<Button>(R.id.buttonEarnMoney)
        val buttonMyInfo = findViewById<Button>(R.id.buttonMyInfo)
        val buttonItem = findViewById<Button>(R.id.buttonItem)
        val buttonBank = findViewById<Button>(R.id.buttonBank)
        val buttonCasino = findViewById<Button>(R.id.buttonCasino)
        val buttonLotto = findViewById<Button>(R.id.buttonLotto)
        slidePanel = findViewById<LinearLayout>(R.id.slidePanel)

        // 앱 첫 시작 시 ExplanationFragment 추가
        if (savedInstanceState == null) { // 액티비티가 처음 생성될 때만
            supportFragmentManager.beginTransaction()
                .add(R.id.contentFrame, ExplanationFragment(), "ExplanationFragment")
                .commit()
        }

        slidePanel.getChildAt(0).setOnClickListener {
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    slidePanel.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            slidePanel.startAnimation(slideDown)
        }

        // 버튼 효과음 설정
        setupButtonSounds()

        // 설정 버튼 클릭 리스너 설정
        val buttonSettings = findViewById<Button>(R.id.buttonSettings)
        buttonSettings.setOnClickListener {
            soundController.playButtonSound()
            val settingsDialog = SettingsDialogFragment()
            settingsDialog.show(supportFragmentManager, SettingsDialogFragment.TAG)
        }
    }

    /**
     * 배경음악 설정을 초기화하지만 음악은 재생하지 않음
     */
    private fun initializeSoundSettings() {
        try {
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            val soundEnabled = prefs.getBoolean("sound_enabled", true)
            val soundEffectEnabled = prefs.getBoolean("sound_effect_enabled", true)
            val isMuted = prefs.getBoolean("mute_enabled", false)
            val volume = prefs.getFloat("current_volume", 0.7f)
            
            // 볼륨 설정만 적용
            soundController.setVolume(volume)
            
            // 로그 출력
            android.util.Log.d("MainActivity", "사운드 설정 초기화: 배경음악=$soundEnabled, 효과음=$soundEffectEnabled, 음소거=$isMuted, 볼륨=$volume")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "사운드 설정 초기화 오류: ${e.message}")
        }
    }
    
    /**
     * SplashActivity에서 넘어온 경우 배경음악 시작
     */
    private fun startMusicFromSplash() {
        try {
            // 애플리케이션 플래그 확인
            if (P20Application.isMusicInitialized()) {
                val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                val soundEnabled = prefs.getBoolean("sound_enabled", true)
                val isMuted = prefs.getBoolean("mute_enabled", false)
                
                if (!isMuted && soundEnabled) {
                    // 딜레이를 주어 UI가 모두 로드된 후 배경음악 시작
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        soundController.startBackgroundMusic()
                        android.util.Log.d("MainActivity", "스플래시 화면 후 배경음악 시작됨")
                    }, 500) // 0.5초 후 시작
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "배경음악 시작 오류: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        
        // 화면이 다시 보일 때 배경음악 재생 확인
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        val isMuted = prefs.getBoolean("mute_enabled", false)
        
        if (!isMuted && soundEnabled) {
            soundController.resumeBackgroundMusic()
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // 앱이 백그라운드로 갈 때 배경음악 일시정지
        soundController.pauseBackgroundMusic()
    }

    // 볼륨 조절
    fun setVolume(volume: Float) {
        soundController.setVolume(volume)
    }
    
    /**
     * 게임 오버 후 다시 시작할 때 배경음악을 처음부터 재생
     */
    fun restartBackgroundMusic() {
        try {
            android.util.Log.d("MainActivity", "게임 재시작 - 배경음악 처음부터 재생")
            
            // SoundController의 restartBackgroundMusic 메서드 호출
            soundController.restartBackgroundMusic()
            
            android.util.Log.d("MainActivity", "게임 재시작 후 배경음악 다시 시작됨")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "배경음악 재시작 오류: ${e.message}")
        }
    }

    // 배경음악 중지
    fun stopBackgroundMusic() {
        soundController.stopBackgroundMusic()
    }
    
    // 게임 오버 시 음악 중지
    fun stopBackgroundMusicForGameOver() {
        soundController.stopBackgroundMusic()
    }

    /**
     * 버튼 효과음을 재생합니다.
     */
    private fun playButtonSound() {
        soundController.playButtonSound()
    }

    // 효과음 설정 변경 리시버 등록
    private fun registerSoundSettingsReceiver() {
        val filter = android.content.IntentFilter("com.example.p20.SOUND_SETTINGS_CHANGED")
        soundSettingsReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                // 설정 변경 감지
            }
        }
        
        registerReceiver(soundSettingsReceiver, filter)
    }

    override fun onDestroy() {
        // 앱이 완전히 종료될 때 데이터 저장
        try {
            stockViewModel.saveStockData()
            assetViewModel.saveAssetToPreferences()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "데이터 저장 오류: ${e.message}")
        }
        
        // 모든 애니메이션 종료
        stopAllAnimations()
        
        // 모든 핸들러 정리
        for (handler in activeHandlers) {
            handler.removeCallbacksAndMessages(null)
        }
        activeHandlers.clear()
        activeRunnables.clear()
        
        // 컨트롤러 해제
        soundController.release()
        animationController.release()
        
        // 효과음 설정 리시버 등록 해제
        try {
            unregisterReceiver(soundSettingsReceiver)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "리시버 해제 오류: ${e.message}")
        }
        
        super.onDestroy()
    }

    // 모든 애니메이션을 중지하는 헬퍼 메서드
    private fun stopAllAnimations() {
        animationController.stopAllAnimations(
            contentContainer,
            timeWarningEffect,
            flashEffect,
            visionNarrowingEffect,
            globalRemainingTimeTextView
        )
    }

    /**
     * 모든 버튼에 효과음을 설정합니다.
     */
    private fun setupButtonSounds() {
        // ButtonHelper를 사용하여 버튼과 액션 매핑
        val buttonsMap = mapOf<Button, () -> Unit>(
            findViewById<Button>(R.id.buttonMyInfo) to {
                removeExplanationFragment()
                showFragment(MyInfoFragment(), "MyInfoFragment")
            },
            findViewById<Button>(R.id.buttonBank) to {
                removeExplanationFragment()
                showFragment(BankFragment(), "BankFragment")
            },
            findViewById<Button>(R.id.buttonItem) to {
                removeExplanationFragment()
                showFragment(ItemFragment(), "ItemFragment")
            },
            findViewById<Button>(R.id.buttonEarnMoney) to {
                removeExplanationFragment()
                toggleSlidePanel()
            },
            findViewById<Button>(R.id.buttonSettings) to {
                removeExplanationFragment()
                SettingsDialogFragment().show(supportFragmentManager, "SettingsDialog")
            },
            findViewById<Button>(R.id.buttonAlba) to {
                removeExplanationFragment()
                slidePanel.visibility = View.GONE
                showFragment(AlbaFragment(), "AlbaFragment")
            },
            findViewById<Button>(R.id.buttonStock) to {
                removeExplanationFragment()
                slidePanel.visibility = View.GONE
                showFragment(StockFragment(), "StockFragment")
            },
            findViewById<Button>(R.id.buttonRealEstate) to {
                removeExplanationFragment()
                slidePanel.visibility = View.GONE
                showFragment(RealEstateFragment(), "RealEstateFragment")
            },
            findViewById<Button>(R.id.buttonCasino) to {
                removeExplanationFragment()
                slidePanel.visibility = View.GONE
                showFragment(CasinoFragment(), "CasinoFragment")
            },
            findViewById<Button>(R.id.buttonLotto) to {
                removeExplanationFragment()
                slidePanel.visibility = View.GONE
                showFragment(LottoFragment(), "LottoFragment")
            }
        )
        
        // 모든 버튼에 동일한 효과음 적용
        buttonsMap.forEach { (button, action) ->
            button.setOnClickListener {
                soundController.playButtonSound()
                action()
            }
        }
    }

    /**
     * 슬라이드 패널 토글 메서드
     */
    private fun toggleSlidePanel() {
        if (slidePanel.visibility == View.VISIBLE) {
            // 슬라이드 패널이 보이는 상태면 닫기
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    slidePanel.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            slidePanel.startAnimation(slideDown)
        } else {
            // 슬라이드 패널이 안 보이는 상태면 열기
            slidePanel.visibility = View.VISIBLE
            val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
            slidePanel.startAnimation(slideUp)
        }
    }

    // ExplanationFragment 제거 함수
    private fun removeExplanationFragment() {
        val explanationFragment = supportFragmentManager.findFragmentByTag("ExplanationFragment")
        if (explanationFragment != null) {
            supportFragmentManager.beginTransaction().remove(explanationFragment).commit()
            // ExplanationFragment가 제거될 때 타이머 시작
            // 참고: ExplanationFragment의 onDestroy에서도 타이머를 시작하지만,
            // 혹시 모를 상황에 대비해 여기서도 타이머 시작
            timeViewModel.startTimer()
            
            // 게임 설명창이 사라진 후 배경음악 재생 시작
            soundController.startBackgroundMusic()
            
            // 게임 설명창이 사라진 후 시간 표시 드래그 관련 안내 메시지 표시
            showDragTimeViewMessage()
        }
    }

    /**
     * 시간 표시 드래그 안내를 시각적으로 표시하는 메소드
     * 텍스트 설명 대신 애니메이션으로 사용자에게 드래그 방법을 안내함
     */
    fun showDragTimeViewMessage() {
        // 드래그 튜토리얼을 위한 약간의 딜레이 (1초)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            // 튜토리얼 뷰 생성
            showDragTutorialAnimation()
        }, 1000)
    }
    
    /**
     * 드래그 방법을 시각적으로 보여주는 튜토리얼 애니메이션
     * 손가락 아이콘과 함께 실제 남은 시간 표시 창이 움직이는 예시를 보여줌
     */
    private fun showDragTutorialAnimation() {
        try {
            // 튜토리얼 활성 상태 추적
            var isTutorialActive = true
            
            // 튜토리얼 컨테이너 생성
            val tutorialContainer = FrameLayout(this)
            tutorialContainer.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // 반투명 배경
            val background = View(this)
            background.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background.setBackgroundColor(Color.parseColor("#55000000"))
            
            // 실제 남은 시간 표시 뷰의 초기 위치 저장
            val originalTimeViewX = globalRemainingTimeTextView.x
            val originalTimeViewY = globalRemainingTimeTextView.y
            
            // 대상 위치 계산 (남은 시간 표시 위치)
            val targetViewLocation = IntArray(2)
            globalRemainingTimeTextView.getLocationInWindow(targetViewLocation)
            val targetWidth = globalRemainingTimeTextView.width
            val targetHeight = globalRemainingTimeTextView.height
            
            // 터치 포인트 중심 위치 계산
            val centerX = (targetViewLocation[0] + targetWidth/2).toFloat()
            val centerY = (targetViewLocation[1] + targetHeight/2).toFloat()
            
            // 화면 중앙 방향으로 드래그하도록 방향 계산
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val screenCenterX = screenWidth / 2f
            val screenCenterY = screenHeight / 2f
            
            // 드래그 방향을 화면 중앙 쪽으로 설정
            val dragDirectionX = if (centerX > screenCenterX) -1f else 1f
            val dragDirectionY = if (centerY > screenCenterY) -1f else 1f
            
            // 적절한 드래그 거리 계산 (화면 안으로 드래그)
            val dragDistance = dpToPx(50) // 기본 거리를 줄임
            
            // 손가락 아이콘 생성 (원형 배경)
            val touchPoint = View(this)
            touchPoint.layoutParams = FrameLayout.LayoutParams(dpToPx(24), dpToPx(24))
            touchPoint.background = androidx.core.content.ContextCompat.getDrawable(
                this, android.R.drawable.radiobutton_off_background
            )
            DrawableCompat.setTint(touchPoint.background, Color.WHITE)
            touchPoint.alpha = 0.9f
            
            // 시작 위치 설정 (정확히 남은 시간 텍스트뷰 위에 위치)
            touchPoint.translationX = centerX - dpToPx(12)
            touchPoint.translationY = centerY - dpToPx(12)
            
            // 간단한 설명 텍스트
            val tutorialText = TextView(this)
            tutorialText.text = "드래그하여 이동 / 더블 탭하여 원위치\n(화면을 터치하여 닫기)"
            tutorialText.setTextColor(Color.WHITE)
            tutorialText.setBackgroundResource(androidx.appcompat.R.drawable.tooltip_frame_dark)
            tutorialText.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            val textParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            textParams.gravity = Gravity.CENTER
            textParams.bottomMargin = dpToPx(50)
            tutorialText.layoutParams = textParams
            
            // 더블 탭 힌트 추가
            val doubleTapHint = TextView(this)
            doubleTapHint.text = "더블 탭"
            doubleTapHint.setTextColor(Color.WHITE)
            doubleTapHint.setBackgroundResource(androidx.appcompat.R.drawable.tooltip_frame_dark)
            doubleTapHint.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val hintParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            doubleTapHint.layoutParams = hintParams
            
            // 더블 탭 힌트 위치 설정
            doubleTapHint.translationX = touchPoint.translationX + (dragDirectionX * dpToPx(15))
            doubleTapHint.translationY = touchPoint.translationY - dpToPx(20)
            doubleTapHint.alpha = 0f // 초기에는 숨김
            
            // 드래그 방향 화살표 추가
            val arrowView = androidx.appcompat.widget.AppCompatImageView(this)
            arrowView.layoutParams = FrameLayout.LayoutParams(dpToPx(100), dpToPx(30))
            arrowView.setImageResource(android.R.drawable.arrow_down_float)
            arrowView.rotation = 45f // 드래그 방향으로 회전
            arrowView.alpha = 0f // 초기에는 투명
            
            // 화살표 색상 설정
            val arrowDrawable = arrowView.drawable.mutate()
            DrawableCompat.setTint(arrowDrawable, Color.WHITE)
            arrowView.setImageDrawable(arrowDrawable)
            
            // 화살표 위치 설정
            arrowView.translationX = centerX
            arrowView.translationY = centerY + dpToPx(10)
            
            // 핸들러 참조 저장
            val handlerList = mutableListOf<android.os.Handler>()
            
            // 애니메이션 루프를 취소하기 위한 함수
            fun cancelAllAnimations() {
                // 튜토리얼 비활성화
                isTutorialActive = false
                
                // 모든 핸들러 콜백 제거
                for (handler in handlerList) {
                    handler.removeCallbacksAndMessages(null)
                }
                
                // 모든 뷰 애니메이션 취소
                touchPoint.clearAnimation()
                arrowView.clearAnimation()
                doubleTapHint.clearAnimation()
                globalRemainingTimeTextView.clearAnimation()
                
                // 남은 시간 표시 뷰 원위치로
                globalRemainingTimeTextView.animate().cancel()
                globalRemainingTimeTextView.animate()
                    .x(originalTimeViewX)
                    .y(originalTimeViewY)
                    .setDuration(300)
                    .start()
            }
            
            // 컨테이너에 추가
            tutorialContainer.addView(background)
            tutorialContainer.addView(tutorialText)
            tutorialContainer.addView(arrowView)
            tutorialContainer.addView(doubleTapHint)
            tutorialContainer.addView(touchPoint)
            
            // 루트 레이아웃에 추가
            val rootLayout = findViewById<FrameLayout>(R.id.rootLayout)
            rootLayout.addView(tutorialContainer)
            
            // 클릭 시 튜토리얼 종료 및 남은 시간 표시 원위치로
            tutorialContainer.setOnClickListener {
                // 애니메이션 중지
                cancelAllAnimations()
                
                // 튜토리얼 제거
                rootLayout.removeView(tutorialContainer)
            }
            
            // 애니메이션 재생 및 루프 함수
            fun startAnimationCycle() {
                // 튜토리얼이 활성 상태가 아니면 중지
                if (!isTutorialActive) return
                
                // 애니메이션 초기화
                touchPoint.translationX = centerX - dpToPx(12)
                touchPoint.translationY = centerY - dpToPx(12)
                touchPoint.alpha = 0.9f
                
                arrowView.translationX = centerX
                arrowView.translationY = centerY + dpToPx(10)
                arrowView.alpha = 0f
                
                doubleTapHint.alpha = 0f
                
                // 실제 남은 시간 표시 뷰를 초기 위치로 되돌림
                globalRemainingTimeTextView.animate()
                    .x(originalTimeViewX)
                    .y(originalTimeViewY)
                    .setDuration(300)
                    .start()
                
                // 드래그 애니메이션 시작
                // 터치 포인트 애니메이션
                val touchDragAnimX = ObjectAnimator.ofFloat(touchPoint, "translationX", 
                    touchPoint.translationX, 
                    touchPoint.translationX + (dragDistance * dragDirectionX))
                touchDragAnimX.duration = 1000
                
                val touchDragAnimY = ObjectAnimator.ofFloat(touchPoint, "translationY",
                    touchPoint.translationY, 
                    touchPoint.translationY + (dragDistance/2 * dragDirectionY))
                touchDragAnimY.duration = 1000
                
                // 실제 남은 시간 표시도 함께 움직임
                globalRemainingTimeTextView.animate()
                    .x(originalTimeViewX + (dragDistance * dragDirectionX))
                    .y(originalTimeViewY + (dragDistance/2 * dragDirectionY))
                    .setDuration(1000)
                    .start()
                
                // 애니메이션 시작
                touchDragAnimX.start()
                touchDragAnimY.start()
                
                // 화살표 애니메이션 시작
                val arrowHandler = android.os.Handler(Looper.getMainLooper())
                handlerList.add(arrowHandler)
                
                arrowHandler.postDelayed({
                    // 튜토리얼이 비활성화되었으면 중지
                    if (!isTutorialActive) return@postDelayed
                    
                    // 화살표 페이드인 및 위치 이동
                    ObjectAnimator.ofFloat(arrowView, "alpha", 0f, 1f).apply {
                        duration = 500
                        start()
                    }
                    
                    // 화살표 위치 설정 - 화면 안에 표시
                    arrowView.translationX = centerX + (dragDirectionX * dpToPx(10))
                    arrowView.translationY = centerY + (dragDirectionY * dpToPx(10))
                    
                    // 화살표 이동 (남은 시간 텍스트뷰에서 드래그 방향으로)
                    ObjectAnimator.ofFloat(arrowView, "translationX", 
                        arrowView.translationX, 
                        arrowView.translationX + (dragDistance/2 * dragDirectionX)).apply {
                        duration = 800
                        start()
                    }
                    
                    ObjectAnimator.ofFloat(arrowView, "translationY", 
                        arrowView.translationY, 
                        arrowView.translationY + (dragDistance/4 * dragDirectionY)).apply {
                        duration = 800
                        start()
                    }
                }, 200)
                
                // 드래그 후 더블 탭 애니메이션
                val tapHandler = android.os.Handler(Looper.getMainLooper())
                handlerList.add(tapHandler)
                
                tapHandler.postDelayed({
                    // 튜토리얼이 비활성화되었으면 중지
                    if (!isTutorialActive) return@postDelayed
                    
                    // 화살표 페이드 아웃
                    ObjectAnimator.ofFloat(arrowView, "alpha", 1f, 0f).apply {
                        duration = 300
                        start()
                    }
                    
                    // 더블 탭 힌트 설정 및 표시
                    // 드래그된 위치에 따라 더블 탭 힌트 위치 조정
                    doubleTapHint.translationX = touchPoint.translationX + (dragDirectionX * dpToPx(15))
                    doubleTapHint.translationY = touchPoint.translationY - dpToPx(20)
                    
                    // 더블 탭 힌트 표시
                    ObjectAnimator.ofFloat(doubleTapHint, "alpha", 0f, 1f).apply {
                        duration = 300
                        start()
                    }
                    
                    // 탭 애니메이션 (손가락을 위아래로 움직임)
                    val tapAnim1 = ObjectAnimator.ofFloat(touchPoint, "translationY", 
                        touchPoint.translationY + (dragDistance/2 * dragDirectionY), 
                        (touchPoint.translationY + (dragDistance/2 * dragDirectionY)) - dpToPx(15),
                        touchPoint.translationY + (dragDistance/2 * dragDirectionY))
                    tapAnim1.duration = 300
                    tapAnim1.start()
                    
                    // 터치 포인트 깜빡임 1
                    val touchAlphaAnim1 = ObjectAnimator.ofFloat(touchPoint, "alpha", 0.9f, 0.4f, 0.9f)
                    touchAlphaAnim1.duration = 300
                    touchAlphaAnim1.start()
                    
                    val tap2Handler = android.os.Handler(Looper.getMainLooper())
                    handlerList.add(tap2Handler)
                    
                    tap2Handler.postDelayed({
                        // 튜토리얼이 비활성화되었으면 중지
                        if (!isTutorialActive) return@postDelayed
                        
                        // 두 번째 탭 애니메이션
                        val tapAnim2 = ObjectAnimator.ofFloat(touchPoint, "translationY", 
                            touchPoint.translationY + (dragDistance/2 * dragDirectionY), 
                            (touchPoint.translationY + (dragDistance/2 * dragDirectionY)) - dpToPx(15),
                            touchPoint.translationY + (dragDistance/2 * dragDirectionY))
                        tapAnim2.duration = 300
                        tapAnim2.start()
                        
                        // 터치 포인트 깜빡임 2
                        val touchAlphaAnim2 = ObjectAnimator.ofFloat(touchPoint, "alpha", 0.9f, 0.4f, 0.9f)
                        touchAlphaAnim2.duration = 300
                        touchAlphaAnim2.start()
                        
                        val returnHandler = android.os.Handler(Looper.getMainLooper())
                        handlerList.add(returnHandler)
                        
                        returnHandler.postDelayed({
                            // 튜토리얼이 비활성화되었으면 중지
                            if (!isTutorialActive) return@postDelayed
                            
                            // 더블 탭 힌트 숨기기
                            ObjectAnimator.ofFloat(doubleTapHint, "alpha", 1f, 0f).apply {
                                duration = 300
                                start()
                            }
                            
                            // 터치 포인트 원위치 애니메이션
                            val touchReturnAnimX = ObjectAnimator.ofFloat(touchPoint, "translationX",
                                touchPoint.translationX + (dragDistance * dragDirectionX), centerX - dpToPx(12))
                            touchReturnAnimX.duration = 600
                            touchReturnAnimX.start()
                            
                            val touchReturnAnimY = ObjectAnimator.ofFloat(touchPoint, "translationY",
                                touchPoint.translationY + (dragDistance/2 * dragDirectionY), centerY - dpToPx(12))
                            touchReturnAnimY.duration = 600
                            touchReturnAnimY.start()
                            
                            // 실제 남은 시간 표시 뷰를 원래 위치로 되돌림
                            globalRemainingTimeTextView.animate()
                                .x(originalTimeViewX)
                                .y(originalTimeViewY)
                                .setDuration(600)
                                .start()
                            
                            // 잠시 대기 후 다음 애니메이션 사이클 시작
                            val cycleHandler = android.os.Handler(Looper.getMainLooper())
                            handlerList.add(cycleHandler)
                            
                            cycleHandler.postDelayed({
                                // 튜토리얼이 비활성화되었으면 중지
                                if (!isTutorialActive) return@postDelayed
                                
                                // 애니메이션 사이클 다시 시작
                                startAnimationCycle()
                            }, 1000) // 1초 대기 후 다시 시작
                            
                        }, 600)
                    }, 400)
                }, 1500)
            }
            
            // 첫 번째 애니메이션 사이클 시작
            startAnimationCycle()
            
        } catch (e: Exception) {
            // 오류 발생 시 기존 메시지 표시로 폴백
            MessageManager.showMessage(this, "남은 시간 표시를 드래그하여 이동하거나 더블 탭하여 원위치로 되돌릴 수 있습니다")
        }
    }
    
    /**
     * dp 값을 픽셀로 변환
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // Fragment를 표시하는 메소드
    fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        val transaction = supportFragmentManager.beginTransaction()

        if (existingFragment == null) {
            transaction.setCustomAnimations(
                R.anim.fragment_slide_in,
                R.anim.fragment_slide_out,
                R.anim.fragment_pop_in,
                R.anim.fragment_pop_out
            )
            transaction.replace(R.id.contentFrame, fragment, tag)
        } else {
            transaction.setCustomAnimations(
                R.anim.fragment_pop_in,
                R.anim.fragment_pop_out
            )
            transaction.show(existingFragment)
        }

        transaction.commit()
    }

    // 앱 데이터를 저장하고 종료하는 public 메소드
    fun saveDataAndExit() {
        // 필요한 데이터 저장 로직 실행
        // 예: 주식 데이터, 게임 상태 등 저장

        // 앱 종료
        finishAffinity()
    }

    override fun onStop() {
        super.onStop()
        stockViewModel.saveStockData() // 기존 주식 데이터 저장
        assetViewModel.saveAssetToPreferences() // 추가: 자산 데이터 저장
    }

    // 버튼 활성화/비활성화 헬퍼 메서드 추가
    private fun setAllButtonsEnabled(enabled: Boolean) {
        // 모든 기능 버튼 참조
        val buttonAlba = findViewById<Button>(R.id.buttonAlba)
        val buttonStock = findViewById<Button>(R.id.buttonStock)
        val buttonRealEstate = findViewById<Button>(R.id.buttonRealEstate)
        val buttonEarnMoney = findViewById<Button>(R.id.buttonEarnMoney)
        val buttonMyInfo = findViewById<Button>(R.id.buttonMyInfo)
        val buttonItem = findViewById<Button>(R.id.buttonItem)
        val buttonBank = findViewById<Button>(R.id.buttonBank)
        val buttonCasino = findViewById<Button>(R.id.buttonCasino)
        val buttonLotto = findViewById<Button>(R.id.buttonLotto)
        val buttonSettings = findViewById<Button>(R.id.buttonSettings)
        
        // ButtonHelper를 사용하여 버튼 상태 설정
        val buttons = listOf(
            buttonAlba, buttonStock, buttonRealEstate, buttonEarnMoney,
            buttonMyInfo, buttonItem, buttonBank, buttonCasino, buttonLotto, buttonSettings
        )
        com.example.p20.helpers.ButtonHelper.setButtonsEnabled(buttons, enabled)
    }

    /**
     * 남은 시간 표시를 드래그로 이동할 수 있도록 설정하는 메서드
     * 가장 단순한 방식으로 구현
     */
    private fun setupDraggableTimeView() {
        // 기본 변수 설정
        val timeView = globalRemainingTimeTextView
        var dX = 0f
        var dY = 0f
        var lastAction = 0
        
        // 초기 위치 저장을 위한 변수
        var originalX = 0f
        var originalY = 0f
        var isInitialPositionSaved = false
        
        // 더블 탭 감지기
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                // 초기 위치가 저장되어 있으면 그 위치로 복원
                if (isInitialPositionSaved) {
                    timeView.animate()
                        .x(originalX)
                        .y(originalY)
                        .setDuration(300)
                        .withStartAction {
                            // 복원 효과 시작
                            timeView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
                        }
                        .withEndAction {
                            // 효과 종료
                            timeView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                            MessageManager.showMessage(this@MainActivity, "시간 표시가 원래 위치로 돌아갔습니다")
                        }
                        .start()
                }
                return true
            }
        })
        
        // 화면이 처음 그려진 후에 초기 위치 저장
        timeView.post {
            originalX = timeView.x
            originalY = timeView.y
            isInitialPositionSaved = true
            
            android.util.Log.d("TimeView", "초기 위치 저장: x=$originalX, y=$originalY")
        }
        
        // 터치 이벤트 설정
        timeView.setOnTouchListener { view, event ->
            // 제스처 감지기에 이벤트 전달 (더블 탭 감지용)
            if (gestureDetector.onTouchEvent(event)) {
                return@setOnTouchListener true
            }
            
            // 원시 터치 이벤트 처리
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 초기 위치 저장
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    lastAction = android.view.MotionEvent.ACTION_DOWN
                    
                    // 터치 피드백
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                    true
                }
                
                android.view.MotionEvent.ACTION_MOVE -> {
                    // 새 위치 계산
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    
                    // 화면 경계 계산
                    val parentView = view.parent as android.view.View
                    val minX = 0f
                    val maxX = parentView.width - view.width
                    val minY = 0f
                    val maxY = parentView.height - view.height
                    
                    // 경계 내에서만 이동
                    if (newX >= minX && newX <= maxX) {
                        view.x = newX
                    }
                    
                    if (newY >= minY && newY <= maxY) {
                        view.y = newY
                    }
                    
                    lastAction = android.view.MotionEvent.ACTION_MOVE
                    true
                }
                
                android.view.MotionEvent.ACTION_UP -> {
                    // 클릭 감지 (ACTION_DOWN 이후 이동 없이 ACTION_UP이 발생한 경우)
                    if (lastAction == android.view.MotionEvent.ACTION_DOWN) {
                        view.performClick()
                    }
                    
                    // 터치 효과 제거
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    
                    // 이동 완료 시각적 피드백
                    view.animate()
                        .alpha(0.7f)
                        .setDuration(100)
                        .withEndAction {
                            view.animate()
                                .alpha(1.0f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                    
                    true
                }
                
                android.view.MotionEvent.ACTION_CANCEL -> {
                    // 터치 효과 제거
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    true
                }
                
                else -> false
            }
        }
    }
}
