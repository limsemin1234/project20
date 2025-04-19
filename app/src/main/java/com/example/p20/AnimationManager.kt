package com.example.p20

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import android.util.Log

class AnimationManager private constructor() {
    // 활성화된 애니메이션을 추적하기 위한 리스트
    private val activeAnimations = mutableListOf<AnimatorSet>()
    
    companion object {
        @Volatile
        private var instance: AnimationManager? = null
        
        fun getInstance(): AnimationManager {
            return instance ?: synchronized(this) {
                instance ?: AnimationManager().also { instance = it }
            }
        }
    }
    
    // 기본 애니메이션
    fun createFadeInAnimation(view: View, duration: Long = 300): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            this.duration = duration
        }
    }
    
    fun createFadeOutAnimation(view: View, duration: Long = 300): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            this.duration = duration
        }
    }
    
    fun createScaleAnimation(view: View, from: Float, to: Float, duration: Long = 300): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", from, to)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", from, to)
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            this.duration = duration
            interpolator = OvershootInterpolator()
        }
    }
    
    // 카지노 특화 애니메이션
    fun createCasinoWinAnimation(container: ViewGroup, message: String, amount: String): AnimatorSet {
        val context = container.context
        val animatorSet = AnimatorSet()
        
        // 배경 애니메이션
        val backgroundView = View(context).apply {
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            alpha = 0f
        }
        container.addView(backgroundView)
        
        // 메시지 애니메이션
        val messageView = TextView(context).apply {
            text = message
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        container.addView(messageView)
        
        // 금액 애니메이션
        val amountView = TextView(context).apply {
            text = amount
            textSize = 32f
            setTextColor(Color.YELLOW)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        container.addView(amountView)
        
        // 애니메이션 조합
        val fadeIn = createFadeInAnimation(backgroundView, 500)
        val scaleUp = createScaleAnimation(messageView, 0f, 1f, 500)
        val amountScale = createScaleAnimation(amountView, 0f, 1f, 500)
        
        animatorSet.playSequentially(fadeIn, scaleUp, amountScale)
        
        // 애니메이션 추적 리스트에 추가
        activeAnimations.add(animatorSet)
        
        // 종료 시 리스트에서 제거하는 리스너 추가
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                activeAnimations.remove(animatorSet)
                
                // 뷰 정리
                try {
                    container.removeView(backgroundView)
                    container.removeView(messageView)
                    container.removeView(amountView)
                } catch (e: Exception) {
                    Log.e("AnimationManager", "뷰 정리 오류: ${e.message}")
                }
            }
        })
        
        return animatorSet
    }
    
    fun createCasinoLoseAnimation(container: ViewGroup, message: String): AnimatorSet {
        val context = container.context
        val animatorSet = AnimatorSet()
        
        // 배경 애니메이션
        val backgroundView = View(context).apply {
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            alpha = 0f
        }
        container.addView(backgroundView)
        
        // 메시지 애니메이션
        val messageView = TextView(context).apply {
            text = message
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        container.addView(messageView)
        
        // 애니메이션 조합
        val fadeIn = createFadeInAnimation(backgroundView, 500)
        val shake = ObjectAnimator.ofFloat(messageView, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
            duration = 500
        }
        
        animatorSet.playSequentially(fadeIn, shake)
        
        // 애니메이션 추적 리스트에 추가
        activeAnimations.add(animatorSet)
        
        // 종료 시 리스트에서 제거하는 리스너 추가
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                activeAnimations.remove(animatorSet)
                
                // 뷰 정리
                try {
                    container.removeView(backgroundView)
                    container.removeView(messageView)
                } catch (e: Exception) {
                    Log.e("AnimationManager", "뷰 정리 오류: ${e.message}")
                }
            }
        })
        
        return animatorSet
    }
    
    // 카드 관련 애니메이션
    fun createCardFlipAnimation(view: View): AnimatorSet {
        val flipOut = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f)
        val flipIn = ObjectAnimator.ofFloat(view, "rotationY", 270f, 360f)
        
        val animatorSet = AnimatorSet().apply {
            playSequentially(flipOut, flipIn)
            duration = 300
        }
        
        // 애니메이션 추적 리스트에 추가
        activeAnimations.add(animatorSet)
        
        // 종료 시 리스트에서 제거하는 리스너 추가
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                activeAnimations.remove(animatorSet)
            }
        })
        
        return animatorSet
    }
    
    fun createChipStackAnimation(view: View): AnimatorSet {
        val scaleUp = createScaleAnimation(view, 1f, 1.2f, 200)
        val scaleDown = createScaleAnimation(view, 1.2f, 1f, 200)
        
        val animatorSet = AnimatorSet().apply {
            playSequentially(scaleUp, scaleDown)
        }
        
        // 애니메이션 추적 리스트에 추가
        activeAnimations.add(animatorSet)
        
        // 종료 시 리스트에서 제거하는 리스너 추가
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                activeAnimations.remove(animatorSet)
            }
        })
        
        return animatorSet
    }
    
    // 특수 효과 애니메이션
    fun createWinAnimation(container: ViewGroup, message: String, amount: String): AnimatorSet {
        return createCasinoWinAnimation(container, message, amount)
    }
    
    fun createLoseAnimation(container: ViewGroup, message: String): AnimatorSet {
        return createCasinoLoseAnimation(container, message)
    }
    
    /**
     * 현재 실행 중인 모든 애니메이션을 취소합니다.
     * 액티비티나 프래그먼트가 소멸될 때 호출해야 합니다.
     */
    fun cancelAllAnimations() {
        try {
            // 리스트 복사본을 만들어 ConcurrentModificationException 방지
            val animationsCopy = ArrayList(activeAnimations)
            
            for (animation in animationsCopy) {
                animation.cancel()
            }
            
            activeAnimations.clear()
            Log.d("AnimationManager", "모든 애니메이션 취소 완료")
        } catch (e: Exception) {
            Log.e("AnimationManager", "애니메이션 취소 오류: ${e.message}")
        }
    }
    
    /**
     * 애니메이션 매니저의 리소스를 정리합니다.
     * 앱이 종료될 때 호출해야 합니다.
     */
    fun release() {
        try {
            cancelAllAnimations()
            instance = null
            Log.d("AnimationManager", "리소스 해제 완료")
        } catch (e: Exception) {
            Log.e("AnimationManager", "리소스 해제 오류: ${e.message}")
        }
    }
} 