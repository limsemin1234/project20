package com.example.p20

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appNameText = findViewById<TextView>(R.id.appNameText)
        val loadingText = findViewById<TextView>(R.id.loadingText)

        // appNameText에 XML 애니메이션 적용
        val titleAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_title_anim)
        appNameText.startAnimation(titleAnimation)

        // 텍스트 페이드인 애니메이션 시작
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 2000 // 2초 동안 페이드인
        loadingText.startAnimation(fadeIn)

        // 3초 후에 MainActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            // 배경음악 시작 플래그 활성화 - MainActivity로 이동하면서 배경음악 시작
            P20Application.setMusicInitialized(true)
            
            // MainActivity로 이동 준비
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("startMusic", true) // 음악 시작 플래그 전달
            
            Log.d("SplashActivity", "MainActivity로 이동 - 배경음악 재생 플래그 설정")
            
            // MainActivity 시작 및 현재 액티비티 종료
            startActivity(intent)
            finish()
        }, 3000)
    }
} 