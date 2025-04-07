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

    private val _deposit = MutableLiveData<Long>()
    val deposit: LiveData<Long> = _deposit

    private val _loan = MutableLiveData<Long>()
    val loan: LiveData<Long> = _loan

    init {
        // --- 수정: SharedPreferences에서 자산 로드 활성화 ---
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        // --- 수정: 기본 자산 50만원으로 변경 ---
        val savedAsset = sharedPreferences.getLong("asset", 500_000L) // 저장된 값 로드, 없으면 50만원
        // --- 수정 끝 ---
        _asset.value = savedAsset
        _deposit.value = 0L
        _loan.value = 0L
        // _asset.value = 40_000_000L // 항상 4천만원으로 시작하는 코드 삭제 또는 주석 처리
        // saveAssetToPreferences() // 시작 시 저장 로직은 필요 없음 (로드 실패 시 기본값 사용)
        // --- 수정 끝 ---
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

    fun saveAssetToPreferences() {
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
        _asset.value = 500_000L // 초기 자산 50만원으로 변경
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
        // --- 수정: 초기 자산 50만원으로 변경 ---
        _asset.value = 500_000L // 초기 자산 50만원으로 변경
        // --- 수정 끝 ---
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

        // --- 수정: 모든 아이템 보유 수량 초기화 ---
        val itemPrefs = context.getSharedPreferences("item_prefs", Context.MODE_PRIVATE)
        with(itemPrefs.edit()) {
            putInt("time_amplifier_quantity", 0) // Time증폭(60초) 아이템 수량 0으로 초기화
            putInt("time_amplifier2_quantity", 0) // Time증폭(120초) 아이템 수량 0으로 초기화
            putInt("time_amplifier3_quantity", 0) // Time증폭(180초) 아이템 수량 0으로 초기화
            apply()
        }
        // --- 수정 끝 ---
    }

    fun setAsset(value: Long) {
        _asset.value = value
        saveAssetToPreferences()
    }

    fun addDeposit(amount: Long) {
        _deposit.value = (_deposit.value ?: 0L) + amount
    }

    fun subtractDeposit(amount: Long) {
        _deposit.value = (_deposit.value ?: 0L) - amount
    }

    fun addLoan(amount: Long) {
        _loan.value = (_loan.value ?: 0L) + amount
    }

    fun subtractLoan(amount: Long) {
        _loan.value = (_loan.value ?: 0L) - amount
    }

    // --- 추가: ViewModel 소멸 시 자산 저장 ---
    override fun onCleared() {
        super.onCleared()
        saveAssetToPreferences() // ViewModel이 제거될 때 현재 자산 저장
    }
    // --- 추가 끝 ---
}
