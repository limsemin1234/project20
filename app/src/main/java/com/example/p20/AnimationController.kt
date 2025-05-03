package com.example.p20

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.LiveData

/**
 * 게임의 모든 애니메이션 효과를 관리하는 컨트롤러 클래스
 * - 시간 경고 효과
 * - 시각적 피드백
 * - 화면 효과
 */
class AnimationController private constructor(private val context: Context) {

    // 애니메이션 객체들
    private var timeBlinkAnimation: AlphaAnimation? = null
    private var heartbeatAnimator: android.animation.ObjectAnimator? = null
    private var shakeAnimation: TranslateAnimation? = null
    private var freezeScaleAnimation: android.animation.ValueAnimator? = null
    private var visionNarrowingScaleAnimator: android.animation.ValueAnimator? = null
    private var flashAnimator: android.animation.ObjectAnimator? = null
    
    // 효과 상태 변수
    private var warningEffectLevel = 0 // 0: 없음, 1: 약함, 2: 중간, 3: 강함
    
    // 핸들러 관리 변수
    private val activeHandlers = mutableListOf<Handler>()
    private val activeRunnables = mutableMapOf<Handler, Runnable>()
    
    // 외부 참조
    private var soundController: SoundController? = null
    private var remainingTimeLiveData: LiveData<Int>? = null
    
