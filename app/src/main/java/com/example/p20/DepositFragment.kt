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

/**
 * 예금 기능을 담당하는 Fragment
 */
class DepositFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var depositAmountInput: TextView
    private lateinit var depositButton: Button
    private lateinit var withdrawButton: Button
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
    private lateinit var btn1Man: Button
    private lateinit var btn10Man: Button
    private lateinit var btn100Man: Button
    private lateinit var btn1000Man: Button
    private lateinit var btn1Eok: Button
    private lateinit var btn10Eok: Button
    private lateinit var btn100Eok: Button
    private lateinit var btn1000Eok: Button

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
        return inflater.inflate(R.layout.fragment_deposit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

        depositAmountInput = view.findViewById(R.id.depositAmountInput)
        depositButton = view.findViewById(R.id.depositButton)
        withdrawButton = view.findViewById(R.id.withdrawButton)
        resetButton = view.findViewById(R.id.resetButton)
        
        // 숨겨진 컨트롤이므로 조건부로 초기화
        try {
            // 퍼센트 버튼은 현재 레이아웃에 없을 수 있음
            percent25Button = view.findViewById(R.id.percent25Button)
            percent50Button = view.findViewById(R.id.percent50Button)
            percent75Button = view.findViewById(R.id.percent75Button)
            percent100Button = view.findViewById(R.id.percent100Button)
            
            // 퍼센트 버튼 이벤트 추가
            setupPercentButtons()
        } catch (e: Exception) {
            // 버튼이 없으면 무시
        }
        
        try {
            // 단위 버튼도 현재 레이아웃에 없을 수 있음
            unitManButton = view.findViewById(R.id.unitManButton)
            unitEokButton = view.findViewById(R.id.unitEokButton)
            unitJoButton = view.findViewById(R.id.unitJoButton)
            
            // 단위 버튼 이벤트 추가
            setupUnitButtons()
        } catch (e: Exception) {
            // 버튼이 없으면 무시
        }
        
        try {
            // 숫자 버튼도 현재 레이아웃에 없을 수 있음
            number1Button = view.findViewById(R.id.number1Button)
            number10Button = view.findViewById(R.id.number10Button)
            number100Button = view.findViewById(R.id.number100Button)
            number1000Button = view.findViewById(R.id.number1000Button)
            
            // 숫자 버튼 이벤트 추가
            setupNumberButtons()
        } catch (e: Exception) {
            // 버튼이 없으면 무시
        }
        
        btn1Man = view.findViewById(R.id.btn1Man)
        btn10Man = view.findViewById(R.id.btn10Man)
        btn100Man = view.findViewById(R.id.btn100Man)
        btn1000Man = view.findViewById(R.id.btn1000Man)
        btn1Eok = view.findViewById(R.id.btn1Eok)
        btn10Eok = view.findViewById(R.id.btn10Eok)
        btn100Eok = view.findViewById(R.id.btn100Eok)
        btn1000Eok = view.findViewById(R.id.btn1000Eok)

        // 금액 초기화 버튼 클릭 이벤트
        resetButton.setOnClickListener {
            setAmount(0L)
        }
        
        // 금액 버튼 클릭 이벤트
        setupAmountButtons()
        
        // 예금 및 출금 버튼 이벤트
        setupActionButtons()
    }
    
    private fun setupPercentButtons() {
        // 퍼센트 버튼이 모두 초기화되었을 때만 실행
        if (::percent25Button.isInitialized && ::percent50Button.isInitialized && 
            ::percent75Button.isInitialized && ::percent100Button.isInitialized) {
            
            // 자산 퍼센트 버튼 이벤트
            percent25Button.setOnClickListener {
                val currentAsset = viewModel.asset.value ?: 0L
                val amount = (currentAsset * 0.25).toLong()
                // 백의 자리로 올림 처리
                val roundedAmount = roundToHundreds(amount)
                setAmount(roundedAmount)
            }
            
            percent50Button.setOnClickListener {
                val currentAsset = viewModel.asset.value ?: 0L
                val amount = (currentAsset * 0.5).toLong()
                // 백의 자리로 올림 처리
                val roundedAmount = roundToHundreds(amount)
                setAmount(roundedAmount)
            }
            
            percent75Button.setOnClickListener {
                val currentAsset = viewModel.asset.value ?: 0L
                val amount = (currentAsset * 0.75).toLong()
                // 백의 자리로 올림 처리
                val roundedAmount = roundToHundreds(amount)
                setAmount(roundedAmount)
            }
            
            percent100Button.setOnClickListener {
                val currentAsset = viewModel.asset.value ?: 0L
                // 백의 자리로 올림 처리
                val roundedAmount = roundToHundreds(currentAsset)
                setAmount(roundedAmount)
            }
        }
    }
    
    private fun setupUnitButtons() {
        // 단위 버튼이 모두 초기화되었을 때만 실행
        if (::unitManButton.isInitialized && ::unitEokButton.isInitialized && ::unitJoButton.isInitialized) {
            // 단위 버튼 클릭 이벤트
            unitManButton.setOnClickListener {
                selectedUnit = 10000L // 만원
                calculateAmount()
            }

            unitEokButton.setOnClickListener {
                selectedUnit = 100000000L // 억원
                calculateAmount()
            }

            unitJoButton.setOnClickListener {
                selectedUnit = 1000000000000L // 조원
                calculateAmount()
            }
        }
    }
    
    private fun setupNumberButtons() {
        // 숫자 버튼이 모두 초기화되었을 때만 실행
        if (::number1Button.isInitialized && ::number10Button.isInitialized && 
            ::number100Button.isInitialized && ::number1000Button.isInitialized) {
            
            // 초기 버튼 텍스트 설정
            updateNumberButtons()
            
            // 숫자 버튼 클릭 이벤트
            number1Button.setOnClickListener {
                selectedNumber1 = (selectedNumber1 % 9) + 1
                updateNumberButtons()
                calculateAmount()
            }

            number10Button.setOnClickListener {
                selectedNumber10 = (selectedNumber10 % 9) + 1
                updateNumberButtons()
                calculateAmount()
            }

            number100Button.setOnClickListener {
                selectedNumber100 = (selectedNumber100 % 9) + 1
                updateNumberButtons()
                calculateAmount()
            }

            number1000Button.setOnClickListener {
                selectedNumber1000 = (selectedNumber1000 % 9) + 1
                updateNumberButtons()
                calculateAmount()
            }
        }
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
        depositButton.setOnClickListener {
            val amount = getSelectedAmount()
            if (amount <= 0) {
                MessageManager.showMessage(requireContext(), "올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            viewModel.addDeposit(amount)
        }

        withdrawButton.setOnClickListener {
            val amount = getSelectedAmount()
            if (amount <= 0) {
                MessageManager.showMessage(requireContext(), "올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            viewModel.subtractDeposit(amount)
        }
    }
    
    /**
     * 백의 자리로 금액 반올림
     */
    private fun roundToHundreds(amount: Long): Long {
        return (amount / 100) * 100
    }

    /**
     * 선택한 숫자 버튼에 따른 금액 계산
     */
    private fun calculateAmount() {
        val number = selectedNumber1000 * 1000L + selectedNumber100 * 100L + 
                    selectedNumber10 * 10L + selectedNumber1
        val amount = number * selectedUnit
        setAmount(amount)
    }

    /**
     * 입력 필드에 금액 설정
     */
    private fun setAmount(amount: Long) {
        depositAmountInput.text = viewModel.formatNumber(amount)
    }
    
    /**
     * 현재 입력된 금액 반환
     */
    private fun getSelectedAmount(): Long {
        try {
            val amountText = depositAmountInput.text.toString()
            // 쉼표 제거 후 숫자로 변환
            return amountText.replace(",", "").toLong()
        } catch (e: Exception) {
            return 0L
        }
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
     * 금액 추가
     */
    private fun addAmount(amount: Long) {
        val currentAmount = getSelectedAmount()
        val newAmount = currentAmount + amount
        setAmount(newAmount)
    }
} 