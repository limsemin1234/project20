package com.example.p20

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.drawerlayout.widget.DrawerLayout

/**
 * 해킹 알바 프래그먼트 클래스
 * 숫자 비밀번호를 추측하는 논리 게임 제공
 */
class HackingAlbaFragment : BaseFragment() {

    // 클래스 상수 정의
    companion object {
        private const val TAG = "HackingAlba"
        
        // 효과음 리소스 ID 상수
        private val SOUND_CORRECT = R.raw.alba_hacking_success
        private val SOUND_WRONG = R.raw.alba_hacking_fail
        private val SOUND_TYPING = R.raw.alba_hacking_number
        private val SOUND_START = R.raw.alba_hacking_start
        private val SOUND_SUCCESS = R.raw.alba_hacking_success
        private val SOUND_FAIL = R.raw.alba_hacking_fail_end
        private val SOUND_HACKING_START = R.raw.alba_hacking_start
        private val SOUND_HACKING_BUTTON = R.raw.alba_hacking_button
    }
    
    private lateinit var albaViewModel: AlbaViewModel
    
    // 핸들러 추가
    private val handler = Handler(Looper.getMainLooper())
    
    // 드로어 리스너 객체 저장
    private var drawerListener: DrawerLayout.DrawerListener? = null
    
    // 게임 관련 변수
    private var secretCode = intArrayOf(0, 0, 0, 0) // 4자리 비밀번호
    private var attemptCount = 0 // 시도 횟수
    private var maxAttempts = 10 // 최대 시도 횟수
    private var isGameActive = false // 게임 활성화 상태
    private var currentLevel = 1 // 현재 레벨 (프래그먼트 생성 시 SharedPreferences에서 로드됨)
    
    // UI 요소
    private lateinit var gameContainer: ViewGroup
    private lateinit var codeInputContainer: ViewGroup
    private lateinit var historyContainer: ViewGroup
    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var historyPanelContainer: ViewGroup
    private lateinit var historyScrollView: ScrollView
    private lateinit var startButton: Button
    private lateinit var submitButton: Button
    private lateinit var toggleHistoryButton: Button
    private lateinit var closeHistoryButton: Button
    private lateinit var levelText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var feedbackText: TextView
    private lateinit var lastResultTextView: TextView
    private lateinit var digitButtons: Array<TextView>
    private lateinit var codeDigits: Array<TextView>
    private lateinit var deleteButton: TextView  // DEL 버튼 전역 변수로 추가
    
    // 기록 창 표시 상태
    private var isHistoryShown = false
    
    // 게임 데이터
    private var reward: Long = 0L
    
    // 효과음 로드 상태 추적
    private val loadedSounds = mutableSetOf<Int>()
    
