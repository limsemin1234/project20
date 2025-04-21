package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider

/**
 * 사용자의 자산 정보를 종합적으로 표시하는 Fragment
 */
class MyInfoFragment : BaseFragment() {
    private lateinit var stockViewModel: StockViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel

    // 자산 변수
    private var currentAsset: Long = 0
    private var stockAsset: Long = 0
    private var realEstateAsset: Long = 0
    private var depositAsset: Long = 0    // 예금 자산 추가
    private var loanAsset: Long = 0       // 대출 금액 추가
    private lateinit var totalAssetTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_myinfo, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // BaseFragment에서 제공되지 않는 ViewModel 초기화
        stockViewModel = ViewModelProvider(requireActivity())[StockViewModel::class.java]
        realEstateViewModel = ViewModelProvider(requireActivity())[RealEstateViewModel::class.java]

        // 텍스트뷰 참조
        totalAssetTextView = view.findViewById(R.id.totalAssetTextView)
        val assetTextView = view.findViewById<TextView>(R.id.assetTextView)
        val stockTextView = view.findViewById<TextView>(R.id.stockTextView)
        val realEstateTextView = view.findViewById<TextView>(R.id.realEstateTextView)
        val depositTextView = view.findViewById<TextView>(R.id.depositTextView)
        val loanTextView = view.findViewById<TextView>(R.id.loanTextView)
        
        // 배경 이미지에 애니메이션 적용 - BaseFragment의 애니메이션 유틸리티 활용
        val backgroundImageView = view.findViewById<ImageView>(R.id.backgroundImageView)
        if (backgroundImageView != null) {
            // trackAnimation을 통해 애니메이션 리소스 관리
            applyAnimation(backgroundImageView, "heartbeat", 1500, 0.1f)
        }

        // 자산 정보 표시
        assetViewModel.asset.observe(viewLifecycleOwner) { asset ->
            currentAsset = asset
            assetTextView.text = formatCurrency(asset)
            
            // 자산이 마이너스일 경우 색상 변경
            if (asset < 0) {
                assetTextView.setTextColor(Color.RED)
            } else {
                assetTextView.setTextColor(Color.WHITE) // 기존 색상 유지
            }
            
            updateTotalAsset()
        }

        // 주식 자산 정보 표시
        stockViewModel.stockItems.observe(viewLifecycleOwner) { stocks ->
            stockAsset = 0
            stocks.forEach { stock ->
                // 보유 주식 * 현재가
                if (stock.holding > 0) {
                    stockAsset += stock.price.toLong() * stock.holding
                }
            }
            stockTextView.text = formatCurrency(stockAsset)
            updateTotalAsset()
        }

        // 부동산 자산 정보 표시
        realEstateViewModel.realEstateList.observe(viewLifecycleOwner) { realEstates ->
            realEstateAsset = 0
            realEstates.forEach { estate ->
                // 보유 부동산 * 현재가
                if (estate.owned) {
                    realEstateAsset += estate.price
                }
            }
            realEstateTextView.text = formatCurrency(realEstateAsset)
            updateTotalAsset()
        }
        
        // 예금 정보 표시
        assetViewModel.deposit.observe(viewLifecycleOwner) { deposit ->
            depositAsset = deposit
            depositTextView.text = formatCurrency(deposit)
            updateTotalAsset()
        }
        
        // 대출 정보 표시 (마이너스로 표시)
        assetViewModel.loan.observe(viewLifecycleOwner) { loan ->
            loanAsset = loan
            loanTextView.text = formatCurrency(-loan) // 대출은 마이너스로 표시
            updateTotalAsset()
        }
    }
    
    // 총자산 업데이트 함수
    private fun updateTotalAsset() {
        // 총자산 = 현금 + 주식 + 부동산 + 예금 - 대출
        val totalAsset = currentAsset + stockAsset + realEstateAsset + depositAsset - loanAsset
        totalAssetTextView.text = formatCurrency(totalAsset)
        
        // 총자산이 마이너스일 경우 색상 변경
        if (totalAsset < 0) {
            totalAssetTextView.setTextColor(Color.RED)
        } else {
            totalAssetTextView.setTextColor(Color.WHITE) // 기존 색상 유지
        }
    }
} 