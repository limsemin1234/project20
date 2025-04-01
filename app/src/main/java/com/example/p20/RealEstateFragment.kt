package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.motion.widget.MotionLayout
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

    private lateinit var incomeMessageText: TextView
    private lateinit var estateActionResultText: TextView

    // MotionLayout 관련
    private lateinit var motionLayout: MotionLayout
    private lateinit var estateDetailLayout: LinearLayout
    private lateinit var estateDetailName: TextView
    private lateinit var estateDetailInfo: TextView
    private lateinit var detailBuyButton: Button
    private lateinit var detailSellButton: Button
    private lateinit var detailCloseButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private var clearMessageRunnable: Runnable? = null

    private var backPressedCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_estate, container, false)

        realEstateRecyclerView = view.findViewById(R.id.realEstateRecyclerView)
        incomeMessageText = view.findViewById(R.id.incomeMessageText)
        estateActionResultText = view.findViewById(R.id.estateActionResultText)

        motionLayout = view.findViewById(R.id.motionLayout)
        estateDetailLayout = view.findViewById(R.id.estateDetailLayout)
        estateDetailName = view.findViewById(R.id.estateDetailName)
        estateDetailInfo = view.findViewById(R.id.estateDetailInfo)
        detailBuyButton = view.findViewById(R.id.detailBuyButton)
        detailSellButton = view.findViewById(R.id.detailSellButton)
        detailCloseButton = view.findViewById(R.id.detailCloseButton)

        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        realEstateRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        realEstateAdapter = RealEstateAdapter(emptyList()) { estate ->
            selectedEstate = estate
            showEstateDetailSlide(estate)
        }
        realEstateRecyclerView.adapter = realEstateAdapter

        realEstateViewModel.realEstateList.observe(viewLifecycleOwner) { updatedList ->
            realEstateAdapter.updateList(updatedList)

            // 목록의 현재가 변동 시 슬라이드에 있는 정보도 자동으로 업데이트
            selectedEstate?.let {
                if (it.name == selectedEstate?.name) {
                    updateEstateDetailInfo(it)
                }
            }
        }

        // 임대 수익 발생 시 해당 부동산 아이템에 메시지 표시
        realEstateViewModel.incomeCallback = { estate, income ->
            if (income > 0) {
                assetViewModel.increaseAsset(income)
                realEstateAdapter.showIncomeMessage(estate.id, income)
            }
        }

        detailBuyButton.setOnClickListener {
            selectedEstate?.let {
                attemptToBuyEstate(it)
            }
        }

        detailSellButton.setOnClickListener {
            selectedEstate?.let {
                attemptToSellEstate(it)
            }
        }

        detailCloseButton.setOnClickListener {
            closeEstateDetailSlide()
        }

        return view
    }

    // 부동산의 세부 정보를 슬라이드 뷰에 표시
    private fun showEstateDetailSlide(estate: RealEstate) {
        estateDetailLayout.visibility = View.VISIBLE
        motionLayout.transitionToEnd()

        // 부동산 세부 정보 업데이트
        updateEstateDetailInfo(estate)
    }

    // 부동산 정보에 맞게 슬라이드 뷰 갱신
    private fun updateEstateDetailInfo(estate: RealEstate) {
        val formatter = DecimalFormat("#,###")
        val currentPrice = estate.price
        val avgPrice = estate.getAvgPurchasePrice()
        val profitLoss = estate.getProfitLoss()
        val profitRate = estate.getProfitRate()

        val profitSign = if (profitLoss >= 0) "+" else "-"
        val profitColor = if (profitLoss >= 0) "#00FF66" else "#FF5555"

        val infoText = """
            현재가: ${formatter.format(currentPrice)}원
            구매가: ${formatter.format(avgPrice)}원
            차익금: $profitSign${formatter.format(kotlin.math.abs(profitLoss))}원
            수익률: $profitSign${"%.2f".format(kotlin.math.abs(profitRate))}%
        """.trimIndent()

        estateDetailName.text = estate.name
        estateDetailInfo.text = infoText
        estateDetailInfo.setTextColor(android.graphics.Color.parseColor(profitColor))
    }

    private fun closeEstateDetailSlide() {
        motionLayout.transitionToStart()
        estateDetailLayout.visibility = View.GONE
    }

    private fun attemptToBuyEstate(estate: RealEstate) {
        val currentAsset = assetViewModel.asset.value ?: 0L
        when {
            estate.owned -> showEstateActionMessage("이미 보유 중인 부동산입니다.")
            currentAsset >= estate.price -> {
                assetViewModel.decreaseAsset(estate.price.toLong())
                realEstateViewModel.buy(estate)
                showEstateActionMessage("${estate.name} 매수 완료!")
                Toast.makeText(requireContext(), "${estate.name} 매수 완료!", Toast.LENGTH_SHORT).show()
                closeEstateDetailSlide()
            }
            else -> showEstateActionMessage("자산이 부족합니다!")
        }
    }

    private fun attemptToSellEstate(estate: RealEstate) {
        if (estate.owned) {
            realEstateViewModel.sell(estate)
            assetViewModel.increaseAsset(estate.price.toLong())
            showEstateActionMessage("${estate.name} 매도 완료!")
            Toast.makeText(requireContext(), "${estate.name} 매도 완료!", Toast.LENGTH_SHORT).show()
            closeEstateDetailSlide()
        } else {
            showEstateActionMessage("보유하지 않은 부동산입니다.")
        }
    }

    private fun showEstateActionMessage(message: String) {
        estateActionResultText.text = message
        clearMessageRunnable?.let { handler.removeCallbacks(it) }

        clearMessageRunnable = Runnable {
            estateActionResultText.text = ""
        }
        handler.postDelayed(clearMessageRunnable!!, 3000)
    }
}
