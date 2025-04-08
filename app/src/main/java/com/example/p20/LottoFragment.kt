package com.example.p20

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.p20.databinding.FragmentLottoBinding
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.os.Looper
import android.os.Handler

class LottoFragment : Fragment() {

    private var _binding: FragmentLottoBinding? = null
    private val binding get() = _binding!!

    private val assetViewModel: AssetViewModel by activityViewModels()

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

        binding.lottoPriceText.text = "티켓 가격: ${formatCurrency(lottoPrice)}"

        // 구매 버튼 초기 상태 설정
        binding.buyLottoButton.isEnabled = isBuyButtonEnabled
        updateCooldownDisplay() // 쿨다운 텍스트 초기화

        binding.buyLottoButton.setOnClickListener {
            // 쿨타임 중이면 클릭 무시
            if (!isBuyButtonEnabled) {
                showCustomSnackbar("잠시 후 다시 시도해주세요. 남은 시간: ${remainingCooldownTime / 1000}초")
                return@setOnClickListener
            }
            buyLottoTicket()
        }

        binding.scratchCoatingImage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isLottoPurchased) {
                revealPrize()
                true
            } else {
                false
            }
        }

        // 스크래치 영역 초기 상태 설정
        binding.scratchCoatingImage.visibility = View.VISIBLE
    }

    private fun buyLottoTicket() {
        if (isLottoPurchased) {
            showCustomSnackbar("이미 로또를 구매했습니다. 긁어주세요!")
            return
        }

        val currentAsset = assetViewModel.asset.value ?: 0L
        if (currentAsset >= lottoPrice) {
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
            showCustomSnackbar("로또 구매 완료! 긁어서 확인하세요.")

            // 구매 버튼 비활성화 및 쿨타임 시작
            disableBuyButtonTemporarily()
        } else {
            showCustomSnackbar("자산이 부족합니다.")
        }
    }

    private fun revealPrize() {
        binding.scratchCoatingImage.visibility = View.INVISIBLE
        binding.prizeText.visibility = View.VISIBLE
        binding.resultMessageText.visibility = View.VISIBLE

        if (currentPrize > 0) {
            binding.resultMessageText.text = "축하합니다! ${formatCurrency(currentPrize)} 당첨!"
            assetViewModel.increaseAsset(currentPrize)
        } else {
            binding.resultMessageText.text = "아쉽지만, 꽝입니다."
        }
        isLottoPurchased = false
    }

    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return formatter.format(amount)
    }

    private fun showCustomSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
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
            binding.cooldownText.setTextColor(Color.RED)
        } else {
            binding.cooldownText.text = "구매 가능"
            binding.cooldownText.setTextColor(Color.parseColor("#4CAF50")) // 녹색
        }
    }
    
    // 구매 버튼 활성화 함수
    private fun enableBuyButton() {
        isBuyButtonEnabled = true
        binding.buyLottoButton.isEnabled = true
        remainingCooldownTime = 0
        updateCooldownDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 핸들러 콜백 제거
        buyCooldownHandler.removeCallbacks(cooldownCountdownRunnable)
        _binding = null
    }
}