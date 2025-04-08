package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class CasinoFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CasinoViewPagerAdapter

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

        // 어댑터 설정
        adapter = CasinoViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // 탭과 뷰페이저 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "블랙잭"
                1 -> "1인발라트로"
                2 -> "발라트로 포커"
                else -> "게임"
            }
        }.attach()
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