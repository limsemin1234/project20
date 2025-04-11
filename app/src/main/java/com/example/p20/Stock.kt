package com.example.p20

import kotlin.math.roundToInt
import kotlin.random.Random

data class Stock(
    val name: String,        // 주식 이름
    var price: Int,          // 주식 가격
    var changeValue: Int,    // 변동값
    var changeRate: Double,  // 변동률
    var holding: Int,        // 보유량
    val purchasePrices: MutableList<Int> = mutableListOf(), // 매입 가격 리스트
    var isPositiveNews: Boolean = false, // 호재 영향 여부
    var isNegativeNews: Boolean = false  // 악제 영향 여부
) {
    fun updateChangeValue() {
        val minChangePercent: Double
        val maxChangePercent: Double
        
        // 호재/악제 영향 받는 종목은 각각 상승/하락만 하도록
        when {
            isPositiveNews -> {
                minChangePercent = 0.01 // 최소 0.01% 상승
                maxChangePercent = 0.10 // 최대 0.10% 상승
            }
            isNegativeNews -> {
                minChangePercent = -0.10 // 최소 0.10% 하락
                maxChangePercent = -0.01 // 최대 0.01% 하락
            }
            else -> {
                minChangePercent = -0.04
                maxChangePercent = 0.045
            }
        }
        
        val randomPercent = (minChangePercent..maxChangePercent).random()
        val calculatedChange = (price * randomPercent / 100.0).roundToInt() * 100
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
