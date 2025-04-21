package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.GridLayout
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentLottoBinding
import android.graphics.Color
import kotlin.random.Random
import kotlin.math.roundToInt

class LottoFragment : BaseFragment() {

    private var _binding: FragmentLottoBinding? = null
    private val binding get() = _binding!!

    // SoundManager 인스턴스
    private lateinit var soundManager: SoundManager

    private val lottoPrice = 10000L
    private var currentPrize = 0L
    private var isLottoPurchased = false

    // --- 쿨타임 관련 변수 ---
    private var isBuyButtonEnabled = true
    private val buyCooldownHandler = Handler(Looper.getMainLooper())
    private val cooldownDuration = 10000L // 10초
    private var remainingCooldownTime = 0L // 남은 쿨타임 시간 (밀리초)
    private val updateInterval = 1000L // 1초마다 업데이트
    
    // 쿨타임 카운트다운 Runnable
    private val cooldownCountdownRunnable = object : Runnable {
        override fun run() {
            if (remainingCooldownTime > 0) {
                remainingCooldownTime -= updateInterval
                updateCooldownDisplay()
                buyCooldownHandler.postDelayed(this, updateInterval)
            } else {
                enableBuyButton()
            }
        }
    }

    private var isScratchable = false
    
    // 로또 번호를 저장할 배열
    private val selectedNumbers = mutableSetOf<Int>()
    private val winningNumbers = mutableSetOf<Int>()
    private val numberButtons = arrayOfNulls<Button>(45)
    
    // 로또 당첨금 등급별 배당률
    private val lottoRewards = mapOf(
        6 to 2000000000L, // 1등: 20억
        5 to 50000000L,   // 2등: 5천만원
        4 to 5000000L,    // 3등: 500만원
        3 to 50000L,      // 4등: 5만원
        2 to 5000L,       // 5등: 5천원
        1 to 0L,          // 꽝
        0 to 0L           // 꽝
    )
    
    // 소리 리소스 ID
    companion object {
        private val SOUND_BUY = R.raw.lotto_button_buy
        private val SOUND_OPEN = R.raw.lotto_button_open
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLottoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SoundManager 초기화
        soundManager = SoundManager.getInstance(requireContext())

        binding.lottoPriceText.text = "티켓 가격: ${formatCurrency(lottoPrice)}"

        // 구매 버튼 초기 상태 설정
        binding.buyLottoButton.isEnabled = isBuyButtonEnabled
        updateCooldownDisplay() // 쿨다운 텍스트 초기화

        binding.buyLottoButton.setOnClickListener {
            // 쿨타임 중이면 클릭 무시
            if (!isBuyButtonEnabled) {
                showErrorMessage("잠시 후 다시 시도해주세요. 남은 시간: ${remainingCooldownTime / 1000}초")
                return@setOnClickListener
            }
            // 구매 효과음 재생
            playBuySound()
            buyLottoTicket()
        }

        binding.scratchCoatingImage.setOnTouchListener { v, event ->
            if (event.action == ACTION_DOWN && isLottoPurchased) {
                // 스크래치 효과음 재생
                playOpenSound()
                revealPrize()
                true
            } else {
                false
            }
        }

        // 스크래치 영역 초기 상태 설정
        binding.scratchCoatingImage.visibility = View.VISIBLE

        // 초기 애니메이션 적용
        applyInitialAnimations(view)
    }

    private fun applyInitialAnimations(view: View) {
        // 제목 카드 애니메이션
        val titleCard = binding.lottoTitleCard
        titleCard.alpha = 0f
        titleCard.translationY = -50f
        titleCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator())
            .start()
            
        // 구매 버튼 펄스 애니메이션
        val pulseAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        pulseAnim.duration = 1000
        pulseAnim.repeatCount = 3
        pulseAnim.repeatMode = android.view.animation.Animation.REVERSE
        binding.buyLottoButton.startAnimation(pulseAnim)
        
