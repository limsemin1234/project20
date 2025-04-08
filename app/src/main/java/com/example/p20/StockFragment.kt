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
import android.view.Gravity
import android.widget.FrameLayout

class StockFragment : BaseFragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockDetailsTextView: LinearLayout
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

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stockViewModel = ViewModelProvider(requireActivity())[StockViewModel::class.java]

        stockViewModel.stockItems.observe(viewLifecycleOwner, Observer { updatedStockList ->
            updateStockList(updatedStockList)
            selectedStock?.let { 
                updateStockDetails(it)
                val updatedStock = updatedStockList.find { stock -> stock.name == it.name }
                if (updatedStock != null) {
                    stockAdapter.setSelectedStock(updatedStock)
                }
            }
        })
        
        // 호재 이벤트 콜백 설정
        stockViewModel.setPositiveNewsCallback { stockNames ->
            showPositiveNewsMessage(stockNames)
        }
        
        // 악제 이벤트 콜백 설정
        stockViewModel.setNegativeNewsCallback { stockNames ->
            showNegativeNewsMessage(stockNames)
        }

        setupUI(view)
    }
    
    private fun setupUI(view: View) {
        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockDetailsTextView = view.findViewById(R.id.stockDetailsTextView)
        featuresInfoText = view.findViewById(R.id.stockFeaturesInfoText)

        val buyButton: Button = view.findViewById(R.id.buyButton)
        val sellButton: Button = view.findViewById(R.id.sellButton)
        val buyAllButton: Button = view.findViewById(R.id.buyAllButton)
        val sellAllButton: Button = view.findViewById(R.id.sellAllButton)

        avgPurchasePriceData = view.findViewById(R.id.avgPurchasePriceData)
        profitLossData = view.findViewById(R.id.profitLossData)
        profitRateData = view.findViewById(R.id.profitRateData)
        stockQuantityData = view.findViewById(R.id.stockQuantityData)
        selectedStockName = view.findViewById(R.id.selectedStockName)

        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockDetails(stock) // 주식 상세 정보 업데이트
        }
        stockRecyclerView.adapter = stockAdapter

        buyButton.setOnClickListener {
            selectedStock?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= it.price.toLong()) {
                    assetViewModel.decreaseAsset(it.price.toLong())
                    stockViewModel.buyStock(it)
                    showMessage("${it.name}을(를) 매수했습니다! 보유량: ${it.holding}주")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(it) // 주식 상세 정보 업데이트
                } else {
                    showErrorMessage("자산이 부족합니다!")
                }
            } ?: showMessage("주식을 선택하세요.")
        }

        sellButton.setOnClickListener {
            selectedStock?.let {
                if (it.holding > 0) {
                    stockViewModel.sellStock(it)
                    assetViewModel.increaseAsset(it.price.toLong())
                    val profitLoss = it.getProfitLoss()
                    showMessage("${it.name} 매도! 손익: ${profitLoss}원")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(it) // 주식 상세 정보 업데이트
                } else {
                    showErrorMessage("보유한 주식이 없습니다!")
                }
            } ?: showMessage("주식을 선택하세요.")
        }

        buyAllButton.setOnClickListener {
            selectedStock?.let { stock ->
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= stock.price.toLong()) {
                    val buyCount = stockViewModel.buyAllStock(stock, currentAsset)
                    val usedAsset = stock.price.toLong() * buyCount
                    assetViewModel.decreaseAsset(usedAsset)
                    showMessage("${stock.name}을(를) ${buyCount}주 전체 매수했습니다!")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(stock) // 주식 상세 정보 업데이트
                } else {
                    showErrorMessage("자산이 부족합니다!")
                }
            } ?: showMessage("주식을 선택하세요.")
        }

        sellAllButton.setOnClickListener {
            selectedStock?.let { stock ->
                if (stock.holding > 0) {
                    val sellCount = stockViewModel.sellAllStock(stock)
                    val gain = stock.price.toLong() * sellCount
                    assetViewModel.increaseAsset(gain)
                    showMessage("${stock.name} ${sellCount}주 전체 매도 완료!")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(stock) // 주식 상세 정보 업데이트
                } else {
                    showErrorMessage("보유한 주식이 없습니다!")
                }
            } ?: showMessage("주식을 선택하세요.")
        }
    }

    private fun updateStockList(updatedStockList: List<Stock>) {
        stockItems = updatedStockList
        stockAdapter.updateData(updatedStockList)
    }

    private fun updateStockDetails(stock: Stock) {
        selectedStockName?.text = stock.name
        avgPurchasePriceData?.text = formatCurrency(stock.getAvgPurchasePrice().toLong()) + "원"
        
        val profitLoss = stock.getProfitLoss()
        profitLossData?.text = formatWithSign(profitLoss) + "원"
        profitLossData?.setTextColor(getChangeColor(profitLoss))
        
        val profitRate = stock.getProfitRate()
        profitRateData?.text = formatPercent(profitRate / 100)
        profitRateData?.setTextColor(getChangeColor(profitLoss))
        
        stockQuantityData?.text = "${stock.holding}주"
    }

    /**
     * 호재 메시지용 스낵바를 표시합니다.
     */
    private fun showPositiveNewsMessage(stockNames: List<String>) {
        val message = "호재 발생! ${stockNames.joinToString(", ")} 주가 상승 예상!"
        showSuccessMessage(message)
    }

    /**
     * 악재 메시지용 스낵바를 표시합니다.
     */
    private fun showNegativeNewsMessage(stockNames: List<String>) {
        val message = "악재 발생! ${stockNames.joinToString(", ")} 주가 하락 예상!"
        showErrorMessage(message)
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
