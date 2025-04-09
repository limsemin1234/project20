package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Timer
import java.util.TimerTask
import kotlin.math.roundToLong

/**
 * 예금 및 대출 이자 계산을 처리하는 클래스
 */
class InterestCalculator(
    private val repository: AssetRepository,
    private val application: Application
) {
    
    companion object {
        const val DEPOSIT_INTEREST_RATE = 0.03 // 3%
        const val LOAN_INTEREST_RATE = 0.10 // 10%
        
        // 이자 발생 주기 (밀리초)
        const val INTEREST_PERIOD_MS = 30_000L // 30초
        
        // 알림 제한 시간 (밀리초)
        private const val NOTIFICATION_LIMIT_MS = 10_000L // 10초
        
        // 게임 내 시간 가속 계수 (현실의 1년을 게임에서 몇 초로 압축할지)
        private const val TIME_ACCELERATION = 2160.0 // 1년을 4시간(14400초)으로 압축

        // SharedPreferences 키
        private const val PREF_DEPOSIT_TIME_REMAINING = "deposit_time_remaining"
        private const val PREF_LOAN_TIME_REMAINING = "loan_time_remaining"
        private const val PREF_LAST_SAVE_TIME = "last_save_time"
    }
    
    // 메인 스레드 핸들러 (백그라운드 스레드에서 메인 스레드로 작업을 전달하기 위함)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 이자 발생 타이머
    private var depositTimer: Timer? = null
    private var depositTimerTask: TimerTask? = null
    private var loanTimer: Timer? = null
    private var loanTimerTask: TimerTask? = null
    
    // 이자 발생 시간 관련 LiveData (초 단위로 변환)
    private val _depositTimeRemaining = MutableLiveData<Long>()
    val depositTimeRemaining: LiveData<Long> = _depositTimeRemaining
    
    private val _loanTimeRemaining = MutableLiveData<Long>()
    val loanTimeRemaining: LiveData<Long> = _loanTimeRemaining
    
    // 이자 발생 활성화 상태
    private var isDepositTimerActive = false
    private var isLoanTimerActive = false
    
    // 알림 메시지 관련 LiveData
    private val _interestNotification = MutableLiveData<String>()
    val interestNotification: LiveData<String> = _interestNotification
    
    // 알림 타임스탬프
    private val _lastNotificationTimestamp = MutableLiveData<Long>()
    val lastNotificationTimestamp: LiveData<Long> = _lastNotificationTimestamp
    
    // 마지막 이자 발생 시간
    private var lastDepositInterestTime: Long = 0
    private var lastLoanInterestTime: Long = 0

    init {
        // SharedPreferences에서 이자 쿨타임 복원
        restoreInterestTimes()
    }
    
    /**
     * SharedPreferences에서 이자 쿨타임 복원
     */
    private fun restoreInterestTimes() {
        val prefs = application.getSharedPreferences("interest_times", Context.MODE_PRIVATE)
        
        // 예금 쿨타임 복원
        val savedDepositTime = prefs?.getLong(PREF_DEPOSIT_TIME_REMAINING, INTEREST_PERIOD_MS / 1000) ?: (INTEREST_PERIOD_MS / 1000)
        _depositTimeRemaining.value = savedDepositTime
        
        // 대출 쿨타임 복원
        val savedLoanTime = prefs?.getLong(PREF_LOAN_TIME_REMAINING, INTEREST_PERIOD_MS / 1000) ?: (INTEREST_PERIOD_MS / 1000)
        _loanTimeRemaining.value = savedLoanTime
    }
    
    /**
     * 이자 쿨타임을 SharedPreferences에 저장
     */
    private fun saveInterestTimes() {
        val prefs = application.getSharedPreferences("interest_times", Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putLong(PREF_DEPOSIT_TIME_REMAINING, _depositTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000))
            putLong(PREF_LOAN_TIME_REMAINING, _loanTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000))
            apply()
        }
    }
    
    /**
     * 예금 이자 계산 타이머 시작
     */
    private fun startDepositTimer() {
        stopDepositTimer() // 기존 타이머가 있다면 정지
        
        depositTimer = Timer()
        depositTimerTask = object : TimerTask() {
            override fun run() {
                calculateDepositInterest()
            }
        }
        
        depositTimer?.scheduleAtFixedRate(depositTimerTask, 0, 1000) // 1초마다 실행
    }
    
    /**
     * 대출 이자 계산 타이머 시작
     */
    private fun startLoanTimer() {
        stopLoanTimer() // 기존 타이머가 있다면 정지
        
        loanTimer = Timer()
        loanTimerTask = object : TimerTask() {
            override fun run() {
                calculateLoanInterest()
            }
        }
        
        loanTimer?.scheduleAtFixedRate(loanTimerTask, 0, 1000) // 1초마다 실행
    }
    
    /**
     * 예금 이자 계산 타이머 중지
     */
    fun stopDepositTimer() {
        depositTimerTask?.cancel()
        depositTimerTask = null
        
        depositTimer?.cancel()
        depositTimer?.purge()
        depositTimer = null
        
        isDepositTimerActive = false
    }
    
    /**
     * 대출 이자 계산 타이머 중지
     */
    fun stopLoanTimer() {
        loanTimerTask?.cancel()
        loanTimerTask = null
        
        loanTimer?.cancel()
        loanTimer?.purge()
        loanTimer = null
        
        isLoanTimerActive = false
    }
    
    /**
     * 예금 이자 계산
     */
    private fun calculateDepositInterest() {
        val deposit = repository.deposit.value ?: 0L
        
        if (deposit > 0) {
            val remainingTime = (_depositTimeRemaining.value ?: 0L) - 1
            _depositTimeRemaining.postValue(remainingTime)
            
            if (remainingTime <= 0) {
                val interest = (deposit * DEPOSIT_INTEREST_RATE).roundToLong()
                mainHandler.post {
                    repository.increaseAsset(interest)  // 이자를 자산에 추가
                    repository.addDeposit(interest)     // 이자를 예금에 추가
                    val message = "예금 이자 ${repository.formatNumber(interest)}원이 발생했습니다"
                    _interestNotification.postValue(message)
                    _lastNotificationTimestamp.postValue(System.currentTimeMillis())
                    MessageManager.showMessage(application, message)
                }
                resetDepositTimer()
            }
        }
    }
    
    /**
     * 대출 이자 계산
     */
    private fun calculateLoanInterest() {
        val loan = repository.loan.value ?: 0L
        
        if (loan > 0) {
            val remainingTime = (_loanTimeRemaining.value ?: 0L) - 1
            _loanTimeRemaining.postValue(remainingTime)
            
            if (remainingTime <= 0) {
                val interest = (loan * LOAN_INTEREST_RATE).roundToLong()
                mainHandler.post {
                    repository.decreaseAsset(interest)  // 이자를 자산에서 차감
                    repository.addLoan(interest)        // 이자를 대출에 추가
                    val message = "대출 이자 ${repository.formatNumber(interest)}원이 발생했습니다"
                    _interestNotification.postValue(message)
                    _lastNotificationTimestamp.postValue(System.currentTimeMillis())
                    MessageManager.showMessage(application, message)
                }
                resetLoanTimer()
            }
        }
    }
    
    /**
     * 예금 이벤트 발생 시 타이머 재설정
     */
    fun resetDepositTimer() {
        val depositAmount = repository.deposit.value ?: 0L
        if (depositAmount > 0) {
            // 예금이 있으면 타이머 활성화 및 재설정
            isDepositTimerActive = true
            _depositTimeRemaining.postValue(INTEREST_PERIOD_MS / 1000)
            if (depositTimer == null) {
                startDepositTimer()
            }
        } else {
            // 예금이 없으면 타이머 비활성화 및 쿨타임 초기화
            stopDepositTimer()
            _depositTimeRemaining.postValue(INTEREST_PERIOD_MS / 1000)
        }
    }
    
    /**
     * 대출 이벤트 발생 시 타이머 재설정
     */
    fun resetLoanTimer() {
        val loanAmount = repository.loan.value ?: 0L
        if (loanAmount > 0) {
            // 대출이 있으면 타이머 활성화 및 재설정
            isLoanTimerActive = true
            _loanTimeRemaining.postValue(INTEREST_PERIOD_MS / 1000)
            if (loanTimer == null) {
                startLoanTimer()
            }
        } else {
            // 대출이 없으면 타이머 비활성화 및 쿨타임 초기화
            stopLoanTimer()
            _loanTimeRemaining.postValue(INTEREST_PERIOD_MS / 1000)
        }
    }
    
    /**
     * 현재 예금/대출 상태를 확인하고 타이머 상태 업데이트
     */
    private fun checkAndUpdateTimerStates() {
        val depositAmount = repository.deposit.value ?: 0L
        val loanAmount = repository.loan.value ?: 0L
        
        isDepositTimerActive = depositAmount > 0
        isLoanTimerActive = loanAmount > 0
    }
    
    /**
     * 예금 이자 발생까지 남은 시간 포맷팅
     */
    fun formatDepositRemainingTime(): String {
        val timeRemaining = _depositTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000)
        return "$timeRemaining"
    }
    
    /**
     * 대출 이자 발생까지 남은 시간 포맷팅
     */
    fun formatLoanRemainingTime(): String {
        val timeRemaining = _loanTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000)
        return "$timeRemaining"
    }
    
    /**
     * 리소스 해제
     */
    fun cleanup() {
        stopDepositTimer()
        stopLoanTimer()
        // 마지막으로 쿨타임 저장
        saveInterestTimes()
    }
} 