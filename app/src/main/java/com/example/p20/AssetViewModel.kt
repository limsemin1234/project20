package com.example.p20

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AssetViewModel(private val context: Context) : ViewModel() {
    // 자산 값을 LiveData로 관리
    private val _asset = MutableLiveData<Int>()
    val asset: LiveData<Int> get() = _asset

    init {
        // SharedPreferences에서 자산 값 복원
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val savedAsset = sharedPreferences.getInt("asset", 100000) // 초기 자산 100,000원
        _asset.value = savedAsset
    }

    // 자산 증가 함수
    fun increaseAsset(amount: Int) {
        val currentAsset = _asset.value ?: 0
        _asset.value = currentAsset + amount
        saveAssetToPreferences() // 자산 값 저장
    }

    // 자산 감소 함수
    fun decreaseAsset(amount: Int) {
        val currentAsset = _asset.value ?: 0
        if (currentAsset - amount >= 0) {
            _asset.value = currentAsset - amount
            saveAssetToPreferences() // 자산 값 저장
        } else {
            // 자산이 부족하면 알림을 표시하는 로직을 추가할 수 있습니다.
            println("자산이 부족합니다!")
        }
    }

    // 자산 값 저장
    private fun saveAssetToPreferences() {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("asset", _asset.value ?: 0)
        editor.apply()
    }

    // 자산 텍스트 포맷 (천 단위 구분)
    fun getAssetText(): String {
        return "자산: ${(_asset.value ?: 0).toString().replace(Regex("(?<=\\d)(?=(\\d{3})+\\b)"), ",")}원"
    }

    // 자산 초기화 메서드
    fun resetAsset() {
        _asset.value = 1000000000 // 초기 자산 값으로 리셋
        saveAssetToPreferences() // 자산 초기화 후 저장
    }
}

