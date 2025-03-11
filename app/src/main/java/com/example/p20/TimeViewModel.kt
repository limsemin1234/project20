package com.example.p20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import android.os.Handler
import android.os.Looper

class TimeViewModel : ViewModel() {
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

    fun startTimer() {
        handler.post(runnable)
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacks(runnable)
    }
}
