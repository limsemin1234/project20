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
import com.example.p20.databinding.FragmentLoanBinding

/**
 * 대출 기능을 담당하는 Fragment
 */
class LoanFragment : BaseFragment() {
    private var _binding: FragmentLoanBinding? = null
    private val binding get() = _binding!!

    private lateinit var loanAmountInput: TextView
    private lateinit var loanButton: Button
    private lateinit var repayButton: Button
    private lateinit var resetButton: Button
    private lateinit var percent25Button: Button
    private lateinit var percent50Button: Button
    private lateinit var percent75Button: Button
    private lateinit var percent100Button: Button
    private lateinit var unitManButton: Button
    private lateinit var unitEokButton: Button
    private lateinit var unitJoButton: Button
    private lateinit var number1Button: Button
    private lateinit var number10Button: Button
    private lateinit var number100Button: Button
    private lateinit var number1000Button: Button
    private lateinit var nextInterestText: TextView
    private lateinit var accumulatedInterestText: TextView

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
    ): View {
        _binding = FragmentLoanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loanAmountInput = view.findViewById(R.id.loanAmountInput)
        loanButton = view.findViewById(R.id.loanButton)
        repayButton = view.findViewById(R.id.repayButton)
        resetButton = view.findViewById(R.id.resetButton)
        
        // 이자 정보 텍스트 참조 가져오기 및 변경
        val interestInfoText = view.findViewWithTag<TextView>("interest_info_text")
        if (interestInfoText != null) {
            interestInfoText.text = "이자율: 5% (30초마다 자산에서 차감)"
        }
        
        // 추가: 이자 정보 TextView 초기화
        nextInterestText = view.findViewById(R.id.nextInterestText)
        accumulatedInterestText = view.findViewById(R.id.accumulatedInterestText)
        updateInterestInfoTexts()
        
        try {
            percent25Button = view.findViewById(R.id.percent25Button)
            percent50Button = view.findViewById(R.id.percent50Button)
            percent75Button = view.findViewById(R.id.percent75Button)
            percent100Button = view.findViewById(R.id.percent100Button)
            
            // 퍼센트 버튼 이벤트 설정
            setupPercentButtons()
        } catch (e: Exception) {
            // 버튼이 없으면 무시
        }
        
        try {
            unitManButton = view.findViewById(R.id.unitManButton)
            unitEokButton = view.findViewById(R.id.unitEokButton)
            unitJoButton = view.findViewById(R.id.unitJoButton)
            
            // 단위 버튼 이벤트 설정
            setupUnitButtons()
        } catch (e: Exception) {
            // 버튼이 없으면 무시
        }
        
        try {
            number1Button = view.findViewById(R.id.number1Button)
            number10Button = view.findViewById(R.id.number10Button)
            number100Button = view.findViewById(R.id.number100Button)
            number1000Button = view.findViewById(R.id.number1000Button)
            
            // 숫자 버튼 이벤트 및 텍스트 설정
            setupNumberButtons()
        } catch (e: Exception) {
            // 버튼이 없으면 무시
        }
        
        // 상환하기 버튼 텍스트 변경
        repayButton.text = "전체상환하기"

        // 대출 금액에 따라 UI 업데이트
        updateLoanUI(assetViewModel.loan.value ?: 0L)
        
        // 대출 금액 변경 감지
        assetViewModel.loan.observe(viewLifecycleOwner) { loanAmount ->
            loanAmountInput.text = "대출: ${formatCurrency(loanAmount)}"
            updateLoanUI(loanAmount)
            updateInterestInfoTexts()
        }
        
        // 초기값 설정 (대출: 0원)
        updateAmountInput(0L)
        
        // 이자 발생 타이머 관찰
        assetViewModel.loanRemainingTime.observe(viewLifecycleOwner) { _ ->
            updateInterestInfoTexts()
        }
        
        // 누적 이자 정보 관찰
        assetViewModel.totalLoanInterest.observe(viewLifecycleOwner) { totalInterest ->
            updateAccumulatedInterestText(totalInterest)
        }

        // 금액 초기화 버튼 클릭 이벤트
        resetButton.setOnClickListener {
            updateAmountInput(0L)
        }
        
        // 대출 및 상환 버튼 이벤트 설정
        setupActionButtons()
    }
    
    // 추가: 이자 정보 텍스트 업데이트
    private fun updateInterestInfoTexts() {
        // 다음 이자 금액 계산 및 표시
        updateNextInterestText()
        
        // 누적 이자 정보 표시
        val totalAccumulated = assetViewModel.totalLoanInterest.value ?: 0L
        updateAccumulatedInterestText(totalAccumulated)
    }
    
    // 추가: 다음 이자 금액 계산 및 텍스트 업데이트
    private fun updateNextInterestText() {
        val nextInterest = assetViewModel.calculateNextLoanInterest()
        nextInterestText.text = "다음 이자 금액: ${formatCurrency(nextInterest)}"
    }
    
    // 추가: 누적 이자 정보 텍스트 업데이트
    private fun updateAccumulatedInterestText(totalInterest: Long) {
        accumulatedInterestText.text = "총 대출 이자: ${formatCurrency(totalInterest)}"
    }
    
    /**
     * 퍼센트 버튼 이벤트 설정
     */
    private fun setupPercentButtons() {
        // 자산 퍼센트 버튼 이벤트
        percent25Button.setOnClickListener {
            val currentAsset = assetViewModel.asset.value ?: 0L
            val amount = (currentAsset * 0.25).toLong()
            // 백의 자리로 올림 처리
            val roundedAmount = roundToHundreds(amount)
            updateAmountInput(roundedAmount)
        }
        
        percent50Button.setOnClickListener {
            val currentAsset = assetViewModel.asset.value ?: 0L
            val amount = (currentAsset * 0.5).toLong()
            // 백의 자리로 올림 처리
            val roundedAmount = roundToHundreds(amount)
            updateAmountInput(roundedAmount)
        }
        
        percent75Button.setOnClickListener {
            val currentAsset = assetViewModel.asset.value ?: 0L
            val amount = (currentAsset * 0.75).toLong()
            // 백의 자리로 올림 처리
            val roundedAmount = roundToHundreds(amount)
            updateAmountInput(roundedAmount)
        }
        
        percent100Button.setOnClickListener {
            val currentAsset = assetViewModel.asset.value ?: 0L
            // 백의 자리로 올림 처리
            val roundedAmount = roundToHundreds(currentAsset)
            updateAmountInput(roundedAmount)
        }
    }
    
    /**
     * 단위 버튼 이벤트 설정
     */
    private fun setupUnitButtons() {
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
    }
    
    /**
     * 숫자 버튼 이벤트 및 텍스트 설정
     */
    private fun setupNumberButtons() {
        // 초기 버튼 텍스트 설정
        updateNumberButtons()
        
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
    }
    
    /**
     * 대출 및 상환 버튼 이벤트 설정
     */
    private fun setupActionButtons() {
        loanButton.setOnClickListener {
            val amount = getSelectedAmount()
            if (amount <= 0) {
                showMessage("올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            val currentLoan = assetViewModel.loan.value ?: 0L
            // 이미 대출이 있는 경우 추가 대출 불가 (이미 버튼이 비활성화되어 있어야 함)
            if (currentLoan > 0) {
                showMessage("기존 대출금을 전액 상환해야 새로운 대출이 가능합니다")
                return@setOnClickListener
            }
            
            assetViewModel.addLoan(amount)
            // 대출 처리 후 입력값 초기화
            updateAmountInput(0L)
            // 숫자 버튼 값도 초기화
            selectedNumber1 = 0
            selectedNumber10 = 0
            selectedNumber100 = 0
            selectedNumber1000 = 0
            updateNumberButtons()
        }

        repayButton.setOnClickListener {
            // 전체 대출금 가져오기
            val currentLoan = assetViewModel.loan.value ?: 0L
            
            if (currentLoan <= 0) {
                showMessage("상환할 대출이 없습니다")
                return@setOnClickListener
            }
            
            // 대출 상환
            if (assetViewModel.subtractLoan(currentLoan)) {
                updateAmountInput(0L)
            }
        }
    }

    /**
     * 백의 자리로 금액 반올림
     */
    private fun roundToHundreds(amount: Long): Long {
        return (amount / 100) * 100
    }

    /**
     * 선택한 숫자에 따른 금액 계산
     */
    private fun calculateAmount(): Long {
        val number = selectedNumber1000 * 1000L + selectedNumber100 * 100L + 
                    selectedNumber10 * 10L + selectedNumber1
        return number * selectedUnit
    }

    /**
     * 숫자 버튼 텍스트 업데이트
     */
    private fun updateNumberButtons() {
        number1Button.text = "${selectedNumber1}\n(일의 자리)"
        number10Button.text = "${selectedNumber10}0\n(십의 자리)"
        number100Button.text = "${selectedNumber100}00\n(백의 자리)"
        number1000Button.text = "${selectedNumber1000}000\n(천의 자리)"
    }

    /**
     * 금액 입력 필드 업데이트
     */
    private fun updateAmountInput() {
        val amount = calculateAmount()
        loanAmountInput.text = "대출: ${formatCurrency(amount)}"
    }

    /**
     * 금액 입력 필드에 특정 금액 설정
     */
    private fun updateAmountInput(amount: Long) {
        loanAmountInput.text = "대출: ${formatCurrency(amount)}"
    }

    /**
     * 입력 필드에서 금액 가져오기
     */
    private fun getSelectedAmount(): Long {
        try {
            val amountText = loanAmountInput.text.toString()
                .replace(",", "") // 쉼표 제거
                .replace("대출: ", "") // "대출: " 문구 제거
                .replace("원", "") // "원" 문구 제거
                .trim() // 공백 제거
            return if (amountText.isEmpty()) 0L else amountText.toLong()
        } catch (e: Exception) {
            return 0L
        }
    }

    /**
     * 대출 상태에 따른 UI 업데이트
     */
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

    override fun onResume() {
        super.onResume()
        // 화면에 다시 표시될 때마다 입력값 초기화
        updateAmountInput(0L)
        // 숫자 버튼 값도 초기화
        selectedNumber1 = 0
        selectedNumber10 = 0
        selectedNumber100 = 0
        selectedNumber1000 = 0
        updateNumberButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 