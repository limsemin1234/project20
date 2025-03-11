package com.example.p20

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StockViewModel : ViewModel() {

    // LiveData로 변경하여 외부에서 관찰할 수 있도록 합니다.
    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems

    init {
        // 초기 데이터 설정
        _stockItems.value = mutableListOf(
            Stock("테슬라", 10000, 0, 0.0, 0),
            Stock("애플", 10000, 0, 0.0, 0),
            Stock("아마존", 10000, 0, 0.0, 0),
            Stock("MS", 10000, 0, 0.0, 0)
        )
    }

    // 주식 가격을 3초마다 업데이트하는 함수
    fun updateStockPrices() {
        _stockItems.value?.forEach { it.updateChangeValue() }
        _stockItems.postValue(_stockItems.value) // postValue로 변경 트리거
    }
}
