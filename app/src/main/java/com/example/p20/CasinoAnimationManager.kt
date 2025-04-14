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

/**
 * 카지노 게임의 승리와 패배 시 화려한 애니메이션을 표시하는 매니저 클래스
 */
class CasinoAnimationManager {
    
    companion object {
        /**
         * 승리 애니메이션을 표시합니다.
         * @param rootView 애니메이션을 표시할 루트 뷰
         * @param message 승리 메시지
         * @param amount 획득한 금액 (포맷팅된 문자열)
         */
        fun showWinAnimation(rootView: ViewGroup, message: String, amount: String) {
            val context = rootView.context
            
            // 애니메이션 컨테이너 생성
            val container = FrameLayout(context)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            container.layoutParams = layoutParams
            
            // 반투명 배경 생성
            val backgroundView = View(context)
            backgroundView.layoutParams = layoutParams
            backgroundView.setBackgroundColor(Color.argb(150, 0, 0, 0))
            backgroundView.alpha = 0f
            container.addView(backgroundView)
            
            // 승리 카드 생성
            val cardView = CardView(context)
            val cardParams = FrameLayout.LayoutParams(
                dpToPx(context, 300),
                dpToPx(context, 200)
            )
            cardParams.gravity = Gravity.CENTER
            cardView.layoutParams = cardParams
            cardView.radius = dpToPx(context, 16).toFloat()
            cardView.setCardBackgroundColor(Color.parseColor("#FFD700")) // 금색
            cardView.alpha = 0f
            cardView.scaleX = 0.5f
            cardView.scaleY = 0.5f
            
            // 카드 내용 컨테이너
            val contentLayout = FrameLayout(context)
            contentLayout.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // 승리 메시지 텍스트
            val titleText = TextView(context)
            val titleParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            titleParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            titleParams.topMargin = dpToPx(context, 24)
            titleText.layoutParams = titleParams
            titleText.text = "🎰 승리 🎰"
            titleText.setTextColor(Color.parseColor("#8B4513")) // 진한 갈색
            titleText.textSize = 24f
            titleText.typeface = Typeface.DEFAULT_BOLD
            contentLayout.addView(titleText)
            
            // 획득 금액 텍스트
            val amountText = TextView(context)
            val amountParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            amountParams.gravity = Gravity.CENTER
            amountText.layoutParams = amountParams
            amountText.text = amount
            amountText.setTextColor(Color.parseColor("#8B4513")) // 진한 갈색
            amountText.textSize = 28f
            amountText.typeface = Typeface.DEFAULT_BOLD
            contentLayout.addView(amountText)
            
            // 상세 메시지 텍스트
            val detailText = TextView(context)
            val detailParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            detailParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            detailParams.bottomMargin = dpToPx(context, 24)
            detailText.layoutParams = detailParams
            detailText.text = message
            detailText.setTextColor(Color.parseColor("#8B4513")) // 진한 갈색
            detailText.textSize = 16f
            contentLayout.addView(detailText)
            
            cardView.addView(contentLayout)
            container.addView(cardView)
            
            // 3D 칩 이미지 생성 (여러 개 흩뿌려짐)
            val chipCount = 15
            val random = java.util.Random()
            val chips = Array(chipCount) { 
                val chip = ImageView(context)
                val chipSize = dpToPx(context, 40)
                val chipParams = FrameLayout.LayoutParams(chipSize, chipSize)
                chipParams.gravity = Gravity.CENTER
                chip.layoutParams = chipParams
                chip.setImageResource(R.drawable.ic_casino_chip) // 카지노 칩 이미지 리소스
                chip.setColorFilter(Color.parseColor("#FFD700")) // 황금색
                chip.alpha = 0f
                chip.translationY = dpToPx(context, 100).toFloat()
                container.addView(chip)
                chip
            }
            
            // 루트 뷰에 컨테이너 추가
            rootView.addView(container)
            
            // 애니메이션 시작
            // 1. 배경 페이드 인
            val backgroundFadeIn = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 1f)
            backgroundFadeIn.duration = 300
            
            // 2. 카드 애니메이션
            val cardFadeIn = ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f)
            cardFadeIn.duration = 500
            
            val cardScaleX = ObjectAnimator.ofFloat(cardView, "scaleX", 0.5f, 1.1f, 1f)
            cardScaleX.duration = 500
            cardScaleX.interpolator = OvershootInterpolator(1.5f)
            
