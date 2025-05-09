package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.text.NumberFormat
import java.util.Locale

/**
 * 예금 기능을 담당하는 Fragment
 */
class DepositFragment : BaseFragment() {
    private lateinit var depositAmountInput: TextView
    private lateinit var depositButton: Button
    private lateinit var withdrawButton: Button
    private lateinit var withdrawAllButton: Button
    private lateinit var withdrawThousandButton: Button
    private lateinit var resetButton: Button
    private lateinit var btn1Man: Button
    private lateinit var btn10Man: Button
    private lateinit var btn100Man: Button
    private lateinit var btn1000Man: Button
    private lateinit var btn1Eok: Button
    private lateinit var btn10Eok: Button
    private lateinit var btn100Eok: Button
    private lateinit var btn1000Eok: Button
    private lateinit var totalInterestText: TextView
    private lateinit var accumulatedInterestText: TextView

    // 마지막으로 처리한 알림의 타임스탬프
    private var lastProcessedNotificationTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_deposit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기본 UI 요소 초기화
        depositAmountInput = view.findViewById(R.id.depositAmountInput)
        depositButton = view.findViewById(R.id.depositButton)
        withdrawButton = view.findViewById(R.id.withdrawButton)
        withdrawAllButton = view.findViewById(R.id.withdrawAllButton)
        withdrawThousandButton = view.findViewById(R.id.withdrawThousandButton)
        resetButton = view.findViewById(R.id.resetButton)
        
        // 초기값 설정
        setAmount(0L) // 입력: 0원으로 초기화
        
        // 이자 정보 텍스트 참조 가져오기 및 변경
        val interestInfoText = view.findViewWithTag<TextView>("interest_info_text")
        if (interestInfoText != null) {
            interestInfoText.text = "이자율: 1% (30초마다 자산에 추가)"
        }
        
        // 총 이자 텍스트 참조 찾기
        totalInterestText = view.findViewById(R.id.totalInterestText)
        
        // 누적 이자 텍스트 참조 찾기
        accumulatedInterestText = view.findViewById(R.id.accumulatedInterestText)
        
        // 이자 정보 업데이트
        updateInterestInfoTexts()
        
        btn1Man = view.findViewById(R.id.btn1Man)
        btn10Man = view.findViewById(R.id.btn10Man)
        btn100Man = view.findViewById(R.id.btn100Man)
        btn1000Man = view.findViewById(R.id.btn1000Man)
        btn1Eok = view.findViewById(R.id.btn1Eok)
        btn10Eok = view.findViewById(R.id.btn10Eok)
        btn100Eok = view.findViewById(R.id.btn100Eok)
        btn1000Eok = view.findViewById(R.id.btn1000Eok)

        // 예금 금액 변경 감지
        assetViewModel.deposit.observe(viewLifecycleOwner) { amount ->
            depositAmountInput.text = "예금: ${formatCurrency(amount)}"
            updateInterestInfoTexts()
        }
        
        // 이자 발생 타이머 관찰
        assetViewModel.depositRemainingTime.observe(viewLifecycleOwner) { _ ->
            updateInterestInfoTexts()
        }
        
        // 누적 이자 정보 관찰
        assetViewModel.totalDepositInterest.observe(viewLifecycleOwner) { totalInterest ->
            updateAccumulatedInterestText(totalInterest)
        }

        // 금액 초기화 버튼 클릭 이벤트
        resetButton.setOnClickListener {
            setAmount(0L)
        }
        
        // 금액 버튼 클릭 이벤트
        setupAmountButtons()
        
