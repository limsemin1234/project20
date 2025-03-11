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
    private lateinit var earnText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alba_layout, container, false)

        // ViewModel 초기화
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)

        // TextView와 ImageView 객체 가져오기
        val albaImage: ImageView = view.findViewById(R.id.albaImage)
        earnText = view.findViewById(R.id.earnText)


        // 터치 이벤트 추가
        albaImage.setOnClickListener {
            if (albaViewModel.isCooldown.value == false) {
                albaViewModel.increaseTouchCount()
                (activity as? MainActivity)?.increaseAsset(100)
            } else {
                // 쿨다운 중일 때는 UI에서 알리도록 수정
                earnText.text = "30초 후 다시 가능합니다."
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

        return view
    }
}







