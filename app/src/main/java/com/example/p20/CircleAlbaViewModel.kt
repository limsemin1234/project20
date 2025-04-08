package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.abs

class CircleAlbaViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val sharedPreferences = application.getSharedPreferences("circle_alba_data", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    // 내부 원 크기 (0.1 ~ 1.0)
    private val _innerCircleScale = MutableLiveData(0.5f)
    val innerCircleScale: LiveData<Float> get() = _innerCircleScale

    // 외부 원 크기 (0.1 ~ 1.0)
    private val _outerCircleScale = MutableLiveData(1.0f)
    val outerCircleScale: LiveData<Float> get() = _outerCircleScale

    // 내부 원 크기 변화 방향 (1: 커짐, -1: 작아짐)
    private var innerDirection = 1
    private var outerDirection = -1
    
    // 원 크기 변화 속도
    private var innerSpeed = 0.01f
    private var outerSpeed = 0.008f
    
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
    
    private val runnable = object : Runnable {
        override fun run() {
            updateCircles()
            if (_isGameActive.value == true) {
                handler.postDelayed(this, 16) // 약 60fps로 부드럽게 업데이트
            }
        }
    }

    init {
        loadCircleAlbaData()
    }
    
    // 아이템 획득 이벤트를 소비합니다
    fun consumeItemRewardEvent() {
        _itemRewardEvent.value = null
    }
    
    fun startGame() {
        if (_isCooldown.value == true) return
        
        _isGameActive.value = true
        
        // 시작 위치 및 방향 설정
        _innerCircleScale.value = 0.5f
        _outerCircleScale.value = 1.0f
        innerDirection = 1
        outerDirection = -1
        
        // 레벨에 따라 속도 증가
        val levelBonus = (_albaLevel.value ?: 1) * 0.001f
        innerSpeed = 0.01f + levelBonus
        outerSpeed = 0.008f + levelBonus
        
        // 게임 시작 시 배율 초기화
        _rewardMultiplier.value = 1.0f
        
        handler.removeCallbacks(runnable)
        handler.post(runnable)
        
        _lastSuccess.value = 0
    }
    
    private fun updateCircles() {
        // 내부 원 크기 업데이트
        var currentInnerScale = _innerCircleScale.value ?: 0.5f
        currentInnerScale += innerDirection * innerSpeed
        
        // 내부 원 경계 체크 및 방향 전환
        if (currentInnerScale >= 1.0f) {
            currentInnerScale = 1.0f
            innerDirection = -1
        } else if (currentInnerScale <= 0.1f) {
            currentInnerScale = 0.1f
            innerDirection = 1
        }
        _innerCircleScale.value = currentInnerScale
        
        // 외부 원 크기 업데이트
        var currentOuterScale = _outerCircleScale.value ?: 1.0f
        currentOuterScale += outerDirection * outerSpeed
        
        // 외부 원 경계 체크 및 방향 전환
        if (currentOuterScale >= 1.0f) {
            currentOuterScale = 1.0f
            outerDirection = -1
        } else if (currentOuterScale <= 0.1f) {
            currentOuterScale = 0.1f
            outerDirection = 1
        }
        _outerCircleScale.value = currentOuterScale
    }
    
    fun checkTiming() {
        if (_isGameActive.value != true) return
        
        _isGameActive.value = false
        handler.removeCallbacks(runnable)
        
        val innerScale = _innerCircleScale.value ?: 0.5f
        val outerScale = _outerCircleScale.value ?: 1.0f
        val difference = abs(innerScale - outerScale)
        
        // 판정 결과 (5단계: 0=실패, 1=일반, 2=좋음, 3=매우좋음, 4=퍼펙트)
        val result = when {
            difference <= 0.005f -> 4 // 두 원이 거의 완벽하게 일치 (0.5% 이내)
            difference <= 0.05f -> 3 // 두 원이 매우 비슷함 (5% 이내)
            difference <= 0.15f -> 2 // 두 원이 비슷함 (15% 이내)
            difference <= 0.3f -> 1 // 두 원의 차이가 일반적 (30% 이내)
            else -> 0 // 두 원의 차이가 큼
        }
        
        // 결과에 따라 보상 배율 설정
        _rewardMultiplier.value = when (result) {
            4 -> 5.0f // 퍼펙트 - 5배
            3 -> 3.0f // 매우 좋음 - 3배
            2 -> 2.0f // 좋음 - 2배
            1 -> 1.0f // 일반 - 1배
            else -> 0f // 실패 - 0배
        }
        
        // 성공 여부 설정 (2 이상이면 성공으로 간주)
        if (result >= 2) {
            _lastSuccess.value = 1 // 성공
            
            // 성공했을 때만 레벨업 로직 처리
            val currentAttempts = sharedPreferences.getInt("successful_attempts", 0)
            val newAttempts = (currentAttempts + 1) % 5
            
            sharedPreferences.edit().putInt("successful_attempts", newAttempts).apply()
            
            // 남은 성공 횟수 업데이트
            _successfulAttempts.value = newAttempts
            
            if (newAttempts == 0) {
                val currentLevel = _albaLevel.value ?: 1
                val newLevel = currentLevel + 1
                _albaLevel.value = newLevel
                sharedPreferences.edit().putInt("circle_alba_level", newLevel).apply()
                
                // 레벨업 시 아이템 재고 증가 처리
                val context = getApplication<Application>().applicationContext
                val itemReward = ItemUtil.processCircleAlbaLevelUp(context, newLevel)
                
                // 아이템 재고 증가 이벤트 발생
                if (itemReward != null) {
                    _itemRewardEvent.value = itemReward
                }
            }
        } else {
            _lastSuccess.value = -1 // 실패
        }
        
        // 쿨다운 시작
        startCooldown()
    }
    
    private val cooldownRunnable = object : Runnable {
        override fun run() {
            val currentTime = _cooldownTime.value ?: 0
            if (currentTime > 0) {
                // 쿨다운 중일 때 isCooldown 값을 true로 유지
                _isCooldown.value = true
                _cooldownTime.value = currentTime - 1
                handler.postDelayed(this, 1000)
            } else {
                // 쿨다운이 완전히 끝났을 때만 isCooldown을 false로 설정
                _isCooldown.value = false
                _cooldownTime.value = 0
            }
        }
    }

    private fun startCooldown() {
        _isCooldown.value = true
        _cooldownTime.value = 3 // 3초 쿨다운
        
        // 기존 콜백 제거 후 새로운 카운트다운 시작
        handler.removeCallbacks(cooldownRunnable)
        handler.postDelayed(cooldownRunnable, 1000)
    }
    
    fun getRewardAmount(): Int {
        val baseReward = 500 * (_albaLevel.value ?: 1)
        val multiplier = _rewardMultiplier.value ?: 0f
        return (baseReward * multiplier).toInt()
    }
    
    private fun loadCircleAlbaData() {
        _albaLevel.value = sharedPreferences.getInt("circle_alba_level", 1)
        _successfulAttempts.value = sharedPreferences.getInt("successful_attempts", 0)
    }
    
    fun resetCircleAlba() {
        _albaLevel.value = 1
        _isGameActive.value = false
        _isCooldown.value = false
        _cooldownTime.value = 0
        _lastSuccess.value = 0
        _successfulAttempts.value = 0
        
        val editor = sharedPreferences.edit()
        editor.putInt("circle_alba_level", 1)
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
        val cooldownTime = _cooldownTime.value ?: 0
        
        // 쿨다운 중이거나 쿨다운 시간이 남아있으면 아무 동작 안함
        if (isCooldown || cooldownTime > 0) return
        
        // 게임 중이면 타이밍 체크
        if (isActive) {
            checkTiming()
        } 
        // 게임 중이 아니면 게임 시작
        else {
            startGame()
        }
    }

    // 게임 상태를 초기화하는 메소드
    fun resetGameState() {
        _isGameActive.value = false
        _lastSuccess.value = 0
        _innerCircleScale.value = 0.5f
        _outerCircleScale.value = 1.0f
        handler.removeCallbacks(runnable)
    }
} 