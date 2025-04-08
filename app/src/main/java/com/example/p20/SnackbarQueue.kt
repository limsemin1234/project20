package com.example.p20

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import com.google.android.material.snackbar.Snackbar
import java.util.LinkedList
import java.util.Queue

/**
 * 여러 스낵바를 동시에 표시할 수 있는 관리자 클래스
 */
object SnackbarQueue {
    private val messageQueue: Queue<SnackbarInfo> = LinkedList()
    private var currentShownCount = 0
    private const val MAX_SNACKBARS = 5 // 동시에 표시할 최대 스낵바 개수
    private const val MARGIN_BETWEEN_SNACKBARS = 60 // 스낵바 사이 간격 (dp)
    private const val ANIMATION_DELAY: Long = 150L // 스낵바 표시 애니메이션 딜레이 (ms)
    private const val ANIMATION_DURATION: Long = 300L // 애니메이션 지속 시간 (ms)
    private const val DEFAULT_DISPLAY_DURATION: Long = 5000L // 기본 스낵바 표시 시간 (ms)
    private const val PUSHED_SNACKBAR_DURATION: Long = 8000L // 밀려난 스낵바 유지 시간 (ms)
    private val handler = Handler(Looper.getMainLooper())
    
    // 현재 표시 중인 스낵바 뷰들과 자동 제거용 핸들러 목록을 관리
    private val activeSnackbars = mutableListOf<SnackbarItem>()
    
    data class SnackbarItem(
        val snackbar: Snackbar,
        val view: View,
        val dismissRunnable: Runnable
    )

    data class SnackbarInfo(
        val message: String,
        val duration: Int
    )

    /**
     * 스낵바를 큐에 추가하고 표시
     */
    fun show(activity: Activity, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        // 메시지를 큐에 추가
        messageQueue.add(SnackbarInfo(message, duration))
        
        // 현재 표시 중인 스낵바가 최대 개수보다 적으면 새 스낵바 표시
        if (currentShownCount < MAX_SNACKBARS) {
            showNext(activity)
        }
    }

    /**
     * 다음 스낵바를 표시
     */
    private fun showNext(activity: Activity) {
        if (messageQueue.isEmpty()) return
        
        val info = messageQueue.poll() ?: return
        currentShownCount++
        
        // 기존 스낵바들을 아래로 이동
        moveExistingSnackbarsDown(activity)
        
        val rootView = activity.findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, info.message, Snackbar.LENGTH_INDEFINITE) // 무기한 표시로 설정하고 수동으로 처리
        
        // 스낵바 스타일 적용
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.argb(230, 33, 33, 33)) // 배경색 더 선명하게 조정
        
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            
            // Gravity 설정 (화면 중앙에 표시)
            params.gravity = Gravity.CENTER
            
            // 항상 첫 위치에 표시 (topMargin은 0)
            params.topMargin = 0
            
            snackbarView.layoutParams = params
            
            // 스낵바 텍스트 스타일 적용
            val textView = snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text)
            textView?.let {
                (it as? android.widget.TextView)?.setTextColor(Color.WHITE)
                (it as? android.widget.TextView)?.textSize = 14f // 텍스트 크기 조정
            }
            
            // 일정 시간 후 스낵바를 자동으로 닫는 Runnable
            val dismissRunnable = Runnable {
                if (activeSnackbars.isNotEmpty() && activeSnackbars.any { it.snackbar == snackbar }) {
                    snackbar.dismiss()
                }
            }
            
            // 새 스낵바 아이템 생성 및 목록에 추가
            val snackbarItem = SnackbarItem(snackbar, snackbarView, dismissRunnable)
            activeSnackbars.add(0, snackbarItem)
            
            // 최상단 스낵바는 5초 후에 자동으로 사라지도록 설정
            handler.postDelayed(dismissRunnable, DEFAULT_DISPLAY_DURATION)
            
        } catch (e: ClassCastException) {
            // 레이아웃 파라미터를 설정할 수 없는 경우 무시
        }
        
        // 스낵바가 사라질 때 처리
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(snackbar: Snackbar, event: Int) {
                super.onDismissed(snackbar, event)
                
                // 활성 스낵바 목록에서 해당 스낵바 찾기 및 제거
                val item = activeSnackbars.find { it.snackbar == snackbar }
                if (item != null) {
                    // 자동 닫기 핸들러 제거
                    handler.removeCallbacks(item.dismissRunnable)
                    // 목록에서 제거
                    activeSnackbars.remove(item)
                }
                
                currentShownCount--
                
                // ANIMATION_DELAY 후에 다음 스낵바 표시 (애니메이션이 자연스럽게 보이도록)
                handler.postDelayed({
                    if (messageQueue.isNotEmpty()) {
                        showNext(activity)
                    }
                }, ANIMATION_DELAY)
            }
        })
        
        snackbar.show()
    }
    
    /**
     * 기존 스낵바들을 아래로 이동시키는 애니메이션
     */
    private fun moveExistingSnackbarsDown(activity: Activity) {
        // 기존에 표시된 스낵바들에 대해 아래로 이동하는 애니메이션 적용
        for ((index, item) in activeSnackbars.withIndex()) {
            try {
                val view = item.view
                
                // 자동 제거 핸들러 취소 (이미 예약된 경우)
                handler.removeCallbacks(item.dismissRunnable)
                
                // 새로운 위치 계산 (아래로 이동)
                val params = view.layoutParams as FrameLayout.LayoutParams
                val targetMargin = (index + 1) * dpToPx(activity, MARGIN_BETWEEN_SNACKBARS)
                
                // 애니메이션 생성 (현재 위치에서 targetMargin 만큼 아래로)
                val slideDown = TranslateAnimation(
                    0f, 0f, // X축 시작, 종료
                    0f, targetMargin.toFloat() // Y축 시작, 종료
                )
                slideDown.duration = ANIMATION_DURATION // 애니메이션 지속 시간
                slideDown.fillAfter = false
                slideDown.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    
                    override fun onAnimationEnd(animation: Animation?) {
                        // 애니메이션 완료 후 실제 위치 변경
                        params.topMargin = targetMargin
                        view.clearAnimation()
                        view.layoutParams = params
                        
                        // 밀려난 스낵바는 더 오래 유지되도록 설정 (8초)
                        handler.postDelayed(item.dismissRunnable, PUSHED_SNACKBAR_DURATION)
                    }
                    
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                
                view.startAnimation(slideDown)
            } catch (e: Exception) {
                // 애니메이션 적용 실패 시 무시
                e.printStackTrace()
            }
        }
    }
    
    /**
     * dp를 픽셀로 변환
     */
    private fun dpToPx(activity: Activity, dp: Int): Int {
        val scale = activity.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
} 