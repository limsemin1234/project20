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

    private val _albaLevel = MutableLiveData(1)
    val albaLevel: LiveData<Int> get() = _albaLevel

    private val _isActivePhase = MutableLiveData(false)
    val isActivePhase: LiveData<Boolean> get() = _isActivePhase

    private val _activePhaseTime = MutableLiveData(0)
    val activePhaseTime: LiveData<Int> get() = _activePhaseTime

    private val activePhaseDuration = 5 // 알바 활성 시간 (초)

    private val _isCooldown = MutableLiveData(false)
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData(0)
    val cooldownTime: LiveData<Int> get() = _cooldownTime

    init {
        loadAlbaData()
        if (_isCooldown.value == true && _cooldownTime.value ?: 0 > 0) {
            continueCooldown()
        } else {
            _isCooldown.value = false
            _cooldownTime.value = 0
        }
    }

    fun startActivePhase() {
        if (_isCooldown.value == true || _isActivePhase.value == true) return

        _isActivePhase.value = true
        _activePhaseTime.value = activePhaseDuration
        saveAlbaData()

        val activePhaseRunnable = object : Runnable {
            override fun run() {
                if (_activePhaseTime.value ?: 0 > 0) {
                    _activePhaseTime.value = (_activePhaseTime.value ?: 0) - 1
                    handler.postDelayed(this, 1000)
                } else {
                    _isActivePhase.value = false
                    saveAlbaData()
                    startCooldown()
                }
            }
        }
        handler.post(activePhaseRunnable)
    }

    fun startCooldown() {
        _isCooldown.value = true
        _cooldownTime.value = 20
        saveAlbaData()
        continueCooldown()
    }

    private fun continueCooldown() {
        handler.removeCallbacksAndMessages(null)
        val cooldownRunnable = object : Runnable {
            override fun run() {
                if (_cooldownTime.value ?: 0 > 0) {
                    _cooldownTime.value = (_cooldownTime.value ?: 0) - 1
                    handler.postDelayed(this, 1000)
                } else {
                    _isCooldown.value = false
                    saveAlbaData()
                }
            }
        }
        if (_cooldownTime.value ?: 0 > 0) {
            handler.postDelayed(cooldownRunnable, 1000)
        }
    }

    fun getRewardAmount(): Int {
        return (_albaLevel.value ?: 1) * 100
    }

    private fun saveAlbaData() {
        val editor = sharedPreferences.edit()
        editor.putInt("alba_level", _albaLevel.value ?: 1)
        editor.putBoolean("is_cooldown", _isCooldown.value ?: false)
        editor.putInt("cooldown_time", _cooldownTime.value ?: 0)
        editor.apply()
    }

    private fun loadAlbaData() {
        _albaLevel.value = sharedPreferences.getInt("alba_level", 1)
        _isCooldown.value = sharedPreferences.getBoolean("is_cooldown", false)
        _cooldownTime.value = sharedPreferences.getInt("cooldown_time", 0)
    }

    fun resetAlba() {
        _albaLevel.value = 1
        _isActivePhase.value = false
        _activePhaseTime.value = 0
        _isCooldown.value = false
        _cooldownTime.value = 0
        handler.removeCallbacksAndMessages(null)
        saveAlbaData()
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
