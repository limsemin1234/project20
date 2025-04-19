package com.example.p20

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * 주식 관련 ViewModel
 * - Repository와 EventManager를 조정
 * - 주식 거래 기능 제공
 * - UI 상태 관리
 */
class StockViewModel(application: Application) : AndroidViewModel(application) {

    // Repository와 EventManager 인스턴스
    private val repository: StockRepository = StockRepository(application)
    private val eventManager: StockEventManager = StockEventManager(application, repository)

    // LiveData - Repository의 LiveData를 사용
    val stockItems: LiveData<MutableList<Stock>> = repository.stockItems

    // 선택된 주식을 추적하기 위한 LiveData
    private val _selectedStock = MutableLiveData<Stock>()
    val selectedStockLiveData: LiveData<Stock> get() = _selectedStock
    
    // 선택된 수량 및 거래 모드
    var selectedQuantity: Int = 0
    var isBuyMode: Boolean = true  // 기본값은 매수 모드

    init {
        // 이벤트 시스템 초기화
        eventManager.initializeEventSystem()
    }
    
    /**
     * 주식 선택
     * @param position 선택할 주식 인덱스
     */
    fun selectStock(position: Int) {
        val stocks = stockItems.value ?: return
        if (position in stocks.indices) {
            _selectedStock.value = stocks[position]
        }
    }
    
    /**
     * 주식 선택 (Stock 객체로 선택)
     * @param stock 선택할 주식 객체
     */
    fun selectStock(stock: Stock) {
        _selectedStock.value = stock
    }
    
    /**
     * 호재 이벤트 콜백 설정
     */
    fun setPositiveNewsCallback(callback: (List<String>) -> Unit) {
        eventManager.setPositiveNewsCallback(callback)
    }
    
    /**
     * 악제 이벤트 콜백 설정
     */
    fun setNegativeNewsCallback(callback: (List<String>) -> Unit) {
        eventManager.setNegativeNewsCallback(callback)
    }
    
    /**
     * 이벤트 콜백 설정
     */
    fun setEventCallback(callback: (StockEvent) -> Unit) {
        eventManager.setEventCallback(callback)
    }
    
    /**
     * 주식 매수
     * @param stock 매수할 주식
     * @param quantity 매수 수량
     * @return 매수 성공 여부
     */
    fun buyStock(stock: Stock, quantity: Int): Boolean {
        if (quantity <= 0) return false
        
        // 실제로는 매수에 필요한 금액을 AssetViewModel에서 차감하므로,
        // 여기에서는 보유량만 증가시킴
        stock.holding += quantity
        
        // 매수한 주식의 가격을 매입 이력에 추가
        for (i in 0 until quantity) {
            stock.purchasePrices.add(stock.price)
        }
        
        // 데이터 저장
        repository.saveStockData()
        
        return true
    }
    
    /**
     * 주식 매도
     * @param stock 매도할 주식
     * @param quantity 매도 수량
     * @return 매도 성공 여부
     */
    fun sellStock(stock: Stock, quantity: Int): Boolean {
        if (quantity <= 0) return false
        if (stock.holding < quantity) return false
        
        stock.holding -= quantity
        
        // 매입 이력에서 가장 오래된 것부터 제거 (FIFO)
        for (i in 0 until quantity) {
            if (stock.purchasePrices.isNotEmpty()) {
                stock.purchasePrices.removeAt(0)
            }
        }
        
        // 데이터 저장
        repository.saveStockData()
        
        return true
    }
    
    /**
     * 주식 매수 (이전 호환성 유지)
     * @param stock 매수할 주식
     * @param quantity 매수 수량
     * @return 매수한 수량
     */
    fun buyStocks(stock: Stock, quantity: Int): Int {
        return if (buyStock(stock, quantity)) quantity else 0
    }
    
    /**
     * 주식 매도 (이전 호환성 유지)
     * @param stock 매도할 주식
     * @param quantity 매도 수량
     * @return 매도한 수량
     */
    fun sellStocks(stock: Stock, quantity: Int): Int {
        return if (sellStock(stock, quantity)) quantity else 0
    }
    
    /**
     * 주식 전체 매수 (가능한 최대 수량)
     * @param stock 매수할 주식
     * @param availableAsset 사용 가능한 자산
     * @return 매수한 수량
     */
    fun buyAllStock(stock: Stock, availableAsset: Long): Int {
        // 살 수 있는 최대 수량 계산
        val maxQuantity = (availableAsset / stock.price).toInt()
        if (maxQuantity <= 0) return 0
        
        // 매수 진행
        if (buyStock(stock, maxQuantity)) {
            return maxQuantity
        }
        
        return 0
    }
    
    /**
     * 주식 전체 매도
     * @param stock 매도할 주식
     * @return 매도한 수량
     */
    fun sellAllStock(stock: Stock): Int {
        val quantity = stock.holding
        if (quantity <= 0) return 0
        
        // 매도 진행
        if (sellStock(stock, quantity)) {
            return quantity
        }
        
        return 0
    }
    
    /**
     * 평균 매입가 계산
     * @param stock 대상 주식
     * @return 평균 매입가
     */
    fun getAveragePurchasePrice(stock: Stock): Int {
        if (stock.purchasePrices.isEmpty()) return 0
        return stock.purchasePrices.average().toInt()
    }
    
    /**
     * 평가손익 계산
     * @param stock 대상 주식
     * @return 평가손익
     */
    fun calculateProfit(stock: Stock): Long {
        if (stock.holding <= 0) return 0
        
        val avgPurchasePrice = getAveragePurchasePrice(stock)
        return (stock.price - avgPurchasePrice) * stock.holding.toLong()
    }
    
    /**
     * 수익률 계산
     * @param stock 대상 주식
     * @return 수익률 (%)
     */
    fun calculateProfitRate(stock: Stock): Double {
        if (stock.holding <= 0) return 0.0
        
        val avgPurchasePrice = getAveragePurchasePrice(stock)
        if (avgPurchasePrice == 0) return 0.0
        
        return (stock.price - avgPurchasePrice).toDouble() / avgPurchasePrice * 100.0
    }
    
    /**
     * 주식 데이터 저장
     */
    fun saveStockData() {
        repository.saveStockData()
    }
    
    /**
     * 재시작 시 새로운 주식 종목 생성
     */
    fun resetStocksWithNewCompanies() {
        repository.resetStocksWithNewCompanies()
        eventManager.clearAllEvents()
        eventManager.initializeEventSystem()
    }
    
    /**
     * 주식 초기화 (가격은 유지)
     */
    fun resetStocks() {
        repository.resetStocks()
        eventManager.clearAllEvents()
    }

    /**
     * 리소스 해제
     */
    override fun onCleared() {
        super.onCleared()
        eventManager.cleanup()
    }
}
