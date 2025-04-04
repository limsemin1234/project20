package com.example.p20

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ExplanationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_explanation, container, false)

        val explanationTextView = view.findViewById<TextView>(R.id.explanationText)

        // --- 추가: 텍스트뷰 페이드인 애니메이션 ---
        explanationTextView.alpha = 0f // 시작 시 투명하게
        val fadeIn = ObjectAnimator.ofFloat(explanationTextView, "alpha", 0f, 1f)
        fadeIn.duration = 3000 // 2초 -> 3초로 변경하여 더 천천히 나오게
        fadeIn.start()
        // --- 추가 끝 ---

        // 프래그먼트 영역(배경 포함) 클릭 시 자신을 제거
        view.setOnClickListener {
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
        
        return view
    }
} 