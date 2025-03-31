package com.example.p20

data class RealEstate(
    val name: String,
    var price: Int,
    var owned: Boolean = false,
    val purchasePrices: MutableList<Int> = mutableListOf(), // 구매가 기록
    val initialPrice: Int = price // 최초 가격 저장
) {
    fun updatePrice() {
        val changeRates = listOf(-20, -10, 0, 10, 20)
        val rate = changeRates.random()
        price = initialPrice + (initialPrice * rate / 100)
    }

    fun buy() {
        owned = true
        purchasePrices.add(price)
    }

    fun sell() {
        owned = false
        purchasePrices.clear()
    }

    fun getAvgPurchasePrice(): Int {
        return if (purchasePrices.isNotEmpty()) {
            purchasePrices.sum() / purchasePrices.size
        } else 0
    }

    fun getProfitLoss(): Int {
        return if (owned) {
            price - getAvgPurchasePrice()
        } else 0
    }

    fun getProfitRate(): Double {
        val avgPrice = getAvgPurchasePrice()
        return if (owned && avgPrice > 0) {
            ((price - avgPrice) / avgPrice.toDouble()) * 100
        } else 0.0
    }
}
