package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer

class AlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var assetViewModel: AssetViewModel // AssetViewModel 추가
    private lateinit var earnText: TextView
    private lateinit var levelText: TextView  // 레벨 텍스트 추가

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alba_layout, container, false)

        // ViewModel 초기화
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        // AssetViewModel 초기화
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        // TextView와 ImageView 객체 가져오기
        val albaImage: ImageView = view.findViewById(R.id.albaImage)
        earnText = view.findViewById(R.id.earnText)
        levelText = view.findViewById(R.id.levelText)  // 레벨 텍스트 가져오기


        // 초기 화면은 "터치!! 터치!!"로 설정
        earnText.text = "터치!! 터치!!"


        // 터치 이벤트 추가
        albaImage.setOnClickListener {
            if (albaViewModel.isCooldown.value == false) {
                albaViewModel.increaseTouchCount()

                // 알바 레벨에 따른 보상 금액을 증가시킴
                val rewardAmount = albaViewModel.getRewardAmount()
                assetViewModel.increaseAsset(rewardAmount)  // 보상 금액 증가

            } else {
                // 쿨다운 상태일 때만 문구 업데이트
                if (albaViewModel.cooldownTime.value ?: 0 > 0) {
                    earnText.text = "${albaViewModel.cooldownTime.value} 초 후 다시 가능합니다."
                }
            }
        }


        // 쿨다운 상태 및 남은 시간 감시하여 UI 변경
        albaViewModel.isCooldown.observe(viewLifecycleOwner, Observer { isCooling ->
            if (isCooling) {
                earnText.text = "30초 대기 중..."
            } else {
                earnText.text = "터치!! 터치!!"
            }
        })

        // 남은 시간 감시하여 UI 변경
        albaViewModel.cooldownTime.observe(viewLifecycleOwner, Observer { time ->
            if (time > 0) {
                earnText.text = "$time 초 후 다시 가능합니다."
            } else {
                earnText.text = "터치!! 터치!!"
            }
        })

        // 알바 레벨 및 보상 금액 업데이트 (UI 변경)
        albaViewModel.albaLevel.observe(viewLifecycleOwner, Observer { level ->
            // 레벨을 레벨 텍스트에만 표시
            val rewardAmount = albaViewModel.getRewardAmount()  // 레벨에 따른 보상 금액 계산
            levelText.text = "레벨: $level\n보상: $rewardAmount 원"  // 레벨과 보상 금액을 함께 표시
        })

        return view
    }
}







