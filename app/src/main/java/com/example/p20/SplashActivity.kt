package com.example.p20

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 텍스트뷰 페이드인 애니메이션
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 2000 // 2초 동안 페이드인

        val appNameText = findViewById<TextView>(R.id.appNameText)
        val loadingText = findViewById<TextView>(R.id.loadingText)
        val startButton = findViewById<Button>(R.id.startButton)

        // 텍스트 페이드인 애니메이션 시작
        appNameText.startAnimation(fadeIn)
        loadingText.startAnimation(fadeIn)
        startButton.startAnimation(fadeIn)

        // 게임 시작 버튼 클릭 리스너
        startButton.setOnClickListener {
            // 메인 액티비티로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
} 