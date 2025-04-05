package com.example.p20

import android.graphics.Color
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
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar

class StockFragment : Fragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockDetailsTextView: LinearLayout
    private lateinit var assetViewModel: AssetViewModel
    private lateinit var featuresInfoText: TextView

    private var selectedStock: Stock? = null

    private lateinit var stockViewModel: StockViewModel
    private var stockItems: List<Stock> = listOf()

    private var avgPurchasePriceData: TextView? = null
    private var profitLossData: TextView? = null
    private var profitRateData: TextView? = null
    private var stockQuantityData: TextView? = null
    private var selectedStockName: TextView? = null

    private var isPositiveNewsFeatureAdded = false
    private var isNegativeNewsFeatureAdded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stock, container, false)

        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)

        stockViewModel.stockItems.observe(viewLifecycleOwner, Observer { updatedStockList ->
            updateStockList(updatedStockList)
            selectedStock?.let { updateStockDetails(it) }
        })
        
        // 호재 이벤트 콜백 설정
        stockViewModel.setPositiveNewsCallback { stockNames ->
            showPositiveNewsMessage(stockNames)
            
            // 호재 기능 설명은 이미 추가되어 있으므로 제거
        }
        
        // 악제 이벤트 콜백 설정
        stockViewModel.setNegativeNewsCallback { stockNames ->
            showNegativeNewsMessage(stockNames)
            
            // 악제 기능 설명은 이미 추가되어 있으므로 제거
        }

        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockDetailsTextView = view.findViewById(R.id.stockDetailsTextView)
        featuresInfoText = view.findViewById(R.id.stockFeaturesInfoText)

        // 구현 기능 설명에 호재/악제 이벤트 설명 미리 추가
        updateFeaturesInfo("호재 이벤트: 30초마다 30% 확률로 2개 주식에 호재 발생 (20초간 상승만 함)")
        updateFeaturesInfo("악제 이벤트: 30초마다 30% 확률로 2개 주식에 악제 발생 (20초간 하락만 함)")
        isPositiveNewsFeatureAdded = true
        isNegativeNewsFeatureAdded = true
        
        val buyButton: Button = view.findViewById(R.id.buyButton)
        val sellButton: Button = view.findViewById(R.id.sellButton)
        val buyAllButton: Button = view.findViewById(R.id.buyAllButton)
        val sellAllButton: Button = view.findViewById(R.id.sellAllButton)

        avgPurchasePriceData = view.findViewById(R.id.avgPurchasePriceData)
        profitLossData = view.findViewById(R.id.profitLossData)
        profitRateData = view.findViewById(R.id.profitRateData)
        stockQuantityData = view.findViewById(R.id.stockQuantityData)
        selectedStockName = view.findViewById(R.id.selectedStockName)

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        assetViewModel.asset.observe(viewLifecycleOwner, Observer { newAsset ->
            // 자산 정보를 스낵바로 표시
            showSnackbar("현재 자산: ${String.format("%,d", newAsset)}원")
        })

        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            showSnackbar("${stock.name}이(가) 선택되었습니다.")
            updateStockDetails(stock) // 주식 상세 정보 업데이트
        }
        stockRecyclerView.adapter = stockAdapter

        buyButton.setOnClickListener {
            selectedStock?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= it.price.toLong()) {
                    assetViewModel.decreaseAsset(it.price.toLong())
                    stockViewModel.buyStock(it)
                    showSnackbar("${it.name}을(를) 매수했습니다! 보유량: ${it.holding}주")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(it) // 주식 상세 정보 업데이트
                } else {
                    showSnackbar("자산이 부족합니다!")
                }
            } ?: showSnackbar("주식을 선택하세요.")
        }

        sellButton.setOnClickListener {
            selectedStock?.let {
                if (it.holding > 0) {
                    stockViewModel.sellStock(it)
                    assetViewModel.increaseAsset(it.price.toLong())
                    val profitLoss = it.getProfitLoss()
                    showSnackbar("${it.name} 매도! 손익: ${profitLoss}원")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(it) // 주식 상세 정보 업데이트
                } else {
                    showSnackbar("보유한 주식이 없습니다!")
                }
            } ?: showSnackbar("주식을 선택하세요.")
        }

        buyAllButton.setOnClickListener {
            selectedStock?.let { stock ->
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= stock.price.toLong()) {
                    val buyCount = stockViewModel.buyAllStock(stock, currentAsset)
                    val usedAsset = stock.price.toLong() * buyCount
                    assetViewModel.decreaseAsset(usedAsset)
                    showSnackbar("${stock.name}을(를) ${buyCount}주 전체 매수했습니다!")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(stock) // 주식 상세 정보 업데이트
                } else {
                    showSnackbar("자산이 부족합니다!")
                }
            } ?: showSnackbar("주식을 선택하세요.")
        }

        sellAllButton.setOnClickListener {
            selectedStock?.let { stock ->
                if (stock.holding > 0) {
                    val sellCount = stockViewModel.sellAllStock(stock)
                    val gain = stock.price.toLong() * sellCount
                    assetViewModel.increaseAsset(gain)
                    showSnackbar("${stock.name} ${sellCount}주 전체 매도 완료!")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(stock) // 주식 상세 정보 업데이트
                } else {
                    showSnackbar("보유한 주식이 없습니다!")
                }
            } ?: showSnackbar("주식을 선택하세요.")
        }

        return view
    }

    /**
     * 일반 메시지용 스낵바를 표시합니다.
     */
    private fun showSnackbar(message: String) {
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 3
        snackbar.show()
    }

    private fun showPositiveNewsMessage(stockNames: List<String>) {
        val message = "🔥 호재 발생! ${stockNames.joinToString(", ")} 종목 상승중! (20초간)"
        
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.parseColor("#4CAF50")) // 초록색 배경
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.maxLines = 3
        
        snackbar.show()
    }
    
    private fun showNegativeNewsMessage(stockNames: List<String>) {
        val message = "⚠️ 악제 발생! ${stockNames.joinToString(", ")} 종목 하락중! (20초간)"
        
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.parseColor("#F44336")) // 빨간색 배경
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(Color.WHITE)
        textView.maxLines = 3
        
        snackbar.show()
    }
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun updateStockList(newStockItems: MutableList<Stock>?) {
        // 기존 어댑터에 데이터 업데이트
        newStockItems?.let {
             stockAdapter.updateData(it)
        }
    }

    private fun clearStockDetails() {
        selectedStockName?.text = "-"
        avgPurchasePriceData?.text = "0원"
        profitLossData?.text = "0원"
        profitRateData?.text = "0%"
        stockQuantityData?.text = "0주"
        profitLossData?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        profitRateData?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
    }

    private fun updateStockDetails(stock: Stock) {
        selectedStockName?.text = stock.name

        if (stock.holding > 0) {
            val avgPurchasePrice = stock.getAvgPurchasePrice()
            val profitLoss = stock.getProfitLoss()
            val profitRate = stock.getProfitRate()

            avgPurchasePriceData?.text = "${String.format("%,d", avgPurchasePrice)}원"
            profitLossData?.text = "${String.format("%,d", profitLoss)}원"
            profitRateData?.text = "${"%.2f".format(profitRate)}%"
            stockQuantityData?.text = "${String.format("%,d", stock.holding)}주"


            // 색상 처리
            val red = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
            val blue = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            val black = ContextCompat.getColor(requireContext(), android.R.color.black)

            val profitLossColor = when {
                profitLoss > 0 -> red
                profitLoss < 0 -> blue
                else -> black
            }
            val profitRateColor = when {
                profitRate > 0 -> red
                profitRate < 0 -> blue
                else -> black
            }

            profitLossData?.setTextColor(profitLossColor)
            profitRateData?.setTextColor(profitRateColor)

        } else {
            avgPurchasePriceData?.text = "0원"
            profitLossData?.text = "0원"
            profitRateData?.text = "0%"
            stockQuantityData?.text = "0주"

            profitLossData?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            profitRateData?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        }
    }

    fun updateFeaturesInfo(newFeature: String) {
        val currentText = featuresInfoText.text.toString()
        val baseText = currentText.split("\n")[0]
        val features = currentText.substringAfter("\n").split("\n- ").filter { it.isNotEmpty() }.toMutableList()
        
        if (!features.contains(newFeature)) {
            features.add(newFeature)
        }
        
        val updatedText = "$baseText\n- ${features.joinToString("\n- ")}"
        featuresInfoText.text = updatedText
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
