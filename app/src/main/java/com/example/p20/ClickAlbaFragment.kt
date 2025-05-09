package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

/**
 * 클릭알바 프래그먼트 클래스
 * 이미지를 클릭하여 돈을 버는 간단한 게임 제공
 */
class ClickAlbaFragment : BaseFragment() {

    // 현금 터치 효과음 리소스 ID 정의
    companion object {
        private val SOUND_COIN_CLICK = R.raw.alba_click_coin
        private const val TAG = "ClickAlbaFragment"
    }

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var earnText: TextView
    private lateinit var levelText: TextView
    private lateinit var cooldownText: TextView
    private lateinit var animationContainer: FrameLayout
    private lateinit var albaImage: ImageView
    private lateinit var activePhaseText: TextView
    private lateinit var expProgressBar: ProgressBar
    private lateinit var expTextView: TextView
    private lateinit var rewardAmountText: TextView
    
    // 경험치 애니메이션 관련 변수
    private var isExpAnimating = false
    private var lastShownExp = 0
    private val expUpdateQueue = mutableListOf<Int>()
    
    // 날아가는 텍스트 애니메이션 추적용 카운터
    private var pendingAnimationCount = 0
    
    // 디바운싱을 위한 변수
    private var lastClickTime = 0L
    private val MIN_CLICK_INTERVAL = 80L // 밀리초 (0.08초)
    
    // 애니메이션 리소스 관리를 위한 변수
    private val animatingViews = mutableListOf<View>()

