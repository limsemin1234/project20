package com.example.p20

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper

class TimeViewModel(private val context: Context) : ViewModel() {
    private val _time = MutableLiveData<String>()
    val time: LiveData<String> get() = _time

    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = object : Runnable {
        override fun run() {
            seconds++
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            _time.value = String.format("%02d:%02d:%02d", hours, minutes, secs)

            handler.postDelayed(this, 1000) // 1초마다 실행
        }
    }

    init {
        // SharedPreferences에서 시간 값 복원
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        seconds = sharedPreferences.getInt("time_seconds", 0) // 초기 시간 0초
        updateTimeDisplay()
    }

    fun startTimer() {
        handler.post(runnable)
    }

    fun stopTimer() {
        handler.removeCallbacks(runnable)
        saveTimeToPreferences() // 시간 저장
    }

    private fun saveTimeToPreferences() {
        val sharedPreferences = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("time_seconds", seconds)
        editor.apply()
    }

    private fun updateTimeDisplay() {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        _time.value = String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer() // ViewModel이 파괴될 때 타이머 중지
    }
}

