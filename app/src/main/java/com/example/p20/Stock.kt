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
        changeValue = ((Math.random() * 1001 - 500) / 100).toInt() * 100  // -500 ~ +500 범위
        updatePriceAndChangeValue()
    }

    private fun updatePriceAndChangeValue() {
        val oldPrice = price
        price = maxOf(price + changeValue, 10)  // 가격이 10원 이하로 내려가지 않도록 설정
        changeRate = ((changeValue.toDouble() / oldPrice) * 100)  // 변동률 계산
    }

    fun getAvgPurchasePrice(): Int {
        return if (purchasePrices.isNotEmpty()) purchasePrices.average().toInt() else 0
    }

    fun getProfitLoss(): Int {
        val avgPurchasePrice = getAvgPurchasePrice()
        return if (holding > 0) (price - avgPurchasePrice) * holding else 0
    }

    fun getProfitRate(): Double {
        return if (purchasePrices.isNotEmpty()) (getProfitLoss().toDouble() / (getAvgPurchasePrice() * holding)) * 100 else 0.0
    }

    fun buyStock(purchasePrice: Int) {
        holding += 1
        purchasePrices.add(purchasePrice)
    }

    fun sellStock(): Int {
        if (holding > 0) {
            holding -= 1
            val avgPurchasePrice = getAvgPurchasePrice() // 평균 매입단가 가져오기
            return price - avgPurchasePrice // 이익(+) or 손실(-) 계산
        }
        return 0
    }

}
