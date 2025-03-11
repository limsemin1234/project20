package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer


class MainActivity : AppCompatActivity() {

    private lateinit var assetManager: AssetManager // 자산 관리 객체
    private lateinit var assetTextView: TextView // 최상단 자산 표시

    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)

        val timeInfo: TextView = findViewById(R.id.timeInfo)

        // ViewModel 초기화 (기존 viewModels() 대신 ViewModelProvider 사용)
        timeViewModel = ViewModelProvider(this).get(TimeViewModel::class.java)

        // LiveData 감시하여 UI 업데이트
        timeViewModel.time.observe(this, Observer { newTime ->
            timeInfo.text = "시간: $newTime"
        })

        // 타이머 시작
        timeViewModel.startTimer()


        // 자산 관리 객체 초기화
        assetManager = AssetManager()
        assetTextView = findViewById(R.id.assetInfo) // 자산 TextView

        // 자산 초기 표시
        assetTextView.text = assetManager.getAssetText()

        val button1 = findViewById<Button>(R.id.button1)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)

        button1.setOnClickListener {
            contentFrame.setBackgroundColor(Color.parseColor("#FFC1C1")) // 연한 빨강
            removeCurrentFragment() // 현재 프래그먼트 제거
        }

        button2.setOnClickListener {
            showFragment(AlbaFragment(), "AlbaFragment") // 알바 화면 표시
        }

        button3.setOnClickListener {
            showFragment(StockFragment(), "StockFragment") // 주식 화면 표시
        }

        button4.setOnClickListener {
            contentFrame.setBackgroundColor(Color.parseColor("#FFFACD")) // 연한 노랑
            removeCurrentFragment() // 현재 프래그먼트 제거
        }

        button5.setOnClickListener {
            finish() // 앱 종료
        }
    }

    // 자산 증가 함수
    fun increaseAsset(amount: Int) {
        assetManager.increaseAsset(amount) // AssetManager를 통해 자산 증가
        assetTextView.text = assetManager.getAssetText() // 자산 텍스트 업데이트
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)
        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contentFrame, fragment, tag)
                .commit()
        }
    }

    // 현재 프래그먼트 제거 함수
    private fun removeCurrentFragment() {
        supportFragmentManager.findFragmentById(R.id.contentFrame)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }
}
