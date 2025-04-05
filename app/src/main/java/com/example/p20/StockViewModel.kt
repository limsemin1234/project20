package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.random.Random

class StockViewModel(application: Application) : AndroidViewModel(application) {

    private val _stockItems = MutableLiveData<MutableList<Stock>>()
    val stockItems: LiveData<MutableList<Stock>> get() = _stockItems

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L // 주식 가격 업데이트 간격 (3초)
    private val positiveNewsInterval = 30000L // 호재 이벤트 체크 간격 (30초)
    private val positiveNewsChance = 0.3 // 호재 발생 확률 (30%)
    private val positiveNewsDuration = 20000L // 호재 지속 시간 (20초)
    
    private val sharedPreferences = application.getSharedPreferences("stock_data", Context.MODE_PRIVATE)
    
    // 호재 이벤트 콜백
    private var positiveNewsCallback: ((List<String>) -> Unit)? = null

    init {
        _stockItems.value = mutableListOf(
            Stock("만원", 10000, 0, 0.0, 0),
            Stock("이만", 20000, 0, 0.0, 0),
            Stock("오만", 50000, 0, 0.0, 0),
            Stock("십만", 100000, 0, 0.0, 0),
            Stock("이십만", 200000, 0, 0.0, 0)
        )

        loadStockData()
        startStockPriceUpdates()
        startPositiveNewsCheck()
    }
    
    fun setPositiveNewsCallback(callback: (List<String>) -> Unit) {
        positiveNewsCallback = callback
    }

    private fun startStockPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updateStockPrices()
                handler.postDelayed(this, updateInterval)
            }
        })
    }
    
    private fun startPositiveNewsCheck() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkForPositiveNews()
                handler.postDelayed(this, positiveNewsInterval)
            }
        }, positiveNewsInterval)
    }
    
    private fun checkForPositiveNews() {
        if (Random.nextDouble() < positiveNewsChance) {
            // 30% 확률로 호재 발생
            applyPositiveNews()
        }
    }
    
    private fun applyPositiveNews() {
        _stockItems.value?.let { stocks ->
            // 모든 주식 호재 영향 초기화
            stocks.forEach { it.isPositiveNews = false }
            
            // 주식 목록을 섞고 처음 2개 선택
            val selectedStocks = stocks.shuffled().take(2)
            
            // 선택된 주식에 호재 적용
            selectedStocks.forEach { it.isPositiveNews = true }
            
            // 호재 영향 받는 주식 이름 리스트
            val positiveNewsStockNames = selectedStocks.map { it.name }
            
            // 콜백 호출 (Fragment에 알림)
            positiveNewsCallback?.invoke(positiveNewsStockNames)
            
            // 20초 후에 호재 효과 제거
            handler.postDelayed({
                removePositiveNews()
            }, positiveNewsDuration)
            
            // UI 업데이트
            _stockItems.value = stocks
        }
    }
    
    private fun removePositiveNews() {
        _stockItems.value?.let { stocks ->
            // 모든 주식 호재 영향 제거
            stocks.forEach { it.isPositiveNews = false }
            
            // UI 업데이트
            _stockItems.value = stocks
        }
    }

    fun updateStockPrices() {
        _stockItems.value?.let { stocks ->
            stocks.forEach { it.updateChangeValue() }
            _stockItems.value = stocks
        }
    }

    fun buyStock(stock: Stock) {
        stock.buyStock()
        saveStockData()
    }

    fun sellStock(stock: Stock) {
        stock.sellStock()
        saveStockData()
    }

    fun buyAllStock(stock: Stock, currentAsset: Long): Int {
        val buyCount = stock.buyAllStock(currentAsset)
        saveStockData()
        _stockItems.value = _stockItems.value
        return buyCount
    }

    fun sellAllStock(stock: Stock): Int {
        val sellCount = stock.sellAllStock()
        saveStockData()
        _stockItems.value = _stockItems.value
        return sellCount
    }

    fun saveStockData() {
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
            when (stock.name) {
                "만원" -> stock.price = 10000
                "이만" -> stock.price = 20000
                "오만" -> stock.price = 50000
                "십만" -> stock.price = 100000
                "이십만" -> stock.price = 200000
            }
            stock.holding = 0
            stock.purchasePrices.clear()
            stock.isPositiveNews = false
        }
        saveStockData()
        _stockItems.value = _stockItems.value
    }

    fun resetStocks() {
        _stockItems.value = mutableListOf(
            Stock("만원", 10000, 0, 0.0, 0),
            Stock("이만", 20000, 0, 0.0, 0),
            Stock("오만", 50000, 0, 0.0, 0),
            Stock("십만", 100000, 0, 0.0, 0),
            Stock("이십만", 200000, 0, 0.0, 0)
        )
        saveStockData()
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
