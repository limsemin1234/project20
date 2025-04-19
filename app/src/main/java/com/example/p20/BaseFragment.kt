package com.example.p20

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import android.util.Log
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * 모든 Fragment의 기본 클래스
 * 공통 기능을 제공합니다.
 */
abstract class BaseFragment : Fragment() {
    
    // 공통으로 사용하는 ViewModel
    protected lateinit var assetViewModel: AssetViewModel
    protected lateinit var timeViewModel: TimeViewModel
    
    // 프래그먼트 내에서 사용하는 리소스 추적
    private val activeAnimators = mutableListOf<Animator>()
    private val activeMediaPlayers = mutableListOf<MediaPlayer>()
    private val activeHandlers = mutableListOf<Handler>()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 공통 ViewModel 초기화
        assetViewModel = ViewModelProvider(requireActivity())[AssetViewModel::class.java]
        timeViewModel = ViewModelProvider(requireActivity())[TimeViewModel::class.java]
        
        // 게임 오버 상태 관찰
        observeGameOverState()
    }
    
    /**
     * 애니메이터를 추적 목록에 추가합니다.
     * 이 메서드를 통해 애니메이터를 관리하면 프래그먼트가 소멸될 때 자동으로 정리됩니다.
     * 
     * @param animator 추적할 애니메이터
     * @return 추가된 애니메이터
     */
    protected fun <T : Animator> trackAnimator(animator: T): T {
        activeAnimators.add(animator)
        return animator
    }
    
    /**
     * 미디어 플레이어를 추적 목록에 추가합니다.
     * 이 메서드를 통해 미디어 플레이어를 관리하면 프래그먼트가 소멸될 때 자동으로 정리됩니다.
     * 
     * @param mediaPlayer 추적할 미디어 플레이어
     * @return 추가된 미디어 플레이어
     */
    protected fun trackMediaPlayer(mediaPlayer: MediaPlayer): MediaPlayer {
        activeMediaPlayers.add(mediaPlayer)
        return mediaPlayer
    }
    
    /**
     * 핸들러를 추적 목록에 추가합니다.
     * 이 메서드를 통해 핸들러를 관리하면 프래그먼트가 소멸될 때 자동으로 정리됩니다.
     * 
     * @param handler 추적할 핸들러
     * @return 추가된 핸들러
     */
    protected fun trackHandler(handler: Handler): Handler {
        activeHandlers.add(handler)
        return handler
    }
    
    /**
     * 지연 실행 작업을 예약하고 핸들러를 자동으로 추적합니다.
     * 
     * @param delayMillis 지연 시간(밀리초)
     * @param runnable 실행할 작업
     * @return 생성된 핸들러
     */
    protected fun postDelayed(delayMillis: Long, runnable: Runnable): Handler {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(runnable, delayMillis)
        return trackHandler(handler)
    }
    
    /**
     * 일반 메시지용 알림 표시
     */
    protected fun showMessage(message: String) {
        // 상단 메시지로만 표시 (context가 있는 경우)
        context?.let { 
            MessageManager.showMessage(it, message)
        }
    }
    
    /**
     * 성공 메시지용 알림 표시
     */
    protected fun showSuccessMessage(message: String) {
        // 상단 메시지로만 표시 (context가 있는 경우)
        context?.let { 
            MessageManager.showMessage(it, message)
        }
    }
    
    /**
     * 오류 메시지용 알림 표시
     */
    protected fun showErrorMessage(message: String) {
        // 상단 메시지로만 표시 (context가 있는 경우)
        context?.let { 
            MessageManager.showMessage(it, message)
        }
    }
    
    /**
     * 확인 메시지 표시 (액션 포함)
     * 주의: 액션이 필요한 경우에는 스낵바를 계속 사용
     */
    protected fun showConfirmMessage(message: String, actionText: String, action: () -> Unit) {
        val view = view ?: return
        MessageManager.showSnackbar(view, message, Snackbar.LENGTH_LONG, false, actionText, action)
    }
    
    /**
     * 숫자 포맷팅 (통화 형식)
     */
    protected fun formatCurrency(amount: Long): String {
        return FormatUtils.formatCurrency(amount)
    }
    
    /**
     * 숫자 포맷팅 (간결한 형식)
     */
    protected fun formatCompactCurrency(amount: Long): String {
        return FormatUtils.formatCompactCurrency(amount)
    }
    
    /**
     * 변동값 포맷팅 (부호 추가)
     */
    protected fun formatWithSign(value: Int): String {
        return FormatUtils.formatWithSign(value)
    }
    
    /**
     * 변동률 포맷팅 (퍼센트)
     */
    protected fun formatPercent(value: Double): String {
        return FormatUtils.formatPercent(value)
    }
    
    /**
     * 변동값에 따른 색상 코드 반환
     */
    protected fun getChangeColor(change: Int): Int {
        return FormatUtils.getChangeColor(change)
    }
    
    /**
     * 게임 오버 상태를 관찰하고 UI 접근을 제한합니다
     */
    private fun observeGameOverState() {
        timeViewModel.isGameOver.observe(viewLifecycleOwner) { isGameOver ->
            // 게임 오버일 경우 프래그먼트의 모든 상호작용 비활성화
            view?.let { rootView ->
                setViewAndChildrenEnabled(rootView, !isGameOver)
            }
            
            if (isGameOver) {
                // 게임 오버 시 진행 중인 작업 중단
                onGameOver()
            }
        }
    }
    
    /**
     * 게임 오버 상태일 때 호출되는 메서드
     * 자식 클래스에서 필요하면 오버라이드하여 추가 처리
     */
    protected open fun onGameOver() {
        // 기본 구현은 비어있음
        // 자식 클래스에서 필요하면 오버라이드
    }
    
    /**
     * 뷰와 모든 자식 뷰의 활성화 상태를 설정합니다
     */
    private fun setViewAndChildrenEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1.0f else 0.5f
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                setViewAndChildrenEnabled(child, enabled)
            }
        }
    }
    
    /**
     * 뷰가 소멸되기 전에 호출되어 리소스를 정리합니다.
     */
    override fun onDestroyView() {
        // 뷰 관련 리소스 정리
        clearAnimations()
        super.onDestroyView()
    }
    
    /**
     * 프래그먼트가 소멸될 때 호출되어 모든 리소스를 정리합니다.
     */
    override fun onDestroy() {
        // 모든 리소스 정리
        clearAllResources()
        super.onDestroy()
    }
    
    /**
     * 애니메이션 리소스를 정리합니다.
     */
    private fun clearAnimations() {
        try {
            // ConcurrentModificationException 방지를 위한 복사본 사용
            val animatorsCopy = ArrayList(activeAnimators)
            
            for (animator in animatorsCopy) {
                try {
                    animator.cancel()
                } catch (e: Exception) {
                    Log.e("BaseFragment", "애니메이터 취소 오류: ${e.message}")
                }
            }
            
            activeAnimators.clear()
        } catch (e: Exception) {
            Log.e("BaseFragment", "애니메이션 정리 오류: ${e.message}")
        }
    }
    
    /**
     * 미디어 플레이어 리소스를 정리합니다.
     */
    private fun clearMediaPlayers() {
        try {
            // ConcurrentModificationException 방지를 위한 복사본 사용
            val mediaPlayersCopy = ArrayList(activeMediaPlayers)
            
            for (mediaPlayer in mediaPlayersCopy) {
                try {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.stop()
                    }
                    mediaPlayer.release()
                } catch (e: Exception) {
                    Log.e("BaseFragment", "미디어 플레이어 해제 오류: ${e.message}")
                }
            }
            
            activeMediaPlayers.clear()
        } catch (e: Exception) {
            Log.e("BaseFragment", "미디어 플레이어 정리 오류: ${e.message}")
        }
    }
    
    /**
     * 핸들러 콜백을 정리합니다.
     */
    private fun clearHandlers() {
        try {
            // ConcurrentModificationException 방지를 위한 복사본 사용
            val handlersCopy = ArrayList(activeHandlers)
            
            for (handler in handlersCopy) {
                try {
                    handler.removeCallbacksAndMessages(null)
                } catch (e: Exception) {
                    Log.e("BaseFragment", "핸들러 콜백 제거 오류: ${e.message}")
                }
            }
            
            activeHandlers.clear()
        } catch (e: Exception) {
            Log.e("BaseFragment", "핸들러 정리 오류: ${e.message}")
        }
    }
    
    /**
     * 모든 리소스를 정리합니다.
     */
    private fun clearAllResources() {
        clearAnimations()
        clearMediaPlayers()
        clearHandlers()
    }
} 