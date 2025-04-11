package com.example.p20

/**
 * 주식 이벤트 타입을 정의하는 열거형
 */
enum class StockEventType {
    // 개별 종목 이벤트
    POSITIVE_SMALL,      // 소형 호재
    POSITIVE_MEDIUM,     // 중형 호재
    POSITIVE_LARGE,      // 대형 호재
    
    NEGATIVE_SMALL,      // 소형 악재
    NEGATIVE_MEDIUM,     // 중형 악재
    NEGATIVE_LARGE,      // 대형 악재
    
    // 시장 전체 이벤트
    MARKET_BOOM,         // 경기 부양
    MARKET_RECESSION,    // 경기 침체
    MARKET_SURGE,        // 시장 폭등
    MARKET_CRASH,        // 시장 폭락
    
    // 극단 일회성 이벤트
    STOCK_SURGE,         // 대박 종목
    STOCK_CRASH,         // 대폭락 종목
    
    // 변동성 이벤트
    VOLATILITY_UP,       // 변동성 증가
    VOLATILITY_DOWN      // 변동성 감소
}

/**
 * 주식 이벤트 정보를 담는 데이터 클래스
 */
data class StockEvent(
    val type: StockEventType,             // 이벤트 타입
    val minChangeRate: Double,            // 최소 변동률
    val maxChangeRate: Double,            // 최대 변동률
    val duration: Long,                   // 지속 시간 (ms)
    val message: String,                  // 이벤트 메시지
    val affectedStockNames: List<String>, // 영향 받는 종목 이름 목록
    val volatilityMultiplier: Double = 1.0 // 변동성 승수 (기본값: 1.0, 영향 없음)
) 