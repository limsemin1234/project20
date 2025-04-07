package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale

class DepositFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var depositAmountInput: TextView
    private lateinit var depositButton: Button
    private lateinit var withdrawButton: Button
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

        // 이자 알림 관찰
        viewModel.interestNotification.observe(viewLifecycleOwner) { message ->
            if (message.contains("예금 이자")) {
                showSnackbar(message)
            }
        }

        depositAmountInput = view.findViewById(R.id.depositAmountInput)
        depositButton = view.findViewById(R.id.depositButton)
        withdrawButton = view.findViewById(R.id.withdrawButton)
        resetButton = view.findViewById(R.id.resetButton)
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

        depositButton.setOnClickListener {
            val amount = calculateAmount()
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
            val amount = calculateAmount()
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
        depositAmountInput.text = formatNumber(amount)
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.depositCard)
                .show()
        }
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }
} 