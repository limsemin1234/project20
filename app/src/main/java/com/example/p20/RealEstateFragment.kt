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
import com.google.android.material.snackbar.Snackbar
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import java.text.DecimalFormat

class RealEstateFragment : Fragment() {

    private lateinit var realEstateRecyclerView: RecyclerView
    private lateinit var realEstateAdapter: RealEstateAdapter
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel
    private var selectedEstate: RealEstate? = null

    private lateinit var incomeMessageText: TextView
    private lateinit var warEventMessageText: TextView
    private lateinit var featuresInfoText: TextView

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
        warEventMessageText = view.findViewById(R.id.warEventMessageText)
        featuresInfoText = view.findViewById(R.id.featuresInfoText)

        motionLayout = view.findViewById(R.id.motionLayout)
        estateDetailLayout = view.findViewById(R.id.estateDetailLayout)
        estateDetailName = view.findViewById(R.id.estateDetailName)
        estateDetailInfo = view.findViewById(R.id.estateDetailInfo)
        detailBuyButton = view.findViewById(R.id.detailBuyButton)
        detailSellButton = view.findViewById(R.id.detailSellButton)

        realEstateViewModel = ViewModelProvider(requireActivity()).get(RealEstateViewModel::class.java)
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        realEstateRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        realEstateAdapter = RealEstateAdapter(
            emptyList(),
            { estate ->
                selectedEstate = estate
                showEstateDetailSlide(estate)
            },
            realEstateViewModel  // ViewModel 전달
        )
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

        // 전쟁 이벤트 메시지 처리
        realEstateViewModel.warEventMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrEmpty()) {
                warEventMessageText.visibility = View.GONE
            } else {
                warEventMessageText.text = message
                warEventMessageText.visibility = View.VISIBLE
                
                // 메시지가 전쟁 종료에 관한 것이면 5초 후 숨김
                if (message.contains("복구")) {
                    handler.postDelayed({
                        warEventMessageText.visibility = View.GONE
                    }, 5000)
                }
            }
        }
        
        // 전쟁 이벤트 콜백 설정
        realEstateViewModel.warEventCallback = { message ->
            // 진동, 소리 등 추가 알림을 여기서 처리할 수 있음
            activity?.runOnUiThread {
                showCustomSnackbar(message)
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
            estate.owned -> showCustomSnackbar("이미 보유 중인 부동산입니다.")
            currentAsset >= estate.price -> {
                assetViewModel.decreaseAsset(estate.price.toLong())
                realEstateViewModel.buy(estate)
                showCustomSnackbar("${estate.name} 매수 완료!")
            }
            else -> showCustomSnackbar("자산이 부족합니다!")
        }
    }

    private fun attemptToSellEstate(estate: RealEstate) {
        if (estate.owned) {
            realEstateViewModel.sell(estate)
            assetViewModel.increaseAsset(estate.price.toLong())
            showCustomSnackbar("${estate.name} 매도 완료!")
        } else {
            showCustomSnackbar("보유하지 않은 부동산입니다.")
        }
    }

    private fun showCustomSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 콜백 참조 해제하여 메모리 누수 방지
        realEstateViewModel.incomeCallback = null
        realEstateViewModel.warEventCallback = null
        // 핸들러 콜백도 명시적으로 제거 (안전성 강화)
        handler.removeCallbacksAndMessages(null)
    }

    // 구현 기능 설명 업데이트 메서드
    fun updateFeaturesInfo(newFeature: String) {
        val currentText = featuresInfoText.text.toString()
        val baseText = currentText.split("\n")[0] // "📌 구현 기능:" 부분만 가져옴
        val features = currentText.substringAfter("\n").split("\n- ").filter { it.isNotEmpty() }.toMutableList()
        
        // 새 기능이 이미 있는지 확인
        if (!features.contains(newFeature)) {
            features.add(newFeature)
        }
        
        // 업데이트된 텍스트 설정
        val updatedText = "$baseText\n- ${features.joinToString("\n- ")}"
        featuresInfoText.text = updatedText
    }
}
