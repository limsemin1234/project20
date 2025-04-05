package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class RealEstateViewModel(application: Application) : AndroidViewModel(application) {

    private val _realEstateList = MutableLiveData<MutableList<RealEstate>>()
    val realEstateList: LiveData<MutableList<RealEstate>> get() = _realEstateList

    private val sharedPreferences = application.getSharedPreferences("real_estate_data", Context.MODE_PRIVATE)

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 10000L // 부동산 가격변동 10초

    private val incomeHandler = Handler(Looper.getMainLooper())
    private val incomeInterval = 25000L // 임대수익 25초

    // 전쟁 이벤트 관련 변수
    private val warEventHandler = Handler(Looper.getMainLooper())
    private val warEventInterval = 30000L // 30초마다 전쟁 이벤트 발생 확률 체크
    private val warRecoveryDuration = 60000L // 전쟁 후 1분(60초) 후 가격 회복
    private val warProbability = 10 // 10% 확률로 전쟁 발생
    private val _warEventMessage = MutableLiveData<String>()
    val warEventMessage: LiveData<String> get() = _warEventMessage
    private val affectedEstateIds = mutableSetOf<Int>() // 전쟁 영향 받는 부동산 ID
    private val originalPrices = mutableMapOf<Int, Long>() // 원래 가격 저장

    var incomeCallback: ((Long) -> Unit)? = null

    // 전쟁 이벤트 콜백
    var warEventCallback: ((String) -> Unit)? = null

    init {
        _realEstateList.value = mutableListOf(
            RealEstate(1, "반지하 원룸", 30_000_000L),
            RealEstate(2, "상가 건물", 50_000_000L),
            RealEstate(3, "아파트", 80_000_000L),
            RealEstate(4, "오피스텔", 120_000_000L),
            RealEstate(5, "단독 주택", 200_000_000L),
            RealEstate(6, "빌딩", 400_000_000L),
            RealEstate(7, "고급빌라", 800_000_000L),
            RealEstate(8, "초고층 빌딩", 1_500_000_000L),
            RealEstate(9, "월드타워", 3_000_000_000L),
            RealEstate(10, "킹타워", 6_000_000_000L)
        )
        loadRealEstateData()
        startPriceUpdates()
        startIncomeGeneration()
        startWarEventChecker() // 전쟁 이벤트 체커 시작
    }

    fun buy(realEstate: RealEstate) {
        realEstate.buy()
        saveRealEstateData()
        _realEstateList.value = _realEstateList.value
    }

    fun sell(realEstate: RealEstate) {
        realEstate.sell()
        saveRealEstateData()
        _realEstateList.value = _realEstateList.value
    }

    private fun saveRealEstateData() {
        val editor = sharedPreferences.edit()
        _realEstateList.value?.forEachIndexed { index, estate ->
            editor.putBoolean("owned_$index", estate.owned)
            val purchaseString = estate.purchasePrices.joinToString(",")
            editor.putString("purchasePrices_$index", purchaseString)
            editor.putLong("price_$index", estate.price)
        }
        editor.apply()
    }

    private fun loadRealEstateData() {
        _realEstateList.value?.forEachIndexed { index, estate ->
            estate.owned = sharedPreferences.getBoolean("owned_$index", false)
            estate.purchasePrices.clear()
            val purchaseString = sharedPreferences.getString("purchasePrices_$index", null)
            if (!purchaseString.isNullOrEmpty()) {
                val prices = purchaseString.split(",").mapNotNull { it.toLongOrNull() }
                estate.purchasePrices.addAll(prices)
            }
            estate.price = sharedPreferences.getLong("price_$index", estate.initialPrice)
        }
        _realEstateList.value = _realEstateList.value
    }

    private fun startPriceUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                updatePrices()
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun updatePrices() {
        _realEstateList.value?.forEach { estate ->
            // 전쟁 이벤트의 영향을 받는 부동산은 가격 업데이트 건너뛰기
            if (!affectedEstateIds.contains(estate.id)) {
                estate.updatePrice()
            }
        }
        _realEstateList.value = _realEstateList.value
        saveRealEstateData()
    }

    private fun startIncomeGeneration() {
        incomeHandler.post(object : Runnable {
            override fun run() {
                generateTotalIncome()
                incomeHandler.postDelayed(this, incomeInterval)
            }
        })
    }

    private fun generateTotalIncome() {
        var totalIncome = 0L
        _realEstateList.value?.forEach { estate ->
            if (estate.owned) {
                val income = (estate.price * 0.01).toLong()
                totalIncome += income
            }
        }
        if (totalIncome > 0) {
            incomeCallback?.invoke(totalIncome)
        }
    }

    fun resetRealEstatePrices() {
        _realEstateList.value?.forEach { estate ->
            estate.price = estate.initialPrice
            estate.owned = false
            estate.purchasePrices.clear()
        }
        _realEstateList.value = _realEstateList.value
        saveRealEstateData()
    }

    // 전쟁 이벤트 체커 시작
    private fun startWarEventChecker() {
        warEventHandler.post(object : Runnable {
            override fun run() {
                checkWarEvent()
                warEventHandler.postDelayed(this, warEventInterval)
            }
        })
    }

    // 전쟁 이벤트 발생 여부 체크
    private fun checkWarEvent() {
        // 이미 전쟁 진행 중이면 체크하지 않음
        if (affectedEstateIds.isNotEmpty()) return
        
        // 전쟁 발생 확률 체크 (10%)
        if ((1..100).random() <= warProbability) {
            triggerWarEvent()
        }
    }

    // 전쟁 이벤트 발생
    private fun triggerWarEvent() {
        val estateList = _realEstateList.value ?: return
        
        // 랜덤하게 1~3개의 부동산 선택
        val numAffected = (1..3).random()
        val selectedIndices = estateList.indices.shuffled().take(numAffected)
        
        // 선택된 부동산들의 가격 저장 및 반으로 감소
        selectedIndices.forEach { index ->
            val estate = estateList[index]
            affectedEstateIds.add(estate.id)
            originalPrices[estate.id] = estate.price
            estate.price = estate.price / 2 // 가격 반으로 감소
        }
        
        // 영향받은 부동산 이름 목록
        val affectedNames = selectedIndices.map { estateList[it].name }
        val message = "⚠️ 전쟁 발생! ${affectedNames.joinToString(", ")}의 가격이 반으로 하락했습니다!"
        
        // 메시지 업데이트 및 콜백 호출
        _warEventMessage.value = message
        warEventCallback?.invoke(message)
        
        // 리스트 업데이트 및 저장
        _realEstateList.value = estateList
        saveRealEstateData()
        
        // 1분 후 가격 복구 스케줄링
        warEventHandler.postDelayed({
            recoverFromWarEvent()
        }, warRecoveryDuration)
    }

    // 전쟁 이벤트 복구
    private fun recoverFromWarEvent() {
        val estateList = _realEstateList.value ?: return
        
        // 영향받은 부동산들의 가격 복구
        estateList.forEach { estate ->
            if (affectedEstateIds.contains(estate.id)) {
                estate.price = originalPrices[estate.id] ?: estate.price
            }
        }
        
        // 복구 메시지
        val message = "✓ 전쟁 상태가 종료되고 부동산 가격이 복구되었습니다."
        _warEventMessage.value = message
        warEventCallback?.invoke(message)
        
        // 상태 초기화
        affectedEstateIds.clear()
        originalPrices.clear()
        
        // 리스트 업데이트 및 저장
        _realEstateList.value = estateList
        saveRealEstateData()
    }

    // 특정 부동산이 전쟁 이벤트의 영향을 받고 있는지 확인하는 메서드
    fun isAffectedByWar(estateId: Int): Boolean {
        return affectedEstateIds.contains(estateId)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        incomeHandler.removeCallbacksAndMessages(null)
        warEventHandler.removeCallbacksAndMessages(null) // 전쟁 이벤트 핸들러 정리
    }
}
