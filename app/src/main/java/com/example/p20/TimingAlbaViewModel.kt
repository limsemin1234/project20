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
        _pointerPosition.value = 0.0f  // 이렇게 하면 조정 후 0.3 위치에서 시작
        direction = 1
        speed = 0.012f + (_albaLevel.value ?: 1) * 0.002f // 레벨에 따라 속도 더 빠르게 증가
        
        // 게임 시작 시 배율 초기화 (이전 실패의 영향으로 0이 되었을 수 있음)
        _rewardMultiplier.value = 1.0f
        
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
        
        // 20분할 기준으로 포인터 위치 확인
        // 0.0~1.0을 20개 구간으로 나누면 각 구간은 0.05 크기
        // 10번째 칸: 0.45~0.5, 11번째 칸: 0.5~0.55
        val isPerfect = position >= 0.45f && position < 0.55f
        
        if (isPerfect) {
            // 성공 처리 (10, 11번째 칸에 들어오면 퍼펙트 성공)
            _lastSuccess.value = 1
            _rewardMultiplier.value = 5.0f // 퍼펙트 - 5배
            
            // 성공 시 디버깅 정보
            android.util.Log.d("TimingAlbaDebug", "퍼펙트 성공!")
            android.util.Log.d("TimingAlbaDebug", "포인터 위치: $position (${position * 100}%)")
            android.util.Log.d("TimingAlbaDebug", "구간: ${Math.floor(position.toDouble() * 20).toInt() + 1}번째 칸")
            android.util.Log.d("TimingAlbaDebug", "배율 설정: ${_rewardMultiplier.value}")
            android.util.Log.d("TimingAlbaDebug", "레벨: ${_albaLevel.value}, 기본 보상: ${500 * (_albaLevel.value ?: 1)}")
            android.util.Log.d("TimingAlbaDebug", "예상 보상: ${500 * (_albaLevel.value ?: 1) * 5.0f}")
            
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
            // 실패 처리 (10, 11번째 칸 외의 위치)
            _lastSuccess.value = -1
            _rewardMultiplier.value = 0f
            
            // 디버깅 정보 (개발 중에만 사용)
            android.util.Log.d("TimingAlbaDebug", "실패 처리:")
            android.util.Log.d("TimingAlbaDebug", "포인터 위치: $position (${position * 100}%)")
            android.util.Log.d("TimingAlbaDebug", "구간: ${Math.floor(position.toDouble() * 20).toInt() + 1}번째 칸")
            android.util.Log.d("TimingAlbaDebug", "판정: 구간이 10, 11번째 칸(0.45~0.55)이 아님")
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
        _cooldownTime.value = 5 // 5초 쿨다운
        
        // 기존 콜백 제거 후 새로운 카운트다운 시작
        handler.removeCallbacks(cooldownRunnable)
        handler.postDelayed(cooldownRunnable, 1000)
    }
    
    fun getRewardAmount(): Int {
        val baseReward = 500 * (_albaLevel.value ?: 1) // 100 -> 500으로 증가
        val multiplier = _rewardMultiplier.value ?: 0f
        val reward = (baseReward * multiplier).toInt()
        
        // 보상 계산 디버깅 로그
        android.util.Log.d("TimingAlbaDebug", "보상 계산:")
        android.util.Log.d("TimingAlbaDebug", "기본 보상: $baseReward, 현재 배율: $multiplier")
        android.util.Log.d("TimingAlbaDebug", "최종 보상: $reward")
        
        return reward
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
        _pointerPosition.value = 0.0f
        // 게임 상태 초기화 시 배율 초기화하지 않음 (_rewardMultiplier 값 유지)
    }

    // 배율에 따른 영역 크기를 반환하는 함수 (Fragment에서 사용)
    fun getMultiplierZoneSize(multiplier: Int): Float {
        return when (multiplier) {
            5 -> 0.05f // 20분할 시 한 칸의 크기
            else -> 0f
        }
    }
} 