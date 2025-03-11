package com.example.p20

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AssetViewModel : ViewModel() {
    // 자산 값을 LiveData로 관리
    private val _asset = MutableLiveData<Int>().apply { value = 100000 }  // 초기 자산 100,000원
    val asset: LiveData<Int> get() = _asset  // 외부에서 읽을 수 있도록 공개

    // 자산 증가 함수
    fun increaseAsset(amount: Int) {
        _asset.value = (_asset.value ?: 0) + amount
    }

    // 자산 감소 함수
    fun decreaseAsset(amount: Int) {
        val currentAsset = _asset.value ?: 0
        if (currentAsset - amount >= 0) {
            _asset.value = currentAsset - amount
        } else {
            // 자산이 부족하면 알림을 표시하는 로직을 추가할 수 있습니다.
            println("자산이 부족합니다!")
        }
    }

    // 자산 텍스트 포맷 (천 단위 구분)
    fun getAssetText(): String {
        return "자산: ${(_asset.value ?: 0).toString().replace(Regex("(?<=\\d)(?=(\\d{3})+\\b)"), ",")}원"
    }
}
