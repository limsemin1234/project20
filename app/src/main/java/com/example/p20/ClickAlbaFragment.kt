package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

/**
 * 클릭알바 프래그먼트 클래스
 * 이미지를 클릭하여 돈을 버는 간단한 게임 제공
 */
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
                // 현재 시간 가져오기
                val currentTime = System.currentTimeMillis()
                
                // 활성 시간이 끝났는지 확인
                val activeTimeDone = albaViewModel.isActivePhase.value == true && (albaViewModel.activePhaseTime.value ?: 0) <= 0
                
                // 쿨다운 상태가 아니고 활성 시간이 끝나지 않았을 때만 효과음 재생
                val isCooldown = albaViewModel.isCooldown.value ?: false
                if (!isCooldown && !activeTimeDone) {
                    playCoinSound()
                }
                
                // 게임 로직과 애니메이션은 디바운싱 적용
                if (currentTime - lastClickTime > MIN_CLICK_INTERVAL) {
                    lastClickTime = currentTime
                    
                    if (albaViewModel.isCooldown.value == false && albaViewModel.isActivePhase.value == false) {
                        albaViewModel.startActivePhase()
                        val rewardAmount = albaViewModel.getRewardAmount().toLong()
                        assetViewModel.increaseAsset(rewardAmount)
                        
                        // 즉시 경험치바 업데이트
                        updateExpBar(albaViewModel.getClickCounter())
                        
                        // 보상 애니메이션 표시
                        val location = IntArray(2)
                        albaImage.getLocationOnScreen(location)
                        showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
                    } else if (albaViewModel.isActivePhase.value == true && albaViewModel.activePhaseTime.value ?: 0 > 0) {
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
        })
        albaViewModel.activePhaseTime.observe(viewLifecycleOwner, Observer { time ->
            updateStatusText()
        })
        albaViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            levelText.text = "레벨: $level"
        })
        
        // 클릭 카운터 리셋 이벤트 관찰
        albaViewModel.clickCounterResetEvent.observe(viewLifecycleOwner, Observer { isReset ->
            if (isReset) {
                // 경험치 바 초기화 애니메이션 처리
                animateExpReset()
                // 이벤트 소비
                albaViewModel.consumeClickCounterResetEvent()
            }
        })
        
        // 아이템 보상 이벤트 관찰
        albaViewModel.itemRewardEvent.observe(viewLifecycleOwner, Observer { reward ->
            reward?.let {
                // 아이템 획득 애니메이션 표시
                showItemRewardAnimation()
                // 이벤트 소비
                albaViewModel.consumeItemRewardEvent()
            }
        })

        return view
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
        val anim = ObjectAnimator.ofInt(expProgressBar, "progress", startValue, 0)
        anim.duration = 500 // 0.5초
        
        anim.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            expTextView.text = "$animatedValue/20"
        }
        
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 애니메이션 종료 후 상태 업데이트
                isExpAnimating = false
                lastShownExp = 0
                
                // 큐에 대기 중인 다음 애니메이션이 있으면 실행
                if (expUpdateQueue.isNotEmpty()) {
                    handler.postDelayed({
                        val nextExp = expUpdateQueue.removeAt(0)
                        updateExpBar(nextExp)
                    }, 100)
                }
            }
        })
        
        anim.start()
    }
    
    /**
     * SoundPool을 초기화하고 효과음을 로드합니다.
     */
    private fun initSoundPool() {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            SoundPool(5, AudioManager.STREAM_MUSIC, 0)
        }
        
        // 코인 효과음 로드
        coinSoundId = soundPool.load(requireContext(), R.raw.coin, 1)
        android.util.Log.d("ClickAlba", "코인 효과음 로드됨: $coinSoundId")
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
        
        // 상승 애니메이션
        rewardText.animate()
            .translationY(-150f) // 위로 150dp 이동
            .alpha(0f) // 투명해짐
            .setDuration(1000) // 1초 동안
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 애니메이션 끝나면 뷰 제거
                    animationContainer.removeView(rewardText)
                    
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
        
        // 깜빡임 애니메이션
        flashView.animate()
            .alpha(0f)
            .setDuration(500)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animationContainer.removeView(flashView)
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
     * 코인 효과음을 재생합니다.
     * SoundPool을 사용하여 효율적으로 효과음을 중첩 재생합니다.
     */
    private fun playCoinSound() {
        try {
            // 활성 시간이 끝났으면 소리 재생하지 않음
            if (albaViewModel.isActivePhase.value == true && (albaViewModel.activePhaseTime.value ?: 0) <= 0) {
                return
            }
            
            // 효과음 설정 확인 (MainActivity에서 설정 가져오기)
            val mainActivity = activity as? MainActivity
            if (mainActivity?.isSoundEffectEnabled() != true) {
                return  // 효과음이 비활성화되어 있으면 재생하지 않음
            }
            
            // 효과음이 로드되었는지 확인
            if (coinSoundId > 0) {
                // 현재 볼륨 설정 가져오기 - 호출 시점에 최신 볼륨값 가져오기
                val volume = mainActivity.getCurrentVolume()
                // 효과음 재생 (좌우 볼륨을 현재 설정 볼륨으로 설정)
                soundPool.play(coinSoundId, volume, volume, 1, 0, 1.0f)
                android.util.Log.d("ClickAlbaFragment", "코인 효과음 재생: 볼륨=$volume")
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