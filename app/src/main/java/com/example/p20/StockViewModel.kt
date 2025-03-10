package com.example.p20

import androidx.lifecycle.ViewModel

class StockViewModel : ViewModel() {
    val stockItems: MutableList<Stock> = mutableListOf()

    init {
        // 초기 주식 데이터 설정
        stockItems.add(Stock("테슬라", 10000, 0, 0.0, 0))
        stockItems.add(Stock("애플", 10000, 0, 0.0, 0))
        stockItems.add(Stock("아마존", 10000, 0, 0.0, 0))
        stockItems.add(Stock("MS", 10000, 0, 0.0, 0))
    }

    // 주식 목록을 갱신하는 메서드
    fun updateStockList() {
        // 예를 들어, 주식 데이터를 외부에서 가져오는 로직이 있다면 여기서 처리
        // 현재는 이미 초기화 되어 있으므로 특별히 할 일이 없다면 비워두거나 다른 갱신 로직을 작성할 수 있음
    }
}
