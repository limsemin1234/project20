package com.example.p20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import android.os.Handler
import android.os.Looper

class AlbaViewModel : ViewModel() {
    private val _touchCount = MutableLiveData(0)
    val touchCount: LiveData<Int> get() = _touchCount

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
            startCooldown() // 10번 터치 후 쿨다운 시작
        }
    }

    private fun startCooldown() {
        _isCooldown.value = true
        _cooldownTime.value = 30 // 30초로 초기화

        // 1초마다 남은 시간 업데이트
        handler.postDelayed(object : Runnable {
            override fun run() {
                val currentTime = _cooldownTime.value ?: 0
                if (currentTime > 1) {
                    _cooldownTime.value = currentTime - 1
                    handler.postDelayed(this, 1000)
                } else {
                    _touchCount.value = 0 // 터치 횟수 초기화
                    _isCooldown.value = false // 쿨다운 종료
                    _cooldownTime.value = 0 // 남은 시간 초기화
                }
            }
        }, 1000)
    }
}