    companion object {
        @Volatile
        private var instance: AnimationController? = null
        
        fun getInstance(context: Context): AnimationController {
            return instance ?: synchronized(this) {
                instance ?: AnimationController(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 필요한 외부 참조를 설정합니다.
     */
    fun setSoundController(controller: SoundController) {
        this.soundController = controller
    }
    
    /**
     * 남은 시간 LiveData를 설정합니다.
     */
    fun setRemainingTimeLiveData(liveData: LiveData<Int>) {
        this.remainingTimeLiveData = liveData
    }
    
    /**
     * 시간 경고 깜빡임 애니메이션을 설정합니다.
     */
    fun setupTimeBlinkAnimation(textView: TextView) {
        if (timeBlinkAnimation == null) {
            timeBlinkAnimation = AlphaAnimation(0.0f, 1.0f).apply {
                duration = Constants.ANIMATION_DURATION_MEDIUM
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            textView.startAnimation(timeBlinkAnimation)
        }
    }
    
    /**
     * 남은 시간에 따른 위급 상황 효과를 설정합니다.
     * @param remainingSeconds 남은 시간(초)
     * @param contentContainer 화면 컨테이너 뷰
     * @param timeWarningEffect 시간 경고 효과 뷰
     * @param flashEffect 플래시 효과 뷰
     */
    fun setupEmergencyEffects(
        remainingSeconds: Long,
        contentContainer: FrameLayout,
        timeWarningEffect: View,
        flashEffect: View
    ) {
        // 효과 레벨 결정
        val newEffectLevel = when {
            remainingSeconds > Constants.WARNING_TIME_THRESHOLD -> 0 // 효과 없음
            remainingSeconds > 10 -> 1 // 약한 효과
            remainingSeconds > Constants.CRITICAL_TIME_THRESHOLD -> 2  // 중간 효과
            else -> 3 // 강한 효과
        }
        
        // 효과 레벨이 변경된 경우에만 새로운 효과 설정
        if (newEffectLevel != warningEffectLevel) {
            warningEffectLevel = newEffectLevel
            
            // 기존 애니메이션 정리
            heartbeatAnimator?.cancel()
            heartbeatAnimator = null
            shakeAnimation?.cancel()
            contentContainer.clearAnimation()
            
            when (warningEffectLevel) {
                0 -> {
                    // 모든 효과 제거
                    stopAllAnimations(contentContainer, timeWarningEffect, flashEffect)
                    
                    // 원래 음악으로 돌아가기 (만약 20초 효과 음악이 재생 중이었다면)
                    soundController?.let {
                        if (it.isPlaying15SecondWarning()) {
                            it.restoreOriginalMusic()
                        }
                    }
                    return
                }
                1 -> {
                    // 약한 효과: 미세한 심장박동만
                    heartbeatAnimator = android.animation.ObjectAnimator.ofFloat(
                        contentContainer, "scaleX", 1.0f, 1.01f
                    ).apply {
                        duration = Constants.ANIMATION_DURATION_LONG
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                        
                        // Y축 스케일도 함께 변경
                        addUpdateListener { animator ->
                            val value = animator.animatedValue as Float
                            contentContainer.scaleY = value
                        }
                        
                        start()
                    }
                    
                    // 약한 빨간색 효과
                    timeWarningEffect.visibility = View.VISIBLE
                    timeWarningEffect.animate().alpha(0.2f).setDuration(Constants.ANIMATION_DURATION_MEDIUM).start()
                    
                    // 20초 효과 음악 재생
                    soundController?.setTemporaryMusic(R.raw.time_20_second_2)
                }
                2 -> {
                    // 중간 효과: 심장박동 + 약한 흔들림
                    heartbeatAnimator = android.animation.ObjectAnimator.ofFloat(
                        contentContainer, "scaleX", 1.0f, 1.02f
                    ).apply {
                        duration = Constants.ANIMATION_DURATION_MEDIUM
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                        
                        addUpdateListener { animator ->
                            val value = animator.animatedValue as Float
                            contentContainer.scaleY = value
                        }
                        
                        start()
                    }
                    
                    // 약한 흔들림 효과
                    shakeAnimation = TranslateAnimation(
                        -2f, 2f, -1f, 1f
                    ).apply {
                        duration = Constants.ANIMATION_DURATION_SHORT / 3
                        repeatCount = Animation.INFINITE
                        repeatMode = Animation.REVERSE
                    }
                    contentContainer.startAnimation(shakeAnimation)
                    
                    // 중간 빨간색 효과
                    timeWarningEffect.visibility = View.VISIBLE
                    timeWarningEffect.animate().alpha(0.4f).setDuration(Constants.ANIMATION_DURATION_MEDIUM).start()
                    
                    // 간헐적 플래시 효과 (10초에 한번)
                    scheduleFlashEffect(flashEffect, 10000)
                }
                3 -> {
                    // 강한 효과: 빠른 심장박동 + 강한 흔들림
                    heartbeatAnimator = android.animation.ObjectAnimator.ofFloat(
                        contentContainer, "scaleX", 1.0f, 1.04f
                    ).apply {
                        duration = Constants.ANIMATION_DURATION_SHORT
                        repeatCount = android.animation.ObjectAnimator.INFINITE
                        repeatMode = android.animation.ObjectAnimator.REVERSE
                        
                        addUpdateListener { animator ->
                            val value = animator.animatedValue as Float
                            contentContainer.scaleY = value
                        }
                        
                        start()
                    }
                    
                    // 강한 흔들림 효과
                    shakeAnimation = TranslateAnimation(
                        -5f, 5f, -3f, 3f
                    ).apply {
                        duration = 50
                        repeatCount = Animation.INFINITE
                        repeatMode = Animation.REVERSE
                    }
                    contentContainer.startAnimation(shakeAnimation)
                    
                    // 강한 빨간색 효과
                    timeWarningEffect.visibility = View.VISIBLE
                    timeWarningEffect.animate().alpha(0.6f).setDuration(Constants.ANIMATION_DURATION_MEDIUM).start()
                    
                    // 빈번한 플래시 효과 (3초에 한번)
                    scheduleFlashEffect(flashEffect, 3000)
                }
            }
        }
    }

    /**
     * 번쩍임 효과를 주기적으로 실행
     */
    private fun scheduleFlashEffect(flashEffect: View, intervalMs: Int) {
        // 기존 핸들러가 있다면 취소
        val existingHandlers = activeHandlers.toList() // 복사본 생성 (ConcurrentModificationException 방지)
        for (handler in existingHandlers) {
            val runnable = activeRunnables[handler]
            if (runnable != null) {
                handler.removeCallbacks(runnable)
                activeRunnables.remove(handler)
            }
        }
        
        // 새 핸들러 생성
        val handler = Handler(Looper.getMainLooper())
        activeHandlers.add(handler) // 추적 목록에 추가
        
        val flashRunnable = object : Runnable {
            override fun run() {
                if (warningEffectLevel >= 2) {
                    // 번쩍임 효과 실행
                    flashEffect.visibility = View.VISIBLE
                    flashEffect.alpha = 0.3f
                    
                    flashEffect.animate()
                        .alpha(0f)
                        .setDuration(150)
                        .withEndAction {
                            flashEffect.visibility = View.INVISIBLE
                        }
                        .start()
                    
                    // 다음 번쩍임 예약
                    if (warningEffectLevel >= 2) {
                        handler.postDelayed(this, intervalMs.toLong())
                    }
                }
            }
        }
        
        // 첫 번쩍임 예약 및 추적
        activeRunnables[handler] = flashRunnable
        handler.postDelayed(flashRunnable, intervalMs.toLong())
    }

    /**
     * 모든 애니메이션을 중지합니다.
     */
    fun stopAllAnimations(
        contentContainer: FrameLayout,
        timeWarningEffect: View,
        flashEffect: View,
        visionNarrowingEffect: View? = null,
        timeTextView: TextView? = null
    ) {
        // 효과 레벨 초기화
        warningEffectLevel = 0
        
        // 모든 핸들러 및 Runnable 중지
        val handlersCopy = activeHandlers.toList() // 복사본 생성
        for (handler in handlersCopy) {
            handler.removeCallbacksAndMessages(null)
            activeRunnables.remove(handler)
        }
        activeHandlers.clear()
        
        // 시간 텍스트뷰 애니메이션 중지
        timeTextView?.clearAnimation()
        timeBlinkAnimation = null
        
        // 콘텐츠 컨테이너 애니메이션 중지
        contentContainer.clearAnimation()
        contentContainer.scaleX = 1.0f
        contentContainer.scaleY = 1.0f
        
        // 흔들림 효과 중지
        shakeAnimation = null
        
        // 심장박동 효과 중지
        heartbeatAnimator?.cancel()
        heartbeatAnimator = null
        
        // 빨간색 효과 중지
        timeWarningEffect.clearAnimation()
        timeWarningEffect.visibility = View.INVISIBLE
        timeWarningEffect.alpha = 0f
        
        // 시야 축소 효과 중지 (있는 경우)
        visionNarrowingEffect?.clearAnimation()
        visionNarrowingEffect?.visibility = View.INVISIBLE
        visionNarrowingEffect?.alpha = 0f
        
        // 플래시 효과 중지
        flashEffect.clearAnimation()
        flashEffect.visibility = View.INVISIBLE
        flashEffect.alpha = 0f
        
        // 기존 애니메이션 변수 정리
        freezeScaleAnimation?.cancel()
        freezeScaleAnimation = null
        visionNarrowingScaleAnimator?.cancel()
        visionNarrowingScaleAnimator = null
        flashAnimator?.cancel()
        flashAnimator = null
        
        // 20초 효과 음악이 재생 중이었다면 원래 음악으로 돌아가기
        // 하지만 여전히 20초 이하라면 음악을 유지
        soundController?.let {
            if (it.isPlaying15SecondWarning() && !isPlaying15SecondWarning()) {
                it.restoreOriginalMusic()
            }
        }
    }
    
    /**
     * 임시로 설정된 15초 경고 음악이 재생 중인지 확인
     */
    private fun isPlaying15SecondWarning(): Boolean {
        // 남은 시간이 15초 이하인지 확인
        return remainingTimeLiveData?.value?.let { it <= 15 } ?: false
    }
    
    /**
     * 리소스 해제
     */
    fun release() {
        // 모든 핸들러 및 Runnable 중지
        val handlersCopy = activeHandlers.toList()
        for (handler in handlersCopy) {
            handler.removeCallbacksAndMessages(null)
            activeRunnables.remove(handler)
        }
        activeHandlers.clear()
        
        // 애니메이션 객체 정리
        heartbeatAnimator?.cancel()
        heartbeatAnimator = null
        freezeScaleAnimation?.cancel()
        freezeScaleAnimation = null
        visionNarrowingScaleAnimator?.cancel()
        visionNarrowingScaleAnimator = null
        flashAnimator?.cancel()
        flashAnimator = null
        
        timeBlinkAnimation = null
        shakeAnimation = null
        
        // 참조 해제
        soundController = null
        remainingTimeLiveData = null
    }
} 