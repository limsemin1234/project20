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
import android.media.MediaPlayer

class StockFragment : BaseFragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockDetailsTextView: LinearLayout
    private lateinit var featuresInfoText: TextView

    private var selectedStock: Stock? = null
    private var selectedQuantity: Int = 0  // 기본 수량을 0으로 설정
    private var isBuyMode: Boolean = true  // 매수 모드 기본값

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

    // 효과음 관련 변수 추가
    private var selectSound: MediaPlayer? = null
    private var buttonSound: MediaPlayer? = null

    // 수량 버튼 참조 변수
    private lateinit var quantityBtn1: Button
    private lateinit var quantityBtn5: Button
    private lateinit var quantityBtn10: Button
    private lateinit var quantityBtn500: Button  // 기존 20주 버튼을 500주로 변경
    private lateinit var quantityBtn50: Button
    private lateinit var quantityBtn100: Button
    
    // 거래 모드 토글 버튼
    private lateinit var buyModeButton: Button  // 기존 buyButton 사용
    private lateinit var sellModeButton: Button  // 기존 sellButton 사용

    // 매수/매도 전체 버튼
    private lateinit var buyAllButton: Button
    private lateinit var sellAllButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stockViewModel = ViewModelProvider(requireActivity())[StockViewModel::class.java]

        // 효과음 초기화
        initSounds()
        
        // 초기화
        initViews(view)
        observeViewModel()

        // 처음에는 기본적으로 첫 번째 주식 선택
        stockViewModel.selectStock(0)
        
        // 주식 기능 설명 텍스트 설정
        updateFeatureInfoText()
    }
    
    override fun onResume() {
        super.onResume()
        // 화면으로 돌아올 때 수량 초기화
        resetSelectedQuantity()
    }
    
    private fun initViews(view: View) {
        // RecyclerView 및 Adapter 초기화
        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockAdapter = StockAdapter(this::onStockClicked, this::showStockGraphDialog)
        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockRecyclerView.adapter = stockAdapter

        // 텍스트뷰 바인딩 - ID를 실제 레이아웃에 맞게 수정
        stockDetailsTextView = view.findViewById(R.id.stockDetailsTextView)
        featuresInfoText = view.findViewById(R.id.stockFeaturesInfoText)
        buyModeButton = view.findViewById(R.id.buyButton)
        sellModeButton = view.findViewById(R.id.sellButton)
        buyAllButton = view.findViewById(R.id.buyAllButton)
        sellAllButton = view.findViewById(R.id.sellAllButton)

        // 수량 버튼 초기화
        quantityBtn1 = view.findViewById(R.id.quantityBtn1)
        quantityBtn5 = view.findViewById(R.id.quantityBtn5)
        quantityBtn10 = view.findViewById(R.id.quantityBtn10)
        quantityBtn500 = view.findViewById(R.id.quantityBtn500)  // ID를 일관되게 변경
        quantityBtn50 = view.findViewById(R.id.quantityBtn50)
        quantityBtn100 = view.findViewById(R.id.quantityBtn100)

        avgPurchasePriceData = view.findViewById(R.id.avgPurchasePriceData)
        profitLossData = view.findViewById(R.id.profitLossData)
        profitRateData = view.findViewById(R.id.profitRateData)
        stockQuantityData = view.findViewById(R.id.stockQuantityData)
        selectedStockName = view.findViewById(R.id.selectedStockName)

        // 매수/매도 모드 버튼 설정
        setupModeButtons()
        
        // 수량 버튼 클릭 리스너 설정
        setupQuantityButtons()

        // 매수/매도 전체 버튼 설정
        setupAllButtons()
    }

    private fun observeViewModel() {
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
    }

    // 매수/매도 모드 버튼 설정
    private fun setupModeButtons() {
        // 초기 모드 설정 (매수 모드가 기본)
        updateModeButtons()
        
        buyModeButton.setOnClickListener {
            if (!isBuyMode) {
                playButtonSound()
                isBuyMode = true
                updateModeButtons()
                // 모드 전환 확인 메시지
                showMessage("매수 모드로 전환했습니다")
            }
        }
        
        sellModeButton.setOnClickListener {
            if (isBuyMode) {
                playButtonSound()
                isBuyMode = false
                updateModeButtons()
                // 모드 전환 확인 메시지
                showMessage("매도 모드로 전환했습니다")
            }
        }
    }
    
    // 매수/매도 모드에 따라 UI 업데이트
    private fun updateModeButtons() {
        if (isBuyMode) {
            // 매수 모드일 때
            buyModeButton.setBackgroundResource(R.drawable.button_active_blue)
            buyModeButton.setTextColor(Color.WHITE)
            sellModeButton.setBackgroundResource(R.drawable.button_inactive)
            sellModeButton.setTextColor(Color.DKGRAY)
        } else {
            // 매도 모드일 때
            buyModeButton.setBackgroundResource(R.drawable.button_inactive)
            buyModeButton.setTextColor(Color.DKGRAY)
            sellModeButton.setBackgroundResource(R.drawable.button_active_red)
            sellModeButton.setTextColor(Color.WHITE)
        }
    }

    // 매수/매도 전체 버튼 설정
    private fun setupAllButtons() {
        buyAllButton.setOnClickListener {
            playButtonSound()
            if (executeTradeWithCheck()) {
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
        }

        sellAllButton.setOnClickListener {
            playButtonSound()
            if (executeTradeWithCheck()) {
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
    }

    // 수량 버튼 설정
    private fun setupQuantityButtons() {
        quantityBtn1.setOnClickListener { 
            playButtonSound()
            executeTradeWithQuantity(1)
        }
        
        quantityBtn5.setOnClickListener { 
            playButtonSound()
            executeTradeWithQuantity(5)
        }
        
        quantityBtn10.setOnClickListener { 
            playButtonSound()
            executeTradeWithQuantity(10)
        }
        
        quantityBtn500.setOnClickListener { 
            playButtonSound()
            executeTradeWithQuantity(500)  // 500주로 사용
        }
        
        quantityBtn50.setOnClickListener { 
            playButtonSound()
            executeTradeWithQuantity(50)
        }
        
        quantityBtn100.setOnClickListener { 
            playButtonSound()
            executeTradeWithQuantity(100)
        }
    }

    // 지정된 수량으로 거래 실행
    private fun executeTradeWithQuantity(quantity: Int) {
        if (executeTradeWithCheck()) {
            if (isBuyMode) {
                executeBuy(quantity)
            } else {
                executeSell(quantity)
            }
        }
    }

    // 거래 전 체크
    private fun executeTradeWithCheck(): Boolean {
        if (selectedStock == null) {
            showMessage("주식을 선택하세요.")
            return false
        }
        return true
    }
    
    // 매수 실행
    private fun executeBuy(quantity: Int) {
        selectedStock?.let {
            val currentAsset = assetViewModel.asset.value ?: 0L
            val totalCost = it.price.toLong() * quantity
            
            if (currentAsset >= totalCost) {
                val buyCount = stockViewModel.buyStocks(it, quantity)
                assetViewModel.decreaseAsset(totalCost)
                showMessage("${it.name}을(를) ${buyCount}주 매수했습니다! 보유량: ${it.holding}주")
                stockAdapter.notifyDataSetChanged()
                updateStockDetails(it) // 주식 상세 정보 업데이트
            } else {
                showErrorMessage("자산이 부족합니다! 필요 금액: ${formatCurrency(totalCost)}")
            }
        }
    }
    
    // 매도 실행
    private fun executeSell(quantity: Int) {
        selectedStock?.let {
            if (it.holding >= quantity) {
                val sellCount = stockViewModel.sellStocks(it, quantity)
                val totalGain = it.price.toLong() * sellCount
                assetViewModel.increaseAsset(totalGain)
                showMessage("${it.name} ${sellCount}주 매도! 총액: ${formatCurrency(totalGain)}원")
                stockAdapter.notifyDataSetChanged()
                updateStockDetails(it) // 주식 상세 정보 업데이트
            } else {
                showErrorMessage("보유한 주식이 부족합니다! 현재 보유량: ${it.holding}주")
            }
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
        // 천단위 구분자를 적용한 평가손익 값을 표시
        val formattedProfitLoss = if (profitLoss >= 0) {
            "+${formatCurrency(profitLoss.toLong())}"
        } else {
            formatCurrency(profitLoss.toLong())
        }
        profitLossData?.text = formattedProfitLoss + "원"
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

    // 주식 선택 처리 함수
    private fun onStockClicked(stock: Stock) {
        playSelectSound()
        selectedStock = stock
        stockViewModel.selectStock(stock)
        updateStockDetails(stock)
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
        val graphUpdateObserver = Observer<MutableList<Stock>> { updatedStockList ->
            updatedStockList.find { it.name == stock.name }?.let { updatedStock ->
                // 변경된 경우에만 그래프 업데이트
                if (updatedStock.price != graphState.lastPrice) {
                    graphState.isTimerProcessing = true
                    updateGraph(lineChart, updatedStock, graphState)
                    graphState.isTimerProcessing = false
                }
            }
        }
        
        // 주식 데이터 변경 감지를 위한 옵저버 등록
        stockViewModel.stockItems.observe(viewLifecycleOwner, graphUpdateObserver)
        
        // 다이얼로그가 닫힐 때 옵저버 제거
        dialog.setOnDismissListener {
            stockViewModel.stockItems.removeObserver(graphUpdateObserver)
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
    
    /**
     * 주식 기능 설명 텍스트를 업데이트합니다.
     */
    private fun updateFeatureInfoText() {
        val featureText = """
            📊 주식 기능 설명
            
            1️⃣ 주식 선택: 목록에서 주식을 선택하여 거래하세요
            2️⃣ 매수/매도 모드: 상단 버튼으로 매수와 매도 모드를 전환하세요
            3️⃣ 거래 수량: 1, 5, 10, 500, 50, 100주 버튼을 선택하여 거래할 수 있어요
            4️⃣ 전체 매수/매도: 보유한 자산으로 최대 매수하거나 보유한 주식을 전체 매도할 수 있어요
            5️⃣ 그래프 보기: 각 주식 항목의 그래프 아이콘을 클릭하면 주가 변동 그래프를 볼 수 있어요
            6️⃣ 호재/악재: 일정 시간마다 호재나 악재가 발생하여 주가가 변동할 수 있어요
            
            👨‍💼 전략적으로 매수/매도하여 수익을 극대화하세요!
        """.trimIndent()
        
        featuresInfoText.text = featureText
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // 효과음 해제
        selectSound?.release()
        buttonSound?.release()
        selectSound = null
        buttonSound = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    // 수량 초기화 메서드 수정
    private fun resetSelectedQuantity() {
        // 선택된 수량 초기화
        selectedQuantity = 0
    }

    /**
     * 효과음을 초기화합니다.
     */
    private fun initSounds() {
        // 주식 선택 효과음
        selectSound = MediaPlayer.create(requireContext(), R.raw.stock_select)
        
        // 버튼 효과음
        buttonSound = MediaPlayer.create(requireContext(), R.raw.stock_button)
    }

    /**
     * 주식 선택 효과음을 재생합니다.
     */
    private fun playSelectSound() {
        selectSound?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }
    }

    /**
     * 버튼 효과음을 재생합니다.
     */
    private fun playButtonSound() {
        buttonSound?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }
    }
}
