package com.example.p20

data class Stock(
    val name: String,        // 주식 이름
    var price: Int,          // 주식 가격
    var changeValue: Int,    // 변동값
    var changeRate: Double,  // 변동률
    var holding: Int,        // 보유량
    val purchasePrices: MutableList<Int> = mutableListOf() // 매입 가격 리스트
) {
    fun updateChangeValue() {
        val minChange = -400
        val maxChange = 600
        val randomChangeValue = (minChange..maxChange).random()
        changeValue = (randomChangeValue / 100) * 100
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
