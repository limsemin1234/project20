package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentResetBinding

/**
 * 게임 리셋 기능을 제공하는 Fragment
 * 시간, 자산, 주식, 알바, 부동산 등의 리셋 기능을 제공합니다.
 */
class ResetFragment : BaseFragment() {

    private lateinit var stockViewModel: StockViewModel
    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel

    private var _binding: FragmentResetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetBinding.inflate(inflater, container, false)

        // BaseFragment에서 제공되지 않는 ViewModel 초기화
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)

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

        // 시간 초기화 버튼
        binding.resetTimeButton.setOnClickListener {
            timeViewModel.resetTimer()
            showMessage("시간이 초기화되었습니다.")
        }

        // 자산 초기화 버튼
        binding.resetAssetButton.setOnClickListener {
            assetViewModel.resetAssets()
            stockViewModel.resetStocksWithNewCompanies()
            albaViewModel.resetAlba()
            realEstateViewModel.resetRealEstatePrices()
            showMessage("초기화되었습니다.")
        }

        // 주식 가격 초기화 버튼
        binding.resetStockButton.setOnClickListener {
            stockViewModel.resetStocksWithNewCompanies()
            showMessage("주식이 새로운 종목으로 변경되었습니다.")
        }

        // 알바 초기화 버튼
        binding.resetAlbaButton.setOnClickListener {
            albaViewModel.resetAlba()
            showMessage("알바가 초기화되었습니다.")
        }

        // 부동산 가격 초기화
        binding.resetRealEstateButton.setOnClickListener {
            realEstateViewModel.resetRealEstatePrices()
            showMessage("부동산 가격이 초기화되었습니다.")
        }

        // 테스트용 시간 초기화(10초) 버튼 리스너
        binding.testResetTime10sButton.setOnClickListener {
            timeViewModel.setRemainingTime(10) // 남은 시간을 10초로 설정
            showMessage("테스트: 남은 시간이 10초로 설정되었습니다.")
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
} 