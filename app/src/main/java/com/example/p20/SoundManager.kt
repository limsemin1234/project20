package com.example.p20

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.util.Log
import java.util.HashMap

/**
 * 게임 내 소리 관리를 위한 싱글톤 클래스
 */
class SoundManager private constructor(private val context: Context) {
    private var backgroundMusic: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val soundMap = HashMap<Int, Int>()
    
    companion object {
        @Volatile
        private var instance: SoundManager? = null
        
        // 효과음 리소스 ID
        val SOUND_BUTTON = R.raw.main_button_1
        val SOUND_STOCK_SELECT = R.raw.stock_select
        val SOUND_STOCK_BUTTON = R.raw.stock_button
        
        // 카지노 관련 효과음
        val SOUND_BLACKJACK_BUTTON = R.raw.casino_card_select
        val SOUND_BLACKJACK_BET = R.raw.casino_betting
        
        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        try {
            // 오디오 속성 설정
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
                
            // 사운드풀 생성
            soundPool = SoundPool.Builder()
                .setMaxStreams(10)  // 최대 동시 재생 수를 10으로 증가
                .setAudioAttributes(attributes)
                .build()
                
            // 사운드 로드 완료 리스너
            soundPool?.setOnLoadCompleteListener { pool, sampleId, status ->
                if (status == 0) {
                    Log.d("SoundManager", "사운드 로드 완료: $sampleId")
                    soundMap[sampleId] = sampleId
                } else {
                    Log.e("SoundManager", "사운드 로드 실패: $sampleId, 상태: $status")
                }
            }
            
            // 주요 효과음 미리 로드
            preloadCommonSounds()
        } catch (e: Exception) {
            Log.e("SoundManager", "초기화 오류: ${e.message}")
        }
    }
    
    /**
     * 자주 사용되는 효과음을 미리 로드합니다.
     */
    private fun preloadCommonSounds() {
        try {
            // 버튼 효과음
            loadSound(SOUND_BUTTON)
            
            // 주식 관련 효과음
            loadSound(SOUND_STOCK_SELECT)
            loadSound(SOUND_STOCK_BUTTON)
            
            // 카지노 관련 효과음
            loadSound(SOUND_BLACKJACK_BUTTON)
            loadSound(SOUND_BLACKJACK_BET)
            
            Log.d("SoundManager", "공통 효과음 로드 완료")
        } catch (e: Exception) {
            Log.e("SoundManager", "효과음 사전 로드 오류: ${e.message}")
        }
    }
    
    // 배경음악 관리
    fun playBackgroundMusic(resId: Int) {
        try {
            stopBackgroundMusic()
            backgroundMusic = MediaPlayer.create(context, resId)
            backgroundMusic?.apply {
                isLooping = true
                setVolume(0.8f, 0.8f)  // 기본 볼륨 설정
                start()
            }
            Log.d("SoundManager", "배경음악 재생 시작: $resId")
        } catch (e: Exception) {
            Log.e("SoundManager", "배경음악 재생 오류: ${e.message}")
        }
    }
    
    fun pauseBackgroundMusic() {
        try {
            backgroundMusic?.pause()
        } catch (e: Exception) {
            Log.e("SoundManager", "배경음악 일시정지 오류: ${e.message}")
        }
    }
    
    fun resumeBackgroundMusic() {
        try {
            backgroundMusic?.start()
        } catch (e: Exception) {
            Log.e("SoundManager", "배경음악 재개 오류: ${e.message}")
        }
    }
    
    fun stopBackgroundMusic() {
        try {
            backgroundMusic?.release()
            backgroundMusic = null
        } catch (e: Exception) {
            Log.e("SoundManager", "배경음악 정지 오류: ${e.message}")
        }
    }
    
    // 효과음 관리
    fun loadSound(resId: Int): Int {
        try {
            val soundId = soundPool?.load(context, resId, 1) ?: 0
            soundMap[resId] = soundId
            return soundId
        } catch (e: Exception) {
            Log.e("SoundManager", "효과음 로드 오류: ${e.message}")
            return 0
        }
    }
    
    fun playSound(resId: Int) {
        try {
            var soundId = soundMap[resId]
            
            // 사운드가 아직 로드되지 않았다면 로드
            if (soundId == null) {
                soundId = loadSound(resId)
                if (soundId == 0) return  // 로드 실패
            }
            
            // 볼륨 설정 (MainActivity의 설정 가져오기)
            val volume = getCurrentVolume()
            
            // 효과음 재생
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
            Log.d("SoundManager", "효과음 재생: $resId, 볼륨: $volume")
        } catch (e: Exception) {
            Log.e("SoundManager", "효과음 재생 오류: ${e.message}")
        }
    }
    
    /**
     * 현재 설정된 볼륨값을 가져옵니다.
     */
    private fun getCurrentVolume(): Float {
        try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            return prefs.getFloat("current_volume", 0.8f)
        } catch (e: Exception) {
            Log.e("SoundManager", "볼륨 설정 가져오기 오류: ${e.message}")
            return 0.8f  // 기본값 반환
        }
    }
    
    fun release() {
        try {
            stopBackgroundMusic()
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            Log.d("SoundManager", "리소스 해제 완료")
        } catch (e: Exception) {
            Log.e("SoundManager", "리소스 해제 오류: ${e.message}")
        }
    }
} 