package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L // 3초마다 업데이트
    private val sharedPreferences = application.getSharedPreferences("stock_data", Context.MODE_PRIVATE)

    init {
        _stockItems.value = mutableListOf(
            Stock("테슬라", 10000, 0, 0.0, 0),
            Stock("애플", 10000, 0, 0.0, 0),
            Stock("아마존", 10000, 0, 0.0, 0),
            Stock("MS", 10000, 0, 0.0, 0)
        )

        loadStockData() // 앱 실행 시 데이터 로드
        startStockPriceUpdates()
    }

    private fun startStockPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateStockPrices()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    fun updateStockPrices() {
        _stockItems.value?.let { stocks ->
            stocks.forEach { it.updateChangeValue() }
            _stockItems.value = stocks
        }
    }

    private fun saveStockData() {
        val editor = sharedPreferences.edit()
        _stockItems.value?.forEachIndexed { index, stock ->
            editor.putInt("price_$index", stock.price)
            editor.putInt("holding_$index", stock.holding)
            editor.putInt("purchasePrice_$index", stock.getAvgPurchasePrice())
            editor.putInt("profitLoss_$index", stock.getProfitLoss())
            editor.putFloat("profitRate_$index", stock.getProfitRate().toFloat())
        }
        editor.apply()
    }

    private fun loadStockData() {
        _stockItems.value?.forEachIndexed { index, stock ->
            stock.price = sharedPreferences.getInt("price_$index", stock.price)
            stock.holding = sharedPreferences.getInt("holding_$index", stock.holding)
            stock.purchasePrices.clear()
            val savedPurchasePrice = sharedPreferences.getInt("purchasePrice_$index", 0)
            if (savedPurchasePrice > 0) {
                stock.purchasePrices.add(savedPurchasePrice)
            }
        }
        _stockItems.value = _stockItems.value
    }

    fun resetStockPrices() {
        _stockItems.value?.forEach { stock ->
            stock.price = 10000 // 초기 가격으로 리셋
            stock.holding = 0
            stock.purchasePrices.clear()
        }
        saveStockData()
        _stockItems.value = _stockItems.value
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        saveStockData()
    }
}
