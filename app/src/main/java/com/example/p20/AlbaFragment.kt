package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer

class AlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var earnText: TextView
    private lateinit var levelText: TextView
    private lateinit var cooldownText: TextView
    private lateinit var animationContainer: FrameLayout
    private lateinit var albaImage: ImageView
    private lateinit var activePhaseText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_alba, container, false)

        albaViewModel = ViewModelProvider(requireActivity())[AlbaViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

        albaImage = view.findViewById(R.id.albaImage)
        earnText = view.findViewById(R.id.earnText)
        levelText = view.findViewById(R.id.levelText)
        cooldownText = view.findViewById(R.id.cooldownText)
        animationContainer = view.findViewById(R.id.animationContainer)
        activePhaseText = view.findViewById(R.id.cooldownText)

        earnText.text = "알바 시작!"

        albaImage.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (albaViewModel.isCooldown.value == false && albaViewModel.isActivePhase.value == false) {
                    albaViewModel.startActivePhase()
                    val rewardAmount = albaViewModel.getRewardAmount().toLong()
                    assetViewModel.increaseAsset(rewardAmount)
                    val location = IntArray(2)
                    albaImage.getLocationOnScreen(location)
                    showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
                } else if (albaViewModel.isActivePhase.value == true) {
                    val rewardAmount = albaViewModel.getRewardAmount().toLong()
                    assetViewModel.increaseAsset(rewardAmount)
                    val location = IntArray(2)
                    albaImage.getLocationOnScreen(location)
                    showRewardAnimation(event.rawX.toInt() - location[0], event.rawY.toInt() - location[1], rewardAmount)
                }
            }
            true
        }

        albaViewModel.isCooldown.observe(viewLifecycleOwner, Observer { isCoolingDown ->
            updateStatusText()
        })
        albaViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { time ->
            updateStatusText()
        })
        albaViewModel.isActivePhase.observe(viewLifecycleOwner, Observer { isActive ->
            updateStatusText()
        })
        albaViewModel.activePhaseTime.observe(viewLifecycleOwner, Observer { time ->
            updateStatusText()
        })

        albaViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val rewardAmount = albaViewModel.getRewardAmount()
            levelText.text = "레벨: $level\n보상: ${"%,d".format(rewardAmount)}원"
        })

        return view
    }

    private fun updateStatusText() {
        val isActive = albaViewModel.isActivePhase.value ?: false
        val isCooldown = albaViewModel.isCooldown.value ?: false
        val activeTime = albaViewModel.activePhaseTime.value ?: 0
        val cooldownTime = albaViewModel.cooldownTime.value ?: 0

        activePhaseText.text = when {
            isActive -> "클릭! 남은 시간: ${activeTime}초"
            isCooldown -> "쿨다운: ${cooldownTime}초"
            else -> "알바 가능!"
        }
    }

    private fun showRewardAnimation(
        x: Int,
        y: Int,
        reward: Long,
        shakeAngle: Float = 20f,
        shakeSpeed: Long = 100L,
        moveDuration: Long = 1500L,
        scaleTarget: Float = 0.5f
    ) {
        val rewardTextView = TextView(requireContext()).apply {
            text = "+${"%,d".format(reward)}원"
            textSize = 20f
            setTextColor(resources.getColor(R.color.reward_text, null))
            setShadowLayer(5f, 1f, 1f, android.graphics.Color.BLACK)
            setPadding(10, 10, 10, 10)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        animationContainer.addView(rewardTextView)

        rewardTextView.x = albaImage.x + x
        rewardTextView.y = albaImage.y + y

        val targetLocation = IntArray(2)
        levelText.getLocationOnScreen(targetLocation)

        val containerLocation = IntArray(2)
        animationContainer.getLocationOnScreen(containerLocation)

        val targetX = targetLocation[0] - containerLocation[0] + levelText.width / 2 - rewardTextView.width / 2
        val targetY = targetLocation[1] - containerLocation[1] + levelText.height / 2 - rewardTextView.height / 2

        val moveX = ObjectAnimator.ofFloat(rewardTextView, "x", rewardTextView.x, targetX.toFloat()).apply {
            duration = moveDuration
        }
        val moveY = ObjectAnimator.ofFloat(rewardTextView, "y", rewardTextView.y, targetY.toFloat()).apply {
            duration = moveDuration
        }
        val fadeOut = ObjectAnimator.ofFloat(rewardTextView, "alpha", 1f, 0f).apply {
            duration = moveDuration
        }
        val scaleX = ObjectAnimator.ofFloat(rewardTextView, "scaleX", 1f, scaleTarget).apply {
            duration = moveDuration
        }
        val scaleY = ObjectAnimator.ofFloat(rewardTextView, "scaleY", 1f, scaleTarget).apply {
            duration = moveDuration
        }

        val shake = ObjectAnimator.ofFloat(rewardTextView, "rotation", 0f, shakeAngle, -shakeAngle, shakeAngle / 1.5f, -shakeAngle / 1.5f, 0f).apply {
            this.duration = shakeSpeed
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                shake.cancel()
                animationContainer.removeView(rewardTextView)
            }
        })

        moveX.start()
        moveY.start()
        fadeOut.start()
        scaleX.start()
        scaleY.start()
        shake.start()
    }
}
