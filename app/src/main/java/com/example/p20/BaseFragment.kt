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
import android.view.animation.Animation
import com.example.p20.helpers.AnimationHelper

/**
 * 모든 Fragment의 기본 클래스
 * 공통 기능을 제공합니다.
 */
abstract class BaseFragment : Fragment() {
    
    // 공통으로 사용하는 ViewModel
    protected val assetViewModel: AssetViewModel by lazy {
        ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
    }
    protected val timeViewModel: TimeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)
    }
    
    // 프래그먼트 내에서 사용하는 리소스 추적
    private val activeAnimators = mutableListOf<Animator>()
    private val activeMediaPlayers = mutableListOf<MediaPlayer>()
    private val activeHandlers = mutableListOf<Handler>()
    
    // 사운드 컨트롤러 (전역에서 액세스)
    protected val soundController: SoundController by lazy {
        P20Application.getSoundController()
    }
    
    // SoundManager 인스턴스
    protected val soundManager: SoundManager by lazy {
        SoundManager.getInstance(requireContext())
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 게임 오버 상태 관찰
        observeGameOverState()
    }
    
    /**
     * 프래그먼트가 화면에 보이게 될 때 호출됩니다.
     * 배경음악과 효과음을 복원합니다.
     */
    override fun onResume() {
        super.onResume()
        
        try {
            // 앱 설정에서 사운드 활성화 상태 확인
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                val soundEnabled = prefs.getBoolean("sound_enabled", true)
                val isMuted = prefs.getBoolean("mute_enabled", false)
                
                if (!isMuted && soundEnabled) {
                    // 배경음악 재개
                    soundController.startBackgroundMusic()
                    
                    // 효과음 재로드 (필요한 경우)
                    onReloadSounds()
                    
                    Log.d("BaseFragment", "onResume: 배경음악 및 효과음 복원됨")
                }
            }
        } catch (e: Exception) {
            Log.e("BaseFragment", "onResume 오류: ${e.message}")
        }
    }
    
    /**
     * 프래그먼트가 화면에서 사라질 때 호출됩니다.
     */
    override fun onPause() {
        super.onPause()
        
        try {
            // 배경음악은 MainActivity에서 관리하므로 여기서는 특별한 처리를 하지 않음
            // 필요한 경우 자식 클래스에서 오버라이드하여 추가 정리 작업 수행
            Log.d("BaseFragment", "onPause 호출됨")
        } catch (e: Exception) {
            Log.e("BaseFragment", "onPause 오류: ${e.message}")
        }
    }
    
    /**
     * 효과음을 다시 로드합니다.
     * 자식 클래스에서 필요한 경우 오버라이드하여 효과음 로드 로직 구현
     */
    protected open fun onReloadSounds() {
        // 기본 구현은 비어 있음
        // 자식 클래스에서 필요한 효과음을 다시 로드하기 위해 오버라이드
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
     * Android View Animation을 추적 목록에 추가합니다.
     * 이 메서드를 통해 애니메이션을 관리하면 프래그먼트가 소멸될 때 자동으로 정리됩니다.
     * 
     * @param view 애니메이션이 적용된 뷰
     * @param animation 추적할 애니메이션
     * @return 추가된 애니메이션
     */
    protected fun <T : Animation> trackAnimation(view: View, animation: T): T {
        // 애니메이션 종료 시 정리할 수 있도록 리스너 추가
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                // 필요하다면 종료 후 추가 작업 수행
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })
        
        // 애니메이터도 추가하여 onDestroyView에서 정리할 수 있도록 처리
        val emptyAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 1f)
        emptyAnimator.duration = 1
        emptyAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                try {
                    view.clearAnimation()
                } catch (e: Exception) {
                    Log.e("BaseFragment", "애니메이션 정리 오류: ${e.message}")
                }
            }
            override fun onAnimationCancel(animation: Animator) {
                try {
                    view.clearAnimation()
                } catch (e: Exception) {
                    Log.e("BaseFragment", "애니메이션 정리 오류: ${e.message}")
                }
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })
        activeAnimators.add(emptyAnimator)
        
        return animation
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
    
    /**
     * 애니메이션 헬퍼를 사용하여 뷰에 애니메이션을 적용합니다.
     * 애니메이션은 자동으로 추적되어 프래그먼트가 소멸될 때 정리됩니다.
     *
     * @param view 애니메이션을 적용할 뷰
     * @param animationType 애니메이션 유형 (fade_in, fade_out, scale, heartbeat 등)
     * @param duration 애니메이션 시간(밀리초), 기본값은 300ms
     * @param params 애니메이션 유형에 따른 추가 파라미터
     * @return 생성된 애니메이션 객체
     */
    protected fun applyAnimation(
        view: View,
        animationType: String,
        duration: Long = 300,
        vararg params: Float
    ): Any {
        // AnimationHelper를 사용하여 애니메이션 생성
        val animator = when (animationType.lowercase()) {
            "fade_in" -> AnimationHelper.createFadeInAnimation(view, duration)
            "fade_out" -> AnimationHelper.createFadeOutAnimation(view, duration)
            "scale" -> {
                val from = if (params.isNotEmpty()) params[0] else 0f
                val to = if (params.size > 1) params[1] else 1f
                AnimationHelper.createScaleAnimation(view, from, to, duration)
            }
            "heartbeat" -> {
                val intensity = if (params.isNotEmpty()) params[0] else 0.1f
                AnimationHelper.createHeartbeatAnimation(view, intensity, duration)
            }
            "shake" -> {
                val intensity = if (params.isNotEmpty()) params[0] else 10f
                AnimationHelper.createShakeAnimation(intensity, duration, 3)
            }
            "blink" -> {
                AnimationHelper.createBlinkAnimation(duration, 3)
            }
            else -> AnimationHelper.createFadeInAnimation(view, duration)
        }
        
        // 애니메이터 객체를 적절히 추적
        when (animator) {
            is android.animation.Animator -> trackAnimator(animator)
            is Animation -> {
                view.startAnimation(animator)
                trackAnimation(view, animator)
            }
        }
        
        return animator
    }
    
    /**
     * 뷰 가시성을 애니메이션과 함께 변경합니다.
     * 
     * @param view 가시성을 변경할 뷰
     * @param visible 가시성 여부 (true: 보임, false: 숨김)
     * @param duration 애니메이션 시간(밀리초)
     */
    protected fun animateVisibility(view: View, visible: Boolean, duration: Long = 300) {
        AnimationHelper.animateVisibility(view, visible, duration)
    }
} 