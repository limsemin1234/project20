package com.example.p20.helpers

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * 미디어 플레이어 관련 유틸리티 기능을 제공하는 헬퍼 클래스
 * MediaPlayer 초기화, 재생, 정리 등의 중복 코드 제거를 위해 사용
 */
object MediaHelper {
    
    private const val TAG = "MediaHelper"
    
    /**
     * 미디어 플레이어를 생성하고 초기화합니다.
     * 
     * @param context 컨텍스트
     * @param resId 음원 리소스 ID
     * @param isLooping 반복 재생 여부
     * @param volume 볼륨 (0.0f ~ 1.0f)
     * @return 초기화된 MediaPlayer 인스턴스
     */
    fun createMediaPlayer(
        context: Context, 
        resId: Int, 
        isLooping: Boolean = false, 
        volume: Float = 1.0f
    ): MediaPlayer? {
        try {
            val mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer.isLooping = isLooping
            mediaPlayer.setVolume(volume, volume)
            return mediaPlayer
        } catch (e: Exception) {
            Log.e(TAG, "미디어 플레이어 생성 오류: ${e.message}")
            return null
        }
    }
    
    /**
     * 미디어 플레이어를 재생합니다.
     * 이미 재생 중이면 멈추고 처음부터 다시 재생합니다.
     * 
     * @param mediaPlayer 미디어 플레이어
     */
    fun playSound(mediaPlayer: MediaPlayer?) {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.prepare()
                }
                it.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "미디어 플레이어 재생 오류: ${e.message}")
        }
    }
    
    /**
     * 미디어 플레이어의 볼륨을 설정합니다.
     * 
     * @param mediaPlayer 미디어 플레이어
     * @param volume 볼륨 (0.0f ~ 1.0f)
     */
    fun setVolume(mediaPlayer: MediaPlayer?, volume: Float) {
        try {
            val safeVolume = volume.coerceIn(0.0f, 1.0f)
            mediaPlayer?.setVolume(safeVolume, safeVolume)
        } catch (e: Exception) {
            Log.e(TAG, "볼륨 설정 오류: ${e.message}")
        }
    }
    
    /**
     * 미디어 플레이어를 일시정지합니다.
     * 
     * @param mediaPlayer 미디어 플레이어
     */
    fun pauseMediaPlayer(mediaPlayer: MediaPlayer?) {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "미디어 플레이어 일시정지 오류: ${e.message}")
        }
    }
    
    /**
     * 미디어 플레이어를 중지하고 해제합니다.
     * 
     * @param mediaPlayer 미디어 플레이어
     */
    fun releaseMediaPlayer(mediaPlayer: MediaPlayer?) {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "미디어 플레이어 해제 오류: ${e.message}")
        }
    }
    
    /**
     * 에러 리스너를 설정합니다.
     * 
     * @param mediaPlayer 미디어 플레이어
     * @param context 컨텍스트
     * @param resId 오류 발생 시 재생할 음원 리소스 ID
     */
    fun setErrorListener(mediaPlayer: MediaPlayer?, context: Context, resId: Int) {
        mediaPlayer?.setOnErrorListener { mp, what, extra ->
            Log.e(TAG, "MediaPlayer 오류: what=$what, extra=$extra")
            
            try {
                mp.release()
                val newMediaPlayer = MediaPlayer.create(context, resId)
                newMediaPlayer.isLooping = mediaPlayer.isLooping
                newMediaPlayer.start()
                
                // MediaPlayer 객체 교체를 위한 콜백 필요
                // 이 메서드에서는 직접 교체 불가능하므로 true 반환
            } catch (e: Exception) {
                Log.e(TAG, "미디어 플레이어 오류 복구 실패: ${e.message}")
            }
            true
        }
    }
    
    /**
     * 여러 미디어 플레이어를 한번에 정리합니다.
     * 
     * @param mediaPlayers 미디어 플레이어 목록
     */
    fun releaseAll(vararg mediaPlayers: MediaPlayer?) {
        for (player in mediaPlayers) {
            releaseMediaPlayer(player)
        }
    }
} 