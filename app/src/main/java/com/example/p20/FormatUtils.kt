package com.example.p20

import java.text.NumberFormat
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * 앱 전체에서 사용하는 포맷팅 유틸리티 함수들
 */
object FormatUtils {
    
    /**
     * 숫자를 통화 형식으로 포맷팅 (예: 1000 -> 1,000)
     */
    fun formatCurrency(amount: Long): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
        return numberFormat.format(amount)
    }
    
    /**
     * 숫자를 간결한 형식으로 포맷팅 (예: 1000000 -> 100만)
     */
    fun formatCompactCurrency(amount: Long): String {
        return when {
            amount >= 1_0000_0000_0000 -> "${(amount / 1_0000_0000_0000.0).roundToInt()}조"
            amount >= 1_0000_0000 -> "${(amount / 1_0000_0000.0).roundToInt()}억"
            amount >= 1_0000 -> "${(amount / 1_0000.0).roundToInt()}만"
            else -> formatCurrency(amount)
        }
    }
    
    /**
     * 초를 분:초 형식으로 포맷팅 (예: 65초 -> 1:05)
     */
    fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
    
    /**
     * 초를 HH:MM:SS 형식으로 포맷팅
     */
    fun formatTimeHMS(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
    
    /**
     * 남은 시간을 사람이 읽기 쉬운 형식으로 포맷팅
     */
    fun formatRemainingTime(millis: Long): String {
        val seconds = millis / 1000
        
        return when {
            seconds < 60 -> "${seconds}초"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val remainingSecs = seconds % 60
                if (remainingSecs > 0) "${minutes}분 ${remainingSecs}초" else "${minutes}분"
            }
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes > 0) "${hours}시간 ${minutes}분" else "${hours}시간"
            }
        }
    }
    
    /**
     * 퍼센트 값 포맷팅 (예: 0.156 -> +15.6%)
     */
    fun formatPercent(value: Double): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign${Math.round(value * 10) / 10.0}%"
    }
    
    /**
     * 변동값에 따른 색상 코드 반환
     */
    fun getChangeColor(change: Int): Int {
        return when {
            change > 0 -> android.graphics.Color.RED
            change < 0 -> android.graphics.Color.BLUE
            else -> android.graphics.Color.BLACK
        }
    }
    
    /**
     * 변동값에 따른 부호 추가
     */
    fun formatWithSign(value: Int): String {
        return when {
            value > 0 -> "+$value"
            else -> "$value"
        }
    }
    
    /**
     * 변동값에 따른 부호 추가 (Long)
     */
    fun formatWithSign(value: Long): String {
        return when {
            value > 0 -> "+$value"
            else -> "$value"
        }
    }
    
    /**
     * 변동값에 따른 부호 추가 (Double, 소수점 표시)
     */
    fun formatWithSign(value: Double, decimals: Int = 1): String {
        val multiplier = Math.pow(10.0, decimals.toDouble())
        val roundedValue = (value * multiplier).roundToInt() / multiplier
        
        return when {
            roundedValue > 0 -> "+$roundedValue"
            else -> "$roundedValue"
        }
    }
} 