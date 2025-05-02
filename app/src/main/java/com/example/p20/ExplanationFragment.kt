package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * 게임 설명을 표시하는 Fragment
 * 사용자에게 게임 규칙과 조작법을 보여주며, 화면을 터치하면 게임을 시작합니다.
 * 설명을 읽는 동안에는 게임 타이머가 일시 정지됩니다.
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

        val explanationTextView = view.findViewById<TextView>(R.id.explanationText)

        // 텍스트 페이드인 애니메이션 적용
        applyAnimation(explanationTextView, "fade_in", 2000)

        // 프래그먼트 영역(배경 포함) 클릭 시 자신을 제거하고 타이머 시작
        view.setOnClickListener {
            // 클릭 애니메이션 효과 추가
            applyAnimation(explanationTextView, "fade_out", 500)
            
            // 딜레이 후 타이머 시작 및 프래그먼트 제거
            postDelayed(400) {
                // 타이머 다시 시작
                timeViewModel.startTimer()
                
                // 메인 액티비티에서 메시지를 표시하도록 함
                (activity as? MainActivity)?.showDragTimeViewMessage()
                
                // 프래그먼트 제거
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        }
        
        return view
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 프래그먼트가 다른 방법으로 제거될 경우에도 타이머 시작 보장
        // startTimer 메서드는 내부적으로 타이머가 이미 실행 중인지 확인하므로 그냥 호출해도 안전함
        timeViewModel.startTimer()
    }
} 