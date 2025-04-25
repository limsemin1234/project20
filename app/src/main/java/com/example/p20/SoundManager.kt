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
    
    // 임시 음악 관련 변수
    private var isTemporaryMusic = false
    private val originalMusicResId = R.raw.main_music_loop
    
    // 리소스 해제 상태 트래킹
    private var _isReleased = false
    val isReleased: Boolean
        get() = _isReleased
    
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
        // 초기화 코드
        initializePool()
        initializeMediaPlayer()
        
        // 초기화 로그
        Log.d("SoundManager", "SoundManager 초기화 완료")
    }
    
    /**
     * SoundPool 초기화
     */
    private fun initializePool() {
        soundPool?.release()
        
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(16)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(16, android.media.AudioManager.STREAM_MUSIC, 0)
        }
        
        // 소리 맵 초기화
        soundMap.clear()
    }
    
    /**
     * MediaPlayer 초기화
     */
    fun initializeMediaPlayer() {
        try {
            stopBackgroundMusic()
            backgroundMusic = MediaPlayer.create(context, R.raw.main_music_loop)?.apply {
                isLooping = true
                
                // 볼륨 설정 가져오기
                val currentVolume = getCurrentVolume()
                setVolume(currentVolume, currentVolume)
                
                Log.d("SoundManager", "MediaPlayer 초기화 완료, 볼륨: $currentVolume")
            }
            _isReleased = false
        } catch (e: Exception) {
            Log.e("SoundManager", "MediaPlayer 초기화 오류: ${e.message}")
        }
    }
    
    /**
     * 오류 리스너 설정
     */
    fun setOnErrorListener(listener: MediaPlayer.OnErrorListener) {
        backgroundMusic?.setOnErrorListener(listener)
    }
    
    /**
     * 배경 음악의 재생 중 여부 반환
     */
    val isPlaying: Boolean
        get() = backgroundMusic?.isPlaying ?: false
        
    /**
     * 배경 음악의 총 길이 반환
     */
    val duration: Int
        get() = backgroundMusic?.duration ?: 0
        
    /**
     * 배경 음악의 현재 재생 위치 반환
     */
    val currentPosition: Int
        get() = backgroundMusic?.currentPosition ?: 0
        
    /**
     * 배경 음악의 루핑 상태 설정/반환
     */
    var isLooping: Boolean
        get() = backgroundMusic?.isLooping ?: false
        set(value) {
            backgroundMusic?.isLooping = value
        }
    
    /**
     * 볼륨 설정
     */
    fun setVolume(volume: Float, volume2: Float) {
        try {
            val safeVolume1 = volume.coerceIn(0.0f, 1.0f)
            val safeVolume2 = volume2.coerceIn(0.0f, 1.0f)
            
            Log.d("SoundManager", "볼륨 설정: $safeVolume1, $safeVolume2")
            backgroundMusic?.setVolume(safeVolume1, safeVolume2)
        } catch (e: Exception) {
            Log.e("SoundManager", "볼륨 설정 오류: ${e.message}")
        }
    }
    
    // 배경음악 관리
    fun startBackgroundMusic() {
        try {
            if (backgroundMusic == null) {
                initializeMediaPlayer()
            }
            
            if (backgroundMusic?.isPlaying == false) {
                // 볼륨 설정 다시 적용
                val currentVolume = getCurrentVolume()
                setVolume(currentVolume, currentVolume)
                
                backgroundMusic?.start()
                Log.d("SoundManager", "배경음악 재생 시작, 볼륨: $currentVolume")
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "배경음악 재생 오류: ${e.message}")
            // 오류 발생 시 재초기화 시도
            try {
                initializeMediaPlayer()
                backgroundMusic?.start()
            } catch (e2: Exception) {
                Log.e("SoundManager", "배경음악 재시작 실패: ${e2.message}")
            }
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
            val player = backgroundMusic
            if (player != null) {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                backgroundMusic = null
            }
            Log.d("SoundManager", "배경음악 정지 완료")
        } catch (e: Exception) {
            Log.e("SoundManager", "배경음악 정지 오류: ${e.message}")
            // 예외 발생시에도 null 처리하여 메모리 누수 방지
            backgroundMusic = null
        }
    }
    
    /**
     * 임시 배경음악 설정 (특정 이벤트 시)
     */
    fun setTemporaryMusic(musicResId: Int) {
        try {
            if (!isTemporaryMusic && isSoundEnabled()) {
                // 현재 재생 중인 음악 정보 저장
                val currentPlayer = backgroundMusic
                
                // 기존 미디어 플레이어 해제 먼저 수행
                if (currentPlayer != null) {
                    if (currentPlayer.isPlaying) {
                        currentPlayer.stop()
                    }
                    currentPlayer.release()
                    backgroundMusic = null
                }
                
                // 새로운 미디어 플레이어 생성
                backgroundMusic = MediaPlayer.create(context, musicResId)
                backgroundMusic?.isLooping = true
                
                val currentVolume = getCurrentVolume()
                backgroundMusic?.setVolume(currentVolume, currentVolume)
                
                backgroundMusic?.start()
                isTemporaryMusic = true
                
                Log.d("SoundManager", "임시 배경음악으로 변경: $musicResId")
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "임시 음악 설정 오류: ${e.message}")
            // 오류 발생 시 기본 상태로 복원
            isTemporaryMusic = false
        }
    }
    
    /**
     * 원래 배경음악으로 복원
     */
    fun restoreOriginalMusic() {
        try {
            if (isTemporaryMusic && isSoundEnabled()) {
                // 기존 미디어 플레이어 해제 먼저 수행
                val currentPlayer = backgroundMusic
                if (currentPlayer != null) {
                    if (currentPlayer.isPlaying) {
                        currentPlayer.stop()
                    }
                    currentPlayer.release()
                    backgroundMusic = null
                }
                
                // 새로운 미디어 플레이어 생성
                backgroundMusic = MediaPlayer.create(context, originalMusicResId)
                backgroundMusic?.isLooping = true
                
                val currentVolume = getCurrentVolume()
                backgroundMusic?.setVolume(currentVolume, currentVolume)
                
                backgroundMusic?.start()
                isTemporaryMusic = false
                
                Log.d("SoundManager", "원래 배경음악으로 복원")
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "원래 음악 복원 오류: ${e.message}")
            // 플래그는 항상 리셋
            isTemporaryMusic = false
        }
    }
    
    /**
     * 사운드 활성화 여부 확인
     */
    private fun isSoundEnabled(): Boolean {
        return context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("sound_enabled", true)
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
            // 배경 음악 정지 및 해제
            stopBackgroundMusic()
            
            // 사운드풀 정리
            val pool = soundPool
            if (pool != null) {
                pool.release()
                soundPool = null
            }
            
            // 맵 비우기
            soundMap.clear()
            isTemporaryMusic = false
            _isReleased = true
            Log.d("SoundManager", "모든 리소스 해제 완료")
        } catch (e: Exception) {
            Log.e("SoundManager", "리소스 해제 오류: ${e.message}")
            // 예외 발생해도 null 처리
            soundPool = null
            soundMap.clear()
            _isReleased = true
        }
    }
} 