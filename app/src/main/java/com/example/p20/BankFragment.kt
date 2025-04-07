package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.text.NumberFormat
import java.util.Locale

class BankFragment : Fragment() {
    private lateinit var viewModel: AssetViewModel
    private lateinit var depositAmountText: TextView
    private lateinit var loanAmountText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bank, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]

        // 뷰 초기화
        depositAmountText = view.findViewById(R.id.depositAmountText)
        loanAmountText = view.findViewById(R.id.loanAmountText)

        // 예금과 대출 금액 변경 감지
        viewModel.deposit.observe(viewLifecycleOwner) { deposit ->
            updateUI()
        }
        viewModel.loan.observe(viewLifecycleOwner) { loan ->
            updateUI()
        }

        // 뷰페이저와 탭 레이아웃 설정
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)

        // 어댑터 설정
        val adapter = BankPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // 탭 레이아웃과 뷰페이저 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "예금"
                1 -> "대출"
                else -> null
            }
        }.attach()
    }

    private fun updateUI() {
        val deposit = viewModel.deposit.value ?: 0L
        val loan = viewModel.loan.value ?: 0L
        depositAmountText.text = "예금: ${formatNumber(deposit)}원"
        loanAmountText.text = "대출: ${formatNumber(loan)}원"
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }
} 