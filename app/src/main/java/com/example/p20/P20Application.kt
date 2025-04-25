package com.example.p20

import android.app.Application

/**
 * 애플리케이션 클래스
 * - 컨트롤러들에 대한 전역 접근 제공
 * - 앱 수준의 초기화 처리
 */
class P20Application : Application() {

    private lateinit var soundController: SoundController
    private lateinit var animationController: AnimationController
    
    // 배경음악 시작 여부를 제어하는 플래그
    private var musicInitialized = false

    override fun onCreate() {
        super.onCreate()
        
        // 싱글톤 인스턴스 초기화
        soundController = SoundController.getInstance(this)
        
        // 배경음악 자동 시작 방지
        soundController.preventAutoPlay()
        
        animationController = AnimationController.getInstance(this)
        
        // 컨트롤러 연결
        animationController.setSoundController(soundController)
        
        // 전역 인스턴스 설정
        instance = this
    }

    companion object {
        @Volatile
        private var instance: P20Application? = null

        fun getInstance(): P20Application {
            return instance ?: throw IllegalStateException("P20Application is not initialized")
        }
        
        // 사운드 컨트롤러 접근 메소드
        fun getSoundController(): SoundController {
            return getInstance().soundController
        }
        
        // 애니메이션 컨트롤러 접근 메소드
        fun getAnimationController(): AnimationController {
            return getInstance().animationController
        }
        
        // 배경음악 시작 플래그 설정 메소드
        fun setMusicInitialized(initialized: Boolean) {
            getInstance().musicInitialized = initialized
        }
        
        // 배경음악 시작 플래그 확인 메소드
        fun isMusicInitialized(): Boolean {
            return getInstance().musicInitialized
        }
    }
} 