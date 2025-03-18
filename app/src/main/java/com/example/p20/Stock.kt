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
        // 변동 범위 설정
        val minChange = -100     // 최소 변동값
        val maxChange = 1000     // 최대 변동값

        // minChange와 maxChange 사이의 랜덤한 값 생성
        val randomChangeValue = (minChange..maxChange).random()

        // 변동값을 100의 배수로 정리하여 일의 자리가 0이 되도록
        changeValue = (randomChangeValue / 100) * 100  // 일의 자리가 0인 값으로 변환

        // 변동값을 반영한 가격 업데이트
        updatePriceAndChangeValue()
    }

    private fun updatePriceAndChangeValue() {
        val oldPrice = price
        // 가격을 changeValue만큼 증가시키거나 감소시킴
        price = price + changeValue
        // 가격이 10원 이하로 내려가지 않도록 제한
        price = maxOf(price, 10)
        // 변동률 계산
        changeRate = ((changeValue.toDouble() / oldPrice) * 100)
    }

    // 평균 매입가 계산 (매수 시에만 호출)
    fun getAvgPurchasePrice(): Int {
        return if (purchasePrices.isNotEmpty()) purchasePrices.average().toInt() else 0
    }

    // 손익 계산 (보유 주식 수를 고려)
    fun getProfitLoss(): Int {
        val avgPurchasePrice = getAvgPurchasePrice()
        return if (holding > 0) (price - avgPurchasePrice) * holding else 0
    }

    // 수익률 계산
    fun getProfitRate(): Double {
        return if (purchasePrices.isNotEmpty()) {
            (getProfitLoss().toDouble() / (getAvgPurchasePrice() * holding)) * 100
        } else 0.0
    }

    // 주식 매수 (매입 가격을 추가하고 보유량 증가)
    fun buyStock(purchasePrice: Int) {
        holding += 1
        purchasePrices.add(purchasePrice)
        // 매수 후, 매수 가격을 기반으로 새로운 평균 매입가로 갱신
        updateAveragePurchasePrice()
    }

    // 주식 매도 (이익/손실을 계산하고 보유량 감소)
    fun sellStock(): Int {
        if (holding > 0) {
            holding -= 1
            val avgPurchasePrice = getAvgPurchasePrice() // 평균 매입단가 가져오기
            if (holding == 0) {
                // 보유량이 0이 되면 매수 리스트 초기화
                purchasePrices.clear()
            }
            return price - avgPurchasePrice // 이익(+) or 손실(-) 계산
        }
        return 0
    }

    // 매수 후 평균 매입 단가 갱신
    private fun updateAveragePurchasePrice() {
        if (purchasePrices.isNotEmpty()) {
            val avgPrice = purchasePrices.average().toInt()

        }
    }

}
