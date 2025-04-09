package com.example.p20

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.NumberFormat
import java.util.Locale

/**
 * 자산, 예금, 대출 데이터를 관리하는 Repository 클래스
 * SharedPreferences를 통한 데이터 저장 및 불러오기 처리
 */
class AssetRepository(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "game_preferences"
        private const val KEY_ASSET = "asset"
        private const val KEY_DEPOSIT = "deposit"
        private const val KEY_LOAN = "loan"
        private const val DEFAULT_INITIAL_ASSET = 500_000L
    }
    
    // LiveData
    private val _asset = MutableLiveData<Long>()
    val asset: LiveData<Long> get() = _asset
    
    private val _deposit = MutableLiveData<Long>()
    val deposit: LiveData<Long> get() = _deposit
    
    private val _loan = MutableLiveData<Long>()
    val loan: LiveData<Long> get() = _loan
    
    init {
        loadFromPreferences()
    }
    
    /**
     * SharedPreferences에서 자산, 예금, 대출 정보 로드
     */
    fun loadFromPreferences() {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _asset.value = sharedPreferences.getLong(KEY_ASSET, DEFAULT_INITIAL_ASSET)
        _deposit.value = sharedPreferences.getLong(KEY_DEPOSIT, 0L)
        _loan.value = sharedPreferences.getLong(KEY_LOAN, 0L)
    }
    
    /**
     * 현재 자산, 예금, 대출 정보를 SharedPreferences에 저장
     */
    fun saveToPreferences() {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong(KEY_ASSET, _asset.value ?: DEFAULT_INITIAL_ASSET)
            putLong(KEY_DEPOSIT, _deposit.value ?: 0L)
            putLong(KEY_LOAN, _loan.value ?: 0L)
            apply()
        }
    }
    
    // 자산 관련 메서드
    fun updateAsset(amount: Long) {
        _asset.value = amount
        saveToPreferences()
    }
    
    fun increaseAsset(amount: Long) {
        val currentAsset = _asset.value ?: 0L
        _asset.value = currentAsset + amount
        saveToPreferences()
    }
    
    fun decreaseAsset(amount: Long): Boolean {
        val currentAsset = _asset.value ?: 0L
        if (currentAsset - amount >= 0L) {
            _asset.value = currentAsset - amount
            saveToPreferences()
            return true
        }
        return false
    }
    
    // 예금 관련 메서드
    fun addDeposit(amount: Long): Boolean {
        if (decreaseAsset(amount)) {
            _deposit.value = (_deposit.value ?: 0L) + amount
            saveToPreferences()
            return true
        }
        return false
    }
    
    fun subtractDeposit(amount: Long): Boolean {
        val currentDeposit = _deposit.value ?: 0L
        if (amount <= currentDeposit) {
            _deposit.value = if (amount >= currentDeposit) 0L else currentDeposit - amount
            increaseAsset(amount)
            saveToPreferences()
            return true
        }
        return false
    }
    
    // 대출 관련 메서드
    fun addLoan(amount: Long) {
        _loan.value = (_loan.value ?: 0L) + amount
        increaseAsset(amount)
        saveToPreferences()
    }
    
    fun subtractLoan(amount: Long): Boolean {
        val currentLoan = _loan.value ?: 0L
        if (amount <= currentLoan) {
            _loan.value = if (amount >= currentLoan) 0L else currentLoan - amount
            saveToPreferences()
            return true
        }
        return false
    }
    
    // 이자 관련 메서드
    fun addLoanInterest(interest: Long) {
        _loan.value = (_loan.value ?: 0L) + interest
        saveToPreferences()
    }
    
    // 리셋 메서드
    fun resetAll() {
        _asset.value = DEFAULT_INITIAL_ASSET
        _deposit.value = 0L
        _loan.value = 0L
        saveToPreferences()
    }
    
    // 포맷팅 유틸리티
    fun formatNumber(number: Long): String {
        return FormatUtils.formatCurrency(number)
    }
    
    fun getAssetText(): String {
        val assetValue = _asset.value ?: 0L
        return "자산: ${formatNumber(assetValue)}원"
    }
} 