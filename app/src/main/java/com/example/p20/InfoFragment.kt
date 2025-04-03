package com.example.p20

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentInfoBinding
import androidx.lifecycle.Observer
import android.widget.Button

class InfoFragment : Fragment() {

    private lateinit var assetViewModel: AssetViewModel
    private lateinit var stockViewModel: StockViewModel
    private lateinit var albaViewModel: AlbaViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private lateinit var timeViewModel: TimeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentInfoBinding.inflate(inflater, container, false)

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)
        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        timeViewModel = ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)

        // 시간 초기화 버튼
        binding.resetTimeButton.setOnClickListener {
            timeViewModel.resetTimer()
            Toast.makeText(requireContext(), "시간이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 자산 초기화 버튼
        binding.resetAssetButton.setOnClickListener {
            assetViewModel.resetAssets()
            stockViewModel.resetStocks()
            albaViewModel.resetAlba()
            realEstateViewModel.resetRealEstatePrices()
            Toast.makeText(requireContext(), "초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 주식 가격 초기화 버튼
        binding.resetStockButton.setOnClickListener {
            stockViewModel.resetStockPrices()
            Toast.makeText(requireContext(), "주식 가격이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 알바 초기화 버튼
        binding.resetAlbaButton.setOnClickListener {
            albaViewModel.resetAlba()
            Toast.makeText(requireContext(), "알바가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 부동산 가격 초기화
        binding.resetRealEstateButton.setOnClickListener {
            realEstateViewModel.resetRealEstatePrices()
            Toast.makeText(requireContext(), "부동산 가격이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
}
