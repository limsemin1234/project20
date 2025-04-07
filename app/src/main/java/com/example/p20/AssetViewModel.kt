package com.example.p20

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.NumberFormat
import java.util.Locale

class AssetViewModel(private val context: Context) : ViewModel() {

    private val _asset = MutableLiveData<Long>()
    val asset: LiveData<Long> get() = _asset

    private val _realEstateList = MutableLiveData<List<RealEstate>>()
    val realEstateList: LiveData<List<RealEstate>> get() = _realEstateList

    private val _deposit = MutableLiveData<Long>()
    val deposit: LiveData<Long> = _deposit

    private val _loan = MutableLiveData<Long>()
    val loan: LiveData<Long> = _loan

    private val _interestNotification = MutableLiveData<String>()
    val interestNotification: LiveData<String> = _interestNotification

    private var depositTimer: CountDownTimer? = null
    private var loanTimer: CountDownTimer? = null

    init {
        // --- 수정: SharedPreferences에서 자산 로드 활성화 ---
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        // --- 수정: 기본 자산 50만원으로 변경 ---
        val savedAsset = sharedPreferences.getLong("asset", 500_000L) // 저장된 값 로드, 없으면 50만원
        val savedDeposit = sharedPreferences.getLong("deposit", 0L)
        val savedLoan = sharedPreferences.getLong("loan", 0L)
        // --- 수정 끝 ---
        _asset.value = savedAsset
        _deposit.value = savedDeposit
        _loan.value = savedLoan
        // _asset.value = 40_000_000L // 항상 4천만원으로 시작하는 코드 삭제 또는 주석 처리
        // saveAssetToPreferences() // 시작 시 저장 로직은 필요 없음 (로드 실패 시 기본값 사용)
        // --- 수정 끝 ---
        startInterestTimers()
    }

    private fun startInterestTimers() {
        // 예금 이자 타이머 (3% 이자, 60초마다 지급)
        depositTimer = object : CountDownTimer(Long.MAX_VALUE, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentDeposit = _deposit.value ?: 0L
                if (currentDeposit > 0) {
                    val interest = (currentDeposit * 0.03).toLong()
                    _deposit.value = currentDeposit + interest
                    _asset.value = (_asset.value ?: 0L) + interest
                    _interestNotification.value = "예금 이자 ${formatNumber(interest)}원이 지급되었습니다"
                } else {
                    // 예금이 0원이면 타이머 중지
                    cancel()
                    depositTimer = null
                }
            }

            override fun onFinish() {}
        }.start()

        // 대출 이자 타이머 (10% 이자, 60초마다 발생)
        loanTimer = object : CountDownTimer(Long.MAX_VALUE, 60000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentLoan = _loan.value ?: 0L
                if (currentLoan > 0) {
                    val interest = (currentLoan * 0.10).toLong()
                    _loan.value = currentLoan + interest
                    _interestNotification.value = "대출 이자 ${formatNumber(interest)}원이 발생했습니다"
                } else {
                    // 대출이 0원이면 타이머 중지
                    cancel()
                    loanTimer = null
                }
            }

            override fun onFinish() {}
        }.start()
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
        with(sharedPreferences.edit()) {
            putLong("asset", _asset.value ?: 0L)
            putLong("deposit", _deposit.value ?: 0L)
            putLong("loan", _loan.value ?: 0L)
            apply()
        }
    }

    private fun loadAssetFromPreferences() {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        _asset.value = sharedPreferences.getLong("asset", 500_000L) // 기본값 50만원
        _deposit.value = sharedPreferences.getLong("deposit", 0L)
        _loan.value = sharedPreferences.getLong("loan", 0L)
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
        // 기존 타이머 취소
        depositTimer?.cancel()
        loanTimer?.cancel()
        
        // 값 초기화
        _asset.value = 500_000L // 기본 자산 50만원으로 초기화
        _deposit.value = 0L // 예금 초기화
        _loan.value = 0L // 대출 초기화
        
        // 새로운 타이머 시작
        startInterestTimers()
        
        // 변경사항 저장
        saveAssetToPreferences()
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
        _asset.value = (_asset.value ?: 0L) + amount
    }

    fun subtractLoan(amount: Long) {
        _loan.value = (_loan.value ?: 0L) - amount
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }

    // --- 추가: ViewModel 소멸 시 자산 저장 ---
    override fun onCleared() {
        super.onCleared()
        saveAssetToPreferences() // ViewModel이 제거될 때 현재 자산 저장
        depositTimer?.cancel()
        loanTimer?.cancel()
    }
    // --- 추가 끝 ---
}
