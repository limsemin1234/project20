package com.example.p20

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StockViewModel : ViewModel() {

    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L // 3초마다 업데이트

    init {
        // 초기 주식 데이터 설정
        _stockItems.value = mutableListOf(
            Stock("테슬라", 10000, 0, 0.0, 0),
            Stock("애플", 10000, 0, 0.0, 0),
            Stock("아마존", 10000, 0, 0.0, 0),
            Stock("MS", 10000, 0, 0.0, 0)
        )

        // 앱 실행과 동시에 주식 변동 시작
        startStockPriceUpdates()
    }

    private fun startStockPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateStockPrices()
                handler.postDelayed(this, updateInterval) // 3초마다 반복 실행
            }
        })
    }

    // 주식 가격 업데이트 함수
    fun updateStockPrices() {
        _stockItems.value?.let { stocks ->
            stocks.forEach { it.updateChangeValue() }
            _stockItems.postValue(stocks) // 변경 사항 반영
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null) // 메모리 누수 방지
    }
}

