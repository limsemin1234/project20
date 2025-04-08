package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.util.LinkedList
import java.util.Queue
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar

/**
 * 화면 상단에 메시지를 표시하는 관리자 클래스
 */
object MessageManager {
    private const val ANIMATION_DURATION = 300L // 애니메이션 지속 시간 (ms)
    private const val MESSAGE_DISPLAY_DURATION = 2000L // 메시지 표시 기본 시간 (ms) - 5초에서 2초로 변경
    private const val MAX_MESSAGES = 5 // 동시에 표시할 최대 메시지 수

    private val messageQueue: Queue<String> = LinkedList()
    private val activeMessages = mutableMapOf<View, Runnable>()
    private val handler = Handler(Looper.getMainLooper())
    
    private var messageContainer: LinearLayout? = null
    private var initialized = false

    // 새로 추가: Snackbar 관련 설정값
    private const val SNACKBAR_DURATION_SHORT = 2000
    private const val SNACKBAR_DURATION_LONG = 3500

    /**
     * MessageManager 초기화
     */
    fun initialize(container: LinearLayout) {
        messageContainer = container
        container.removeAllViews() // 기존 뷰 모두 제거
        initialized = true
    }

    /**
     * 메시지 표시 요청
     */
    fun showMessage(context: Context, message: String) {
        if (!initialized || messageContainer == null) {
            // 초기화되지 않았으면 무시
            return
        }

        // 메시지를 큐에 추가
        messageQueue.add(message)
        
        // 메시지 처리
        processMessageQueue(context)
    }

    /**
     * 메시지 큐 처리
     */
    private fun processMessageQueue(context: Context) {
        // 컨테이너가 없으면 무시
        val container = messageContainer ?: return
        
        // 표시 중인 메시지가 최대 개수보다 적고, 큐에 메시지가 있으면 표시
        while (activeMessages.size < MAX_MESSAGES && messageQueue.isNotEmpty()) {
            val message = messageQueue.poll() ?: continue
            
            // 메시지 뷰 생성
            val messageView = createMessageView(context, message)
            
            // 컨테이너에 추가
            container.addView(messageView)
            
            // 애니메이션으로 표시
            showWithAnimation(messageView)
            
            // 일정 시간 후 제거할 Runnable 생성 및 예약
            val dismissRunnable = Runnable {
                hideWithAnimation(messageView)
            }
            
            // 활성 메시지 목록에 추가
            activeMessages[messageView] = dismissRunnable
            
            // 일정 시간 후 메시지 제거
            handler.postDelayed(dismissRunnable, MESSAGE_DISPLAY_DURATION)
        }
    }

