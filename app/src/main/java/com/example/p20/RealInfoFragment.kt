package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        val assetTextView = view.findViewById<TextView>(R.id.assetTextView)
        val stockTextView = view.findViewById<TextView>(R.id.stockTextView)
        val albaTextView = view.findViewById<TextView>(R.id.albaTextView)
        val realEstateTextView = view.findViewById<TextView>(R.id.realEstateTextView)

        // ViewModel 초기화
        viewModel = ViewModelProvider(requireActivity())[TimeViewModel::class.java]

        // 자산 정보 업데이트
        viewModel.asset.observe(viewLifecycleOwner) { asset ->
            assetTextView.text = "현재 자산: ${formatNumber(asset)}원"
        }

        // 주식 정보 업데이트
        viewModel.stockInfo.observe(viewLifecycleOwner) { stockInfo ->
            val stockText = StringBuilder()
            stockText.append("보유 주식 정보:\n")
            for ((stockName, quantity) in stockInfo) {
                if (quantity > 0) {
                    stockText.append("$stockName: $quantity 주\n")
                }
            }
            stockTextView.text = stockText.toString()
        }

        // 알바 정보 업데이트
        viewModel.albaInfo.observe(viewLifecycleOwner) { albaInfo ->
            val albaText = StringBuilder()
            albaText.append("알바 정보:\n")
            for ((albaName, count) in albaInfo) {
                if (count > 0) {
                    albaText.append("$albaName: $count 명\n")
                }
            }
            albaTextView.text = albaText.toString()
        }

        // 부동산 정보 업데이트
        viewModel.realEstateInfo.observe(viewLifecycleOwner) { realEstateInfo ->
            val realEstateText = StringBuilder()
            realEstateText.append("보유 부동산 정보:\n")
            for ((propertyName, owned) in realEstateInfo) {
                if (owned) {
                    realEstateText.append("$propertyName\n")
                }
            }
            realEstateTextView.text = realEstateText.toString()
        }
    }

    private fun formatNumber(number: Long): String {
        return String.format("%,d", number)
    }
} 