package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentInfoBinding
import androidx.lifecycle.Observer

class InfoFragment : Fragment() {

    private lateinit var assetViewModel: AssetViewModel
    private lateinit var stockViewModel: StockViewModel
    private lateinit var albaViewModel: AlbaViewModel
    //private lateinit var resetAlbaButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentInfoBinding.inflate(inflater, container, false)

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)

        // 자산 초기화 버튼 클릭 시 자산을 초기화
        binding.resetAssetButton.setOnClickListener {
            assetViewModel.resetAsset()
            Toast.makeText(requireContext(), "자산이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 주식 가격 초기화 버튼 클릭 시 주식 초기화
        binding.resetStockButton.setOnClickListener {
            stockViewModel.resetStockPrices()
            Toast.makeText(requireContext(), "주식 가격이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 알바 초기화 버튼 클릭 시 알바 레벨과 보상 초기화
        binding.resetAlbaButton.setOnClickListener {
            albaViewModel.resetAlba()
            Toast.makeText(requireContext(), "알바가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
}
