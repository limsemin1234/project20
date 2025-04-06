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
import com.google.android.material.snackbar.Snackbar

class CircleAlbaFragment : Fragment() {

    private lateinit var circleViewModel: CircleAlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    
    private lateinit var circleLevelText: TextView
    private lateinit var circleResultText: TextView
    private lateinit var circleStatusText: TextView
    private lateinit var innerCircle: View
    private lateinit var outerCircle: View
    private lateinit var circleGameButton: Button
    private lateinit var circleSuccessCountText: TextView
    
    // 원 애니메이션 컨테이너
    private lateinit var animationContainer: FrameLayout
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_circle_alba, container, false)
        
        // ViewModels 초기화
        circleViewModel = ViewModelProvider(requireActivity())[CircleAlbaViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]
        
        // UI 요소 바인딩
        circleLevelText = view.findViewById(R.id.circleLevelText)
        circleResultText = view.findViewById(R.id.circleResultText)
        circleStatusText = view.findViewById(R.id.circleStatusText)
        innerCircle = view.findViewById(R.id.innerCircle)
        outerCircle = view.findViewById(R.id.outerCircle)
        circleGameButton = view.findViewById(R.id.circleGameButton)
        circleSuccessCountText = view.findViewById(R.id.circleSuccessCountText)
        animationContainer = view.findViewById<FrameLayout>(R.id.root_frame_layout) ?: view.parent as FrameLayout
        
        // 통합된 게임 버튼 리스너
        circleGameButton.setOnClickListener {
            circleViewModel.onGameButtonClicked()
        }
        
        // 내부 원 크기 관찰
        circleViewModel.innerCircleScale.observe(viewLifecycleOwner, Observer { scale ->
            updateInnerCircleScale(scale)
        })
        
        // 외부 원 크기 관찰
        circleViewModel.outerCircleScale.observe(viewLifecycleOwner, Observer { scale ->
            updateOuterCircleScale(scale)
        })
        
        // 쿨다운 상태 관찰
        circleViewModel.isCooldown.observe(viewLifecycleOwner, Observer { isCooldown ->
            updateStatus()
        })
        
        circleViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { cooldownTime ->
            updateStatus()
        })
        
        // 게임 활성화 상태 관찰
        circleViewModel.isGameActive.observe(viewLifecycleOwner, Observer { isActive ->
            // 버튼 텍스트 변경
            circleGameButton.text = if (isActive) "탭!" else "시작하기"
            
            // 버튼 상태 업데이트 (색상 포함)
            updateStatus()
        })
        
        // 성공 여부 관찰
        circleViewModel.lastSuccess.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                1 -> { // 성공
                    // 보상 계산
                    val reward = circleViewModel.getRewardAmount().toLong()
                    val multiplier = circleViewModel.rewardMultiplier.value ?: 1.0f
                    
                    // 실제 보상 금액 증가
                    assetViewModel.increaseAsset(reward)
                    
                    // 보상 애니메이션 표시
                    showRewardAnimation(reward, multiplier)
                }
                -1 -> { // 실패
                    showFailureMessage()
                }
            }
        })
        
        // 레벨 관찰
        circleViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val baseReward = 500 * level
            circleLevelText.text = "레벨: $level\n보상: ${"%,d".format(baseReward)}원 x 배율\n(성공 5번마다 레벨업)"
        })
        
        // 성공 횟수 관찰
        circleViewModel.successfulAttempts.observe(viewLifecycleOwner, Observer { attempts ->
            val remainingAttempts = 5 - attempts
            circleSuccessCountText.text = "레벨업까지 남은 성공: ${remainingAttempts}회"
            
            // 남은 횟수가 적을수록 텍스트 색상 변경
            val textColor = when (remainingAttempts) {
                1 -> resources.getColor(R.color.perfect_timing, null) // 1회 남으면 빨간색
                2 -> resources.getColor(R.color.good_timing, null) // 2회 남으면 주황색
                else -> resources.getColor(R.color.normal_timing, null) // 그 외에는 일반 색상
            }
            circleSuccessCountText.setTextColor(textColor)
        })
        
        // 아이템 획득 이벤트 관찰
        circleViewModel.itemRewardEvent.observe(viewLifecycleOwner, Observer { reward ->
            if (reward != null) {
                // 아이템 획득 애니메이션 표시
                showItemRewardAnimation(reward)
                circleViewModel.consumeItemRewardEvent() // 이벤트 소비
            }
        })
        
        return view
    }
    
    private fun updateInnerCircleScale(scale: Float) {
        // 크기 변경을 애니메이션 없이 즉시 적용
        val baseSize = 200 // dp 기준 크기
        val newSize = (baseSize * scale).toInt()
        
        val params = innerCircle.layoutParams
        params.width = dpToPx(newSize)
        params.height = dpToPx(newSize)
        innerCircle.layoutParams = params
        
        // 게임 진행 중일 때만 상태 메시지 업데이트
        if (circleViewModel.isGameActive.value == true) {
            checkAndUpdateCircleStatus()
        }
    }
    
    private fun updateOuterCircleScale(scale: Float) {
        // 크기 변경을 애니메이션 없이 즉시 적용
        val baseSize = 200 // dp 기준 크기
        val newSize = (baseSize * scale).toInt()
        
        val params = outerCircle.layoutParams
        params.width = dpToPx(newSize)
        params.height = dpToPx(newSize)
        outerCircle.layoutParams = params
        
        // 게임 진행 중일 때만 상태 메시지 업데이트
        if (circleViewModel.isGameActive.value == true) {
            checkAndUpdateCircleStatus()
        }
    }
    
    private fun checkAndUpdateCircleStatus() {
        val innerScale = circleViewModel.innerCircleScale.value ?: 0.0f
        val outerScale = circleViewModel.outerCircleScale.value ?: 1.0f
        val difference = Math.abs(innerScale - outerScale)
        
        // 원 크기 차이에 따라 적절한 가이드 메시지 표시
        when {
            difference <= 0.005f -> { // 두 원이 거의 완벽하게 겹칠 때만 퍼펙트 (0.5% 이내)
                circleResultText.text = "퍼펙트 타이밍! 지금 탭하세요!"
                circleResultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
                // 퍼펙트 상태일 때 원 색상 변경
                innerCircle.setBackgroundResource(R.drawable.circle_inner_perfect)
            }
            difference <= 0.15f -> {
                circleResultText.text = "좋은 타이밍! 탭하세요!"
                circleResultText.setTextColor(resources.getColor(R.color.good_timing, null))
                // 좋음 상태일 때 원 색상 원래대로
                innerCircle.setBackgroundResource(R.drawable.circle_inner)
            }
            else -> {
                circleResultText.text = "두 원이 일치할 때 탭하세요!"
                circleResultText.setTextColor(resources.getColor(R.color.normal_timing, null))
                // 일반 상태일 때 원 색상 원래대로
                innerCircle.setBackgroundResource(R.drawable.circle_inner)
            }
        }
    }
    
    private fun updateStatus() {
        val isCooldown = circleViewModel.isCooldown.value ?: false
        val cooldownTime = circleViewModel.cooldownTime.value ?: 0
        val isActive = circleViewModel.isGameActive.value ?: false
        
        circleStatusText.text = when {
            isActive -> "게임 진행 중..."
            isCooldown -> "쿨다운: ${cooldownTime}초"
            else -> "준비 완료!"
        }
        
        // 쿨다운 중이거나 쿨다운 시간이 남아있으면 버튼 비활성화
        val isButtonEnabled = !isCooldown && cooldownTime <= 0
        circleGameButton.isEnabled = isButtonEnabled
        
        // 버튼 색상 변경
        if (isButtonEnabled) {
            // 게임 활성화 상태에 따라 적절한 색상 설정
            if (isActive) {
                circleGameButton.setBackgroundTintList(resources.getColorStateList(R.color.teal_200, null))
            } else {
                circleGameButton.setBackgroundTintList(resources.getColorStateList(R.color.alba_start_button, null))
            }
        } else {
            // 비활성화된 버튼의 색상 - 회색
            circleGameButton.setBackgroundTintList(resources.getColorStateList(R.color.button_disabled, null))
        }
    }
    
    private fun showRewardAnimation(
        reward: Long,
        multiplier: Float,
        moveDuration: Long = 1500L
    ) {
        // 성공 메시지 설정
        val message = when {
            multiplier >= 5.0f -> "퍼펙트! +${"%,d".format(reward)}원"
            multiplier >= 2.0f -> "좋음! +${"%,d".format(reward)}원"
            else -> "+${"%,d".format(reward)}원"
        }
        
        // 애니메이션 색상 설정
        val textColor = when {
            multiplier >= 5.0f -> resources.getColor(R.color.perfect_timing, null)
            multiplier >= 2.0f -> resources.getColor(R.color.good_timing, null)
            else -> resources.getColor(R.color.normal_timing, null)
        }
        
        // 애니메이션 텍스트뷰 생성
        val rewardTextView = TextView(requireContext()).apply {
            text = message
            textSize = 22f
            setTextColor(textColor)
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 애니메이션 컨테이너에 추가
        animationContainer.addView(rewardTextView)
        
        // 초기 위치 설정 (원의 중앙)
        rewardTextView.x = innerCircle.x + innerCircle.width / 2 - rewardTextView.width / 2
        rewardTextView.y = innerCircle.y + innerCircle.height / 2 - rewardTextView.height / 2
        
        // 목표 위치 계산 (레벨 텍스트 쪽으로 이동)
        val targetLocation = IntArray(2)
        circleLevelText.getLocationOnScreen(targetLocation)
        
        val containerLocation = IntArray(2)
        animationContainer.getLocationOnScreen(containerLocation)
        
        val targetX = targetLocation[0] - containerLocation[0] + circleLevelText.width / 2 - rewardTextView.width / 2
        val targetY = targetLocation[1] - containerLocation[1] + circleLevelText.height / 2 - rewardTextView.height / 2
        
        // 애니메이션 생성 및 실행
        val moveX = ObjectAnimator.ofFloat(rewardTextView, "x", rewardTextView.x, targetX.toFloat()).apply {
            duration = moveDuration
        }
        val moveY = ObjectAnimator.ofFloat(rewardTextView, "y", rewardTextView.y, targetY.toFloat()).apply {
            duration = moveDuration
        }
        val fadeOut = ObjectAnimator.ofFloat(rewardTextView, "alpha", 1f, 0f).apply {
            duration = moveDuration
        }
        val scaleX = ObjectAnimator.ofFloat(rewardTextView, "scaleX", 1f, 0.5f).apply {
            duration = moveDuration
        }
        val scaleY = ObjectAnimator.ofFloat(rewardTextView, "scaleY", 1f, 0.5f).apply {
            duration = moveDuration
        }
        
        // 애니메이션 종료 시 정리
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animationContainer.removeView(rewardTextView)
            }
        })
        
        moveX.start()
        moveY.start()
        fadeOut.start()
        scaleX.start()
        scaleY.start()
    }
    
    private fun showFailureMessage() {
        // 실패 메시지 표시
        circleResultText.text = "실패! 두 원이 너무 달라요."
        circleResultText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        
        // 잠시 후 다시 기본 메시지로 복귀
        circleResultText.postDelayed({
            if (isAdded) { // Fragment가 여전히 연결되어 있는지 확인
                circleResultText.text = "두 원이 일치할 때 탭하세요!"
                circleResultText.setTextColor(resources.getColor(R.color.normal_timing, null))
            }
        }, 2000)
    }
    
    // 아이템 획득 애니메이션 표시
    private fun showItemRewardAnimation(reward: ItemReward) {
        val rewardMessage = if (reward.isMultiple) {
            "${reward.itemName} 재고 증가!"
        } else {
            "${reward.itemName} 재고 ${reward.quantity}개 증가!"
        }
        
        // Snackbar로 아이템 획득 메시지 표시
        Snackbar.make(
            requireView(),
            rewardMessage,
            Snackbar.LENGTH_LONG
        ).setAction("확인") {
            // 아무것도 하지 않음
        }.show()
    }
    
    // dp를 픽셀로 변환하는 유틸리티 함수
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
} 