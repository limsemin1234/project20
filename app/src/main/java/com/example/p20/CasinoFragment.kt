package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.fragment.app.Fragment

/**
 * 카지노 기능을 담당하는 메인 프래그먼트
 * 블랙잭과 포커 게임을 탭 레이아웃과 뷰페이저로 전환하여 제공
 */
class CasinoFragment : BaseFragment() {

    // UI 컴포넌트 - 초기화가 필요한 뷰들
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var casinoInfoText: TextView
    
    // SoundManager 인스턴스
    private lateinit var soundManager: SoundManager
    
    // 어댑터 참조 - 프래그먼트가 재생성될 때 메모리 누수 방지를 위해 lazy 초기화
    private val adapter: CasinoViewPagerAdapter by lazy { 
        CasinoViewPagerAdapter(requireActivity())
    }
    
    // 탭 타이틀 - 상수로 분리
    private companion object {
        private val TAB_TITLES = arrayOf("블랙잭", "포커(1인발라트로)")
        private val SOUND_TAB_SELECT = R.raw.casino_tab_select
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

        // SoundManager 초기화
        soundManager = SoundManager.getInstance(requireContext())

        // 뷰 초기화 (지연시키지 않고 즉시 초기화)
        initializeViews(view)
        
        // 탭 레이아웃 및 뷰페이저 설정
        setupTabLayoutWithViewPager()
        
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
        
        // 5초 후에 주의 문구를 애니메이션과 함께 사라지게 함 - 자동 추적 핸들러 사용
        postDelayed(5000) {
            // 페이드아웃 애니메이션 생성
            val fadeOut = android.view.animation.AlphaAnimation(1.0f, 0.0f)
            fadeOut.duration = 1000 // 1초 동안 페이드아웃
            fadeOut.fillAfter = true // 애니메이션 후 상태 유지
            
            // 애니메이션 리스너 설정
            fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // 애니메이션 종료 후 뷰 완전히 숨김
                    casinoInfoText.visibility = View.GONE
                }
                
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
            
            // 애니메이션 시작
            casinoInfoText.startAnimation(fadeOut)
        }
        
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
     * 탭 애니메이션을 생성하는 메서드 예시
     */
    private fun createTabAnimation(view: View) {
        // 애니메이터 생성 및 자동 추적
        val scaleX = android.animation.ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 1.2f, 1.0f)
        val scaleY = android.animation.ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 1.2f, 1.0f)
        
        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
        }
        
        // 자동 추적되는 애니메이터 시작
        trackAnimator(animatorSet).start()
    }
    
    override fun onDestroyView() {
        // 뷰페이저에서 어댑터 분리 - 메모리 누수 방지
        viewPager.adapter = null
        
        // BaseFragment의 onDestroyView가 리소스 정리 담당
        super.onDestroyView()
    }
    
    /**
     * 게임 오버 시 호출되는 메서드
     * 게임 오버 상태에서 필요한 추가 처리
     */
    override fun onGameOver() {
        // 진행 중인 어떤 작업이든 중단
        showMessage("카지노 게임이 중단되었습니다.")
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