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

class DepositFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var depositAmountInput: TextView
    private lateinit var depositButton: Button
    private lateinit var withdrawButton: Button
    private lateinit var withdrawAllButton: Button
    private lateinit var withdrawThousandsButton: Button
    private lateinit var resetButton: Button
    
    // 새로운 금액 버튼
    private lateinit var btn1Man: Button
    private lateinit var btn10Man: Button
    private lateinit var btn100Man: Button
    private lateinit var btn1000Man: Button
    private lateinit var btn1Eok: Button
    private lateinit var btn10Eok: Button
    private lateinit var btn100Eok: Button
    private lateinit var btn1000Eok: Button
    
    // 기존 버튼 (숨김 상태)
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
        return inflater.inflate(R.layout.fragment_deposit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

        depositAmountInput = view.findViewById(R.id.depositAmountInput)
        depositButton = view.findViewById(R.id.depositButton)
        withdrawButton = view.findViewById(R.id.withdrawButton)
        withdrawAllButton = view.findViewById(R.id.withdrawAllButton)
        withdrawThousandsButton = view.findViewById(R.id.withdrawThousandsButton)
        resetButton = view.findViewById(R.id.resetButton)
        btn1Man = view.findViewById(R.id.btn1Man)
        btn10Man = view.findViewById(R.id.btn10Man)
        btn100Man = view.findViewById(R.id.btn100Man)
        btn1000Man = view.findViewById(R.id.btn1000Man)
        btn1Eok = view.findViewById(R.id.btn1Eok)
        btn10Eok = view.findViewById(R.id.btn10Eok)
        btn100Eok = view.findViewById(R.id.btn100Eok)
        btn1000Eok = view.findViewById(R.id.btn1000Eok)
        unitManButton = view.findViewById(R.id.unitManButton)
        unitEokButton = view.findViewById(R.id.unitEokButton)
        unitJoButton = view.findViewById(R.id.unitJoButton)
        number1Button = view.findViewById(R.id.number1Button)
        number10Button = view.findViewById(R.id.number10Button)
        number100Button = view.findViewById(R.id.number100Button)
        number1000Button = view.findViewById(R.id.number1000Button)

        // 초기 버튼 텍스트 설정
        updateNumberButtons()

        // 금액 초기화 버튼 클릭 이벤트
        resetButton.setOnClickListener {
            setAmount(0)
        }

        // 금액 버튼 클릭 이벤트
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

        depositButton.setOnClickListener {
            val amount = getSelectedAmount()
            if (amount <= 0) {
                showSnackbar("올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            val currentAsset = viewModel.asset.value ?: 0L
            if (amount > currentAsset) {
                showSnackbar("보유 자산이 부족합니다")
                return@setOnClickListener
            }

            viewModel.setAsset(currentAsset - amount)
            viewModel.addDeposit(amount)
            showSnackbar("${formatNumber(amount)}원이 예금되었습니다")
        }

        withdrawButton.setOnClickListener {
            val amount = getSelectedAmount()
            if (amount <= 0) {
                showSnackbar("올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            val currentDeposit = viewModel.deposit.value ?: 0L
            if (amount > currentDeposit) {
                showSnackbar("예금 금액이 부족합니다")
                return@setOnClickListener
            }

            val currentAsset = viewModel.asset.value ?: 0L
            viewModel.setAsset(currentAsset + amount)
            viewModel.subtractDeposit(amount)
            showSnackbar("${formatNumber(amount)}원이 출금되었습니다")
        }

        withdrawAllButton.setOnClickListener {
            val currentDeposit = viewModel.deposit.value ?: 0L
            if (currentDeposit <= 0) {
                showSnackbar("출금할 예금이 없습니다")
                return@setOnClickListener
            }
            
            val currentAsset = viewModel.asset.value ?: 0L
            viewModel.setAsset(currentAsset + currentDeposit)
            viewModel.subtractDeposit(currentDeposit)
            showSnackbar("전체 예금 ${formatNumber(currentDeposit)}원이 출금되었습니다")
            
            // 예금 금액 입력창 초기화
            setAmount(0)
        }

        withdrawThousandsButton.setOnClickListener {
            val currentDeposit = viewModel.deposit.value ?: 0L
            if (currentDeposit <= 0) {
                showSnackbar("출금할 예금이 없습니다")
                return@setOnClickListener
            }
            
            // 천의 자리 이하 금액 계산 (나머지 연산)
            val thousandsBelow = currentDeposit % 10000 // 10000으로 나눈 나머지가 천의 자리 이하 금액
            
            if (thousandsBelow == 0L) {
                showSnackbar("천의 자리 이하 금액이 없습니다")
                return@setOnClickListener
            }
            
            val currentAsset = viewModel.asset.value ?: 0L
            viewModel.setAsset(currentAsset + thousandsBelow)
            viewModel.subtractDeposit(thousandsBelow)
            showSnackbar("천의 자리 이하 금액 ${formatNumber(thousandsBelow)}원이 출금되었습니다")
        }
    }

    private fun setAmount(amount: Long) {
        depositAmountInput.text = formatNumber(amount)
    }
    
    private fun getSelectedAmount(): Long {
        try {
            val amountText = depositAmountInput.text.toString()
            // 쉼표 제거 후 숫자로 변환
            return amountText.replace(",", "").toLong()
        } catch (e: Exception) {
            return 0L
        }
    }

    private fun updateNumberButtons() {
        number1Button.text = "${selectedNumber1}\n(일의 자리)"
        number10Button.text = "${selectedNumber10}0\n(십의 자리)"
        number100Button.text = "${selectedNumber100}00\n(백의 자리)"
        number1000Button.text = "${selectedNumber1000}000\n(천의 자리)"
    }

    private fun showSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }

    private fun addAmount(amount: Long) {
        val currentAmount = getSelectedAmount()
        val newAmount = currentAmount + amount
        setAmount(newAmount)
    }
} 