package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 알바 프래그먼트 클래스
 * 클릭알바와 해킹알바 탭을 포함하는 컨테이너 역할
 */
class AlbaFragment : BaseFragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: AlbaViewPagerAdapter
    
    // SoundManager 인스턴스
    private lateinit var soundManager: SoundManager
    
    // 효과음 ID
    companion object {
        private val SOUND_TAB_SELECT = R.raw.tab_select
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alba, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SoundManager 초기화
        soundManager = SoundManager.getInstance(requireContext())

        // 탭 레이아웃과 뷰페이저 초기화
        tabLayout = view.findViewById(R.id.albaTabLayout)
        viewPager = view.findViewById(R.id.albaViewPager)

        // 어댑터 설정
        adapter = AlbaViewPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        // 탭과 뷰페이저 연결 - 탭 레이아웃 표시 활성화
        tabLayout.visibility = View.VISIBLE
        
        // TabLayoutMediator로 탭과 뷰페이저 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "클릭 알바"
                1 -> "해킹 알바"
                else -> ""
            }
        }.attach()
        
        // 탭 선택 리스너 설정
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // 탭 선택 시 효과음 재생
                playTabSelectSound()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // 사용하지 않음
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // 사용하지 않음
            }
        })
    }
    
    /**
     * 탭 선택 효과음을 재생합니다.
     */
    private fun playTabSelectSound() {
        soundManager.playSound(SOUND_TAB_SELECT)
    }

    // 뷰페이저 어댑터 - 클릭 알바와 해킹 알바 탭 제공
    private inner class AlbaViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        
        override fun getItemCount(): Int = 2 // 클릭 알바와 해킹 알바
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ClickAlbaFragment()
                1 -> HackingAlbaFragment()
                else -> ClickAlbaFragment()
            }
        }
    }
}
