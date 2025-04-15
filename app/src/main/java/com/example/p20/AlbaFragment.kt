package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Build
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

        // 타이밍 알바와 원 알바 제거로 인해 탭 레이아웃이 필요 없어졌으므로 숨김 처리
        tabLayout.visibility = View.GONE
    }

    // 뷰페이저 어댑터 - 타이밍 알바와 원 알바 탭 제거
    private inner class AlbaViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        
        override fun getItemCount(): Int = 1 // 클릭 알바만 남김
        
        override fun createFragment(position: Int): Fragment {
            return ClickAlbaFragment()
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
    
    // 효과음 재생을 위한 MediaPlayer
    private var coinSoundPlayer: MediaPlayer? = null
    
    // 동시 재생 가능한 효과음 목록
    private val soundPlayers = mutableListOf<MediaPlayer>()
    
    // 최대 동시 재생 효과음 수
    private val MAX_CONCURRENT_SOUNDS = 4

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_click_alba, container, false)

        albaViewModel = ViewModelProvider(requireActivity())[AlbaViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

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
                if (albaViewModel.isCooldown.value == false && albaViewModel.isActivePhase.value == false) {
                    // 효과음 재생
                    playCoinSound()
                    
                    albaViewModel.startActivePhase()
                    val rewardAmount = albaViewModel.getRewardAmount().toLong()
                    assetViewModel.increaseAsset(rewardAmount)
                    val location = IntArray(2)
                    albaImage.getLocationOnScreen(location)
                    showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
                } else if (albaViewModel.isActivePhase.value == true) {
                    // 효과음 재생
                    playCoinSound()
                    
                    albaViewModel.increaseAlbaLevel()
                    val rewardAmount = albaViewModel.getRewardAmount().toLong()
                    assetViewModel.increaseAsset(rewardAmount)
                    val location = IntArray(2)
                    albaImage.getLocationOnScreen(location)
                    showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
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
     * 코인 효과음을 재생합니다.
     * 빠른 터치에도 소리가 중첩되어 들리도록 여러 MediaPlayer 인스턴스를 사용합니다.
     */
    private fun playCoinSound() {
        try {
            // 동시 재생 가능한 효과음 수 제한
            if (soundPlayers.size >= MAX_CONCURRENT_SOUNDS) {
                // 가장 오래된 효과음 해제
                val oldestPlayer = soundPlayers.removeAt(0)
                oldestPlayer.release()
            }
            
            // 새 효과음 플레이어 생성
            val newPlayer = MediaPlayer.create(requireContext(), R.raw.coin)
            
            // 볼륨 설정 (0.0 ~ 1.0)
            newPlayer.setVolume(0.5f, 0.5f)
            
            // 목록에 추가
            soundPlayers.add(newPlayer)
            
            // 효과음 재생
            newPlayer.start()
            
            // 재생이 끝난 후 리소스 해제
            newPlayer.setOnCompletionListener {
                soundPlayers.remove(it)
                it.release()
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
        
        // 모든 MediaPlayer 리소스 해제
        soundPlayers.forEach { it.release() }
        soundPlayers.clear()
        
        // 기존 coinSoundPlayer도 해제 (호환성 유지)
        coinSoundPlayer?.release()
        coinSoundPlayer = null
    }
}
