package com.example.p20

/**
 * 주식 이벤트 타입 정의
 */
enum class StockEventType {
    // 개별 종목 이벤트
    POSITIVE_SMALL,   // 소형 호재
    POSITIVE_MEDIUM,  // 중형 호재
    POSITIVE_LARGE,   // 대형 호재
    NEGATIVE_SMALL,   // 소형 악재
    NEGATIVE_MEDIUM,  // 중형 악재
    NEGATIVE_LARGE,   // 대형 악재
    
    // 시장 전체 이벤트
    MARKET_BOOM,      // 경기 부양
    MARKET_RECESSION, // 경기 침체
    MARKET_SURGE,     // 시장 폭등
    MARKET_CRASH,     // 시장 폭락
    
    // 일회성 이벤트
    STOCK_SURGE,      // 대박 종목
    STOCK_CRASH       // 대폭락 종목
} 