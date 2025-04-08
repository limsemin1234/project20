package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 화면 상단에 메시지를 표시하고 관리하는 클래스
 */
object MessageManager {
    private const val MESSAGE_SHOW_DURATION = 4000L // 메시지 표시 시간 (ms)
    private const val ANIMATION_DURATION = 300L // 애니메이션 시간 (ms)
    private const val MAX_MESSAGES = 5 // 최대 표시 메시지 수
    
    private val handler = Handler(Looper.getMainLooper())
    private val messageQueue: Queue<String> = LinkedList()
    private val messageIdCounter = AtomicInteger(0)
    private val activeMessages = mutableMapOf<Int, View>()
    
    private lateinit var messageContainer: LinearLayout
    
    /**
     * 메시지 관리자 초기화
     */
    fun initialize(container: LinearLayout) {
        messageContainer = container
    }
    
    /**
     * 메시지 표시
     */
    fun showMessage(context: Context, message: String) {
        messageQueue.add(message)
        processMessageQueue(context)
    }
    
    /**
     * 메시지 큐 처리
     */
    private fun processMessageQueue(context: Context) {
        if (messageQueue.isEmpty() || activeMessages.size >= MAX_MESSAGES) {
            return
        }
        
        val message = messageQueue.poll() ?: return
        val messageId = messageIdCounter.incrementAndGet()
        
        // 메시지 뷰 생성
        val messageView = createMessageView(context, messageId, message)
        
        // 컨테이너에 추가 (가장 위에 추가)
        messageContainer.addView(messageView, 0)
        activeMessages[messageId] = messageView
        
        // 애니메이션 실행
        animateMessageIn(messageView)
        
        // 일정 시간 후 삭제
        handler.postDelayed({
            removeMessage(messageId)
        }, MESSAGE_SHOW_DURATION)
        
        // 메시지가 더 있으면 즉시 처리 (딜레이 없이)
        if (messageQueue.isNotEmpty() && activeMessages.size < MAX_MESSAGES) {
            handler.post {
                processMessageQueue(context)
            }
        }
    }
    
    /**
     * 메시지 뷰 생성
     */
    private fun createMessageView(context: Context, messageId: Int, message: String): View {
        // 카드뷰 생성
        val cardView = CardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 3, 4, 3)
            }
            radius = 8f
            cardElevation = 4f
            setCardBackgroundColor(Color.parseColor("#AA000000"))  // 투명도가 높은 검정색
            alpha = 0f
            visibility = View.VISIBLE
            id = messageId
        }
        
        // 텍스트뷰 생성
        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = message
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(16, 10, 16, 10)
            gravity = Gravity.CENTER_VERTICAL
        }
        
        cardView.addView(textView)
        return cardView
    }
    
    /**
     * 메시지 표시 애니메이션
     */
    private fun animateMessageIn(view: View) {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.alpha = value
            }
        }
        animator.start()
    }
    
    /**
     * 메시지 삭제 애니메이션
     */
    private fun animateMessageOut(view: View, onComplete: () -> Unit) {
        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = ANIMATION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.alpha = value
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete()
                }
            })
        }
        animator.start()
    }
    
    /**
     * 메시지 제거
     */
    private fun removeMessage(messageId: Int) {
        val view = activeMessages[messageId] ?: return
        
        animateMessageOut(view) {
            // 애니메이션 완료 후 뷰 제거
            messageContainer.removeView(view)
            activeMessages.remove(messageId)
            
            // 큐에 메시지가 더 있으면 처리
            if (messageQueue.isNotEmpty()) {
                handler.post {
                    processMessageQueue(view.context)
                }
            }
        }
    }
    
    /**
     * 모든 메시지 제거
     */
    fun clearAllMessages() {
        // 현재 표시 중인 모든 메시지 제거
        val messagesToRemove = ArrayList(activeMessages.keys)
        for (messageId in messagesToRemove) {
            removeMessage(messageId)
        }
        
        // 대기 중인 메시지 제거
        messageQueue.clear()
    }
} 