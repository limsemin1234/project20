package com.example.p20

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class CircleAlbaViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val sharedPreferences = application.getSharedPreferences("circle_alba_data", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    // 레벨 관련 데이터
    private val _albaLevel = MutableLiveData<Int>()
    val albaLevel: LiveData<Int> get() = _albaLevel

    // 게임 진행 상태
    private val _isGameActive = MutableLiveData<Boolean>()
    val isGameActive: LiveData<Boolean> get() = _isGameActive

    // 내부 원 크기 (0.0f ~ 1.0f)
    private val _innerCircleScale = MutableLiveData<Float>()
    val innerCircleScale: LiveData<Float> get() = _innerCircleScale

    // 외부 원 크기 (0.0f ~ 1.0f)
    private val _outerCircleScale = MutableLiveData<Float>()
    val outerCircleScale: LiveData<Float> get() = _outerCircleScale

    // 게임 결과 (1: 성공, 0: 초기, -1: 실패)
    private val _lastSuccess = MutableLiveData<Int>()
    val lastSuccess: LiveData<Int> get() = _lastSuccess

    // 보상 배율
    private val _rewardMultiplier = MutableLiveData<Float>()
    val rewardMultiplier: LiveData<Float> get() = _rewardMultiplier

    // 레벨업까지 필요한 성공 횟수
    private val _successfulAttempts = MutableLiveData<Int>()
    val successfulAttempts: LiveData<Int> get() = _successfulAttempts

    // 쿨다운 관련 데이터
    private val _isCooldown = MutableLiveData<Boolean>()
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData<Int>()
    val cooldownTime: LiveData<Int> get() = _cooldownTime

    // 아이템 보상 이벤트
    private val _itemRewardEvent = MutableLiveData<ItemReward?>()
    val itemRewardEvent: LiveData<ItemReward?> get() = _itemRewardEvent

    // 원 이동 속도 (레벨에 따라 변화)
    private var circleSpeed = 0.01f
    private var innerCircleDirection = 1 // 1: 커짐, -1: 작아짐
    private var outerCircleDirection = -1 // -1: 작아짐, 1: 커짐

    private val runnable = object : Runnable {
        override fun run() {
            updateCircleScales()
            handler.postDelayed(this, 16) // 약 60fps
        }
    }

    init {
        loadCircleAlbaData()
        resetGameState()
    }

    fun startGame() {
        if (_isCooldown.value == true) return

        _isGameActive.value = true
        _lastSuccess.value = 0
        _innerCircleScale.value = 0.2f
        _outerCircleScale.value = 1.0f

        // 레벨에 따라 원 이동 속도 조정
        circleSpeed = 0.005f + (_albaLevel.value?.toFloat() ?: 1f) * 0.001f
        circleSpeed = minOf(circleSpeed, 0.02f) // 최대 속도 제한

        // 원 애니메이션 시작
        handler.post(runnable)
    }

    private fun updateCircleScales() {
        if (_isGameActive.value != true) return

        // 내부 원 크기 업데이트
        var innerScale = _innerCircleScale.value ?: 0.2f
        innerScale += circleSpeed * innerCircleDirection
        
        // 방향 전환 체크
        if (innerScale >= 1.0f) {
            innerScale = 1.0f
            innerCircleDirection = -1
        } else if (innerScale <= 0.2f) {
            innerScale = 0.2f
            innerCircleDirection = 1
        }
        
        _innerCircleScale.value = innerScale

        // 외부 원 크기 업데이트
        var outerScale = _outerCircleScale.value ?: 1.0f
        outerScale += circleSpeed * outerCircleDirection
        
        // 방향 전환 체크
        if (outerScale >= 1.0f) {
            outerScale = 1.0f
            outerCircleDirection = -1
        } else if (outerScale <= 0.2f) {
            outerScale = 0.2f
            outerCircleDirection = 1
        }
        
        _outerCircleScale.value = outerScale
    }

    /**
     * 현재 두 원의 크기 차이에 따라 보상 배율을 계산합니다.
     * @return 보상 배율 (퍼펙트: 5.0, 좋음: 2.0, 실패: 0.0)
     */
    private fun calculateRewardMultiplier(): Float {
        val innerScale = _innerCircleScale.value ?: 0.0f
        val outerScale = _outerCircleScale.value ?: 1.0f
        val difference = Math.abs(innerScale - outerScale)
        
        return when {
            difference <= 0.005f -> 5.0f  // 퍼펙트 (±0.5% - 두 원이 거의 완벽하게 겹칠 때)
            difference <= 0.15f -> 2.0f   // 좋음 (±15%)
            else -> 0.0f                  // 실패
        }
    }

    fun checkTiming() {
        if (_isGameActive.value != true) return
        
        _isGameActive.value = false
        handler.removeCallbacks(runnable)
        
        // 원의 크기 차이로 판정
        val result = calculateRewardMultiplier()
        
        // 로그 출력
        android.util.Log.d("CircleAlbaDebug", "보상 배율: $result")
        
        // 판정 결과 저장
        _rewardMultiplier.value = result
        
        // 결과에 따른 처리
        when {
            result >= 5.0f -> { // 퍼펙트
                _lastSuccess.value = 1
                android.util.Log.d("CircleAlbaDebug", "퍼펙트! 5배 보상")
                processSucessResult()
            }
            result >= 2.0f -> { // 좋음
                _lastSuccess.value = 1
                android.util.Log.d("CircleAlbaDebug", "좋음! 2배 보상")
                processSucessResult()
            }
            else -> { // 실패
                _lastSuccess.value = -1
                android.util.Log.d("CircleAlbaDebug", "실패...")
            }
        }
        
        // 쿨다운 시작
        startCooldown()
    }
    
    private fun processSucessResult() {
        // 성공했을 때 레벨업 로직 처리
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
            sharedPreferences.edit().putInt("circle_alba_level", newLevel).apply()
            
            // 레벨업 시 아이템 재고 증가 처리
            val context = getApplication<Application>().applicationContext
            val itemReward = ItemUtil.processCircleAlbaLevelUp(context, newLevel)
            
            // 아이템 재고 증가 이벤트 발생
            if (itemReward != null) {
                _itemRewardEvent.value = itemReward
            }
        }
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
    
    // 게임 상태 초기화
    fun resetGameState() {
        _isGameActive.value = false
        _lastSuccess.value = 0
        _innerCircleScale.value = 0.5f
        _outerCircleScale.value = 0.5f
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
    
    // 아이템 획득 이벤트를 소비합니다
    fun consumeItemRewardEvent() {
        _itemRewardEvent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
} 