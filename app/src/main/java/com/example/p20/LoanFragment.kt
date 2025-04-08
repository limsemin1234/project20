package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.text.NumberFormat
import java.util.Locale

class LoanFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var loanAmountInput: TextView
    private lateinit var loanButton: Button
    private lateinit var repayButton: Button
    private lateinit var resetButton: Button
    private lateinit var unitManButton: Button
    private lateinit var unitEokButton: Button
    private lateinit var unitJoButton: Button
    private lateinit var number1Button: Button
    private lateinit var number10Button: Button
    private lateinit var number100Button: Button
    private lateinit var number1000Button: Button

    private var selectedUnit: Long = 10000 // 기본 단위는 만원
    private var selectedNumber1: Int = 0 // 1의 자리
    private var selectedNumber10: Int = 0 // 10의 자리
    private var selectedNumber100: Int = 0 // 100의 자리
    private var selectedNumber1000: Int = 0 // 1000의 자리
    
    // 마지막으로 처리한 알림의 타임스탬프
    private var lastProcessedNotificationTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

        loanAmountInput = view.findViewById(R.id.loanAmountInput)
        loanButton = view.findViewById(R.id.loanButton)
        repayButton = view.findViewById(R.id.repayButton)
        resetButton = view.findViewById(R.id.resetButton)
        unitManButton = view.findViewById(R.id.unitManButton)
        unitEokButton = view.findViewById(R.id.unitEokButton)
        unitJoButton = view.findViewById(R.id.unitJoButton)
        number1Button = view.findViewById(R.id.number1Button)
        number10Button = view.findViewById(R.id.number10Button)
        number100Button = view.findViewById(R.id.number100Button)
        number1000Button = view.findViewById(R.id.number1000Button)
        
        // 상환하기 버튼 텍스트 변경
        repayButton.text = "전체상환하기"

        // 초기 버튼 텍스트 설정
        updateNumberButtons()

        // 대출 금액에 따라 UI 업데이트
        updateLoanUI(viewModel.loan.value ?: 0L)
        
        // 대출 금액 변경 감지
        viewModel.loan.observe(viewLifecycleOwner) { loanAmount ->
            updateLoanUI(loanAmount)
        }

        // 금액 초기화 버튼 클릭 이벤트
        resetButton.setOnClickListener {
            selectedNumber1 = 0
            selectedNumber10 = 0
            selectedNumber100 = 0
            selectedNumber1000 = 0
            selectedUnit = 10000L
            updateNumberButtons()
            updateAmountInput()
        }

        // 단위 버튼 클릭 이벤트
        unitManButton.setOnClickListener {
            selectedUnit = 10000L // 만원
            updateAmountInput()
        }

        unitEokButton.setOnClickListener {
            selectedUnit = 100000000L // 억원
            updateAmountInput()
        }

        unitJoButton.setOnClickListener {
            selectedUnit = 1000000000000L // 조원
            updateAmountInput()
        }

        // 숫자 버튼 클릭 이벤트
        number1Button.setOnClickListener {
            selectedNumber1 = (selectedNumber1 % 9) + 1
            updateNumberButtons()
            updateAmountInput()
        }

        number10Button.setOnClickListener {
            selectedNumber10 = (selectedNumber10 % 9) + 1
            updateNumberButtons()
            updateAmountInput()
        }

        number100Button.setOnClickListener {
            selectedNumber100 = (selectedNumber100 % 9) + 1
            updateNumberButtons()
            updateAmountInput()
        }

        number1000Button.setOnClickListener {
            selectedNumber1000 = (selectedNumber1000 % 9) + 1
            updateNumberButtons()
            updateAmountInput()
        }

        loanButton.setOnClickListener {
            val amount = calculateAmount()
            if (amount <= 0) {
                showSnackbar("올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            val currentLoan = viewModel.loan.value ?: 0L
            // 이미 대출이 있는 경우 추가 대출 불가 (이미 버튼이 비활성화되어 있어야 함)
            if (currentLoan > 0) {
                showSnackbar("기존 대출금을 전액 상환해야 새로운 대출이 가능합니다")
                return@setOnClickListener
            }
            
            viewModel.addLoan(amount)
            showSnackbar("${formatNumber(amount)}원을 대출했습니다")
        }

        repayButton.setOnClickListener {
            // 전체 대출금 가져오기
            val currentLoan = viewModel.loan.value ?: 0L
            
            if (currentLoan <= 0) {
                showSnackbar("상환할 대출이 없습니다")
                return@setOnClickListener
            }
            
            val currentAsset = viewModel.asset.value ?: 0L
            if (currentLoan > currentAsset) {
                showSnackbar("보유 자산이 부족합니다 (필요: ${formatNumber(currentLoan)}원)")
                return@setOnClickListener
            }

            // 전체 대출금 상환
            viewModel.setAsset(currentAsset - currentLoan)
            viewModel.subtractLoan(currentLoan)
            showSnackbar("${formatNumber(currentLoan)}원을 전액 상환했습니다")
        }
    }

    private fun calculateAmount(): Long {
        val number = selectedNumber1000 * 1000L + selectedNumber100 * 100L + 
                    selectedNumber10 * 10L + selectedNumber1
        return number * selectedUnit
    }

    private fun updateNumberButtons() {
        number1Button.text = "${selectedNumber1}\n(일의 자리)"
        number10Button.text = "${selectedNumber10}0\n(십의 자리)"
        number100Button.text = "${selectedNumber100}00\n(백의 자리)"
        number1000Button.text = "${selectedNumber1000}000\n(천의 자리)"
    }

    private fun updateAmountInput() {
        val amount = calculateAmount()
        loanAmountInput.text = formatNumber(amount)
    }

    private fun showSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }

    private fun updateLoanUI(loanAmount: Long) {
        // 대출금이 있으면 대출하기 버튼 비활성화, 전체상환하기 버튼 활성화
        val hasLoan = loanAmount > 0
        loanButton.isEnabled = !hasLoan
        loanButton.alpha = if (hasLoan) 0.5f else 1.0f
        
        // 대출금이 있을 때 대출하기 버튼에 추가 텍스트 표시
        if (hasLoan) {
            loanButton.text = "대출하기 (기존 대출 상환 필요)"
        } else {
            loanButton.text = "대출하기"
        }
    }
} 