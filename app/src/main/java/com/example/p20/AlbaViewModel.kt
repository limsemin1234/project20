package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope

class AlbaViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val sharedPreferences = application.getSharedPreferences("alba_data", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    private val _albaLevel = MutableLiveData(1)
    val albaLevel: LiveData<Int> get() = _albaLevel

    private var clickCounter = 0
    private val CLICKS_PER_LEVEL = 20 // 20번 클릭해야 레벨 1 증가

    private val _isActivePhase = MutableLiveData(false)
    val isActivePhase: LiveData<Boolean> get() = _isActivePhase

    private val _activePhaseTime = MutableLiveData(0)
    val activePhaseTime: LiveData<Int> get() = _activePhaseTime

    private val activePhaseDuration = 5 // 알바 활성 시간 (초)

    private val _isCooldown = MutableLiveData(false)
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData(0)
    val cooldownTime: LiveData<Int> get() = _cooldownTime
    
    // 아이템 획득 이벤트
    private val _itemRewardEvent = MutableLiveData<ItemReward?>()
    val itemRewardEvent: LiveData<ItemReward?> get() = _itemRewardEvent

    // 클릭 카운터 리셋 이벤트
    private val _clickCounterResetEvent = MutableLiveData<Boolean>()
    val clickCounterResetEvent: LiveData<Boolean> get() = _clickCounterResetEvent

    init {
        loadAlbaData()
        if (_isCooldown.value == true && _cooldownTime.value ?: 0 > 0) {
            continueCooldown()
        } else {
            _isCooldown.value = false
            _cooldownTime.value = 0
        }
    }
    
    // 아이템 획득 이벤트를 소비합니다
    fun consumeItemRewardEvent() {
        _itemRewardEvent.value = null
    }

    // 클릭 카운터 리셋 이벤트를 소비합니다
    fun consumeClickCounterResetEvent() {
        _clickCounterResetEvent.value = false
    }

    fun startActivePhase() {
        if (_isCooldown.value == true || _isActivePhase.value == true) return

        _isActivePhase.value = true
        _activePhaseTime.value = activePhaseDuration
        saveAlbaData()

        increaseAlbaLevel()

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

    /**
     * 현재 클릭 카운터 값을 반환합니다.
     * @return 현재 클릭 횟수
     */
    fun getClickCounter(): Int {
        return clickCounter
    }

    fun increaseAlbaLevel() {
        clickCounter++
        
        // 레벨업에 필요한 클릭 수에 도달했는지 확인
        if (clickCounter >= CLICKS_PER_LEVEL) {
            val currentLevel = _albaLevel.value ?: 1
            val newLevel = currentLevel + 1
            _albaLevel.value = newLevel
            clickCounter = 0
            
            // 클릭 카운터 리셋 이벤트 발생
            _clickCounterResetEvent.value = true
            
            // 레벨업 시 아이템 재고 증가 처리
            val context = getApplication<Application>().applicationContext
            val itemReward = ItemUtil.processClickAlbaLevelUp(context, newLevel)
            
            // 아이템 재고 증가 이벤트 발생 및 메시지 표시
            if (itemReward != null) {
                _itemRewardEvent.value = itemReward
                
                // MessageManager를 통해 상단 알림창에 메시지 표시
                showItemRewardMessage(itemReward, context)
            }
            
            saveAlbaData()
        }
    }
    
    /**
     * 아이템 보상 메시지를 MessageManager를 통해 표시
     */
    private fun showItemRewardMessage(reward: ItemReward, context: Context) {
        val message = if (reward.isMultiple) {
            "${reward.itemName} 재고 증가!"
        } else {
            "레벨업! ${_albaLevel.value}레벨 달성! ${reward.itemName} 재고 ${reward.quantity}개 증가!"
        }
        
        // MessageManager를 사용하여 상단에 메시지 표시
        MessageManager.showMessage(context, message)
    }

    private fun saveAlbaData() {
        val editor = sharedPreferences.edit()
        editor.putInt("alba_level", _albaLevel.value ?: 1)
        editor.putBoolean("is_cooldown", _isCooldown.value ?: false)
        editor.putInt("cooldown_time", _cooldownTime.value ?: 0)
        editor.putInt("click_counter", clickCounter)
        editor.apply()
    }

    private fun loadAlbaData() {
        _albaLevel.value = sharedPreferences.getInt("alba_level", 1)
        _isCooldown.value = sharedPreferences.getBoolean("is_cooldown", false)
        _cooldownTime.value = sharedPreferences.getInt("cooldown_time", 0)
        clickCounter = sharedPreferences.getInt("click_counter", 0)
    }

    fun resetAlba() {
        _albaLevel.value = 1
        _isActivePhase.value = false
        _activePhaseTime.value = 0
        _isCooldown.value = false
        _cooldownTime.value = 0
        clickCounter = 0
        handler.removeCallbacksAndMessages(null)
        saveAlbaData()
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}
