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

class MainActivity : AppCompatActivity() {

    lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
    private lateinit var assetTextView: TextView // 최상단 자산 표시
    lateinit var stockViewModel: StockViewModel
    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언
    private lateinit var realEstateViewModel: RealEstateViewModel // 부동산 뷰모델 추가
    private lateinit var albaViewModel: AlbaViewModel // 알바 뷰모델 추가
    private lateinit var globalRemainingTimeTextView: TextView // 전역 남은 시간 표시 텍스트뷰
    private lateinit var viewModelFactory: ViewModelFactory // viewModelFactory 클래스 변수로 선언

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
        // 추가 효과 뷰 찾기
        val distortionEffect = findViewById<View>(R.id.distortionEffect)
        val screenCrackEffect = findViewById<View>(R.id.screenCrackEffect)

        timeViewModel.remainingTime.observe(this) { remainingSeconds ->
            // 텍스트 업데이트 (초 단위)
            globalRemainingTimeTextView.text = "남은 시간: ${remainingSeconds}초"

            // 15초 이하일 때 깜빡이는 애니메이션과 빨간색 화면 효과
            if (remainingSeconds <= 15) {
                // 텍스트 깜빡임 애니메이션
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 500
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                globalRemainingTimeTextView.startAnimation(anim)

                // 빨간색 화면 효과 표시
                timeWarningEffect.visibility = View.VISIBLE

                // 남은 시간에 따라 효과의 강도(알파값) 조절
                // 15초에서 0초로 갈수록 0.2에서 0.9로 알파값 증가 (더 강한 빨간색)
                val intensity = 0.2f + (0.7f * (15 - remainingSeconds) / 15f)

                // 화면 왜곡 효과 (10초 이하부터 시작)
                if (remainingSeconds <= 10) {
                    distortionEffect.visibility = View.VISIBLE
                    // 10초에서 0초로 갈수록 0.1에서 0.5로 알파값 증가
                    val distortionIntensity = 0.1f + (0.4f * (10 - remainingSeconds) / 10f)
                    distortionEffect.alpha = distortionIntensity

                    // 화면 왜곡 효과에 떨림 애니메이션 추가 (남은 시간이 적을수록 더 심하게 떨림)
                    val shakeAmount = (5 + (15 * (10 - remainingSeconds) / 10)).toInt()
                    val shakeAnim = android.view.animation.TranslateAnimation(
                        -shakeAmount.toFloat(), shakeAmount.toFloat(),
                        -shakeAmount.toFloat(), shakeAmount.toFloat()
                    )
                    shakeAnim.duration = 100
                    shakeAnim.repeatCount = android.view.animation.Animation.INFINITE
                    shakeAnim.repeatMode = android.view.animation.Animation.REVERSE

                    // 기존 애니메이션 제거 후 새 애니메이션 시작
                    distortionEffect.clearAnimation()
                    distortionEffect.startAnimation(shakeAnim)
                } else {
                    distortionEffect.visibility = View.INVISIBLE
                    distortionEffect.clearAnimation()
                }

                // 화면 깨짐 효과 (5초 이하부터 시작)
                if (remainingSeconds <= 5) {
                    screenCrackEffect.visibility = View.VISIBLE
                    // 5초에서 0초로 갈수록 0.2에서 0.7로 알파값 증가
                    val crackIntensity = 0.2f + (0.5f * (5 - remainingSeconds) / 5f)
                    screenCrackEffect.alpha = crackIntensity

                    // 화면 깨짐 효과에 펄스 애니메이션 추가 (심박동과 비슷하게)
                    val pulseAnim2 = android.animation.ObjectAnimator.ofFloat(
                        screenCrackEffect,
                        "alpha",
                        crackIntensity * 0.7f,
                        crackIntensity * 1.2f
                    )

                    pulseAnim2.duration = 150
                    pulseAnim2.repeatCount = android.animation.ObjectAnimator.INFINITE
                    pulseAnim2.repeatMode = android.animation.ObjectAnimator.REVERSE

                    // 기존 애니메이션 제거 후 새 애니메이션 시작
                    screenCrackEffect.clearAnimation()
                    pulseAnim2.start()
                } else {
                    screenCrackEffect.visibility = View.INVISIBLE
                    screenCrackEffect.clearAnimation()
                    screenCrackEffect.animate().cancel()
                }

                // 심박동 애니메이션 효과 - 시간이 줄어들수록 더 빠르게 펄스
                val pulseAnim = android.animation.ObjectAnimator.ofFloat(
                    timeWarningEffect,
                    "alpha",
                    intensity * 0.4f, // 최소 알파값 (더 큰 변화를 위해 40%로 조정)
                    intensity * 1.3f  // 최대 알파값 (더 강한 효과를 위해 130%로 조정)
                )

                // 남은 시간이 적을수록 더 빠르게 진동 (300ms에서 100ms까지)
                // 맥박 효과를 더 극적으로 변경
                val pulseDuration = (300 - 200 * (15 - remainingSeconds) / 15).toLong().coerceAtLeast(100)
                pulseAnim.duration = pulseDuration
                pulseAnim.repeatCount = android.animation.ObjectAnimator.INFINITE
                pulseAnim.repeatMode = android.animation.ObjectAnimator.REVERSE

                // 기존 애니메이션 제거 후 새 애니메이션 시작
                timeWarningEffect.clearAnimation()
                pulseAnim.start()

            } else {
                // 15초 이상일 때는 모든 효과 제거
                globalRemainingTimeTextView.clearAnimation()

                timeWarningEffect.visibility = View.INVISIBLE
                timeWarningEffect.alpha = 0f
                timeWarningEffect.clearAnimation()

                distortionEffect.visibility = View.INVISIBLE
                distortionEffect.alpha = 0f
                distortionEffect.clearAnimation()

                screenCrackEffect.visibility = View.INVISIBLE
                screenCrackEffect.alpha = 0f
                screenCrackEffect.clearAnimation()
                screenCrackEffect.animate().cancel()
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
                globalRemainingTimeTextView.clearAnimation()
                // 다이얼로그가 이미 떠 있지 않다면 새로 띄움
                if (existingDialog == null) {
                    GameOverDialogFragment().show(supportFragmentManager, dialogTag)
                }
            } else {
                // 게임 오버 상태가 아니라면 다이얼로그 닫기
                existingDialog?.dismiss()
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
     */
    private fun setupDraggableTimeView() {
        // 초기 마진 값 저장 (레이아웃 속성 복원을 위해)
        val layoutParams = globalRemainingTimeTextView.layoutParams as FrameLayout.LayoutParams
        val initialMarginTop = layoutParams.topMargin
        val initialMarginEnd = layoutParams.rightMargin
        val initialGravity = layoutParams.gravity

        // 마지막 터치 위치를 저장할 변수
        var lastX = 0f
        var lastY = 0f
        var isDragging = false

        // 터치 이벤트 리스너 설정
        globalRemainingTimeTextView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 터치 시작 - 마지막 위치 저장
                    lastX = event.rawX
                    lastY = event.rawY
                    isDragging = true

                    // 터치 시 배경 강조 효과
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()

                    true
                }

                android.view.MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) return@setOnTouchListener false

                    // 이동 거리 계산
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY

                    // 뷰의 현재 위치 가져오기
                    val params = view.layoutParams as FrameLayout.LayoutParams

                    // gravity 속성 제거하고 절대 위치 사용
                    params.gravity = android.view.Gravity.NO_GRAVITY

                    // 뷰의 새 위치 계산 (화면 경계 벗어나지 않도록)
                    val newX = (view.x + deltaX).coerceIn(0f, (view.parent as View).width - view.width.toFloat())
                    val newY = (view.y + deltaY).coerceIn(0f, (view.parent as View).height - view.height.toFloat())

                    // 뷰 위치 업데이트
                    view.x = newX
                    view.y = newY

                    // 마지막 위치 업데이트
                    lastX = event.rawX
                    lastY = event.rawY

                    true
                }

                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    isDragging = false

