package com.example.p20.helpers

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.p20.MessageManager
import com.example.p20.SoundManager

/**
 * 버튼 관련 유틸리티 기능을 제공하는 헬퍼 클래스
 * 버튼 초기화, 이벤트 리스너 설정, 상태 관리 등의 중복 코드 제거를 위해 사용
 */
object ButtonHelper {
    
    /**
     * 버튼 목록의 활성화/비활성화 상태를 일괄 설정합니다.
     * 
     * @param buttons 상태를 변경할 버튼 목록
     * @param enabled 활성화 여부
     * @param alpha 비활성화 시 투명도 (기본값: 0.5f)
     */
    fun setButtonsEnabled(buttons: List<Button>, enabled: Boolean, alpha: Float = 0.5f) {
        buttons.forEach { button ->
            button.isEnabled = enabled
            button.alpha = if (enabled) 1.0f else alpha
        }
    }
    
    /**
     * 여러 버튼에 클릭 효과음과 함께
     * 
     * @param buttons 버튼과 클릭 핸들러의 쌍 목록
     * @param soundManager SoundManager 인스턴스
     * @param soundId 재생할 효과음 ID
     */
    fun setupButtons(buttons: Map<Button, () -> Unit>, soundManager: SoundManager? = null, soundId: Int = 0) {
        buttons.forEach { (button, action) ->
            button.setOnClickListener {
                // 효과음 재생 (soundManager가 제공된 경우)
                soundManager?.playSound(soundId)
                // 액션 실행
                action.invoke()
            }
        }
    }
    
    /**
     * 베팅 버튼 설정을 위한 편의 메서드
     * 일반적인 액션에 효과음을 추가합니다.
     * 
     * @param buttons 버튼과 금액의 맵
     * @param onBet 베팅 액션
     * @param soundManager SoundManager 인스턴스
     * @param soundId 재생할 효과음 ID
     */
    fun setupBettingButtons(
        buttons: Map<Button, Long>, 
        onBet: (Long) -> Unit,
        soundManager: SoundManager? = null,
        soundId: Int = 0
    ) {
        buttons.forEach { (button, amount) ->
            button.setOnClickListener {
                // 효과음 재생 (soundManager가 제공된 경우)
                soundManager?.playSound(soundId)
                // 베팅 액션 실행
                onBet(amount)
            }
        }
    }
    
    /**
     * 길게 누르기 리스너를 설정합니다.
     * 
     * @param button 대상 버튼
     * @param action 액션
     * @return 소비 여부
     */
    fun setLongClickListener(button: Button, action: () -> Boolean) {
        button.setOnLongClickListener { action() }
    }
    
    /**
     * 프래그먼트를 표시합니다.
     * 
     * @param fragmentManager 프래그먼트 매니저
     * @param containerId 컨테이너 ID
     * @param fragment 표시할 프래그먼트
     * @param tag 프래그먼트 태그
     */
    fun showFragment(fragmentManager: FragmentManager, containerId: Int, fragment: Fragment, tag: String? = null) {
        fragmentManager.beginTransaction()
            .replace(containerId, fragment, tag)
            .commit()
    }
    
    /**
     * 메시지를 표시합니다.
     * 
     * @param context 컨텍스트
     * @param message 메시지
     */
    fun showMessage(context: Context, message: String) {
        MessageManager.showMessage(context, message)
    }
} 