package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AlbaViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("alba_data", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private val _touchCount = MutableLiveData(0)
    val touchCount: LiveData<Int> get() = _touchCount

    private val _albaLevel = MutableLiveData(1) // 알바 레벨
    val albaLevel: LiveData<Int> get() = _albaLevel

    private val _isCooldown = MutableLiveData(false) // 30초 쿨다운 상태 확인
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData(0) // 남은 시간
    val cooldownTime: LiveData<Int> get() = _cooldownTime

    init {
        loadAlbaData()
        if (_isCooldown.value == true) {
            startCooldown() // 앱 실행 시 쿨다운이 있으면 타이머 실행
        }
    }

    fun increaseTouchCount() {
        if (_isCooldown.value == false && _touchCount.value ?: 0 < 10) {
            _touchCount.value = (_touchCount.value ?: 0) + 1
        }

        if (_touchCount.value == 10) {
            startCooldown()
        }
    }

    private fun increaseLevel() {
        val currentLevel = _albaLevel.value ?: 1
        _albaLevel.value = currentLevel + 1
        _touchCount.value = 0
        saveAlbaData()
    }

    fun startCooldown() {
        _isCooldown.value = true
        _cooldownTime.value = 30
        saveAlbaData()

        val cooldownRunnable = object : Runnable {
            override fun run() {
                if (_cooldownTime.value ?: 0 > 0) {
                    _cooldownTime.value = (_cooldownTime.value ?: 0) - 1
                    handler.postDelayed(this, 1000)
                } else {
                    _isCooldown.value = false
                    _touchCount.value = 0
                    increaseLevel()
                    saveAlbaData()
                }
            }
        }
        handler.post(cooldownRunnable)
    }

    fun getRewardAmount(): Int {
        return (_albaLevel.value ?: 1) * 100
    }

    private fun saveAlbaData() {
        val editor = sharedPreferences.edit()
        editor.putInt("alba_level", _albaLevel.value ?: 1)
        editor.putInt("touch_count", _touchCount.value ?: 0)
        editor.putBoolean("is_cooldown", _isCooldown.value ?: false)
        editor.putInt("cooldown_time", _cooldownTime.value ?: 0)
        editor.apply()
    }

    private fun loadAlbaData() {
        _albaLevel.value = sharedPreferences.getInt("alba_level", 1)
        _touchCount.value = sharedPreferences.getInt("touch_count", 0)
        _isCooldown.value = sharedPreferences.getBoolean("is_cooldown", false)
        _cooldownTime.value = sharedPreferences.getInt("cooldown_time", 0)
    }

    fun resetAlba() {
        _albaLevel.value = 1 // 레벨 초기화
        _touchCount.value = 0 // 터치 횟수 초기화
        _isCooldown.value = false // 쿨다운 상태 해제
        _cooldownTime.value = 0 // 쿨다운 시간 초기화
        saveAlbaData() // 변경 사항 저장
    }

}
