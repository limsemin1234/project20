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
    private var _binding: FragmentInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)


        // 자산 초기화 버튼 클릭 리스너
        binding.resetAssetButton.setOnClickListener {
            assetViewModel.resetAsset()
            Toast.makeText(requireContext(), "자산이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 🔥 "주식 가격 초기화" 버튼 클릭 리스너 추가!
        binding.resetStockButton.setOnClickListener {
            stockViewModel.resetStockPrices()
            Toast.makeText(requireContext(), "주식 가격이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}
