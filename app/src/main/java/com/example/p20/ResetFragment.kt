package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentResetBinding // 새 바인딩 클래스 사용

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

        // --- 수정: View Binding 사용하도록 버튼 참조 변경 ---
        // 시간 초기화 버튼
        binding.resetTimeButton.setOnClickListener { // binding. 사용
            timeViewModel.resetTimer()
            Toast.makeText(requireContext(), "시간이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 자산 초기화 버튼
        binding.resetAssetButton.setOnClickListener { // binding. 사용
            assetViewModel.resetAssets()
            stockViewModel.resetStocks()
            albaViewModel.resetAlba()
            realEstateViewModel.resetRealEstatePrices()
            Toast.makeText(requireContext(), "초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 주식 가격 초기화 버튼
        binding.resetStockButton.setOnClickListener { // binding. 사용
            stockViewModel.resetStockPrices()
            Toast.makeText(requireContext(), "주식 가격이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 알바 초기화 버튼
        binding.resetAlbaButton.setOnClickListener { // binding. 사용
            albaViewModel.resetAlba()
            Toast.makeText(requireContext(), "알바가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 부동산 가격 초기화
        binding.resetRealEstateButton.setOnClickListener { // binding. 사용
            realEstateViewModel.resetRealEstatePrices()
            Toast.makeText(requireContext(), "부동산 가격이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // --- 추가: 테스트용 시간 초기화(10초) 버튼 리스너 ---
        binding.testResetTime10sButton.setOnClickListener {
            timeViewModel.setRemainingTime(10) // 남은 시간을 10초로 설정
            Toast.makeText(requireContext(), "테스트: 남은 시간이 10초로 설정되었습니다.", Toast.LENGTH_SHORT).show()
        }
        // --- 추가 끝 ---

        return binding.root // binding.root 반환
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
} 