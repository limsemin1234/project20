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
    var isPositiveNews: Boolean = false // 호재 영향 여부
) {
    fun updateChangeValue() {
        val minChangePercent: Double
        val maxChangePercent: Double
        
        // 호재 영향 받는 종목은 항상 상승
        if (isPositiveNews) {
            minChangePercent = 0.01 // 최소 0.01% 상승
            maxChangePercent = 0.10 // 최대 0.10% 상승
        } else {
            minChangePercent = -0.04
            maxChangePercent = 0.045
        }
        
        val randomPercent = (minChangePercent..maxChangePercent).random()
        val calculatedChange = (price * randomPercent / 100.0).roundToInt() * 100
        changeValue = calculatedChange

        if (changeValue == 0 && price >= 1000) {
            // 호재 영향을 받는 종목은 무조건 상승하도록
            changeValue = if (isPositiveNews) 100 else if (Random.nextBoolean()) 100 else -100
        }

        updatePriceAndChangeValue()
    }

    private fun updatePriceAndChangeValue() {
        val oldPrice = price
        price += changeValue
        price = maxOf(price, 10)
        changeRate = ((changeValue.toDouble() / oldPrice) * 100)
    }

    fun getAvgPurchasePrice(): Int {
        return if (purchasePrices.isNotEmpty()) purchasePrices.average().toInt() else 0
    }

    fun getProfitLoss(): Int {
        val avgPurchasePrice = getAvgPurchasePrice()
        return if (holding > 0) (price - avgPurchasePrice) * holding else 0
    }

    fun getProfitRate(): Double {
        return if (purchasePrices.isNotEmpty()) {
            (getProfitLoss().toDouble() / (getAvgPurchasePrice() * holding)) * 100
        } else 0.0
    }

    private fun updateAveragePurchasePrice() {
        if (purchasePrices.isNotEmpty()) {
            val avgPrice = purchasePrices.average().toInt()
        }
    }

    fun buyStock() {
        holding += 1
        purchasePrices.add(price)
        updateAveragePurchasePrice()
    }

    fun sellStock(): Int {
        if (holding > 0) {
            holding -= 1
            val avgPurchasePrice = getAvgPurchasePrice()
            if (holding == 0) {
                purchasePrices.clear()
            }
            return price - avgPurchasePrice
        }
        return 0
    }

    fun buyAllStock(currentAsset: Long): Int {
        val maxBuyCount = (currentAsset / price).toInt()
        repeat(maxBuyCount) {
            buyStock()
        }
        return maxBuyCount
    }

    fun sellAllStock(): Int {
        val sellCount = holding
        repeat(sellCount) {
            sellStock()
        }
        return sellCount
    }
}

fun ClosedRange<Double>.random() = Random.nextDouble(start, endInclusive)
