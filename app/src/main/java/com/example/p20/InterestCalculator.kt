package com.example.p20

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
    private val context: Context? = null
) {
    
    companion object {
        const val DEPOSIT_INTEREST_RATE = 0.03 // 3%
        const val LOAN_INTEREST_RATE = 0.10 // 10%
        const val EARLY_REPAYMENT_FEE_RATE = 0.05 // 5%
        
        // 이자 발생 주기 (밀리초)
        const val INTEREST_PERIOD_MS = 30_000L // 30초
        
        // 알림 제한 시간 (밀리초)
        private const val NOTIFICATION_LIMIT_MS = 10_000L // 10초
        
        // 게임 내 시간 가속 계수 (현실의 1년을 게임에서 몇 초로 압축할지)
        private const val TIME_ACCELERATION = 2160.0 // 1년을 4시간(14400초)으로 압축
    }
    
    // 메인 스레드 핸들러 (백그라운드 스레드에서 메인 스레드로 작업을 전달하기 위함)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 이자 발생 타이머
    private var interestTimer: Timer? = null
    
    // 이자 발생 시간 관련 LiveData (초 단위로 변환)
    private val _depositTimeRemaining = MutableLiveData<Long>(INTEREST_PERIOD_MS / 1000)
    val depositTimeRemaining: LiveData<Long> get() = _depositTimeRemaining
    
    private val _loanTimeRemaining = MutableLiveData<Long>(INTEREST_PERIOD_MS / 1000)
    val loanTimeRemaining: LiveData<Long> get() = _loanTimeRemaining
    
    // 이자 발생 활성화 상태
    private var isInterestTimerActive = false
    
    // 알림 메시지 관련 LiveData
    private val _interestNotification = MutableLiveData<String>()
    val interestNotification: LiveData<String> get() = _interestNotification
    
    // 알림 타임스탬프
    private val _lastNotificationTimestamp = MutableLiveData<Long>()
    val lastNotificationTimestamp: LiveData<Long> get() = _lastNotificationTimestamp
    
    // 마지막 이자 발생 시간
    private var lastDepositInterestTime: Long = 0
    private var lastLoanInterestTime: Long = 0
    
    /**
     * 이자 계산 타이머 시작
     */
    fun startInterestTimer() {
        if (isInterestTimerActive) return
        
        android.util.Log.d("InterestCalculator", "이자 계산 타이머 시작")
        
        isInterestTimerActive = true
        interestTimer = Timer()
        
        // 1초마다 타이머 업데이트
        interestTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // 백그라운드 스레드에서 메인 스레드로 작업 전달
                mainHandler.post {
                    updateTimers()
                }
            }
        }, 0, 1000)
    }
    
    /**
     * 이자 계산 타이머 중지
     */
    fun stopInterestTimer() {
        interestTimer?.cancel()
        interestTimer = null
        isInterestTimerActive = false
    }
    
    /**
     * 예금 및 대출 타이머 업데이트
     */
    private fun updateTimers() {
        // 예금 타이머 업데이트 (초 단위)
        val currentDepositTime = _depositTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000)
        if (currentDepositTime > 0) {
            _depositTimeRemaining.value = currentDepositTime - 1
        } else {
            applyDepositInterest()
            _depositTimeRemaining.value = INTEREST_PERIOD_MS / 1000
        }
        
        // 대출 타이머 업데이트 (초 단위)
        val currentLoanTime = _loanTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000)
        if (currentLoanTime > 0) {
            _loanTimeRemaining.value = currentLoanTime - 1
        } else {
            applyLoanInterest()
            _loanTimeRemaining.value = INTEREST_PERIOD_MS / 1000
        }
    }
    
    /**
     * 예금 이자 적용
     */
    private fun applyDepositInterest() {
        val currentDeposit = repository.deposit.value ?: 0L
        if (currentDeposit > 0) {
            // 예금 이자 계산 - 30초마다 3% 적용
            val interest = (currentDeposit * DEPOSIT_INTEREST_RATE).roundToLong()
            
            // 최소 이자 보장 (너무 작은 금액에도 이자가 발생하도록)
            val finalInterest = if (interest <= 0 && currentDeposit >= 10000) 1L else interest
            
            // 이자 적용
            if (finalInterest > 0) {
                repository.addDeposit(finalInterest)
                
                // 이자율 표시 메시지
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastDepositInterestTime > NOTIFICATION_LIMIT_MS) {
                    val message = "예금 이자 ${repository.formatNumber(finalInterest)}원이 지급되었습니다"
                    
                    // 알림 전송
                    showNotification(message)
                    
                    // 로그 기록
                    android.util.Log.d("InterestCalculator", "예금 이자 지급: ${repository.formatNumber(finalInterest)}원")
                    
                    // 마지막 이자 지급 시간 업데이트
                    lastDepositInterestTime = currentTime
                } else {
                    android.util.Log.d("InterestCalculator", "예금 이자 발생 (알림 없음): ${repository.formatNumber(finalInterest)}원")
                }
            }
        }
    }
    
    /**
     * 대출 이자 적용
     */
    private fun applyLoanInterest() {
        val currentLoan = repository.loan.value ?: 0L
        if (currentLoan > 0) {
            // 대출 이자 계산 - 30초마다 10% 적용
            val interest = (currentLoan * LOAN_INTEREST_RATE).roundToLong()
            
            // 최소 이자 보장 (너무 작은 금액에도 이자가 발생하도록)
            val finalInterest = if (interest <= 0 && currentLoan >= 10000) 1L else interest
            
            // 이자 적용
            if (finalInterest > 0) {
                repository.addLoan(finalInterest)
                
                // 이자율 표시 메시지
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastLoanInterestTime > NOTIFICATION_LIMIT_MS) {
                    val message = "대출 이자 ${repository.formatNumber(finalInterest)}원이 발생했습니다"
                    
                    // 알림 전송
                    showNotification(message)
                    
                    // 로그 기록
                    android.util.Log.d("InterestCalculator", "대출 이자 발생: ${repository.formatNumber(finalInterest)}원")
                    
                    // 마지막 이자 발생 시간 업데이트
                    lastLoanInterestTime = currentTime
                } else {
                    android.util.Log.d("InterestCalculator", "대출 이자 발생 (알림 없음): ${repository.formatNumber(finalInterest)}원")
                }
            }
        }
    }
    
    /**
     * 알림 표시
     */
    private fun showNotification(message: String) {
        _interestNotification.value = message
        _lastNotificationTimestamp.value = System.currentTimeMillis()
        
        // MessageManager로 알림 표시 (context가 있는 경우에만)
        context?.let {
            MessageManager.showMessage(it, message)
        }
    }
    
    /**
     * 예금 이자 발생까지 남은 시간 포맷팅
     */
    fun formatDepositRemainingTime(): String {
        val timeRemaining = _depositTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000)
        return String.format("%02d초", timeRemaining)
    }
    
    /**
     * 대출 이자 발생까지 남은 시간 포맷팅
     */
    fun formatLoanRemainingTime(): String {
        val timeRemaining = _loanTimeRemaining.value ?: (INTEREST_PERIOD_MS / 1000)
        return String.format("%02d초", timeRemaining)
    }
    
    /**
     * 대출 조기 상환 시 수수료 계산
     */
    fun calculateEarlyRepaymentFee(amount: Long): Long {
        return (amount * EARLY_REPAYMENT_FEE_RATE).roundToLong()
    }
    
    /**
     * 리소스 해제
     */
    fun cleanup() {
        stopInterestTimer()
    }
} 