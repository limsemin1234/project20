package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class RealEstateViewModel(application: Application) : AndroidViewModel(application) {

    private val _realEstateList = MutableLiveData<MutableList<RealEstate>>()
    val realEstateList: LiveData<MutableList<RealEstate>> get() = _realEstateList

    private val sharedPreferences = application.getSharedPreferences("real_estate_data", Context.MODE_PRIVATE)

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 10000L // 부동산 가격변동 10초

    private val incomeHandler = Handler(Looper.getMainLooper())
    private val incomeInterval = 25000L // 임대수익 25초

    var incomeCallback: ((Long) -> Unit)? = null

    init {
        _realEstateList.value = mutableListOf(
            RealEstate(1, "반지하 원룸", 30_000_000L),
            RealEstate(2, "상가 건물", 50_000_000L),
            RealEstate(3, "아파트", 80_000_000L),
            RealEstate(4, "오피스텔", 120_000_000L),
            RealEstate(5, "단독 주택", 200_000_000L),
            RealEstate(6, "빌딩", 400_000_000L)
        )
        loadRealEstateData()
        startPriceUpdates()
        startIncomeGeneration()
    }

    fun buy(realEstate: RealEstate) {
        realEstate.buy()
        saveRealEstateData()
        _realEstateList.value = _realEstateList.value
    }

    fun sell(realEstate: RealEstate) {
        realEstate.sell()
        saveRealEstateData()
        _realEstateList.value = _realEstateList.value
    }

    private fun saveRealEstateData() {
        val editor = sharedPreferences.edit()
        _realEstateList.value?.forEachIndexed { index, estate ->
            editor.putBoolean("owned_$index", estate.owned)
            val purchaseString = estate.purchasePrices.joinToString(",")
            editor.putString("purchasePrices_$index", purchaseString)
            editor.putLong("price_$index", estate.price)
        }
        editor.apply()
    }

    private fun loadRealEstateData() {
        _realEstateList.value?.forEachIndexed { index, estate ->
            estate.owned = sharedPreferences.getBoolean("owned_$index", false)
            estate.purchasePrices.clear()
            val purchaseString = sharedPreferences.getString("purchasePrices_$index", null)
            if (!purchaseString.isNullOrEmpty()) {
                val prices = purchaseString.split(",").mapNotNull { it.toLongOrNull() }
                estate.purchasePrices.addAll(prices)
            }
            estate.price = sharedPreferences.getLong("price_$index", estate.initialPrice)
        }
        _realEstateList.value = _realEstateList.value
    }

    private fun startPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updatePrices()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun updatePrices() {
        _realEstateList.value?.forEach { estate ->
            estate.updatePrice()
        }
        _realEstateList.value = _realEstateList.value
        saveRealEstateData()
    }

    private fun startIncomeGeneration() {
        incomeHandler.post(object : Runnable {
            override fun run() {
                generateTotalIncome()
                incomeHandler.postDelayed(this, incomeInterval)
            }
        })
    }

    private fun generateTotalIncome() {
        var totalIncome = 0L
        _realEstateList.value?.forEach { estate ->
            if (estate.owned) {
                val income = (estate.price * 0.01).toLong()
                totalIncome += income
            }
        }
        if (totalIncome > 0) {
            incomeCallback?.invoke(totalIncome)
        }
    }

    fun resetRealEstatePrices() {
        _realEstateList.value?.forEach { estate ->
            estate.price = estate.initialPrice
        }
        _realEstateList.value = _realEstateList.value
        saveRealEstateData()
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        incomeHandler.removeCallbacksAndMessages(null)
    }
}
