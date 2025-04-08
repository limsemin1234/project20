package com.example.p20

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * 자산, 예금, 대출 관리를 담당하는 ViewModel
 * Repository 패턴과 계산 로직 분리를 통한 코드 구조화
 */
class AssetViewModel(
    private val context: Context,
    private val repository: AssetRepository = AssetRepository(context),
    private val calculator: InterestCalculator = InterestCalculator(repository, context)
) : ViewModel() {

    // Repository에서 제공하는 LiveData
    val asset: LiveData<Long> = repository.asset
    val deposit: LiveData<Long> = repository.deposit
    val loan: LiveData<Long> = repository.loan
    
    // Calculator에서 제공하는 LiveData
    val depositRemainingTime: LiveData<Long> = calculator.depositTimeRemaining
    val loanRemainingTime: LiveData<Long> = calculator.loanTimeRemaining
    val interestNotification: LiveData<String> = calculator.interestNotification
    val lastNotificationTimestamp: LiveData<Long> = calculator.lastNotificationTimestamp
    
    // 부동산 정보
    private val _realEstateList = MutableLiveData<List<RealEstate>>()
    val realEstateList: LiveData<List<RealEstate>> get() = _realEstateList

    // 활성화된 대출 정보
    private val _depositActive = MutableLiveData<Boolean>()
    val depositActive: LiveData<Boolean> get() = _depositActive

    private val _loanActive = MutableLiveData<Boolean>()
    val loanActive: LiveData<Boolean> get() = _loanActive

    init {
        // 예금 또는 대출이 있는 경우 이자 계산 타이머 시작
        if (repository.deposit.value ?: 0L > 0 || repository.loan.value ?: 0L > 0) {
            calculator.startInterestTimer()
        }
        
        // 예금 이자가 발생하는 경우의 콜백 설정
        observeRepositoryChanges()

        // 활성화 상태 초기화
        _depositActive.value = repository.deposit.value ?: 0L > 0
        _loanActive.value = repository.loan.value ?: 0L > 0
    }
    
    /**
     * Repository 변경사항을 관찰하여 필요한 작업 수행
     */
    private fun observeRepositoryChanges() {
        // 예금 또는 대출 변화 감지
        repository.deposit.observeForever { newDeposit ->
            if (newDeposit > 0 && (repository.loan.value ?: 0L > 0)) {
                calculator.startInterestTimer()
            } else if (newDeposit == 0L && (repository.loan.value ?: 0L == 0L)) {
                calculator.stopInterestTimer()
            }
        }
        
        repository.loan.observeForever { newLoan ->
            if (newLoan > 0 && (repository.deposit.value ?: 0L > 0)) {
                calculator.startInterestTimer()
            } else if (newLoan == 0L && (repository.deposit.value ?: 0L == 0L)) {
                calculator.stopInterestTimer()
            }
        }
    }

    /**
     * 자산 증가
     */
    fun increaseAsset(amount: Long) {
        repository.increaseAsset(amount)
    }

    /**
     * 자산 감소
     */
    fun decreaseAsset(amount: Long): Boolean {
        return repository.decreaseAsset(amount)
    }

    /**
     * 자산 설정
     */
    fun setAsset(value: Long) {
        repository.updateAsset(value)
    }

    /**
     * 예금 추가
     */
    fun addDeposit(amount: Long) {
        // 자산에서 해당 금액 차감
        if (repository.decreaseAsset(amount)) {
            repository.addDeposit(amount)
            // 예금 타이머 재설정
            calculator.resetDepositTimer()
            showMessage("${formatNumber(amount)}원이 예금되었습니다")
        } else {
            showMessage("보유 자산이 부족합니다")
        }
    }

    /**
     * 예금 출금
     */
    fun subtractDeposit(amount: Long): Boolean {
        if (repository.subtractDeposit(amount)) {
            repository.increaseAsset(amount)
            // 예금 타이머 재설정
            calculator.resetDepositTimer()
            showMessage("${formatNumber(amount)}원이 출금되었습니다")
            return true
        }
        showMessage("예금 금액이 부족합니다")
        return false
    }

    /**
     * 대출 추가
     */
    fun addLoan(amount: Long) {
        repository.addLoan(amount)
        // 대출 타이머 재설정
        calculator.resetLoanTimer()
        showMessage("${formatNumber(amount)}원을 대출했습니다")
    }

    /**
     * 대출 상환
     */
    fun subtractLoan(amount: Long, earlyRepaymentFee: Long = 0): Boolean {
        val totalAmount = amount + earlyRepaymentFee
        
        // 자산에서 대출금 + 수수료 차감
        if (repository.decreaseAsset(totalAmount)) {
            repository.subtractLoan(amount)
            // 대출 타이머 재설정
            calculator.resetLoanTimer()
            
            if (earlyRepaymentFee > 0) {
                showMessage("${formatNumber(amount)}원 상환 완료 (조기상환 수수료: ${formatNumber(earlyRepaymentFee)}원)")
            } else {
                showMessage("${formatNumber(amount)}원을 상환했습니다")
            }
            return true
        }
        
        showMessage("보유 자산이 부족합니다 (필요: ${formatNumber(totalAmount)}원)")
        return false
    }

    /**
     * 조기 상환 수수료 계산
     */
    fun calculateEarlyRepaymentFee(amount: Long): Long {
        return calculator.calculateEarlyRepaymentFee(amount)
    }

    /**
     * 자산 표시 텍스트 포맷
     */
    fun getAssetText(): String {
        return "자산: ${formatNumber(repository.asset.value ?: 0)}원"
    }

    /**
     * 메시지 표시 및 알림 설정
     */
    private fun showMessage(message: String) {
        MessageManager.showMessage(context, message)
    }

    /**
     * 전체 자산 초기화
     */
    fun resetAssets() {
        calculator.stopInterestTimer()
        repository.resetAll()
    }

    /**
     * 숫자 포맷팅
     */
    fun formatNumber(number: Long): String {
        return repository.formatNumber(number)
    }

    /**
     * 예금 이자 발생까지 남은 시간 포맷팅
     */
    fun getDepositRemainingTimeText(): String {
        return calculator.formatDepositRemainingTime()
    }

    /**
     * 대출 이자 발생까지 남은 시간 포맷팅
     */
    fun getLoanRemainingTimeText(): String {
        return calculator.formatLoanRemainingTime()
    }

    /**
     * 자산 데이터를 저장
     */
    fun saveAssetToPreferences() {
        repository.saveToPreferences()
    }

    /**
     * ViewModel 정리
     */
    override fun onCleared() {
        super.onCleared()
        calculator.cleanup()
    }
}
