package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 은행 기능을 담당하는 Fragment
 * 예금과 대출 탭을 포함
 */
class BankFragment : BaseFragment() {
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

        // 효과음 초기화 - SoundManager를 통해 처리
        val soundManager = SoundManager.getInstance(requireContext())

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
        
        // 탭 선택 리스너 설정
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // SoundManager를 통한 효과음 재생
                soundManager.playSound(SoundManager.SOUND_BUTTON)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // 사용하지 않음
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // 사용하지 않음
            }
        })

        setupObservers()
    }

    private fun setupObservers() {
        // 예금 정보 관찰
        assetViewModel.deposit.observe(viewLifecycleOwner) { deposit ->
            updateDepositInfo(deposit)
        }

        // 대출 정보 관찰
        assetViewModel.loan.observe(viewLifecycleOwner) { loan ->
            updateLoanInfo(loan)
        }

        // 예금 남은 시간 관찰
        assetViewModel.depositRemainingTime.observe(viewLifecycleOwner) { remainingTime ->
            updateDepositRemainingTime(remainingTime)
        }

        // 대출 남은 시간 관찰
        assetViewModel.loanRemainingTime.observe(viewLifecycleOwner) { remainingTime ->
            updateLoanRemainingTime(remainingTime)
        }
        
        // 이자 알림 메시지 관찰 - 처음 진입할 때는 기존 알림 무시
        lastProcessedNotificationTime = assetViewModel.lastNotificationTimestamp.value ?: 0L
        
        // 이자 알림 메시지 관찰 - 이후 알림만 처리
        assetViewModel.interestNotification.observe(viewLifecycleOwner) { message ->
            val currentNotificationTime = assetViewModel.lastNotificationTimestamp.value ?: 0L
            if (message.isNotEmpty() && currentNotificationTime > lastProcessedNotificationTime) {
                // 메시지 표시
                showMessage(message)
                
                // 처리 타임스탬프 업데이트
                lastProcessedNotificationTime = currentNotificationTime
            }
        }
    }

    private fun updateDepositInfo(deposit: Long) {
        depositInfoText.text = "예금: ${formatCurrency(deposit)}원"
    }

    private fun updateLoanInfo(loan: Long) {
        loanInfoText.text = "대출: ${formatCurrency(loan)}원"
    }

    private fun updateDepositRemainingTime(remainingTime: Long) {
        // 예금 이자 타이머 표시 수정
        depositRemainingTimeText.text = "(${remainingTime}초 후 자산에 이자 추가)"
        depositRemainingTimeText.visibility = View.VISIBLE
        
        // 10초 이하일 때 텍스트 색상 변경
        if (remainingTime <= 10) {
            depositRemainingTimeText.setTextColor(Color.RED)
        } else {
            depositRemainingTimeText.setTextColor(Color.parseColor("#4CAF50")) // 녹색
        }
    }

    private fun updateLoanRemainingTime(remainingTime: Long) {
        // 대출 이자 타이머 표시 수정
        loanRemainingTimeText.text = "(${remainingTime}초 후 자산에서 이자 차감)"
        loanRemainingTimeText.visibility = View.VISIBLE
        
        // 10초 이하일 때 텍스트 색상 변경
        if (remainingTime <= 10) {
            loanRemainingTimeText.setTextColor(Color.RED)
        } else {
            loanRemainingTimeText.setTextColor(Color.parseColor("#FF5722")) // 주황색
        }
    }
} 