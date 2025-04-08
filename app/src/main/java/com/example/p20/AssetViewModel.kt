package com.example.p20

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
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

    private val _depositRemainingTime = MutableLiveData<Long>()
    val depositRemainingTime: LiveData<Long> = _depositRemainingTime

    private val _loanRemainingTime = MutableLiveData<Long>()
    val loanRemainingTime: LiveData<Long> = _loanRemainingTime

    // 알림이 이미 표시되었는지 추적하는 플래그
    private var _lastNotificationTimestamp = MutableLiveData<Long>()
    val lastNotificationTimestamp: LiveData<Long> = _lastNotificationTimestamp
    
    // 마지막 예금 이자 지급 시간과 대출 이자 발생 시간 추적
    private var lastDepositInterestTime: Long = 0
    private var lastLoanInterestTime: Long = 0
    
    private var depositTimer: CountDownTimer? = null
    private var loanTimer: CountDownTimer? = null
    
    // 타이머가 활성화되어 있는지 추적
    private var isDepositTimerActive = false
    private var isLoanTimerActive = false
    
    // 핸들러 추가
    private val handler = Handler(Looper.getMainLooper())

    init {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val savedAsset = sharedPreferences.getLong("asset", 500_000L)
        val savedDeposit = sharedPreferences.getLong("deposit", 0L)
        val savedLoan = sharedPreferences.getLong("loan", 0L)
        
        _asset.value = savedAsset
        _deposit.value = savedDeposit
        _loan.value = savedLoan
        
        // 초기 타이머 설정 (필요한 경우에만)
        if (savedDeposit > 0) {
            startDepositTimer()
        } else {
            _depositRemainingTime.value = 0L
        }
        
        if (savedLoan > 0) {
            startLoanTimer()
        } else {
            _loanRemainingTime.value = 0L
        }
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
        _asset.value = sharedPreferences.getLong("asset", 500_000L)
        _deposit.value = sharedPreferences.getLong("deposit", 0L)
        _loan.value = sharedPreferences.getLong("loan", 0L)
    }

    fun getAssetText(): String {
        val assetValue = _asset.value ?: 0L
        return "자산: ${"%,d".format(assetValue)}원"
    }

    fun resetAsset() {
        _asset.value = 500_000L
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
        stopDepositTimer()
        stopLoanTimer()
        
        // 값 초기화
        _asset.value = 500_000L
        _deposit.value = 0L
        _loan.value = 0L
        
        // 변경사항 저장
        saveAssetToPreferences()
    }

    fun setAsset(value: Long) {
        _asset.value = value
        saveAssetToPreferences()
    }

    fun addDeposit(amount: Long) {
        _deposit.value = (_deposit.value ?: 0L) + amount
        if (!isDepositTimerActive) {
            startDepositTimer()
        }
        saveAssetToPreferences()
    }

    fun subtractDeposit(amount: Long) {
        val currentDeposit = _deposit.value ?: 0L
        if (amount >= currentDeposit) {
            _deposit.value = 0L
            stopDepositTimer()
        } else {
            _deposit.value = currentDeposit - amount
        }
        saveAssetToPreferences()
    }

    fun addLoan(amount: Long) {
        _loan.value = (_loan.value ?: 0L) + amount
        _asset.value = (_asset.value ?: 0L) + amount
        if (!isLoanTimerActive) {
            startLoanTimer()
        }
        saveAssetToPreferences()
    }

    fun subtractLoan(amount: Long) {
        val currentLoan = _loan.value ?: 0L
        if (amount >= currentLoan) {
            _loan.value = 0L
            stopLoanTimer()
        } else {
            _loan.value = currentLoan - amount
        }
        saveAssetToPreferences()
    }

    private fun stopDepositTimer() {
        depositTimer?.cancel()
        depositTimer = null
        isDepositTimerActive = false
        _depositRemainingTime.value = 0L
    }

    private fun stopLoanTimer() {
        loanTimer?.cancel()
        loanTimer = null
        isLoanTimerActive = false
        _loanRemainingTime.value = 0L
    }

    private fun startDepositTimer() {
        // 기존 타이머 취소
        stopDepositTimer()
        
        val currentDeposit = _deposit.value ?: 0L
        if (currentDeposit <= 0) {
            return  // 예금이 없으면 타이머 시작하지 않음
        }
        
        android.util.Log.d("AssetViewModel", "예금 타이머 시작: ${currentDeposit}원")
        
        isDepositTimerActive = true
        depositTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _depositRemainingTime.value = millisUntilFinished / 1000
            }

            override fun onFinish() {
                val deposit = _deposit.value ?: 0L
                if (deposit > 0) {
                    val interest = (deposit * 0.03).toLong()
                    _deposit.value = deposit + interest
                    _asset.value = (_asset.value ?: 0L) + interest
                    
                    // 현재 시간 기록
                    val currentTime = System.currentTimeMillis()
                    
                    // 마지막 이자 지급 시간과 충분한 차이가 있을 때만 알림 표시
                    if (currentTime - lastDepositInterestTime > 10000) { // 10초 이상 차이가 있을 때만
                        // 알림 메시지와 타임스탬프 업데이트
                        val message = "예금 이자 ${formatNumber(interest)}원이 지급되었습니다"
                        _interestNotification.value = message
                        _lastNotificationTimestamp.value = currentTime
                        
                        // MessageManager로 메시지 표시
                        MessageManager.showMessage(context, message)
                        
                        // 로그로 알림 확인
                        android.util.Log.d("AssetViewModel", "예금 이자 지급: ${formatNumber(interest)}원")
                        
                        // 마지막 이자 지급 시간 업데이트
                        lastDepositInterestTime = currentTime
                    } else {
                        android.util.Log.d("AssetViewModel", "예금 이자 발생 (알림 없음): ${formatNumber(interest)}원")
                    }
                    
                    saveAssetToPreferences()
                    
                    // 다음 이자 계산을 위해 새 타이머 시작
                    handler.post {
                        startDepositTimer()
                    }
                } else {
                    stopDepositTimer()
                }
            }
        }.start()
    }

    private fun startLoanTimer() {
        // 기존 타이머 취소
        stopLoanTimer()
        
        val currentLoan = _loan.value ?: 0L
        if (currentLoan <= 0) {
            return  // 대출이 없으면 타이머 시작하지 않음
        }
        
        android.util.Log.d("AssetViewModel", "대출 타이머 시작: ${currentLoan}원")
        
        isLoanTimerActive = true
        loanTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _loanRemainingTime.value = millisUntilFinished / 1000
            }

            override fun onFinish() {
                val loan = _loan.value ?: 0L
                if (loan > 0) {
                    val interest = (loan * 0.10).toLong()
                    _loan.value = loan + interest
                    
                    // 현재 시간 기록
                    val currentTime = System.currentTimeMillis()
                    
                    // 마지막 이자 발생 시간과 충분한 차이가 있을 때만 알림 표시
                    if (currentTime - lastLoanInterestTime > 10000) { // 10초 이상 차이가 있을 때만
                        // 알림 메시지와 타임스탬프 업데이트
                        val message = "대출 이자 ${formatNumber(interest)}원이 발생했습니다"
                        _interestNotification.value = message
                        _lastNotificationTimestamp.value = currentTime
                        
                        // MessageManager로 메시지 표시
                        MessageManager.showMessage(context, message)
                        
                        // 로그로 알림 확인
                        android.util.Log.d("AssetViewModel", "대출 이자 발생: ${formatNumber(interest)}원")
                        
                        // 마지막 이자 발생 시간 업데이트
                        lastLoanInterestTime = currentTime
                    } else {
                        android.util.Log.d("AssetViewModel", "대출 이자 발생 (알림 없음): ${formatNumber(interest)}원")
                    }
                    
                    saveAssetToPreferences()
                    
                    // 다음 이자 계산을 위해 새 타이머 시작
                    handler.post {
                        startLoanTimer()
                    }
                } else {
                    stopLoanTimer()
                }
            }
        }.start()
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }

    override fun onCleared() {
        super.onCleared()
        stopDepositTimer()
        stopLoanTimer()
    }
}
