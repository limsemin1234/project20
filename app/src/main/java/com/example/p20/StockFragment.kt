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
import android.app.Dialog
import android.view.Window
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class StockFragment : BaseFragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockDetailsTextView: LinearLayout
    private lateinit var featuresInfoText: TextView
    private lateinit var showGraphButton: Button

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

        // 그래프 버튼 추가
        showGraphButton = view.findViewById(R.id.btnShowGraph)
        // 리사이클러뷰 아이템에 그래프 버튼이 추가되어 기존 버튼은 숨김
        showGraphButton.visibility = View.GONE

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
        stockAdapter = StockAdapter(
            stockItems, 
            { stock -> // 아이템 클릭 콜백
                selectedStock = stock
                updateStockDetails(stock) // 주식 상세 정보 업데이트
            },
            { stock -> // 그래프 버튼 클릭 콜백
                showStockGraphDialog(stock)
            }
        )
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
        val infoBuilder = StringBuilder()
        
        infoBuilder.append("■ 향상된 가격 변동 메커니즘\n")
        infoBuilder.append("• 주식 가격이 한 방향으로 연속 5회 이상 변동하면 반대 방향으로의 반동이 발생합니다.\n")
        infoBuilder.append("• 연속 변동 횟수가 증가할수록 반동 확률이 높아집니다.\n")
        infoBuilder.append("• 10회 이상 연속 변동 시 95% 확률로 반동이 발생합니다.\n")
        infoBuilder.append("• 반동 발생 시 20초~40초간 해당 방향으로의 가격 변동이 유도됩니다.\n")
        infoBuilder.append("• 다른 이벤트가 진행 중일 때 반동 조건이 만족되면, 이벤트 종료 후 반동이 발생합니다.\n\n")
        
        infoBuilder.append("■ 기본 주식 변동 시스템\n")
        infoBuilder.append("• 추세 기능이 제거되어 가격 변동이 100% 랜덤으로 이루어집니다.\n")
        infoBuilder.append("• 주식마다 기본 변동성(1.0~1.4)이 다르게 적용됩니다.\n")
        infoBuilder.append("• 기본 변동 범위: -4% ~ +4%\n\n")
        
        // 이벤트 시스템 설명 추가
        infoBuilder.append("■ 주식 이벤트 시스템\n")
        infoBuilder.append("• 개별 종목 이벤트:\n")
        infoBuilder.append("  - 소형 호재/악재: +2%~4% / -4%~-2% (비활성화)\n")
        infoBuilder.append("  - 중형 호재/악재: +3%~6% / -6%~-3% (비활성화)\n")
        infoBuilder.append("  - 대형 호재/악재: +5%~9% / -9%~-5% (비활성화)\n")
        infoBuilder.append("• 시장 전체 이벤트:\n")
        infoBuilder.append("  - 경기 부양/침체: +2%~5% / -5%~-2% (비활성화)\n")
        infoBuilder.append("  - 시장 폭등/폭락: +4%~8% / -8%~-4% (비활성화)\n")
        infoBuilder.append("• 특별 이벤트:\n")
        infoBuilder.append("  - 대박/대폭락 종목: +10%~20% / -20%~-10% (비활성화)\n")
        infoBuilder.append("• 반동 효과가 진행 중일 때는 새로운 이벤트가 적용되지 않습니다.\n")
        
        featuresInfoText.text = infoBuilder.toString()
    }
    
    /**
     * 주식 가격 변동 그래프를 다이얼로그로 표시합니다.
     */
    private fun showStockGraphDialog(stock: Stock) {
        // 커스텀 다이얼로그 생성
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_stock_graph)
        
        // 다이얼로그 제목 설정
        val titleTextView = dialog.findViewById<TextView>(R.id.graphTitleText)
        titleTextView.text = "${stock.name} 가격 변동 그래프"
        
        // 그래프 설정
        val lineChart = dialog.findViewById<LineChart>(R.id.stockLineChart)
        
        // 초기 그래프 설정
        setupStockGraph(lineChart, stock, true)
        
        // 그래프 데이터 상태 추적 객체
        val graphState = GraphUpdateState(stock.name, stock.price)
        
        // 닫기 버튼
        val closeButton = dialog.findViewById<Button>(R.id.btnCloseGraph)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // 그래프 실시간 업데이트를 위한 옵저버
        // LiveData는 값이 변경될 때만 알림을 보냄 (효율적)
        val graphUpdateObserver = Observer<MutableList<Stock>> { updatedStockList ->
            updatedStockList.find { it.name == stock.name }?.let { updatedStock ->
                // 이미 타이머가 처리 중인지 확인
                if (!graphState.isTimerProcessing) {
                    updateGraph(lineChart, updatedStock, graphState)
                }
            }
        }
        
        // 백업 타이머 - LiveData 이벤트를 놓치는 경우를 대비
        val graphUpdateHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val graphUpdateRunnable = object : Runnable {
            override fun run() {
                // 더 이상 매 5초마다 자동으로 데이터 포인트를 추가하지 않음
                // 대신 LiveData 업데이트만 감지하여 변경된 경우에만 그래프 업데이트

                // 다이얼로그가 아직 표시 중이면 다음 타이머 예약
                if (dialog.isShowing) {
                    graphUpdateHandler.postDelayed(this, 5000)
                }
            }
        }
        
        // 주식 데이터 변경 감지를 위한 옵저버 등록 - 가격 변경 시에만 그래프 업데이트
        stockViewModel.stockItems.observe(viewLifecycleOwner, Observer { updatedStockList ->
            updatedStockList.find { it.name == stock.name }?.let { updatedStock ->
                // 변경된 경우에만 그래프 업데이트
                if (updatedStock.price != graphState.lastPrice) {
                    graphState.isTimerProcessing = true
                    updateGraph(lineChart, updatedStock, graphState)
                    graphState.isTimerProcessing = false
                }
            }
        })
        
        // 첫 타이머 예약
        graphUpdateHandler.postDelayed(graphUpdateRunnable, 5000)
        
        // 다이얼로그가 닫힐 때 옵저버 및 핸들러 제거
        dialog.setOnDismissListener {
            stockViewModel.stockItems.removeObserver(graphUpdateObserver)
            graphUpdateHandler.removeCallbacksAndMessages(null)
        }
        
        // 다이얼로그 표시
        dialog.show()
        
        // 다이얼로그 크기 조정
        val window = dialog.window
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    /**
     * 그래프 업데이트 상태를 추적하기 위한 클래스
     */
    private class GraphUpdateState(
        val stockName: String,
        var lastPrice: Int,
        var isTimerProcessing: Boolean = false
    )
    
    /**
     * 그래프 업데이트 공통 로직
     */
    private fun updateGraph(lineChart: LineChart, stock: Stock, state: GraphUpdateState) {
        // 데이터 세트가 있는지 확인
        val data = lineChart.data ?: return
        if (data.dataSetCount == 0) return
        
        // 기존 데이터 세트 가져오기
        val dataSet = data.getDataSetByIndex(0) as? LineDataSet ?: return
        
        // 최신 가격 확인
        val newPrice = stock.price
        
        // 새 데이터 포인트 추가 (가격이 같더라도 항상 추가)
        val newIndex = dataSet.entryCount.toFloat()
        dataSet.addEntry(Entry(newIndex, newPrice.toFloat()))
        
        // 스타일 업데이트
        setupDataSetStyle(dataSet, stock)
        
        // 데이터 업데이트
        data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        
        // 필요한 경우 그래프 범위 업데이트
        lineChart.setVisibleXRangeMaximum(30f)  // 최대 30개 표시
        
        // 가장 최근 항목으로 스크롤
        lineChart.moveViewToX(newIndex)
        
        // 그래프 갱신
        lineChart.invalidate()
        
        // 마지막 가격 업데이트
        state.lastPrice = newPrice
    }
    
    /**
     * 주식 가격 변동 그래프를 설정합니다.
     * @param isInitialSetup 초기 설정인지 여부
     */
    private fun setupStockGraph(lineChart: LineChart, stock: Stock, isInitialSetup: Boolean = true) {
        if (isInitialSetup) {
            // 처음 그래프를 설정할 때만 기본 설정 변경
            lineChart.description.isEnabled = false  // 설명 비활성화
            lineChart.legend.isEnabled = true        // 범례 활성화
            lineChart.setTouchEnabled(true)          // 터치 활성화
            lineChart.isDragEnabled = true           // 드래그 활성화
            lineChart.setScaleEnabled(true)          // 확대/축소 활성화
            
            // X축 설정
            val xAxis = lineChart.xAxis
            xAxis.granularity = 1f                   // 최소 간격
            xAxis.isGranularityEnabled = true        // 간격 강제 적용
            
            // Y축 설정
            val leftAxis = lineChart.axisLeft
            leftAxis.setDrawGridLines(true)          // 그리드 라인 표시
            
            lineChart.axisRight.isEnabled = false    // 오른쪽 Y축 비활성화
            
            // 데이터 세트 초기화
            val entries = ArrayList<Entry>()
            
            // 가격 이력 데이터를 그래프에 추가 (최대 30개까지만)
            val historySize = stock.priceHistory.size
            val startIdx = if (historySize > 30) historySize - 30 else 0
            
            for (i in startIdx until historySize) {
                val price = stock.priceHistory[i]
                entries.add(Entry((i - startIdx).toFloat(), price.toFloat()))
            }
            
            // 이력에 현재 가격이 없다면 현재 가격 추가
            if (entries.isEmpty() || entries.last().y != stock.price.toFloat()) {
                entries.add(Entry(entries.size.toFloat(), stock.price.toFloat()))
            }
            
            // 데이터 세트 생성 및 설정
            val dataSet = LineDataSet(entries, "${stock.name} 가격 추이")
            setupDataSetStyle(dataSet, stock)
            
            // 그래프에 데이터 설정
            val lineData = LineData(dataSet)
            lineChart.data = lineData
            
            // 애니메이션 추가 (부드러운 표시)
            lineChart.animateX(500)
            
            // 그래프 갱신
            lineChart.invalidate()
        }
    }
    
    /**
     * 데이터 세트 스타일 설정
     */
    private fun setupDataSetStyle(dataSet: LineDataSet, stock: Stock) {
        dataSet.color = if (stock.changeValue >= 0) Color.RED else Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 2f                   // 선 두께
        dataSet.setDrawCircles(true)             // 데이터 포인트 표시
        dataSet.setCircleColor(dataSet.color)    // 데이터 포인트 색상
        dataSet.circleRadius = 4f                // 데이터 포인트 크기
        dataSet.setDrawValues(true)              // 값 표시
        
        // 변동성이 큰 경우 색상 변경
        if (stock.volatility > 1.0) {
            dataSet.color = ContextCompat.getColor(requireContext(), R.color.purple_500)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
