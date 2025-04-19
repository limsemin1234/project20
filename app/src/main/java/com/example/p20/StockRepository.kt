package com.example.p20

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 주식 데이터를 관리하는 Repository 클래스
 * - 데이터 저장/로드
 * - 초기 주식 생성
 * - 주식 데이터 관리
 */
class StockRepository(private val application: Application) {
    
    private val sharedPreferences: SharedPreferences = 
        application.getSharedPreferences("stock_data", Context.MODE_PRIVATE)
    
    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems
    
    // 가능한 주식 종목 리스트
    val availableStocks = listOf(
        // 대형주
        StockInfo("강룡전자", 65000, "대형주"),
        StockInfo("천마반도", 120000, "대형주"),
        StockInfo("용마에너지", 450000, "대형주"),
        StockInfo("청룡바이오", 50000, "대형주"),
        StockInfo("봉황통신", 180000, "대형주"),
        StockInfo("백호전기", 180000, "대형주"),
        StockInfo("청솔모바일", 80000, "대형주"),
        StockInfo("태양기술", 300000, "대형주"),
        StockInfo("명월금융", 54000, "대형주"),
        StockInfo("천일건설", 38000, "대형주"),
        
        // 중형주
        StockInfo("금강철강", 28000, "중형주"),
        StockInfo("비단소재", 72000, "중형주"),
        StockInfo("해룡조선", 150000, "중형주"),
        StockInfo("동해화학", 220000, "중형주"),
        StockInfo("호랑바이오", 780000, "중형주"),
        StockInfo("산맥물산", 34000, "중형주"),
        StockInfo("푸른에너지", 42000, "중형주"),
        StockInfo("초록제약", 68000, "중형주"),
        StockInfo("자연식품", 93000, "중형주"),
        StockInfo("미래모빌", 55000, "중형주"),
        
        // 소형주
        StockInfo("샛별정밀", 23000, "소형주"),
        StockInfo("새솔기술", 48000, "소형주"),
        StockInfo("달빛전자", 370000, "소형주"),
        StockInfo("별빛반도", 520000, "소형주"),
        StockInfo("바다물산", 140000, "소형주"),
        StockInfo("구름소프트", 15000, "소형주"),
        StockInfo("소나정보", 19000, "소형주"),
        StockInfo("하늘통신", 27000, "소형주"),
        StockInfo("나무소재", 32000, "소형주"),
        StockInfo("푸름제약", 41000, "소형주")
    )
    
    // 사용자가 지정한 주식 가격 목록
    private val availablePrices = listOf(
        20000, 30000, 40000, 50000, 60000,
        70000, 80000, 90000, 100000, 120000, 150000, 200000
    )
    
    // 초기 주식 생성 시 사용될 가격 목록
    private val randomPricePool = listOf(
        30000, 40000, 50000, 60000, 70000, 
        80000, 90000, 100000, 120000, 150000, 200000
    )
    
    // 주식 정보 데이터 클래스
    data class StockInfo(
        val name: String,
        val initialPrice: Int,
        val category: String
    )
    
    init {
        // 초기화 - 데이터 로드 또는 생성
        initializeStockItems()
    }
    
    /**
     * 초기화 - 저장된 데이터가 있으면 로드, 없으면 새로 생성
     */
    private fun initializeStockItems() {
        if (hasStockData()) {
            // 임시로 빈 리스트로 초기화
            _stockItems.value = mutableListOf(
                Stock("주식1", 10000, 0, 0.0, 0),
                Stock("주식2", 20000, 0, 0.0, 0),
                Stock("주식3", 50000, 0, 0.0, 0),
                Stock("주식4", 100000, 0, 0.0, 0),
                Stock("주식5", 200000, 0, 0.0, 0)
            )
            
            // 반동 관련 필드 명시적 초기화
            _stockItems.value?.forEach { stock ->
                initializeStockProperties(stock)
            }
            
            // 저장된 데이터 로드
            loadStockData()
        } else {
            // 처음 실행 시 랜덤 종목 생성
            generateRandomStocks()
            
            // 반동 관련 필드 명시적 초기화
            _stockItems.value?.forEach { stock ->
                initializeStockProperties(stock)
            }
        }
    }
    
    /**
     * 주식 객체 속성 초기화
     */
    private fun initializeStockProperties(stock: Stock) {
        stock.reversionActive = false
        stock.reversionDirection = 0
        stock.reversionRemainingMs = 0
        stock.consecutiveMovesInSameDirection = 0
        stock.lastMoveDirection = 0
        
        // 이벤트 초기화
        stock.clearAllEvents()
        
        // 초기 가격 저장
        if (stock.priceHistory.isNotEmpty()) {
            stock.initialPrice = stock.priceHistory[0]
        } else {
            stock.initialPrice = stock.price
        }
    }
    
    /**
     * 저장된 주식 데이터가 있는지 확인
     */
    fun hasStockData(): Boolean {
        return sharedPreferences.contains("price_0") && sharedPreferences.contains("stockName_0")
    }
    
