package com.example.p20

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

data class Stock(
    var name: String,        // 주식 이름
    var price: Int,          // 주식 가격
    var changeValue: Int,    // 변동값
    var changeRate: Double,  // 변동률
    var holding: Int,        // 보유량
    val purchasePrices: MutableList<Int> = mutableListOf(), // 매입 가격 리스트
    var isPositiveNews: Boolean = false, // 호재 영향 여부
    var isNegativeNews: Boolean = false, // 악제 영향 여부
    
    // 향상된 가격 변동 알고리즘을 위한 추가 필드
    val priceHistory: MutableList<Int> = mutableListOf(), // 가격 변동 이력
    var trendStrength: Double = 0.0,     // 현재 추세 강도 (-1.0 ~ 1.0)
    var volatility: Double = 1.0,         // 기본 변동성 계수
    
    // 새 이벤트 시스템을 위한 필드
    val activeEvents: MutableMap<StockEventType, StockEvent> = mutableMapOf(), // 활성화된 이벤트 맵

    // 반동 메커니즘을 위한 필드
    var consecutiveMovesInSameDirection: Int = 0, // 같은 방향으로 연속 변동 횟수
    var lastMoveDirection: Int = 0,              // 마지막 변동 방향 (1: 상승, -1: 하락, 0: 변동 없음)
    
    // 반동 효과의 지속 시간을 위한 필드
    var reversionActive: Boolean = false,        // 현재 반동 효과가 활성화되어 있는지
    var reversionDirection: Int = 0,             // 반동의 방향 (1: 상승 유도, -1: 하락 유도)
    var reversionRemainingMs: Long = 0         // 반동 효과 남은 지속 시간 (ms 단위)
) {
    // 이력 최대 크기
    private val MAX_HISTORY_SIZE = 30
    
    // 추세 영향력 가중치 (0.0 ~ 1.0)
    private val TREND_WEIGHT = 0.4
    
    // 랜덤 변동 가중치
    private val RANDOM_WEIGHT = 0.6
    
    // 반동 확률 기본값
    private val BASE_REVERSION_PROBABILITY = 0.3
    
    // 반동 강도 가중치 (연속 변동 횟수에 따라 증가)
    private val REVERSION_STRENGTH_MULTIPLIER = 0.15
    
    // 반동 확률 최대치 (기존 0.85에서 1.0으로 변경)
    private val MAX_REVERSION_PROBABILITY = 1.0
    
    // 반동 효과 지속 시간 (ms 단위)
    private val REVERSION_DURATION_SMALL = 15000L  // 소형 호재/악재 지속 시간 (15초)
    private val REVERSION_DURATION_MEDIUM = 15000L // 중형 호재/악재 지속 시간 (15초)
    private val REVERSION_DURATION_LARGE = 18000L  // 대형 호재/악재 지속 시간 (18초)
    
    // 가격 업데이트 간격 (ms 단위)
    private val UPDATE_INTERVAL = 5000L   // 가격 업데이트 간격 (5초)
    
    // 가격 이상치 감지 비율 (초기 가격의 몇 배 이상이면 강제 반동 발생)
    private val PRICE_ANOMALY_THRESHOLD = 2.0  // 초기 가격의 2배 이상
    
    // 초기 가격 저장 (이상치 감지용)
    private var initialPrice: Int = 0
    
    init {
        // 초기 가격을 이력에 추가
        priceHistory.add(price)
        
        // 주식별 기본 변동성 설정 - 사용자 지정 가격 범위에 맞게 조정
        volatility = when {
            price <= 30000 -> 0.9    // 저가주는 안정적
            price <= 100000 -> 1.0   // 중간 가격은 보통
            price > 100000 -> 1.1   // 고가주는 변동성 큼
            else -> 1.0   // 기본값
        }
        
        // 초기 가격 저장 (이상치 감지용)
        initialPrice = price
    }
    
    /**
     * 주식 가격 변동값을 계산하고 업데이트합니다.
     * @return 반동 발생 시 메시지, 없으면 빈 문자열
     */
    fun updateChangeValue(): String {
        // 현재 가격이 이미 이력에 있는지 확인
        val shouldAddToHistory = priceHistory.isEmpty() || priceHistory.last() != price
        
        // 가격이 다를 때만 이력에 추가
        if (shouldAddToHistory) {
            priceHistory.add(price)
            if (priceHistory.size > MAX_HISTORY_SIZE) {
                priceHistory.removeAt(0)
            }
        }
        
        // 현재 추세 계산
        calculateTrend()
        
        var minChangePercent = -0.02  // 기본 최소 변동률 (-0.02%)
        var maxChangePercent = 0.02   // 기본 최대 변동률 (0.02%)
        var currentVolatility = volatility  // 기본 변동성
        
        // 반동 메시지를 저장할 변수
        var reversionMessage = ""
        
        // 반동 메커니즘이 활성화되어 있지 않을 때만 이벤트 효과 적용
        if (!reversionActive) {
            // 기존 호재/악재 이벤트 처리 (이전 버전과의 호환성)
            when {
                isPositiveNews -> {
                    minChangePercent = 0.01 // 최소 0.01% 상승
                    maxChangePercent = 0.05 // 최대 0.05% 상승
                }
                isNegativeNews -> {
                    minChangePercent = -0.05 // 최소 0.05% 하락
                    maxChangePercent = -0.01 // 최대 0.01% 하락
                }
            }
            
            // 새 이벤트 시스템 - 활성화된 이벤트들의 효과 적용
            if (activeEvents.isNotEmpty()) {
                // 변동성 이벤트 처리 (VOLATILITY_UP, VOLATILITY_DOWN)
                for (event in activeEvents.values) {
                    if (event.type == StockEventType.VOLATILITY_UP || 
                        event.type == StockEventType.VOLATILITY_DOWN) {
                        currentVolatility *= event.volatilityMultiplier
                    }
                }
                
                // 가장 우선순위 높은 가격 변동 이벤트 찾기
                val priceEvent = activeEvents.values.filter { 
                    it.type != StockEventType.VOLATILITY_UP && 
                    it.type != StockEventType.VOLATILITY_DOWN 
                }.maxByOrNull { 
                    // 변동폭이 클수록 우선순위 높음
                    (it.maxChangeRate - it.minChangeRate).absoluteValue
                }
                
                // 가격 변동 이벤트 적용
                priceEvent?.let {
                    minChangePercent = it.minChangeRate
                    maxChangePercent = it.maxChangeRate
                }
            }
        }
        
        // 반동 메커니즘 처리
        if (reversionActive) {
            // 이미 반동 효과가 활성화되어 있는 경우 - 적용 후 카운트다운
            val (adjustedMin, adjustedMax) = applyActiveReversion(minChangePercent, maxChangePercent)
            minChangePercent = adjustedMin
            maxChangePercent = adjustedMax
            
            // 남은 지속 시간 감소 (5000ms = 5초마다 호출되므로)
            reversionRemainingMs -= UPDATE_INTERVAL
            
            // 지속 시간이 끝났으면 반동 효과 종료
            if (reversionRemainingMs <= 0) {
                reversionActive = false
                reversionDirection = 0
            }
        } else {
            // 반동 효과가 활성화되어 있지 않은 경우 - 새로운 반동 효과 발생 여부 체크
            reversionMessage = checkAndStartReversion()
        }
        
        // 랜덤 변동 요소 계산
        val randomPercent = (minChangePercent..maxChangePercent).random()
        
        // 추세 요소 (trendStrength는 -1.0 ~ 1.0 사이)
        val trendPercent = trendStrength * 0.05 // 추세 크기 조정
        
        // 최종 변동률 계산 (랜덤 요소 + 추세 요소)
        val finalChangePercent = (randomPercent * RANDOM_WEIGHT) + (trendPercent * TREND_WEIGHT)
        
        // 변동성 적용
        val adjustedChangePercent = finalChangePercent * currentVolatility
        
        // 변동값 계산 (100원 단위로 반올림)
        val calculatedChange = (price * adjustedChangePercent / 100.0).roundToInt() * 100
        
        // 반동이 활성화된 경우 변동 방향 보정
        changeValue = if (reversionActive) {
            when (reversionDirection) {
                1 -> { // 상승 유도
                    // 최소 100원 이상 상승하도록 보정
                    maxOf(calculatedChange, 100)
                }
                -1 -> { // 하락 유도
                    // 최소 100원 이상 하락하도록 보정
                    minOf(calculatedChange, -100)
                }
                else -> calculatedChange
            }
        } else {
            calculatedChange
        }

        // 변동값이 0인 경우에 최소 변동 보장
        if (changeValue == 0 && price >= 1000) {
            changeValue = if (reversionActive) {
                // 반동 중에는 반동 방향에 따라 변동
                when (reversionDirection) {
                    1 -> 100  // 상승 유도
                    -1 -> -100 // 하락 유도
                    else -> if (Random.nextBoolean()) 100 else -100
                }
            } else {
                // 일반적인 경우 - 호재/악재에 따라 변동
                when {
                    isPositiveNews || activeEvents.values.any { it.minChangeRate > 0 } -> 100
                    isNegativeNews || activeEvents.values.any { it.maxChangeRate < 0 } -> -100
                    else -> if (Random.nextBoolean()) 100 else -100
                }
            }
        }

        // 변동 방향 저장
        val currentDirection = changeValue.sign
        
        // 반동 효과가 활성화되어 있지 않을 때만 연속 변동 카운터 업데이트
        if (!reversionActive) {
            if (currentDirection == lastMoveDirection && currentDirection != 0) {
                consecutiveMovesInSameDirection++
            } else if (currentDirection != 0) {
                // 방향이 바뀌었거나 처음 변동
                consecutiveMovesInSameDirection = 1
                lastMoveDirection = currentDirection
            }
        }

        updatePriceAndChangeValue()
        
        return reversionMessage
    }

    /**
     * 반동 메커니즘 발생 조건을 확인하고, 조건이 만족되면 반동 효과 활성화
     * @return 반동 이벤트 메시지 (반동이 발생하지 않으면 빈 문자열)
     */
    private fun checkAndStartReversion(): String {
        // 가격 이상치 검사 - 초기 가격의 일정 비율을 넘어가면 강제 반동 적용
        val priceRatio = price.toDouble() / initialPrice.toDouble()
        
        // 가격이 초기 가격의 PRICE_ANOMALY_THRESHOLD 배 이상이면 강제 하락 반동
        if (priceRatio >= PRICE_ANOMALY_THRESHOLD) {
            return applyForcedReversion(-1)  // 하락 반동 강제 적용
        }
        
        // 가격이 초기 가격의 1/PRICE_ANOMALY_THRESHOLD 이하면 강제 상승 반동
        if (initialPrice > 0 && price.toDouble() / initialPrice.toDouble() <= 1.0 / PRICE_ANOMALY_THRESHOLD) {
            return applyForcedReversion(1)  // 상승 반동 강제 적용
        }
        
        // 연속 변동이 3회 미만이면 반동 효과 적용하지 않음
        if (consecutiveMovesInSameDirection < 3 || lastMoveDirection == 0) {
            return ""
        }

        // 연속 상승/하락 횟수에 따라 반동 확률 증가
        val reversionProbability = BASE_REVERSION_PROBABILITY + 
                                 (consecutiveMovesInSameDirection - 2) * REVERSION_STRENGTH_MULTIPLIER
        
        // 최대 100%로 제한
        val cappedProbability = minOf(reversionProbability, MAX_REVERSION_PROBABILITY)
        
        // 특별 조건: 8회 이상 연속 변동 시 100% 반동 발생
        if (consecutiveMovesInSameDirection >= 8) {
            return applyForcedReversion(-lastMoveDirection)
        }
        
        // 반동 확률에 따라 반동 효과 활성화
        if (Random.nextDouble() < cappedProbability) {
            return applyReversion(-lastMoveDirection)
        }
        
        return ""
    }
    
    /**
     * 반동 효과를 활성화합니다.
     * @param direction 반동 방향 (1: 상승 유도, -1: 하락 유도)
     * @return 반동 이벤트 메시지
     */
    private fun applyReversion(direction: Int): String {
        reversionActive = true
        reversionDirection = direction
        
        // 반동 효과는 호재/악재 이벤트 지속 시간과 동일하게 설정
        // 소형/중형/대형 호재/악재 랜덤 선택 시 해당 이벤트의 지속 시간 적용
        val eventType = if (direction == 1) {
            // 상승 반동일 때 호재 이벤트 적용
            when (Random.nextInt(3)) {
                0 -> StockEventType.POSITIVE_SMALL
                1 -> StockEventType.POSITIVE_MEDIUM
                else -> StockEventType.POSITIVE_LARGE
            }
        } else {
            // 하락 반동일 때 악재 이벤트 적용
            when (Random.nextInt(3)) {
                0 -> StockEventType.NEGATIVE_SMALL
                1 -> StockEventType.NEGATIVE_MEDIUM
                else -> StockEventType.NEGATIVE_LARGE
            }
        }
        
        // 이벤트 타입에 따른 지속 시간 설정
        reversionRemainingMs = when (eventType) {
            StockEventType.POSITIVE_SMALL, StockEventType.NEGATIVE_SMALL -> REVERSION_DURATION_SMALL
            StockEventType.POSITIVE_MEDIUM, StockEventType.NEGATIVE_MEDIUM -> REVERSION_DURATION_MEDIUM
            StockEventType.POSITIVE_LARGE, StockEventType.NEGATIVE_LARGE -> REVERSION_DURATION_LARGE
            else -> REVERSION_DURATION_MEDIUM // 기본값
        }
        
        // 연속 변동 카운터만 초기화 (lastMoveDirection은 유지)
        consecutiveMovesInSameDirection = 0
        
        // 반동 방향에 따라 이벤트 추가
        return if (direction == 1) {
            // 상승 반동일 때 호재 이벤트 적용
            applyPositiveEventForReversion(eventType = eventType)
        } else if (direction == -1) {
            // 하락 반동일 때 악재 이벤트 적용
            applyNegativeEventForReversion(eventType = eventType)
        } else {
            "반동 효과 발생"
        }
    }
    
    /**
     * 강제 반동 효과를 적용합니다 (이상치 감지시)
     * @param direction 반동 방향 (1: 상승 유도, -1: 하락 유도)
     * @return 반동 이벤트 메시지
     */
    private fun applyForcedReversion(direction: Int): String {
        reversionActive = true
        reversionDirection = direction
        
        // 강제 반동은 대형 호재/악재 지속 시간의 2배로 설정
        reversionRemainingMs = REVERSION_DURATION_LARGE * 2
        
        // 연속 변동 카운터 초기화
        consecutiveMovesInSameDirection = 0
        
        // 반동 방향에 따라 이벤트 추가 (강제 반동은 더 강한 이벤트 적용)
        return if (direction == 1) {
            // 상승 반동일 때 대형 호재 이벤트 적용
            applyPositiveEventForReversion(
                isForced = true, 
                eventType = StockEventType.POSITIVE_LARGE
            )
        } else if (direction == -1) {
            // 하락 반동일 때 대형 악재 이벤트 적용
            applyNegativeEventForReversion(
                isForced = true,
                eventType = StockEventType.NEGATIVE_LARGE
            )
        } else {
            "강제 반동 효과 발생"
        }
    }
    
    /**
     * 상승 반동에 대한 호재 이벤트 적용
     * @param isForced 강제 반동 여부 (true일 경우 더 강한 이벤트 적용)
     * @param eventType 적용할 이벤트 타입 (지정되지 않으면 랜덤 선택)
     * @return 반동 이벤트 메시지
     */
    private fun applyPositiveEventForReversion(
        isForced: Boolean = false,
        eventType: StockEventType? = null
    ): String {
        // 기존 호재/악재 효과 제거
        isPositiveNews = false
        isNegativeNews = false
        
        // 기존 이벤트 제거 (반동 이벤트와 충돌 방지)
        clearAllEvents()
        
        // 호재 타입 선택
        val finalEventType = eventType ?: if (isForced) {
            StockEventType.POSITIVE_LARGE
        } else {
            // 무작위로 호재 타입 선택
            when (Random.nextInt(3)) {
                0 -> StockEventType.POSITIVE_SMALL
                1 -> StockEventType.POSITIVE_MEDIUM
                else -> StockEventType.POSITIVE_LARGE
            }
        }
        
        // 호재 이벤트 생성 및 적용
        val event = createReversionEvent(finalEventType)
        addEvent(event)
        
        return event.message
    }
    
    /**
     * 하락 반동에 대한 악재 이벤트 적용
     * @param isForced 강제 반동 여부 (true일 경우 더 강한 이벤트 적용)
     * @param eventType 적용할 이벤트 타입 (지정되지 않으면 랜덤 선택)
     * @return 반동 이벤트 메시지
     */
    private fun applyNegativeEventForReversion(
        isForced: Boolean = false,
        eventType: StockEventType? = null
    ): String {
        // 기존 호재/악재 효과 제거
        isPositiveNews = false
        isNegativeNews = false
        
        // 기존 이벤트 제거 (반동 이벤트와 충돌 방지)
        clearAllEvents()
        
        // 악재 타입 선택
        val finalEventType = eventType ?: if (isForced) {
            StockEventType.NEGATIVE_LARGE
        } else {
            // 무작위로 악재 타입 선택
            when (Random.nextInt(3)) {
                0 -> StockEventType.NEGATIVE_SMALL
                1 -> StockEventType.NEGATIVE_MEDIUM
                else -> StockEventType.NEGATIVE_LARGE
            }
        }
        
        // 악재 이벤트 생성 및 적용
        val event = createReversionEvent(finalEventType)
        addEvent(event)
        
        return event.message
    }
    
    /**
     * 반동에 대한 이벤트 생성
     * @param eventType 이벤트 타입
     * @return 생성된 이벤트
     */
    private fun createReversionEvent(eventType: StockEventType): StockEvent {
        // 이벤트 타입에 따라 변동률 범위 설정
        val (minRate, maxRate) = when (eventType) {
            StockEventType.POSITIVE_SMALL -> Pair(0.01, 0.02)
            StockEventType.POSITIVE_MEDIUM -> Pair(0.02, 0.04)
            StockEventType.POSITIVE_LARGE -> Pair(0.04, 0.07)
            StockEventType.NEGATIVE_SMALL -> Pair(-0.02, -0.01)
            StockEventType.NEGATIVE_MEDIUM -> Pair(-0.04, -0.02)
            StockEventType.NEGATIVE_LARGE -> Pair(-0.07, -0.04)
            else -> Pair(0.0, 0.0) // 기본값
        }
        
        // 반동 이벤트 지속 시간 설정 (기존에 계산한 반동 효과 지속 시간과 동일하게)
        val durationMs = when (eventType) {
            StockEventType.POSITIVE_SMALL, StockEventType.NEGATIVE_SMALL -> REVERSION_DURATION_SMALL
            StockEventType.POSITIVE_MEDIUM, StockEventType.NEGATIVE_MEDIUM -> REVERSION_DURATION_MEDIUM
            StockEventType.POSITIVE_LARGE, StockEventType.NEGATIVE_LARGE -> REVERSION_DURATION_LARGE
            else -> reversionRemainingMs // 기타 이벤트 타입의 경우 현재 설정된 반동 지속 시간 사용
        }
        
        // 이벤트 메시지 생성
        val eventMessage = when (eventType) {
            StockEventType.POSITIVE_SMALL -> "반동 효과: 소형 호재 발생!"
            StockEventType.POSITIVE_MEDIUM -> "반동 효과: 중형 호재 발생!"
            StockEventType.POSITIVE_LARGE -> "반동 효과: 대형 호재 발생!"
            StockEventType.NEGATIVE_SMALL -> "반동 효과: 소형 악재 발생!"
            StockEventType.NEGATIVE_MEDIUM -> "반동 효과: 중형 악재 발생!"
            StockEventType.NEGATIVE_LARGE -> "반동 효과: 대형 악재 발생!"
            else -> "반동 효과 발생"
        }
        
        // 이벤트 생성 및 반환
        return StockEvent(
            type = eventType,
            minChangeRate = minRate,
            maxChangeRate = maxRate,
            duration = durationMs,
            message = eventMessage,
            affectedStockNames = listOf(name),
            volatilityMultiplier = 1.0 // 변동성 변화 없음
        )
    }

    /**
     * 과거 가격 이력을 기반으로 현재 추세 강도와 방향을 계산합니다.
     * trendStrength 값은 -1.0 (강한 하락 추세) ~ 1.0 (강한 상승 추세) 범위입니다.
     */
    private fun calculateTrend() {
        if (priceHistory.size < 3) return
        
        // 단기 추세 (최근 5개 또는 전체 이력)
        val shortTermSize = minOf(5, priceHistory.size - 1)
        var shortTermTrend = 0.0
        
        // 최근 변동들에 가중치를 부여 (최근 변동에 더 높은 가중치)
        for (i in 1..shortTermSize) {
            val idx = priceHistory.size - i
            val prevIdx = priceHistory.size - i - 1
            val change = priceHistory[idx] - priceHistory[prevIdx]
            
            // 변동의 방향(sign)과 크기를 고려, 최근 변동에 더 높은 가중치 부여
            val weight = (shortTermSize - i + 1.0) / shortTermSize
            shortTermTrend += change.sign * weight * (change.absoluteValue / 1000.0)
        }
        
        // 추세 강도 정규화 (-1.0 ~ 1.0 범위로)
        shortTermTrend = shortTermTrend.coerceIn(-1.0, 1.0)
        
        // 추세 강도를 업데이트 (기존 추세에 약간의 관성 부여)
        trendStrength = (trendStrength * 0.7) + (shortTermTrend * 0.3)
    }

    private fun updatePriceAndChangeValue() {
        val oldPrice = price
        price += changeValue
        price = maxOf(price, 10)
        // 0으로 나누기 방지
        changeRate = if (oldPrice > 0) ((changeValue.toDouble() / oldPrice) * 100) else 0.0
        
        // 새 가격을 즉시 이력에 추가하지 않음 - updateChangeValue()에서 처리
    }

    /**
     * 매입 평균 가격을 계산합니다.
     * @return 평균 매입 가격, 매입 내역이 없을 경우 0 반환
     */
    fun getAvgPurchasePrice(): Int {
        return if (purchasePrices.isNotEmpty()) purchasePrices.average().toInt() else 0
    }

    /**
     * 전체 손익을 계산합니다.
     * @return 손익 금액 (현재가 - 매입가) × 보유량
     */
    fun getProfitLoss(): Int {
        val avgPurchasePrice = getAvgPurchasePrice()
        return if (holding > 0) (price - avgPurchasePrice) * holding else 0
    }

    /**
     * 수익률을 계산합니다.
     * @return 수익률(%) = (손익 / 총 투자금액) × 100
     */
    fun getProfitRate(): Double {
        val avgPrice = getAvgPurchasePrice()
        val totalInvestment = avgPrice * holding
        
        // 0으로 나누기 방지 및 보유량이 0인 경우 체크
        return if (holding > 0 && avgPrice > 0 && totalInvestment > 0) {
            (getProfitLoss().toDouble() / totalInvestment) * 100
        } else {
            0.0
        }
    }

    /**
     * 1주 매수합니다.
     */
    fun buyStock() {
        holding += 1
        purchasePrices.add(price)
    }

    /**
     * 지정된 수량만큼 주식을 매수합니다.
     * @param quantity 매수할 주식의 수량
     * @return 실제로 매수한 주식의 수량
     */
    fun buyStocks(quantity: Int): Int {
        if (quantity <= 0) return 0
        repeat(quantity) {
            purchasePrices.add(price)
        }
        holding += quantity
        return quantity
    }

    /**
     * 1주 매도합니다.
     * @return 매도 손익 (현재가 - 평균 매입가)
     */
    fun sellStock(): Int {
        if (holding <= 0) return 0
        
        val avgPurchasePrice = getAvgPurchasePrice()
        
        // 보유량이 감소하면 매입 내역에서 가장 오래된 매입 기록을 제거
        if (purchasePrices.isNotEmpty()) {
            purchasePrices.removeAt(0)
        }
        
        holding -= 1
        
        return price - avgPurchasePrice
    }

    /**
     * 지정된 수량만큼 주식을 매도합니다.
     * @param quantity 매도할 주식의 수량
     * @return 실제로 매도한 주식의 수량
     */
    fun sellStocks(quantity: Int): Int {
        if (quantity <= 0) return 0
        
        // 보유량보다 많이 매도할 수 없음
        val sellCount = minOf(quantity, holding)
        
        // 평균 매입가 계산
        val avgPurchasePrice = getAvgPurchasePrice()
        
        // 매도 시 매입 내역에서 가장 오래된 것부터 제거
        repeat(sellCount) {
            if (purchasePrices.isNotEmpty()) {
                purchasePrices.removeAt(0)
            }
        }
        
        // 보유량 감소
        holding -= sellCount
        
        return sellCount
    }

    /**
     * 가능한 모든 주식을 현재 자산으로 매수합니다.
     * @param currentAsset 현재 가용 자산
     * @return 매수한 주식의 수량
     */
    fun buyAllStock(currentAsset: Long): Int {
        val maxBuyCount = (currentAsset / price).toInt()
        if (maxBuyCount <= 0) return 0
        
        // 효율성을 위해 반복문 대신 한 번에 처리
        val newPurchases = List(maxBuyCount) { price }
        purchasePrices.addAll(newPurchases)
        holding += maxBuyCount
        
        return maxBuyCount
    }

    /**
     * 보유한 모든 주식을 매도합니다.
     * @return 매도한 주식의 수량
     */
    fun sellAllStock(): Int {
        val sellCount = holding
        holding = 0
        purchasePrices.clear()  // 모든 매입 내역 삭제
        return sellCount
    }

    /**
     * 이벤트를 추가합니다.
     */
    fun addEvent(event: StockEvent) {
        activeEvents[event.type] = event
    }
    
    /**
     * 특정 타입의 이벤트를 제거합니다.
     */
    fun removeEvent(eventType: StockEventType) {
        activeEvents.remove(eventType)
    }
    
    /**
     * 보유량을 초기화합니다.
     */
    fun resetHoldings() {
        holding = 0
        purchasePrices.clear()
        clearAllEvents()
    }
    
    /**
     * 가격을 초기 가격으로 리셋합니다.
     * @param initialPrice 초기 가격
     */
    fun resetPrice(initialPrice: Int) {
        price = initialPrice
        this.initialPrice = initialPrice  // 기준 가격 업데이트
        changeValue = 0
        changeRate = 0.0
        priceHistory.clear()
        priceHistory.add(price)
        trendStrength = 0.0  // 추세 초기화
        
        // 반동 관련 필드 초기화
        reversionActive = false
        reversionDirection = 0
        reversionRemainingMs = 0
        consecutiveMovesInSameDirection = 0
        lastMoveDirection = 0
        
        // 가격에 따른 변동성 재설정
        volatility = when {
            price <= 30000 -> 0.9    // 저가주는 안정적
            price <= 100000 -> 1.0   // 중간 가격은 보통
            price > 100000 -> 1.1   // 고가주는 변동성 큼
            else -> 1.0   // 기본값
        }
        
        clearAllEvents()
    }

    /**
     * 모든 이벤트를 제거합니다.
     */
    fun clearAllEvents() {
        activeEvents.clear()
        isPositiveNews = false
        isNegativeNews = false
    }

    /**
     * 활성화된 반동 효과를 적용하여 변동률을 조정합니다.
     */
    private fun applyActiveReversion(minChangePercent: Double, maxChangePercent: Double): Pair<Double, Double> {
        // 반동 방향에 따라 변동률 범위 조정 기능 추가 (단순화된 버전)
        return when (reversionDirection) {
            1 -> { // 상승 반동 (연속 하락 후)
                // 최소값을 0 이상으로 설정하여 하락을 방지
                Pair(0.01, maxOf(maxChangePercent, 0.02))
            }
            -1 -> { // 하락 반동 (연속 상승 후)
                // 최대값을 0 이하로 설정하여 상승을 방지
                Pair(minOf(minChangePercent, -0.01), -0.01)
            }
            else -> Pair(minChangePercent, maxChangePercent)
        }
    }
}

fun ClosedRange<Double>.random() = Random.nextDouble(start, endInclusive)