    // 리시버 변수 추가
    private var soundSettingsReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_hacking_alba, container, false)

        albaViewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[AlbaViewModel::class.java]
        
        // 저장된 레벨 불러오기
        loadSavedLevel()
        
        // UI 요소 초기화
        initializeViews(view)
        
        // 효과음 로드
        loadSounds()
        
        // 효과음 설정 변경을 수신하는 리시버 등록
        registerSoundSettingsReceiver()
        
        // 이벤트 리스너 설정
        setupEventListeners()
        
        // 초기 UI 상태 설정 - 게임은 비활성화 상태로 시작
        isGameActive = false
        updateUIState(false)
        
        // DrawerLayout 리스너 설정
        drawerListener = object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // 드로어가 슬라이드될 때의 동작
            }

            override fun onDrawerOpened(drawerView: View) {
                // 드로어가 열렸을 때 isHistoryShown 상태 업데이트
                isHistoryShown = true
                
                // 스크롤을 맨 위로 (최신 결과가 보이도록)
                historyScrollView.fullScroll(ScrollView.FOCUS_UP)
                
                android.util.Log.d("HackingAlba", "기록 사이드 패널 열림")
            }

            override fun onDrawerClosed(drawerView: View) {
                // 드로어가 닫혔을 때 isHistoryShown 상태 업데이트
                isHistoryShown = false
                
                android.util.Log.d("HackingAlba", "기록 사이드 패널 닫힘")
            }

            override fun onDrawerStateChanged(newState: Int) {
                // 드로어 상태가 변경되었을 때의 동작
            }
        }
        
        drawerLayout.addDrawerListener(drawerListener!!)
        
        // 디버깅 로그 추가
        android.util.Log.d("HackingAlba", "프래그먼트 생성됨, 버튼 초기 상태: 시작=${startButton.isEnabled}, 제출=${submitButton.isEnabled}")
        
        return view
    }
    
    /**
     * UI 요소들을 초기화합니다.
     */
    private fun initializeViews(view: View) {
        gameContainer = view.findViewById(R.id.gameContainer)
        codeInputContainer = view.findViewById(R.id.codeInputContainer)
        historyContainer = view.findViewById(R.id.historyContainer)
        drawerLayout = view.findViewById(R.id.drawerLayout)
        historyPanelContainer = view.findViewById(R.id.historyPanelContainer)
        historyScrollView = view.findViewById(R.id.historyScrollView)
        startButton = view.findViewById<Button>(R.id.startButton)
        submitButton = view.findViewById<Button>(R.id.submitButton)
        toggleHistoryButton = view.findViewById<Button>(R.id.toggleHistoryButton)
        closeHistoryButton = view.findViewById<Button>(R.id.closeHistoryButton)
        levelText = view.findViewById(R.id.levelText)
        attemptsText = view.findViewById(R.id.attemptsText)
        feedbackText = view.findViewById(R.id.feedbackText)
        lastResultTextView = view.findViewById(R.id.lastResultTextView)
        deleteButton = view.findViewById(R.id.digit_delete)  // DEL 버튼 초기화
        
        // 숫자 입력 버튼 초기화
        digitButtons = Array(10) { index ->
            view.findViewById<TextView>(
                resources.getIdentifier("digit_$index", "id", requireContext().packageName)
            )
        }
        
        // 코드 표시 텍스트뷰 초기화
        codeDigits = Array(4) { index ->
            view.findViewById<TextView>(
                resources.getIdentifier("code_digit_$index", "id", requireContext().packageName)
            )
        }
        
        // 초기 텍스트 설정
        levelText.text = "Lv.${currentLevel} 해킹 알바"
        attemptsText.text = "시도: 0/${maxAttempts}"
        feedbackText.text = "해킹을 시작하려면 시작 버튼을 누르세요."
        lastResultTextView.visibility = View.GONE
    }
    
    /**
     * 이벤트 리스너를 설정합니다.
     */
    private fun setupEventListeners() {
        // 시작 버튼
        startButton.setOnClickListener {
            // 해킹 시작 효과음
            playSound(SOUND_HACKING_START)
            startNewGame()
        }
        
        // 제출 버튼 - 명시적으로 다시 설정
        submitButton.setOnClickListener { 
            android.util.Log.d("HackingAlba", "제출 버튼 클릭됨")
            // 코드 입력 효과음
            playSound(SOUND_HACKING_BUTTON)
            checkCode() 
        }
        
        // 기록 버튼 - 사이드 패널 표시
        toggleHistoryButton.setOnClickListener {
            // 패널 상태를 토글
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                hideHistoryDialog()
            } else {
                showHistoryDialog()
            }
        }
        
        // 기록 닫기 버튼
        closeHistoryButton.setOnClickListener {
            hideHistoryDialog()
        }
        
        // 숫자 버튼
        for (i in 0 until 10) {
            digitButtons[i].setOnClickListener {
                if (isGameActive) {
                    inputDigit(i)
                    playSound(SOUND_TYPING)
                }
            }
        }
        
        // 취소(DEL) 버튼 리스너 추가
        deleteButton.setOnClickListener {
            android.util.Log.d("HackingAlba", "DEL 버튼 클릭됨")
            if (isGameActive) {
                deleteLastDigit()
                playSound(SOUND_TYPING) // 타이핑 효과음 사용
            }
        }
    }
    
    /**
     * 기록 사이드 패널을 표시합니다.
     */
    private fun showHistoryDialog() {
        // 기록 컨테이너가 비어있으면 안내 메시지 추가
        if (historyContainer.childCount == 0) {
            val emptyView = layoutInflater.inflate(R.layout.item_hacking_result, historyContainer, false)
            val codeText = emptyView.findViewById<TextView>(R.id.codeTextView)
            val resultText = emptyView.findViewById<TextView>(R.id.resultTextView)
            
            codeText.text = "기록 없음"
            resultText.text = "아직 시도한 결과가 없습니다."
            
            historyContainer.addView(emptyView)
        }
        
        // 드로어 레이아웃을 사용하여 오른쪽에서 슬라이드
        drawerLayout.openDrawer(GravityCompat.END)
        
        // 스크롤 초기화
        historyScrollView.fullScroll(ScrollView.FOCUS_UP)
        
        isHistoryShown = true
        
        // 디버깅 로그
        android.util.Log.d("HackingAlba", "기록 사이드 패널 표시됨")
    }
    
    /**
     * 기록 사이드 패널을 숨깁니다.
     */
    private fun hideHistoryDialog() {
        // 드로어 레이아웃을 사용하여 패널 닫기
        drawerLayout.closeDrawer(GravityCompat.END)
        
        isHistoryShown = false
        
        // 디버깅 로그
        android.util.Log.d("HackingAlba", "기록 사이드 패널 숨겨짐")
    }
    
    /**
     * 게임이 시작될 때 호출됩니다.
     */
    private fun startNewGame() {
        isGameActive = true
        attemptCount = 0
        
        // 기록 컨테이너 초기화
        historyContainer.removeAllViews()
        
        // 레벨에 따른 난이도 설정
        difficultySetting()
        
        // 4자리 비밀 코드 생성
        generateSecretCode()
        
        // UI 상태 업데이트
        updateUIState(true)
        
        // 피드백 초기화
        feedbackText.text = "숫자를 입력하세요. (${maxAttempts}번의 기회가 있습니다)"
        updateAttemptsText()
        
        // 직전 결과 텍스트 초기화 및 숨기기
        lastResultTextView.text = ""
        lastResultTextView.visibility = View.GONE
        
        // 레벨에 따른 난이도 설정
        updateDifficultyByLevel()
        
        // 게임 카운터 초기화
        for (digit in codeDigits) {
            digit.text = "_"
        }
        
        // 마지막 결과 있으면 불러오기
        loadLastGameResult()
        
        // 게임 시작 로그
        android.util.Log.d("HackingAlba", "게임 시작: 비밀번호 ${secretCode.joinToString("")}")
    }
    
    /**
     * 게임 성공 시 호출됩니다.
     */
    private fun gameSuccess() {
        isGameActive = false
        
        // UI 상태 업데이트
        updateUIState(false)
        
        // 피드백 업데이트
        feedbackText.text = "해킹 성공! 보상: ${formatCurrency(reward)}원"
        
        // 성공 효과음 재생
        playSound(SOUND_SUCCESS)
        
        // 성공 시각적 효과 표시
        showSuccessAnimation(reward)
        
        // 보상 지급
        assetViewModel.increaseAsset(reward)
        
        // 게임 결과 저장
        saveGameResult(true)
        
        // 레벨업 확인
        checkLevelUp()
    }
    
    /**
     * 해킹 성공 시 시각적 효과를 표시합니다.
     * @param rewardAmount 획득한 보상 금액
     */
    private fun showSuccessAnimation(rewardAmount: Long) {
        try {
            // 1. 배경 플래시 효과
            val backgroundEffect = View(requireContext())
            backgroundEffect.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            backgroundEffect.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            backgroundEffect.alpha = 0f
            
            gameContainer.addView(backgroundEffect)
            
            // 플래시 효과 애니메이션
            val fadeIn = ObjectAnimator.ofFloat(backgroundEffect, "alpha", 0f, 0.3f)
            fadeIn.duration = 300
            
            val fadeOut = ObjectAnimator.ofFloat(backgroundEffect, "alpha", 0.3f, 0f)
            fadeOut.duration = 700
            fadeOut.startDelay = 300
            
            // 효과 종료 후 뷰 제거
            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    gameContainer.removeView(backgroundEffect)
                }
            })
            
            // 애니메이션 시작
            fadeIn.start()
            fadeOut.start()
            
            // 2. 획득 금액 애니메이션
            val rewardTextView = TextView(requireContext())
            rewardTextView.apply {
                text = "+${formatCurrency(rewardAmount)}원"
                textSize = 30f
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                setShadowLayer(10f, 2f, 2f, ContextCompat.getColor(requireContext(), android.R.color.black))
                gravity = android.view.Gravity.CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                alpha = 0f
            }
            
            // 텍스트뷰 중앙에 배치
            gameContainer.addView(rewardTextView)
            rewardTextView.y = (gameContainer.height / 2 - rewardTextView.height / 2).toFloat()
            
            // 텍스트 애니메이션
            val textFadeIn = ObjectAnimator.ofFloat(rewardTextView, "alpha", 0f, 1f)
            textFadeIn.duration = 500
            
            val scaleX = ObjectAnimator.ofFloat(rewardTextView, "scaleX", 0.5f, 1.2f, 1f)
            scaleX.duration = 1000
            
            val scaleY = ObjectAnimator.ofFloat(rewardTextView, "scaleY", 0.5f, 1.2f, 1f)
            scaleY.duration = 1000
            
            val textFadeOut = ObjectAnimator.ofFloat(rewardTextView, "alpha", 1f, 0f)
            textFadeOut.duration = 500
            textFadeOut.startDelay = 2000
            
            // 효과 종료 후 뷰 제거
            textFadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    gameContainer.removeView(rewardTextView)
                }
            })
            
            // 애니메이션 시작
            textFadeIn.start()
            scaleX.start()
            scaleY.start()
            textFadeOut.start()
            
            // 3. 코드 입력창 성공 효과
            for (digit in codeDigits) {
                // 색상 변경
                digit.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
                
                // 펄스 애니메이션
                val pulse = ObjectAnimator.ofFloat(digit, "scaleX", 1f, 1.3f, 1f)
                pulse.duration = 600
                pulse.repeatCount = 1
                pulse.start()
                
                val pulseY = ObjectAnimator.ofFloat(digit, "scaleY", 1f, 1.3f, 1f)
                pulseY.duration = 600
                pulseY.repeatCount = 1
                pulseY.start()
            }
            
            android.util.Log.d("HackingAlba", "성공 애니메이션 시작됨: 보상=${formatCurrency(rewardAmount)}원")
        } catch (e: Exception) {
            android.util.Log.e("HackingAlba", "성공 애니메이션 오류: ${e.message}")
        }
    }

    /**
     * 게임 실패 시 호출됩니다.
     */
    private fun gameFailed() {
        isGameActive = false
        
        // UI 상태 업데이트
        updateUIState(false)
        
        // 피드백 업데이트
        feedbackText.text = "해킹 실패! 올바른 코드는 ${secretCode.joinToString("")}이었습니다."
        
        // 실패 효과음 재생
        playSound(SOUND_FAIL)
        
        // 게임 결과 저장
        saveGameResult(false)
    }
    
    /**
     * 게임 결과를 저장합니다.
     */
    private fun saveGameResult(isSuccess: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        editor.putBoolean("last_game_result_exists", true)
        editor.putBoolean("last_game_success", isSuccess)
        editor.putInt("last_game_attempt_count", attemptCount)
        editor.putString("last_game_secret_code", secretCode.joinToString(""))
        editor.putLong("last_game_reward", reward)
        editor.putInt("last_game_level", currentLevel)
        
        editor.apply()
    }
    
    /**
     * 마지막 게임 결과를 불러옵니다.
     */
    private fun loadLastGameResult() {
        val sharedPreferences = requireActivity().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        val resultExists = sharedPreferences.getBoolean("last_game_result_exists", false)
        
        if (resultExists) {
            val isSuccess = sharedPreferences.getBoolean("last_game_success", false)
            val lastAttemptCount = sharedPreferences.getInt("last_game_attempt_count", 0)
            val lastSecretCode = sharedPreferences.getString("last_game_secret_code", "")
            val lastReward = sharedPreferences.getLong("last_game_reward", 0)
            val lastLevel = sharedPreferences.getInt("last_game_level", 1)
            
            // 마지막 게임 결과 표시
            val resultMessage = if (isSuccess) {
                "지난 게임: Lv.${lastLevel} 성공 (${lastAttemptCount}번 시도, +${formatCurrency(lastReward)}원)"
            } else {
                "지난 게임: Lv.${lastLevel} 실패 (비밀번호: ${lastSecretCode})"
            }
            
            lastResultTextView.text = resultMessage
            lastResultTextView.visibility = View.VISIBLE
            
            // 성공/실패에 따른 텍스트 색상 설정
            if (isSuccess) {
                lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            } else {
                lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            }
            
            // 결과 데이터 초기화 (한 번만 표시)
            val editor = sharedPreferences.edit()
            editor.putBoolean("last_game_result_exists", false)
            editor.apply()
        }
    }
    
    /**
     * UI 상태를 업데이트합니다.
     * @param isPlaying 게임 진행 중 여부
     */
    private fun updateUIState(isPlaying: Boolean) {
        // 게임 진행 상태에 따라 버튼 활성화/비활성화 설정
        startButton.isEnabled = !isPlaying
        submitButton.isEnabled = isPlaying
        
        // 버튼 색상 변경
        val startButtonColor = if (!isPlaying) 
            ContextCompat.getColor(requireContext(), R.color.alba_start_button)
        else 
            ContextCompat.getColor(requireContext(), R.color.button_disabled)
            
        val submitButtonColor = if (isPlaying) 
            ContextCompat.getColor(requireContext(), R.color.alba_start_button)
        else 
            ContextCompat.getColor(requireContext(), R.color.button_disabled)
        
        // 버튼 배경색 변경 (API 레벨에 따라 다른 방식 사용)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startButton.backgroundTintList = ColorStateList.valueOf(startButtonColor)
            submitButton.backgroundTintList = ColorStateList.valueOf(submitButtonColor)
        }
        
        // 키패드 버튼 활성화/비활성화
        for (button in digitButtons) {
            button.isEnabled = isPlaying
        }
        
        // 로그 기록
        android.util.Log.d("HackingAlba", "UI 상태 업데이트: 게임 활성화=$isPlaying")
    }

    /**
     * 레벨에 따른 난이도를 설정합니다.
     */
    private fun difficultySetting() {
        // 난이도에 따른 최대 시도 횟수 설정
        maxAttempts = when {
            currentLevel < 5 -> 10
            currentLevel < 10 -> 8
            else -> 6
        }
    }
    
    /**
     * 4자리 비밀 코드를 생성합니다.
     */
    private fun generateSecretCode() {
        // 레벨 10 이상에서는 숫자 중복을 허용
        if (currentLevel >= 10) {
            // 중복 허용하는 코드 생성
            for (i in secretCode.indices) {
                secretCode[i] = (0..9).random()
            }
        } else {
            // 중복 없는 코드 생성
            val numbers = (0..9).toMutableList()
            numbers.shuffle()
            for (i in secretCode.indices) {
                secretCode[i] = numbers[i]
            }
        }
    }
    
    /**
     * 레벨에 따른 난이도를 업데이트합니다.
     */
    private fun updateDifficultyByLevel() {
        // 레벨에 따른 난이도 표시 갱신
        levelText.text = "Lv.${currentLevel} 해킹 알바"
        
        // 레벨에 따른 보상 계산
        val baseReward = 10000L + (currentLevel - 1) * 5000L
        reward = baseReward
        
        // 게임 피드백 메시지 업데이트
        feedbackText.text = "4자리 비밀번호를 추측하세요.\n성공 시 최대 ${formatCurrency(baseReward * 2)}원 획득 가능"
    }
    
    /**
     * 현재 시도 횟수 텍스트를 업데이트합니다.
     */
    private fun updateAttemptsText() {
        attemptsText.text = "시도: $attemptCount/$maxAttempts"
    }
    
    /**
     * 레벨업 확인 및 처리를 수행합니다.
     */
    private fun checkLevelUp() {
        // 레벨 증가
        currentLevel++
        
        // 레벨 텍스트 업데이트
        levelText.text = "Lv.${currentLevel} 해킹 알바"
        
        // 레벨업 메시지 표시
        feedbackText.text = "레벨업! Lv.${currentLevel} 달성"
        
        // 레벨 저장
        saveCurrentLevel()
    }

    /**
     * 현재 레벨을 불러옵니다.
     */
    private fun loadSavedLevel() {
        val sharedPreferences = requireActivity().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        currentLevel = sharedPreferences.getInt("current_level", 1)
        android.util.Log.d("HackingAlba", "저장된 레벨 불러옴: $currentLevel")
    }
    
    /**
     * 현재 레벨을 저장합니다.
     */
    private fun saveCurrentLevel() {
        val sharedPreferences = requireActivity().getSharedPreferences("hacking_alba_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("current_level", currentLevel)
        editor.apply()
        android.util.Log.d("HackingAlba", "현재 레벨 저장됨: $currentLevel")
    }

    /**
     * 효과음 설정 변경을 처리하는 BroadcastReceiver를 등록합니다.
     */
    private fun registerSoundSettingsReceiver() {
        soundSettingsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.p20.SOUND_SETTINGS_CHANGED") {
                    // 효과음 설정이 변경되었을 때 로그 출력
                    val isSoundEffectEnabled = intent.getBooleanExtra("sound_effect_enabled", true)
                    val isMuted = intent.getBooleanExtra("mute_enabled", false)
                    
                    android.util.Log.d("HackingAlba", "효과음 설정 변경 감지: 효과음=${isSoundEffectEnabled}, 음소거=${isMuted}")
                }
            }
        }
        
        context?.let { ctx ->
            val filter = IntentFilter("com.example.p20.SOUND_SETTINGS_CHANGED")
            LocalBroadcastManager.getInstance(ctx).registerReceiver(soundSettingsReceiver!!, filter)
            // 전역 브로드캐스트도 등록
            ctx.registerReceiver(soundSettingsReceiver!!, filter)
            
            android.util.Log.d("HackingAlba", "효과음 설정 변경 리시버 등록됨")
        }
    }

    /**
     * 효과음을 로드합니다.
     */
    private fun loadSounds() {
        try {
            // 필요한 모든 효과음 리소스 ID 배열
            val soundResources = intArrayOf(
                SOUND_CORRECT, SOUND_WRONG, SOUND_TYPING, SOUND_START, 
                SOUND_SUCCESS, SOUND_FAIL, SOUND_HACKING_START, SOUND_HACKING_BUTTON
            )
            
            val soundManager = soundController.getSoundManager()
            
            // 효과음 로드 상태 초기화
            loadedSounds.clear()
            
            // 모든 효과음 미리 로드
            for (soundId in soundResources) {
                val result = soundManager.loadSound(soundId)
                if (result > 0) {
                    loadedSounds.add(soundId)
                    android.util.Log.d(TAG, "효과음 로드 성공: $soundId")
                } else {
                    android.util.Log.e(TAG, "효과음 로드 실패: $soundId")
                }
            }
            
            // 로드된 효과음 개수 확인
            android.util.Log.d(TAG, "효과음 로드 결과: ${loadedSounds.size}/${soundResources.size}개 로드됨")
            
            // 누락된 효과음이 있다면 재시도
            if (loadedSounds.size < soundResources.size) {
                android.util.Log.d(TAG, "일부 효과음 로드 실패, 3초 후 재시도")
                Handler(Looper.getMainLooper()).postDelayed({
                    // 누락된 효과음만 다시 로드
                    for (soundId in soundResources) {
                        if (!loadedSounds.contains(soundId)) {
                            val retryResult = soundManager.loadSound(soundId)
                            if (retryResult > 0) {
                                loadedSounds.add(soundId)
                                android.util.Log.d(TAG, "효과음 재로드 성공: $soundId")
                            } else {
                                android.util.Log.e(TAG, "효과음 재로드 실패: $soundId")
                            }
                        }
                    }
                }, 3000) // 3초 딜레이 후 재시도
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "효과음 로드 중 오류 발생: ${e.message}")
            showErrorMessage("효과음 로드 중 오류가 발생했습니다.")
        }
    }

    /**
     * BaseFragment의 onReloadSounds 메서드를 오버라이드하여
     * 화면이 다시 보여질 때 효과음을 다시 로드합니다.
     */
    override fun onReloadSounds() {
        android.util.Log.d(TAG, "onReloadSounds 호출됨: 해킹알바 효과음 재로드")
        loadSounds()
    }

    /**
     * 효과음을 재생합니다.
     */
    private fun playSound(soundId: Int) {
        try {
            if (soundId <= 0) {
                android.util.Log.e(TAG, "효과음 재생 실패: 유효하지 않은 soundId=$soundId")
                return
            }
            
            // 첫번째 방식: SoundController의 playSoundEffect 메서드 사용
            val success = soundController.playSoundEffect(soundId)
            
            // 실패한 경우 백업 방식 사용
            if (!success) {
                android.util.Log.d(TAG, "SoundController를 통한 효과음 재생 실패: soundId=$soundId, 백업 방식으로 재시도")
                
                // 효과음이 로드되어 있는지 확인 후 재생
                if (!loadedSounds.contains(soundId)) {
                    android.util.Log.d(TAG, "효과음이 로드되지 않음, 로드 시도: soundId=$soundId")
                    val loadResult = soundController.getSoundManager().loadSound(soundId)
                    if (loadResult > 0) {
                        loadedSounds.add(soundId)
                    }
                }
                
                // 직접 SoundManager로 효과음 재생 시도
                soundController.getSoundManager().playSound(soundId)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "효과음 재생 오류: soundId=$soundId, 오류=${e.message}")
        }
    }
    
    /**
     * 입력한 코드를 확인합니다.
     */
    private fun checkCode() {
        if (!isGameActive) return
        
        // 현재 입력된 코드 가져오기
        val inputCode = IntArray(4) { i ->
            codeDigits[i].text.toString().toIntOrNull() ?: -1
        }
        
        // 모든 자리를 입력했는지 확인
        if (inputCode.contains(-1)) {
            feedbackText.text = "4자리 숫자를 모두 입력하세요."
            return
        }
        
        // 시도 횟수 증가
        attemptCount++
        updateAttemptsText()
        
        // 입력 확인 중 메시지 표시
        feedbackText.text = "코드 확인 중..."
        
        // 제출 버튼 일시적으로 비활성화
        submitButton.isEnabled = false
        
        // 숫자 키패드 일시적으로 비활성화
        for (button in digitButtons) {
            button.isEnabled = false
        }
        
        // 1초 지연 후에 결과 처리
        handler.postDelayed({
            processCodeResult(inputCode)
        }, 1000) // 1초 지연
    }
    
    /**
     * 코드 검증 결과를 처리합니다.
     */
    private fun processCodeResult(inputCode: IntArray) {
        // 정답 확인
        var correctPosition = 0 // 숫자와 위치가 모두 맞는 개수
        var correctDigit = 0 // 숫자는 맞지만 위치가 틀린 개수
        
        // 입력 코드와 비밀 코드의 중복 체크를 위한 배열
        val secretChecked = BooleanArray(4) { false }
        val inputChecked = BooleanArray(4) { false }
        
        // 숫자와 위치가 모두 맞는 경우 체크
        for (i in secretCode.indices) {
            if (inputCode[i] == secretCode[i]) {
                correctPosition++
                secretChecked[i] = true
                inputChecked[i] = true
            }
        }
        
        // 숫자만 맞는 경우 체크
        for (i in secretCode.indices) {
            if (!inputChecked[i]) {
                for (j in secretCode.indices) {
                    if (!secretChecked[j] && inputCode[i] == secretCode[j]) {
                        correctDigit++
                        secretChecked[j] = true
                        break
                    }
                }
            }
        }
        
        // 직전 결과 텍스트뷰에 표시
        val lastResultString = "입력: ${inputCode.joinToString("")} → ${correctPosition}S ${correctDigit}B"
        lastResultTextView.text = lastResultString
        lastResultTextView.visibility = View.VISIBLE
        
        // 색상 설정 - 정답일 경우 초록색, 아닐 경우 기본 흰색
        if (correctPosition == 4) {
            lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))
            // 성공 효과음 재생
            playSound(SOUND_SUCCESS)
            
            // 보상 조정 - 시도 횟수가 적을수록 추가 보상
            val efficiencyBonus = (maxAttempts - attemptCount + 1).toFloat() / maxAttempts
            reward = (reward * (1 + efficiencyBonus)).toLong()
            
            // 성공 처리 (효과음은 gameSuccess 내부에서 재생)
            gameSuccess()
            return
        } else {
            lastResultTextView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            // 부분 정답 또는 오답 효과음 재생
            if (correctPosition > 0 || correctDigit > 0) {
                playSound(SOUND_WRONG)
            } else {
                playSound(SOUND_WRONG)
            }
        }
        
        // 결과 피드백 생성
        val resultView = createResultView(inputCode, correctPosition, correctDigit)
        historyContainer.addView(resultView, 0) // 최신 결과를 상단에 추가
        
        // 기록이 표시되지 않은 상태면 자동으로 사이드 패널 표시
        if (!isHistoryShown) {
            // 약간의 지연 후 기록 패널 표시
            handler.postDelayed({
                showHistoryDialog()
                
                // 정답이거나 게임 종료 시에만 자동 닫기 스케줄링
                if (correctPosition == 4 || attemptCount >= maxAttempts) {
                    // 3초 후에 자동으로 닫기
                    handler.postDelayed({
                        if (isHistoryShown) {
                            hideHistoryDialog()
                        }
                    }, 3000)
                }
            }, 200)
        } else {
            // 이미 표시되어 있는 경우 스크롤만 조정
            historyScrollView.fullScroll(ScrollView.FOCUS_UP)
        }
        
        // 최대 시도 횟수 초과
        if (attemptCount >= maxAttempts) {
            // 게임 UI 비활성화
            fadeOutGameComponents()
            
            // 실패 처리 (효과음은 gameFailed 내부에서 재생)
            gameFailed()
            return
        }
        
        // 게임이 계속되는 경우에만 버튼과 키패드 다시 활성화
        if (isGameActive) {
            // 다시 제출 버튼 활성화
            submitButton.isEnabled = true
            
            // 숫자 키패드 다시 활성화
            for (button in digitButtons) {
                button.isEnabled = true
            }
        }
        
        // 계속 진행
        resetInputDigits()
        feedbackText.text = "힌트: $correctPosition 개 숫자와 위치 일치, $correctDigit 개 숫자만 일치"
    }
    
    /**
     * 결과 표시를 위한 뷰를 생성합니다.
     */
    private fun createResultView(inputCode: IntArray, correctPosition: Int, correctDigit: Int): View {
        val resultView = layoutInflater.inflate(R.layout.item_hacking_result, historyContainer, false)
        
        // 입력 코드 표시
        val codeText = resultView.findViewById<TextView>(R.id.codeTextView)
        codeText.setText(inputCode.joinToString(""))
        
        // 결과 표시
        val resultText = resultView.findViewById<TextView>(R.id.resultTextView)
        resultText.setText("${correctPosition}S ${correctDigit}B")
        
        return resultView
    }
    
    /**
     * 입력 숫자를 리셋합니다.
     */
    private fun resetInputDigits() {
        for (digit in codeDigits) {
            digit.setText("_")
        }
    }
    
    /**
     * 숫자를 입력합니다.
     */
    private fun inputDigit(value: Int) {
        // 빈 자리 찾기
        for (i in codeDigits.indices) {
            if (codeDigits[i].text == "_" || codeDigits[i].text.toString() == "") {
                codeDigits[i].setText(value.toString())
                
                // 모든 자리가 입력되었는지 확인
                var allFilled = true
                for (digit in codeDigits) {
                    if (digit.text == "_" || digit.text.toString() == "") {
                        allFilled = false
                        break
                    }
                }
                
                // 모든 자리가 채워지면 로그 출력
                if (allFilled) {
                    android.util.Log.d("HackingAlba", "모든 자리 입력 완료")
                }
                
                return
            }
        }
    }

    /**
     * 게임 UI 컴포넌트들을 서서히 흐려지게 하는 애니메이션을 수행합니다.
     * 최대 시도 횟수에 도달했을 때 게임 종료 효과로 사용됩니다.
     */
    private fun fadeOutGameComponents() {
        try {
            // 숫자 패드 애니메이션
            for (button in digitButtons) {
                button.animate()
                    .alpha(0.5f)
                    .setDuration(800)
                    .start()
            }
            
            // 코드 입력창 애니메이션
            for (digit in codeDigits) {
                digit.animate()
                    .alpha(0.5f)
                    .setDuration(800)
                    .start()
            }
            
            // 제출 버튼 애니메이션
            submitButton.animate()
                .alpha(0.5f)
                .setDuration(800)
                .start()
            
            // 코드 입력 컨테이너 흐려짐 효과
            codeInputContainer.animate()
                .alpha(0.7f)
                .setDuration(1000)
                .start()
            
            android.util.Log.d("HackingAlba", "게임 UI 페이드아웃 애니메이션 시작됨")
        } catch (e: Exception) {
            android.util.Log.e("HackingAlba", "페이드아웃 애니메이션 오류: ${e.message}")
        }
    }

    /**
     * 마지막으로 입력된 숫자를 삭제합니다.
     */
    private fun deleteLastDigit() {
        // 맨 마지막에 입력된 숫자부터 거꾸로 찾아서 지웁니다
        for (i in codeDigits.indices.reversed()) {
            if (codeDigits[i].text != "_" && codeDigits[i].text.toString() != "") {
                codeDigits[i].setText("_")
                
                // 로그 출력
                android.util.Log.d("HackingAlba", "마지막 입력 숫자 삭제됨")
                return
            }
        }
    }

    /**
     * 게임이 종료되었을 때 호출되는 메서드입니다.
     */
    override fun onGameOver() {
        super.onGameOver()
        
        // 게임 진행 중인 경우 강제 종료
        if (isGameActive) {
            isGameActive = false
            updateUIState(false)
            showMessage("해킹 작업이 중단되었습니다.")
        }
        
        // 드로어가 열려있으면 닫기
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        }
    }
    
    /**
     * 프래그먼트가 일시정지될 때 호출됩니다.
     */
    override fun onPause() {
        super.onPause()
        
        // 필요한 상태 저장
        saveCurrentLevel()
    }
    
    /**
     * 프래그먼트의 뷰가 소멸될 때 호출됩니다.
     */
    override fun onDestroyView() {
        // 드로어 리스너 제거
        if (::drawerLayout.isInitialized && drawerListener != null) {
            drawerLayout.removeDrawerListener(drawerListener!!)
            drawerListener = null
        }
        
        // 버튼 클릭 리스너 제거
        if (::startButton.isInitialized) {
            startButton.setOnClickListener(null)
        }
        
        if (::submitButton.isInitialized) {
            submitButton.setOnClickListener(null)
        }
        
        if (::toggleHistoryButton.isInitialized) {
            toggleHistoryButton.setOnClickListener(null)
        }
        
        if (::closeHistoryButton.isInitialized) {
            closeHistoryButton.setOnClickListener(null)
        }
        
        // 디지트 버튼 리스너 제거
        if (::digitButtons.isInitialized) {
            for (button in digitButtons) {
                button.setOnClickListener(null)
            }
        }
        
        if (::deleteButton.isInitialized) {
            deleteButton.setOnClickListener(null)
        }
        
        // 모든 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null)
        
        // SoundPool 리소스는 더 이상 직접 관리하지 않으므로 여기서 해제하지 않음
        // 대신 P20Application에서 관리됨
        
        // 부모 클래스의 onDestroyView 호출 (BaseFragment에서 나머지 리소스 정리)
        super.onDestroyView()
    }
    
    /**
     * 프래그먼트가 소멸될 때 호출됩니다.
     */
    override fun onDestroy() {
        super.onDestroy()
        
        // BroadcastReceiver 해제
        if (soundSettingsReceiver != null) {
            try {
                context?.let { ctx ->
                    // LocalBroadcastManager에서 해제
                    LocalBroadcastManager.getInstance(ctx).unregisterReceiver(soundSettingsReceiver!!)
                    // 전역 브로드캐스트에서도 해제
                    ctx.unregisterReceiver(soundSettingsReceiver!!)
                    android.util.Log.d("HackingAlba", "효과음 설정 변경 리시버 해제됨")
                }
            } catch (e: Exception) {
                android.util.Log.e("HackingAlba", "리시버 해제 오류: ${e.message}")
            } finally {
                soundSettingsReceiver = null
            }
        }
    }
} 