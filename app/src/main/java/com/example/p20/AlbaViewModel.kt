package com.example.p20

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData


class AlbaViewModel : ViewModel() {
    // 알바 터치 횟수
    private val _touchCount = MutableLiveData<Int>()
    val touchCount: LiveData<Int> get() = _touchCount

    init {
        // 초기 터치 횟수는 0으로 설정
        _touchCount.value = 0
    }

    // 자산 증가 함수
    fun increaseTouchCount() {
        // 터치 횟수 10번까지만 자산 증가
        if (_touchCount.value ?: 0 < 10) {
            _touchCount.value = (_touchCount.value ?: 0) + 1
        }
    }

    // 터치 횟수 리셋
    fun resetTouchCount() {
        _touchCount.value = 0
    }
}