    /**
     * 저장된 주식 데이터 로드
     */
    fun loadStockData() {
        try {
            val stocks = _stockItems.value ?: return
            
            // 저장된 주식 데이터 로드
            for (i in stocks.indices) {
                val price = sharedPreferences.getInt("price_$i", 0)
                val name = sharedPreferences.getString("stockName_$i", "") ?: ""
                val holding = sharedPreferences.getInt("holding_$i", 0)
                val changeValue = sharedPreferences.getInt("changeValue_$i", 0)
                val changeRate = sharedPreferences.getFloat("changeRate_$i", 0f).toDouble()
                
                // 구매 이력 로드
                val purchaseCount = sharedPreferences.getInt("purchaseCount_$i", 0)
                val purchasePrices = mutableListOf<Int>()
                for (j in 0 until purchaseCount) {
                    val purchasePrice = sharedPreferences.getInt("purchasePrice_${i}_$j", 0)
                    purchasePrices.add(purchasePrice)
                }
                
                // 주식 객체 업데이트
                if (i < stocks.size && price > 0 && name.isNotEmpty()) {
                    stocks[i].price = price
                    stocks[i].name = name
                    stocks[i].holding = holding
                    stocks[i].changeValue = changeValue
                    stocks[i].changeRate = changeRate
                    stocks[i].purchasePrices.clear()
                    stocks[i].purchasePrices.addAll(purchasePrices)
                    
                    // 변동성 로드
                    val volatility = sharedPreferences.getFloat("volatility_$i", 1.0f).toDouble()
                    stocks[i].volatility = volatility
                    
                    // 가격 이력 로드
                    val historySize = sharedPreferences.getInt("historySize_$i", 0)
                    stocks[i].priceHistory.clear()
                    for (j in 0 until historySize) {
                        val historyPrice = sharedPreferences.getInt("history_${i}_$j", 0)
                        stocks[i].priceHistory.add(historyPrice)
                    }
                    
                    // 초기 가격 로드
                    stocks[i].initialPrice = sharedPreferences.getInt("initialPrice_$i", price)
                }
            }
            
            _stockItems.value = stocks
        } catch (e: Exception) {
            Log.e("StockRepository", "주식 데이터 로드 오류: ${e.message}")
        }
    }
    
    /**
     * 현재 주식 데이터 저장
     */
    fun saveStockData() {
        try {
            val stocks = _stockItems.value ?: return
            val editor = sharedPreferences.edit()
            
            for (i in stocks.indices) {
                val stock = stocks[i]
                
                editor.putInt("price_$i", stock.price)
                editor.putString("stockName_$i", stock.name)
                editor.putInt("holding_$i", stock.holding)
                editor.putInt("changeValue_$i", stock.changeValue)
                editor.putFloat("changeRate_$i", stock.changeRate.toFloat())
                
                // 구매 이력 저장
                editor.putInt("purchaseCount_$i", stock.purchasePrices.size)
                for (j in stock.purchasePrices.indices) {
                    editor.putInt("purchasePrice_${i}_$j", stock.purchasePrices[j])
                }
                
                // 변동성 저장
                editor.putFloat("volatility_$i", stock.volatility.toFloat())
                
                // 가격 이력 저장
                editor.putInt("historySize_$i", stock.priceHistory.size)
                for (j in stock.priceHistory.indices) {
                    editor.putInt("history_${i}_$j", stock.priceHistory[j])
                }
                
                // 초기 가격 저장
                editor.putInt("initialPrice_$i", stock.initialPrice)
            }
            
            editor.apply()
        } catch (e: Exception) {
            Log.e("StockRepository", "주식 데이터 저장 오류: ${e.message}")
        }
    }
    
    /**
     * 랜덤 주식 종목 생성
     */
    fun generateRandomStocks() {
        // 종목명 중복 방지를 위해 랜덤하게 5개 선택
        val randomStocks = availableStocks.shuffled().take(5)
        
        // 가격 선택 - 첫 번째는 2만원 고정, 나머지는 랜덤하게 4개 선택
        val fixedPrice = 20000
        val randomPrices = randomPricePool.shuffled().take(4).toMutableList()
        randomPrices.add(0, fixedPrice) // 첫 번째 위치에 2만원 삽입
        
        // 변동성 값 배열
        val volatilityOptions = listOf(1.0, 1.1, 1.2, 1.3, 1.4)
        // 변동성 중복 방지를 위해 섞기
        val randomVolatilities = volatilityOptions.shuffled().take(5)
        
        // 주식 객체 생성
        val stockList = mutableListOf<Stock>()
        for (i in 0 until 5) {
            val stock = Stock(randomStocks[i].name, randomPrices[i], 0, 0.0, 0)
            
            // 기본 변동성 설정 (랜덤 값 사용)
            stock.volatility = randomVolatilities[i]
            
            // 반동 관련 필드 초기화
            initializeStockProperties(stock)
            
            stockList.add(stock)
        }
        
        _stockItems.value = stockList
    }
    
    /**
     * 저장된 주식 데이터 삭제
     */
    fun clearStockData() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
    
    /**
     * 주식 데이터 설정
     */
    fun setStockItems(stocks: MutableList<Stock>) {
        _stockItems.value = stocks
    }
    
    /**
     * 재시작시 새로운 주식 종목 생성
     */
    fun resetStocksWithNewCompanies() {
        // 새로운 랜덤 종목 생성
        generateRandomStocks()
        
        // 기존 데이터 초기화
        _stockItems.value?.forEach { stock ->
            stock.resetHoldings()
            initializeStockProperties(stock)
        }
        
        // 저장된 데이터 삭제
        clearStockData()
        
        // 현재 상태 저장 (새로 생성된 종목 저장)
        saveStockData()
    }
    
    /**
     * 주식 초기화 (가격은 유지)
     */
    fun resetStocks() {
        _stockItems.value?.forEach { stock ->
            stock.resetHoldings()
        }
        saveStockData()
    }
} 