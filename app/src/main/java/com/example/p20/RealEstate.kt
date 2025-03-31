package com.example.p20

data class RealEstate(
    val name: String,
    val initialPrice: Long, // 최초 가격
    var price: Long = initialPrice,
    var owned: Boolean = false,
    val purchasePrices: MutableList<Long> = mutableListOf()
) {
    fun updatePrice() {
        val changeRates = listOf(-40, -30, -20, -10, 10, 20, 30, 40)
        val rate = changeRates.random()
        price = (initialPrice * (1 + rate / 100.0)).toLong()
    }

    fun buy() {
        owned = true
        purchasePrices.add(price)
    }

    fun sell() {
        owned = false
        purchasePrices.clear()
    }

    fun getAvgPurchasePrice(): Long {
        return if (purchasePrices.isNotEmpty()) {
            purchasePrices.sum() / purchasePrices.size
        } else 0L
    }

    fun getProfitLoss(): Long {
        return if (owned) {
            price - getAvgPurchasePrice()
        } else 0L
    }

    fun getProfitRate(): Double {
        val avgPrice = getAvgPurchasePrice()
        return if (owned && avgPrice > 0) {
            ((price - avgPrice) / avgPrice.toDouble()) * 100
        } else 0.0
    }
}
