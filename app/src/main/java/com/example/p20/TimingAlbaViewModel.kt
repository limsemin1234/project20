package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TimingAlbaViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val sharedPreferences = application.getSharedPreferences("timing_alba_data", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    // 바 위치 (0.0 ~ 1.0)
    private val _pointerPosition = MutableLiveData(0.0f)
    val pointerPosition: LiveData<Float> get() = _pointerPosition

    // 타이밍 바 이동 방향 (1: 오른쪽, -1: 왼쪽)
    private var direction = 1
    
    // 타이밍 바 이동 속도
    private var speed = 0.015f // 기본 속도 증가
    
    // 레벨
    private val _albaLevel = MutableLiveData(1)
    val albaLevel: LiveData<Int> get() = _albaLevel
    
    // 성공 여부
    private val _lastSuccess = MutableLiveData<Int>(0) // 0: 기본, 1: 성공, -1: 실패
    val lastSuccess: LiveData<Int> get() = _lastSuccess
    
    // 보상 배율
    private val _rewardMultiplier = MutableLiveData(1.0f)
    val rewardMultiplier: LiveData<Float> get() = _rewardMultiplier
    
    // 쿨다운 관련 
    private val _isCooldown = MutableLiveData(false)
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData(0)
    val cooldownTime: LiveData<Int> get() = _cooldownTime
    
    // 게임 활성화 상태
    private val _isGameActive = MutableLiveData(false)
    val isGameActive: LiveData<Boolean> get() = _isGameActive
    
    // 아이템 획득 이벤트
    private val _itemRewardEvent = MutableLiveData<ItemReward?>()
    val itemRewardEvent: LiveData<ItemReward?> get() = _itemRewardEvent
    
    // 레벨업까지 남은 성공 횟수 추적
    private val _successfulAttempts = MutableLiveData<Int>(0)
    val successfulAttempts: LiveData<Int> get() = _successfulAttempts
    
    // 성공 시 중앙에서의 거리에 따른 보상 배율
    private val SUCCESS_ZONE_SIZE = 0.1f // 중앙으로부터 5% 거리 내에서 성공으로 간주
    private val PERFECT_ZONE_SIZE = 0.02f // 중앙으로부터 1% 거리 내에서 퍼펙트
    
    private val runnable = object : Runnable {
        override fun run() {
            updatePointerPosition()
            if (_isGameActive.value == true) {
                handler.postDelayed(this, 12) // 약 83fps로 업데이트 빈도 높임
            }
        }
    }

    init {
        loadTimingAlbaData()
    }
    
    // 아이템 획득 이벤트를 소비합니다
    fun consumeItemRewardEvent() {
        _itemRewardEvent.value = null
    }
    
    fun startGame() {
        if (_isCooldown.value == true) return
        
        _isGameActive.value = true
        _pointerPosition.value = 0.0f
        direction = 1
        speed = 0.012f + (_albaLevel.value ?: 1) * 0.002f // 레벨에 따라 속도 더 빠르게 증가
        
        handler.removeCallbacks(runnable)
        handler.post(runnable)
        
        _lastSuccess.value = 0
    }
    
    private fun updatePointerPosition() {
        var currentPosition = _pointerPosition.value ?: 0.0f
        
        // 방향에 따라 포인터 이동
        currentPosition += direction * speed
        
        // 경계에 도달하면 방향 전환
        if (currentPosition >= 1.0f) {
            currentPosition = 1.0f
            direction = -1
        } else if (currentPosition <= 0.0f) {
            currentPosition = 0.0f
            direction = 1
        }
        
        _pointerPosition.value = currentPosition
    }
    
    fun checkTiming() {
        if (_isGameActive.value != true) return
        
        _isGameActive.value = false
        handler.removeCallbacks(runnable)
        
        val position = _pointerPosition.value ?: 0.0f
        val centerDistance = Math.abs(position - 0.5f)
        
        // 중앙 영역(0.45 ~ 0.55)에 있는지 확인
        if (centerDistance <= SUCCESS_ZONE_SIZE) {
            // 성공 처리
            _lastSuccess.value = 1
            
            // 중앙에 가까울수록 높은 보상 (최대 5배로 증가)
            val normalizedDistance = centerDistance / SUCCESS_ZONE_SIZE // 0~1 사이 값
            val multiplier = if (centerDistance <= PERFECT_ZONE_SIZE) {
                5.0f // 퍼펙트 타이밍 - 5배로 증가
            } else {
                5.0f - (normalizedDistance * 3.0f) // 거리에 따라 2~5배 보상으로 범위 확대
            }
            
            _rewardMultiplier.value = multiplier
            
            // 5번 성공할 때마다 레벨업
            val currentAttempts = sharedPreferences.getInt("successful_attempts", 0)
            val newAttempts = (currentAttempts + 1) % 5
            sharedPreferences.edit().putInt("successful_attempts", newAttempts).apply()
            
            // 남은 성공 횟수 업데이트
            _successfulAttempts.value = newAttempts
            
            if (newAttempts == 0) {
                val currentLevel = _albaLevel.value ?: 1
                val newLevel = currentLevel + 1
                _albaLevel.value = newLevel
                sharedPreferences.edit().putInt("timing_alba_level", newLevel).apply()
                
                // 레벨업 시 아이템 재고 증가 처리
                val context = getApplication<Application>().applicationContext
                val itemReward = ItemUtil.processTimingAlbaLevelUp(context, newLevel)
                
                // 아이템 재고 증가 이벤트 발생
                if (itemReward != null) {
                    _itemRewardEvent.value = itemReward
                }
            }
        } else {
            // 실패 처리
            _lastSuccess.value = -1
            _rewardMultiplier.value = 0f
        }
        
        // 쿨다운 시작
        startCooldown()
    }
    
    private fun startCooldown() {
        _isCooldown.value = true
        _cooldownTime.value = 5 // 5초 쿨다운
        
        val cooldownRunnable = object : Runnable {
            override fun run() {
                val currentTime = _cooldownTime.value ?: 0
                if (currentTime > 0) {
                    _cooldownTime.value = currentTime - 1
                    handler.postDelayed(this, 1000)
                } else {
                    _isCooldown.value = false
                }
            }
        }
        
        handler.postDelayed(cooldownRunnable, 1000)
    }
    
    fun getRewardAmount(): Int {
        val baseReward = 500 * (_albaLevel.value ?: 1) // 100 -> 500으로 증가
        val multiplier = _rewardMultiplier.value ?: 0f
        return (baseReward * multiplier).toInt()
    }
    
    private fun loadTimingAlbaData() {
        _albaLevel.value = sharedPreferences.getInt("timing_alba_level", 1)
        _successfulAttempts.value = sharedPreferences.getInt("successful_attempts", 0)
    }
    
    fun resetTimingAlba() {
        _albaLevel.value = 1
        _isGameActive.value = false
        _isCooldown.value = false
        _cooldownTime.value = 0
        _lastSuccess.value = 0
        _successfulAttempts.value = 0
        
        val editor = sharedPreferences.edit()
        editor.putInt("timing_alba_level", 1)
        editor.putInt("successful_attempts", 0)
        editor.apply()
        
        handler.removeCallbacksAndMessages(null)
    }
    
    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }

    // 게임 상태에 따른 버튼 동작 처리
    fun onGameButtonClicked() {
        val isActive = _isGameActive.value ?: false
        val isCooldown = _isCooldown.value ?: false
        
        // 쿨다운 중이면 아무 동작 안함
        if (isCooldown) return
        
        // 게임 중이면 타이밍 체크
        if (isActive) {
            checkTiming()
        } 
        // 게임 중이 아니면 게임 시작
        else {
            startGame()
        }
    }
} 