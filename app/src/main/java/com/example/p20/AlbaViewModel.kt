package com.example.p20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import android.os.Handler
import android.os.Looper

class AlbaViewModel : ViewModel() {
    private val _touchCount = MutableLiveData(0)
    val touchCount: LiveData<Int> get() = _touchCount

    private val _albaLevel = MutableLiveData(1) // 알바 레벨
    val albaLevel: LiveData<Int> get() = _albaLevel

    private val _isCooldown = MutableLiveData(false) // 30초 쿨다운 상태 확인
    val isCooldown: LiveData<Boolean> get() = _isCooldown

    private val _cooldownTime = MutableLiveData(0) // 남은 시간, 앱 실행 시 초기값을 0으로 설정
    val cooldownTime: LiveData<Int> get() = _cooldownTime

    private val handler = Handler(Looper.getMainLooper())



    // 터치 횟수 증가 함수
    fun increaseTouchCount() {
        // 쿨다운 중이거나 터치 횟수가 10번 이상이면 터치 횟수 증가를 막음
        if (_isCooldown.value == false && _touchCount.value ?: 0 < 10) {
            _touchCount.value = (_touchCount.value ?: 0) + 1
        }

        // 터치 횟수가 10번에 도달하면 쿨다운 시작
        if (_touchCount.value == 10) {
            //increaseLevel()  // 레벨업 처리
            startCooldown()  // 10번 터치 후 쿨다운 시작
        }
    }


    // 알바 레벨업 함수
    private fun increaseLevel() {
        val currentLevel = _albaLevel.value ?: 1
        val newLevel = currentLevel + 1
        _albaLevel.value = newLevel

        // 터치 횟수 초기화
        _touchCount.value = 0
    }

    fun startCooldown() {
        // 쿨다운 상태로 전환하고, 쿨다운 타이머를 시작
        _isCooldown.value = true
        // 쿨다운 시간 설정 (예: 30초)
        _cooldownTime.value = 30

        // 쿨다운 타이머가 끝나면 쿨다운 종료 후 레벨업을 처리
        val cooldownRunnable = object : Runnable {
            override fun run() {
                if (_cooldownTime.value ?: 0 > 0) {
                    _cooldownTime.value = (_cooldownTime.value ?: 0) - 1
                    Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                } else {
                    // 쿨다운 종료 후 레벨업 가능 상태로 설정
                    _isCooldown.value = false
                    _touchCount.value = 0  // 쿨다운 후에는 다시 터치 횟수 초기화
                    increaseLevel()  // 쿨다운 후에 레벨업
                }
            }
        }

        Handler(Looper.getMainLooper()).post(cooldownRunnable)
    }


    // 레벨에 따른 보상 금액 계산
    fun getRewardAmount(): Int {
        val level = _albaLevel.value ?: 1
        return level * 100 // 레벨마다 100원씩 증가
    }
}
