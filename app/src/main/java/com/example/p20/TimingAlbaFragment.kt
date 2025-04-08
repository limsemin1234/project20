package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.view.Gravity

class TimingAlbaFragment : Fragment() {

    private lateinit var timingViewModel: TimingAlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    
    private lateinit var timingLevelText: TextView
    private lateinit var resultText: TextView
    private lateinit var statusText: TextView
    private lateinit var pointer: View
    private lateinit var gameButton: Button
    private lateinit var successCountText: TextView
    
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
        successCountText = view.findViewById(R.id.successCountText)
        
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
            // 버튼 텍스트 변경
            gameButton.text = if (isActive) "탭!" else "시작하기"
            
            // 버튼 상태 업데이트 (색상 포함)
            updateStatus()
        })
        
        // 성공 여부 관찰
        timingViewModel.lastSuccess.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                1 -> { // 성공
                    // 보상 계산
                    val baseReward = 500 * (timingViewModel.albaLevel.value ?: 1)
                    val multiplier = timingViewModel.rewardMultiplier.value ?: 1.0f
                    val position = timingViewModel.pointerPosition.value ?: 0.0f
                    val reward = timingViewModel.getRewardAmount().toLong()
                    
                    // 직접 계산해본 보상 값
                    val calculatedReward = (baseReward * 5.0f).toLong()
                    
                    // 실제 보상 금액 증가
                    assetViewModel.increaseAsset(calculatedReward)
                    
                    // 보상 애니메이션 표시
                    showRewardAnimation(calculatedReward, 5.0f)
                }
                -1 -> { // 실패
                    showFailureMessage()
                }
            }
        })
        
        // 레벨 관찰
        timingViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val baseReward = 500 * level  // 100 -> 500으로 변경
            timingLevelText.text = "레벨: $level\n보상: ${"%,d".format(baseReward)}원 x 배율\n(성공 5번마다 레벨업)"
        })
        
        // 성공 횟수 관찰
        timingViewModel.successfulAttempts.observe(viewLifecycleOwner, Observer { attempts ->
            val remainingAttempts = 5 - attempts
            successCountText.text = "레벨업까지 남은 성공: ${remainingAttempts}회"
            
            // 남은 횟수가 적을수록 텍스트 색상 변경
            val textColor = when (remainingAttempts) {
                1 -> resources.getColor(R.color.perfect_timing, null) // 1회 남으면 빨간색
                2 -> resources.getColor(R.color.good_timing, null) // 2회 남으면 주황색
                else -> resources.getColor(R.color.normal_timing, null) // 그 외에는 일반 색상
            }
            successCountText.setTextColor(textColor)
        })
        
        // 아이템 획득 이벤트 관찰
        timingViewModel.itemRewardEvent.observe(viewLifecycleOwner, Observer { reward ->
            if (reward != null) {
                // 아이템 획득 애니메이션 표시
                showItemRewardAnimation(reward)
                timingViewModel.consumeItemRewardEvent() // 이벤트 소비
            }
        })
        
        return view
    }
    
    private fun updatePointerPosition(position: Float) {
        val parent = pointer.parent as View
        val containerWidth = parent.width - pointer.width
        
        // 새로운 포인터 넓이를 고려한 위치 계산 (포인터가 3dp로 변경되었기 때문)
        val translationX = containerWidth * position
        pointer.translationX = translationX
        
        // 게임 진행 중일 때만 안내 메시지 표시
        if (timingViewModel.isGameActive.value == true) {
            // 현재 위치가 퍼펙트 영역에 있는지 시각적으로 표시
            val isPerfectZone = position >= 0.44f && position < 0.56f
            
            if (isPerfectZone) {
                // 퍼펙트 영역에 있을 때 시각적 피드백
                resultText.text = "퍼펙트 영역! 지금 탭하세요!"
                resultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
                // 포인터 색상도 변경하여 강조
                pointer.setBackgroundColor(resources.getColor(R.color.perfect_timing, null))
            } else {
                // 퍼펙트 영역 밖에 있을 때
                resultText.text = "중앙의 빨간색 영역에 정확히 탭하세요!"
                resultText.setTextColor(resources.getColor(R.color.alba_level, null))
                // 포인터 색상 원래대로
                pointer.setBackgroundColor(resources.getColor(R.color.pointer_normal, null))
            }
        }
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
        
        // 쿨다운 중이거나 쿨다운 시간이 남아있으면 버튼 비활성화
        val isButtonEnabled = !isCooldown && cooldownTime <= 0
        gameButton.isEnabled = isButtonEnabled
        
        // 버튼 색상 변경
        if (isButtonEnabled) {
            // 게임 활성화 상태에 따라 적절한 색상 설정
            if (isActive) {
                gameButton.setBackgroundTintList(resources.getColorStateList(R.color.teal_200, null))
            } else {
                gameButton.setBackgroundTintList(resources.getColorStateList(R.color.alba_start_button, null))
            }
        } else {
            // 비활성화된 버튼의 색상 - 회색
            gameButton.setBackgroundTintList(resources.getColorStateList(R.color.button_disabled, null))
        }
    }
    
    private fun showRewardAnimation(
        reward: Long,
        multiplier: Float,
        moveDuration: Long = 1500L
    ) {
        // 포인터 위치 정보 가져오기
        val position = timingViewModel.pointerPosition.value ?: 0.0f
        val centerDistance = Math.abs(position - 0.5f)
        
        // 배율에 따른 텍스트 생성 (항상 퍼펙트)
        val multiplierText = "퍼펙트! x5"
        
        // 위치 정보 텍스트 (백분율로 표시)
        val positionPercent = (position * 100).toInt()
        val centerDistancePercent = (centerDistance * 100).toInt()
        
        // 메시지 생성
        val message = "+${"%,d".format(reward)}원 ($multiplierText)"
        
        // 가이드 메시지로 되돌리기
        resultText.text = "중앙의 빨간색 영역에 정확히 탭하세요!"
        resultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
        
        // MessageManager를 사용하여 상단에 메시지 표시
        MessageManager.showMessage(requireContext(), message)
    }
    
    private fun showFailureMessage() {
        val message = "실패! 정확한 타이밍에 탭하세요."
        MessageManager.showMessage(requireContext(), message)
    }
    
    // 아이템 획득 애니메이션 표시
    private fun showItemRewardAnimation(reward: ItemReward) {
        val message = if (reward.isMultiple) {
            "${reward.itemName} 재고 증가!"
        } else {
            "${reward.itemName} 재고 ${reward.quantity}개 증가!"
        }
        
        // MessageManager를 사용하여 상단에 메시지 표시
        MessageManager.showMessage(requireContext(), message)
    }
} 