package com.example.p20

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
import android.graphics.Typeface

class AlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var earnText: TextView
    private lateinit var levelText: TextView
    private lateinit var cooldownText: TextView
    private lateinit var animationContainer: FrameLayout
    private lateinit var albaImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.alba_layout, container, false)

        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        albaImage = view.findViewById(R.id.albaImage)
        earnText = view.findViewById(R.id.earnText)
        levelText = view.findViewById(R.id.levelText)
        cooldownText = view.findViewById(R.id.cooldownText)
        animationContainer = view.findViewById(R.id.animationContainer)

        earnText.text = "터치!! 터치!!"

        // 터치 이벤트 추가 (클릭한 위치에 보상 표시)
        albaImage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (albaViewModel.isCooldown.value == false) {
                    albaViewModel.increaseTouchCount()
                    val rewardAmount = albaViewModel.getRewardAmount()
                    assetViewModel.increaseAsset(rewardAmount)

                    // 클릭한 위치에 보상 표시
                    val location = IntArray(2)
                    albaImage.getLocationOnScreen(location)
                    val touchX = event.rawX - location[0]
                    val touchY = event.rawY - location[1]

                    showRewardAnimation(touchX.toInt(), touchY.toInt(), rewardAmount)
                }
            }
            true
        }

        albaViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { time ->
            cooldownText.text = if (albaViewModel.isCooldown.value == true && time > 0) {
                "쿨다운: ${time}초"
            } else {
                "알바 가능!"
            }
        })

        albaViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val rewardAmount = albaViewModel.getRewardAmount()
            levelText.text = "레벨: $level\n보상: $rewardAmount 원"
        })

        return view
    }

    // 클릭한 위치에 보상을 표시하는 함수
    private fun showRewardAnimation(x: Int, y: Int, reward: Int) {
        val rewardTextView = TextView(requireContext()).apply {
            text = "+${reward}원"
            textSize = 18f
            setTypeface(null, Typeface.BOLD) // 🔥 글씨 진하게 설정
            setTextColor(resources.getColor(R.color.reward_text, null))
            setShadowLayer(5f, 2f, 2f, android.graphics.Color.BLACK) // 🔥 그림자 효과

            setPadding(10, 10, 10, 10)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        animationContainer.addView(rewardTextView)

        // 위치 조정 (터치한 곳에서 애니메이션 시작)
        rewardTextView.x = albaImage.x + x
        rewardTextView.y = albaImage.y + y

        // 애니메이션 효과: 위로 떠오르면서 점점 사라지게
        val moveUp = ObjectAnimator.ofFloat(rewardTextView, "translationY", rewardTextView.y, rewardTextView.y - 100f)
        val fadeOut = ObjectAnimator.ofFloat(rewardTextView, "alpha", 1f, 0f)

        moveUp.duration = 1000
        fadeOut.duration = 1000

        moveUp.start()
        fadeOut.start()

        // 1초 후 TextView 제거
        Handler(Looper.getMainLooper()).postDelayed({
            animationContainer.removeView(rewardTextView)
        }, 1000)
    }
}
