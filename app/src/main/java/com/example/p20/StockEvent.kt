package com.example.p20

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