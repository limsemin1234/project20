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
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout


class MainActivity : AppCompatActivity() {

    private lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
    private lateinit var assetTextView: TextView // 최상단 자산 표시
    private lateinit var stockViewModel: StockViewModel
    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

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


/////////////////////////////////시간관리//////////////////////////
        // ViewModel 초기화 (기존 viewModels() 대신 ViewModelProvider 사용)
        timeViewModel = ViewModelProvider(this, TimeViewModelFactory(applicationContext))
            .get(TimeViewModel::class.java)

        // LiveData 감시하여 UI 업데이트
        timeViewModel.time.observe(this, Observer { newTime ->
            timeInfo.text = "시간: $newTime"
        })

        // 타이머 시작
        timeViewModel.startTimer()


 //////////////////////////자산관리/////////////////////////////////////

        // AssetViewModel 초기화
        assetViewModel = ViewModelProvider(this, AssetViewModelFactory(applicationContext))
            .get(AssetViewModel::class.java)
        assetTextView = findViewById(R.id.assetInfo) // 자산 TextView

        // 자산 초기 표시 (LiveData를 통해 자산 값을 가져옴)
        assetViewModel.asset.observe(this, Observer { newAsset ->
            assetTextView.text = assetViewModel.getAssetText() // 자산 텍스트 업데이트
        })





        /////////////////////////////버튼///////////////////////////////
        val button1 = findViewById<Button>(R.id.button1)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)
        val button6 = findViewById<Button>(R.id.button6)
        val buttonInfo = findViewById<Button>(R.id.buttonInfo)
        val buttonItem = findViewById<Button>(R.id.buttonItem)
        val slidePanel = findViewById<LinearLayout>(R.id.slidePanel)

        // 슬라이드 패널의 반투명 배경 클릭 시 패널 닫기
        slidePanel.getChildAt(0).setOnClickListener {
            val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    slidePanel.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            slidePanel.startAnimation(slideDown)
        }

        // 각 버튼 클릭 시 동작 설정
        button1.setOnClickListener {
            // 제목 숨기고 '내정보' 프래그먼트로 변경
            titleText.visibility = View.GONE  // 제목 숨기기
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            showFragment(InfoFragment(),"InfoFragment") // 예시: '내정보' 프래그먼트
        }

        button2.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            showFragment(AlbaFragment(), "AlbaFragment") // 알바 화면 표시
        }

        button3.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            showFragment(StockFragment(), "StockFragment") // 주식 화면 표시
        }

        button4.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            showFragment(RealEstateFragment(), "RealEstateFragment") // 주식 화면 표시
        }

        buttonInfo.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            showFragment(InfoFragment(), "InfoFragment")
        }

        buttonItem.setOnClickListener {
            titleText.visibility = View.GONE  // 제목 숨기기
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            // 임시로 알림 표시
            AlertDialog.Builder(this)
                .setTitle("아이템")
                .setMessage("아이템 기능은 아직 준비 중입니다.")
                .setPositiveButton("확인", null)
                .show()
        }

        button6.setOnClickListener {
            // 슬라이드 패널 토글
            if (slidePanel.visibility == View.VISIBLE) {
                val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
                slideDown.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        slidePanel.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                slidePanel.startAnimation(slideDown)
            } else {
                slidePanel.visibility = View.VISIBLE
                val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                slidePanel.startAnimation(slideUp)
            }
        }

        button5.setOnClickListener {
            slidePanel.visibility = View.GONE // 슬라이드 패널 숨기기
            AlertDialog.Builder(this)
                .setTitle("게임 종료")
                .setMessage("정말 종료하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    stockViewModel.saveStockData() // 데이터 저장
                    finishAffinity()               // 앱 전체 종료
                }
                .setNegativeButton("아니오", null)
                .show()
        }

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

    // 메모리 릭 방지를 위한 정리 작업
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // activity 종료 시 handler의 모든 콜백과 메시지 제거
    }

    override fun onStop() {
        super.onStop()
        stockViewModel.saveStockData() // 앱 종료 시 주식 데이터 저장
    }

}
