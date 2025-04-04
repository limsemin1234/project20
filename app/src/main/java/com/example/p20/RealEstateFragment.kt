package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class RealEstateFragment : Fragment() {

    private lateinit var realEstateRecyclerView: RecyclerView
    private lateinit var realEstateAdapter: RealEstateAdapter
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private var selectedEstate: RealEstate? = null

    private lateinit var incomeMessageText: TextView

    // MotionLayout 관련
    private lateinit var motionLayout: MotionLayout
    private lateinit var estateDetailLayout: LinearLayout
    private lateinit var estateDetailName: TextView
    private lateinit var estateDetailInfo: TextView
    private lateinit var detailBuyButton: Button
    private lateinit var detailSellButton: Button

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_estate, container, false)

        realEstateRecyclerView = view.findViewById(R.id.realEstateRecyclerView)
        incomeMessageText = view.findViewById(R.id.incomeMessageText)

        motionLayout = view.findViewById(R.id.motionLayout)
        estateDetailLayout = view.findViewById(R.id.estateDetailLayout)
        estateDetailName = view.findViewById(R.id.estateDetailName)
        estateDetailInfo = view.findViewById(R.id.estateDetailInfo)
        detailBuyButton = view.findViewById(R.id.detailBuyButton)
        detailSellButton = view.findViewById(R.id.detailSellButton)

        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        realEstateRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        realEstateAdapter = RealEstateAdapter(emptyList()) { estate ->
            selectedEstate = estate
            showEstateDetailSlide(estate)
        }
        realEstateRecyclerView.adapter = realEstateAdapter

        realEstateViewModel.realEstateList.observe(viewLifecycleOwner) { updatedList ->
            realEstateAdapter.updateList(updatedList)
            
            // --- 추가: 리스트 업데이트 시 선택 상태 및 상세 UI 초기화 ---
            selectedEstate?.let { currentSelected ->
                // 리셋 등으로 인해 기존 선택 항목이 사라졌는지 확인
                val stillExists = updatedList?.any { it.id == currentSelected.id } ?: false
                if (!stillExists || !currentSelected.owned) { // 소유하지 않게 된 경우 포함
                    selectedEstate = null
                    motionLayout.transitionToStart() // 슬라이드 닫기
                } else {
                    // 선택 상태 유지 시 상세 정보만 업데이트
                    updateEstateDetailInfo(currentSelected)
                }
            }
            // --- 추가 끝 ---
        }

        // 통합형 임대 수익 메시지 처리
        realEstateViewModel.incomeCallback = { totalIncome ->
            assetViewModel.increaseAsset(totalIncome)

            val formatter = DecimalFormat("#,###")
            incomeMessageText.text = "총 임대 수익 +${formatter.format(totalIncome)}원 발생!"
            incomeMessageText.visibility = View.VISIBLE
            incomeMessageText.alpha = 1f
            incomeMessageText.animate()
                .alpha(0f)
                .setDuration(4000)
                    .withEndAction {
                        // 다시 기본 문구로 복원
                        incomeMessageText.text = "임대 수익 발생 시 표시됩니다."
                        incomeMessageText.alpha = 1f
                        incomeMessageText.visibility = View.VISIBLE
                    }.start()

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

        val closeButton = view.findViewById<ImageButton>(R.id.detailCloseButton)
        closeButton.setOnClickListener {
            motionLayout.transitionToStart()
        }


        return view
    }

    private fun showEstateDetailSlide(estate: RealEstate) {
        estateDetailLayout.visibility = View.VISIBLE
        motionLayout.transitionToEnd()
        estateDetailName.text = estate.name // 이 부분 추가
        updateEstateDetailInfo(estate)
    }


    /////////슬라이스 안에 내용///////
    private fun updateEstateDetailInfo(estate: RealEstate) {
        val formatter = DecimalFormat("#,###")
        val currentPrice = estate.price
        val avgPrice = estate.getAvgPurchasePrice()
        val profitLoss = estate.getProfitLoss()
        val profitRate = estate.getProfitRate()

        val profitSign = if (profitLoss > 0) "+" else if (profitLoss < 0) "-" else ""
        val infoText = """
        현재가: ${formatter.format(currentPrice)}원
        구매가: ${formatter.format(avgPrice)}원
        차익금: $profitSign${formatter.format(kotlin.math.abs(profitLoss))}원
        수익률: $profitSign${"%.2f".format(kotlin.math.abs(profitRate))}%
    """.trimIndent()

        val spannable = android.text.SpannableString(infoText)

        // 현재가 & 구매가 → 흰색
        val currentPriceStart = infoText.indexOf("현재가:")
        val avgPriceStart = infoText.indexOf("구매가:")
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE),
            currentPriceStart,
            avgPriceStart + "구매가: ${formatter.format(avgPrice)}원".length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // 차익금 & 수익률 → 조건별 색상
        val profitStart = infoText.indexOf("차익금:")
        val profitColor = when {
            profitLoss > 0 -> "#00FF66" // 초록
            profitLoss < 0 -> "#FF5555" // 빨강
            else -> "#FFFFFF" // 0일 때 흰색
        }
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor(profitColor)),
            profitStart,
            infoText.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        estateDetailInfo.text = spannable
    }


    ///////////////////////////


    private fun closeEstateDetailSlide() {
        motionLayout.transitionToStart()
        estateDetailLayout.visibility = View.GONE
    }

    private fun attemptToBuyEstate(estate: RealEstate) {
        val currentAsset = assetViewModel.asset.value ?: 0L
        when {
            estate.owned -> Toast.makeText(requireContext(), "이미 보유 중인 부동산입니다.", Toast.LENGTH_SHORT).show()
            currentAsset >= estate.price -> {
                assetViewModel.decreaseAsset(estate.price.toLong())
                realEstateViewModel.buy(estate)
                Toast.makeText(requireContext(), "${estate.name} 매수 완료!", Toast.LENGTH_SHORT).show()
                //closeEstateDetailSlide() //구매 시 슬라이스 닫힘
            }
            else -> Toast.makeText(requireContext(), "자산이 부족합니다!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attemptToSellEstate(estate: RealEstate) {
        if (estate.owned) {
            realEstateViewModel.sell(estate)
            assetViewModel.increaseAsset(estate.price.toLong())
            Toast.makeText(requireContext(), "${estate.name} 매도 완료!", Toast.LENGTH_SHORT).show()
            //closeEstateDetailSlide() //판매 시 슬라이스 닫힘
        } else {
            Toast.makeText(requireContext(), "보유하지 않은 부동산입니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
