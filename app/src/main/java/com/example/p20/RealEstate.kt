package com.example.p20

data class RealEstate(
    val id: Int,
    val name: String,
    val initialPrice: Long,
    var price: Long = initialPrice,
    var owned: Boolean = false,
    val purchasePrices: MutableList<Long> = mutableListOf(),
    val rentalRate: Double = 0.005 // ⭐️ 임대 수익률 추가 (기본 0.5%)
) {
    fun updatePrice() {
        val changeRates = listOf(-15, -10, -5, 0, 5, 10, 15)
        val rate = changeRates.random()
        val calculated = (initialPrice * (1 + rate / 100.0))
        price = Math.round(calculated)
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

    // ✅ 현재 가격 단계 반환 (-15, -10, -5, 0, 5, 10, 15)
    fun getCurrentRate(): Int {
        val rate = ((price - initialPrice).toDouble() / initialPrice * 100).toInt()
        return when {
            rate >= 15 -> 15
            rate >= 10 -> 10
            rate >= 5 -> 5
            rate >= -5 -> 0
            rate >= -10 -> -5
            rate >= -15 -> -10
            else -> -15
        }
    }

    // ✅ 예상 임대 수익 반환 (현재 가격의 0.5% 예시)
    fun getExpectedRentalIncome(): Long {
        return (price * rentalRate).toLong()
    }
}