            val cardScaleY = ObjectAnimator.ofFloat(cardView, "scaleY", 0.5f, 1.1f, 1f)
            cardScaleY.duration = 500
            cardScaleY.interpolator = OvershootInterpolator(1.5f)
            
            // 3. 칩 애니메이션
            val chipAnimators = ArrayList<ObjectAnimator>()
            chips.forEach { chip ->
                // 랜덤한 방향으로 날아가도록 설정
                val targetX = (random.nextFloat() * 2 - 1) * dpToPx(context, 150)
                val targetY = (random.nextFloat() * 2 - 1) * dpToPx(context, 200)
                val delay = random.nextInt(500).toLong()
                
                val fadeIn = ObjectAnimator.ofFloat(chip, "alpha", 0f, 1f)
                fadeIn.startDelay = 500 + delay
                fadeIn.duration = 300
                
                val moveX = ObjectAnimator.ofFloat(chip, "translationX", 0f, targetX)
                moveX.startDelay = 500 + delay
                moveX.duration = 1000
                moveX.interpolator = DecelerateInterpolator()
                
                val moveY = ObjectAnimator.ofFloat(chip, "translationY", dpToPx(context, 100).toFloat(), targetY)
                moveY.startDelay = 500 + delay
                moveY.duration = 1000
                moveY.interpolator = DecelerateInterpolator()
                
                val rotate = ObjectAnimator.ofFloat(chip, "rotation", 0f, random.nextInt(360).toFloat())
                rotate.startDelay = 500 + delay
                rotate.duration = 1000
                
                chipAnimators.add(fadeIn)
                chipAnimators.add(moveX)
                chipAnimators.add(moveY)
                chipAnimators.add(rotate)
            }
            
            // 모든 애니메이션 결합
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(backgroundFadeIn, cardFadeIn, cardScaleX, cardScaleY, *chipAnimators.toTypedArray())
            