    /**
     * 메시지 뷰 생성
     */
    private fun createMessageView(context: Context, message: String): CardView {
        // CardView 생성
        val cardView = CardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 4, 16, 4) // 좌, 상, 우, 하 마진 설정
            }
            radius = 8f // 모서리 둥글게
            cardElevation = 4f // 그림자 효과
            setCardBackgroundColor(Color.argb(200, 51, 51, 51)) // 배경색에 투명도 추가 (78% 불투명)
            alpha = 0f // 처음에는 투명하게 시작
        }
        
        // 텍스트뷰 생성
        val textView = TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text = message
            setTextColor(Color.WHITE)
            textSize = 16f // 글씨 크기 14f에서 16f로 증가
            gravity = Gravity.CENTER
            setPadding(16, 12, 16, 12) // 좌, 상, 우, 하 패딩 설정
        }
        
        // CardView에 TextView 추가
        cardView.addView(textView)
        
        return cardView
    }

    /**
     * 애니메이션으로 메시지 표시
     */
    private fun showWithAnimation(view: View) {
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        fadeIn.duration = ANIMATION_DURATION
        fadeIn.start()
    }

    /**
     * 애니메이션으로 메시지 숨기기
     */
    private fun hideWithAnimation(view: View) {
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        fadeOut.duration = ANIMATION_DURATION
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 애니메이션 종료 후 뷰 제거 및 관련 데이터 정리
                val container = messageContainer
                if (container != null) {
                    container.removeView(view)
                    
                    // 활성 메시지 목록에서 제거
                    val runnable = activeMessages.remove(view)
                    runnable?.let { handler.removeCallbacks(it) }
                    
                    // 큐에 메시지가 있으면 다음 메시지 처리
                    if (messageQueue.isNotEmpty() && container.context != null) {
                        processMessageQueue(container.context)
                    }
                }
            }
        })
        fadeOut.start()
    }

    /**
     * 모든 메시지 제거
     */
    fun clearAllMessages() {
        val container = messageContainer ?: return
        
        // 활성 메시지에 대한 타이머 취소
        for (runnable in activeMessages.values) {
            handler.removeCallbacks(runnable)
        }
        
        // 모든 메시지 뷰 제거
        container.removeAllViews()
        
        // 데이터 초기화
        activeMessages.clear()
        messageQueue.clear()
    }

    /**
     * 표준화된 Snackbar 표시 메서드
     * @param view 스낵바를 표시할 뷰
     * @param message 표시할 메시지
     * @param duration 표시 시간 (Snackbar.LENGTH_SHORT 또는 Snackbar.LENGTH_LONG)
     * @param isError 오류 메시지 여부 (색상 결정)
     * @param action 액션 텍스트 (null이면 액션 없음)
     * @param actionCallback 액션 콜백 (null이면 콜백 없음)
     */
    fun showSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        isError: Boolean = false,
        action: String? = null,
        actionCallback: (() -> Unit)? = null
    ) {
        if (!isViewValid(view)) return
        
        try {
            val snackbar = Snackbar.make(view, message, duration)
            val snackbarView = snackbar.view
            
            // 배경색 설정
            snackbarView.setBackgroundColor(Color.argb(200, 33, 33, 33))
            
            // 텍스트 스타일 설정
            val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView?.let {
                it.maxLines = 3
                if (isError) {
                    it.setTextColor(Color.parseColor("#FF5252"))
                } else {
                    it.setTextColor(Color.WHITE)
                }
            }
            
            // 액션 설정
            if (action != null && actionCallback != null) {
                snackbar.setAction(action) { actionCallback() }
                snackbar.setActionTextColor(Color.parseColor("#8BC34A"))
            }
            
            // 중앙 정렬 시도
            if (snackbarView.layoutParams is FrameLayout.LayoutParams) {
                val params = snackbarView.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                snackbarView.layoutParams = params
            }
            
            snackbar.show()
        } catch (e: Exception) {
            // 로그 출력
            android.util.Log.e("MessageManager", "Snackbar 표시 오류: ${e.message}")
        }
    }
    
    /**
     * 성공 Snackbar 표시
     */
    fun showSuccessSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        if (!isViewValid(view)) return
        
        try {
            val snackbar = Snackbar.make(view, message, duration)
            val snackbarView = snackbar.view
            
            // 성공 배경색
            snackbarView.setBackgroundColor(Color.parseColor("#388E3C"))
            
            // 텍스트 스타일
            val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView?.setTextColor(Color.WHITE)
            
            // 중앙 정렬
            if (snackbarView.layoutParams is FrameLayout.LayoutParams) {
                val params = snackbarView.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                snackbarView.layoutParams = params
            }
            
            snackbar.show()
        } catch (e: Exception) {
            android.util.Log.e("MessageManager", "Success Snackbar 표시 오류: ${e.message}")
        }
    }
    
    /**
     * 오류 Snackbar 표시
     */
    fun showErrorSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        showSnackbar(view, message, duration, true)
    }
    
    /**
     * 뷰가 유효한지 확인
     */
    private fun isViewValid(view: View): Boolean {
        return try {
            // Activity가 파괴되었는지 확인
            val context = view.context
            context != null && !view.isAttachedToWindow.not()
        } catch (e: Exception) {
            android.util.Log.e("MessageManager", "View 유효성 확인 오류: ${e.message}")
            false
        }
    }
} 