        // 예금 및 출금 버튼 이벤트
        setupActionButtons()
    }
    
    // 이자 정보 텍스트 업데이트
    private fun updateInterestInfoTexts() {
        // 다음 이자 금액 계산 및 표시
        updateTotalInterestText()
        
        // 누적 이자 정보 표시
        val totalAccumulated = assetViewModel.totalDepositInterest.value ?: 0L
        updateAccumulatedInterestText(totalAccumulated)
    }
    
    // 다음 이자 금액 계산 및 텍스트 업데이트
    private fun updateTotalInterestText() {
        val nextInterest = assetViewModel.calculateNextDepositInterest()
        totalInterestText.text = "다음 이자 금액: ${formatCurrency(nextInterest)}"
    }
    
    // 누적 이자 정보 텍스트 업데이트
    private fun updateAccumulatedInterestText(totalInterest: Long) {
        accumulatedInterestText.text = "총 받은 이자: ${formatCurrency(totalInterest)}"
    }
    
    private fun setupAmountButtons() {
        btn1Man.setOnClickListener {
            addAmount(10000) // 1만원
        }

        btn10Man.setOnClickListener {
            addAmount(100000) // 10만원
        }

        btn100Man.setOnClickListener {
            addAmount(1000000) // 100만원
        }

        btn1000Man.setOnClickListener {
            addAmount(10000000) // 1000만원
        }

        btn1Eok.setOnClickListener {
            addAmount(100000000) // 1억원
        }

        btn10Eok.setOnClickListener {
            addAmount(1000000000) // 10억원
        }

        btn100Eok.setOnClickListener {
            addAmount(10000000000) // 100억원
        }

        btn1000Eok.setOnClickListener {
            addAmount(100000000000) // 1000억원
        }
    }
    
    private fun setupActionButtons() {
        // 예금하기 버튼 클릭 이벤트
        depositButton.setOnClickListener {
            val amount = getAmount()
            if (amount > 0) {
                // 현재 자산 확인
                val currentAsset = assetViewModel.asset.value ?: 0L
                
                // 자산보다 많은 금액 예금 시도 시 경고 메시지
                if (amount > currentAsset) {
                    showMessage(
                        "보유 자산(${formatCurrency(currentAsset)})보다 많은 금액은 예금할 수 없습니다."
                    )
                    return@setOnClickListener
                }
                
                // 입금 처리
                if (assetViewModel.addDeposit(amount)) {
                    resetInputAmount()
                } else {
                    // 입금 실패해도 입력값은 초기화
                    resetInputAmount()
                }
            }
        }

        // 출금하기 버튼 클릭 이벤트
        withdrawButton.setOnClickListener {
            val amount = getAmount()
            if (amount > 0) {
                // 출금 처리
                if (assetViewModel.subtractDeposit(amount)) {
                    resetInputAmount()
                } else {
                    // 출금 실패해도 입력값은 초기화
                    resetInputAmount()
                }
            }
        }

        // 전액출금 버튼 클릭 이벤트
        withdrawAllButton.setOnClickListener {
            val currentDeposit = assetViewModel.deposit.value ?: 0L
            if (currentDeposit <= 0) {
                showMessage("출금할 예금이 없습니다")
                return@setOnClickListener
            }
            
            if (assetViewModel.subtractDeposit(currentDeposit)) {
                setAmount(0L)
            }
        }

        // 천단위 출금 버튼 클릭 이벤트
        withdrawThousandButton.setOnClickListener {
            val currentDeposit = assetViewModel.deposit.value ?: 0L
            if (currentDeposit > 0) {
                // 만단위 이하 금액 계산 (예: 12345 -> 2345)
                val tenThousandRemainder = currentDeposit % 10000
                if (tenThousandRemainder > 0) {
                    // 만단위 이하 금액 출금
                    if (assetViewModel.subtractDeposit(tenThousandRemainder)) {
                        updateAmountInput(0L)
                        showMessage(
                            "만단위 이하 ${formatCurrency(tenThousandRemainder)}을 출금했습니다."
                        )
                    }
                } else {
                    showMessage(
                        "출금할 만단위 이하 금액이 없습니다."
                    )
                }
            }
        }
    }

    private fun getAmount(): Long {
        return try {
            // 천 단위 구분자와 접두사, 접미사 제거 후 Long으로 변환
            depositAmountInput.text.toString()
                .replace(",", "")
                .replace("입력: ", "")
                .replace("예금: ", "")
                .replace("원", "")
                .trim()
                .toLongOrNull() ?: 0L
        } catch (e: NumberFormatException) {
            0L
        }
    }

    private fun setAmount(amount: Long) {
        // 천 단위 구분자 추가하고 접두사 및 접미사 추가
        depositAmountInput.text = "예금: ${formatCurrency(amount)}"
    }

    private fun addAmount(amount: Long) {
        val currentAmount = getAmount()
        setAmount(currentAmount + amount)
    }

    private fun updateAmountInput(amount: Long) {
        setAmount(amount)
    }

    /**
     * 금액 입력 필드 초기화
     */
    private fun resetInputAmount() {
        setAmount(0L)
    }
    
    /**
     * 프래그먼트가 화면에 다시 표시될 때 호출됨
     */
    override fun onResume() {
        super.onResume()
        // 화면에 다시 표시될 때마다 입력값 초기화
        resetInputAmount()
    }
} 