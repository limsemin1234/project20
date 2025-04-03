package com.example.p20

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AssetViewModel(private val context: Context) : ViewModel() {

    private val _asset = MutableLiveData<Long>()
    val asset: LiveData<Long> get() = _asset

    private val _realEstateList = MutableLiveData<List<RealEstate>>()
    val realEstateList: LiveData<List<RealEstate>> get() = _realEstateList

    init {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val savedAsset = sharedPreferences.getLong("asset", 1_000_000L) // 초기 자산 1,000,000원
        _asset.value = savedAsset
    }

    fun increaseAsset(amount: Long) {
        val currentAsset = _asset.value ?: 0L
        _asset.value = currentAsset + amount
        saveAssetToPreferences()
    }

    fun decreaseAsset(amount: Long) {
        val currentAsset = _asset.value ?: 0L
        if (currentAsset - amount >= 0L) {
            _asset.value = currentAsset - amount
            saveAssetToPreferences()
        } else {
            println("자산이 부족합니다!")
        }
    }

    private fun saveAssetToPreferences() {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong("asset", _asset.value ?: 0L)
        editor.apply()
    }

    fun getAssetText(): String {
        val assetValue = _asset.value ?: 0L
        return "자산: ${"%,d".format(assetValue)}원"
    }

    fun resetAsset() {
        _asset.value = 1_000_000_000L // 초기 자산
        saveAssetToPreferences()
    }

    private fun saveRealEstateToPreferences() {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        _realEstateList.value?.forEachIndexed { index, estate ->
            editor.putBoolean("estate_owned_$index", estate.owned)
            editor.putLong("estate_price_$index", estate.price)
        }
        editor.apply()
    }

    fun resetAssets() {
        _asset.value = 10000000 // 초기 현금으로 초기화
        _realEstateList.value = listOf( // 부동산 목록 초기화
            RealEstate(1, "반지하 원룸", 30_000_000L),
            RealEstate(2, "상가 건물", 50_000_000L),
            RealEstate(3, "아파트", 80_000_000L),
            RealEstate(4, "오피스텔", 120_000_000L),
            RealEstate(5, "단독 주택", 200_000_000L),
            RealEstate(6, "빌딩", 400_000_000L)
        )
        saveAssetToPreferences()
        saveRealEstateToPreferences()
    }
}