        // 스크래치 영역 애니메이션
        binding.scratchAreaCard.alpha = 0.7f
        binding.scratchAreaCard.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(500)
            .start()
    }

    private fun buyLottoTicket() {
        if (isLottoPurchased) {
            showErrorMessage("이미 로또를 구매했습니다. 긁어주세요!")
            return
        }

        val currentAsset = assetViewModel.asset.value ?: 0L
        if (currentAsset >= lottoPrice) {
            // 구매 버튼 애니메이션
            animatePurchaseButton()
            
            assetViewModel.decreaseAsset(lottoPrice)
            isLottoPurchased = true

            val randomValue = Random.nextInt(100)
            currentPrize = when (randomValue) {
                in 0..69 -> 0L
                in 70..94 -> Random.nextLong(100, 50001)
                in 95..98 -> Random.nextLong(50001, 500001)
                else -> 3_000_000L
            }

            binding.prizeText.text = if (currentPrize > 0) formatCurrency(currentPrize) else "꽝"
            binding.prizeText.visibility = View.GONE
            binding.scratchCoatingImage.visibility = View.VISIBLE
            binding.resultMessageText.visibility = View.INVISIBLE
            
            // 스크래치 영역 활성화 애니메이션
            animateScratchArea()
            
            showMessage("로또 구매 완료! 긁어서 확인하세요.")

            // 구매 버튼 비활성화 및 쿨타임 시작
            disableBuyButtonTemporarily()
        } else {
            showErrorMessage("자산이 부족합니다.")
        }
    }
    
    private fun animatePurchaseButton() {
        val scaleX = ObjectAnimator.ofFloat(binding.buyLottoButton, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.buyLottoButton, "scaleY", 1f, 1.2f, 1f)
        val rotate = ObjectAnimator.ofFloat(binding.buyLottoButton, "rotation", 0f, 5f, -5f, 0f)
        
        val animSet = AnimatorSet()
        animSet.playTogether(scaleX, scaleY, rotate)
        animSet.duration = 500
        animSet.interpolator = AccelerateDecelerateInterpolator()
        animSet.start()
    }
    
    private fun animateScratchArea() {
        // 스크래치 코팅 반짝임 효과
        val coatingFadeIn = ObjectAnimator.ofFloat(binding.scratchCoatingImage, "alpha", 0.5f, 1f)
        coatingFadeIn.duration = 300
        
        val scaleX = ObjectAnimator.ofFloat(binding.scratchAreaCard, "scaleX", 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.scratchAreaCard, "scaleY", 1f, 1.05f, 1f)
        
        val animSet = AnimatorSet()
        animSet.playTogether(coatingFadeIn, scaleX, scaleY)
        animSet.duration = 800
        animSet.start()
        
        // 스크래치 지시 텍스트 강조
        binding.scratchInstructionText.visibility = View.VISIBLE
        binding.scratchInstructionText.alpha = 0f
        binding.scratchInstructionText.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()
    }

    private fun revealPrize() {
        // 스크래치 효과 애니메이션
        val fadeOut = ObjectAnimator.ofFloat(binding.scratchCoatingImage, "alpha", 1f, 0f)
        fadeOut.duration = 800
        
        // 지시 텍스트 페이드 아웃
        binding.scratchInstructionText.animate()
            .alpha(0f)
            .setDuration(300)
            .start()
        
        // 복권 결과 애니메이션
        binding.prizeText.alpha = 0f
        binding.prizeText.scaleX = 0.8f
        binding.prizeText.scaleY = 0.8f
        
        fadeOut.start()
        
        binding.scratchCoatingImage.visibility = View.INVISIBLE
        binding.prizeText.visibility = View.VISIBLE
        
        // 결과 텍스트 애니메이션
        binding.prizeText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(1.5f))
            .withEndAction {
                revealResultMessage()
            }
            .start()
    }
    
    private fun revealResultMessage() {
        binding.resultMessageText.visibility = View.VISIBLE
        binding.resultMessageText.alpha = 0f
        binding.resultMessageText.translationY = 50f

        if (currentPrize > 0) {
            binding.resultMessageText.text = "축하합니다! ${formatCurrency(currentPrize)} 당첨!"
            binding.resultMessageText.setTextColor(Color.parseColor("#FFD700")) // 골드 색상
            
            // 당첨 효과 애니메이션
            val celebrateAnim = AnimatorSet()
            val scaleX = ObjectAnimator.ofFloat(binding.resultMessageText, "scaleX", 0.8f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.resultMessageText, "scaleY", 0.8f, 1.2f, 1f)
            val rotate = ObjectAnimator.ofFloat(binding.resultMessageText, "rotation", -5f, 5f, 0f)
            
            celebrateAnim.playTogether(scaleX, scaleY, rotate)
            celebrateAnim.duration = 1000
            celebrateAnim.start()
            
            assetViewModel.increaseAsset(currentPrize)
            showSuccessMessage("${formatCurrency(currentPrize)}원에 당첨되었습니다!")
        } else {
            binding.resultMessageText.text = "아쉽지만, 꽝입니다."
            binding.resultMessageText.setTextColor(Color.parseColor("#3498DB")) // 밝은 파란색
            
            // 꽝인 경우 50% 확률로 시간증폭 아이템 재고 +1 증가
            if (Random.nextInt(100) < 50) {
                // 아이템 재고 증가
                val itemReward = ItemUtil.increaseLottoTimeItemStock(requireContext())
                
                // 아이템 획득 메시지 표시
                itemReward?.let {
                    showSuccessMessage("꽝이지만 ${it.itemName} 아이템 재고가 +1 증가했습니다!")
                }
            } else {
                showMessage("아쉽지만 꽝입니다. 다음 기회에...")
            }
        }
        
        binding.resultMessageText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .start()
            
        isLottoPurchased = false
    }

    // 구매 버튼 임시 비활성화 함수
    private fun disableBuyButtonTemporarily() {
        isBuyButtonEnabled = false
        binding.buyLottoButton.isEnabled = false
        
        // 쿨타임 시작
        remainingCooldownTime = cooldownDuration
        updateCooldownDisplay()
        
        // 이전 콜백 제거 후 새로운 카운트다운 시작
        buyCooldownHandler.removeCallbacks(cooldownCountdownRunnable)
        buyCooldownHandler.post(cooldownCountdownRunnable)
    }

    // 쿨다운 표시 업데이트
    private fun updateCooldownDisplay() {
        if (remainingCooldownTime > 0) {
            val seconds = (remainingCooldownTime / 1000).toInt()
            binding.cooldownText.text = "대기 중: ${seconds}초"
            binding.cooldownText.setTextColor(Color.WHITE)
            
            // 쿨다운 시 배경색 빨간색으로 변경
            binding.cooldownCardView.setCardBackgroundColor(Color.parseColor("#E74C3C"))
        } else {
            binding.cooldownText.text = "구매 가능"
            binding.cooldownText.setTextColor(Color.WHITE)
            
            // 구매 가능 시 배경색 녹색으로 변경
            binding.cooldownCardView.setCardBackgroundColor(Color.parseColor("#27AE60"))
        }
    }
    
    // 구매 버튼 활성화 함수
    private fun enableBuyButton() {
        isBuyButtonEnabled = true
        binding.buyLottoButton.isEnabled = true
        remainingCooldownTime = 0
        updateCooldownDisplay()
    }

    /**
     * 효과음을 재생합니다.
     */
    private fun playBuySound() {
        soundManager.playSound(SOUND_BUY)
    }
    
    /**
     * 당첨 확인 효과음을 재생합니다.
     */
    private fun playOpenSound() {
        soundManager.playSound(SOUND_OPEN)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 핸들러 콜백 제거
        buyCooldownHandler.removeCallbacks(cooldownCountdownRunnable)
        _binding = null
    }
}