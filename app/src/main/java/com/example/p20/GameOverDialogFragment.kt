package com.example.p20

import android.app.Dialog
import android.content.Context
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
import androidx.fragment.app.FragmentManager
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

        // 게임 오버 시 배경 음악 멈추기
        (activity as? MainActivity)?.stopBackgroundMusicForGameOver()

        val view = inflater.inflate(R.layout.dialog_game_over, container, false)

        // ViewModel 초기화
        timeViewModel = ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)

        // 뷰 참조
        val finalAssetText = view.findViewById<TextView>(R.id.dialogFinalAssetText)
        val restartMessageText = view.findViewById<TextView>(R.id.dialogRestartMessageText)
        val restartButton = view.findViewById<Button>(R.id.dialogRestartButton)
        val exitButton = view.findViewById<Button>(R.id.dialogExitButton)

        // 총자산 계산 (현재 자산 + 주식 자산 + 부동산 자산)
        val currentAsset = assetViewModel.asset.value ?: 0
        var stockAsset = 0L
        var realEstateAsset = 0L
        
        // 주식 자산 계산
        stockViewModel.stockItems.value?.forEach { stock ->
            if (stock.holding > 0) {
                stockAsset += stock.price.toLong() * stock.holding
            }
        }
        
        // 부동산 자산 계산
        realEstateViewModel.realEstateList.value?.forEach { estate ->
            if (estate.owned) {
                realEstateAsset += estate.price
            }
        }
        
        // 총자산 합계
        val totalAsset = currentAsset + stockAsset + realEstateAsset
        
        // 최종 자산 표시 (총자산으로 변경)
        finalAssetText.text = "최종 총자산: ${formatNumber(totalAsset)}원\n" +
                              "현금 자산: ${formatNumber(currentAsset)}원\n" +
                              "주식 자산: ${formatNumber(stockAsset)}원\n" +
                              "부동산 자산: ${formatNumber(realEstateAsset)}원"

        // 다시 시작 버튼 리스너
        restartButton.setOnClickListener {
            // 컨텐츠 컨테이너 숨기기
            val contentContainer = view.findViewById<View>(R.id.contentContainer)
            contentContainer?.visibility = View.GONE
            
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
                // 프래그먼트가 액티비티에 연결되어 있는지 확인
                if (!isAdded) {
                    android.util.Log.d("GameOverDialog", "프래그먼트가 더 이상 액티비티에 연결되어 있지 않음")
                    return@postDelayed
                }
            
                try {
                    // MainActivity에 다시 시작 요청 (리셋 및 ExplanationFragment 표시 트리거)
                    resetGame()
                    
                    // 리스너를 통해 ExplanationFragment가 표시될 때 배경음악을 재생하도록 설정
                    val mainActivity = activity as? MainActivity
                    mainActivity?.let { activity ->
                        // ExplanationFragment 표시 감지를 위한 FragmentManager 리스너 등록
                        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                                override fun onFragmentStarted(fm: androidx.fragment.app.FragmentManager, f: androidx.fragment.app.Fragment) {
                                    super.onFragmentStarted(fm, f)
                                    if (f is ExplanationFragment) {
                                        // ExplanationFragment가 시작되면 배경음악 재생
                                        val prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                                        val soundEnabled = prefs.getBoolean("sound_enabled", true)
                                        
                                        android.util.Log.d("GameOverDialog", "ExplanationFragment 시작: 배경음악 설정=$soundEnabled")
                                        
                                        if (soundEnabled) {
                                            activity.restartBackgroundMusic()
                                        }
                                        
                                        // 한 번만 실행하도록 리스너 제거
                                        activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(this)
                                    }
                                }
                            }, false
                        )
                    }
                    
                    // 게임 재시작 요청
                    timeViewModel.requestRestart()
                    
                    // 다이얼로그 닫기
                    if (dialog?.isShowing == true && !requireActivity().isFinishing) {
                        dismiss()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GameOverDialog", "다시 시작 중 오류 발생: ${e.message}")
                }
            }, 3000)
        }

        // 나가기 버튼 리스너
        exitButton.setOnClickListener {
            // 종료 전 데이터 리셋
            resetGame()
            
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

    // 게임 리셋 함수
    private fun resetGame() {
        // 시간 초기화
        timeViewModel.resetTimer()
        
        // 자산 초기화
        assetViewModel.resetAssets()
        
        // 주식 종목 새로 생성 및 초기화
        stockViewModel.resetStocksWithNewCompanies()
        
        // 알바 초기화
        albaViewModel.resetAlba()
        
        // 부동산 초기화
        realEstateViewModel.resetRealEstatePrices()
        
        // 아이템 초기화
        resetItems()
        
        // 해킹 알바 게임 초기화
        resetHackingAlba()
    }
    
    // 아이템 초기화 함수
    private fun resetItems() {
        val itemPrefs = requireContext().getSharedPreferences("item_prefs", Context.MODE_PRIVATE)
        val itemEditor = itemPrefs.edit()
        
        // 기존 데이터 모두 삭제
        itemEditor.clear()
        
        // 초기 아이템 재고 설정: 각 아이템 재고는 1, 보유량은 0으로 설정
        for (itemId in 1..3) {
            // 아이템 재고 = 1
            itemEditor.putInt("item_stock_$itemId", 1)
            // 아이템 보유량 = 0
            itemEditor.putInt("item_quantity_$itemId", 0)
        }
        
        // 아이템 초기화 완료 표시
        itemEditor.putBoolean("has_initialized_stocks", true)
        itemEditor.apply()
    }

    // 해킹 알바 초기화 함수
    private fun resetHackingAlba() {
        val hackingPrefs = requireContext().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        val hackingEditor = hackingPrefs.edit()
        
        // 해킹 알바 데이터 초기화
        hackingEditor.clear()
        
        // 초기값 설정
        hackingEditor.putBoolean("last_game_result_exists", false)
        hackingEditor.putInt("last_game_level", 1)
        
        hackingEditor.apply()
        
        android.util.Log.d("GameOverDialog", "해킹 알바 데이터 초기화 완료")
    }
} 