package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentResetBinding // 새 바인딩 클래스 사용
import com.google.android.material.snackbar.Snackbar // Snackbar import 추가
import android.graphics.Color // Color 사용 위해 추가
import android.view.Gravity // Gravity 사용 위해 추가
import android.widget.FrameLayout // FrameLayout 사용 위해 추가

// --- 수정: 클래스 이름 변경 InfoFragment -> ResetFragment ---
class ResetFragment : Fragment() {
// --- 수정 끝 ---

    private lateinit var assetViewModel: AssetViewModel
    private lateinit var stockViewModel: StockViewModel
    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private lateinit var timeViewModel: TimeViewModel

    private var _binding: FragmentResetBinding? = null // 바인딩 변수 타입 변경
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // --- 수정: View Binding 사용 및 레이아웃 참조 확인 ---
        _binding = FragmentResetBinding.inflate(inflater, container, false) // 새 바인딩 클래스 사용
        // val view = inflater.inflate(R.layout.fragment_reset, container, false) // 이전 inflate 제거
        // --- 수정 끝 ---

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        timeViewModel = ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)

        // 모든 초기화 버튼 비활성화
        binding.resetTimeButton.isEnabled = true // 시간 초기화만 활성화
        binding.resetAssetButton.isEnabled = false
        binding.resetStockButton.isEnabled = false
        binding.resetAlbaButton.isEnabled = false
        binding.resetRealEstateButton.isEnabled = false
        binding.testResetTime10sButton.isEnabled = true // 테스트 시간 초기화 버튼 활성화
        
        // 비활성화 상태를 시각적으로 표시
        binding.resetTimeButton.alpha = 1.0f // 시간 초기화는 완전 불투명(활성화 상태)
        binding.resetAssetButton.alpha = 0.5f
        binding.resetStockButton.alpha = 0.5f
        binding.resetAlbaButton.alpha = 0.5f
        binding.resetRealEstateButton.alpha = 0.5f
        binding.testResetTime10sButton.alpha = 1.0f // 테스트 시간 초기화 버튼 완전 불투명(활성화 상태)

        // --- 수정: View Binding 사용하도록 버튼 참조 변경 ---
        // 시간 초기화 버튼
        binding.resetTimeButton.setOnClickListener { // binding. 사용
            timeViewModel.resetTimer()
            showCustomSnackbar("시간이 초기화되었습니다.")
        }

        // 자산 초기화 버튼
        binding.resetAssetButton.setOnClickListener { // binding. 사용
            assetViewModel.resetAssets()
            stockViewModel.resetStocksWithNewCompanies()
            albaViewModel.resetAlba()
            realEstateViewModel.resetRealEstatePrices()
            showCustomSnackbar("초기화되었습니다.")
        }

        // 주식 가격 초기화 버튼
        binding.resetStockButton.setOnClickListener { // binding. 사용
            stockViewModel.resetStocksWithNewCompanies()
            showCustomSnackbar("주식이 새로운 종목으로 변경되었습니다.")
        }

        // 알바 초기화 버튼
        binding.resetAlbaButton.setOnClickListener { // binding. 사용
            albaViewModel.resetAlba()
            showCustomSnackbar("알바가 초기화되었습니다.")
        }

        // 부동산 가격 초기화
        binding.resetRealEstateButton.setOnClickListener { // binding. 사용
            realEstateViewModel.resetRealEstatePrices()
            showCustomSnackbar("부동산 가격이 초기화되었습니다.")
        }

        // --- 추가: 테스트용 시간 초기화(10초) 버튼 리스너 ---
        binding.testResetTime10sButton.setOnClickListener {
            timeViewModel.setRemainingTime(10) // 남은 시간을 10초로 설정
            showCustomSnackbar("테스트: 남은 시간이 10초로 설정되었습니다.")
        }
        // --- 추가 끝 ---

        return binding.root // binding.root 반환
    }

    // --- 수정: 메시지 매니저를 사용하는 함수로 변경 ---
    private fun showCustomSnackbar(message: String) { // 함수 이름 변경
        MessageManager.showMessage(requireContext(), message)
    }
    // --- 수정 끝 ---

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
} 