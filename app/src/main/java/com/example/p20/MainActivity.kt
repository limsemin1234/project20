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
import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {

    private lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
    private lateinit var assetTextView: TextView // 최상단 자산 표시
    private lateinit var stockViewModel: StockViewModel
    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언
    private lateinit var realEstateViewModel: RealEstateViewModel // 부동산 뷰모델 추가
    private lateinit var albaViewModel: AlbaViewModel // 알바 뷰모델 추가
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

        // RealEstateViewModel 초기화
        realEstateViewModel = ViewModelProvider(this).get(RealEstateViewModel::class.java)

        // AlbaViewModel 초기화
        albaViewModel = ViewModelProvider(this).get(AlbaViewModel::class.java)

        // TimeViewModel 초기화 (수정)
        timeViewModel = ViewModelProvider(this).get(TimeViewModel::class.java)

        // LiveData 감시하여 UI 업데이트
        timeViewModel.time.observe(this) { newTime ->
            timeInfo.text = "시간: $newTime"
        }

        // 앱 시작 시 저장된 상태 그대로 시작
        timeViewModel.startTimer()

        // AssetViewModel 초기화
        assetViewModel = ViewModelProvider(this, AssetViewModelFactory(applicationContext))
            .get(AssetViewModel::class.java)
        assetTextView = findViewById(R.id.assetInfo)

        // 자산 초기 표시
        assetViewModel.asset.observe(this) { newAsset ->
            assetTextView.text = assetViewModel.getAssetText()
        }

        /////////////////////////////버튼///////////////////////////////
        val buttonReset = findViewById<Button>(R.id.buttonReset)
        val buttonAlba = findViewById<Button>(R.id.buttonAlba)
        val buttonStock = findViewById<Button>(R.id.buttonStock)
        val buttonRealEstate = findViewById<Button>(R.id.buttonRealEstate)
        val buttonExit = findViewById<Button>(R.id.buttonExit)
        val buttonEarnMoney = findViewById<Button>(R.id.buttonEarnMoney)
        val buttonMyInfo = findViewById<Button>(R.id.buttonMyInfo)
        val buttonItem = findViewById<Button>(R.id.buttonItem)
        val slidePanel = findViewById<LinearLayout>(R.id.slidePanel)

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

        buttonReset.setOnClickListener {
            titleText.visibility = View.GONE
            slidePanel.visibility = View.GONE
            showFragment(InfoFragment(),"InfoFragment")
        }

        buttonAlba.setOnClickListener {
            titleText.visibility = View.GONE
            slidePanel.visibility = View.GONE
            showFragment(AlbaFragment(), "AlbaFragment")
        }

        buttonStock.setOnClickListener {
            titleText.visibility = View.GONE
            slidePanel.visibility = View.GONE
            showFragment(StockFragment(), "StockFragment")
        }

        buttonRealEstate.setOnClickListener {
            titleText.visibility = View.GONE
            slidePanel.visibility = View.GONE
            showFragment(RealEstateFragment(), "RealEstateFragment")
        }

        buttonMyInfo.setOnClickListener {
            titleText.visibility = View.GONE
            slidePanel.visibility = View.GONE
            showFragment(RealInfoFragment(), "RealInfoFragment")
        }

        buttonItem.setOnClickListener {
            titleText.visibility = View.GONE
            slidePanel.visibility = View.GONE
            showFragment(ItemFragment(), "ItemFragment")
        }

        buttonEarnMoney.setOnClickListener {
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

        buttonExit.setOnClickListener {
            slidePanel.visibility = View.GONE
            AlertDialog.Builder(this)
                .setTitle("게임 종료")
                .setMessage("정말 종료하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    stockViewModel.saveStockData()
                    finishAffinity()
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val existingFragment = supportFragmentManager.findFragmentByTag(tag)

        val transaction = supportFragmentManager.beginTransaction()

        if (existingFragment == null) {
            transaction.setCustomAnimations(
                R.anim.fragment_slide_in,
                R.anim.fragment_slide_out,
                R.anim.fragment_pop_in,
                R.anim.fragment_pop_out
            )
            transaction.replace(R.id.contentFrame, fragment, tag)
        } else {
            transaction.setCustomAnimations(
                R.anim.fragment_pop_in,
                R.anim.fragment_pop_out
            )
            transaction.show(existingFragment)
        }

        transaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()
        stockViewModel.saveStockData()
    }
}
