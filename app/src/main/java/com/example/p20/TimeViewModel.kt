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

    private val _isGameOver = MutableLiveData<Boolean>()
    val isGameOver: LiveData<Boolean> = _isGameOver

    private val _remainingTime = MutableLiveData<Int>()
    val remainingTime: LiveData<Int> = _remainingTime

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
    var useCurrentTime = true
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
        _remainingTime.value = 60 // 1분 = 60초
        _isGameOver.value = false
        startTimer()
    }

    private fun loadTimeData() {
        _time.value = sharedPreferences.getString("current_time", "00:00:00") ?: "00:00:00"
        useCurrentTime = sharedPreferences.getBoolean("use_current_time", true)
        startTimeInSeconds = timeStringToSeconds(_time.value ?: "00:00:00")
    }

    private fun saveTimeData() {
        val editor = sharedPreferences.edit()
        editor.putString("current_time", _time.value)
        editor.putBoolean("use_current_time", useCurrentTime)
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
                if (useCurrentTime) {
                    val currentTime = Calendar.getInstance().time
                    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    _time.value = timeFormat.format(currentTime)
                } else {
                    val currentTimeInSeconds = startTimeInSeconds + 
                        ((System.currentTimeMillis() - startTime) / 1000)
                    _time.value = secondsToTimeString(currentTimeInSeconds)
                }

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
        useCurrentTime = false
        startTimeInSeconds = 0
        _remainingTime.value = 60 // 타이머 리셋시 60초로 초기화
        _isGameOver.value = false
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
}

