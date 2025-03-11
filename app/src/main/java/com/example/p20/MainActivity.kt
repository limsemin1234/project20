package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer


class MainActivity : AppCompatActivity() {

    private lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
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


        // AssetViewModel 초기화
        assetViewModel = ViewModelProvider(this).get(AssetViewModel::class.java)
        assetTextView = findViewById(R.id.assetInfo) // 자산 TextView

        // 자산 초기 표시 (LiveData를 통해 자산 값을 가져옴)
        assetViewModel.asset.observe(this, Observer { newAsset ->
            assetTextView.text = assetViewModel.getAssetText() // 자산 텍스트 업데이트
        })

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
            AlertDialog.Builder(this)
                .setTitle("게임 종료")
                .setMessage("정말 종료하시겠습니까?")
                .setPositiveButton("예") { _, _ -> finish() }
                .setNegativeButton("아니오", null)
                .show()
        }
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
