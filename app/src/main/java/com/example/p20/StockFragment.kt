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
    private var selectedQuantity: Int = 0  // 기본 수량을 0으로 설정

    private lateinit var stockViewModel: StockViewModel
    private var stockItems: List<Stock> = listOf()

    private var avgPurchasePriceData: TextView? = null
    private var profitLossData: TextView? = null
    private var profitRateData: TextView? = null
    private var stockQuantityData: TextView? = null
    private var selectedStockName: TextView? = null
    private var selectedQuantityText: TextView? = null  // 선택된 수량을 표시할 TextView

    private var isPositiveNewsFeatureAdded = false
    private var isNegativeNewsFeatureAdded = false

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // 수량 버튼 참조 변수
    private lateinit var quantityBtn1: Button
    private lateinit var quantityBtn5: Button
    private lateinit var quantityBtn10: Button
    private lateinit var quantityBtn20: Button
    private lateinit var quantityBtn50: Button
    private lateinit var quantityBtn100: Button
    private lateinit var resetQuantityBtn: Button

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
        
        // 화면 전환 후 다시 돌아왔을 때 수량 초기화
        resetSelectedQuantity()
        
        // 기능 설명 업데이트
        updateImplementedFeatures()
    }
    
    override fun onResume() {
        super.onResume()
        // 화면으로 돌아올 때 수량 초기화
        resetSelectedQuantity()
    }
    
    private fun setupUI(view: View) {
        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockDetailsTextView = view.findViewById(R.id.stockDetailsTextView)
        featuresInfoText = view.findViewById(R.id.stockFeaturesInfoText)

        val buyButton: Button = view.findViewById(R.id.buyButton)
        val sellButton: Button = view.findViewById(R.id.sellButton)
        val buyAllButton: Button = view.findViewById(R.id.buyAllButton)
        val sellAllButton: Button = view.findViewById(R.id.sellAllButton)

        // 수량 버튼 찾기
        quantityBtn1 = view.findViewById(R.id.quantityBtn1)
        quantityBtn5 = view.findViewById(R.id.quantityBtn5)
        quantityBtn10 = view.findViewById(R.id.quantityBtn10)
        quantityBtn20 = view.findViewById(R.id.quantityBtn20)
        quantityBtn50 = view.findViewById(R.id.quantityBtn50)
        quantityBtn100 = view.findViewById(R.id.quantityBtn100)
        resetQuantityBtn = view.findViewById(R.id.resetQuantityBtn)
        selectedQuantityText = view.findViewById(R.id.selectedQuantityText)

        // 수량 텍스트 초기화
        updateSelectedQuantityText()

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

        // 수량 버튼 클릭 리스너 설정
        setupQuantityButtons()

        buyButton.setOnClickListener {
            selectedStock?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                val totalCost = it.price.toLong() * selectedQuantity
                
                if (selectedQuantity <= 0) {
                    showErrorMessage("거래 수량을 선택해주세요.")
                    return@setOnClickListener
                }
                
                if (currentAsset >= totalCost) {
                    // 다수의 주식 매수 메소드 사용
                    val buyCount = stockViewModel.buyStocks(it, selectedQuantity)
                    assetViewModel.decreaseAsset(totalCost)
                    showMessage("${it.name}을(를) ${buyCount}주 매수했습니다! 보유량: ${it.holding}주")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(it) // 주식 상세 정보 업데이트
                    resetSelectedQuantity() // 거래 후 수량 초기화
                } else {
                    showErrorMessage("자산이 부족합니다! 필요 금액: ${formatCurrency(totalCost)}")
                }
            } ?: showMessage("주식을 선택하세요.")
        }

        sellButton.setOnClickListener {
            selectedStock?.let {
                if (selectedQuantity <= 0) {
                    showErrorMessage("거래 수량을 선택해주세요.")
                    return@setOnClickListener
                }
                
                if (it.holding >= selectedQuantity) {
                    // 다수의 주식 매도 메소드 사용
                    val sellCount = stockViewModel.sellStocks(it, selectedQuantity)
                    val totalGain = it.price.toLong() * sellCount
                    assetViewModel.increaseAsset(totalGain)
                    showMessage("${it.name} ${sellCount}주 매도! 총액: ${formatCurrency(totalGain)}원")
                    stockAdapter.notifyDataSetChanged()
                    updateStockDetails(it) // 주식 상세 정보 업데이트
                    resetSelectedQuantity() // 거래 후 수량 초기화
                } else {
                    showErrorMessage("보유한 주식이 부족합니다! 현재 보유량: ${it.holding}주")
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
                    resetSelectedQuantity() // 거래 후 수량 초기화
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
                    resetSelectedQuantity() // 거래 후 수량 초기화
                } else {
                    showErrorMessage("보유한 주식이 없습니다!")
                }
            } ?: showMessage("주식을 선택하세요.")
        }
    }

    // 수량 버튼 설정
    private fun setupQuantityButtons() {
        quantityBtn1.setOnClickListener { 
            selectedQuantity += 1
            updateSelectedQuantityText()
        }
        
        quantityBtn5.setOnClickListener { 
            selectedQuantity += 5
            updateSelectedQuantityText()
        }
        
        quantityBtn10.setOnClickListener { 
            selectedQuantity += 10
            updateSelectedQuantityText()
        }
        
        quantityBtn20.setOnClickListener { 
            selectedQuantity += 20
            updateSelectedQuantityText()
        }
        
        quantityBtn50.setOnClickListener { 
            selectedQuantity += 50
            updateSelectedQuantityText()
        }
        
        quantityBtn100.setOnClickListener { 
            selectedQuantity += 100
            updateSelectedQuantityText()
        }
        
        resetQuantityBtn.setOnClickListener {
            resetSelectedQuantity()
        }
    }

    // 선택된 수량 텍스트 업데이트
    private fun updateSelectedQuantityText() {
        selectedQuantityText?.text = "$selectedQuantity 주"
    }
    
    // 선택된 수량을 0으로 초기화
    private fun resetSelectedQuantity() {
        selectedQuantity = 0
        updateSelectedQuantityText()
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
        profitRateData?.text = formatPercent(profitRate)
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
    
    /**
     * 구현된 기능 목록을 업데이트합니다.
     */
    private fun updateImplementedFeatures() {
        // 기존 기능 텍스트 업데이트
        featuresInfoText.text = "📌 구현 기능:\n" +
            "- 주식 가격 변동: 3초마다 -0.02%~+0.025% 범위에서 랜덤하게 변경\n" +
            "- 종목별 특성: 종목마다 다른 변동성 특성 (만원:0.8, 이만:0.9, 오만:1.0, 십만:1.1, 이십만:1.3)\n" +
            "- 추세 분석: 최근 가격 이력 기반 상승/하락 추세 반영\n" +
            "- 종목별 대비/등락률 표시: 오늘 시작가 대비 가격 변동 표시\n" +
            "- 호재 이벤트: 30초마다 30% 확률로 2개 주식에 호재 발생 (20초간 0.01%~0.05% 상승만 함)\n" +
            "- 악제 이벤트: 30초마다 30% 확률로 2개 주식에 악제 발생 (20초간 -0.05%~-0.01% 하락만 함)\n" +
            "- 주식 거래 수량 선택: 버튼을 여러 번 눌러 주식 수량 추가 선택 가능"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
