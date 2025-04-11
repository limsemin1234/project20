package com.example.p20

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

data class Stock(
    val name: String,        // 주식 이름
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
    var volatility: Double = 1.0         // 기본 변동성 계수
) {
    // 이력 최대 크기
    private val MAX_HISTORY_SIZE = 30
    
    // 추세 영향력 가중치 (0.0 ~ 1.0)
    private val TREND_WEIGHT = 0.4
    
    // 랜덤 변동 가중치
    private val RANDOM_WEIGHT = 0.6
    
    init {
        // 초기 가격을 이력에 추가
        priceHistory.add(price)
        
        // 주식별 기본 변동성 설정
        volatility = when(name) {
            "만원" -> 0.8    // 안정적
            "이만" -> 0.9    // 약간 안정적
            "오만" -> 1.0    // 보통
            "십만" -> 1.1    // 약간 변동이 큼
            "이십만" -> 1.3  // 변동이 매우 큼
            else -> 1.0
        }
    }
    
    fun updateChangeValue() {
        // 가격을 이력에 추가
        priceHistory.add(price)
        if (priceHistory.size > MAX_HISTORY_SIZE) {
            priceHistory.removeAt(0)
        }
        
        // 현재 추세 계산
        calculateTrend()
        
        val minChangePercent: Double
        val maxChangePercent: Double
        
        // 호재/악제 영향 받는 종목은 각각 상승/하락만 하도록
        when {
            isPositiveNews -> {
                minChangePercent = 0.01 // 최소 0.01% 상승
                maxChangePercent = 0.05 // 최대 0.05% 상승 (기존 0.10%에서 수정)
            }
            isNegativeNews -> {
                minChangePercent = -0.05 // 최소 0.05% 하락 (기존 -0.10%에서 수정)
                maxChangePercent = -0.01 // 최대 0.01% 하락
            }
            else -> {
                // 기본 변동 범위 (-0.02% ~ 0.025%) (기존 -0.04% ~ 0.045%에서 수정)
                minChangePercent = -0.02
                maxChangePercent = 0.025
            }
        }
        
        // 랜덤 변동 요소 계산
        val randomPercent = (minChangePercent..maxChangePercent).random()
        
        // 추세 요소 (trendStrength는 -1.0 ~ 1.0 사이)
        // 양수면 상승 추세, 음수면 하락 추세, 0에 가까울수록 약한 추세
        val trendPercent = trendStrength * 0.04 // 추세 크기 조정
        
        // 최종 변동률 계산 (랜덤 요소 + 추세 요소)
        val finalChangePercent = (randomPercent * RANDOM_WEIGHT) + (trendPercent * TREND_WEIGHT)
        
        // 변동성 적용
        val adjustedChangePercent = finalChangePercent * volatility
        
        // 변동값 계산
        val calculatedChange = (price * adjustedChangePercent / 100.0).roundToInt() * 100
        changeValue = calculatedChange

        if (changeValue == 0 && price >= 1000) {
            // 호재/악제 영향을 받는 종목은 각각 무조건 상승/하락만 하도록
            changeValue = when {
                isPositiveNews -> 100
                isNegativeNews -> -100
                else -> if (Random.nextBoolean()) 100 else -100
            }
        }

        updatePriceAndChangeValue()
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
}

fun ClosedRange<Double>.random() = Random.nextDouble(start, endInclusive)
