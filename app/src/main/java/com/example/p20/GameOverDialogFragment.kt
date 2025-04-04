package com.example.p20

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import java.text.DecimalFormat

class GameOverDialogFragment : DialogFragment() {

    private lateinit var timeViewModel: TimeViewModel
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var stockViewModel: StockViewModel
    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 다이얼로그의 기본 타이틀 제거
        dialog?.window?.requestFeature(android.view.Window.FEATURE_NO_TITLE)
        // 다이얼로그 밖 터치 시 닫히지 않도록 설정
        isCancelable = false

        val view = inflater.inflate(R.layout.dialog_game_over, container, false)

        // ViewModel 초기화
        timeViewModel = ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        val timingAlbaViewModel = ViewModelProvider(requireActivity()).get(TimingAlbaViewModel::class.java)

        // 뷰 참조
        val finalAssetText = view.findViewById<TextView>(R.id.dialogFinalAssetText)
        val restartMessageText = view.findViewById<TextView>(R.id.dialogRestartMessageText)
        val restartButton = view.findViewById<Button>(R.id.dialogRestartButton)
        val exitButton = view.findViewById<Button>(R.id.dialogExitButton)

        // 최종 자산 표시
        finalAssetText.text = "최종 자산: ${formatNumber(assetViewModel.asset.value ?: 0)}원"

        // 다시 시작 버튼 리스너
        restartButton.setOnClickListener {
            // 버튼 비활성화 및 즉시 숨김
            restartButton.isEnabled = false
            restartButton.visibility = View.INVISIBLE
            exitButton.isEnabled = false
            exitButton.visibility = View.INVISIBLE

            // 기존 내용 숨기기 (INVISIBLE 사용)
            view.findViewById<TextView>(R.id.gameOverTitleText).visibility = View.INVISIBLE // 타이틀 숨김
            finalAssetText.visibility = View.INVISIBLE // 최종 자산 숨김

            // 메시지 표시 및 깜빡임 애니메이션
            restartMessageText.visibility = View.VISIBLE
            val blinkAnimation = AlphaAnimation(1.0f, 0.0f).apply {
                duration = 500
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            restartMessageText.startAnimation(blinkAnimation)

            // 3초 후 실행 (멤버 핸들러 사용)
            handler.postDelayed({
                // MainActivity에 다시 시작 요청 (리셋 및 ExplanationFragment 표시 트리거)
                timeViewModel.requestRestart()
                // 다이얼로그 닫기
                dismiss()
            }, 3000)
        }

        // 나가기 버튼 리스너
        exitButton.setOnClickListener {
            // 종료 전 데이터 리셋
            timeViewModel.resetTimer()
            assetViewModel.resetAssets()
            stockViewModel.resetStocks()
            albaViewModel.resetAlba()
            timingAlbaViewModel.resetTimingAlba()
            realEstateViewModel.resetRealEstatePrices()
            // 앱 종료
            requireActivity().finishAffinity()
        }

        return view
    }
    
    // 숫자 포맷팅 함수
    private fun formatNumber(number: Long): String {
        val formatter = DecimalFormat("#,###")
        return formatter.format(number)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 예약된 콜백 제거하여 메모리 누수 방지
        handler.removeCallbacksAndMessages(null)
    }

    override fun onStart() {
        super.onStart()
        // --- 수정: 다이얼로그 크기를 화면 전체로 설정 ---
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        // --- 수정 끝 ---
        // 배경 투명하게 설정 (선택 사항 - 레이아웃 배경 사용)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
} 