    // 상태 저장 변수
    private var savedImageVisibility: Int = View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 프래그먼트 상태 유지 활성화
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_click_alba, container, false)
        
        // 저장된 상태 복원
        savedInstanceState?.let {
            savedImageVisibility = it.getInt("image_visibility", View.VISIBLE)
        }

        albaViewModel = ViewModelProvider(requireActivity())[AlbaViewModel::class.java]
        
        initializeViews(view)
        
        // 효과음 미리 로드
        preloadSounds()
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAlbaImage()
        setupViewModelObservers()
    }
    
    private fun initializeViews(view: View) {
        albaImage = view.findViewById<ImageView>(R.id.albaImage).apply {
            visibility = savedImageVisibility
        }
        earnText = view.findViewById(R.id.earnText)
        levelText = view.findViewById(R.id.levelText)
        cooldownText = view.findViewById(R.id.cooldownText)
        animationContainer = view.findViewById(R.id.animationContainer)
        activePhaseText = view.findViewById(R.id.cooldownText)
        rewardAmountText = view.findViewById(R.id.rewardAmountText)
        
        expProgressBar = view.findViewById(R.id.expProgressBar)
        expTextView = view.findViewById(R.id.expTextView)
        
        lastShownExp = albaViewModel.getClickCounter().coerceIn(0, 20)
        updateExpBar(lastShownExp)
        
        earnText.text = "알바 시작!"
    }
    
    private fun setupAlbaImage() {
        if (::albaImage.isInitialized) {
            android.util.Log.d("ClickAlbaFragment", "이미지 설정 시작")
            try {
                albaImage.setImageResource(R.drawable.alba1)
                albaImage.visibility = savedImageVisibility
                setupClickListener()
                android.util.Log.d("ClickAlbaFragment", "이미지 설정 완료: visibility=${albaImage.visibility}")
            } catch (e: Exception) {
                android.util.Log.e("ClickAlbaFragment", "이미지 설정 실패: ${e.message}")
            }
        } else {
            android.util.Log.e("ClickAlbaFragment", "이미지 뷰가 초기화되지 않음")
        }
    }
    
    private fun setupClickListener() {
        albaImage.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleImageClick(event)
            }
            true
        }
    }
    
    private fun handleImageClick(event: MotionEvent) {
        val currentTime = System.currentTimeMillis()
        
        // 활성 시간이 끝났는지 확인
        val activeTimeDone = albaViewModel.isActivePhase.value == true && 
                           (albaViewModel.activePhaseTime.value ?: 0) <= 0
        
        // 쿨다운 상태가 아니고 활성 시간이 끝나지 않았을 때만 효과음 재생
        val isCooldown = albaViewModel.isCooldown.value ?: false
        if (!isCooldown && !activeTimeDone) {
            playCoinSound()
        }
        
        if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
            processClick(event, currentTime)
        }
    }
    
    private fun processClick(event: MotionEvent, currentTime: Long) {
        lastClickTime = currentTime
        
        if (albaViewModel.isCooldown.value == false && 
            albaViewModel.isActivePhase.value == false) {
            handleInitialClick(event)
        } else if (albaViewModel.isActivePhase.value == true && 
                  (albaViewModel.activePhaseTime.value ?: 0) > 0) {
            handleActivePhaseClick(event)
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::albaImage.isInitialized) {
            outState.putInt("image_visibility", albaImage.visibility)
            savedImageVisibility = albaImage.visibility
            android.util.Log.d("ClickAlbaFragment", "상태 저장: visibility=${albaImage.visibility}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::albaImage.isInitialized) {
            if (albaImage.drawable == null || albaImage.visibility != savedImageVisibility) {
                android.util.Log.d("ClickAlbaFragment", "이미지 복원 시도")
                setupAlbaImage()
            }
        }
    }
    
    override fun onDestroyView() {
        if (::albaImage.isInitialized) {
            savedImageVisibility = albaImage.visibility
            android.util.Log.d("ClickAlbaFragment", "뷰 소멸 시 상태 저장: visibility=${albaImage.visibility}")
        }
        clearAnimations()
        super.onDestroyView()
    }
    
    /**
     * 경험치 바를 업데이트합니다.
     * @param exp 현재 경험치 값 (0-20)
     */
    private fun updateExpBar(exp: Int) {
        val safeExp = exp.coerceIn(0, 20)
        expProgressBar.progress = safeExp
        expTextView.text = "$safeExp/20"
        
        // 마지막으로 표시된 값 업데이트
        lastShownExp = safeExp
    }
    
    /**
     * 경험치 리셋 애니메이션을 표시합니다.
     * 레벨업 시 경험치가 0으로 초기화될 때 호출됩니다.
     */
    private fun animateExpReset() {
        if (isExpAnimating) {
            // 이미 애니메이션 중이면 큐에 추가
            expUpdateQueue.add(0)
            return
        }
        
        isExpAnimating = true
        
        // 프로그레스바를 점진적으로 0으로 설정
        val startValue = lastShownExp
        val anim = ObjectAnimator.ofInt(expProgressBar, "progress", startValue, 0).apply {
            duration = 500 // 0.5초
            
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                expTextView.text = "$animatedValue/20"
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션 종료 후 상태 업데이트
                    isExpAnimating = false
                    lastShownExp = 0
                    
                    // 큐에 대기 중인 다음 애니메이션이 있으면 실행
                    if (expUpdateQueue.isNotEmpty()) {
                        postDelayed(100) {
                            val nextExp = expUpdateQueue.removeAt(0)
                            updateExpBar(nextExp)
                        }
                    }
                }
            })
        }
        
        // 애니메이션 추적 및 실행
        trackAnimator(anim).start()
    }
    
    /**
     * 필요한 효과음을 미리 로드합니다.
     */
    private fun preloadSounds() {
        try {
            // 코인 클릭 효과음 미리 로드
            soundController.getSoundManager().loadSound(SOUND_COIN_CLICK)
            android.util.Log.d(TAG, "코인 클릭 효과음 미리 로드 완료")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "효과음 미리 로드 오류: ${e.message}")
        }
    }
    
    /**
     * BaseFragment의 onReloadSounds 메서드를 오버라이드하여
     * 화면이 다시 보여질 때 효과음을 다시 로드합니다.
     */
    override fun onReloadSounds() {
        android.util.Log.d(TAG, "onReloadSounds 호출됨: 클릭알바 효과음 재로드")
        preloadSounds()
    }
    
    /**
     * 코인 효과음을 재생합니다.
     * SoundManager를 사용하여 효율적으로 효과음을 재생합니다.
     */
    private fun playCoinSound() {
        try {
            // 로그 추가 - 디버깅 목적
            android.util.Log.d(TAG, "playCoinSound 호출됨")
            
            // 활성 시간이 끝났거나 쿨다운 중이면 소리 재생하지 않음
            if ((albaViewModel.isActivePhase.value == true && (albaViewModel.activePhaseTime.value ?: 0) <= 0) ||
                albaViewModel.isCooldown.value == true) {
                android.util.Log.d(TAG, "효과음 재생 중단: 활성 시간 종료 또는 쿨다운 중")
                return
            }
            
            // SoundController를 통해 직접 효과음 설정 확인 및 재생
            if (soundController.isSoundEffectEnabled()) {
                // 코인 효과음 재생
                val success = soundController.playSoundEffect(SOUND_COIN_CLICK)
                android.util.Log.d(TAG, "코인 효과음 재생 요청 결과: $success")
                
                if (!success) {
                    // 실패 시 백업 메서드로 재시도
                    android.util.Log.d(TAG, "코인 효과음 재생 실패, 백업 메서드로 재시도")
                    soundController.getSoundManager().playSound(SOUND_COIN_CLICK)
                }
            } else {
                android.util.Log.d(TAG, "효과음 설정이 꺼져 있음")
            }
        } catch (e: Exception) {
            // 효과음 재생 중 오류 발생 시 로그 출력 후 계속 진행
            android.util.Log.e(TAG, "효과음 재생 오류: ${e.message}")
        }
    }
    
    /**
     * 보상 애니메이션을 표시합니다.
     * @param x 클릭한 X 좌표
     * @param y 클릭한 Y 좌표
     * @param amount 획득한 금액
     */
    private fun showRewardAnimation(x: Int, y: Int, amount: Long) {
        // 메인 스레드에서 실행
        val rewardText = TextView(requireContext())
        rewardText.text = "+${amount}원"
        rewardText.textSize = 16f
        rewardText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
        
        // 레이아웃 파라미터 설정
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = x - 50 // 텍스트가 손가락에 가려지지 않도록 약간 옆으로 이동
        params.topMargin = y - 50
        rewardText.layoutParams = params
        
        // 큐에 애니메이션 추가
        pendingAnimationCount++
        
        // 애니메이션 컨테이너에 추가
        animationContainer.addView(rewardText)
        
        // 뷰 추적 목록에 추가
        animatingViews.add(rewardText)
        
        // 상승 애니메이션
        rewardText.animate()
            .translationY(-150f) // 위로 150dp 이동
            .alpha(0f) // 투명해짐
            .setDuration(1000) // 1초 동안
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션 끝나면 뷰 제거
                    if (view != null && isAdded) {
                        animationContainer.removeView(rewardText)
                    }
                    
                    // 애니메이션 목록에서 제거
                    animatingViews.remove(rewardText)
                    
                    // 카운터 감소
                    pendingAnimationCount--
                }
            })
            .start()
    }
    
    /**
     * 아이템 획득 애니메이션을 표시합니다.
     */
    private fun showItemRewardAnimation() {
        // 간단한 시각적 피드백 - 전체 화면 플래시 효과
        val flashView = View(requireContext())
        flashView.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
        flashView.alpha = 0.5f
        
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        flashView.layoutParams = params
        
        // 애니메이션 컨테이너에 추가
        animationContainer.addView(flashView)
        
        // 뷰 추적 목록에 추가
        animatingViews.add(flashView)
        
        // 깜빡임 애니메이션
        flashView.animate()
            .alpha(0f)
            .setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (view != null && isAdded) {
                        animationContainer.removeView(flashView)
                    }
                    
                    // 애니메이션 목록에서 제거
                    animatingViews.remove(flashView)
                }
            })
            .start()
    }
    
    /**
     * 상태 텍스트를 업데이트합니다.
     * 쿨다운 상태 또는 활성 상태에 따라 텍스트와 색상이 변경됩니다.
     */
    private fun updateStatusText() {
        when {
            albaViewModel.isCooldown.value == true -> {
                val timeLeft = albaViewModel.cooldownTime.value ?: 0
                cooldownText.text = "쿨타임: ${timeLeft}초"
                cooldownText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
                earnText.text = "알바 쿨타임 중..."
            }
            albaViewModel.isActivePhase.value == true -> {
                val timeLeft = albaViewModel.activePhaseTime.value ?: 0
                if (timeLeft > 0) {
                    cooldownText.text = "활성 시간: ${timeLeft}초"
                    cooldownText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                    earnText.text = "빨리 클릭하세요!!"
                } else {
                    cooldownText.text = "시간 종료!"
                    cooldownText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
                    earnText.text = "시간이 다 되었습니다!"
                }
            }
            else -> {
                cooldownText.text = "알바 가능!"
                cooldownText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light))
                earnText.text = "터치!! 터치!!"
            }
        }
    }
    
    /**
     * 보상 금액 텍스트를 업데이트합니다.
     */
    private fun updateRewardAmountText() {
        val rewardAmount = albaViewModel.getRewardAmount()
        rewardAmountText.text = "클릭 보상: ${rewardAmount}원"
    }

    /**
     * 모든 진행 중인 애니메이션을 중지합니다.
     */
    private fun clearAnimations() {
        // 애니메이션 중인 모든 뷰 제거
        val viewsToRemove = ArrayList(animatingViews)
        for (view in viewsToRemove) {
            view.clearAnimation()
            if (::animationContainer.isInitialized && this.view != null && isAdded) {
                animationContainer.removeView(view)
            }
        }
        
        animatingViews.clear()
        
        // 애니메이션 컨테이너의 모든 자식 뷰 제거
        if (::animationContainer.isInitialized && view != null && isAdded) {
            animationContainer.removeAllViews()
        }
        
        pendingAnimationCount = 0
    }

    override fun onGameOver() {
        super.onGameOver()
        // 게임 오버 시 모든 애니메이션 정리
        clearAnimations()
        // 사용자에게 알림
        showMessage("알바가 중단되었습니다.")
    }

    private fun handleInitialClick(event: MotionEvent) {
        albaViewModel.startActivePhase()
        val rewardAmount = albaViewModel.getRewardAmount().toLong()
        assetViewModel.increaseAsset(rewardAmount)
        
        // 즉시 경험치바 업데이트
        updateExpBar(albaViewModel.getClickCounter())
        
        // 보상 애니메이션 표시
        val location = IntArray(2)
        albaImage.getLocationOnScreen(location)
        showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
    }
    
    private fun handleActivePhaseClick(event: MotionEvent) {
        // 활성 시간이 남아있을 때만 경험치 증가
        albaViewModel.increaseAlbaLevel()
        val rewardAmount = albaViewModel.getRewardAmount().toLong()
        assetViewModel.increaseAsset(rewardAmount)
        
        // 즉시 경험치바 업데이트
        updateExpBar(albaViewModel.getClickCounter())
        
        // 보상 애니메이션 표시
        val location = IntArray(2)
        albaImage.getLocationOnScreen(location)
        showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
    }

    // ViewModel 옵저버 설정
    private fun setupViewModelObservers() {
        albaViewModel.isCooldown.observe(viewLifecycleOwner) { isCoolingDown ->
            updateStatusText()
        }
        albaViewModel.cooldownTime.observe(viewLifecycleOwner) { time ->
            updateStatusText()
        }
        albaViewModel.isActivePhase.observe(viewLifecycleOwner) { isActive ->
            updateStatusText()
        }
        albaViewModel.activePhaseTime.observe(viewLifecycleOwner) { time ->
            updateStatusText()
        }
        albaViewModel.albaLevel.observe(viewLifecycleOwner) { level ->
            levelText.text = "레벨: $level"
            updateRewardAmountText()
        }
        
        // 클릭 카운터 리셋 이벤트 관찰
        albaViewModel.clickCounterResetEvent.observe(viewLifecycleOwner) { isReset ->
            if (isReset) {
                // 경험치 바 초기화 애니메이션 처리
                animateExpReset()
                // 이벤트 소비
                albaViewModel.consumeClickCounterResetEvent()
            }
        }
        
        // 아이템 보상 이벤트 관찰
        albaViewModel.itemRewardEvent.observe(viewLifecycleOwner) { reward ->
            reward?.let {
                // 아이템 획득 애니메이션 표시
                showItemRewardAnimation()
                // 이벤트 소비
                albaViewModel.consumeItemRewardEvent()
            }
        }
        
        // 초기 보상 금액 표시 업데이트
        updateRewardAmountText()
    }
} 