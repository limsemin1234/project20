package com.example.p20

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.AndroidViewModel
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.*

class TimeViewModel(application: Application) : AndroidViewModel(application) {
    private val _time = MutableLiveData<String>()
    val time: LiveData<String> = _time

    private val _isGameOver = MutableLiveData<Boolean>(false)
    val isGameOver: LiveData<Boolean> = _isGameOver

    private val _remainingTime = MutableLiveData<Int>()
    val remainingTime: LiveData<Int> = _remainingTime

    private val _restartRequested = MutableLiveData<Boolean>(false)
    val restartRequested: LiveData<Boolean> = _restartRequested

    private val _showRestartMessageInInfo = MutableLiveData<Boolean>(false)
    val showRestartMessageInInfo: LiveData<Boolean> = _showRestartMessageInInfo

    // --- 추가: 게임 리셋 이벤트 LiveData ---
    private val _gameResetEvent = MutableLiveData<Boolean>()
    val gameResetEvent: LiveData<Boolean> = _gameResetEvent
    // --- 추가 끝 ---

    private val _asset = MutableLiveData<Long>()
    val asset: LiveData<Long> = _asset

    private val _stockInfo = MutableLiveData<Map<String, Int>>()
    val stockInfo: LiveData<Map<String, Int>> = _stockInfo

    private val _albaInfo = MutableLiveData<Map<String, Int>>()
    val albaInfo: LiveData<Map<String, Int>> = _albaInfo

    private val _realEstateInfo = MutableLiveData<Map<String, Boolean>>()
    val realEstateInfo: LiveData<Map<String, Boolean>> = _realEstateInfo

    private val handler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private val sharedPreferences = application.getSharedPreferences("time_data", Context.MODE_PRIVATE)
    private var startTimeInSeconds = 0L
    private var startTime: Long = 0

    init {
        loadTimeData()
        _asset.value = 1000000L // 초기 자산 100만원
        _stockInfo.value = mapOf(
            "삼성전자" to 0,
            "SK하이닉스" to 0,
            "네이버" to 0,
            "카카오" to 0
        )
        _albaInfo.value = mapOf(
            "편의점 알바" to 0,
            "카페 알바" to 0,
            "음식점 알바" to 0,
            "학원 강사" to 0
        )
        _realEstateInfo.value = mapOf(
            "원룸" to false,
            "아파트" to false,
            "빌딩" to false,
            "주상복합" to false
        )
    }

    private fun loadTimeData() {
        _time.value = sharedPreferences.getString("current_time", "00:00:00") ?: "00:00:00"
        startTimeInSeconds = timeStringToSeconds(_time.value ?: "00:00:00")
        
        val loadedRemainingTime = sharedPreferences.getInt("remaining_time", 120)
        if (loadedRemainingTime <= 1) {
            _remainingTime.value = 120
            _time.value = "00:00:00"
            startTimeInSeconds = 0
            saveTimeData()
        } else {
            _remainingTime.value = loadedRemainingTime
        }
    }

    private fun saveTimeData() {
        val editor = sharedPreferences.edit()
        editor.putString("current_time", _time.value)
        editor.putInt("remaining_time", _remainingTime.value ?: 60)
        editor.apply()
    }

    private fun timeStringToSeconds(timeString: String): Long {
        val parts = timeString.split(":")
        return parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
    }

    private fun secondsToTimeString(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                val currentTimeInSeconds = startTimeInSeconds + 
                    ((System.currentTimeMillis() - startTime) / 1000)
                _time.value = secondsToTimeString(currentTimeInSeconds)

                // 남은 시간 감소
                _remainingTime.value = _remainingTime.value?.minus(1)
                
                // 시간이 0이 되면 게임 오버
                if (_remainingTime.value ?: 0 <= 0) {
                    _isGameOver.value = true
                    stopTimer()
                    return
                }

                handler.postDelayed(this, 1000)
            }
        }
    }

    fun startTimer() {
        if (!isTimerRunning) {
            startTimeInSeconds = timeStringToSeconds(_time.value ?: "00:00:00")
            startTime = System.currentTimeMillis()
            isTimerRunning = true
            handler.post(updateTimeRunnable)
        }
    }

    fun stopTimer() {
        isTimerRunning = false
        handler.removeCallbacks(updateTimeRunnable)
    }

    fun resetTimer() {
        stopTimer()
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        _time.value = "00:00:00"
        startTimeInSeconds = 0
        _remainingTime.value = 120
        _isGameOver.value = false
        _restartRequested.value = false
        _showRestartMessageInInfo.value = false
        saveTimeData()
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        saveTimeData()
    }

    fun updateAsset(newAsset: Long) {
        _asset.value = newAsset
    }

    fun updateStockInfo(stockName: String, quantity: Int) {
        val currentStocks = _stockInfo.value?.toMutableMap() ?: mutableMapOf()
        currentStocks[stockName] = quantity
        _stockInfo.value = currentStocks
    }

    fun updateAlbaInfo(albaName: String, count: Int) {
        val currentAlba = _albaInfo.value?.toMutableMap() ?: mutableMapOf()
        currentAlba[albaName] = count
        _albaInfo.value = currentAlba
    }

    fun updateRealEstateInfo(propertyName: String, owned: Boolean) {
        val currentRealEstate = _realEstateInfo.value?.toMutableMap() ?: mutableMapOf()
        currentRealEstate[propertyName] = owned
        _realEstateInfo.value = currentRealEstate
    }

    // 게임 시작 시 타이머를 명시적으로 시작하는 함수
    fun startGameTimer() {
        // isGameOver 상태 확인 제거 (항상 false로 시작) 및 isTimerRunning 확인만
        if (!isTimerRunning) {
            startTimer()
        }
    }

    fun requestRestart() {
        _restartRequested.value = true
    }

    fun triggerShowRestartMessageInInfo() {
        _showRestartMessageInInfo.value = true
    }

    fun consumedShowRestartMessageInInfo() {
        _showRestartMessageInInfo.value = false
    }

    fun increaseRemainingTime(seconds: Int) {
        _remainingTime.value = (_remainingTime.value ?: 0) + seconds
        saveTimeData()
    }

    // --- 추가: 남은 시간 직접 설정 함수 (테스트용) ---
    fun setRemainingTime(seconds: Int) {
        if (seconds >= 0) { // 음수 값 방지
            _remainingTime.value = seconds
            saveTimeData() // 변경된 시간 저장
        }
    }
    // --- 추가 끝 ---

    // --- 추가: 게임 리셋 이벤트 발생 함수 ---
    fun triggerGameResetEvent() {
        _gameResetEvent.value = true
    }

    // --- 추가: 게임 리셋 이벤트 소비 함수 (선택 사항) ---
    fun consumedGameResetEvent() {
        _gameResetEvent.value = false
    }
    // --- 추가 끝 ---
}

