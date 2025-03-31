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
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private var selectedEstate: RealEstate? = null

    // 상태 표시용 텍스트
    private lateinit var selectedEstateText: TextView
    private lateinit var incomeMessageText: TextView
    private lateinit var estateActionResultText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_estate, container, false)

        // View 연결
        realEstateRecyclerView = view.findViewById(R.id.realEstateRecyclerView)
        selectedEstateText = view.findViewById(R.id.selectedEstateText)
        incomeMessageText = view.findViewById(R.id.incomeMessageText)
        estateActionResultText = view.findViewById(R.id.estateActionResultText)

        val buyButton: Button = view.findViewById(R.id.buyRealEstateButton)
        val sellButton: Button = view.findViewById(R.id.sellRealEstateButton)

        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        // RecyclerView 설정
        realEstateRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        realEstateAdapter = RealEstateAdapter(emptyList()) { estate ->
            selectedEstate = estate
            showEstateDetails(estate)
        }
        realEstateRecyclerView.adapter = realEstateAdapter

        // 데이터 옵저빙
        realEstateViewModel.realEstateList.observe(viewLifecycleOwner) { updatedList ->
            realEstateAdapter.updateList(updatedList)
            selectedEstate?.let { showEstateDetails(it) }
        }

        // 임대 수익 콜백
        realEstateViewModel.incomeCallback = { income ->
            if (income > 0) {
                assetViewModel.increaseAsset(income)
                val formatter = DecimalFormat("#,###")
                incomeMessageText.text = "+${formatter.format(income)}원 임대 수익 발생!"
                incomeMessageText.alpha = 1f
                incomeMessageText.animate()
                    .alpha(0f)
                    .setDuration(2000)
                    .start()
            }
        }

        // 매수
        buyButton.setOnClickListener {
            selectedEstate?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= it.price && !it.owned) {
                    assetViewModel.decreaseAsset(it.price.toLong())
                    realEstateViewModel.buy(it)
                    showEstateDetails(it)
                    estateActionResultText.text = "${it.name} 매수 완료!"
                } else if (it.owned) {
                    estateActionResultText.text = "이미 보유 중인 부동산입니다."
                } else {
                    estateActionResultText.text = "자산이 부족합니다!"
                }
            } ?: run {
                estateActionResultText.text = "부동산을 선택하세요."
            }
        }

        // 매도
        sellButton.setOnClickListener {
            selectedEstate?.let {
                if (it.owned) {
                    realEstateViewModel.sell(it)
                    assetViewModel.increaseAsset(it.price.toLong())
                    showEstateDetails(it)
                    estateActionResultText.text = "${it.name} 매도 완료!"
                } else {
                    estateActionResultText.text = "보유하지 않은 부동산입니다."
                }
            } ?: run {
                estateActionResultText.text = "부동산을 선택하세요."
            }
        }

        return view
    }

    // 선택된 부동산 정보 표시
    private fun showEstateDetails(estate: RealEstate) {
        val formatter = DecimalFormat("#,###")

        val avgPrice = estate.getAvgPurchasePrice()
        val profitLoss = estate.getProfitLoss()
        val profitRate = estate.getProfitRate()

        selectedEstateText.text = "${estate.name} 선택됨"
        estateActionResultText.text = """
            현재가: ${formatter.format(estate.price)}원
            평균 매입가: ${formatter.format(avgPrice)}원
            평가손익: ${formatter.format(profitLoss)}원
            수익률: ${"%.2f".format(profitRate)}%
        """.trimIndent()
    }
}
