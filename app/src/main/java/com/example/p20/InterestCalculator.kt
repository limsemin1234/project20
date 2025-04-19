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
        // SharedPreferences 키
        private const val PREF_DEPOSIT_TIME_REMAINING = "deposit_time_remaining"
        private const val PREF_LOAN_TIME_REMAINING = "loan_time_remaining"
        private const val PREF_LAST_SAVE_TIME = "last_save_time"
        private const val PREF_TOTAL_DEPOSIT_INTEREST = "total_deposit_interest"
        private const val PREF_TOTAL_LOAN_INTEREST = "total_loan_interest"
        
        // 알림 제한 시간 (밀리초)
        private const val NOTIFICATION_LIMIT_MS = 10_000L // 10초
        
        // 게임 내 시간 가속 계수 (현실의 1년을 게임에서 몇 초로 압축할지)
        private const val TIME_ACCELERATION = 2160.0 // 1년을 4시간(14400초)으로 압축
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
    
    // 추가: 누적 이자 정보 LiveData
    private val _totalDepositInterest = MutableLiveData<Long>()
    val totalDepositInterest: LiveData<Long> = _totalDepositInterest
    
    private val _totalLoanInterest = MutableLiveData<Long>()
    val totalLoanInterest: LiveData<Long> = _totalLoanInterest

    init {
        // SharedPreferences에서 이자 쿨타임 복원
        restoreInterestTimes()
        // 추가: 누적 이자 정보 로드
        restoreTotalInterestData()
    }
    
    /**
     * SharedPreferences에서 이자 쿨타임 복원
     */
    private fun restoreInterestTimes() {
        val prefs = application.getSharedPreferences("interest_times", Context.MODE_PRIVATE)
        
        // 이자 간격을 초 단위로 변환
        val interestIntervalSeconds = Constants.INTEREST_INTERVAL_SECONDS
        
        // 예금 쿨타임 복원
        val savedDepositTime = prefs?.getLong(PREF_DEPOSIT_TIME_REMAINING, interestIntervalSeconds) ?: interestIntervalSeconds
        _depositTimeRemaining.value = savedDepositTime
        
        // 대출 쿨타임 복원
        val savedLoanTime = prefs?.getLong(PREF_LOAN_TIME_REMAINING, interestIntervalSeconds) ?: interestIntervalSeconds
        _loanTimeRemaining.value = savedLoanTime
    }
    
    /**
     * 추가: 누적 이자 정보 로드
     */
    private fun restoreTotalInterestData() {
        val prefs = application.getSharedPreferences("interest_data", Context.MODE_PRIVATE)
        
        // 총 예금 이자 로드
        val savedTotalDepositInterest = prefs?.getLong(PREF_TOTAL_DEPOSIT_INTEREST, 0L) ?: 0L
        _totalDepositInterest.value = savedTotalDepositInterest
        
        // 총 대출 이자 로드
        val savedTotalLoanInterest = prefs?.getLong(PREF_TOTAL_LOAN_INTEREST, 0L) ?: 0L
        _totalLoanInterest.value = savedTotalLoanInterest
    }
    
    /**
     * 이자 쿨타임을 SharedPreferences에 저장
     */
    private fun saveInterestTimes() {
        val prefs = application.getSharedPreferences("interest_times", Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putLong(PREF_DEPOSIT_TIME_REMAINING, _depositTimeRemaining.value ?: Constants.INTEREST_INTERVAL_SECONDS)
            putLong(PREF_LOAN_TIME_REMAINING, _loanTimeRemaining.value ?: Constants.INTEREST_INTERVAL_SECONDS)
            apply()
        }
    }
    
    /**
     * 추가: 누적 이자 정보 저장
     */
    private fun saveTotalInterestData() {
        val prefs = application.getSharedPreferences("interest_data", Context.MODE_PRIVATE)
        prefs?.edit()?.apply {
            putLong(PREF_TOTAL_DEPOSIT_INTEREST, _totalDepositInterest.value ?: 0L)
            putLong(PREF_TOTAL_LOAN_INTEREST, _totalLoanInterest.value ?: 0L)
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
        // 현재 예금 및 쿨타임 가져오기
        val deposit = repository.deposit.value ?: 0L
        var timeRemaining = _depositTimeRemaining.value ?: Constants.INTEREST_INTERVAL_SECONDS
        
        // 예금이 없으면 계산하지 않음
        if (deposit <= 0) {
            _depositTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
            return
        }
        
        // 쿨타임 감소
        timeRemaining--
        _depositTimeRemaining.postValue(timeRemaining)
        
        // 쿨타임이 0이 되면 이자 발생
        if (timeRemaining <= 0) {
            // 이자 계산 (1%)
            val interest = (deposit * Constants.DEPOSIT_INTEREST_RATE).roundToLong()
            
            // 자산에 이자 추가
            mainHandler.post {
                repository.increaseAsset(interest)
                
                // 이자 발생 메시지 표시
                showInterestNotification("예금 이자 +${FormatUtils.formatCurrency(interest)}원이 추가되었습니다")
                
                // 누적 이자 정보 갱신
                val currentTotalInterest = _totalDepositInterest.value ?: 0L
                _totalDepositInterest.value = currentTotalInterest + interest
                saveTotalInterestData()
            }
            
            // 쿨타임 리셋
            _depositTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
        }
        
        // 쿨타임 저장
        saveInterestTimes()
    }
    
    /**
     * 대출 이자 계산
     */
    private fun calculateLoanInterest() {
        // 현재 대출 및 쿨타임 가져오기
        val loan = repository.loan.value ?: 0L
        var timeRemaining = _loanTimeRemaining.value ?: Constants.INTEREST_INTERVAL_SECONDS
        
        // 대출이 없으면 계산하지 않음
        if (loan <= 0) {
            _loanTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
            return
        }
        
        // 쿨타임 감소
        timeRemaining--
        _loanTimeRemaining.postValue(timeRemaining)
        
        // 쿨타임이 0이 되면 이자 발생
        if (timeRemaining <= 0) {
            // 이자 계산 (5%)
            val interest = (loan * Constants.LOAN_INTEREST_RATE).roundToLong()
            
            // 자산에서 이자 차감
            mainHandler.post {
                repository.decreaseAsset(interest)
                
                // 이자 발생 메시지 표시
                showInterestNotification("대출 이자 -${FormatUtils.formatCurrency(interest)}원이 차감되었습니다")
                
                // 누적 이자 정보 갱신
                val currentTotalInterest = _totalLoanInterest.value ?: 0L
                _totalLoanInterest.value = currentTotalInterest + interest
                saveTotalInterestData()
            }
            
            // 쿨타임 리셋
            _loanTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
        }
        
        // 쿨타임 저장
        saveInterestTimes()
    }
    
    /**
     * 총 예금 이자 계산
     */
    fun calculateNextDepositInterest(): Long {
        val deposit = repository.deposit.value ?: 0L
        return (deposit * Constants.DEPOSIT_INTEREST_RATE).roundToLong()
    }
    
    /**
     * 총 대출 이자 계산
     */
    fun calculateNextLoanInterest(): Long {
        val loan = repository.loan.value ?: 0L
        return (loan * Constants.LOAN_INTEREST_RATE).roundToLong()
    }
    
    /**
     * 누적 이자 데이터 초기화
     */
    fun resetTotalInterestData() {
        _totalDepositInterest.value = 0L
        _totalLoanInterest.value = 0L
        saveTotalInterestData()
    }
    
    /**
     * 예금 이벤트 발생 시 타이머 재설정
     */
    fun resetDepositTimer() {
        val depositAmount = repository.deposit.value ?: 0L
        if (depositAmount > 0) {
            // 예금이 있으면 타이머 활성화 및 재설정
            isDepositTimerActive = true
            _depositTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
            if (depositTimer == null) {
                startDepositTimer()
            }
        } else {
            // 예금이 없으면 타이머 비활성화 및 쿨타임 초기화
            stopDepositTimer()
            _depositTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
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
            _loanTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
            if (loanTimer == null) {
                startLoanTimer()
            }
        } else {
            // 대출이 없으면 타이머 비활성화 및 쿨타임 초기화
            stopLoanTimer()
            _loanTimeRemaining.postValue(Constants.INTEREST_INTERVAL_SECONDS)
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
        val timeRemaining = _depositTimeRemaining.value ?: Constants.INTEREST_INTERVAL_SECONDS
        return "$timeRemaining"
    }
    
    /**
     * 대출 이자 발생까지 남은 시간 포맷팅
     */
    fun formatLoanRemainingTime(): String {
        val timeRemaining = _loanTimeRemaining.value ?: Constants.INTEREST_INTERVAL_SECONDS
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
    
    /**
     * 이자 알림 메시지를 표시합니다.
     * 
     * @param message 표시할 알림 메시지
     */
    private fun showInterestNotification(message: String) {
        _interestNotification.postValue(message)
        _lastNotificationTimestamp.postValue(System.currentTimeMillis())
        MessageManager.showMessage(application, message)
    }
} 