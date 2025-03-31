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
    private val updateInterval = 5000L // 5초마다 가격 변동

    private val incomeHandler = Handler(Looper.getMainLooper())
    private val incomeInterval = 10000L // 10초마다 임대 수익 지급

    var incomeCallback: ((Int) -> Unit)? = null

    init {
        _realEstateList.value = mutableListOf(
            RealEstate("서울 아파트", 150_000_000),
            RealEstate("부산 오피스텔", 100_000_000),
            RealEstate("제주 타운하우스", 50_000_000)
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
        }
        editor.apply()
    }

    private fun loadRealEstateData() {
        _realEstateList.value?.forEachIndexed { index, estate ->
            estate.owned = sharedPreferences.getBoolean("owned_$index", false)
            estate.purchasePrices.clear()
            val purchaseString = sharedPreferences.getString("purchasePrices_$index", null)
            if (!purchaseString.isNullOrEmpty()) {
                val prices = purchaseString.split(",").mapNotNull { it.toIntOrNull() }
                estate.purchasePrices.addAll(prices)
            }
        }
        _realEstateList.value = _realEstateList.value
    }

    fun resetRealEstatePrices() {
        _realEstateList.value?.forEach { estate ->
            when (estate.name) {
                "서울 아파트" -> estate.price = 150_000_000
                "부산 오피스텔" -> estate.price = 100_000_000
                "제주 타운하우스" -> estate.price = 50_000_000
            }
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
        _realEstateList.value?.forEach { it.updatePrice() }
        _realEstateList.value = _realEstateList.value
    }

    private fun startIncomeGeneration() {
        incomeHandler.post(object : Runnable {
            override fun run() {
                generateIncome()
                incomeHandler.postDelayed(this, incomeInterval)
            }
        })
    }

    private fun generateIncome() {
        var totalIncome = 0
        _realEstateList.value?.forEach { estate ->
            if (estate.owned) {
                val income = (estate.price * 0.01).toInt() // 현재가의 1%
                totalIncome += income
            }
        }
        incomeCallback?.invoke(totalIncome)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        incomeHandler.removeCallbacksAndMessages(null)
    }
}
