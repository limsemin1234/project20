package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale

class LoanFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var loanAmountInput: EditText
    private lateinit var loanButton: Button
    private lateinit var repayButton: Button

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

        loanButton.setOnClickListener {
            val amount = loanAmountInput.text.toString().toLongOrNull()
            if (amount == null || amount <= 0) {
                showSnackbar("올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            viewModel.addLoan(amount)
            loanAmountInput.text.clear()
            showSnackbar("${formatNumber(amount)}원이 대출되었습니다")
        }

        repayButton.setOnClickListener {
            val amount = loanAmountInput.text.toString().toLongOrNull()
            if (amount == null || amount <= 0) {
                showSnackbar("올바른 금액을 입력해주세요")
                return@setOnClickListener
            }

            val currentLoan = viewModel.loan.value ?: 0L
            if (amount > currentLoan) {
                showSnackbar("대출 금액이 부족합니다")
                return@setOnClickListener
            }

            val currentAsset = viewModel.asset.value ?: 0L
            if (amount > currentAsset) {
                showSnackbar("보유 자산이 부족합니다")
                return@setOnClickListener
            }

            viewModel.setAsset(currentAsset - amount)
            viewModel.subtractLoan(amount)
            loanAmountInput.text.clear()
            showSnackbar("${formatNumber(amount)}원이 상환되었습니다")
        }
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.loanCard)
                .show()
        }
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }
} 