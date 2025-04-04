package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class RealInfoFragment : Fragment() {
    private lateinit var viewModel: TimeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_real_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TextView 참조
        val remainingTimeText = view.findViewById<TextView>(R.id.remainingTimeText)

        // ViewModel 초기화
        viewModel = ViewModelProvider(requireActivity())[TimeViewModel::class.java]

        // 남은 시간 업데이트
        viewModel.remainingTime.observe(viewLifecycleOwner) { remainingSeconds ->
            remainingTimeText.text = "⏰ ${remainingSeconds}초"
            
            // 10초 이하일 때 빨간색으로 깜빡이게
            if (remainingSeconds <= 10) {
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 500
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                remainingTimeText.startAnimation(anim)
            } else {
                remainingTimeText.clearAnimation()
            }
        }

        // 게임 오버 처리
        viewModel.isGameOver.observe(viewLifecycleOwner) { isGameOver ->
            if (isGameOver) {
                // 애니메이션 중지
                remainingTimeText.clearAnimation()
                
                // 게임 오버 다이얼로그 표시
                AlertDialog.Builder(requireContext())
                    .setTitle("게임 오버!")
                    .setMessage("시간이 모두 소진되었습니다.\n최종 자산: ${formatNumber(viewModel.asset.value ?: 0)}원")
                    .setPositiveButton("다시 시작") { _, _ ->
                        viewModel.resetTimer()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startTimer() // 화면이 보일 때 타이머 시작
    }

    private fun formatNumber(number: Long): String {
        return String.format("%,d", number)
    }
} 