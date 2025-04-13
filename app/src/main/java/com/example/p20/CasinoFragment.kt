package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.lang.ref.WeakReference

/**
 * 카지노 기능을 담당하는 메인 프래그먼트
 * 블랙잭과 포커 게임을 탭 레이아웃과 뷰페이저로 전환하여 제공
 */
class CasinoFragment : Fragment(), LifecycleObserver {

    // UI 컴포넌트 - 초기화가 필요한 뷰들
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var casinoInfoText: TextView
    
    // 어댑터 참조 - 프래그먼트가 재생성될 때 메모리 누수 방지를 위해 lazy 초기화
    private val adapter: CasinoViewPagerAdapter by lazy { 
        CasinoViewPagerAdapter(requireActivity())
    }
    
    // 탭 타이틀 - 상수로 분리
    private companion object {
        private val TAB_TITLES = arrayOf("블랙잭", "포커(1인발라트로)")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 레이아웃 인플레이션
        return inflater.inflate(R.layout.fragment_casino, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 프래그먼트 라이프사이클 관리
        viewLifecycleOwner.lifecycle.addObserver(this)

        // 뷰 초기화 (지연시키지 않고 즉시 초기화)
        initializeViews(view)
        
        // 탭 레이아웃 및 뷰페이저 설정
        setupTabLayoutWithViewPager()
        
        // 경고 메시지 표시
        showInitialWarningMessage(view)
    }
    
    /**
     * 뷰 초기화 메서드 분리 (관심사 분리)
     */
    private fun initializeViews(view: View) {
        // 탭 레이아웃과 뷰페이저 초기화
        tabLayout = view.findViewById(R.id.casinoTabLayout)
        viewPager = view.findViewById(R.id.casinoViewPager)
        casinoInfoText = view.findViewById(R.id.casinoInfoText)

        // 카지노 정보 텍스트 설정 - StringBuilder 사용
        val warningText = StringBuilder().apply {
            append("⚠️ 주의: 카지노 게임은 운에 좌우되며 큰 손실을 가져올 수 있습니다.\n")
            append("자신의 자산 범위 내에서 책임감 있게 게임하세요.")
        }
        casinoInfoText.text = warningText.toString()
        
        // 어댑터 설정
        viewPager.adapter = adapter
        
        // 페이지 프리로딩 설정 - 성능 최적화
        viewPager.offscreenPageLimit = 1
    }
    
    /**
     * 탭 레이아웃과 뷰페이저 연결 메서드
     */
    private fun setupTabLayoutWithViewPager() {
        // 탭과 뷰페이저 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = TAB_TITLES.getOrElse(position) { "게임" }
        }.attach()
    }
    
    /**
     * 초기 경고 메시지 표시 메서드
     */
    private fun showInitialWarningMessage(view: View) {
        Snackbar.make(
            view, 
            "카지노 게임은 높은 위험성을 가지고 있습니다. 신중하게 베팅하세요!", 
            Snackbar.LENGTH_LONG
        ).show()
    }
    
    /**
     * 프래그먼트 라이프사이클 관리 - 리소스 해제
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onViewDestroyed() {
        // 뷰페이저에서 어댑터 분리
        viewPager.adapter = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // 라이프사이클 옵저버 제거
        viewLifecycleOwner.lifecycle.removeObserver(this)
    }
    
    /**
     * 뷰페이저 어댑터 - 프래그먼트 재사용 최적화
     */
    private inner class CasinoViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        
        override fun getItemCount(): Int = TAB_TITLES.size
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BlackjackFragment()
                1 -> PokerFragment()
                else -> BlackjackFragment()
            }
        }
    }
}