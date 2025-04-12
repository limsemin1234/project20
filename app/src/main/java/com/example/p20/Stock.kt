package com.example.p20

import kotlin.math.absoluteValue
import kotlin.math.max
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
    
    // 랜덤 변동 가중치 (추세 제거로 인해 100%로 설정)
    private val RANDOM_WEIGHT = 1.0
    
    // 반동 확률 최대치 
    private val MAX_REVERSION_PROBABILITY = 0.95
    
    // 가격 업데이트 간격 (ms 단위)
    private val UPDATE_INTERVAL = 5000L   // 가격 업데이트 간격 (5초)
    
    // 반동 지속 시간 배수 (가격 업데이트 간격 단위)
    private val REVERSION_TICKS_MIN = 3   // 최소 3회 가격 변동
    private val REVERSION_TICKS_MAX = 6   // 최대 6회 가격 변동
    
    // 초기 가격 저장 (이력용)
    var initialPrice: Int = 0
    
    init {
        // 초기 가격을 이력에 추가
        priceHistory.add(price)
        
        // 초기 가격 저장
        initialPrice = price
    }
    
    /**
     * 주식 가격 변동값을 계산하고 업데이트합니다.
     * @return 반동 발생 시 메시지, 없으면 빈 문자열
     */
    fun updateChangeValue(): String {
        // 항상 현재 가격을 이력에 추가
        priceHistory.add(price)
        if (priceHistory.size > MAX_HISTORY_SIZE) {
            priceHistory.removeAt(0)
        }
        
        var minChangePercent = -0.04  // 기본 최소 변동률 (-4%)
        var maxChangePercent = 0.04   // 기본 최대 변동률 (4%)
        var currentVolatility = volatility  // 기본 변동성
        
        // 반동 메시지를 저장할 변수
        var reversionMessage = ""
        
        // 반동 메커니즘이 활성화되어 있지 않을 때만 이벤트 효과 적용
        if (!reversionActive) {
            // 기존 호재/악재 이벤트 처리 (이전 버전과의 호환성)
            when {
                isPositiveNews -> {
                    minChangePercent = 0.02 // 최소 2% 상승
                    maxChangePercent = 0.07 // 최대 7% 상승
                }
                isNegativeNews -> {
                    minChangePercent = -0.07 // 최소 7% 하락
                    maxChangePercent = -0.02 // 최대 2% 하락
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
            // 이미 반동 효과가 활성화되어 있는 경우 - 카운트다운
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
        
        // 최종 변동률 계산 (100% 랜덤, 추세 제거)
        val finalChangePercent = randomPercent * RANDOM_WEIGHT
        
        // 변동성 적용
        val adjustedChangePercent = finalChangePercent * currentVolatility
        
        // 변동값 계산 (100원 단위로 조정)
        var calculatedChange = (price * adjustedChangePercent / 100.0).roundToInt() * 100
        
        // 반동 효과가 활성화된 경우 방향성 강제 적용
        if (reversionActive) {
            // 0원이거나 반동 방향과 일치하지 않는 경우 최소 변동값 강제 적용
            if (calculatedChange == 0 || calculatedChange.sign != reversionDirection) {
                // 최소 100원의 상승/하락 보장
                calculatedChange = reversionDirection * max(100, calculatedChange.absoluteValue)
            }
        }
        
        // 계산된 변동값 사용
        changeValue = calculatedChange

        // 변동 방향 저장
        val currentDirection = changeValue.sign
        
        // 반동 효과가 활성화되어 있지 않을 때만 연속 변동 카운터 업데이트
        if (!reversionActive) {
            if (currentDirection == 0) {
                // 변동값이 0일 경우에는 연속 카운터를 업데이트하지 않음
                // 카운터와 lastMoveDirection은 이전 상태를 유지
            } else if (currentDirection == lastMoveDirection && lastMoveDirection != 0) {
                // 이전과 같은 방향으로 변동할 경우
                consecutiveMovesInSameDirection++
            } else {
                // 방향이 바뀌었거나 처음 변동
                consecutiveMovesInSameDirection = 1
                lastMoveDirection = currentDirection
            }
        }

        // 변동값과 상관없이 항상 가격 처리 수행
        updatePriceAndChangeValue()
        
        return reversionMessage
    }

    /**
     * 반동 메커니즘 발생 조건을 확인하고, 조건이 만족되면 반동 효과 활성화
     * @return 반동 이벤트 메시지 (반동이 발생하지 않으면 빈 문자열)
     */
    private fun checkAndStartReversion(): String {       
        // 연속 변동이 5회 미만이면 반동 효과 적용하지 않음
        // 추가 조건: 가격 이력이 최소 6개 이상 있어야 함 (초기 가격 + 5번의 변동)
        // lastMoveDirection이 0인 조건 제거 (변동값이 0이어도 반동 메커니즘이 작동하도록)
        if (consecutiveMovesInSameDirection < 5 || priceHistory.size < 6) {
            return ""
        }

        // 현재 추세 방향이 없는 경우(lastMoveDirection이 0인 경우) 반동 기능 적용 안함
        if (lastMoveDirection == 0) {
            return ""
        }

        // 연속 변동 횟수에 따른 반동 확률 결정
        val reversionProbability = when (consecutiveMovesInSameDirection) {
            5 -> 0.5   // 5번 연속 변동 시 50% 확률
            6 -> 0.6   // 6번 연속 변동 시 60% 확률
            7 -> 0.7   // 7번 연속 변동 시 70% 확률
            8 -> 0.8   // 8번 연속 변동 시 80% 확률
            9 -> 0.9   // 9번 연속 변동 시 90% 확률
            else -> MAX_REVERSION_PROBABILITY // 10번 이상 연속 변동 시 95% 확률
        }
        
        // 반동 확률에 따라 반동 효과 활성화
        if (Random.nextDouble() < reversionProbability) {
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
        
        // 반동 지속 시간을 3~6회 가격 변동 중 랜덤하게 설정
        val randomTicks = (REVERSION_TICKS_MIN..REVERSION_TICKS_MAX).random()
        reversionRemainingMs = randomTicks * UPDATE_INTERVAL
        
        // 연속 변동 카운터만 초기화 (lastMoveDirection은 유지)
        consecutiveMovesInSameDirection = 0
        
        // 반동 방향에 따라 이벤트 추가
        return if (direction == 1) {
            // 상승 반동일 때 호재 이벤트 적용
            applyCustomPositiveReversionEvent(randomTicks)
        } else if (direction == -1) {
            // 하락 반동일 때 악재 이벤트 적용
            applyCustomNegativeReversionEvent(randomTicks)
        } else {
            "반동 효과 발생"
        }
    }
    
    /**
     * 상승 반동에 대한 커스텀 호재 이벤트 적용 (+2% ~ +9%)
     * @param ticks 반동 지속 틱 수
     * @return 반동 이벤트 메시지
     */
    private fun applyCustomPositiveReversionEvent(ticks: Int = REVERSION_TICKS_MIN): String {
        // 기존 호재/악재 효과 제거
        isPositiveNews = false
        isNegativeNews = false
        
        // 기존 이벤트 제거 (반동 이벤트와 충돌 방지)
        clearAllEvents()
        
        // 상승 반동을 위한 커스텀 이벤트 생성 - 반드시 양수 변동값이 나오도록 조정
        val minRate = 0.02  // +2%
        val maxRate = 0.09  // +9%
        
        // 지속 시간 설정
        val durationMs = ticks * UPDATE_INTERVAL
        
        // 이벤트 메시지
        val message = "반동 효과: 상승 유도 (2%~9%) - ${ticks}회 적용"
        
        // 이벤트 생성
        val event = StockEvent(
            type = StockEventType.POSITIVE_LARGE,  // 타입은 큰 의미 없으나 대형 호재로 설정
            minChangeRate = minRate,
            maxChangeRate = maxRate,
            duration = durationMs,
            message = message,
            affectedStockNames = listOf(name),
            volatilityMultiplier = 1.0
        )
        
        // 이벤트 적용
        addEvent(event)
        
        return event.message
    }
    
    /**
     * 하락 반동에 대한 커스텀 악재 이벤트 적용 (-9% ~ -2%)
     * @param ticks 반동 지속 틱 수
     * @return 반동 이벤트 메시지
     */
    private fun applyCustomNegativeReversionEvent(ticks: Int = REVERSION_TICKS_MIN): String {
        // 기존 호재/악재 효과 제거
        isPositiveNews = false
        isNegativeNews = false
        
        // 기존 이벤트 제거 (반동 이벤트와 충돌 방지)
        clearAllEvents()
        
        // 하락 반동을 위한 커스텀 이벤트 생성 - 반드시 음수 변동값이 나오도록 조정
        val minRate = -0.09  // -9%
        val maxRate = -0.02  // -2%
        
        // 지속 시간 설정
        val durationMs = ticks * UPDATE_INTERVAL
        
        // 이벤트 메시지
        val message = "반동 효과: 하락 유도 (-9%~-2%) - ${ticks}회 적용"
        
        // 이벤트 생성
        val event = StockEvent(
            type = StockEventType.NEGATIVE_LARGE,  // 타입은 큰 의미 없으나 대형 악재로 설정
            minChangeRate = minRate,
            maxChangeRate = maxRate,
            duration = durationMs,
            message = message,
            affectedStockNames = listOf(name),
            volatilityMultiplier = 1.0
        )
        
        // 이벤트 적용
        addEvent(event)
        
        return event.message
    }

    /**
     * 과거 가격 이력을 기반으로 현재 추세 강도와 방향을 계산합니다.
     * trendStrength 값은 -1.0 (강한 하락 추세) ~ 1.0 (강한 상승 추세) 범위입니다.
     * 추세 기능이 제거됨에 따라 빈 메서드로 유지
     */
    private fun calculateTrend() {
        // 추세 기능 제거
    }

    private fun updatePriceAndChangeValue() {
        val oldPrice = price
        price += changeValue
        price = maxOf(price, 10)
        
        // 변동률 계산 수정 - 직접 변동률 범위를 -4% ~ +4%로 표현
        // 원래 계산된 변동률을 사용하는 대신, 기존 계산에서 구한 changeValue로부터 
        // 비율을 다시 계산하여 변동률 범위가 -4% ~ +4%가 되도록 함
        changeRate = if (oldPrice > 0) (changeValue.toDouble() / oldPrice) * 100 else 0.0
        
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
        volatility = 1.0  // 변동성 초기화
        
        // 반동 관련 필드 초기화
        reversionActive = false
        reversionDirection = 0
        reversionRemainingMs = 0
        consecutiveMovesInSameDirection = 0
        lastMoveDirection = 0
        
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
}

fun ClosedRange<Double>.random() = Random.nextDouble(start, endInclusive)
