package com.example.p20

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AssetViewModel(private val context: Context) : ViewModel() {

    private val _asset = MutableLiveData<Long>()
    val asset: LiveData<Long> get() = _asset

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
}
