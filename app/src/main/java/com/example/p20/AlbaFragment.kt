package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AlbaFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: AlbaViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alba, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 탭 레이아웃과 뷰페이저 초기화
        tabLayout = view.findViewById(R.id.albaTabLayout)
        viewPager = view.findViewById(R.id.albaViewPager)

        // 어댑터 설정
        adapter = AlbaViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // 탭과 뷰페이저 연결 - 탭 레이아웃 표시 활성화
        tabLayout.visibility = View.VISIBLE
        
        // TabLayoutMediator로 탭과 뷰페이저 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "클릭 알바"
                1 -> "해킹 알바"
                else -> ""
            }
        }.attach()
    }

    // 뷰페이저 어댑터 - 클릭 알바와 해킹 알바 탭 제공
    private inner class AlbaViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        
        override fun getItemCount(): Int = 2 // 클릭 알바와 해킹 알바
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ClickAlbaFragment()
                1 -> HackingAlbaFragment()
                else -> ClickAlbaFragment()
            }
        }
    }
}

class ClickAlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var earnText: TextView
    private lateinit var levelText: TextView
    private lateinit var cooldownText: TextView
    private lateinit var animationContainer: FrameLayout
    private lateinit var albaImage: ImageView
    private lateinit var activePhaseText: TextView
    private lateinit var expProgressBar: ProgressBar
    private lateinit var expTextView: TextView
    
    // 경험치 애니메이션 관련 변수
    private var isExpAnimating = false
    private var lastShownExp = 0
    private val expUpdateQueue = mutableListOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    
    // 날아가는 텍스트 애니메이션 추적용 카운터
    private var pendingAnimationCount = 0
    
    // 효과음 재생을 위한 SoundPool
    private lateinit var soundPool: SoundPool
    private var coinSoundId: Int = 0
    
    // 디바운싱을 위한 변수
    private var lastClickTime = 0L
    private val MIN_CLICK_INTERVAL = 80L // 밀리초 (0.08초)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_click_alba, container, false)

        albaViewModel = ViewModelProvider(requireActivity())[AlbaViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

        // SoundPool 초기화
        initSoundPool()

        albaImage = view.findViewById(R.id.albaImage)
        earnText = view.findViewById(R.id.earnText)
        levelText = view.findViewById(R.id.levelText)
        cooldownText = view.findViewById(R.id.cooldownText)
        animationContainer = view.findViewById(R.id.animationContainer)
        activePhaseText = view.findViewById(R.id.cooldownText)
        
        // 경험치 바 초기화
        expProgressBar = view.findViewById(R.id.expProgressBar)
        expTextView = view.findViewById(R.id.expTextView)
        
        // 경험치 바 초기 설정
        lastShownExp = albaViewModel.getClickCounter().coerceIn(0, 20)
        updateExpBar(lastShownExp)

        earnText.text = "알바 시작!"

        albaImage.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 쿨다운 상태가 아닐 때만 효과음 재생
                val isCooldown = albaViewModel.isCooldown.value ?: false
                if (!isCooldown) {
                    playCoinSound()
                }
                
                // 현재 시간 가져오기
                val currentTime = System.currentTimeMillis()
                
                // 게임 로직과 애니메이션은 디바운싱 적용
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    
                    if (albaViewModel.isCooldown.value == false && albaViewModel.isActivePhase.value == false) {
                        albaViewModel.startActivePhase()
                        val rewardAmount = albaViewModel.getRewardAmount().toLong()
                        assetViewModel.increaseAsset(rewardAmount)
                        val location = IntArray(2)
                        albaImage.getLocationOnScreen(location)
                        showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
                    } else if (albaViewModel.isActivePhase.value == true) {
                        albaViewModel.increaseAlbaLevel()
                        val rewardAmount = albaViewModel.getRewardAmount().toLong()
                        assetViewModel.increaseAsset(rewardAmount)
                        val location = IntArray(2)
                        albaImage.getLocationOnScreen(location)
                        showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
                    }
                }
            }
            true
        }

        albaViewModel.isCooldown.observe(viewLifecycleOwner, Observer { isCoolingDown ->
            updateStatusText()
        })
        albaViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { time ->
            updateStatusText()
        })
        albaViewModel.isActivePhase.observe(viewLifecycleOwner, Observer { isActive ->
            updateStatusText()
            
            // 알바 활성 상태가 끝났을 때 (false로 변경되었을 때)
            if (isActive == false && pendingAnimationCount > 0) {
                // 아직 진행 중인 애니메이션이 있으면 1초마다 경험치 업데이트 확인
                checkPendingAnimations()
            }
        })
        albaViewModel.activePhaseTime.observe(viewLifecycleOwner, Observer { time ->
            updateStatusText()
        })

        albaViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val rewardAmount = albaViewModel.getRewardAmount()
            levelText.text = "레벨: $level\n보상: ${"%,d".format(rewardAmount)}원\n(20번 클릭마다 레벨업)"
        })

        // 아이템 획득 이벤트 관찰
        albaViewModel.itemRewardEvent.observe(viewLifecycleOwner, Observer { reward ->
            if (reward != null) {
                // 아이템 획득 이벤트만 소비 (메시지는 ViewModel에서 표시)
                albaViewModel.consumeItemRewardEvent() // 이벤트 소비
            }
        })
        
        // 클릭 카운터 리셋 이벤트 관찰
        albaViewModel.clickCounterResetEvent.observe(viewLifecycleOwner, Observer { isReset ->
            if (isReset == true) {
                // 경험치 바 초기화
                expUpdateQueue.clear() // 큐 비우기
                lastShownExp = 0
                updateExpBar(0)
                
                // 경험치 바 리셋 효과 애니메이션
                val fadeOut = ObjectAnimator.ofFloat(expProgressBar, "alpha", 1f, 0.3f)
                fadeOut.duration = 300
                fadeOut.repeatMode = ObjectAnimator.REVERSE
                fadeOut.repeatCount = 1
                fadeOut.start()
                
                // 이벤트 소비
                albaViewModel.consumeClickCounterResetEvent()
            }
        })

        return view
    }

    private fun updateStatusText() {
        val isActive = albaViewModel.isActivePhase.value ?: false
        val isCooldown = albaViewModel.isCooldown.value ?: false
        val activeTime = albaViewModel.activePhaseTime.value ?: 0
        val cooldownTime = albaViewModel.cooldownTime.value ?: 0

        activePhaseText.text = when {
            isActive -> "클릭! 남은 시간: ${activeTime}초"
            isCooldown -> "쿨다운: ${cooldownTime}초"
            else -> "알바 가능!"
        }
    }

    private fun showRewardAnimation(
        x: Int,
        y: Int,
        reward: Long,
        shakeAngle: Float = 20f,
        shakeSpeed: Long = 100L,
        moveDuration: Long = 1500L,
        scaleTarget: Float = 0.5f
    ) {
        // 애니메이션 카운터 증가
        pendingAnimationCount++
        
        val rewardTextView = TextView(requireContext()).apply {
            text = "+${"%,d".format(reward)}원"
            textSize = 20f
            setTextColor(resources.getColor(R.color.reward_text, null))
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        animationContainer.addView(rewardTextView)

        rewardTextView.x = albaImage.x + x
        rewardTextView.y = albaImage.y + y

        val targetLocation = IntArray(2)
        levelText.getLocationOnScreen(targetLocation)

        val containerLocation = IntArray(2)
        animationContainer.getLocationOnScreen(containerLocation)

        val targetX = targetLocation[0] - containerLocation[0] + levelText.width / 2 - rewardTextView.width / 2
        val targetY = targetLocation[1] - containerLocation[1] + levelText.height / 2 - rewardTextView.height / 2

        val moveX = ObjectAnimator.ofFloat(rewardTextView, "x", rewardTextView.x, targetX.toFloat()).apply {
            duration = moveDuration
        }
        val moveY = ObjectAnimator.ofFloat(rewardTextView, "y", rewardTextView.y, targetY.toFloat()).apply {
            duration = moveDuration
        }
        
        // 크기 축소 효과 다시 활성화
        val scaleX = ObjectAnimator.ofFloat(rewardTextView, "scaleX", 1f, scaleTarget).apply {
            duration = moveDuration
        }
        val scaleY = ObjectAnimator.ofFloat(rewardTextView, "scaleY", 1f, scaleTarget).apply {
            duration = moveDuration
        }
        // 투명도 효과는 계속 비활성화 상태 유지

        val shake = ObjectAnimator.ofFloat(rewardTextView, "rotation", 0f, shakeAngle, -shakeAngle, shakeAngle / 1.5f, -shakeAngle / 1.5f, 0f).apply {
            this.duration = shakeSpeed
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }

        // 이동 애니메이션이 끝나면 View 제거
        moveX.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                shake.cancel()
                animationContainer.removeView(rewardTextView)
                
                // 애니메이션 종료 시 경험치 바 큐에 추가
                queueExpBarUpdate(albaViewModel.getClickCounter())
                
                // 애니메이션 카운터 감소
                pendingAnimationCount--
            }
        })

        moveX.start()
        moveY.start()
        scaleX.start()
        scaleY.start()
        shake.start()
    }

    // 아이템 획득 애니메이션 표시 - 더 이상 사용하지 않음
    // MessageManager로 대체되어 제거할 수 있지만, 코드 호환성을 위해 빈 함수로 유지
    private fun showItemRewardAnimation(reward: ItemReward) {
        // 비워둠 - 이제 MessageManager를 통해 메시지가 표시됨
    }

    /**
     * 경험치 바 업데이트를 큐에 추가합니다.
     * @param targetClickCount 목표 클릭 횟수
     */
    private fun queueExpBarUpdate(targetClickCount: Int) {
        // 0~20 사이 값으로 제한
        val targetProgress = targetClickCount.coerceIn(0, 20)
        
        // 현재 목표값이 이미 처리 중인 마지막 목표보다 작으면 무시
        if (expUpdateQueue.isNotEmpty() && targetProgress < expUpdateQueue.last()) {
            return
        }
        
        // 큐가 비어있지 않고 마지막 항목이 새 목표와 같으면 추가하지 않음
        if (expUpdateQueue.isNotEmpty() && expUpdateQueue.last() == targetProgress) {
            return
        }
        
        // 큐 비우고 최신 목표만 추가 - 중간 과정 생략
        expUpdateQueue.clear()
        expUpdateQueue.add(targetProgress)
        
        // 애니메이션 중이 아니면 애니메이션 시작
        if (!isExpAnimating) {
            processNextExpAnimation()
        }
    }
    
    /**
     * 다음 경험치 애니메이션을 처리합니다.
     */
    private fun processNextExpAnimation() {
        if (expUpdateQueue.isEmpty()) {
            isExpAnimating = false
            return
        }
        
        isExpAnimating = true
        val nextTarget = expUpdateQueue[0]
        
        // 현재 보여지는 경험치와 목표 경험치가 같으면 큐에서 제거하고 다음으로
        if (lastShownExp == nextTarget) {
            expUpdateQueue.removeAt(0)
            processNextExpAnimation()
            return
        }
        
        // 경험치는 항상 증가하도록 설정 (감소 로직 제거)
        val nextExp = if (lastShownExp < nextTarget) lastShownExp + 1 else nextTarget
        
        // 경험치 바 업데이트
        updateExpBar(nextExp)
        lastShownExp = nextExp
        
        // 목표에 도달했으면 큐에서 제거
        if (nextExp == nextTarget) {
            expUpdateQueue.removeAt(0)
        }
        
        // 경험치 바 업데이트 효과
        val pulse = ObjectAnimator.ofFloat(expProgressBar, "scaleY", 1f, 1.2f, 1f)
        pulse.duration = 150
        pulse.start()
        
        // 다음 애니메이션 예약 (일정 지연 후)
        handler.postDelayed({
            processNextExpAnimation()
        }, 150) // 150ms 간격으로 업데이트
    }
    
    /**
     * 경험치 바를 업데이트합니다.
     * @param clickCount 현재 클릭 횟수
     */
    private fun updateExpBar(clickCount: Int) {
        // 0~20 사이 값으로 제한
        val progress = clickCount.coerceIn(0, 20)
        expProgressBar.progress = progress
        expTextView.text = "$progress/20"
        
        // 경험치가 쌓일 때마다 색상 변화 효과
        val greenColor = resources.getColor(android.R.color.holo_green_light, null)
        val yellowColor = resources.getColor(android.R.color.holo_orange_light, null)
        val redColor = resources.getColor(android.R.color.holo_red_light, null)
        
        val color = when {
            progress < 7 -> greenColor
            progress < 14 -> yellowColor
            else -> redColor
        }
        
        // API 레벨 21 이상에서 색상 변경
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            expProgressBar.progressTintList = ColorStateList.valueOf(color)
        }
    }

    /**
     * 대기 중인 애니메이션이 있는지 주기적으로 확인하여 경험치를 업데이트합니다.
     */
    private fun checkPendingAnimations() {
        // 대기 중인 애니메이션이 없거나, 활성 상태가 다시 시작된 경우 확인 중지
        val isActive = albaViewModel.isActivePhase.value ?: false
        if (pendingAnimationCount <= 0 || isActive) {
            return
        }
        
        // 현재 클릭 카운터 값으로 경험치 바 업데이트
        queueExpBarUpdate(albaViewModel.getClickCounter())
        
        // 1초 후에 다시 확인
        handler.postDelayed({
            checkPendingAnimations()
        }, 1000)
    }

    /**
     * SoundPool 초기화 및 효과음 로드
     */
    private fun initSoundPool() {
        // API 레벨 21 이상에서는 AudioAttributes 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(4)  // 최대 동시 재생 수
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            // 하위 버전 호환성
            @Suppress("DEPRECATION")
            soundPool = SoundPool(4, AudioManager.STREAM_MUSIC, 0)
        }
        
        // 효과음 로드
        coinSoundId = soundPool.load(requireContext(), R.raw.coin, 1)
    }

    /**
     * 코인 효과음을 재생합니다.
     * SoundPool을 사용하여 효율적으로 효과음을 중첩 재생합니다.
     */
    private fun playCoinSound() {
        try {
            // 효과음이 로드되었는지 확인
            if (coinSoundId > 0) {
                // 효과음 재생 (좌우 볼륨은 0.5로 설정, 우선순위는 1, 반복 횟수 0, 속도 1.0)
                soundPool.play(coinSoundId, 0.5f, 0.5f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            // 효과음 재생 중 오류 발생 시 로그 출력 후 계속 진행
            android.util.Log.e("ClickAlbaFragment", "효과음 재생 오류: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null)
        
        // SoundPool 리소스 해제
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}

/**
 * 해킹 알바 프래그먼트 클래스
 * 숫자 비밀번호를 추측하는 논리 게임 제공
 */
class HackingAlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    
    // 게임 관련 변수
    private var secretCode = intArrayOf(0, 0, 0, 0) // 4자리 비밀번호
    private var attemptCount = 0 // 시도 횟수
    private var maxAttempts = 10 // 최대 시도 횟수
    private var isGameActive = false // 게임 활성화 상태
    private var currentLevel = 1 // 현재 레벨
    
    // UI 요소
    private lateinit var gameContainer: ViewGroup
    private lateinit var codeInputContainer: ViewGroup
    private lateinit var historyContainer: ViewGroup
    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var historyPanelContainer: ViewGroup
    private lateinit var historyScrollView: ScrollView
    private lateinit var startButton: Button
    private lateinit var submitButton: Button
    private lateinit var toggleHistoryButton: Button
    private lateinit var closeHistoryButton: Button
    private lateinit var levelText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var lastResultTextView: TextView
    private lateinit var digitButtons: Array<TextView>
    private lateinit var codeDigits: Array<TextView>
    
    // 기록 창 표시 상태
    private var isHistoryShown = false
    
    // 게임 데이터
    private var reward: Long = 0L
    
    // 효과음 재생을 위한 SoundPool
    private lateinit var soundPool: SoundPool
    private var correctSoundId: Int = 0
    private var wrongSoundId: Int = 0
    private var typingSoundId: Int = 0
    private var startSoundId: Int = 0
    private var successSoundId: Int = 0
    private var failSoundId: Int = 0
    private var hackingStartSoundId: Int = 0   // 해킹 시작 버튼 효과음
    private var hackingButtonSoundId: Int = 0  // 코드 입력 버튼 효과음
    
    // 핸들러
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_hacking_alba, container, false)

        albaViewModel = ViewModelProvider(requireActivity())[AlbaViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]
        
        // UI 요소 초기화
        initializeViews(view)
        
        // SoundPool 초기화
        initSoundPool()
        
        // 이벤트 리스너 설정
        setupEventListeners()
        
        // 초기 UI 상태 설정 - 게임은 비활성화 상태로 시작
        isGameActive = false
        updateUIState(false)
        
        // DrawerLayout 리스너 설정
        drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // 드로어가 슬라이드될 때의 동작
            }

            override fun onDrawerOpened(drawerView: View) {
                // 드로어가 열렸을 때 isHistoryShown 상태 업데이트
                isHistoryShown = true
                
                // 스크롤을 맨 위로 (최신 결과가 보이도록)
                historyScrollView.fullScroll(ScrollView.FOCUS_UP)
                
                android.util.Log.d("HackingAlba", "기록 사이드 패널 열림")
            }

            override fun onDrawerClosed(drawerView: View) {
                // 드로어가 닫혔을 때 isHistoryShown 상태 업데이트
                isHistoryShown = false
                
                android.util.Log.d("HackingAlba", "기록 사이드 패널 닫힘")
            }

            override fun onDrawerStateChanged(newState: Int) {
                // 드로어 상태가 변경되었을 때의 동작
            }
        })
        
        // 디버깅 로그 추가
        android.util.Log.d("HackingAlba", "프래그먼트 생성됨, 버튼 초기 상태: 시작=${startButton.isEnabled}, 제출=${submitButton.isEnabled}")
        
        return view
    }
    
    /**
     * UI 요소들을 초기화합니다.
     */
    private fun initializeViews(view: View) {
        gameContainer = view.findViewById(R.id.gameContainer)
        codeInputContainer = view.findViewById(R.id.codeInputContainer)
        historyContainer = view.findViewById(R.id.historyContainer)
        drawerLayout = view.findViewById(R.id.drawerLayout)
        historyPanelContainer = view.findViewById(R.id.historyPanelContainer)
        historyScrollView = view.findViewById(R.id.historyScrollView)
        startButton = view.findViewById<Button>(R.id.startButton)
        submitButton = view.findViewById<Button>(R.id.submitButton)
        toggleHistoryButton = view.findViewById<Button>(R.id.toggleHistoryButton)
        closeHistoryButton = view.findViewById<Button>(R.id.closeHistoryButton)
        levelText = view.findViewById(R.id.levelText)
        attemptsText = view.findViewById(R.id.attemptsText)
        feedbackText = view.findViewById(R.id.feedbackText)
        lastResultTextView = view.findViewById(R.id.lastResultTextView)
        
        // 숫자 입력 버튼 초기화
        digitButtons = Array(10) { index ->
            view.findViewById<TextView>(
                resources.getIdentifier("digit_$index", "id", requireContext().packageName)
            )
        }
        
        // 코드 표시 텍스트뷰 초기화
        codeDigits = Array(4) { index ->
            view.findViewById<TextView>(
                resources.getIdentifier("code_digit_$index", "id", requireContext().packageName)
            )
        }
        
        // 초기 텍스트 설정
        levelText.setText("Lv.${currentLevel} 해킹 알바")
        attemptsText.setText("시도: 0/${maxAttempts}")
        feedbackText.setText("해킹을 시작하려면 시작 버튼을 누르세요.")
        lastResultTextView.visibility = View.GONE
    }
    
    /**
     * SoundPool을 초기화하고 효과음을 로드합니다.
     */
    private fun initSoundPool() {
        // API 레벨 21 이상에서는 AudioAttributes 사용
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(6)  // 최대 동시 재생 수 증가
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            // 하위 버전 호환성
            @Suppress("DEPRECATION")
            soundPool = SoundPool(6, AudioManager.STREAM_MUSIC, 0)
        }
        
        // 효과음 로드
        correctSoundId = soundPool.load(requireContext(), R.raw.coin, 1) // 임시로 coin 사용
        wrongSoundId = soundPool.load(requireContext(), R.raw.coin, 1) // 임시로 coin 사용
        typingSoundId = soundPool.load(requireContext(), R.raw.alba_hacking_number, 1)
        startSoundId = soundPool.load(requireContext(), R.raw.coin, 1) // 임시로 coin 사용
        successSoundId = soundPool.load(requireContext(), R.raw.coin, 1) // 임시로 coin 사용
        failSoundId = soundPool.load(requireContext(), R.raw.coin, 1) // 임시로 coin 사용
        
        // 새로운 효과음 로드
        hackingStartSoundId = soundPool.load(requireContext(), R.raw.alba_hacking_start, 1)
        hackingButtonSoundId = soundPool.load(requireContext(), R.raw.alba_hacking_button, 1)
    }
    
    /**
     * 이벤트 리스너를 설정합니다.
     */
    private fun setupEventListeners() {
        // 시작 버튼
        startButton.setOnClickListener {
            // 해킹 시작 효과음
            playSound(hackingStartSoundId)
            startNewGame()
        }
        
        // 제출 버튼 - 명시적으로 다시 설정
        submitButton.setOnClickListener { 
            android.util.Log.d("HackingAlba", "제출 버튼 클릭됨")
            // 코드 입력 효과음
            playSound(hackingButtonSoundId)
            checkCode() 
        }
        
        // 기록 버튼 - 사이드 패널 표시
        toggleHistoryButton.setOnClickListener {
            // 패널 상태를 토글
            if (drawerLayout.isDrawerOpen(androidx.core.view.GravityCompat.END)) {
                hideHistoryDialog()
            } else {
                showHistoryDialog()
            }
        }
        
        // 기록 닫기 버튼
        closeHistoryButton.setOnClickListener {
            hideHistoryDialog()
        }
        
        // 숫자 버튼
        for (i in 0 until 10) {
            digitButtons[i].setOnClickListener {
                if (isGameActive) {
                    inputDigit(i)
                    playSound(typingSoundId)
                }
            }
        }
    }
    
    /**
     * 기록 사이드 패널을 표시합니다.
     */
    private fun showHistoryDialog() {
        // 기록 컨테이너가 비어있으면 안내 메시지 추가
        if (historyContainer.childCount == 0) {
            val emptyView = layoutInflater.inflate(R.layout.item_hacking_result, historyContainer, false)
            val codeText = emptyView.findViewById<TextView>(R.id.codeTextView)
            val resultText = emptyView.findViewById<TextView>(R.id.resultTextView)
            
            codeText.text = "기록 없음"
            resultText.text = "아직 시도한 결과가 없습니다."
            
            historyContainer.addView(emptyView)
        }
        
        // 드로어 레이아웃을 사용하여 오른쪽에서 슬라이드
        drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
        
        // 스크롤 초기화
        historyScrollView.fullScroll(ScrollView.FOCUS_UP)
        
        isHistoryShown = true
        
        // 디버깅 로그
        android.util.Log.d("HackingAlba", "기록 사이드 패널 표시됨")
    }
    
    /**
     * 기록 사이드 패널을 숨깁니다.
     */
    private fun hideHistoryDialog() {
        // 드로어 레이아웃을 사용하여 패널 닫기
        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
        
        isHistoryShown = false
        
        // 디버깅 로그
        android.util.Log.d("HackingAlba", "기록 사이드 패널 숨겨짐")
    }
    
    /**
     * 게임이 시작될 때 호출됩니다.
     */
    private fun startNewGame() {
        isGameActive = true
        attemptCount = 0
        
        // 기록 컨테이너 초기화
        historyContainer.removeAllViews()
        
        // 레벨에 따른 난이도 설정
        difficultySetting()
        
        // 4자리 비밀 코드 생성
        generateSecretCode()
        
        // UI 상태 업데이트
        updateUIState(true)
        
        // 피드백 초기화
        feedbackText.setText("숫자를 입력하세요. (${maxAttempts}번의 기회가 있습니다)")
        updateAttemptsText()
        
        // 직전 결과 텍스트 초기화 및 숨기기
        lastResultTextView.text = ""
        lastResultTextView.visibility = View.GONE
        
        // 레벨에 따른 난이도 설정
        updateDifficultyByLevel()
        
        // 게임 카운터 초기화
        for (digit in codeDigits) {
            digit.setText("_")
        }
        
        // 마지막 결과 있으면 불러오기
        loadLastGameResult()
        
        // 게임 시작 로그
        android.util.Log.d("HackingAlba", "게임 시작: 비밀번호 ${secretCode.joinToString("")}")
    }
    
    /**
     * 게임 성공 시 호출됩니다.
     */
    private fun gameSuccess() {
        isGameActive = false
        
        // UI 상태 업데이트
        updateUIState(false)
        
        // 피드백 업데이트
        feedbackText.setText("해킹 성공! 보상: ${formatCurrency(reward)}원")
        
        // 보상 지급
        assetViewModel.increaseAsset(reward)
        
        // 게임 결과 저장
        saveGameResult(true)
        
        // 레벨업 확인
        checkLevelUp()
    }
    
    /**
     * 게임 실패 시 호출됩니다.
     */
    private fun gameFailed() {
        isGameActive = false
        
        // UI 상태 업데이트
        updateUIState(false)
        
        // 피드백 업데이트
        feedbackText.setText("해킹 실패! 올바른 코드는 ${secretCode.joinToString("")}이었습니다.")
        
        // 게임 결과 저장
        saveGameResult(false)
    }
    
    /**
     * 게임 결과를 저장합니다.
     */
    private fun saveGameResult(isSuccess: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        editor.putBoolean("last_game_result_exists", true)
        editor.putBoolean("last_game_success", isSuccess)
        editor.putInt("last_game_attempt_count", attemptCount)
        editor.putString("last_game_secret_code", secretCode.joinToString(""))
        editor.putLong("last_game_reward", reward)
        editor.putInt("last_game_level", currentLevel)
        
        editor.apply()
    }
    
    /**
     * 마지막 게임 결과를 불러옵니다.
     */
    private fun loadLastGameResult() {
        val sharedPreferences = requireActivity().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        val resultExists = sharedPreferences.getBoolean("last_game_result_exists", false)
        
        if (resultExists) {
            val isSuccess = sharedPreferences.getBoolean("last_game_success", false)
            val lastAttemptCount = sharedPreferences.getInt("last_game_attempt_count", 0)
            val lastSecretCode = sharedPreferences.getString("last_game_secret_code", "")
            val lastReward = sharedPreferences.getLong("last_game_reward", 0)
            val lastLevel = sharedPreferences.getInt("last_game_level", 1)
            
            // 마지막 게임 결과 표시
            val resultMessage = if (isSuccess) {
                "지난 게임: Lv.${lastLevel} 성공 (${lastAttemptCount}번 시도, +${formatCurrency(lastReward)}원)"
            } else {
                "지난 게임: Lv.${lastLevel} 실패 (비밀번호: ${lastSecretCode})"
            }
            
            lastResultTextView.text = resultMessage
            lastResultTextView.visibility = View.VISIBLE
            
            // 성공/실패에 따른 텍스트 색상 설정
            if (isSuccess) {
                lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            } else {
                lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            }
            
            // 결과 데이터 초기화 (한 번만 표시)
            val editor = sharedPreferences.edit()
            editor.putBoolean("last_game_result_exists", false)
            editor.apply()
        }
    }
    
    /**
     * 숫자를 포맷팅합니다.
     */
    private fun formatCurrency(amount: Long): String {
        return android.icu.text.NumberFormat.getInstance().format(amount)
    }

    /**
     * UI 상태를 업데이트합니다.
     * @param isPlaying 게임 진행 중 여부
     */
    private fun updateUIState(isPlaying: Boolean) {
        // 게임 진행 상태에 따라 버튼 활성화/비활성화 설정
        startButton.isEnabled = !isPlaying
        submitButton.isEnabled = isPlaying
        
        // 버튼 색상 변경
        val startButtonColor = if (!isPlaying) 
            ContextCompat.getColor(requireContext(), R.color.alba_start_button)
        else 
            ContextCompat.getColor(requireContext(), R.color.button_disabled)
            
        val submitButtonColor = if (isPlaying) 
            ContextCompat.getColor(requireContext(), R.color.alba_start_button)
        else 
            ContextCompat.getColor(requireContext(), R.color.button_disabled)
        
        // 버튼 배경색 변경 (API 레벨에 따라 다른 방식 사용)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startButton.backgroundTintList = ColorStateList.valueOf(startButtonColor)
            submitButton.backgroundTintList = ColorStateList.valueOf(submitButtonColor)
        }
        
        // 키패드 버튼 활성화/비활성화
        for (button in digitButtons) {
            button.isEnabled = isPlaying
        }
        
        // 로그 기록
        android.util.Log.d("HackingAlba", "UI 상태 업데이트: 게임 활성화=$isPlaying")
    }

    /**
     * 레벨에 따른 난이도를 설정합니다.
     */
    private fun difficultySetting() {
        // 난이도에 따른 최대 시도 횟수 설정
        maxAttempts = when {
            currentLevel < 5 -> 10
            currentLevel < 10 -> 8
            else -> 6
        }
    }
    
    /**
     * 4자리 비밀 코드를 생성합니다.
     */
    private fun generateSecretCode() {
        // 레벨 10 이상에서는 숫자 중복을 허용
        if (currentLevel >= 10) {
            // 중복 허용하는 코드 생성
            for (i in secretCode.indices) {
                secretCode[i] = (0..9).random()
            }
        } else {
            // 중복 없는 코드 생성
            val numbers = (0..9).toMutableList()
            numbers.shuffle()
            for (i in secretCode.indices) {
                secretCode[i] = numbers[i]
            }
        }
    }
    
    /**
     * 레벨에 따른 난이도를 업데이트합니다.
     */
    private fun updateDifficultyByLevel() {
        // 레벨에 따른 난이도 표시 갱신
        levelText.setText("Lv.${currentLevel} 해킹 알바")
        
        // 레벨에 따른 보상 계산
        val baseReward = 10000L + (currentLevel - 1) * 5000L
        reward = baseReward
        
        // 게임 피드백 메시지 업데이트
        feedbackText.setText("4자리 비밀번호를 추측하세요.\n성공 시 최대 ${formatCurrency(baseReward * 2)}원 획득 가능")
    }
    
    /**
     * 현재 시도 횟수 텍스트를 업데이트합니다.
     */
    private fun updateAttemptsText() {
        attemptsText.setText("시도: $attemptCount/$maxAttempts")
    }
    
    /**
     * 레벨업 확인 및 처리를 수행합니다.
     */
    private fun checkLevelUp() {
        // 레벨 증가
        currentLevel++
        
        // 레벨 텍스트 업데이트
        levelText.setText("Lv.${currentLevel} 해킹 알바")
        
        // 레벨업 메시지 표시
        feedbackText.setText("레벨업! Lv.${currentLevel} 달성")
    }

    /**
     * 효과음을 재생합니다.
     */
    private fun playSound(soundId: Int) {
        try {
            // 효과음이 로드되었는지 확인
            if (soundId > 0) {
                // 효과음 재생 (좌우 볼륨은 0.5로 설정, 우선순위는 1, 반복 횟수 0, 속도 1.0)
                soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            android.util.Log.e("HackingAlbaFragment", "효과음 재생 오류: ${e.message}")
        }
    }

    /**
     * 입력한 코드를 확인합니다.
     */
    private fun checkCode() {
        if (!isGameActive) return
        
        // 현재 입력된 코드 가져오기
        val inputCode = IntArray(4) { i ->
            codeDigits[i].text.toString().toIntOrNull() ?: -1
        }
        
        // 모든 자리를 입력했는지 확인
        if (inputCode.contains(-1)) {
            feedbackText.setText("4자리 숫자를 모두 입력하세요.")
            return
        }
        
        // 시도 횟수 증가
        attemptCount++
        updateAttemptsText()
        
        // 정답 확인
        var correctPosition = 0 // 숫자와 위치가 모두 맞는 개수
        var correctDigit = 0 // 숫자는 맞지만 위치가 틀린 개수
        
        // 입력 코드와 비밀 코드의 중복 체크를 위한 배열
        val secretChecked = BooleanArray(4) { false }
        val inputChecked = BooleanArray(4) { false }
        
        // 숫자와 위치가 모두 맞는 경우 체크
        for (i in secretCode.indices) {
            if (inputCode[i] == secretCode[i]) {
                correctPosition++
                secretChecked[i] = true
                inputChecked[i] = true
            }
        }
        
        // 숫자만 맞는 경우 체크
        for (i in secretCode.indices) {
            if (!inputChecked[i]) {
                for (j in secretCode.indices) {
                    if (!secretChecked[j] && inputCode[i] == secretCode[j]) {
                        correctDigit++
                        secretChecked[j] = true
                        break
                    }
                }
            }
        }
        
        // 직전 결과 텍스트뷰에 표시
        val lastResultString = "입력: ${inputCode.joinToString("")} → ${correctPosition}S ${correctDigit}B"
        lastResultTextView.text = lastResultString
        lastResultTextView.visibility = View.VISIBLE
        
        // 색상 설정 - 정답일 경우 초록색, 아닐 경우 기본 흰색
        if (correctPosition == 4) {
            lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
        } else {
            lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
        
        // 결과 피드백 생성
        val resultView = createResultView(inputCode, correctPosition, correctDigit)
        historyContainer.addView(resultView, 0) // 최신 결과를 상단에 추가
        
        // 기록이 표시되지 않은 상태면 자동으로 사이드 패널 표시
        if (!isHistoryShown) {
            // 약간의 지연 후 기록 패널 표시
            handler.postDelayed({
                showHistoryDialog()
                
                // 정답이거나 게임 종료 시에만 자동 닫기 스케줄링
                if (correctPosition == 4 || attemptCount >= maxAttempts) {
                    // 3초 후에 자동으로 닫기
                    handler.postDelayed({
                        if (isHistoryShown) {
                            hideHistoryDialog()
                        }
                    }, 3000)
                }
            }, 200)
        } else {
            // 이미 표시되어 있는 경우 스크롤만 조정
            historyScrollView.fullScroll(ScrollView.FOCUS_UP)
        }
        
        // 정답인 경우
        if (correctPosition == 4) {
            // 보상 조정 - 시도 횟수가 적을수록 추가 보상
            val efficiencyBonus = (maxAttempts - attemptCount + 1).toFloat() / maxAttempts
            reward = (reward * (1 + efficiencyBonus)).toLong()
            
            gameSuccess()
            return
        }
        
        // 최대 시도 횟수 초과
        if (attemptCount >= maxAttempts) {
            gameFailed()
            return
        }
        
        // 계속 진행
        resetInputDigits()
        feedbackText.setText("힌트: $correctPosition 개 숫자와 위치 일치, $correctDigit 개 숫자만 일치")
    }
    
    /**
     * 결과 표시를 위한 뷰를 생성합니다.
     */
    private fun createResultView(inputCode: IntArray, correctPosition: Int, correctDigit: Int): View {
        val resultView = layoutInflater.inflate(R.layout.item_hacking_result, historyContainer, false)
        
        // 입력 코드 표시
        val codeText = resultView.findViewById<TextView>(R.id.codeTextView)
        codeText.setText(inputCode.joinToString(""))
        
        // 결과 표시
        val resultText = resultView.findViewById<TextView>(R.id.resultTextView)
        resultText.setText("${correctPosition}S ${correctDigit}B")
        
        return resultView
    }
    
    /**
     * 입력 숫자를 리셋합니다.
     */
    private fun resetInputDigits() {
        for (digit in codeDigits) {
            digit.setText("_")
        }
    }
    
    /**
     * 숫자를 입력합니다.
     */
    private fun inputDigit(value: Int) {
        // 빈 자리 찾기
        for (i in codeDigits.indices) {
            if (codeDigits[i].text == "_" || codeDigits[i].text.toString() == "") {
                codeDigits[i].setText(value.toString())
                
                // 모든 자리가 입력되었는지 확인
                var allFilled = true
                for (digit in codeDigits) {
                    if (digit.text == "_" || digit.text.toString() == "") {
                        allFilled = false
                        break
                    }
                }
                
                // 모든 자리가 채워지면 로그 출력
                if (allFilled) {
                    android.util.Log.d("HackingAlba", "모든 자리 입력 완료")
                }
                
                return
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null)
        
        // SoundPool 리소스 해제
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}
