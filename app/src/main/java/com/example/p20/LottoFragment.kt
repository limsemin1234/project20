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

    // --- 추가: 구매 버튼 쿨타임 관련 변수 ---
    private var isBuyButtonEnabled = true
    private val buyCooldownHandler = Handler(Looper.getMainLooper())
    private val buyCooldownRunnable = Runnable { enableBuyButton() } // 버튼 활성화 Runnable
    private val cooldownDuration = 10000L // 10초
    // --- 추가 끝 ---

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

        binding.buyLottoButton.setOnClickListener {
            // --- 추가: 쿨타임 중이면 클릭 무시 ---
            if (!isBuyButtonEnabled) {
                showCustomSnackbar("잠시 후 다시 시도해주세요.")
                return@setOnClickListener
            }
            // --- 추가 끝 ---
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

        // 스크래치 영역 초기 상태 설정 (기존과 동일)
        // binding.lottoResultLayout.visibility = View.GONE // 이 줄 삭제 또는 주석 처리
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

            // --- 추가: 구매 버튼 비활성화 및 쿨타임 시작 ---
            disableBuyButtonTemporarily()
            // --- 추가 끝 ---
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
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.argb(150, 50, 50, 50))
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
        }
        snackbar.show()
    }

    // --- 추가: 구매 버튼 임시 비활성화 함수 ---
    private fun disableBuyButtonTemporarily() {
        isBuyButtonEnabled = false
        binding.buyLottoButton.isEnabled = false
        buyCooldownHandler.postDelayed(buyCooldownRunnable, cooldownDuration)
    }
    // --- 추가 끝 ---

    // --- 추가: 구매 버튼 활성화 함수 ---
    private fun enableBuyButton() {
        isBuyButtonEnabled = true
        binding.buyLottoButton.isEnabled = true
    }
    // --- 추가 끝 ---

    override fun onDestroyView() {
        super.onDestroyView()
        // --- 추가: 핸들러 콜백 제거 ---
        buyCooldownHandler.removeCallbacks(buyCooldownRunnable)
        // --- 추가 끝 ---
        _binding = null
    }
}