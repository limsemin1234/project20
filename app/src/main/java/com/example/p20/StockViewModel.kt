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
import java.util.Timer
import java.util.TimerTask
import android.util.Log
import com.example.p20.MessageManager

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 5000L // 주식 가격 업데이트 간격 (5초)
    
    // 기존 호재 이벤트 설정 (호환성 유지)
    private val positiveNewsInterval = 30000L // 호재 이벤트 체크 간격 (30초)
    private val positiveNewsChance = 0.0 // 호재 발생 확률 (0%로 비활성화)
    private val positiveNewsDuration = 20000L // 호재 지속 시간 (20초)
    
    // 기존 악제 이벤트 설정 (호환성 유지)
    private val negativeNewsInterval = 30000L // 악제 이벤트 체크 간격 (30초)
    private val negativeNewsChance = 0.0 // 악제 발생 확률 (0%로 비활성화)
    private val negativeNewsDuration = 20000L // 악제 지속 시간 (20초)
    
    // 새 이벤트 시스템 설정
    // 이벤트 설정 상수
    private val EVENT_SETTINGS = mapOf(
        // 소형 호재
        StockEventType.POSITIVE_SMALL to EventSettings(
            minRate = 0.02, maxRate = 0.04,
            duration = 15000L, interval = 30000L, 
            chance = 0.0, stockCount = 2     // 이벤트 발생 확률 0으로 설정
        ),
        // 중형 호재
        StockEventType.POSITIVE_MEDIUM to EventSettings(
            minRate = 0.03, maxRate = 0.06,
            duration = 15000L, interval = 45000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0으로 설정
        ),
        // 대형 호재
        StockEventType.POSITIVE_LARGE to EventSettings(
            minRate = 0.05, maxRate = 0.09,
            duration = 18000L, interval = 60000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0으로 설정
        ),
        // 소형 악재
        StockEventType.NEGATIVE_SMALL to EventSettings(
            minRate = -0.04, maxRate = -0.02,
            duration = 15000L, interval = 30000L, 
            chance = 0.0, stockCount = 2     // 이벤트 발생 확률 0으로 설정
        ),
        // 중형 악재
        StockEventType.NEGATIVE_MEDIUM to EventSettings(
            minRate = -0.06, maxRate = -0.03,
            duration = 15000L, interval = 45000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0으로 설정
        ),
        // 대형 악재
        StockEventType.NEGATIVE_LARGE to EventSettings(
            minRate = -0.09, maxRate = -0.05,
            duration = 18000L, interval = 60000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0으로 설정
        ),
        // 경기 부양
        StockEventType.MARKET_BOOM to EventSettings(
            minRate = 0.02, maxRate = 0.05,
            duration = 24000L, interval = 180000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0으로 설정
        ),
        // 경기 침체
        StockEventType.MARKET_RECESSION to EventSettings(
            minRate = -0.05, maxRate = -0.02,
            duration = 24000L, interval = 180000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0으로 설정
        ),
        // 시장 폭등
        StockEventType.MARKET_SURGE to EventSettings(
            minRate = 0.04, maxRate = 0.08,
            duration = 12000L, interval = 300000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0으로 설정
        ),
        // 시장 폭락
        StockEventType.MARKET_CRASH to EventSettings(
            minRate = -0.08, maxRate = -0.04,
            duration = 12000L, interval = 300000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0으로 설정
        )
    )
    
    // 일회성 이벤트 설정
    private val ONE_TIME_EVENT_SETTINGS = mapOf(
        // 대박 종목
        StockEventType.STOCK_SURGE to EventSettings(
            minRate = 0.1, maxRate = 0.2,
            duration = 0L, interval = 600000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0으로 설정
        ),
        // 대폭락 종목
        StockEventType.STOCK_CRASH to EventSettings(
            minRate = -0.2, maxRate = -0.1,
            duration = 0L, interval = 600000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0으로 설정
        )
    )
    
    // 변동성 이벤트 설정
    private val VOLATILITY_EVENT_SETTINGS = mapOf(
        // 변동성 증가
        StockEventType.VOLATILITY_UP to EventSettings(
            minRate = 0.0, maxRate = 0.0,
            duration = 21000L, interval = 420000L, 
            chance = 0.0, stockCount = 0,    // 이벤트 발생 확률 0으로 설정
            volatilityMultiplier = 1.5
        ),
        // 변동성 감소
        StockEventType.VOLATILITY_DOWN to EventSettings(
            minRate = 0.0, maxRate = 0.0,
            duration = 21000L, interval = 420000L, 
            chance = 0.0, stockCount = 0,    // 이벤트 발생 확률 0으로 설정
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
    
    // 가능한 주식 종목 리스트
    private val availableStocks = listOf(
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

    // 직전에 UI 업데이트에 사용된 주식 가격 보관 (중복 업데이트 방지)
    private val lastStockPrices = mutableMapOf<String, Int>()

    init {
        // 저장된 데이터가 있는지 확인 후 초기화 진행
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
            
            // 저장된 데이터 로드
            loadStockData()
        } else {
            // 처음 실행 시 랜덤 종목 생성
            generateRandomStocks()
            
            // 반동 관련 필드 명시적 초기화
            _stockItems.value?.forEach { stock ->
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
        }
        
        initializeEventSystem()
    }
    
    // 저장된 주식 데이터가 있는지 확인하는 메서드
    private fun hasStockData(): Boolean {
        // 첫 번째 주식의 가격과 이름 데이터가 있는지 확인
        return sharedPreferences.contains("price_0") && sharedPreferences.contains("stockName_0")
    }
    
    // 랜덤 주식 종목 생성
    private fun generateRandomStocks() {
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
            stock.reversionActive = false
            stock.reversionDirection = 0
            stock.reversionRemainingMs = 0
            stock.consecutiveMovesInSameDirection = 0
            stock.lastMoveDirection = 0
            
            // 이벤트 초기화
            stock.clearAllEvents()
            
            stockList.add(stock)
        }
        
        _stockItems.value = stockList
    }
    
    // 재시작시 새로운 주식 종목 생성
    fun resetStocksWithNewCompanies() {
        // 가격 추적 초기화
        clearPriceTracking()
        
        // 새로운 랜덤 종목 생성
        generateRandomStocks()
        
        // 기존 데이터 초기화
        _stockItems.value?.forEach { stock ->
            stock.resetHoldings()
            
            // 반동 관련 필드 명시적 초기화
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
        
        // 저장된 데이터 삭제 - 종목 데이터를 모두 삭제
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        
        // 현재 상태 저장 (새로 생성된 종목 저장)
        saveStockData()
        
        // 이벤트 시스템 재초기화
        clearAllEvents()
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
        // 이전에 예약된 모든 작업 제거 (중복 방지)
        handler.removeCallbacksAndMessages(null)
        
        // 주식 가격 업데이트 시스템 시작
        startStockPriceUpdates()
        
        // 새 이벤트 시스템 시작
        startAllEventChecks()
        
        // 기존 호재/악제 시스템은 일단 유지 (호환성)
        startPositiveNewsCheck()
        startNegativeNewsCheck()
    }

    // 주식 가격 변동 시스템 시작
    private fun startStockPriceUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                updateStockPrices()
                handler.postDelayed(this, updateInterval)
            }
        }
        
        // 첫 번째 업데이트는 지정된 간격 후에 시작
        handler.postDelayed(updateRunnable, updateInterval)
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
        _stockItems.value?.forEach { stock ->
            stock.clearAllEvents()
            // 반동 관련 필드도 초기화
            stock.reversionActive = false
            stock.reversionDirection = 0
            stock.reversionRemainingMs = 0
            stock.consecutiveMovesInSameDirection = 0
            stock.lastMoveDirection = 0
        }
        clearPriceTracking()
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
            // 각 주식의 가격 변동 업데이트
            var anyStockChanged = false
            
            // 각 주식마다 가격 변동 계산 및 적용
            stocks.forEach { stock ->
                // 현재 가격 저장
                val currentPrice = stock.price
                
                // 변동값 계산 및 가격 업데이트 - 반동 메시지 받기
                val reversionMessage = stock.updateChangeValue()
                
                // 반동 메시지가 있다면 이벤트 알림 표시
                if (reversionMessage.isNotEmpty()) {
                    // 상태 기록
                    val context = getApplication<Application>()
                    
                    // 반동 이벤트 발생을 알림
                    MessageManager.showMessage(context, "${stock.name}: $reversionMessage")
                }
                
                // 이전 가격과 비교하여 변경 여부 확인
                val lastPrice = lastStockPrices[stock.name]
                if (lastPrice == null || lastPrice != stock.price) {
                    anyStockChanged = true
                    lastStockPrices[stock.name] = stock.price
                }
            }
            
            // 가격이 변경된 경우에만 UI 업데이트 (불필요한 옵저버 트리거 방지)
            if (anyStockChanged) {
                // 기존 리스트와 다른 새 인스턴스를 생성하여 할당
                _stockItems.value = stocks.toMutableList()
            }
        }
    }

    // 가격 이력 추적 맵 초기화 (앱 종료 또는 재시작 시)
    fun clearPriceTracking() {
        lastStockPrices.clear()
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
        // 종목 데이터 저장
        val editor = sharedPreferences.edit()
        
        _stockItems.value?.forEachIndexed { index, stock ->
            // 주식 기본 정보 저장
            editor.putString("stockName_$index", stock.name)
            editor.putInt("price_$index", stock.price)
            editor.putInt("holding_$index", stock.holding)
            
            // 호재/악재 상태 저장
            editor.putBoolean("isPositiveNews_$index", stock.isPositiveNews)
            editor.putBoolean("isNegativeNews_$index", stock.isNegativeNews)
            
            // 변동성 정보 저장
            editor.putFloat("volatility_$index", stock.volatility.toFloat())
            
            // 반동 관련 필드는 앱 재시작 시 항상 초기화하도록 함
            // 그러나 데이터 일관성을 위해 저장은 함
            editor.putBoolean("reversionActive_$index", false)
            editor.putInt("reversionDirection_$index", 0)
            editor.putLong("reversionRemainingMs_$index", 0)
            editor.putInt("consecutiveMovesInSameDirection_$index", 0)
            editor.putInt("lastMoveDirection_$index", 0)
            
            // 가격 이력 저장 (최대 10개까지)
            val historySize = minOf(stock.priceHistory.size, 10)
            editor.putInt("historySize_$index", historySize)
            
            for (i in 0 until historySize) {
                val historyIdx = stock.priceHistory.size - historySize + i
                if (historyIdx >= 0 && historyIdx < stock.priceHistory.size) {
                    editor.putInt("priceHistory_${index}_$i", stock.priceHistory[historyIdx])
                }
            }
            
            // 보유량이 있는 경우 매입가격 설정
            stock.purchasePrices.clear()
            val savedPurchasePrice = stock.getAvgPurchasePrice()
            editor.putInt("purchasePrice_$index", savedPurchasePrice)
        }
        
        editor.apply()
    }

    private fun loadStockData() {
        // 주식 이름이 저장되어 있지 않은 경우 (이전 버전 호환성)
        val hasStockNames = sharedPreferences.contains("stockName_0")
        
        _stockItems.value?.forEachIndexed { index, stock ->
            // 중요: 반동 관련 필드 초기화 - 앱 재시작 시 필요
            stock.reversionActive = false
            stock.reversionDirection = 0
            stock.reversionRemainingMs = 0
            stock.consecutiveMovesInSameDirection = 0
            stock.lastMoveDirection = 0
            
            // 종목명 로드 (이름이 저장되어 있을 경우만)
            if (hasStockNames) {
                val savedName = sharedPreferences.getString("stockName_$index", stock.name) ?: stock.name
                stock.name = savedName
            }
            
            stock.price = sharedPreferences.getInt("price_$index", stock.price)
            stock.holding = sharedPreferences.getInt("holding_$index", stock.holding)
            
            // 호재/악재 상태 로드
            stock.isPositiveNews = sharedPreferences.getBoolean("isPositiveNews_$index", false)
            stock.isNegativeNews = sharedPreferences.getBoolean("isNegativeNews_$index", false)
            
            // 변동성 관련 데이터 로드
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
            
            // 초기 가격 설정 (이상치 감지를 위해)
            if (stock.priceHistory.isNotEmpty()) {
                stock.initialPrice = stock.priceHistory[0]
            } else {
                stock.initialPrice = stock.price
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
        // 가격 중복 방지를 위해 5개의 서로 다른 가격 선택
        val randomPrices = availablePrices.shuffled().take(5)
        
        // 변동성 값 배열
        val volatilityOptions = listOf(1.0, 1.1, 1.2, 1.3, 1.4)
        // 변동성 중복 방지를 위해 섞기
        val randomVolatilities = volatilityOptions.shuffled().take(5)
        
        _stockItems.value?.forEachIndexed { index, stock ->
            // 선택된 가격으로 리셋
            val initialPrice = randomPrices[index]
            
            // 가격 리셋
            stock.resetPrice(initialPrice)
            
            // 변동성 리셋
            stock.volatility = randomVolatilities[index]
        }
        
        // 저장된 데이터 초기화
        sharedPreferences.edit().clear().apply()
        
        // UI 업데이트
        _stockItems.value = _stockItems.value
    }

    // 모든 주식 데이터 초기화 (ResetFragment에서 호출)
    fun resetStocks() {
        resetStocksWithNewCompanies()
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        clearAllEvents()
    }
}
