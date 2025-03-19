package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer

class AlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var assetViewModel: AssetViewModel // AssetViewModel 추가
    private lateinit var earnText: TextView
    private lateinit var levelText: TextView  // 레벨 텍스트 추가
    private lateinit var cooldownText: TextView  // 쿨다운 남은 시간 텍스트 추가

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alba_layout, container, false)

        // ViewModel 초기화
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        // TextView와 ImageView 객체 가져오기
        val albaImage: ImageView = view.findViewById(R.id.albaImage)
        earnText = view.findViewById(R.id.earnText)
        levelText = view.findViewById(R.id.levelText)
        cooldownText = view.findViewById(R.id.cooldownText)  // 쿨다운 남은 시간 텍스트

        // 초기 화면 설정
        earnText.text = "터치!! 터치!!"

        // 🔥 터치 이벤트 추가
        albaImage.setOnClickListener {
            if (albaViewModel.isCooldown.value == false) {
                albaViewModel.increaseTouchCount()

                // 🔥 알바 레벨에 따른 보상 추가
                val rewardAmount = albaViewModel.getRewardAmount()
                assetViewModel.increaseAsset(rewardAmount)

                // UI 업데이트
                earnText.text = "+$rewardAmount 원 획득!"
            }
        }

// 🔥 쿨다운 상태 업데이트
        albaViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { time ->
            if (albaViewModel.isCooldown.value == true && time > 0) {
                cooldownText.text = "쿨다운: ${time}초"
            } else {
                cooldownText.text = "알바 가능!"
            }
        })

        // 🔥 알바 레벨 및 보상 금액 업데이트 (UI 변경)
        albaViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            val rewardAmount = albaViewModel.getRewardAmount()
            levelText.text = "레벨: $level\n보상: $rewardAmount 원"
        })

        return view
    }
}
