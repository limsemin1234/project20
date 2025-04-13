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
import android.widget.Toast
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

        // 기본 UI 요소 초기화
        depositAmountInput = view.findViewById(R.id.depositAmountInput)
        depositButton = view.findViewById(R.id.depositButton)
        withdrawButton = view.findViewById(R.id.withdrawButton)
        withdrawAllButton = view.findViewById(R.id.withdrawAllButton)
        withdrawThousandButton = view.findViewById(R.id.withdrawThousandButton)
        resetButton = view.findViewById(R.id.resetButton)
        
        // 이자 정보 텍스트 참조 가져오기 및 변경
        val interestInfoText = view.findViewWithTag<TextView>("interest_info_text")
        if (interestInfoText != null) {
            interestInfoText.text = "이자율: 3% (30초마다 자산에 추가)"
        }
        
        btn1Man = view.findViewById(R.id.btn1Man)
        btn10Man = view.findViewById(R.id.btn10Man)
        btn100Man = view.findViewById(R.id.btn100Man)
        btn1000Man = view.findViewById(R.id.btn1000Man)
        btn1Eok = view.findViewById(R.id.btn1Eok)
        btn10Eok = view.findViewById(R.id.btn10Eok)
        btn100Eok = view.findViewById(R.id.btn100Eok)
        btn1000Eok = view.findViewById(R.id.btn1000Eok)

        // 예금 금액 변경 감지
        viewModel.deposit.observe(viewLifecycleOwner) { amount ->
            depositAmountInput.text = "예금: ${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"
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
                val currentAsset = viewModel.asset.value ?: 0L
                
                // 자산보다 많은 금액 예금 시도 시 경고 메시지
                if (amount > currentAsset) {
                    MessageManager.showMessage(
                        requireContext(),
                        "보유 자산(${NumberFormat.getNumberInstance(Locale.KOREA).format(currentAsset)}원)보다 많은 금액은 예금할 수 없습니다."
                    )
                    return@setOnClickListener
                }
                
                // 입금 처리
                if (viewModel.addDeposit(amount)) {
                    updateAmountInput(0L)
                }
            }
        }

        // 출금하기 버튼 클릭 이벤트
        withdrawButton.setOnClickListener {
            val amount = depositAmountInput.text.toString().toLongOrNull() ?: 0L
            if (amount > 0) {
                // 출금 처리
                if (viewModel.subtractDeposit(amount)) {
                    updateAmountInput(0L)
                }
            }
        }

        // 전액출금 버튼 클릭 이벤트
        withdrawAllButton.setOnClickListener {
            val currentDeposit = viewModel.deposit.value ?: 0L
            if (currentDeposit <= 0) {
                MessageManager.showMessage(requireContext(), "출금할 예금이 없습니다")
                return@setOnClickListener
            }
            
            if (viewModel.subtractDeposit(currentDeposit)) {
                setAmount(0L)
            }
        }

        // 천단위 출금 버튼 클릭 이벤트
        withdrawThousandButton.setOnClickListener {
            val currentDeposit = viewModel.deposit.value ?: 0L
            if (currentDeposit > 0) {
                // 만단위 이하 금액 계산 (예: 12345 -> 2345)
                val tenThousandRemainder = currentDeposit % 10000
                if (tenThousandRemainder > 0) {
                    // 만단위 이하 금액 출금
                    if (viewModel.subtractDeposit(tenThousandRemainder)) {
                        updateAmountInput(0L)
                        MessageManager.showMessage(
                            requireContext(),
                            "만단위 이하 ${NumberFormat.getNumberInstance(Locale.KOREA).format(tenThousandRemainder)}원을 출금했습니다."
                        )
                    }
                } else {
                    MessageManager.showMessage(
                        requireContext(),
                        "출금할 만단위 이하 금액이 없습니다."
                    )
                }
            }
        }
    }

    private fun getAmount(): Long {
        return try {
            depositAmountInput.text.toString().toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    private fun setAmount(amount: Long) {
        depositAmountInput.text = amount.toString()
    }

    private fun addAmount(amount: Long) {
        val currentAmount = getAmount()
        setAmount(currentAmount + amount)
    }

    private fun updateAmountInput(amount: Long) {
        depositAmountInput.text = amount.toString()
        setAmount(amount)
    }
} 