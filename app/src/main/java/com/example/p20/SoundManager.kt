package com.example.p20

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import java.util.HashMap

class SoundManager private constructor(private val context: Context) {
    private var backgroundMusic: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val soundMap = HashMap<Int, Int>()
    
    companion object {
        @Volatile
        private var instance: SoundManager? = null
        
        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build()
            
        soundPool?.setOnLoadCompleteListener { _, sampleId, _ ->
            soundMap[sampleId] = sampleId
        }
    }
    
    // 배경음악 관리
    fun playBackgroundMusic(resId: Int) {
        stopBackgroundMusic()
        backgroundMusic = MediaPlayer.create(context, resId).apply {
            isLooping = true
            start()
        }
    }
    
    fun pauseBackgroundMusic() {
        backgroundMusic?.pause()
    }
    
    fun resumeBackgroundMusic() {
        backgroundMusic?.start()
    }
    
    fun stopBackgroundMusic() {
        backgroundMusic?.release()
        backgroundMusic = null
    }
    
    // 효과음 관리
    fun loadSound(resId: Int) {
        soundPool?.load(context, resId, 1)
    }
    
    fun playSound(resId: Int) {
        soundMap[resId]?.let { soundId ->
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
    
    fun release() {
        stopBackgroundMusic()
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
} 