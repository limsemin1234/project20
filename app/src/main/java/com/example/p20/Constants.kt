package com.example.p20

/**
 * 앱 전체에서 사용되는 상수 값들을 정의하는 객체
 * 하드코딩된 값들을 중앙에서 관리하여 유지보수성을 높입니다.
 */
object Constants {
    // 시간 관련 상수
    const val DEFAULT_TIME_SECONDS = 300L // 기본 게임 시간 (5분)
    const val WARNING_TIME_THRESHOLD = 15L // 시간 경고 표시 기준
    const val CRITICAL_TIME_THRESHOLD = 5L // 심각한 시간 경고 기준
    
    // 금융 관련 상수
    const val DEPOSIT_INTEREST_RATE = 0.01 // 예금 이자율 (1%)
    const val LOAN_INTEREST_RATE = 0.05 // 대출 이자율 (5%)
    const val INTEREST_INTERVAL_SECONDS = 30L // 이자 계산 간격 (30초)
    
    // UI 관련 상수
    const val ANIMATION_DURATION_SHORT = 300L // 짧은 애니메이션 지속 시간
    const val ANIMATION_DURATION_MEDIUM = 500L // 중간 애니메이션 지속 시간
    const val ANIMATION_DURATION_LONG = 1000L // 긴 애니메이션 지속 시간
    
    // 메시지 및 알림 관련 상수
    const val MESSAGE_DISPLAY_DURATION = 3000L // 메시지 표시 지속 시간 (3초)
    
    // SharedPreferences 키 값
    object PrefsKeys {
        const val PREF_NAME = "p20_prefs"
        const val ASSET_KEY = "asset"
        const val DEPOSIT_KEY = "deposit"
        const val LOAN_KEY = "loan"
        const val STOCKS_KEY = "stocks"
        const val REAL_ESTATE_KEY = "real_estate"
        const val GAME_TIME_KEY = "game_time"
        const val SOUND_ENABLED_KEY = "sound_enabled"
        const val MUSIC_ENABLED_KEY = "music_enabled"
        const val VOLUME_LEVEL_KEY = "volume_level"
    }
} 