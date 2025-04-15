package com.example.p20

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class ExplanationFragment : Fragment() {

    private lateinit var timeViewModel: TimeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explanation, container, false)

        // TimeViewModel 초기화
        timeViewModel = ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)
        
        // 설명 화면이 표시되면 타이머 중지
        timeViewModel.stopTimer()

        // 사용자에게 타이머가 일시정지되었음을 알림
        MessageManager.showMessage(requireContext(), "게임 설명을 읽는 동안 시간이 멈춰있습니다. 화면을 터치하면 게임이 시작됩니다.")

        val explanationTextView = view.findViewById<TextView>(R.id.explanationText)

        // --- 추가: 텍스트뷰 페이드인 애니메이션 ---
        explanationTextView.alpha = 0f // 시작 시 투명하게
        val fadeIn = ObjectAnimator.ofFloat(explanationTextView, "alpha", 0f, 1f)
        fadeIn.duration = 3000 // 2초 -> 3초로 변경하여 더 천천히 나오게
        fadeIn.start()
        // --- 추가 끝 ---

        // 프래그먼트 영역(배경 포함) 클릭 시 자신을 제거하고 타이머 시작
        view.setOnClickListener {
            // 타이머 다시 시작
            timeViewModel.startTimer()
            // 프래그먼트 제거
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
        
        return view
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 프래그먼트가 다른 방법으로 제거될 경우에도 타이머 시작 보장
        timeViewModel.startTimer()
    }
} 