package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Job
import kotlin.math.roundToInt
import kotlin.random.Random

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L // 주식 가격 업데이트 간격 (3초)
    
    // 기존 호재 이벤트 설정 (호환성 유지)
    private val positiveNewsInterval = 30000L // 호재 이벤트 체크 간격 (30초)
    private val positiveNewsChance = 0.3 // 호재 발생 확률 (30%)
    private val positiveNewsDuration = 20000L // 호재 지속 시간 (20초)
    
    // 기존 악제 이벤트 설정 (호환성 유지)
    private val negativeNewsInterval = 30000L // 악제 이벤트 체크 간격 (30초)
    private val negativeNewsChance = 0.3 // 악제 발생 확률 (30%)
    private val negativeNewsDuration = 20000L // 악제 지속 시간 (20초)
    
    // 새 이벤트 시스템 설정
    // 이벤트 설정 상수
    private val EVENT_SETTINGS = mapOf(
        // 소형 호재
        StockEventType.POSITIVE_SMALL to EventSettings(
            minRate = 0.01, maxRate = 0.02,
            duration = 15000L, interval = 30000L, 
            chance = 0.25, stockCount = 2
        ),
        // 중형 호재
        StockEventType.POSITIVE_MEDIUM to EventSettings(
            minRate = 0.02, maxRate = 0.04,
            duration = 15000L, interval = 45000L, 
            chance = 0.15, stockCount = 1
        ),
        // 대형 호재
        StockEventType.POSITIVE_LARGE to EventSettings(
            minRate = 0.04, maxRate = 0.07,
            duration = 18000L, interval = 60000L, 
            chance = 0.05, stockCount = 1
        ),
        // 소형 악재
        StockEventType.NEGATIVE_SMALL to EventSettings(
            minRate = -0.02, maxRate = -0.01,
            duration = 15000L, interval = 30000L, 
            chance = 0.25, stockCount = 2
        ),
        // 중형 악재
        StockEventType.NEGATIVE_MEDIUM to EventSettings(
            minRate = -0.04, maxRate = -0.02,
            duration = 15000L, interval = 45000L, 
            chance = 0.15, stockCount = 1
        ),
        // 대형 악재
        StockEventType.NEGATIVE_LARGE to EventSettings(
            minRate = -0.07, maxRate = -0.04,
            duration = 18000L, interval = 60000L, 
            chance = 0.05, stockCount = 1
        ),
        // 경기 부양
        StockEventType.MARKET_BOOM to EventSettings(
            minRate = 0.02, maxRate = 0.03,
            duration = 24000L, interval = 180000L, 
            chance = 0.03, stockCount = 0  // 0은 모든 종목 영향
        ),
        // 경기 침체
        StockEventType.MARKET_RECESSION to EventSettings(
            minRate = -0.03, maxRate = -0.02,
            duration = 24000L, interval = 180000L, 
            chance = 0.03, stockCount = 0
        ),
        // 시장 폭등
        StockEventType.MARKET_SURGE to EventSettings(
            minRate = 0.05, maxRate = 0.08,
            duration = 12000L, interval = 300000L, 
            chance = 0.01, stockCount = 0
        ),
        // 시장 폭락
        StockEventType.MARKET_CRASH to EventSettings(
            minRate = -0.08, maxRate = -0.05,
            duration = 12000L, interval = 300000L, 
            chance = 0.01, stockCount = 0
        )
    )
    
    // 일회성 이벤트 설정
    private val ONE_TIME_EVENT_SETTINGS = mapOf(
        // 대박 종목
        StockEventType.STOCK_SURGE to EventSettings(
            minRate = 0.2, maxRate = 0.3,
            duration = 0L, interval = 600000L, 
            chance = 0.01, stockCount = 1
        ),
        // 대폭락 종목
        StockEventType.STOCK_CRASH to EventSettings(
            minRate = -0.3, maxRate = -0.2,
            duration = 0L, interval = 600000L, 
            chance = 0.01, stockCount = 1
        )
    )
    
    // 변동성 이벤트 설정
    private val VOLATILITY_EVENT_SETTINGS = mapOf(
        // 변동성 증가
        StockEventType.VOLATILITY_UP to EventSettings(
            minRate = 0.0, maxRate = 0.0,
            duration = 21000L, interval = 420000L, 
            chance = 0.02, stockCount = 0,
            volatilityMultiplier = 1.5
        ),
        // 변동성 감소
        StockEventType.VOLATILITY_DOWN to EventSettings(
            minRate = 0.0, maxRate = 0.0,
            duration = 21000L, interval = 420000L, 
            chance = 0.02, stockCount = 0,
            volatilityMultiplier = 0.7
        )
    )
    
    // 이벤트 설정 데이터 클래스
    data class EventSettings(
        val minRate: Double,           // 최소 변동률
        val maxRate: Double,           // 최대 변동률
        val duration: Long,            // 지속 시간(ms)
        val interval: Long,            // 체크 간격(ms)
        val chance: Double,            // 발생 확률
        val stockCount: Int,           // 영향받는 종목 수 (0=전체)
        val volatilityMultiplier: Double = 1.0  // 변동성 승수
    )
    
    private val sharedPreferences = application.getSharedPreferences("stock_data", Context.MODE_PRIVATE)
    
    // 기존 호재/악제 이벤트 콜백 (호환성 유지)
    private var positiveNewsCallback: ((List<String>) -> Unit)? = null
    private var negativeNewsCallback: ((List<String>) -> Unit)? = null
    
    // 새 이벤트 시스템 콜백
    private var eventCallback: ((StockEvent) -> Unit)? = null

    init {
        _stockItems.value = mutableListOf(
            Stock("만원", 10000, 0, 0.0, 0),
            Stock("이만", 20000, 0, 0.0, 0),
            Stock("오만", 50000, 0, 0.0, 0),
            Stock("십만", 100000, 0, 0.0, 0),
            Stock("이십만", 200000, 0, 0.0, 0)
        )

        loadStockData()
        initializeEventSystem()
    }
    
    // 기존 콜백 메서드 (호환성 유지)
    fun setPositiveNewsCallback(callback: (List<String>) -> Unit) {
        positiveNewsCallback = callback
    }
    
    fun setNegativeNewsCallback(callback: (List<String>) -> Unit) {
        negativeNewsCallback = callback
    }
    
    // 새 이벤트 콜백 설정
    fun setEventCallback(callback: (StockEvent) -> Unit) {
        eventCallback = callback
    }
    
    // 시스템 초기화
    private fun initializeEventSystem() {
        startStockPriceUpdates()
        
        // 새 이벤트 시스템 시작
        startAllEventChecks()
        
        // 기존 호재/악제 시스템은 일단 유지 (호환성)
        startPositiveNewsCheck()
        startNegativeNewsCheck()
    }

    private fun startStockPriceUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                updateStockPrices()
                handler.postDelayed(this, updateInterval)
            }
        }
        handler.post(updateRunnable)
    }
    
    // 기존 호재/악제 이벤트 메서드 (호환성 유지)
    private fun startPositiveNewsCheck() {
        val positiveNewsRunnable = object : Runnable {
            override fun run() {
                checkForPositiveNews()
                handler.postDelayed(this, positiveNewsInterval)
            }
        }
        handler.postDelayed(positiveNewsRunnable, positiveNewsInterval / 2) // 처음 시작 시 지연
    }
    
    private fun startNegativeNewsCheck() {
        val negativeNewsRunnable = object : Runnable {
            override fun run() {
                checkForNegativeNews()
                handler.postDelayed(this, negativeNewsInterval)
            }
        }
        handler.postDelayed(negativeNewsRunnable, positiveNewsInterval) // 호재 이벤트와 시간차를 두기 위한 지연
    }
    
    // 새 이벤트 시스템 시작
    private fun startAllEventChecks() {
        // 개별 종목 & 시장 전체 이벤트
        for (eventType in EVENT_SETTINGS.keys) {
            startEventCheck(eventType, EVENT_SETTINGS)
        }
        
        // 일회성 이벤트
        for (eventType in ONE_TIME_EVENT_SETTINGS.keys) {
            startEventCheck(eventType, ONE_TIME_EVENT_SETTINGS, true)
        }
        
        // 변동성 이벤트
        for (eventType in VOLATILITY_EVENT_SETTINGS.keys) {
            startEventCheck(eventType, VOLATILITY_EVENT_SETTINGS)
        }
    }
    
    // 특정 이벤트 체크 시작
    private fun startEventCheck(
        eventType: StockEventType, 
        settingsMap: Map<StockEventType, EventSettings>,
        isOneTime: Boolean = false
    ) {
        val settings = settingsMap[eventType] ?: return
        
        val eventRunnable = object : Runnable {
            override fun run() {
                if (Random.nextDouble() < settings.chance) {
                    if (isOneTime) {
                        applyOneTimeEvent(eventType, settingsMap)
                    } else {
                        applyEvent(eventType, settingsMap)
                    }
                }
                // 다음 체크 예약
                handler.postDelayed(this, settings.interval)
            }
        }
        
        // 시작 시간 랜덤화 (모든 이벤트가 동시에 체크되지 않도록)
        val initialDelay = Random.nextLong(settings.interval / 2)
        handler.postDelayed(eventRunnable, initialDelay)
    }
    
    // 기존 호재/악제 체크 메서드 (호환성 유지)
    private fun checkForPositiveNews() {
        if (Random.nextDouble() < positiveNewsChance) {
            // 30% 확률로 호재 발생
            applyPositiveNews()
        }
    }
    
    private fun checkForNegativeNews() {
        if (Random.nextDouble() < negativeNewsChance) {
            // 30% 확률로 악제 발생
            applyNegativeNews()
        }
    }

    // 일회성 이벤트 적용 (대박/대폭락)
    private fun applyOneTimeEvent(
        eventType: StockEventType,
        settingsMap: Map<StockEventType, EventSettings>
    ) {
        _stockItems.value?.let { stocks ->
            val settings = settingsMap[eventType] ?: return
            
            // 영향 받을 종목 선택 (1개만)
            val stock = stocks.random()
            
            // 변동률 계산
            val changeRate = (settings.minRate..settings.maxRate).random()
            
            // 변동액 계산
            val changeValue = (stock.price * changeRate).roundToInt() * 100
            
            // 직접 가격 변경
            stock.price += changeValue
            stock.changeValue = changeValue
            stock.changeRate = changeRate * 100 // 퍼센트 표시를 위해
            
            // 영향받는 종목 이름 목록
            val affectedStockNames = listOf(stock.name)
            
            // 이벤트 메시지 생성
            val message = generateEventMessage(eventType, affectedStockNames)
            
            // 이벤트 객체 생성 (알림용)
            val event = StockEvent(
                type = eventType,
                minChangeRate = settings.minRate,
                maxChangeRate = settings.maxRate,
                duration = 0, // 일회성
                message = message,
                affectedStockNames = affectedStockNames
            )
            
            // 콜백 호출 (알림 표시)
            eventCallback?.invoke(event)
            
            // UI 업데이트
            _stockItems.value = stocks
        }
    }
    
    // 이벤트 적용 (지속성 이벤트)
    private fun applyEvent(
        eventType: StockEventType,
        settingsMap: Map<StockEventType, EventSettings>
    ) {
        _stockItems.value?.let { stocks ->
            val settings = settingsMap[eventType] ?: return
            
            // 이벤트 영향을 받을 종목 선택
            val affectedStocks = when {
                // 전체 시장 이벤트는 모든 종목 영향
                settings.stockCount <= 0 -> stocks
                
                // 특정 수의 종목에만 영향
                else -> stocks.shuffled().take(settings.stockCount)
            }
            
            // 영향받는 종목 이름 목록
            val affectedStockNames = affectedStocks.map { it.name }
            
            // 이벤트 메시지 생성
            val message = generateEventMessage(eventType, affectedStockNames)
            
            // 이벤트 객체 생성
            val event = StockEvent(
                type = eventType,
                minChangeRate = settings.minRate,
                maxChangeRate = settings.maxRate,
                duration = settings.duration,
                volatilityMultiplier = settings.volatilityMultiplier,
                message = message,
                affectedStockNames = affectedStockNames
            )
            
            // 이벤트를 종목에 적용
            affectedStocks.forEach { stock ->
                stock.addEvent(event)
            }
            
            // 콜백 호출
            eventCallback?.invoke(event)
            
            // 이벤트 지속 시간 후 자동 제거
            handler.postDelayed({
                affectedStocks.forEach { stock ->
                    stock.removeEvent(eventType)
                }
                // UI 업데이트
                _stockItems.value = stocks
            }, settings.duration)
            
            // UI 업데이트
            _stockItems.value = stocks
        }
    }
    
    // 이벤트 메시지 생성
    private fun generateEventMessage(eventType: StockEventType, stockNames: List<String>): String {
        val stockNamesText = stockNames.joinToString(", ")
        
        return when (eventType) {
            StockEventType.POSITIVE_SMALL -> "소형 호재 발생! $stockNamesText 주가 상승 예상!"
            StockEventType.POSITIVE_MEDIUM -> "중형 호재 발생! $stockNamesText 주가 크게 상승 예상!"
            StockEventType.POSITIVE_LARGE -> "대형 호재 발생! $stockNamesText 주가 급등 예상!"
            
            StockEventType.NEGATIVE_SMALL -> "소형 악재 발생! $stockNamesText 주가 하락 예상!"
            StockEventType.NEGATIVE_MEDIUM -> "중형 악재 발생! $stockNamesText 주가 크게 하락 예상!"
            StockEventType.NEGATIVE_LARGE -> "대형 악재 발생! $stockNamesText 주가 급락 예상!"
            
            StockEventType.MARKET_BOOM -> "⭐ 경기 부양 정책 발표! 전체 주가 상승 예상!"
            StockEventType.MARKET_RECESSION -> "⚠️ 경기 침체 조짐! 전체 주가 하락 예상!"
            StockEventType.MARKET_SURGE -> "⭐⭐ 시장 폭등! 모든 종목이 크게 상승합니다!"
            StockEventType.MARKET_CRASH -> "⚠️⚠️ 시장 폭락! 모든 종목이 크게 하락합니다!"
            
            StockEventType.STOCK_SURGE -> "💥 대박 종목 발생! $stockNamesText 주가가 폭등합니다!"
            StockEventType.STOCK_CRASH -> "💥 대폭락 종목 발생! $stockNamesText 주가가 폭락합니다!"
            StockEventType.VOLATILITY_UP -> "📈 시장 변동성 확대! 가격 변동이 더 커집니다!"
            StockEventType.VOLATILITY_DOWN -> "📉 시장 안정화! 가격 변동이 줄어듭니다!"
        }
    }
    
    // 기존 호재/악제 메서드 (호환성 유지)
    private fun applyPositiveNews() {
        _stockItems.value?.let { stocks ->
            // 기존의 호재 영향 초기화
            stocks.filter { it.isPositiveNews }.forEach { it.isPositiveNews = false }
            
            // 악제 영향을 받고 있지 않은 종목들 중에서 선택
            val eligibleStocks = stocks.filter { !it.isNegativeNews }
            
            // 선택 가능한 종목이 2개 이상 있는지 확인
            if (eligibleStocks.size >= 2) {
                // 선택 가능한 종목들 중에서 랜덤하게 2개 선택
                val selectedStocks = eligibleStocks.shuffled().take(2)
                
                // 선택된 주식에 호재 적용
                selectedStocks.forEach { it.isPositiveNews = true }
                
                // 호재 영향 받는 주식 이름 리스트
                val positiveNewsStockNames = selectedStocks.map { it.name }
                
                // 콜백 호출 (Fragment에 알림)
                positiveNewsCallback?.invoke(positiveNewsStockNames)
                
                // 20초 후에 호재 효과 제거
                handler.postDelayed({
                    removePositiveNews()
                }, positiveNewsDuration)
                
                // UI 업데이트
                _stockItems.value = stocks
            }
        }
    }
    
    private fun applyNegativeNews() {
        _stockItems.value?.let { stocks ->
            // 기존의 악제 영향 초기화
            stocks.filter { it.isNegativeNews }.forEach { it.isNegativeNews = false }
            
            // 호재 영향을 받고 있지 않은 종목들 중에서 선택
            val eligibleStocks = stocks.filter { !it.isPositiveNews }
            
            // 선택 가능한 종목이 2개 이상 있는지 확인
            if (eligibleStocks.size >= 2) {
                // 선택 가능한 종목들 중에서 랜덤하게 2개 선택
                val selectedStocks = eligibleStocks.shuffled().take(2)
                
                // 선택된 주식에 악제 적용
                selectedStocks.forEach { it.isNegativeNews = true }
                
                // 악제 영향 받는 주식 이름 리스트
                val negativeNewsStockNames = selectedStocks.map { it.name }
                
                // 콜백 호출 (Fragment에 알림)
                negativeNewsCallback?.invoke(negativeNewsStockNames)
                
                // 20초 후에 악제 효과 제거
                handler.postDelayed({
                    removeNegativeNews()
                }, negativeNewsDuration)
                
                // UI 업데이트
                _stockItems.value = stocks
            }
        }
    }
    
    // 모든 이벤트 정리 (앱 종료 또는 뷰모델 클리어 시)
    fun clearAllEvents() {
        _stockItems.value?.forEach { it.clearAllEvents() }
    }
    
    // 이하 기존 메서드들 유지...
    private fun removePositiveNews() {
        _stockItems.value?.let { stocks ->
            // 호재 영향 제거
            stocks.forEach { it.isPositiveNews = false }
            
            // UI 업데이트
            _stockItems.value = stocks
        }
    }
    
    private fun removeNegativeNews() {
        _stockItems.value?.let { stocks ->
            // 악제 영향 제거
            stocks.forEach { it.isNegativeNews = false }
            
            // UI 업데이트
            _stockItems.value = stocks
        }
    }

    fun updateStockPrices() {
        _stockItems.value?.let { stocks ->
            stocks.forEach { it.updateChangeValue() }
            _stockItems.value = stocks
        }
    }

    fun buyStock(stock: Stock) {
        stock.buyStock()
        saveStockData()
    }

    fun sellStock(stock: Stock) {
        stock.sellStock()
        saveStockData()
    }

    /**
     * 지정된 수량만큼 주식을 매수합니다.
     * @param stock 매수할 주식
     * @param quantity 매수할 수량
     * @return 실제로 매수한 수량
     */
    fun buyStocks(stock: Stock, quantity: Int): Int {
        val buyCount = stock.buyStocks(quantity)
        if (buyCount > 0) {
            saveStockData()
        }
        return buyCount
    }

    /**
     * 지정된 수량만큼 주식을 매도합니다.
     * @param stock 매도할 주식
     * @param quantity 매도할 수량
     * @return 실제로 매도한 수량
     */
    fun sellStocks(stock: Stock, quantity: Int): Int {
        val sellCount = stock.sellStocks(quantity)
        if (sellCount > 0) {
            saveStockData()
        }
        return sellCount
    }

    fun buyAllStock(stock: Stock, currentAsset: Long): Int {
        val buyCount = stock.buyAllStock(currentAsset)
        saveStockData()
        _stockItems.value = _stockItems.value
        return buyCount
    }

    fun sellAllStock(stock: Stock): Int {
        val sellCount = stock.sellAllStock()
        saveStockData()
        _stockItems.value = _stockItems.value
        return sellCount
    }

    fun saveStockData() {
        val editor = sharedPreferences.edit()
        _stockItems.value?.forEachIndexed { index, stock ->
            editor.putInt("price_$index", stock.price)
            editor.putInt("holding_$index", stock.holding)
            editor.putInt("purchasePrice_$index", stock.getAvgPurchasePrice())
            
            // 호재/악제 상태 저장
            editor.putBoolean("isPositiveNews_$index", stock.isPositiveNews)
            editor.putBoolean("isNegativeNews_$index", stock.isNegativeNews)
            
            // 추세 관련 데이터 저장
            editor.putFloat("trendStrength_$index", stock.trendStrength.toFloat())
            editor.putFloat("volatility_$index", stock.volatility.toFloat())
            
            // 가격 이력은 최대 5개만 저장 (효율성을 위해)
            val historySize = minOf(5, stock.priceHistory.size)
            editor.putInt("historySize_$index", historySize)
            for (i in 0 until historySize) {
                val historyIdx = stock.priceHistory.size - historySize + i
                if (historyIdx >= 0 && historyIdx < stock.priceHistory.size) {
                    editor.putInt("priceHistory_${index}_$i", stock.priceHistory[historyIdx])
                }
            }
        }
        editor.apply()
    }

    private fun loadStockData() {
        _stockItems.value?.forEachIndexed { index, stock ->
            stock.price = sharedPreferences.getInt("price_$index", stock.price)
            stock.holding = sharedPreferences.getInt("holding_$index", stock.holding)
            
            // 호재/악제 상태 로드
            stock.isPositiveNews = sharedPreferences.getBoolean("isPositiveNews_$index", false)
            stock.isNegativeNews = sharedPreferences.getBoolean("isNegativeNews_$index", false)
            
            // 추세 관련 데이터 로드
            stock.trendStrength = sharedPreferences.getFloat("trendStrength_$index", 0f).toDouble()
            stock.volatility = sharedPreferences.getFloat("volatility_$index", stock.volatility.toFloat()).toDouble()
            
            // 가격 이력 로드
            val historySize = sharedPreferences.getInt("historySize_$index", 0)
            stock.priceHistory.clear()
            // 항상 현재 가격은 이력에 포함
            stock.priceHistory.add(stock.price)
            
            // 저장된 이력 로드
            for (i in 0 until historySize) {
                val historyPrice = sharedPreferences.getInt("priceHistory_${index}_$i", 0)
                if (historyPrice > 0 && historyPrice != stock.price) {
                    stock.priceHistory.add(historyPrice)
                }
            }
            
            // 보유량이 있는 경우 매입가격 설정
            stock.purchasePrices.clear()
            val savedPurchasePrice = sharedPreferences.getInt("purchasePrice_$index", 0)
            if (savedPurchasePrice > 0 && stock.holding > 0) {
                repeat(stock.holding) {
                    stock.purchasePrices.add(savedPurchasePrice)
                }
            }
        }
        _stockItems.value = _stockItems.value
    }

    fun resetStockPrices() {
        _stockItems.value?.forEach { stock ->
            when (stock.name) {
                "만원" -> stock.price = 10000
                "이만" -> stock.price = 20000
                "오만" -> stock.price = 50000
                "십만" -> stock.price = 100000
                "이십만" -> stock.price = 200000
            }
            stock.holding = 0
            stock.purchasePrices.clear()
            stock.isPositiveNews = false
            stock.isNegativeNews = false
            
            // 추세 관련 데이터 초기화
            stock.trendStrength = 0.0
            stock.priceHistory.clear()
            stock.priceHistory.add(stock.price)
            
            // 주식별 기본 변동성 재설정
            stock.volatility = when(stock.name) {
                "만원" -> 0.8
                "이만" -> 0.9
                "오만" -> 1.0
                "십만" -> 1.1
                "이십만" -> 1.3
                else -> 1.0
            }
        }
        saveStockData()
        _stockItems.value = _stockItems.value
    }

    fun resetStocks() {
        _stockItems.value = mutableListOf(
            Stock("만원", 10000, 0, 0.0, 0),
            Stock("이만", 20000, 0, 0.0, 0),
            Stock("오만", 50000, 0, 0.0, 0),
            Stock("십만", 100000, 0, 0.0, 0),
            Stock("이십만", 200000, 0, 0.0, 0)
        )
        saveStockData()
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        clearAllEvents()
    }
}
