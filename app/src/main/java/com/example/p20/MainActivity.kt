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
            // 텍스트 업데이트 (초 단위)
            globalRemainingTimeTextView.text = "남은 시간: ${remainingSeconds}초"

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

    // onPause와 onResume에서 음악 관리
    override fun onPause() {
        super.onPause()
        soundController.pauseBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        soundController.resumeBackgroundMusic()
    }

    // 배경음악 재시작
    fun restartBackgroundMusic() {
        soundController.restartBackgroundMusic()
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
            
            // 게임 설명창이 사라진 후 시간 표시 드래그 관련 안내 메시지 표시
            showDragTimeViewMessage()
        }
    }

    /**
     * 시간 표시 드래그 안내 메시지를 표시하는 메소드
     * ExplanationFragment나 다른 곳에서 호출하여 사용
     */
    fun showDragTimeViewMessage() {
        // 메시지 표시 전 약간의 딜레이를 줌 (1초)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            MessageManager.showMessage(this, "남은 시간 표시를 드래그하여 이동하거나 더블 탭하여 원위치로 되돌릴 수 있습니다")
        }, 1000)
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
    
    // 볼륨 조절
    fun setVolume(volume: Float) {
        soundController.setVolume(volume)
    }
}
