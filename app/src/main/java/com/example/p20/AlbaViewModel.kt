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

    private val _albaLevel = MutableLiveData(1)
    val albaLevel: LiveData<Int> get() = _albaLevel

    private val _isCooldown = MutableLiveData(false)
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData(0)
    val cooldownTime: LiveData<Int> get() = _cooldownTime

    private var rewardTextCount = 0 // 애니메이션 카운트

    init {
        loadAlbaData()
        if (_isCooldown.value == true) {
            startCooldown()
        }
    }

    fun increaseTouchCount() {
        if (_isCooldown.value == false && _touchCount.value ?: 0 < 10) {
            _touchCount.value = (_touchCount.value ?: 0) + 1
            rewardTextCount++
        }

        if (_touchCount.value == 10) {
            startCooldown()
        }
    }

    fun onRewardAnimationEnd() {
        rewardTextCount--
        if (_touchCount.value == 10 && rewardTextCount == 0) {
            increaseLevel()
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
        _albaLevel.value = 1
        _touchCount.value = 0
        _isCooldown.value = false
        _cooldownTime.value = 0
        rewardTextCount = 0
        saveAlbaData()
    }
}
