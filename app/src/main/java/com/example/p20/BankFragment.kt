package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
    private lateinit var depositInfoText: TextView
    private lateinit var loanInfoText: TextView
    private lateinit var depositRemainingTimeText: TextView
    private lateinit var loanRemainingTimeText: TextView
    private lateinit var tabLayout: TabLayout
    
    // 마지막으로 처리한 알림의 타임스탬프
    private var lastProcessedNotificationTime: Long = 0

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

        depositInfoText = view.findViewById(R.id.depositInfoText)
        loanInfoText = view.findViewById(R.id.loanInfoText)
        depositRemainingTimeText = view.findViewById(R.id.depositRemainingTimeText)
        loanRemainingTimeText = view.findViewById(R.id.loanRemainingTimeText)

        // ViewPager2와 TabLayout 설정
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        
        val adapter = BankPagerAdapter(requireActivity())
        viewPager.adapter = adapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "예금"
                1 -> "대출"
                else -> null
            }
        }.attach()

        setupObservers()
    }

    private fun setupObservers() {
        // 예금 정보 관찰
        viewModel.deposit.observe(viewLifecycleOwner) { deposit ->
            updateDepositInfo(deposit)
        }

        // 대출 정보 관찰
        viewModel.loan.observe(viewLifecycleOwner) { loan ->
            updateLoanInfo(loan)
        }

        // 예금 남은 시간 관찰
        viewModel.depositRemainingTime.observe(viewLifecycleOwner) { remainingTime ->
            updateDepositRemainingTime(remainingTime)
        }

        // 대출 남은 시간 관찰
        viewModel.loanRemainingTime.observe(viewLifecycleOwner) { remainingTime ->
            updateLoanRemainingTime(remainingTime)
        }
        
        // 이자 알림 메시지 관찰 - 처음 진입할 때는 기존 알림 무시
        lastProcessedNotificationTime = viewModel.lastNotificationTimestamp.value ?: 0L
        
        // 이자 알림 메시지 관찰 - 이후 알림만 처리
        viewModel.interestNotification.observe(viewLifecycleOwner) { message ->
            val currentNotificationTime = viewModel.lastNotificationTimestamp.value ?: 0L
            if (message.isNotEmpty() && currentNotificationTime > lastProcessedNotificationTime) {
                // 로그로 알림 확인
                android.util.Log.d("BankFragment", "이자 알림: $message, 시간: $currentNotificationTime")
                
                // 예금 이자 또는 대출 이자 메시지 표시 - MessageManager에서 이미 처리됨
                // showSnackbar(message) - 이 코드는 제거하여 중복 메시지 방지
                
                lastProcessedNotificationTime = currentNotificationTime
            }
        }
    }

    private fun updateDepositInfo(deposit: Long) {
        depositInfoText.text = "예금: ${formatNumber(deposit)}원"
    }

    private fun updateLoanInfo(loan: Long) {
        loanInfoText.text = "대출: ${formatNumber(loan)}원"
    }

    private fun updateDepositRemainingTime(remainingTime: Long) {
        if (remainingTime > 0) {
            depositRemainingTimeText.text = "(${remainingTime}초 후 이자)"
            depositRemainingTimeText.visibility = View.VISIBLE
        } else {
            depositRemainingTimeText.visibility = View.GONE
        }
    }

    private fun updateLoanRemainingTime(remainingTime: Long) {
        if (remainingTime > 0) {
            loanRemainingTimeText.text = "(${remainingTime}초 후 이자)"
            loanRemainingTimeText.visibility = View.VISIBLE
        } else {
            loanRemainingTimeText.visibility = View.GONE
        }
    }
    
    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(number)
    }
    
    private fun showSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
    }
} 