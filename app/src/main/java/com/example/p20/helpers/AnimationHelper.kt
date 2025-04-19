package com.example.p20.helpers

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation

/**
 * 애니메이션 관련 유틸리티 기능을 제공하는 헬퍼 클래스
 * 애니메이션 생성, 관리 등의 중복 코드 제거를 위해 사용
 */
object AnimationHelper {
    
    private const val TAG = "AnimationHelper"
    
    /**
     * 페이드 인 애니메이션을 생성합니다.
     * 
     * @param view 애니메이션을 적용할 뷰
     * @param duration 애니메이션 시간(밀리초)
     * @return 생성된 애니메이션 객체
     */
    fun createFadeInAnimation(view: View, duration: Long = 300): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            this.duration = duration
        }
    }
    
    /**
     * 페이드 아웃 애니메이션을 생성합니다.
     * 
     * @param view 애니메이션을 적용할 뷰
     * @param duration 애니메이션 시간(밀리초)
     * @return 생성된 애니메이션 객체
     */
    fun createFadeOutAnimation(view: View, duration: Long = 300): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            this.duration = duration
        }
    }
    
    /**
     * 스케일 애니메이션을 생성합니다.
     * 
     * @param view 애니메이션을 적용할 뷰
     * @param from 시작 스케일
     * @param to 종료 스케일
     * @param duration 애니메이션 시간(밀리초)
     * @param interpolator 인터폴레이터
     * @return 생성된 애니메이션 세트
     */
    fun createScaleAnimation(
        view: View, 
        from: Float, 
        to: Float, 
        duration: Long = 300,
        interpolator: Interpolator = OvershootInterpolator()
    ): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", from, to)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", from, to)
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            this.duration = duration
            this.interpolator = interpolator
        }
    }
    
    /**
     * 다양한 속성 애니메이션을 생성합니다.
     * 
     * @param view 애니메이션을 적용할 뷰
     * @param property 애니메이션을 적용할 속성
     * @param values 애니메이션 값들
     * @param duration 애니메이션 시간(밀리초)
     * @return 생성된 애니메이션 객체
     */
    fun createPropertyAnimation(
        view: View, 
        property: String, 
        vararg values: Float, 
        duration: Long = 300
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, property, *values).apply {
            this.duration = duration
        }
    }
    
    /**
     * 흔들림 애니메이션을 생성합니다.
     * 
     * @param intensity 흔들림 강도
     * @param duration 애니메이션 시간(밀리초)
     * @param repeatCount 반복 횟수
     * @return 생성된 애니메이션 객체
     */
    fun createShakeAnimation(
        intensity: Float = 10f, 
        duration: Long = 500, 
        repeatCount: Int = Animation.INFINITE
    ): TranslateAnimation {
        return TranslateAnimation(
            -intensity, intensity, -intensity/2, intensity/2
        ).apply {
            this.duration = duration
            this.repeatCount = repeatCount
            this.repeatMode = Animation.REVERSE
        }
    }
    
    /**
     * 깜빡임 애니메이션을 생성합니다.
     * 
     * @param duration 애니메이션 시간(밀리초)
     * @param repeatCount 반복 횟수
     * @return 생성된 애니메이션 객체
     */
    fun createBlinkAnimation(duration: Long = 500, repeatCount: Int = Animation.INFINITE): AlphaAnimation {
        return AlphaAnimation(0.0f, 1.0f).apply {
            this.duration = duration
            this.repeatMode = Animation.REVERSE
            this.repeatCount = repeatCount
        }
    }
    
    /**
     * 심장 박동 애니메이션을 생성합니다.
     * 
     * @param view 애니메이션을 적용할 뷰
     * @param intensity 박동 강도
     * @param duration 애니메이션 시간(밀리초)
     * @return 생성된 애니메이션 객체
     */
    fun createHeartbeatAnimation(view: View, intensity: Float = 0.1f, duration: Long = 1000): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.0f + intensity).apply {
            this.duration = duration
            this.repeatCount = ValueAnimator.INFINITE
            this.repeatMode = ValueAnimator.REVERSE
            
            // Y축 스케일도 함께 변경
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                view.scaleY = value
            }
        }
    }
    
    /**
     * 애니메이션 세트에 종료 리스너를 추가합니다.
     * 
     * @param animator 애니메이션 객체
     * @param onEnd 종료 시 실행할 콜백
     * @return 리스너가 추가된 애니메이션 객체
     */
    fun <T : Animator> addEndListener(animator: T, onEnd: () -> Unit): T {
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        return animator
    }
    
    /**
     * 뷰 가시성 변경 애니메이션을 실행합니다.
     * 
     * @param view 애니메이션을 적용할 뷰
     * @param visible 가시성 여부
     * @param duration 애니메이션 시간(밀리초)
     */
    fun animateVisibility(view: View, visible: Boolean, duration: Long = 300) {
        if (visible) {
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(duration).start()
        } else {
            view.animate().alpha(0f).setDuration(duration)
                .withEndAction {
                    view.visibility = View.GONE
                }.start()
        }
    }
    
    /**
     * 여러 애니메이션을 순차적으로 실행하는 세트를 생성합니다.
     * 
     * @param animators 순차 실행할 애니메이션 목록
     * @return 생성된 애니메이션 세트
     */
    fun createSequentialAnimationSet(vararg animators: Animator): AnimatorSet {
        return AnimatorSet().apply {
            playSequentially(*animators)
        }
    }
    
    /**
     * 여러 애니메이션을 동시에 실행하는 세트를 생성합니다.
     * 
     * @param animators 동시 실행할 애니메이션 목록
     * @return 생성된 애니메이션 세트
     */
    fun createParallelAnimationSet(vararg animators: Animator): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(*animators)
        }
    }
    
    /**
     * 애니메이션을 중지합니다.
     * 
     * @param animator 중지할 애니메이션 객체
     */
    fun cancelAnimation(animator: Animator?) {
        try {
            animator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "애니메이션 취소 오류: ${e.message}")
        }
    }
    
    /**
     * 뷰의 모든 애니메이션을 중지합니다.
     * 
     * @param view 애니메이션을 중지할 뷰
     */
    fun clearViewAnimations(view: View) {
        try {
            view.clearAnimation()
            view.animate().cancel()
        } catch (e: Exception) {
            Log.e(TAG, "뷰 애니메이션 취소 오류: ${e.message}")
        }
    }
} 