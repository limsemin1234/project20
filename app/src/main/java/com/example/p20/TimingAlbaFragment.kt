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
            android.util.Log.e("TimingAlbaFragment", "판정 결과: $result (1:성공, -1:실패, 0:초기상태)")
            android.util.Log.e("TimingAlbaFragment", "현재 포인터 위치: ${timingViewModel.pointerPosition.value}")
            android.util.Log.e("TimingAlbaFragment", "배율: ${timingViewModel.rewardMultiplier.value}")
            
            when (result) {
                1 -> { // 성공
                    val reward = timingViewModel.getRewardAmount().toLong()
                    android.util.Log.e("TimingAlbaFragment", "성공 처리 - 보상: $reward, 배율: ${timingViewModel.rewardMultiplier.value}")
                    assetViewModel.increaseAsset(reward)
                    
                    // 보상 애니메이션 표시
                    showRewardAnimation(reward, timingViewModel.rewardMultiplier.value ?: 1.0f)
                }
                -1 -> { // 실패
                    android.util.Log.e("TimingAlbaFragment", "실패 처리")
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
        
        // 시각적 영역 계산 - 전체 화면 사용
        val totalContainerWidth = parent.width
        
        // 중앙 기준 거리 계산 (0.5에서 얼마나 떨어졌는지)
        val centerDistance = Math.abs(position - 0.5f)
        
        // 화면 위치 계산 - 원본 위치 그대로 사용
        val translationX = containerWidth * position
        pointer.translationX = translationX
        
        // 20번에 한 번씩만 로그 출력 (너무 많은 로그 방지)
        if (position * 100 % 20 < 1) {
            android.util.Log.e("TimingAlbaFragment", "화면 포인터 위치 업데이트")
            android.util.Log.e("TimingAlbaFragment", "위치: $position")
            android.util.Log.e("TimingAlbaFragment", "중앙 거리: $centerDistance") 
            android.util.Log.e("TimingAlbaFragment", "화면 X좌표: $translationX / $containerWidth")
            
            // 현재 영역 확인
            val multiplierZone = if (centerDistance <= timingViewModel.getMultiplierZoneSize(5)) {
                "5배 영역 (퍼펙트)"
            } else {
                "실패 영역"
            }
            android.util.Log.e("TimingAlbaFragment", "현재 예상 영역: $multiplierZone")
        }
        
        // 게임 진행 중일 때만 안내 메시지 표시
        if (timingViewModel.isGameActive.value == true) {
            // 가이드 메시지만 표시 (디버깅 정보 표시하지 않음)
            resultText.text = "중앙의 빨간색 영역에 정확히 탭하세요!"
            resultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
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
        
        // 추가 로그
        android.util.Log.e("TimingAlbaFragment", "성공 상세 - 위치: $position")
        android.util.Log.e("TimingAlbaFragment", "성공 상세 - 중앙 거리: $centerDistance, 배율: $multiplier")
        
        // 배율에 따른 텍스트 색상 및 배경색 (항상 퍼펙트)
        val backgroundColor = Color.argb(200, 211, 47, 47) // 5배 - 빨간색 (#D32F2F)
        
        // 배율 텍스트 (항상 퍼펙트)
        val multiplierText = "퍼펙트! x5"
        
        // 스낵바 메시지 생성
        val message = "+${"%,d".format(reward)}원\n$multiplierText"
        
        // 가이드 메시지로 되돌리기
        resultText.text = "중앙의 빨간색 영역에 정확히 탭하세요!"
        resultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
        
        // 카지노 스타일 스낵바 표시
        val activity = requireActivity()
        val snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        
        // 스낵바 스타일 커스터마이징
        snackbarView.setBackgroundColor(backgroundColor)
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textSize = 18f
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        textView.maxLines = 2
        
        // 중앙 배치
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
            // 레이아웃 파라미터 설정 중 오류 발생시 무시
        }
        
        snackbar.show()
        
        // 일정 시간 후 게임 상태 초기화
        Handler(Looper.getMainLooper()).postDelayed({
            resetGameUI()
        }, 3000) // 3초로 늘려 정보를 더 오래 볼 수 있게 함
    }
    
    // 게임 UI를 초기 상태로 재설정하는 메소드
    private fun resetGameUI() {
        // 가이드 메시지 초기화
        resultText.text = "중앙의 빨간색 영역에 정확히 탭하세요!"
        resultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
        
        // 게임 상태만 초기화 (성공 횟수는 유지)
        timingViewModel.resetGameState()
    }
    
    // 아이템 획득 알림을 스낵바로 표시 (카지노 스타일)
    private fun showItemRewardAnimation(reward: ItemReward) {
        // 아이템 획득 메시지 생성
        val message = if (reward.isMultiple) {
            "${reward.itemName} 재고 증가!"
        } else {
            "${reward.itemName} 재고 ${reward.quantity}개 증가!"
        }
        
        // 카지노 스타일 스낵바 표시
        val activity = requireActivity()
        val snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        
        // 스낵바 스타일 커스터마이징
        snackbarView.setBackgroundColor(Color.argb(200, 33, 150, 243)) // 파란색
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textSize = 16f
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        
        // 중앙 배치
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
            // 레이아웃 파라미터 설정 중 오류 발생시 무시
        }
        
        snackbar.show()
    }
    
    private fun showFailureMessage() {
        val position = timingViewModel.pointerPosition.value ?: 0.0f
        val centerDistance = Math.abs(position - 0.5f)
        
        // 가이드 메시지로 되돌리기
        resultText.text = "중앙의 빨간색 영역에 정확히 탭하세요!"
        resultText.setTextColor(resources.getColor(R.color.perfect_timing, null))
        
        // 스낵바로 실패 메시지 표시
        val message = "실패! 다시 시도하세요."
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.argb(200, 158, 158, 158)) // 회색
        
        // 스낵바 텍스트 스타일
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.textSize = 18f
        textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        
        // 중앙 배치
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
            // 레이아웃 파라미터 설정 중 오류 발생시 무시
        }
        
        snackbar.show()
        
        // 추가 로그
        android.util.Log.e("TimingAlbaFragment", "실패 상세 - 위치: $position")
        android.util.Log.e("TimingAlbaFragment", "실패 상세 - 중앙 거리: $centerDistance, 판정 기준: 0.2")
        
        // 일정 시간 후 메시지 초기화 및 게임 UI 재설정
        Handler(Looper.getMainLooper()).postDelayed({
            resetGameUI()
        }, 3000) // 3초로 늘려 정보를 더 오래 볼 수 있게 함
    }
} 