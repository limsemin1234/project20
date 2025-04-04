package com.example.p20

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.view.animation.AlphaAnimation
import androidx.fragment.app.DialogFragment
import com.example.p20.ResetFragment
import com.example.p20.RealInfoFragment
import com.example.p20.ItemFragment

class MainActivity : AppCompatActivity() {

    private lateinit var assetViewModel: AssetViewModel // 자산 관리 뷰모델
    private lateinit var assetTextView: TextView // 최상단 자산 표시
    private lateinit var stockViewModel: StockViewModel
    private lateinit var timeViewModel: TimeViewModel // 뷰모델 선언
    private lateinit var realEstateViewModel: RealEstateViewModel // 부동산 뷰모델 추가
    private lateinit var albaViewModel: AlbaViewModel // 알바 뷰모델 추가
    private lateinit var globalRemainingTimeTextView: TextView // 전역 남은 시간 표시 텍스트뷰
    private lateinit var mainRestartMessageTextView: TextView // 메인 재시작 메시지 텍스트뷰
    // private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }
    private lateinit var gameOverExitButton: Button
    private lateinit var gameOverRestartMessageText: TextView // 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            timeInfo.text = "게임시간: $newTime"
        }
        
        // 전역 남은 시간 UI 업데이트 추가
        globalRemainingTimeTextView = findViewById(R.id.globalRemainingTimeInfo) // 텍스트뷰 참조
        timeViewModel.remainingTime.observe(this) { remainingSeconds ->
            globalRemainingTimeTextView.text = "남은 시간: ${remainingSeconds}초"
            
            // 10초 이하일 때 깜빡이는 애니메이션
            if (remainingSeconds <= 10) {
                val anim = AlphaAnimation(0.0f, 1.0f)
                anim.duration = 500
                anim.repeatMode = Animation.REVERSE
                anim.repeatCount = Animation.INFINITE
                globalRemainingTimeTextView.startAnimation(anim)
            } else {
                globalRemainingTimeTextView.clearAnimation()
            }
        }

        // --- 수정: 다시 시작 요청 처리 로직 변경 ---
        timeViewModel.restartRequested.observe(this) { requested ->
            if (requested) {
                // 실제 데이터 리셋 (ViewModel의 resetTimer에서 restartRequested를 false로 돌림)
                timeViewModel.resetTimer()
                assetViewModel.resetAssets()
                stockViewModel.resetStocks()
                albaViewModel.resetAlba()
                realEstateViewModel.resetRealEstatePrices()
                // 필요하다면 다른 ViewModel 리셋 추가

                // --- 추가: ExplanationFragment 표시 ---
                val explanationTag = "ExplanationFragment"
                if (supportFragmentManager.findFragmentByTag(explanationTag) == null) {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.contentFrame, ExplanationFragment(), explanationTag)
                        .commit()
                }
                // --- 추가 끝 ---
            }
        }
        // --- 수정 끝 ---

        // --- 추가: 게임 오버 뷰 및 버튼 참조 ---
        // gameOverView = findViewById(R.id.gameOverView)
        // gameOverFinalAssetText = findViewById(R.id.gameOverFinalAssetText)
        // gameOverRestartButton = findViewById(R.id.gameOverRestartButton)
        // gameOverExitButton = findViewById(R.id.gameOverExitButton)
        // gameOverRestartMessageText = findViewById(R.id.gameOverRestartMessageText)
        // --- 추가 끝 ---

        // --- 추가: View.post를 사용하여 gameOverView 숨김 예약 ---
        // gameOverView.post { ... }
        // --- 추가 끝 ---

        // --- 수정: 게임 오버 처리 로직 (DialogFragment 사용) ---
        timeViewModel.isGameOver.observe(this) { isGameOver ->
            val dialogTag = "GameOverDialog"
            val existingDialog = supportFragmentManager.findFragmentByTag(dialogTag) as? DialogFragment

            if (isGameOver) {
                globalRemainingTimeTextView.clearAnimation()
                // 다이얼로그가 이미 떠 있지 않다면 새로 띄움
                if (existingDialog == null) {
                     GameOverDialogFragment().show(supportFragmentManager, dialogTag)
                }
            } else {
                // 게임 오버 상태가 아니라면 다이얼로그 닫기
                existingDialog?.dismiss()
            }
        }
        // --- 수정 끝 ---

        // TimeViewModel 초기화 후 명시적으로 게임 타이머 시작 호출
        timeViewModel.startGameTimer()

        // 앱 시작 시 저장된 상태 그대로 시작 -> startGameTimer로 대체됨
        // timeViewModel.startTimer()

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

        // --- 추가: 앱 첫 시작 시 ExplanationFragment 추가 ---
        if (savedInstanceState == null) { // 액티비티가 처음 생성될 때만
             supportFragmentManager.beginTransaction()
                .add(R.id.contentFrame, ExplanationFragment(), "ExplanationFragment")
                .commit()
        }
        // --- 추가 끝 ---

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
            // --- 수정: titleText 숨김 제거, ExplanationFragment 제거 추가 ---
            // titleText.visibility = View.GONE
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(ResetFragment(), "ResetFragment")
        }

        buttonAlba.setOnClickListener {
            // --- 수정: titleText 숨김 제거, ExplanationFragment 제거 추가 ---
            // titleText.visibility = View.GONE
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(AlbaFragment(), "AlbaFragment")
        }

        buttonStock.setOnClickListener {
            // --- 수정: titleText 숨김 제거, ExplanationFragment 제거 추가 ---
            // titleText.visibility = View.GONE
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(StockFragment(), "StockFragment")
        }

        buttonRealEstate.setOnClickListener {
            // --- 수정: titleText 숨김 제거, ExplanationFragment 제거 추가 ---
            // titleText.visibility = View.GONE
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(RealEstateFragment(), "RealEstateFragment")
        }

        buttonMyInfo.setOnClickListener {
            // --- 수정: titleText 숨김 제거, ExplanationFragment 제거 추가 ---
            // titleText.visibility = View.GONE
            removeExplanationFragment()
            slidePanel.visibility = View.GONE
            showFragment(RealInfoFragment(), "RealInfoFragment")
        }

        buttonItem.setOnClickListener {
            // --- 수정: titleText 숨김 제거, ExplanationFragment 제거 추가 ---
            // titleText.visibility = View.GONE
            removeExplanationFragment()
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
        // 게임 오버 뷰가 보이는 동안에는 다른 프래그먼트 표시 안 함 (선택 사항)
        // if (findViewById<View>(R.id.gameOverView)?.visibility == View.VISIBLE) {
        //     return
        // }
        
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

    // --- 삭제: onResume 내 GameOverFragment 제거 로직 ---
//    override fun onResume() {
//        super.onResume()
//        // ViewModel이 초기화되었고, 게임 오버 상태가 아닐 때만 확인
//        if (::timeViewModel.isInitialized && timeViewModel.isGameOver.value == false) {
//             val gameOverFragmentTag = \"GameOverFragment\"
//             val existingGameOverFragment = supportFragmentManager.findFragmentByTag(gameOverFragmentTag)
//             if (existingGameOverFragment != null) {
//                 supportFragmentManager.beginTransaction().remove(existingGameOverFragment).commitNow()
//             }
//        }
//    }
    // --- 삭제 끝 ---

    override fun onDestroy() {
        super.onDestroy()
        // --- 삭제: 미사용 핸들러 콜백 제거 ---
        // handler.removeCallbacksAndMessages(null)
        // --- 삭제 끝 ---
    }

    override fun onStop() {
        super.onStop()
        stockViewModel.saveStockData()
    }

    // --- 추가: ExplanationFragment 제거 함수 ---
    private fun removeExplanationFragment() {
        val explanationFragment = supportFragmentManager.findFragmentByTag("ExplanationFragment")
        if (explanationFragment != null) {
            supportFragmentManager.beginTransaction().remove(explanationFragment).commit()
        }
    }
    // --- 추가 끝 ---
}
