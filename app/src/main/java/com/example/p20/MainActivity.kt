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
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.view.View


class MainActivity : AppCompatActivity() {

    private lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
    private lateinit var assetTextView: TextView // 최상단 자산 표시
    private lateinit var stockViewModel: StockViewModel
    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val titleText = findViewById<TextView>(R.id.titleText)

        // 텍스트가 서서히 나타나는 애니메이션
        val fadeIn = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f)
        fadeIn.duration = 5000 // 5초 동안 애니메이션 진행
        fadeIn.start()

        val contentFrame = findViewById<FrameLayout>(R.id.contentFrame)
        val timeInfo: TextView = findViewById(R.id.timeInfo)

        // StockViewModel 초기화
        stockViewModel = ViewModelProvider(this).get(StockViewModel::class.java)
        // 앱 시작 시부터 주식 가격 변동 시작
        startStockPriceUpdates()


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

        // 각 버튼 클릭 시 동작 설정
        button1.setOnClickListener {
            // 제목 숨기고 '내정보' 프래그먼트로 변경
            titleText.visibility = View.GONE  // 제목 숨기기
            showFragment(InfoFragment(),"InfoFragment") // 예시: '내정보' 프래그먼트
        }

        button2.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            showFragment(AlbaFragment(), "AlbaFragment") // 알바 화면 표시
        }

        button3.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            showFragment(StockFragment(), "StockFragment") // 주식 화면 표시
        }

        button4.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            showFragment(BodongsanFragment(), "BodongsanFragment") // 주식 화면 표시
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

    // 주식 가격 변동을 주기적으로 업데이트하는 함수
    private fun startStockPriceUpdates() {
        val handler = Handler(Looper.getMainLooper())
        val updateInterval = 3000L // 3초마다 업데이트

        val updateRunnable = object : Runnable {
            override fun run() {
                stockViewModel.updateStockPrices() // 주식 가격 업데이트
                handler.postDelayed(this, updateInterval) // 반복 실행
            }
        }

        handler.post(updateRunnable) // 최초 실행
    }

    //이미 같은 프래그먼트가 있다면 새로 추가하지 않도록 체크
    private fun showFragment(fragment: Fragment, tag: String) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        val transaction = supportFragmentManager.beginTransaction()

        if (existingFragment == null) {
            transaction.setCustomAnimations(
                R.anim.fragment_slide_in,   // 진입 애니메이션
                R.anim.fragment_slide_out,  // 퇴장 애니메이션
                R.anim.fragment_pop_in,     // 백스택에서 돌아올 때 진입 애니메이션
                R.anim.fragment_pop_out     // 백스택에서 나갈 때 퇴장 애니메이션
            )
            transaction.replace(R.id.contentFrame, fragment, tag)
        } else {
            // 기존 프래그먼트가 있으면 보여주는 방식으로 처리
            transaction.setCustomAnimations(
                R.anim.fragment_pop_in,
                R.anim.fragment_pop_out
            )
            transaction.show(existingFragment)
        }

        transaction.commit()
    }

}