                    // 배경 효과 원복
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()

                    // 위치가 수정된 후에는 레이아웃 매개변수를 업데이트
                    val params = view.layoutParams as FrameLayout.LayoutParams
                    params.gravity = android.view.Gravity.NO_GRAVITY
                    view.layoutParams = params

                    // 간단한 애니메이션 효과로 이동 완료 알림
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

                else -> false
            }
        }

        // 더블 탭으로 초기 위치로 돌아가는 기능 추가
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                // 초기 레이아웃 매개변수로 원복
                val params = globalRemainingTimeTextView.layoutParams as FrameLayout.LayoutParams
                params.gravity = initialGravity
                params.topMargin = initialMarginTop
                params.rightMargin = initialMarginEnd
                params.leftMargin = 0
                params.bottomMargin = 0

                // 뷰 위치 초기화
                globalRemainingTimeTextView.x = 0f
                globalRemainingTimeTextView.y = 0f

                globalRemainingTimeTextView.layoutParams = params

                // 간단한 애니메이션 효과로 위치 이동 완료 알림
                globalRemainingTimeTextView.animate()
                    .scaleX(1.2f).scaleY(1.2f)
                    .setDuration(200)
                    .withEndAction {
                        globalRemainingTimeTextView.animate()
                            .scaleX(1.0f).scaleY(1.0f)
                            .setDuration(200)
                            .start()
                    }
                    .start()

                return true
            }
        })

        // 기존 OnTouchListener와 GestureDetector 결합
        globalRemainingTimeTextView.setOnTouchListener { view, event ->
            val gestureResult = gestureDetector.onTouchEvent(event)

            if (!gestureResult) {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        // 터치 시작 - 마지막 위치 저장
                        lastX = event.rawX
                        lastY = event.rawY
                        isDragging = true

                        // 터치 시 배경 강조 효과
                        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()

                        true
                    }

                    android.view.MotionEvent.ACTION_MOVE -> {
                        if (!isDragging) return@setOnTouchListener false

                        // 이동 거리 계산
                        val deltaX = event.rawX - lastX
                        val deltaY = event.rawY - lastY

                        // 뷰의 새 위치 계산 (화면 경계 벗어나지 않도록)
                        val newX = (view.x + deltaX).coerceIn(0f, (view.parent as View).width - view.width.toFloat())
                        val newY = (view.y + deltaY).coerceIn(0f, (view.parent as View).height - view.height.toFloat())

                        // 뷰 위치 업데이트
                        view.x = newX
                        view.y = newY

                        // 마지막 위치 업데이트
                        lastX = event.rawX
                        lastY = event.rawY

                        true
                    }

                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        isDragging = false

                        // 배경 효과 원복
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()

                        true
                    }

                    else -> false
                }
            } else {
                true
            }
        }

        // 작은 도움말 표시
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            MessageManager.showMessage(this, "남은 시간 표시를 드래그하여 이동할 수 있습니다. 더블 탭으로 원위치")
        }, 3000) // 3초 후 안내 메시지
    }
}
