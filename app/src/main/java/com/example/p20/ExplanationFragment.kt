package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * 게임 설명을 표시하는 Fragment
 */
class ExplanationFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explanation, container, false)
        
        // 설명 화면이 표시되면 타이머 중지
        timeViewModel.stopTimer()

        // 사용자에게 타이머가 일시정지되었음을 알림
        showMessage("게임 설명을 읽는 동안 시간이 멈춰있습니다. 화면을 터치하면 게임이 시작됩니다.")

        val explanationTextView = view.findViewById<TextView>(R.id.explanationText)

        // BaseFragment의 애니메이션 유틸리티 활용
        applyAnimation(explanationTextView, "fade_in", 3000)

        // 프래그먼트 영역(배경 포함) 클릭 시 자신을 제거하고 타이머 시작
        view.setOnClickListener {
            // 타이머 다시 시작
            timeViewModel.startTimer()
            
            // 메인 액티비티에서 메시지를 표시하도록 함
            (activity as? MainActivity)?.showDragTimeViewMessage()
            
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