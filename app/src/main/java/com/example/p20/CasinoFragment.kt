package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class CasinoFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CasinoViewPagerAdapter
    private lateinit var casinoInfoText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_casino, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 탭 레이아웃과 뷰페이저 초기화
        tabLayout = view.findViewById(R.id.casinoTabLayout)
        viewPager = view.findViewById(R.id.casinoViewPager)
        casinoInfoText = view.findViewById(R.id.casinoInfoText)

        // 카지노 정보 텍스트 설정
        casinoInfoText.text = "⚠️ 주의: 카지노 게임은 운에 좌우되며 큰 손실을 가져올 수 있습니다.\n" +
                "자신의 자산 범위 내에서 책임감 있게 게임하세요."

        // 어댑터 설정
        adapter = CasinoViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // 탭과 뷰페이저 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "블랙잭"
                1 -> "포커"
                else -> "게임"
            }
        }.attach()

        // 초기 경고 메시지 표시
        Snackbar.make(view, "카지노 게임은 높은 위험성을 가지고 있습니다. 신중하게 베팅하세요!", Snackbar.LENGTH_LONG).show()
    }

    // 뷰페이저 어댑터
    private inner class CasinoViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BlackjackFragment()
                1 -> PokerFragment()
                else -> BlackjackFragment()
            }
        }
    }
}