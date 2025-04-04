package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class TimingAlbaFragment : Fragment() {

    private lateinit var timingViewModel: TimingAlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    
    private lateinit var timingLevelText: TextView
    private lateinit var resultText: TextView
    private lateinit var statusText: TextView
    private lateinit var pointer: View
    private lateinit var gameButton: Button
    private lateinit var animationContainer: FrameLayout
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timing_alba, container, false)
        
        // ViewModels 초기화
        timingViewModel = ViewModelProvider(requireActivity())[TimingAlbaViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]
        
        // UI 요소 바인딩
        timingLevelText = view.findViewById(R.id.timingLevelText)
        resultText = view.findViewById(R.id.resultText)
        statusText = view.findViewById(R.id.statusText)
        pointer = view.findViewById(R.id.pointer)
        gameButton = view.findViewById(R.id.gameButton)
        animationContainer = view.findViewById(R.id.timingAnimationContainer)
        
        // 통합된 게임 버튼 리스너
        gameButton.setOnClickListener {
            timingViewModel.onGameButtonClicked()
        }
        
        // 포인터 위치 관찰
        timingViewModel.pointerPosition.observe(viewLifecycleOwner, Observer { position ->
            updatePointerPosition(position)
        })
        
        // 쿨다운 상태 관찰
        timingViewModel.isCooldown.observe(viewLifecycleOwner, Observer { isCooldown ->
            updateStatus()
        })
        
        timingViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { cooldownTime ->
            updateStatus()
        })
        
        // 게임 활성화 상태 관찰
        timingViewModel.isGameActive.observe(viewLifecycleOwner, Observer { isActive ->
            // 버튼 텍스트와 색상 변경
            if (isActive) {
                gameButton.text = "탭!"
                gameButton.setBackgroundTintList(resources.getColorStateList(R.color.teal_200, null))
            } else {
                gameButton.text = "시작하기"
                gameButton.setBackgroundTintList(resources.getColorStateList(R.color.alba_start_button, null))
            }
            
            // 쿨다운 중이면 버튼 비활성화
            gameButton.isEnabled = !timingViewModel.isCooldown.value!!
        })
        
        // 성공 여부 관찰
        timingViewModel.lastSuccess.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                1 -> { // 성공
                    val reward = timingViewModel.getRewardAmount().toLong()
                    assetViewModel.increaseAsset(reward)
                    resultText.text = "성공! +${"%,d".format(reward)}원"
                    resultText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    
                    // 보상 애니메이션 표시
                    showRewardAnimation(reward, timingViewModel.rewardMultiplier.value ?: 1.0f)
                }
                -1 -> { // 실패
                    resultText.text = "실패! 다시 시도하세요."
                    resultText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            }
        })
        
        // 레벨 관찰
        timingViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val baseReward = 500 * level  // 100 -> 500으로 변경
            timingLevelText.text = "레벨: $level\n보상: ${"%,d".format(baseReward)}원 x 배율\n(성공 5번마다 레벨업)"
        })
        
        return view
    }
    
    private fun updatePointerPosition(position: Float) {
        val containerWidth = (pointer.parent as View).width - pointer.width
        val translationX = containerWidth * position
        pointer.translationX = translationX
    }
    
    private fun updateStatus() {
        val isCooldown = timingViewModel.isCooldown.value ?: false
        val cooldownTime = timingViewModel.cooldownTime.value ?: 0
        val isActive = timingViewModel.isGameActive.value ?: false
        
        statusText.text = when {
            isActive -> "게임 진행 중..."
            isCooldown -> "쿨다운: ${cooldownTime}초"
            else -> "준비 완료!"
        }
        
        // 쿨다운 중인 경우 버튼 비활성화
        gameButton.isEnabled = !isCooldown
    }
    
    private fun showRewardAnimation(
        reward: Long,
        multiplier: Float,
        moveDuration: Long = 1500L
    ) {
        // 배율에 따른 텍스트 색상
        val textColor = when {
            multiplier >= 4.5f -> resources.getColor(R.color.perfect_timing, null) // 퍼펙트
            multiplier >= 3.0f -> resources.getColor(R.color.good_timing, null) // 좋음
            else -> resources.getColor(R.color.normal_timing, null) // 보통
        }
        
        // 배율에 따른 텍스트
        val multiplierText = when {
            multiplier >= 4.5f -> "퍼펙트! x${String.format("%.1f", multiplier)}"
            multiplier >= 3.0f -> "좋아요! x${String.format("%.1f", multiplier)}"
            multiplier >= 2.0f -> "괜찮아요! x${String.format("%.1f", multiplier)}"
            else -> "x${String.format("%.1f", multiplier)}"
        }
        
        // 보상 애니메이션 텍스트뷰
        val rewardTextView = TextView(requireContext()).apply {
            text = "+${"%,d".format(reward)}원\n$multiplierText"
            textSize = 22f
            setTextColor(textColor)
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            gravity = android.view.Gravity.CENTER
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 애니메이션 컨테이너에 추가
        animationContainer.addView(rewardTextView)
        rewardTextView.x = (animationContainer.width - rewardTextView.width) / 2f
        rewardTextView.y = animationContainer.height / 3f
        
        // 애니메이션 효과
        val fadeIn = ObjectAnimator.ofFloat(rewardTextView, "alpha", 0f, 1f).apply {
            duration = moveDuration / 3
        }
        val fadeOut = ObjectAnimator.ofFloat(rewardTextView, "alpha", 1f, 0f).apply {
            duration = moveDuration
            startDelay = moveDuration / 2
        }
        val scaleX = ObjectAnimator.ofFloat(rewardTextView, "scaleX", 0.5f, 1.2f, 1f).apply {
            duration = moveDuration / 2
        }
        val scaleY = ObjectAnimator.ofFloat(rewardTextView, "scaleY", 0.5f, 1.2f, 1f).apply {
            duration = moveDuration / 2
        }
        
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animationContainer.removeView(rewardTextView)
            }
        })
        
        fadeIn.start()
        fadeOut.start()
        scaleX.start()
        scaleY.start()
    }
} 