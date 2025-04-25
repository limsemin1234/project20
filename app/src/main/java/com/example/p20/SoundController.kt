package com.example.p20

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 게임의 모든 사운드 관련 기능을 관리하는 컨트롤러 클래스
 * - 배경음악 관리
 * - 효과음 관리
 * - 볼륨 및 음소거 설정
 */
class SoundController private constructor(private val context: Context) {
    
    // 로그 태그 상수 추가
    companion object {
        @Volatile
        private var instance: SoundController? = null
        private const val TAG = "SoundController" // 로그 태그 추가
        
        fun getInstance(context: Context): SoundController {
            return instance ?: synchronized(this) {
                instance ?: SoundController(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // SoundManager 인스턴스 (기존 로직과의 호환성 유지)
    private val soundManager: SoundManager = SoundManager.getInstance(context)
    
    // 버튼 효과음 재생을 위한 SoundPool 변수
    private lateinit var buttonSoundPool: SoundPool
    private var buttonSoundId: Int = 0
    
    // 상태 변수
    private var isMusicPaused = false
    private var isTemporaryMusic = false
    private val originalMusicResId = R.raw.main_music_loop
    
    // 자동 재생 방지 플래그
    private var autoPlayPrevented = false
    
    // 설정 관련
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    init {
        // 버튼 효과음 초기화
        initButtonSoundPool()
        
        // 기본 설정 초기화
        initializeDefaultSettings()
        
        // 배경음악 초기화만 하고 재생은 하지 않음
        setupBackgroundMusicWithoutPlay()
    }
    
    /**
     * 자동 재생 방지 설정
     * 앱 시작 시 배경음악이 자동으로 재생되지 않도록 합니다.
     */
    fun preventAutoPlay() {
        autoPlayPrevented = true
    }
    
    /**
     * 배경음악 초기화 (재생 없이)
     */
    private fun setupBackgroundMusicWithoutPlay() {
        try {
            // 이전에 생성된 배경음악이 있다면 해제
            soundManager.release()
            
            // 배경음악 초기화
            soundManager.initializeMediaPlayer()
            
            // 볼륨 설정
            val currentVolume = getCurrentVolume()
            soundManager.setVolume(currentVolume, currentVolume)
            
            // 오류 리스너 설정
            soundManager.setOnErrorListener { mp, what, extra ->
                Log.e("SoundController", "MediaPlayer 오류: what=$what, extra=$extra")
                
                try {
                    mp.release()
                    soundManager.initializeMediaPlayer()
                } catch (e: Exception) {
                    Log.e("SoundController", "MediaPlayer 오류 복구 실패: ${e.message}")
                }
                true
            }
            
            Log.d("SoundController", "배경음악 초기화 완료 (자동 재생 없음)")
            
        } catch (e: Exception) {
            Log.e("SoundController", "배경음악 초기화 오류: ${e.message}")
        }
    }
    
    /**
     * 배경음악 초기화 및 재생 메소드
     * 설정에 따라 배경음악을 재생합니다 (이미 초기화된 음악이 있다면 사용)
     */
    private fun setupBackgroundMusic() {
        try {
            // 이미 초기화되어 있고 자동 재생이 방지된 경우 초기화를 건너뜀
            if (autoPlayPrevented) {
                Log.d("SoundController", "자동 재생 방지 모드: 초기화 단계에서 음악 재생 안함")
                return
            }
            
            // 배경음악 초기화가 필요한 경우
            if (soundManager.isReleased) {
                soundManager.initializeMediaPlayer()
                
                // 볼륨 설정
                val currentVolume = getCurrentVolume()
                soundManager.setVolume(currentVolume, currentVolume)
                
                // 오류 리스너 설정
                soundManager.setOnErrorListener { mp, what, extra ->
                    Log.e("SoundController", "MediaPlayer 오류: what=$what, extra=$extra")
                    
                    try {
                        mp.release()
                        soundManager.initializeMediaPlayer()
                        
                        if (isSoundEnabled()) {
                            soundManager.startBackgroundMusic()
                        }
                    } catch (e: Exception) {
                        Log.e("SoundController", "MediaPlayer 오류 복구 실패: ${e.message}")
                    }
                    true
                }
            }
            
            // 음악 재생
            if (isSoundEnabled()) {
                Log.d("SoundController", "배경음악 시작: 사운드 활성화됨, 볼륨=${getCurrentVolume()}")
                soundManager.startBackgroundMusic()
            } else {
                Log.d("SoundController", "배경음악 시작 안함: 사운드 비활성화됨")
            }
            
        } catch (e: Exception) {
            Log.e("SoundController", "배경음악 초기화 오류: ${e.message}")
        }
    }
    
    /**
     * 기본 설정 초기화
     */
    private fun initializeDefaultSettings() {
        if (!prefs.contains("sound_enabled")) {
            prefs.edit()
                .putBoolean("sound_enabled", true)
                .putBoolean("sound_effect_enabled", true)
                .putBoolean("mute_enabled", false)
                .putFloat("current_volume", 0.8f)
                .putInt("volume_level", 80)
                .apply()
        }
    }
    
    /**
     * 버튼 효과음을 위한 SoundPool을 초기화합니다.
     */
    private fun initButtonSoundPool() {
        try {
            // AudioAttributes 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                buttonSoundPool = SoundPool.Builder()
                    .setMaxStreams(5)  // 최대 동시 재생 수
                    .setAudioAttributes(audioAttributes)
                    .build()
            } else {
                // 하위 버전 호환성
                @Suppress("DEPRECATION")
                buttonSoundPool = SoundPool(5, AudioManager.STREAM_MUSIC, 0)
            }
            
            // 효과음 로드
            buttonSoundId = buttonSoundPool.load(context, R.raw.main_button_1, 1)
            
            Log.d("SoundController", "버튼 효과음 초기화 완료")
        } catch (e: Exception) {
            Log.e("SoundController", "SoundPool 초기화 오류: ${e.message}")
        }
    }
    
    /**
     * 버튼 효과음을 재생합니다.
     */
    fun playButtonSound() {
        try {
            // 효과음 설정이 활성화되어 있는지 확인
            if (isSoundEffectEnabled()) {
                val volume = getCurrentVolume()
                buttonSoundPool.play(buttonSoundId, volume, volume, 1, 0, 1.0f)
                Log.d("SoundController", "버튼 효과음 재생: 볼륨=$volume")
            }
        } catch (e: Exception) {
            Log.e("SoundController", "효과음 재생 오류: ${e.message}")
        }
    }
    
    // 배경음악 시작
    fun startBackgroundMusic() {
        try {
            if (isSoundEnabled()) {
                Log.d("SoundController", "startBackgroundMusic 호출됨, 현재 재생 중: ${soundManager.isPlaying}")
                if (!soundManager.isPlaying) {
                    soundManager.startBackgroundMusic()
                }
            }
        } catch (e: Exception) {
            Log.e("SoundController", "배경음악 시작 오류: ${e.message}")
        }
    }
    
    /**
     * 배경음악을 일시정지합니다.
     */
    fun pauseBackgroundMusic() {
        soundManager.pauseBackgroundMusic()
    }
    
    // 배경음악 재개
    fun resumeBackgroundMusic() {
        if (isMusicPaused && isSoundEnabled()) {
            soundManager.startBackgroundMusic()
            isMusicPaused = false
        }
    }
    
    // 배경음악 중지
    fun stopBackgroundMusic() {
        soundManager.stopBackgroundMusic()
    }
    
    /**
     * 배경음악을 처음부터 다시 재생합니다.
     * 게임 오버 후 재시작 시 사용됩니다.
     */
    fun restartBackgroundMusic() {
        try {
            Log.d(TAG, "배경음악 처음부터 재시작")
            
            // 현재 재생 중인 배경음악을 중지합니다
            soundManager.stopBackgroundMusic()
            
            // 약간의 딜레이 후 배경음악을 다시 시작합니다
            Handler(Looper.getMainLooper()).postDelayed({
                soundManager.startBackgroundMusic()
                Log.d(TAG, "배경음악이 처음부터 재생됩니다")
            }, 500) // 0.5초 딜레이로 UI 전환이 완료된 후 재생
        } catch (e: Exception) {
            Log.e(TAG, "배경음악 재시작 중 오류 발생: ${e.message}")
        }
    }
    
    // 임시 음악 설정 (예: 시간 경고 효과음)
    fun setTemporaryMusic(musicResId: Int) {
        if (!isTemporaryMusic) {
            isTemporaryMusic = true
            soundManager.setTemporaryMusic(musicResId)
        }
    }
    
    // 원래 음악으로 복원
    fun restoreOriginalMusic() {
        if (isTemporaryMusic) {
            soundManager.restoreOriginalMusic()
            isTemporaryMusic = false
        }
    }
    
    // 볼륨 설정
    fun setVolume(volume: Float) {
        try {
            val safeVolume = volume.coerceIn(0.0f, 1.0f)
            
            soundManager.setVolume(safeVolume, safeVolume)
            
            prefs.edit()
                .putFloat("current_volume", safeVolume)
                .apply()
            
            // 볼륨 변경 이벤트 브로드캐스트 (필요한 설정 변경만 전달)
            val intent = android.content.Intent("com.example.p20.SOUND_SETTINGS_CHANGED")
                .putExtra("volume_changed", true)
                .putExtra("current_volume", safeVolume)
            context.sendBroadcast(intent)
            
            // 로그만 출력하고 메시지는 표시하지 않음
            Log.d("SoundController", "볼륨 설정: $safeVolume")
        } catch (e: Exception) {
            Log.e("SoundController", "볼륨 설정 오류: ${e.message}")
        }
    }
    
    // 현재 볼륨 가져오기
    fun getCurrentVolume(): Float {
        return prefs.getFloat("current_volume", 0.7f)
    }
    
    // 사운드 활성화 여부 확인
    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", true)
    }
    
    // 사운드 활성화/비활성화 설정
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        
        if (enabled) {
            if (!soundManager.isPlaying) {
                soundManager.startBackgroundMusic()
            }
        } else {
            if (soundManager.isPlaying) {
                soundManager.pauseBackgroundMusic()
            }
        }
        
        // 설정 변경 이벤트 브로드캐스트
        val intent = android.content.Intent("com.example.p20.SOUND_SETTINGS_CHANGED")
            .putExtra("sound_enabled", enabled)
        context.sendBroadcast(intent)
    }
    
    // 효과음 설정 업데이트
    fun updateSoundEffectSettings(enabled: Boolean) {
        try {
            prefs.edit().putBoolean("sound_effect_enabled", enabled).apply()
            
            val intent = android.content.Intent("com.example.p20.SOUND_SETTINGS_CHANGED")
                .putExtra("sound_effect_enabled", enabled)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("SoundController", "효과음 설정 오류: ${e.message}")
        }
    }
    
    // 효과음 활성화 여부 확인
    fun isSoundEffectEnabled(): Boolean {
        val soundEffectEnabled = prefs.getBoolean("sound_effect_enabled", true)
        val muted = prefs.getBoolean("mute_enabled", false)
        return soundEffectEnabled && !muted
    }
    
    // 효과음 재생
    fun playSoundEffect(soundId: Int): Boolean {
        try {
            if (!isSoundEffectEnabled()) {
                Log.d("SoundController", "효과음 비활성화 상태: 재생 안함")
                return false
            }
            
            val volume = getCurrentVolume()
            
            // 효과음 로드 및 재생은 SoundManager를 통해 처리
            soundManager.playSound(soundId)
            Log.d("SoundController", "효과음 재생: ID=$soundId, 볼륨=$volume")
            
            return true
        } catch (e: Exception) {
            Log.e("SoundController", "효과음 재생 오류: ${e.message}")
            return false
        }
    }
    
    // 리소스 해제
    fun release() {
        try {
            buttonSoundPool.release()
            soundManager.release()
        } catch (e: Exception) {
            Log.e("SoundController", "리소스 해제 오류: ${e.message}")
        }
    }
    
    // 임시로 설정된 15초 경고 음악이 재생 중인지 확인
    fun isPlaying15SecondWarning(): Boolean {
        return isTemporaryMusic
    }
    
    // SoundManager 인스턴스 접근 메서드
    fun getSoundManager(): SoundManager {
        return soundManager
    }
} 