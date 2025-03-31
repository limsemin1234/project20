package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class RealEstateFragment : Fragment() {

    private lateinit var realEstateRecyclerView: RecyclerView
    private lateinit var realEstateAdapter: RealEstateAdapter
    private lateinit var realEstateStatusText: TextView
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private var selectedEstate: RealEstate? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_estate, container, false)

        realEstateRecyclerView = view.findViewById(R.id.realEstateRecyclerView)
        realEstateStatusText = view.findViewById(R.id.realEstateStatusText)
        val buyButton: Button = view.findViewById(R.id.buyRealEstateButton)
        val sellButton: Button = view.findViewById(R.id.sellRealEstateButton)

        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        realEstateRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        realEstateAdapter = RealEstateAdapter(emptyList()) { estate ->
            selectedEstate = estate
            showEstateDetails(estate)
        }
        realEstateRecyclerView.adapter = realEstateAdapter

        realEstateViewModel.realEstateList.observe(viewLifecycleOwner) { updatedList ->
            realEstateAdapter.updateList(updatedList)
            selectedEstate?.let { showEstateDetails(it) }
        }

        assetViewModel.asset.observe(viewLifecycleOwner) { newAsset ->
            // 자산 변동 시 필요하면 처리 가능
        }

        realEstateViewModel.incomeCallback = { income ->
            if (income > 0L) {
                assetViewModel.increaseAsset(income)
                val formatter = DecimalFormat("#,###")
                realEstateStatusText.text = "임대 수익 +${formatter.format(income)}원 발생!"
            }
        }

        buyButton.setOnClickListener {
            selectedEstate?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= it.price && !it.owned) {
                    assetViewModel.decreaseAsset(it.price)
                    realEstateViewModel.buy(it)
                    showEstateDetails(it)
                    realEstateStatusText.text = "${it.name} 매수 완료!"
                } else if (it.owned) {
                    realEstateStatusText.text = "이미 보유 중인 부동산입니다."
                } else {
                    realEstateStatusText.text = "자산이 부족합니다!"
                }
            } ?: run {
                realEstateStatusText.text = "부동산을 선택하세요."
            }
        }

        sellButton.setOnClickListener {
            selectedEstate?.let {
                if (it.owned) {
                    realEstateViewModel.sell(it)
                    assetViewModel.increaseAsset(it.price)
                    showEstateDetails(it)
                    realEstateStatusText.text = "${it.name} 매도 완료!"
                } else {
                    realEstateStatusText.text = "보유하지 않은 부동산입니다."
                }
            } ?: run {
                realEstateStatusText.text = "부동산을 선택하세요."
            }
        }

        return view
    }

    private fun showEstateDetails(estate: RealEstate) {
        val formatter = DecimalFormat("#,###")

        val avgPrice = estate.getAvgPurchasePrice()
        val profitLoss = estate.getProfitLoss()
        val profitRate = estate.getProfitRate()

        realEstateStatusText.text = """
        ${estate.name} 선택됨
        현재가: ${formatter.format(estate.price)}원
        평균 매입가: ${formatter.format(avgPrice)}원
        평가손익: ${formatter.format(profitLoss)}원
        수익률: ${"%.2f".format(profitRate)}%
    """.trimIndent()
    }
}
