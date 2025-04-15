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
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialGravity: Int = 0

    // 클래스 변수 추가 - 애니메이션 객체들 저장
    private var timeWarningPulseAnimator: android.animation.ObjectAnimator? = null
    private var timeBlinkAnimation: android.view.animation.AlphaAnimation? = null
    private var freezeScaleAnimation: android.animation.ValueAnimator? = null
    private var visionNarrowingScaleAnimator: android.animation.ValueAnimator? = null
    private var heartbeatAnimator: android.animation.ObjectAnimator? = null
    private var shakeAnimation: android.view.animation.TranslateAnimation? = null
    private var flashAnimator: android.animation.ObjectAnimator? = null
    private var warningEffectLevel = 0 // 0: 없음, 1: 약함, 2: 중간, 3: 강함

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        // LiveData 감시하여 UI 업데이트
        timeViewModel.time.observe(this) { newTime ->
            timeInfo.text = "게임시간: $newTime"
        }

        // 전역 남은 시간 UI 업데이트 추가
        globalRemainingTimeTextView = findViewById(R.id.globalRemainingTimeInfo) // 텍스트뷰 참조

        // 남은 시간 표시를 드래그로 이동할 수 있도록 설정
        setupDraggableTimeView()

        // 시간 위험 효과를 위한 뷰 찾기
        val timeWarningEffect = findViewById<View>(R.id.timeWarningEffect)
        // 사용하지 않는 효과 뷰 참조 제거

        timeViewModel.remainingTime.observe(this) { remainingSeconds ->
            // 텍스트 업데이트 (초 단위)
            globalRemainingTimeTextView.text = "남은 시간: ${remainingSeconds}초"

            // 15초 이하일 때 효과 적용
            if (remainingSeconds <= 15) {
                // 텍스트 깜빡임 애니메이션
                if (timeBlinkAnimation == null) {
                    timeBlinkAnimation = AlphaAnimation(0.0f, 1.0f).apply {
                        duration = 500
                        repeatMode = Animation.REVERSE
                        repeatCount = Animation.INFINITE
                    }
                    globalRemainingTimeTextView.startAnimation(timeBlinkAnimation)
                }

                // 위급 상황 효과 적용
                setupEmergencyEffects(remainingSeconds)
            } else {
                // 15초 이상일 때는 모든 효과 제거
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
                // 모든 시각적 효과 중지 - 메서드로 추출
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
        // 타이밍 알바와 원 알바 뷰모델 초기화 제거

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
        val slidePanel = findViewById<LinearLayout>(R.id.slidePanel)

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

        buttonAlba.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(AlbaFragment(), "AlbaFragment")
        }

        buttonStock.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(StockFragment(), "StockFragment")
        }

        buttonRealEstate.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(RealEstateFragment(), "RealEstateFragment")
        }

        buttonCasino.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(CasinoFragment(), "CasinoFragment")
        }

        buttonLotto.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(LottoFragment(), "LottoFragment")
        }

        buttonMyInfo.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(MyInfoFragment(), "`MyInfoFragment`")
        }

        buttonBank.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(BankFragment(), "BankFragment")
        }

        buttonItem.setOnClickListener {
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(ItemFragment(), "ItemFragment")
        }

        buttonEarnMoney.setOnClickListener {
            if (slidePanel.visibility == View.VISIBLE) {
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
                // 버튼 깜빡임 효과
                val buttonFlash = AlphaAnimation(1.0f, 0.4f)
                buttonFlash.duration = 200
                buttonFlash.repeatCount = 1
                buttonFlash.repeatMode = Animation.REVERSE
                buttonEarnMoney.startAnimation(buttonFlash)

                // 패널 나타나는 효과
                slidePanel.visibility = View.VISIBLE
                val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                slidePanel.startAnimation(slideUp)
            }
        }

        // 설정 버튼 클릭 리스너 설정
        val buttonSettings = findViewById<Button>(R.id.buttonSettings)
        buttonSettings.setOnClickListener {
            val settingsDialog = SettingsDialogFragment()
            settingsDialog.show(supportFragmentManager, SettingsDialogFragment.TAG)
        }
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

    override fun onStop() {
        super.onStop()
        stockViewModel.saveStockData() // 기존 주식 데이터 저장
        assetViewModel.saveAssetToPreferences() // 추가: 자산 데이터 저장
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱이 완전히 종료될 때 데이터 저장
        stockViewModel.saveStockData()
        assetViewModel.saveAssetToPreferences()
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
        }
    }

    // Fragment를 표시하는 메소드
    fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }

    // 앱 데이터를 저장하고 종료하는 public 메소드
    fun saveDataAndExit() {
        // 필요한 데이터 저장 로직 실행
        // 예: 주식 데이터, 게임 상태 등 저장

        // 앱 종료
        finishAffinity()
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
        
        // 안내 메시지
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            MessageManager.showMessage(this, "남은 시간 표시를 드래그하여 이동하거나 더블 탭하여 원위치로 되돌릴 수 있습니다")
        }, 3000)
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
        
        // 모든 버튼의 활성화 상태 설정
        buttonAlba.isEnabled = enabled
        buttonStock.isEnabled = enabled
        buttonRealEstate.isEnabled = enabled
        buttonEarnMoney.isEnabled = enabled
        buttonMyInfo.isEnabled = enabled
        buttonItem.isEnabled = enabled
        buttonBank.isEnabled = enabled
        buttonCasino.isEnabled = enabled
        buttonLotto.isEnabled = enabled
        buttonSettings.isEnabled = enabled
        
        // 비활성화 시 버튼 투명도 조정으로 시각적 피드백 제공
        val alpha = if (enabled) 1.0f else 0.5f
        buttonAlba.alpha = alpha
        buttonStock.alpha = alpha
        buttonRealEstate.alpha = alpha
        buttonEarnMoney.alpha = alpha
        buttonMyInfo.alpha = alpha
        buttonItem.alpha = alpha
        buttonBank.alpha = alpha
        buttonCasino.alpha = alpha
        buttonLotto.alpha = alpha
        buttonSettings.alpha = alpha
    }

    // 심장박동 및 화면 흔들림 효과 구현
    private fun setupEmergencyEffects(remainingSeconds: Int) {
        // contentContainer 참조 가져오기
        val contentContainer = findViewById<FrameLayout>(R.id.contentContainer)
        val timeWarningEffect = findViewById<View>(R.id.timeWarningEffect)
        val flashEffect = findViewById<View>(R.id.flashEffect)
        
        // 효과 레벨 결정
        val newEffectLevel = when {
            remainingSeconds > 15 -> 0 // 효과 없음
            remainingSeconds > 10 -> 1 // 약한 효과
            remainingSeconds > 5 -> 2  // 중간 효과
            else -> 3                  // 강한 효과
        }
        
        // 효과 레벨이 변경된 경우에만 새로운 효과 설정
        if (newEffectLevel != warningEffectLevel) {
            warningEffectLevel = newEffectLevel
            
            // 기존 애니메이션 정리
            heartbeatAnimator?.cancel()
            heartbeatAnimator = null
            shakeAnimation?.cancel()
            contentContainer.clearAnimation()
            
            when (warningEffectLevel) {
                0 -> {
                    // 모든 효과 제거
                    stopAllAnimations()
                    return
                }
                1 -> {
                    // 약한 효과: 미세한 심장박동만
                    heartbeatAnimator = android.animation.ObjectAnimator.ofFloat(
                        contentContainer, "scaleX", 1.0f, 1.01f
                    ).apply {
                        duration = 800
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                        
                        // Y축 스케일도 함께 변경
                        addUpdateListener { animator ->
                            val value = animator.animatedValue as Float
                            contentContainer.scaleY = value
                        }
                        
                        start()
                    }
                    
                    // 약한 빨간색 효과
                    timeWarningEffect.visibility = View.VISIBLE
                    timeWarningEffect.animate().alpha(0.2f).setDuration(500).start()
                }
                2 -> {
                    // 중간 효과: 심장박동 + 약한 흔들림
                    heartbeatAnimator = android.animation.ObjectAnimator.ofFloat(
                        contentContainer, "scaleX", 1.0f, 1.02f
                    ).apply {
                        duration = 500
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                        
                        addUpdateListener { animator ->
                            val value = animator.animatedValue as Float
                            contentContainer.scaleY = value
                        }
                        
                        start()
                    }
                    
                    // 약한 흔들림 효과
                    shakeAnimation = android.view.animation.TranslateAnimation(
                        -2f, 2f, -1f, 1f
                    ).apply {
                        duration = 100
                        repeatCount = android.view.animation.Animation.INFINITE
                        repeatMode = android.view.animation.Animation.REVERSE
                    }
                    contentContainer.startAnimation(shakeAnimation)
                    
                    // 중간 빨간색 효과
                    timeWarningEffect.visibility = View.VISIBLE
                    timeWarningEffect.animate().alpha(0.4f).setDuration(500).start()
                    
                    // 간헐적 플래시 효과 (10초에 한번)
                    scheduleFlashEffect(10000)
                }
                3 -> {
                    // 강한 효과: 빠른 심장박동 + 강한 흔들림
                    heartbeatAnimator = android.animation.ObjectAnimator.ofFloat(
                        contentContainer, "scaleX", 1.0f, 1.04f
                    ).apply {
                        duration = 300
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                        
                        addUpdateListener { animator ->
                            val value = animator.animatedValue as Float
                            contentContainer.scaleY = value
                        }
                        
                        start()
                    }
                    
                    // 강한 흔들림 효과
                    shakeAnimation = android.view.animation.TranslateAnimation(
                        -5f, 5f, -3f, 3f
                    ).apply {
                        duration = 50
                        repeatCount = android.view.animation.Animation.INFINITE
                        repeatMode = android.view.animation.Animation.REVERSE
                    }
                    contentContainer.startAnimation(shakeAnimation)
                    
                    // 강한 빨간색 효과
                    timeWarningEffect.visibility = View.VISIBLE
                    timeWarningEffect.animate().alpha(0.6f).setDuration(500).start()
                    
                    // 빈번한 플래시 효과 (3초에 한번)
                    scheduleFlashEffect(3000)
                }
            }
        }
    }

    // 번쩍임 효과를 주기적으로 실행
    private fun scheduleFlashEffect(intervalMs: Int) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val flashEffect = findViewById<View>(R.id.flashEffect)
        
        val flashRunnable = object : Runnable {
            override fun run() {
                if (warningEffectLevel >= 2) {
                    // 번쩍임 효과 실행
                    flashEffect.visibility = View.VISIBLE
                    flashEffect.alpha = 0.3f
                    
                    flashEffect.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            flashEffect.visibility = View.INVISIBLE
                        }
                        .start()
                    
                    // 다음 번쩍임 예약
                    handler.postDelayed(this, intervalMs.toLong())
                }
            }
        }
        
        // 첫 번쩍임 예약
        handler.postDelayed(flashRunnable, intervalMs.toLong())
    }

    // 모든 애니메이션을 중지하는 헬퍼 메서드 수정
    private fun stopAllAnimations() {
        // 효과 레벨 초기화
        warningEffectLevel = 0
        
        // 시간 텍스트뷰 애니메이션 중지
        globalRemainingTimeTextView.clearAnimation()
        timeBlinkAnimation = null
        
        // 콘텐츠 컨테이너 애니메이션 중지
        val contentContainer = findViewById<FrameLayout>(R.id.contentContainer)
        contentContainer.clearAnimation()
        contentContainer.scaleX = 1.0f
        contentContainer.scaleY = 1.0f
        
        // 흔들림 효과 중지
        shakeAnimation = null
        
        // 심장박동 효과 중지
        heartbeatAnimator?.cancel()
        heartbeatAnimator = null
        
        // 빨간색 효과 중지
        val timeWarningEffect = findViewById<View>(R.id.timeWarningEffect)
        timeWarningEffect.clearAnimation()
        timeWarningEffect.visibility = View.INVISIBLE
        timeWarningEffect.alpha = 0f
        
        // 시야 축소 효과 중지
        val visionNarrowingEffect = findViewById<View>(R.id.visionNarrowingEffect)
        visionNarrowingEffect.clearAnimation()
        visionNarrowingEffect.visibility = View.INVISIBLE
        visionNarrowingEffect.alpha = 0f
        
        // 플래시 효과 중지
        val flashEffect = findViewById<View>(R.id.flashEffect)
        flashEffect.clearAnimation()
        flashEffect.visibility = View.INVISIBLE
        flashEffect.alpha = 0f
        
        // 기존 애니메이션 변수 정리
        freezeScaleAnimation?.cancel()
        freezeScaleAnimation = null
        visionNarrowingScaleAnimator?.cancel()
        visionNarrowingScaleAnimator = null
        flashAnimator?.cancel()
        flashAnimator = null
    }
}
