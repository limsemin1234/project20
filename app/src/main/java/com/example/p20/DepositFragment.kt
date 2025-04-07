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

class DepositFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var depositAmountInput: EditText
    private lateinit var depositButton: Button
    private lateinit var withdrawButton: Button

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

        depositButton.setOnClickListener {
            val amount = depositAmountInput.text.toString().toLongOrNull()
            if (amount == null || amount <= 0) {
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
            depositAmountInput.text.clear()
            showSnackbar("${formatNumber(amount)}원이 예금되었습니다")
        }

        withdrawButton.setOnClickListener {
            val amount = depositAmountInput.text.toString().toLongOrNull()
            if (amount == null || amount <= 0) {
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
            depositAmountInput.text.clear()
            showSnackbar("${formatNumber(amount)}원이 출금되었습니다")
        }
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