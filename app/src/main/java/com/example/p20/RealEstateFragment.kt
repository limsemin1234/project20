package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private lateinit var selectedEstateText: TextView
    private lateinit var incomeMessageText: TextView
    private lateinit var estateActionResultText: TextView
    private lateinit var estateDetailText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var clearMessageRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_estate, container, false)

        realEstateRecyclerView = view.findViewById(R.id.realEstateRecyclerView)
        selectedEstateText = view.findViewById(R.id.selectedEstateText)
        incomeMessageText = view.findViewById(R.id.incomeMessageText)
        estateActionResultText = view.findViewById(R.id.estateActionResultText)
        estateDetailText = view.findViewById(R.id.estateDetailText)

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

        realEstateViewModel.incomeCallback = { income ->
            if (income > 0) {
                assetViewModel.increaseAsset(income)
                val formatter = DecimalFormat("#,###")
                incomeMessageText.text = "+${formatter.format(income)}원 임대 수익 발생!"
                incomeMessageText.alpha = 1f
                incomeMessageText.animate()
                    .alpha(0f)
                    .setDuration(4000)
                    .start()
            }
        }

        buyButton.setOnClickListener {
            selectedEstate?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                when {
                    it.owned -> showEstateActionMessage("이미 보유 중인 부동산입니다.")
                    currentAsset >= it.price -> {
                        assetViewModel.decreaseAsset(it.price.toLong())
                        realEstateViewModel.buy(it)
                        showEstateDetails(it)
                        showEstateActionMessage("${it.name} 매수 완료!")
                    }
                    else -> showEstateActionMessage("자산이 부족합니다!")
                }
            } ?: run {
                showEstateActionMessage("부동산을 선택하세요.")
            }
        }

        sellButton.setOnClickListener {
            selectedEstate?.let {
                if (it.owned) {
                    realEstateViewModel.sell(it)
                    assetViewModel.increaseAsset(it.price.toLong())
                    showEstateDetails(it)
                    showEstateActionMessage("${it.name} 매도 완료!")
                } else {
                    showEstateActionMessage("보유하지 않은 부동산입니다.")
                }
            } ?: run {
                showEstateActionMessage("부동산을 선택하세요.")
            }
        }

        return view
    }

    // 부동산 상세 정보 표시
    private fun showEstateDetails(estate: RealEstate) {
        val formatter = DecimalFormat("#,###")

        val avgPrice = estate.getAvgPurchasePrice()
        val profitLoss = estate.getProfitLoss()
        val profitRate = estate.getProfitRate()

        selectedEstateText.text = "${estate.name} 선택됨"
        estateDetailText.text = """
            현재가: ${formatter.format(estate.price)}원
            평균 매입가: ${formatter.format(avgPrice)}원
            평가손익: ${formatter.format(profitLoss)}원
            수익률: ${"%.2f".format(profitRate)}%
        """.trimIndent()
    }

    // 상태 메시지 표시 후 3초 후 자동 삭제
    private fun showEstateActionMessage(message: String) {
        estateActionResultText.text = message
        clearMessageRunnable?.let { handler.removeCallbacks(it) }

        clearMessageRunnable = Runnable {
            estateActionResultText.text = ""
        }
        handler.postDelayed(clearMessageRunnable!!, 3000)
    }
}