            // 애니메이션 종료 후 정리
            animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f)
                        fadeOut.duration = 500
                        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                rootView.removeView(container)
                            }
                        })
                        fadeOut.start()
                    }, 2000) // 2초 후 사라짐
                }
            })
            
            animatorSet.start()
        }
        
        /**
         * 패배 애니메이션을 표시합니다.
         * @param rootView 애니메이션을 표시할 루트 뷰
         * @param message 패배 메시지
         * @param amount 잃은 금액 (포맷팅된 문자열)
         */
        fun showLoseAnimation(rootView: ViewGroup, message: String, amount: String) {
            val context = rootView.context
            
            // 애니메이션 컨테이너 생성
            val container = FrameLayout(context)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            container.layoutParams = layoutParams
            
            // 반투명 배경 생성
            val backgroundView = View(context)
            backgroundView.layoutParams = layoutParams
            backgroundView.setBackgroundColor(Color.argb(150, 0, 0, 0))
            backgroundView.alpha = 0f
            container.addView(backgroundView)
            
            // 패배 카드 생성
            val cardView = CardView(context)
            val cardParams = FrameLayout.LayoutParams(
                dpToPx(context, 300),
                dpToPx(context, 200)
            )
            cardParams.gravity = Gravity.CENTER
            cardView.layoutParams = cardParams
            cardView.radius = dpToPx(context, 16).toFloat()
            cardView.setCardBackgroundColor(Color.parseColor("#A9A9A9")) // 회색
            cardView.alpha = 0f
            cardView.scaleX = 0.5f
            cardView.scaleY = 0.5f
            
            // 카드 내용 컨테이너
            val contentLayout = FrameLayout(context)
            contentLayout.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // 패배 메시지 텍스트
            val titleText = TextView(context)
            val titleParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            titleParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            titleParams.topMargin = dpToPx(context, 24)
            titleText.layoutParams = titleParams
            titleText.text = "😢 패배 😢"
            titleText.setTextColor(Color.WHITE)
            titleText.textSize = 24f
            titleText.typeface = Typeface.DEFAULT_BOLD
            contentLayout.addView(titleText)
            
            // 손실 금액 텍스트
            val amountText = TextView(context)
            val amountParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            amountParams.gravity = Gravity.CENTER
            amountText.layoutParams = amountParams
            amountText.text = amount
            amountText.setTextColor(Color.WHITE)
            amountText.textSize = 28f
            amountText.typeface = Typeface.DEFAULT_BOLD
            contentLayout.addView(amountText)
            
            // 상세 메시지 텍스트
            val detailText = TextView(context)
            val detailParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            detailParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            detailParams.bottomMargin = dpToPx(context, 24)
            detailText.layoutParams = detailParams
            detailText.text = message
            detailText.setTextColor(Color.WHITE)
            detailText.textSize = 16f
            contentLayout.addView(detailText)
            
            cardView.addView(contentLayout)
            container.addView(cardView)
            
            // 빈 칩 이미지 (떨어지는 형태)
            val chipCount = 5
            val chips = Array(chipCount) { 
                val chip = ImageView(context)
                val chipSize = dpToPx(context, 40)
                val chipParams = FrameLayout.LayoutParams(chipSize, chipSize)
                chipParams.gravity = Gravity.CENTER
                chipParams.topMargin = -dpToPx(context, 200)
                chip.layoutParams = chipParams
                chip.setImageResource(R.drawable.ic_casino_chip) // 카지노 칩 이미지 리소스
                chip.setColorFilter(Color.GRAY) // 회색
                chip.alpha = 0f
                container.addView(chip)
                chip
            }
            
            // 루트 뷰에 컨테이너 추가
            rootView.addView(container)
            
            // 애니메이션 시작
            // 1. 배경 페이드 인
            val backgroundFadeIn = ObjectAnimator.ofFloat(backgroundView, "alpha", 0f, 1f)
            backgroundFadeIn.duration = 300
            
            // 2. 카드 애니메이션
            val cardFadeIn = ObjectAnimator.ofFloat(cardView, "alpha", 0f, 1f)
            cardFadeIn.duration = 500
            
            val cardScaleX = ObjectAnimator.ofFloat(cardView, "scaleX", 0.5f, 1.1f, 1f)
            cardScaleX.duration = 500
            cardScaleX.interpolator = OvershootInterpolator(1.5f)
            
            val cardScaleY = ObjectAnimator.ofFloat(cardView, "scaleY", 0.5f, 1.1f, 1f)
            cardScaleY.duration = 500
            cardScaleY.interpolator = OvershootInterpolator(1.5f)
            
            // 3. 떨어지는 칩 애니메이션
            val chipAnimators = ArrayList<ObjectAnimator>()
            val random = java.util.Random()
            chips.forEach { chip ->
                val delay = random.nextInt(300).toLong()
                val startX = (random.nextFloat() * 2 - 1) * dpToPx(context, 50)
                
                val fadeIn = ObjectAnimator.ofFloat(chip, "alpha", 0f, 1f)
                fadeIn.startDelay = 500 + delay
                fadeIn.duration = 100
                
                val moveX = ObjectAnimator.ofFloat(chip, "translationX", startX)
                moveX.startDelay = 500 + delay
                moveX.duration = 1
                
                val moveY = ObjectAnimator.ofFloat(chip, "translationY", -dpToPx(context, 200).toFloat(), dpToPx(context, 300).toFloat())
                moveY.startDelay = 500 + delay
                moveY.duration = 1000
                moveY.interpolator = AccelerateInterpolator(1.5f)
                
                val rotate = ObjectAnimator.ofFloat(chip, "rotation", 0f, random.nextInt(360).toFloat())
                rotate.startDelay = 500 + delay
                rotate.duration = 1000
                
                chipAnimators.add(fadeIn)
                chipAnimators.add(moveX)
                chipAnimators.add(moveY)
                chipAnimators.add(rotate)
            }
            
            // 모든 애니메이션 결합
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(backgroundFadeIn, cardFadeIn, cardScaleX, cardScaleY, *chipAnimators.toTypedArray())
            
            // 애니메이션 종료 후 정리
            animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f)
                        fadeOut.duration = 500
                        fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                rootView.removeView(container)
                            }
                        })
                        fadeOut.start()
                    }, 2000) // 2초 후 사라짐
                }
            })
            
            animatorSet.start()
        }
        
        /**
         * DP 값을 픽셀로 변환합니다.
         */
        private fun dpToPx(context: Context, dp: Int): Int {
            val density = context.resources.displayMetrics.density
            return (dp * density).toInt()
        }
    }
} 