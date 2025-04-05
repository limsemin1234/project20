package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import java.text.NumberFormat
import java.util.Locale

class RealInfoFragment : Fragment() {
    private lateinit var viewModel: TimeViewModel
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var stockViewModel: StockViewModel
    private lateinit var realEstateViewModel: RealEstateViewModel

    // 자산 변수
    private var currentAsset: Long = 0
    private var stockAsset: Long = 0
    private var realEstateAsset: Long = 0
    private lateinit var totalAssetTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_info, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel 초기화
        viewModel = ViewModelProvider(requireActivity())[TimeViewModel::class.java]
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]
        stockViewModel = ViewModelProvider(requireActivity())[StockViewModel::class.java]
        realEstateViewModel = ViewModelProvider(requireActivity())[RealEstateViewModel::class.java]

        // 텍스트뷰 참조
        totalAssetTextView = view.findViewById(R.id.totalAssetTextView)
        val assetTextView = view.findViewById<TextView>(R.id.assetTextView)
        val stockTextView = view.findViewById<TextView>(R.id.stockTextView)
        val realEstateTextView = view.findViewById<TextView>(R.id.realEstateTextView)
        
        // 배경 이미지에 애니메이션 적용
        val backgroundImageView = view.findViewById<ImageView>(R.id.backgroundImageView)
        if (backgroundImageView != null) {
            val fadeAnimation = AlphaAnimation(0.8f, 1.0f)
            fadeAnimation.duration = 1500
            fadeAnimation.repeatCount = Animation.INFINITE
            fadeAnimation.repeatMode = Animation.REVERSE
            backgroundImageView.startAnimation(fadeAnimation)
        }

        // 자산 정보 표시
        assetViewModel.asset.observe(viewLifecycleOwner) { asset ->
            currentAsset = asset
            assetTextView.text = "현재 자산: ${formatCurrency(asset)}"
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
            stockTextView.text = "주식 자산: ${formatCurrency(stockAsset)}"
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
            realEstateTextView.text = "부동산 자산: ${formatCurrency(realEstateAsset)}"
            updateTotalAsset()
        }
    }
    
    // 총자산 업데이트 함수
    private fun updateTotalAsset() {
        val totalAsset = currentAsset + stockAsset + realEstateAsset
        totalAssetTextView.text = "총자산: ${formatCurrency(totalAsset)}"
    }
    
    // 통화 형식 포맷팅 함수
    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return formatter.format(amount)
    }
} 