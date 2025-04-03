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

    private val handler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    var useCurrentTime = true
    private val sharedPreferences = application.getSharedPreferences("time_data", Context.MODE_PRIVATE)
    private var startTimeInSeconds = 0L
    private var startTime: Long = 0

    init {
        loadTimeData()
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
        saveTimeData()
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        saveTimeData()
    }
}

