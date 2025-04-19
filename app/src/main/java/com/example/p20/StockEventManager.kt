package com.example.p20

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 주식 이벤트를 관리하는 클래스
 * - 이벤트 체크 및 발생 로직
 * - 주식 가격 업데이트
 * - 이벤트 타이머 관리
 */
class StockEventManager(
    private val application: Application,
    private val repository: StockRepository
) {
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
            chance = 0.0, stockCount = 2     // 이벤트 발생 확률 0%로 설정
        ),
        // 중형 호재
        StockEventType.POSITIVE_MEDIUM to EventSettings(
            minRate = 0.03, maxRate = 0.06,
            duration = 15000L, interval = 45000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0%로 설정
        ),
        // 대형 호재
        StockEventType.POSITIVE_LARGE to EventSettings(
            minRate = 0.05, maxRate = 0.09,
            duration = 18000L, interval = 60000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0%로 설정
        ),
        // 소형 악재
        StockEventType.NEGATIVE_SMALL to EventSettings(
            minRate = -0.04, maxRate = -0.02,
            duration = 15000L, interval = 30000L, 
            chance = 0.0, stockCount = 2     // 이벤트 발생 확률 0%로 설정
        ),
        // 중형 악재
        StockEventType.NEGATIVE_MEDIUM to EventSettings(
            minRate = -0.06, maxRate = -0.03,
            duration = 15000L, interval = 45000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0%로 설정
        ),
        // 대형 악재
        StockEventType.NEGATIVE_LARGE to EventSettings(
            minRate = -0.09, maxRate = -0.05,
            duration = 18000L, interval = 60000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0%로 설정
        ),
        // 경기 부양
        StockEventType.MARKET_BOOM to EventSettings(
            minRate = 0.02, maxRate = 0.05,
            duration = 24000L, interval = 180000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0%로 설정
        ),
        // 경기 침체
        StockEventType.MARKET_RECESSION to EventSettings(
            minRate = -0.05, maxRate = -0.02,
            duration = 24000L, interval = 180000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0%로 설정
        ),
        // 시장 폭등
        StockEventType.MARKET_SURGE to EventSettings(
            minRate = 0.04, maxRate = 0.08,
            duration = 12000L, interval = 300000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0%로 설정
        ),
        // 시장 폭락
        StockEventType.MARKET_CRASH to EventSettings(
            minRate = -0.08, maxRate = -0.04,
            duration = 12000L, interval = 300000L, 
            chance = 0.0, stockCount = 0     // 이벤트 발생 확률 0%로 설정
        )
    )
    
    // 일회성 이벤트 설정
    private val ONE_TIME_EVENT_SETTINGS = mapOf(
        // 대박 종목
        StockEventType.STOCK_SURGE to EventSettings(
            minRate = 0.1, maxRate = 0.2,
            duration = 0L, interval = 600000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0%로 설정
        ),
        // 대폭락 종목
        StockEventType.STOCK_CRASH to EventSettings(
            minRate = -0.2, maxRate = -0.1,
            duration = 0L, interval = 600000L, 
            chance = 0.0, stockCount = 1     // 이벤트 발생 확률 0%로 설정
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
    
    // 가격 추적 데이터
    private val lastStockPrices = mutableMapOf<String, Int>()
    
    // 콜백
    private var positiveNewsCallback: ((List<String>) -> Unit)? = null
    private var negativeNewsCallback: ((List<String>) -> Unit)? = null
    private var eventCallback: ((StockEvent) -> Unit)? = null
    
    /**
     * 이벤트 발생 콜백 설정
     */
    fun setEventCallback(callback: (StockEvent) -> Unit) {
        eventCallback = callback
    }
    
    /**
     * 호재 이벤트 콜백 설정 (호환성 유지)
     */
    fun setPositiveNewsCallback(callback: (List<String>) -> Unit) {
        positiveNewsCallback = callback
    }
    
    /**
     * 악제 이벤트 콜백 설정 (호환성 유지)
     */
    fun setNegativeNewsCallback(callback: (List<String>) -> Unit) {
        negativeNewsCallback = callback
    }
    
    /**
     * 이벤트 시스템 초기화 및 시작
     */
    fun initializeEventSystem() {
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
    
    /**
     * 주식 가격 업데이트 시스템 시작
     */
    private fun startStockPriceUpdates() {
        val updateRunnable = object : Runnable {
            override fun run() {
                updateStockPrices()
                handler.postDelayed(this, updateInterval)
            }
        }
        
        // 첫 번째 업데이트는 즉시 실행하고, 이후부터 지정된 간격으로 실행
        updateStockPrices() // 즉시 첫 번째 업데이트 실행
        handler.postDelayed(updateRunnable, updateInterval) // 이후부터 정기적 업데이트
    }
    
    /**
     * 주식 가격 업데이트 실행
     */
    private fun updateStockPrices() {
        val stocks = repository.stockItems.value ?: return
        
        // 모든 주식 가격 업데이트
        for (stock in stocks) {
            // 주식 가격 변동 메시지 (반동 발생 시)
            val reversionMessage = stock.updateChangeValue()
            
            // 반동 메시지가 있으면 상단에 표시
            if (reversionMessage.isNotEmpty()) {
                MessageManager.showMessage(application, reversionMessage)
            }
        }
        
        // UI 업데이트
        repository.setStockItems(stocks)
    }
    
    /**
     * 모든 이벤트 초기화
     */
    fun clearAllEvents() {
        val stocks = repository.stockItems.value ?: return
        
        for (stock in stocks) {
            stock.clearAllEvents()
            stock.isPositiveNews = false
            stock.isNegativeNews = false
        }
        
        repository.setStockItems(stocks)
    }
    
    /**
     * 기존 호재 이벤트 체크 시작 (호환성 유지)
     */
    private fun startPositiveNewsCheck() {
        val positiveNewsRunnable = object : Runnable {
            override fun run() {
                checkForPositiveNews()
                handler.postDelayed(this, positiveNewsInterval)
            }
        }
        handler.postDelayed(positiveNewsRunnable, positiveNewsInterval / 2) // 처음 시작 시 지연
    }
    
    /**
     * 기존 악제 이벤트 체크 시작 (호환성 유지)
     */
    private fun startNegativeNewsCheck() {
        val negativeNewsRunnable = object : Runnable {
            override fun run() {
                checkForNegativeNews()
                handler.postDelayed(this, negativeNewsInterval)
            }
        }
        handler.postDelayed(negativeNewsRunnable, positiveNewsInterval) // 호재 이벤트와 시간차를 두기 위한 지연
    }
    
    /**
     * 모든 이벤트 체크 시작
     */
    private fun startAllEventChecks() {
        // 일반 주식 이벤트 체크 스케줄링
        EVENT_SETTINGS.forEach { (eventType, settings) ->
            scheduleEventCheck(eventType, settings)
        }
        
        // 일회성 이벤트 체크 스케줄링
        ONE_TIME_EVENT_SETTINGS.forEach { (eventType, settings) ->
            scheduleEventCheck(eventType, settings)
        }
    }
    
    /**
     * 이벤트 체크 스케줄링
     */
    private fun scheduleEventCheck(
        eventType: StockEventType,
        settings: EventSettings
    ) {
        // 이벤트 체크 Runnable 생성
        val eventCheckRunnable = object : Runnable {
            override fun run() {
                // 확률에 따라 이벤트 발생
                if (Random.nextDouble() < settings.chance) {
                    applyEvent(eventType, when (eventType) {
                        // 일회성 이벤트인 경우
                        StockEventType.STOCK_SURGE, StockEventType.STOCK_CRASH -> 
                            ONE_TIME_EVENT_SETTINGS
                        // 일반 이벤트인 경우
                        else -> EVENT_SETTINGS
                    })
                }
                
                // 다음 체크 예약
                handler.postDelayed(this, settings.interval)
            }
        }
        
        // 초기 체크 예약 (체크 간격의 절반 이후에 첫 체크)
        handler.postDelayed(eventCheckRunnable, settings.interval / 2)
    }
    
    /**
     * 기존 호재 체크 메서드 (호환성 유지)
     */
    private fun checkForPositiveNews() {
        if (Random.nextDouble() < positiveNewsChance) {
            // 호재 발생
            applyPositiveNews()
        }
    }
    
    /**
     * 기존 악제 체크 메서드 (호환성 유지)
     */
    private fun checkForNegativeNews() {
        if (Random.nextDouble() < negativeNewsChance) {
            // 악제 발생
            applyNegativeNews()
        }
    }
    
    /**
     * 일회성 이벤트 적용 (대박/대폭락)
     */
    private fun applyOneTimeEvent(
        eventType: StockEventType,
        settingsMap: Map<StockEventType, EventSettings>
    ) {
        val stocks = repository.stockItems.value ?: return
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
        repository.setStockItems(stocks)
    }
    
    /**
     * 이벤트 적용 (지속성 이벤트)
     */
    private fun applyEvent(
        eventType: StockEventType,
        settingsMap: Map<StockEventType, EventSettings>
    ) {
        val stocks = repository.stockItems.value ?: return
        val settings = settingsMap[eventType] ?: return
        
        // 이벤트 영향을 받을 종목 선택
        val affectedStocks = when {
            // 전체 시장 이벤트는 모든 종목 영향
            settings.stockCount <= 0 -> stocks
            
            // 특정 수의 종목에만 영향
            else -> stocks.shuffled().take(settings.stockCount)
        }
        
        // 반동 효과가 활성화된 종목 제외
        val filteredStocks = affectedStocks.filter { !it.reversionActive }
        
        // 모든 주식이 반동 상태면 이벤트 적용하지 않음
        if (filteredStocks.isEmpty()) {
            return
        }
        
        // 영향받는 종목 이름 목록
        val affectedStockNames = filteredStocks.map { it.name }
        
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
        
        // 이벤트를 종목에 적용 (반동 효과가 없는 종목만)
        filteredStocks.forEach { stock ->
            stock.addEvent(event)
        }
        
        // 콜백 호출
        eventCallback?.invoke(event)
        
        // 이벤트 지속 시간 후 자동 제거
        handler.postDelayed({
            filteredStocks.forEach { stock ->
                // 해당 종목이 반동 효과가 활성화되지 않은 경우에만 이벤트 제거
                if (!stock.reversionActive) {
                    stock.removeEvent(eventType)
                }
            }
            // UI 업데이트
            repository.setStockItems(stocks)
        }, settings.duration)
        
        // UI 업데이트
        repository.setStockItems(stocks)
    }
    
    /**
     * 기존 호재 이벤트 적용 (호환성 유지)
     */
    private fun applyPositiveNews() {
        val stocks = repository.stockItems.value ?: return
        
        // 호재를 적용할 주식 선택 (2개)
        val targetStockCount = 2
        val targetStocks = stocks.shuffled().take(targetStockCount)
        
        // 호재 설정
        targetStocks.forEach { stock ->
            stock.isPositiveNews = true
            stock.isNegativeNews = false
        }
        
        // 알림을 위한 종목 이름 목록
        val affectedStockNames = targetStocks.map { it.name }
        
        // 호재 이벤트 자동 종료
        handler.postDelayed({
            targetStocks.forEach { stock ->
                stock.isPositiveNews = false
            }
            repository.setStockItems(stocks)
        }, positiveNewsDuration)
        
        // 호재 이벤트 콜백 호출
        positiveNewsCallback?.invoke(affectedStockNames)
        
        // UI 업데이트
        repository.setStockItems(stocks)
    }
    
    /**
     * 기존 악제 이벤트 적용 (호환성 유지)
     */
    private fun applyNegativeNews() {
        val stocks = repository.stockItems.value ?: return
        
        // 악제를 적용할 주식 선택 (2개)
        val targetStockCount = 2
        val targetStocks = stocks.shuffled().take(targetStockCount)
        
        // 악제 설정
        targetStocks.forEach { stock ->
            stock.isNegativeNews = true
            stock.isPositiveNews = false
        }
        
        // 알림을 위한 종목 이름 목록
        val affectedStockNames = targetStocks.map { it.name }
        
        // 악제 이벤트 자동 종료
        handler.postDelayed({
            targetStocks.forEach { stock ->
                stock.isNegativeNews = false
            }
            repository.setStockItems(stocks)
        }, negativeNewsDuration)
        
        // 악제 이벤트 콜백 호출
        negativeNewsCallback?.invoke(affectedStockNames)
        
        // UI 업데이트
        repository.setStockItems(stocks)
    }
    
    /**
     * 이벤트 메시지 생성
     */
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
            StockEventType.MARKET_CRASH -> "⚠️⚠️ 시장 폭락! 모든 종목이 큰 폭으로 하락합니다!"
            
            StockEventType.STOCK_SURGE -> "💰💰 $stockNamesText 대박 소식! 주가가 급등하고 있습니다!"
            StockEventType.STOCK_CRASH -> "💸💸 $stockNamesText 악재 발생! 주가가 폭락하고 있습니다!"
        }
    }
    
    /**
     * 가격 추적 초기화
     */
    fun clearPriceTracking() {
        lastStockPrices.clear()
    }
    
    /**
     * 리소스 해제
     */
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        clearAllEvents()
        clearPriceTracking()
    }
} 