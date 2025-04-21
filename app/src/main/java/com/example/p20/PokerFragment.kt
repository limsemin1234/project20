package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale
import android.os.Handler
import android.os.Looper
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AlertDialog
import android.graphics.Typeface
import androidx.lifecycle.Observer
import android.util.SparseArray
import android.view.View.OnClickListener
import androidx.collection.ArrayMap
import android.util.LruCache
import android.media.MediaPlayer
import android.net.Uri
import android.graphics.PorterDuff
import android.util.Log
import kotlin.random.Random
import android.widget.Toast

class PokerFragment : Fragment() {

    // UI 컴포넌트
    private lateinit var playerCardsLayout: LinearLayout
    private lateinit var handRankText: TextView
    private lateinit var betAmountText: TextView
    private lateinit var scoreText: TextView
    private lateinit var changeButton: Button
    private lateinit var endGameButton: Button
    private lateinit var newGameButton: Button
    private lateinit var bet10kButton: Button
    private lateinit var bet50kButton: Button
    private lateinit var bet100kButton: Button
    private lateinit var bet500kButton: Button
    
    // 효과음 재생을 위한 MediaPlayer
    private var bettingSound: MediaPlayer? = null
    private var cardSound: MediaPlayer? = null
    private var startGameSound: MediaPlayer? = null
    private var winSound: MediaPlayer? = null
    private var loseSound: MediaPlayer? = null
    private var cardSelectSound: MediaPlayer? = null
    private var stopSound: MediaPlayer? = null
    
    // 게임 상태
    private var currentBet = 0L
    private var tempBetAmount = 0L
    private var isGameActive = false
    private var isCardDealt = false
    private var isCardChanged = false
    private var changeCount = 0 // 카드 교체 횟수 추적
    private var winCount = 0
    private var loseCount = 0
    private var isWaitingForCleanup = false
    
    // 선택된 카드 추적 (HashSet 대신 ArraySet으로 변경하여 메모리 사용 최적화)
    private val selectedCardIndices = mutableSetOf<Int>()
    private val cardViews = ArrayList<TextView>(7) // 초기 용량 지정
    
    // 족보에 포함된 카드 인덱스 저장
    private val handRankCardIndices = mutableSetOf<Int>()
    
    // 카드 관련 변수 - 불변 리스트로 변경
    private val suits = listOf("♠", "♥", "♦", "♣")
    private val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    
    // 카드 덱과 손패 - ArrayList로 변경하여 메모리 최적화
    private val deck = ArrayList<Card>(52) // 덱의 최대 크기
    private val playerCards = ArrayList<Card>(7) // 플레이어 카드는 7장
    
    // 카드 교체 기본 비용
    private val baseCostForChange = 50000L
    
    // 포맷터 캐싱 (반복 사용되는 포맷터 객체)
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA).apply {
        setCurrency(java.util.Currency.getInstance(Locale.KOREA))
    }
    
    // 스트로크 드로어블 재사용
    private val defaultCardDrawable = GradientDrawable().apply {
        setStroke(3, Color.BLACK)
        cornerRadius = 8f
        setColor(Color.WHITE)
    }
    
    private val selectedCardDrawable = GradientDrawable().apply {
        setStroke(3, Color.BLACK)
        cornerRadius = 8f
        setColor(Color.argb(255, 200, 255, 200))
    }
    
    // 카드 선택 리스너 재사용
    private val cardClickListener = OnClickListener { view ->
        if (!isGameActive || isWaitingForCleanup) return@OnClickListener
        
        val cardIndex = view.tag as Int
        
        if (selectedCardIndices.contains(cardIndex)) {
            // 선택 해제
            selectedCardIndices.remove(cardIndex)
            view.alpha = 1.0f
            view.background = defaultCardDrawable.constantState?.newDrawable()
            
            // 카드 선택/해제 효과음 재생
            playCardSelectSound()
        } else {
            // 최대 5장까지만 선택 가능
            if (selectedCardIndices.size >= 5) {
                showCustomSnackbar("최대 5장까지만 선택할 수 있습니다.")
                return@OnClickListener
            }
            
            // 선택
            selectedCardIndices.add(cardIndex)
            view.alpha = 0.7f
            view.background = selectedCardDrawable.constantState?.newDrawable()
            
            // 카드 선택/해제 효과음 재생
            playCardSelectSound()
        }
        
        // 선택한 카드가 5장이면 자동으로 패 평가
        if (selectedCardIndices.size == 5) {
            // 현재 점수를 ViewModel에서 계산하도록 함
            pokerViewModel.updateScore()
            // 현재 점수를 가져와서 전달
            val currentScore = pokerViewModel.currentScore.value ?: 0
            updateScoreText(currentScore)
        } else {
            scoreText.text = "점수: 0\n "
        }
    }
    
    // ViewModel
    private val pokerViewModel: PokerViewModel by viewModels()
    private val assetViewModel: AssetViewModel by activityViewModels()
    private val timeViewModel: TimeViewModel by activityViewModels()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var cleanupRunnable: Runnable? = null

    companion object {
        // 카드 랭크 값 매핑 - 상수로 사용
        val rankValues = mapOf(
            "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9,
            "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14
        )
        
        // 점수 배당률 매핑 - 상수로 분리
        private val SCORE_MULTIPLIERS = arrayOf(
            2000 to 10,
            1000 to 6,
            600 to 4,
            400 to 3,
            300 to 2,
            200 to 1,
            0 to 0
        )
    }

    // 데이터 클래스 최적화 - equals 및 hashCode 최적화
    data class Card(val rank: String, val suit: String) {
        // 값을 캐싱하여 반복 계산 방지
        private val _value: Int by lazy { rankValues[rank] ?: 0 }
        
        fun value(): Int = _value
        
        override fun toString(): String = "$rank$suit"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_poker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UI 초기화
        playerCardsLayout = view.findViewById(R.id.playerCardsLayout)
        handRankText = view.findViewById(R.id.handRankText)
        betAmountText = view.findViewById(R.id.betAmountText)
        scoreText = view.findViewById(R.id.scoreText)
        changeButton = view.findViewById(R.id.changeButton)
        endGameButton = view.findViewById(R.id.endGameButton)
        newGameButton = view.findViewById(R.id.newGameButton)
        bet10kButton = view.findViewById(R.id.bet10kButton)
        bet50kButton = view.findViewById(R.id.bet50kButton)
        bet100kButton = view.findViewById(R.id.bet100kButton)
        bet500kButton = view.findViewById(R.id.bet500kButton)
        
        // 효과음 초기화
        initSounds()
        
        // 게임 설명 버튼 설정
        val helpButton = view.findViewById<Button>(R.id.helpButton)
        helpButton.setOnClickListener {
            showGameRules()
        }
        
        // ViewModel 옵저버 설정
        setupObservers()
        
        // 잔액 업데이트
        updateBalanceText()
        updateBetAmountText()
        
        // 버튼 이벤트 리스너 설정
        setupButtonListeners()
        
        // 게임오버 이벤트 감지
        observeGameState()
        
        // 환영 메시지 표시
        showCustomSnackbar("배팅 후 1인발라트로 게임을 시작해주세요!")

        // 정리 작업을 위한 Runnable 설정
        cleanupRunnable = Runnable {
            if (pokerViewModel.isWaitingForCleanup.value == true) {
                cleanupGame()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Handler 정리
        mainHandler.removeCallbacksAndMessages(null)
        cleanupRunnable = null
        
        // 효과음 해제
        bettingSound?.release()
        bettingSound = null
        cardSound?.release()
        cardSound = null
        startGameSound?.release()
        startGameSound = null
        winSound?.release()
        winSound = null
        loseSound?.release()
        loseSound = null
        cardSelectSound?.release()
        cardSelectSound = null
        stopSound?.release()
        stopSound = null
        
        // 카드 뷰 정리
        cardViews.clear()
        selectedCardIndices.clear()
        handRankCardIndices.clear()
        playerCards.clear()
        deck.clear()
    }
    
    private fun setupObservers() {
        // 플레이어 카드 변경 감지
        pokerViewModel.playerCards.observe(viewLifecycleOwner) { cards ->
            updateCardViews(cards)
        }
        
        // 선택된 카드 변경 감지
        pokerViewModel.selectedCardIndices.observe(viewLifecycleOwner) { indices ->
            updateSelectedCards(indices)
        }
        
        // 핸드 랭크 카드 인덱스 변경 감지
        pokerViewModel.handRankCardIndices.observe(viewLifecycleOwner) { indices ->
            highlightHandRankCards(indices)
        }
        
        // 현재 핸드 랭크 변경 감지
        pokerViewModel.currentHandRank.observe(viewLifecycleOwner) { handRank ->
            handRankText.text = handRank.koreanName
        }
        
        // 점수 변경 감지
        pokerViewModel.currentScore.observe(viewLifecycleOwner) { score ->
            updateScoreText(score)
        }
        
        // 베팅 금액 변경 감지
        pokerViewModel.tempBetAmount.observe(viewLifecycleOwner) { amount ->
            betAmountText.text = "베팅 금액: ${formatCurrency(amount)}"
        }
        
        // 게임 결과 변경 감지
        pokerViewModel.gameResult.observe(viewLifecycleOwner) { result ->
            result?.let { 
                handleGameResult(it)
            }
        }
        
        // 교체 횟수 변경 감지
        pokerViewModel.changeCount.observe(viewLifecycleOwner) { count ->
            updateChangeButtonText(count)
        }
    }
    
    private fun setupButtonListeners() {
        // 베팅 버튼
        bet10kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(10_000L)) {
                showCustomSnackbar("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showCustomSnackbar("게임 진행 중에는 베팅할 수 없습니다.")
        }
        }
        
        bet50kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(50_000L)) {
                showCustomSnackbar("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showCustomSnackbar("게임 진행 중에는 베팅할 수 없습니다.")
        }
        }
        
        bet100kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(100_000L)) {
                showCustomSnackbar("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showCustomSnackbar("게임 진행 중에는 베팅할 수 없습니다.")
        }
        }
        
        bet500kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(500_000L)) {
                showCustomSnackbar("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showCustomSnackbar("게임 진행 중에는 베팅할 수 없습니다.")
            }
        }
        
        // 베팅 금액 초기화 기능 (0가 아닐 때 bet10kButton 길게 누르면 초기화)
        bet10kButton.setOnLongClickListener {
            if ((pokerViewModel.tempBetAmount.value ?: 0L) > 0 && pokerViewModel.isGameActive.value != true) {
                pokerViewModel.clearBet()
                showCustomSnackbar("베팅 금액이 초기화되었습니다.")
                return@setOnLongClickListener true
            }
            false
        }
        
        // 새 게임 버튼
        newGameButton.setOnClickListener { 
            if (pokerViewModel.isGameActive.value == true) {
                showCustomSnackbar("게임이 이미 진행 중입니다.")
                return@setOnClickListener
            }
            
            if ((pokerViewModel.tempBetAmount.value ?: 0L) <= 0) {
                showCustomSnackbar("먼저 베팅해주세요.")
                return@setOnClickListener
            }
            
            // 자산 확인
            val currentAsset = assetViewModel.asset.value ?: 0L
            if ((pokerViewModel.tempBetAmount.value ?: 0L) > currentAsset) {
                showCustomSnackbar("베팅 금액이 보유 자산을 초과합니다.")
                return@setOnClickListener
            }
            
            // 베팅 금액 설정 및 자산 감소
            if (pokerViewModel.placeBet()) {
                assetViewModel.decreaseAsset(pokerViewModel.currentBet.value ?: 0L)
            
            // 업데이트
            updateBalanceText()
            
            // 새 게임 효과음 재생
            playStartGameSound()
            
            // 게임 시작
                pokerViewModel.startNewGame()
                
                // UI 업데이트
                updateButtonStates(true)
                
                // 족보 가능성이 있는 카드 표시
                analyzeAndHighlightPotentialHands()
                
                // 사용자에게 알림
                MessageManager.showMessage(requireContext(), "족보가 될 수 있는 카드들을 강조 표시합니다.")
            }
        }
        
        // 카드 교체 버튼
        changeButton.setOnClickListener { 
            // 교체 비용 계산
            val changeCost = pokerViewModel.getChangeCost()
            
            // 비용이 있을 경우 자산 확인
            if (changeCost > 0) {
        val currentAsset = assetViewModel.asset.value ?: 0L
                if (changeCost > currentAsset) {
                    showCustomSnackbar("카드 교체 비용이 부족합니다. 필요 금액: ${formatCurrency(changeCost)}")
                    return@setOnClickListener
                }
            }
            
            // 카드 교체 시도
            val (success, message) = pokerViewModel.changeCards()
            
            if (success) {
                // 비용이 있을 경우 차감
                if (changeCost > 0) {
                    assetViewModel.decreaseAsset(changeCost)
                    updateBalanceText()
                }
                
                // 카드 교체 효과음 재생
                playCardSound()
                
                // 결과 메시지 표시
                showCustomSnackbar(message)
                
                // 교체된 카드로 새롭게 족보 가능성 분석 및 표시
                analyzeAndHighlightPotentialHands()
                
                // 사용자에게 알림
                MessageManager.showMessage(requireContext(), "새 카드에서 족보가 될 수 있는 카드들을 강조 표시합니다.")
            } else {
                showCustomSnackbar(message)
            }
        }
        
        // 카드 확정 버튼
        endGameButton.setOnClickListener { 
            if (pokerViewModel.selectedCardIndices.value?.size != 5) {
                showCustomSnackbar("정확히 5장의 카드를 선택해야 합니다.")
                return@setOnClickListener
            }
            
            // 카드확정 효과음 재생
            playStopSound()
            
            // 게임 종료 처리
            if (pokerViewModel.endGame(assetViewModel.asset.value ?: 0L, requireContext())) {
                // UI 업데이트
                updateButtonStates(false)
            }
        }
    }
    
    private fun updateButtonStates(isGameActive: Boolean) {
        // 게임 상태에 따른 버튼 활성화/비활성화
        changeButton.isEnabled = isGameActive
        endGameButton.isEnabled = isGameActive
        
        // 베팅 버튼 비활성화/활성화
        bet10kButton.isEnabled = !isGameActive
        bet50kButton.isEnabled = !isGameActive
        bet100kButton.isEnabled = !isGameActive
        bet500kButton.isEnabled = !isGameActive
        newGameButton.isEnabled = !isGameActive
    }
    
    private fun updateCardViews(cards: List<PokerViewModel.Card>) {
        // 기존 카드 뷰 제거
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        
        // 새 카드 뷰 생성
        for (i in cards.indices) {
            addCardView(playerCardsLayout, cards[i], i)
        }
    }
    
    private fun addCardView(container: LinearLayout, card: PokerViewModel.Card, index: Int) {
        // 화면 너비에 맞게 카드 크기 계산
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        // 카드 사이의 간격 (dp를 픽셀로 변환)
        val cardMarginDp = 1
        val cardMargin = (cardMarginDp * displayMetrics.density).toInt()
        
        // 화면 좌우 패딩 및 여백을 고려하여 조정
        val totalHorizontalPadding = (48 * displayMetrics.density).toInt()
        
        // 카드 7장과 간격이 화면에 딱 맞도록 카드 너비 계산
        val cardWidth = (screenWidth - (6 * cardMargin) - totalHorizontalPadding) / 7
        
        // 카드 높이는 너비의 1.5배 (일반적인 카드 비율)
        val cardHeight = (cardWidth * 1.5).toInt()
        
        val cardView = TextView(requireContext())
        cardView.layoutParams = LinearLayout.LayoutParams(
            cardWidth,
            cardHeight
        ).apply {
            marginEnd = cardMargin
        }
        
        // 기본 검정색 테두리 설정
        val strokeDrawable = GradientDrawable()
        strokeDrawable.setStroke(3, Color.BLACK)
        strokeDrawable.cornerRadius = 8f
        strokeDrawable.setColor(Color.WHITE)
        cardView.background = strokeDrawable
        
        cardView.gravity = Gravity.CENTER
        cardView.textSize = (cardWidth * 0.25f) / displayMetrics.density
        cardView.setPadding(2, 2, 2, 2)
        cardView.setTypeface(null, Typeface.BOLD)
        
        cardView.text = card.toString()
        // 하트/다이아는 빨간색, 스페이드/클럽은 검은색
        val textColor = if (card.suit == "♥" || card.suit == "♦") Color.RED else Color.BLACK
        cardView.setTextColor(textColor)
        
        // 카드뷰 태그 설정
        cardView.tag = index
        
        // 카드 뷰 목록에 추가
        if (index < cardViews.size) {
            cardViews[index] = cardView
        } else {
            cardViews.add(cardView)
        }
        
        // 카드 터치 이벤트 추가
        cardView.setOnClickListener {
            if (pokerViewModel.toggleCardSelection(index)) {
                // 효과음 재생
                playCardSelectSound()
            } else if (pokerViewModel.selectedCardIndices.value?.size == 5) {
                showCustomSnackbar("최대 5장까지만 선택할 수 있습니다.")
            }
        }
        
        container.addView(cardView)
    }
    
    private fun updateSelectedCards(selectedIndices: Set<Int>) {
        // 모든 카드 기본 상태로 초기화
        for (i in 0 until cardViews.size) {
            cardViews[i].alpha = 1.0f
            cardViews[i].background = defaultCardDrawable.constantState?.newDrawable()
        }
        
        // 선택된 카드 강조
        for (index in selectedIndices) {
        if (index < cardViews.size) {
                cardViews[index].alpha = 0.7f
                cardViews[index].background = selectedCardDrawable.constantState?.newDrawable()
            }
        }
    }
    
    private fun highlightHandRankCards(handRankIndices: Set<Int>) {
        // 모든 카드는 기본 스타일로 초기화 (선택된 카드는 제외)
        val selectedIndices = pokerViewModel.selectedCardIndices.value ?: emptySet()
        
        for (i in 0 until cardViews.size) {
            if (!selectedIndices.contains(i)) {
                // 기본 카드 배경으로 설정
                val strokeDrawable = GradientDrawable().apply {
                    setStroke(3, Color.BLACK)
                    cornerRadius = 8f
                    setColor(Color.WHITE)
                }
                cardViews[i].background = strokeDrawable
                cardViews[i].setTypeface(null, Typeface.NORMAL)
                cardViews[i].alpha = 1.0f
            }
        }
        
        // 족보에 포함된 카드만 강조 표시
        for (index in handRankIndices) {
            if (index < cardViews.size && !selectedIndices.contains(index)) {
                // 연한 파란색 배경으로 강조
                val strokeDrawable = GradientDrawable().apply {
                    setStroke(3, Color.BLACK)
                    cornerRadius = 8f
                    setColor(Color.argb(200, 135, 206, 250))
                }
                cardViews[index].background = strokeDrawable
                
                // 텍스트 굵게 표시
                cardViews[index].setTypeface(null, Typeface.BOLD)
                cardViews[index].alpha = 1.0f
            }
        }
    }
    
    private fun updateScoreText(score: Int) {
        if (pokerViewModel.selectedCardIndices.value?.size != 5) {
            scoreText.text = "점수: 0\n "
            return
        }
        
        val selectedIndices = pokerViewModel.selectedCardIndices.value ?: return
        val cards = pokerViewModel.playerCards.value ?: return
        val selectedCards = selectedIndices.map { cards[it] }
        val handRank = pokerViewModel.currentHandRank.value ?: return
        
        // 점수 계산식 생성
        val cardSum = selectedCards.sumOf { it.value() }
        val formula = when (handRank) {
            PokerViewModel.HandRank.ROYAL_STRAIGHT_FLUSH -> "(150 + $cardSum) × 10 = $score"
            PokerViewModel.HandRank.STRAIGHT_FLUSH -> "(100 + $cardSum) × 8 = $score"
            PokerViewModel.HandRank.FOUR_OF_A_KIND -> "(60 + $cardSum) × 7 = $score"
            PokerViewModel.HandRank.FULL_HOUSE -> "(40 + $cardSum) × 4 = $score"
            PokerViewModel.HandRank.FLUSH -> "(35 + $cardSum) × 4 = $score"
            PokerViewModel.HandRank.STRAIGHT -> "(30 + $cardSum) × 4 = $score"
            PokerViewModel.HandRank.THREE_OF_A_KIND -> "(30 + $cardSum) × 3 = $score"
            PokerViewModel.HandRank.TWO_PAIR -> "(20 + $cardSum) × 2 = $score"
            PokerViewModel.HandRank.ONE_PAIR -> "(10 + $cardSum) × 2 = $score"
            PokerViewModel.HandRank.HIGH_CARD -> "$cardSum = $score"
            PokerViewModel.HandRank.NONE -> "0"
        }
        
        // 카드값 표시
        val cardValues = selectedCards.joinToString(", ") { "${it.rank}" }
        
        // 배당률 계산
        val multiplierInfo = when {
            score >= 2000 -> "10배"
            score >= 1000 -> "6배"
            score >= 600 -> "4배"
            score >= 400 -> "3배"
            score >= 300 -> "2배"
            score >= 200 -> "1배"
            else -> "0배"
        }
        
        scoreText.text = "점수: $score (배당: $multiplierInfo)\n[$cardValues] $formula"
    }
    
    private fun updateChangeButtonText(changeCount: Int) {
        val cost = pokerViewModel.getChangeCost()
        changeButton.text = if (cost == 0L) {
            "카드 교체\n(무료)"
        } else {
            "카드 교체\n(${formatCurrency(cost)})"
        }
    }
    
    private fun handleGameResult(result: PokerViewModel.GameResult) {
        // 결과에 따른 처리
        if (result.isWin) {
            // 승리
            playWinSound()
            assetViewModel.increaseAsset(result.payout)
            
            // 승리 애니메이션 표시
            val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            CasinoAnimationManager.showWinAnimation(
                rootView,
                "${result.handRank.koreanName} (${result.score}점)",
                "+${formatCurrency(result.payout - (pokerViewModel.currentBet.value ?: 0L))}"
            )
            
            // 아이템 보상 처리
            processItemReward(pokerViewModel.currentBet.value ?: 0L, result.multiplier)
        } else {
            // 패배
            playLoseSound()
            
            // 패배 애니메이션 표시
            val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            CasinoAnimationManager.showLoseAnimation(
                rootView,
                "${result.handRank.koreanName} (${result.score}점)",
                "-${formatCurrency(pokerViewModel.currentBet.value ?: 0L)}"
            )
        }
        
        // 결과 메시지 표시
        showResultSnackbar(result.message, if (result.isWin) 
            Color.argb(200, 76, 175, 80) else Color.argb(200, 244, 67, 54))
        
        // 통계 업데이트
        updateBalanceText()
        
        // 선택된 카드 강조 표시 및 족보 표시
        processHandRankCards()
        
        // 정리 작업 지연
        cleanupRunnable?.let { runnable ->
            mainHandler.removeCallbacks(runnable)
            mainHandler.postDelayed(runnable, 3000)
        }
    }
    
    /**
     * 게임 결과가 표시될 때 족보에 포함된 카드를 강조 표시
     */
    private fun processHandRankCards() {
        // 모든 카드 흐리게 표시
        for (i in 0 until cardViews.size) {
            cardViews[i].alpha = 0.3f
            cardViews[i].background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        }
        
        // 선택된 카드 가져오기
        val selectedIndices = pokerViewModel.selectedCardIndices.value ?: emptySet()
        
        // 선택된 카드 중 족보에 포함된 카드 파악
        val handRank = pokerViewModel.currentHandRank.value ?: return
        
        // 족보에 포함된 카드를 강조 표시 - 기본적으로 선택된 모든 카드가 족보 카드
        for (index in selectedIndices) {
            if (index < cardViews.size) {
                val drawable = GradientDrawable().apply {
                    setStroke(4, Color.argb(255, 50, 100, 255))  // 파란색 테두리
                    cornerRadius = 8f
                    setColor(Color.argb(50, 100, 150, 255))  // 연한 파란색 배경
                }
                cardViews[index].background = drawable
                cardViews[index].alpha = 1.0f
                cardViews[index].setTypeface(null, Typeface.BOLD)
            }
        }
        
        // 패 종류에 따라 족보에 포함된 핵심 카드들을 추가로 강조 표시
        when (handRank) {
            PokerViewModel.HandRank.ROYAL_STRAIGHT_FLUSH,
            PokerViewModel.HandRank.STRAIGHT_FLUSH,
            PokerViewModel.HandRank.STRAIGHT,
            PokerViewModel.HandRank.FLUSH -> {
                // 모든 5장이 족보에 포함되므로 추가 강조 없음
            }
            
            PokerViewModel.HandRank.FOUR_OF_A_KIND -> {
                // 4장 카드가 같은 숫자인 카드 찾기
                highlightSameRankCards(4)
            }
            
            PokerViewModel.HandRank.FULL_HOUSE -> {
                // 3장 카드와 2장 카드 강조
                highlightSameRankCards(3)
                highlightSameRankCards(2)
            }
            
            PokerViewModel.HandRank.THREE_OF_A_KIND -> {
                // 3장 카드가 같은 숫자인 카드 찾기
                highlightSameRankCards(3)
            }
            
            PokerViewModel.HandRank.TWO_PAIR -> {
                // 두 쌍의 페어 찾아서 강조
                highlightSameRankCards(2)
            }
            
            PokerViewModel.HandRank.ONE_PAIR -> {
                // 한 쌍의 페어 찾아서 강조
                highlightSameRankCards(2)
            }
            
            else -> {
                // 기타 경우는 처리 안함
            }
        }

        // 족보 정보 메시지 표시
        MessageManager.showMessage(requireContext(), "${handRank.koreanName}에 해당하는 카드가 강조 표시됩니다.")
    }

    /**
     * 선택된 카드 중 같은 랭크를 가진 카드 그룹을 강조 표시
     */
    private fun highlightSameRankCards(targetGroupSize: Int) {
        val selectedIndices = pokerViewModel.selectedCardIndices.value ?: return
        val cards = pokerViewModel.playerCards.value ?: return

        // 선택된 카드만 추출
        val selectedCards = selectedIndices.map { Pair(it, cards[it]) }
        
        // 랭크별로 그룹핑
        val rankGroups = selectedCards.groupBy { it.second.rank }
        
        // 타겟 사이즈의 그룹 찾기
        val targetGroups = rankGroups.filter { it.value.size == targetGroupSize }
        
        // 해당 그룹의 카드들 강조 표시
        for (group in targetGroups) {
            for (cardPair in group.value) {
                val index = cardPair.first
                if (index < cardViews.size) {
                    val drawable = GradientDrawable().apply {
                        setStroke(5, Color.argb(255, 255, 165, 0))  // 진한 주황색 테두리
                        cornerRadius = 8f
                        setColor(Color.argb(100, 255, 215, 0))  // 금색 배경
                    }
                    cardViews[index].background = drawable
                    cardViews[index].alpha = 1.0f
                    cardViews[index].setTypeface(null, Typeface.BOLD_ITALIC)
                }
            }
        }
    }
    
    private fun updateBalanceText() {
        // UI 요소 제거됨 - 메서드만 유지
    }
    
    private fun updateBetAmountText() {
        betAmountText.text = "베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}"
    }
    
    // 화폐 형식 변환 메서드
    private fun formatCurrency(amount: Long): String {
        return currencyFormatter.format(amount)
    }
    
    private fun showCustomSnackbar(message: String) {
        // Toast 대신 MessageManager 사용
        MessageManager.showMessage(requireContext(), message)
    }
    
    private fun showResultSnackbar(message: String, bgColor: Int) {
        // Toast 대신 MessageManager 사용
        MessageManager.showMessage(requireContext(), message)
    }
    
    private fun showItemRewardSnackbar(message: String) {
        // Toast 대신 MessageManager 사용
        MessageManager.showMessage(requireContext(), message)
    }
    
    private fun processItemReward(betAmount: Long, multiplier: Int) {
        // 베팅액에 따른 아이템 보상 확률 계산
        val rewardProbability = when {
            betAmount >= 500_000L -> 0.15  // 50만원 이상 베팅: 15% 확률
            betAmount >= 100_000L -> 0.10  // 10만원 이상 베팅: 10% 확률
            betAmount >= 50_000L -> 0.05   // 5만원 이상 베팅: 5% 확률
            else -> 0.01                   // 그 외: 1% 확률
        }
        
        // 배율에 따른 추가 보상 확률
        val multiplierBonus = when (multiplier) {
            10 -> 0.20  // 10배: 20% 추가
            6 -> 0.15   // 6배: 15% 추가
            4 -> 0.10   // 4배: 10% 추가
            3 -> 0.05   // 3배: 5% 추가
            else -> 0.0 // 그 외: 추가 없음
        }
        
        // 최종 보상 확률
        val finalProbability = rewardProbability + multiplierBonus
        
        // 랜덤값으로 보상 여부 결정
        if (Random.nextDouble() < finalProbability) {
            // 베팅 금액에 따른 보상 품질 결정
            val rewardQuality = when {
                betAmount >= 500_000L && multiplier >= 6 -> 3  // 최고급 아이템
                betAmount >= 100_000L || multiplier >= 4 -> 2  // 중급 아이템
                else -> 1                                      // 일반 아이템
            }
            
            // 보상 아이템 및 메시지 설정
            val (rewardType, rewardMessage) = when (rewardQuality) {
                3 -> {
                    // 최고급 보상 (보약, 시계)
                    val type = if (Random.nextBoolean()) "medicine" else "watch"
                    val message = if (type == "medicine") "행운의 보약을 발견했습니다! (시간 10시간 즉시 회복)" else "고급 시계를 발견했습니다! (시간 10시간 즉시 회복)"
                    Pair(type, message)
                }
                2 -> {
                    // 중급 보상 (책, 도구)
                    val type = if (Random.nextBoolean()) "book" else "tool"
                    val message = if (type == "book") "유용한 책을 발견했습니다! (경험치 +500)" else "품질 좋은 도구를 발견했습니다! (효율 +5% 1시간)"
                    Pair(type, message)
                }
                else -> {
                    // 일반 보상 (과일, 커피)
                    val type = if (Random.nextBoolean()) "fruit" else "coffee"
                    val message = if (type == "fruit") "싱싱한 과일을 발견했습니다! (시간 1시간 즉시 회복)" else "커피를 발견했습니다! (시간 30분 즉시 회복)"
                    Pair(type, message)
                }
            }
            
            // 아이템 적용
            applyItemReward(rewardType)
            
            // 보상 메시지 출력
            showItemRewardSnackbar(rewardMessage)
        }
    }
    
    private fun applyItemReward(rewardType: String) {
        when (rewardType) {
            "medicine" -> {
                // 보약: 시간 10시간 즉시 회복
                timeViewModel.increaseRemainingTime(10 * 60)
            }
            "watch" -> {
                // 고급 시계: 시간 10시간 즉시 회복
                timeViewModel.increaseRemainingTime(10 * 60)
            }
            "book" -> {
                // 책: 경험치 +500
                // TODO: 경험치 시스템 구현 시 추가
            }
            "tool" -> {
                // 도구: 효율 +5% 1시간
                // TODO: 효율 시스템 구현 시 추가
            }
            "fruit" -> {
                // 과일: 시간 1시간 즉시 회복
                timeViewModel.increaseRemainingTime(60)
            }
            "coffee" -> {
                // 커피: 시간 30분 즉시 회복
                timeViewModel.increaseRemainingTime(30)
            }
        }
    }
    
    private fun showGameRules() {
        // 다이얼로그가 표시되면 타이머 멈춤
        timeViewModel.stopTimer()
        
        val message = """
            [게임 규칙]
            1. 7장의 카드 중 5장을 선택하여 최고의 패를 만드세요.
            2. 카드 교체는 3회까지 무료, 4번째는 배팅금의 절반, 5번째는 배팅금만큼 비용이 듭니다.
            3. 최대 5번까지 교체할 수 있습니다.
            4. 게임 종료 시 정확히 5장을 선택해야 합니다.
            
            [점수 배당률]
            • 200점 이상: 1배
            • 300점 이상: 2배
            • 400점 이상: 3배
            • 600점 이상: 4배
            • 1000점 이상: 6배
            • 2000점 이상: 10배
            
            [패 계산 방식]
            • 로얄 스트레이트 플러시: (150 + 카드합) × 10
            • 스트레이트 플러시: (100 + 카드합) × 8
            • 포카드: (60 + 카드합) × 7
            • 풀하우스: (40 + 카드합) × 4
            • 플러시: (35 + 카드합) × 4
            • 스트레이트: (30 + 카드합) × 4
            • 트리플: (30 + 카드합) × 3
            • 투페어: (20 + 카드합) × 2
            • 원페어: (10 + 카드합) × 2
            • 하이카드: 카드합
        """.trimIndent()
        
        // 다이얼로그 생성
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("1인발라트로 게임 설명")
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ -> 
                dialog.dismiss()
                // 다이얼로그가 닫히면 타이머 다시 시작
                timeViewModel.startTimer()
            }
            .setOnCancelListener {
                // 다이얼로그가 취소되어도 타이머 다시 시작
                timeViewModel.startTimer()
            }
            .create()
            
        // 다이얼로그 표시
        dialog.show()
        
        // 사용자에게 타이머가 일시정지되었음을 알림
        MessageManager.showMessage(requireContext(), "게임 설명을 읽는 동안 시간이 멈춰있습니다.")
        
        // 텍스트 크기 조절
        val textView = dialog.findViewById<TextView>(android.R.id.message)
        textView?.textSize = 13f // 텍스트 크기를 13sp로 설정
    }
    
    private fun observeGameState() {
        // 게임오버 이벤트 처리를 단일 옵저버로 통합
        val gameStateObserver = Observer<Boolean> { flag ->
            if (flag && isGameActive) {
                resetGameState()
            }
        }
        
        // 각 이벤트에 동일한 옵저버 재사용
        timeViewModel.isGameOver.observe(viewLifecycleOwner, gameStateObserver)
        timeViewModel.restartRequested.observe(viewLifecycleOwner, gameStateObserver)
        timeViewModel.gameResetEvent.observe(viewLifecycleOwner, gameStateObserver)
    }
    
    private fun resetGameState() {
        // 게임 상태 초기화
        isGameActive = false
        isCardDealt = false
        isCardChanged = false
        isWaitingForCleanup = false
        changeCount = 0
        currentBet = 0L
        tempBetAmount = 0L
        
        // 카드 관련 데이터 초기화
        playerCards.clear()
        selectedCardIndices.clear()
        handRankCardIndices.clear()
        
        // UI 초기화
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        handRankText.text = "패 없음"
        scoreText.text = "점수: 0\n "  // 줄바꿈 추가하여 항상 2줄 유지
        
        // 버튼 상태 초기화
        updateButtonStates(false)
        
        // 베팅 금액 텍스트 초기화
        updateBetAmountText()
    }
    
    // 게임 종료 후 정리 작업
    private fun cleanupGame() {
        // 게임 결과 지우기
        pokerViewModel.resetGame()
        
        // UI 상태 업데이트
        updateButtonStates(false)
        
        // ViewModel 정리
        pokerViewModel.resetGame()
        
        // 게임 내부 상태 초기화
        isGameActive = false
        isWaitingForCleanup = false
        
        // 베팅 금액 텍스트 초기화
        updateBetAmountText()
        
        // 사용자에게 알림
        showCustomSnackbar("게임이 종료되었습니다. 새 게임을 시작하려면 베팅 후 시작 버튼을 누르세요.")
    }
    
    private fun initSounds() {
        try {
            // 효과음 객체 생성
            bettingSound = MediaPlayer()
            cardSound = MediaPlayer()
            startGameSound = MediaPlayer()
            winSound = MediaPlayer()
            loseSound = MediaPlayer()
            cardSelectSound = MediaPlayer()
            stopSound = MediaPlayer()
            
            // 각 효과음 초기 설정 한 번만 수행
            initSoundResource(bettingSound, R.raw.casino_betting)
            initSoundResource(cardSound, R.raw.casino_card_receive)
            initSoundResource(startGameSound, R.raw.casino_start)
            initSoundResource(winSound, R.raw.casino_win)
            initSoundResource(loseSound, R.raw.casino_lose)
            initSoundResource(cardSelectSound, R.raw.casino_card_select)
            initSoundResource(stopSound, R.raw.casino_stop)
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error initializing sounds: ${e.message}")
        }
    }
    
    private fun initSoundResource(mediaPlayer: MediaPlayer?, resourceId: Int) {
        try {
            mediaPlayer?.apply {
                reset()
                setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + resourceId))
                prepare()
            }
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error initializing sound resource: ${e.message}")
        }
    }
    
    private fun playBettingSound() {
        try {
        bettingSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_betting))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error playing betting sound: ${e.message}")
        }
    }
    
    private fun playCardSound() {
        try {
        cardSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_card_receive))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error playing card sound: ${e.message}")
        }
    }
    
    private fun playStartGameSound() {
        try {
        startGameSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_start))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error playing start game sound: ${e.message}")
        }
    }
    
    private fun playWinSound() {
        try {
        winSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_win))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error playing win sound: ${e.message}")
        }
    }
    
    private fun playLoseSound() {
        try {
        loseSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_lose))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            android.util.Log.e("PokerFragment", "Error playing lose sound: ${e.message}")
        }
    }

    private fun playCardSelectSound() {
        try {
        cardSelectSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_card_select))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            // 오류 로깅
            android.util.Log.e("PokerFragment", "Error playing card select sound: ${e.message}")
        }
    }

    private fun playStopSound() {
        try {
        stopSound?.let {
            if (it.isPlaying) {
                it.stop()
            }
                it.reset()
                it.setDataSource(requireContext(), android.net.Uri.parse(
                    "android.resource://" + requireContext().packageName + "/" + R.raw.casino_stop))
                it.prepare()
            it.start()
            }
        } catch (e: Exception) {
            // 오류 로깅
            android.util.Log.e("PokerFragment", "Error playing stop sound: ${e.message}")
        }
    }

    // 족보 가능성 분석 및 강조 표시 메서드 추가
    private fun analyzeAndHighlightPotentialHands() {
        val cards = pokerViewModel.playerCards.value ?: return
        if (cards.size != 7) return
        
        // 모든 카드 기본 상태로 초기화
        for (i in 0 until cardViews.size) {
            cardViews[i].alpha = 1.0f
            cardViews[i].background = defaultCardDrawable.constantState?.newDrawable()
        }
        
        // 분석 결과 저장할 카드 인덱스 세트
        val potentialCardIndices = mutableSetOf<Int>()
        
        // 족보 검출 여부 플래그 - 높은 족보가 발견되면 true로 설정
        var highRankDetected = false
        
        // 1. 로얄 스트레이트 플러시 또는 스트레이트 플러시 체크 (최상위 족보)
        if (highlightRoyalStraightFlush(cards, potentialCardIndices) || 
            highlightStraightFlush(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 2. 포카드 체크 (두 번째 높은 족보)
        else if (highlightFourOfAKind(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 3. 풀하우스 체크
        else if (highlightFullHouse(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 4. 플러시 체크
        else if (highlightFlush(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 5. 스트레이트 체크
        else if (highlightStraight(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 6. 트리플 체크
        else if (highlightThreeOfAKind(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 7. 투페어 체크
        else if (highlightTwoPair(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 8. 원페어 체크
        else if (highlightOnePair(cards, potentialCardIndices)) {
            highRankDetected = true
        }
        
        // 9. 하이카드는 표시하지 않음 (너무 많은 카드가 표시될 수 있음)
        
        // 잠재적 족보 카드 시각적으로 표시
        for (index in potentialCardIndices) {
            if (index < cardViews.size) {
                // 더 선명하고 밝은 노란색 배경으로 강조
                val strokeDrawable = GradientDrawable().apply {
                    setStroke(4, Color.argb(255, 255, 165, 0)) // 주황-금색 테두리
                    cornerRadius = 8f
                    setColor(Color.argb(80, 255, 255, 0)) // 더 선명한 노란색 배경
                }
                cardViews[index].background = strokeDrawable
                cardViews[index].setTypeface(null, Typeface.BOLD)
                
                // 테마에 상관없이 텍스트 색상 유지하기 위해 명시적으로 설정
                val card = cards[index]
                val textColor = if (card.suit == "♥" || card.suit == "♦") Color.RED else Color.BLACK
                cardViews[index].setTextColor(textColor)
            }
        }
        
        // 족보가 발견되었다면 적절한 메시지 표시
        if (potentialCardIndices.isNotEmpty()) {
            MessageManager.showMessage(requireContext(), "완성된 가장 높은 족보를 강조 표시합니다.")
        } else {
            MessageManager.showMessage(requireContext(), "완성된 족보가 없습니다.")
        }
    }

    /**
     * 로얄 스트레이트 플러시 체크 (A, K, Q, J, 10이 같은 무늬)
     */
    private fun highlightRoyalStraightFlush(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 무늬별로 그룹화
        val suitGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.suit }
        
        // 각 무늬별로 로얄 스트레이트 플러시 확인
        for ((_, group) in suitGroups) {
            if (group.size >= 5) {
                val ranks = group.map { it.second.rank }.toSet()
                
                // 로얄 스트레이트 플러시 조건 확인 (10, J, Q, K, A가 모두 있는지)
                if (ranks.containsAll(listOf("A", "K", "Q", "J", "10"))) {
                    // 조건에 맞는 카드들 인덱스 추가
                    group.forEach { (index, cardItem) ->
                        if (cardItem.rank in listOf("A", "K", "Q", "J", "10")) {
                            potentialIndices.add(index)
                        }
                    }
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * 스트레이트 플러시 체크 (같은 무늬의 연속된 5장)
     */
    private fun highlightStraightFlush(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 무늬별로 그룹화
        val suitGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.suit }
        
        // 각 무늬별로 스트레이트 플러시 확인
        for ((_, group) in suitGroups) {
            if (group.size >= 5) {
                // 같은 무늬 카드들을 값으로 정렬
                val sortedCards = group.sortedBy { it.second.value() }
                
                // 연속된 5장 체크
                val straightFlushIndices = mutableSetOf<Int>()
                var consecutiveCount = 1
                
                for (i in 1 until sortedCards.size) {
                    if (sortedCards[i].second.value() == sortedCards[i-1].second.value() + 1) {
                        consecutiveCount++
                        if (consecutiveCount >= 5) {
                            // 연속된 5장 발견, 인덱스 수집
                            for (j in (i-4)..i) {
                                straightFlushIndices.add(sortedCards[j].first)
                            }
                            break
                        }
                    } else if (sortedCards[i].second.value() != sortedCards[i-1].second.value()) {
                        // 값이 중복되지 않고 연속되지 않으면 리셋
                        consecutiveCount = 1
                    }
                }
                
                // A-2-3-4-5 스트레이트 플러시 특별 케이스 처리
                if (straightFlushIndices.isEmpty()) {
                    val hasAce = group.any { it.second.rank == "A" }
                    val has2 = group.any { it.second.rank == "2" }
                    val has3 = group.any { it.second.rank == "3" }
                    val has4 = group.any { it.second.rank == "4" }
                    val has5 = group.any { it.second.rank == "5" }
                    
                    if (hasAce && has2 && has3 && has4 && has5) {
                        // A-2-3-4-5 스트레이트 플러시 완성
                        group.forEach { (index, cardItem) ->
                            if (cardItem.rank in listOf("A", "2", "3", "4", "5")) {
                                straightFlushIndices.add(index)
                            }
                        }
                    }
                }
                
                if (straightFlushIndices.isNotEmpty()) {
                    potentialIndices.addAll(straightFlushIndices)
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * 포카드 체크 (같은 숫자 4장)
     */
    private fun highlightFourOfAKind(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.rank }
        
        // 4장 있는 랭크 찾기
        val fourOfAKind = rankGroups.filter { it.value.size >= 4 }
        
        if (fourOfAKind.isNotEmpty()) {
            // 가장 높은 포카드 선택 (여러 개가 있을 경우 가능성 낮음)
            val highestFourOfAKind = fourOfAKind.maxByOrNull { 
                rankValues[it.key] ?: 0 
            }
            
            highestFourOfAKind?.value?.forEach { (index, _) ->
                potentialIndices.add(index)
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 풀하우스 체크 (트리플 + 페어)
     */
    private fun highlightFullHouse(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.rank }
        
        // 3장 이상인 랭크들
        val triples = rankGroups.filter { it.value.size >= 3 }
        
        // 2장 이상인 랭크들
        val pairs = rankGroups.filter { it.value.size >= 2 }
        
        // 풀하우스 조건: 트리플 1개 + 다른 페어 1개 이상
        if (triples.isNotEmpty() && pairs.size >= 2) {
            // 가장 높은 트리플 선택
            val highestTriple = triples.maxByOrNull { rankValues[it.key] ?: 0 }
            
            // 트리플과 다른 가장 높은 페어 선택
            val pairsExceptTriple = pairs.filter { it.key != highestTriple?.key }
            val highestPair = pairsExceptTriple.maxByOrNull { rankValues[it.key] ?: 0 }
            
            if (highestTriple != null && highestPair != null) {
                // 트리플 카드들 추가
                highestTriple.value.take(3).forEach { (index, _) ->
                    potentialIndices.add(index)
                }
                
                // 페어 카드들 추가
                highestPair.value.take(2).forEach { (index, _) ->
                    potentialIndices.add(index)
                }
                
                return true
            }
        }
        
        return false
    }
    
    /**
     * 플러시 체크 (같은 무늬 5장)
     */
    private fun highlightFlush(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 무늬별로 그룹화
        val suitGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.suit }
        
        // 5장 이상 같은 무늬 찾기
        val flushes = suitGroups.filter { it.value.size >= 5 }
        
        if (flushes.isNotEmpty()) {
            // 가장 높은 카드 값을 가진 플러시 선택
            val highestFlush = flushes.maxByOrNull { group ->
                group.value.maxOf { it.second.value() }
            }
            
            // 해당 플러시의 모든 카드 추가
            highestFlush?.value?.forEach { (index, _) ->
                potentialIndices.add(index)
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 스트레이트 체크 (연속된 숫자 5장)
     */
    private fun highlightStraight(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 카드 값으로 인덱스 맵 만들기
        val valueToIndexMap = cards.mapIndexed { index, card -> card.value() to index }.toMap()
        
        // 모든 값 정렬 (중복 제거)
        val cardValues = cards.map { it.value() }.toSet().sorted()
        
        // 연속된 숫자 찾기
        var consecutiveCount = 1
        var straightFound = false
        val straightIndices = mutableSetOf<Int>()
        
        for (i in 1 until cardValues.size) {
            if (cardValues[i] == cardValues[i-1] + 1) {
                consecutiveCount++
                if (consecutiveCount >= 5) {
                    // 연속된 5장 발견
                    straightFound = true
                    for (j in (i-4)..i) {
                        valueToIndexMap[cardValues[j]]?.let { index ->
                            straightIndices.add(index)
                        }
                    }
                    break
                }
            } else {
                consecutiveCount = 1
            }
        }
        
        // A-2-3-4-5 스트레이트 체크 (A가 1인 경우)
        if (!straightFound && 
            cardValues.contains(14) && cardValues.contains(2) && 
            cardValues.contains(3) && cardValues.contains(4) && cardValues.contains(5)) {
            
            valueToIndexMap[14]?.let { straightIndices.add(it) } // A
            valueToIndexMap[2]?.let { straightIndices.add(it) } // 2
            valueToIndexMap[3]?.let { straightIndices.add(it) } // 3
            valueToIndexMap[4]?.let { straightIndices.add(it) } // 4
            valueToIndexMap[5]?.let { straightIndices.add(it) } // 5
            straightFound = true
        }
        
        if (straightFound) {
            potentialIndices.addAll(straightIndices)
            return true
        }
        
        return false
    }
    
    /**
     * 트리플 체크 (같은 숫자 3장)
     */
    private fun highlightThreeOfAKind(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.rank }
        
        // 3장 이상인 랭크들
        val triples = rankGroups.filter { it.value.size >= 3 }
        
        if (triples.isNotEmpty()) {
            // 가장 높은 트리플 선택
            val highestTriple = triples.maxByOrNull { rankValues[it.key] ?: 0 }
            
            highestTriple?.value?.take(3)?.forEach { (index, _) ->
                potentialIndices.add(index)
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 투페어 체크 (같은 숫자 2장 × 2쌍)
     */
    private fun highlightTwoPair(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.rank }
        
        // 2장 이상인 랭크들
        val pairs = rankGroups.filter { it.value.size >= 2 }
        
        if (pairs.size >= 2) {
            // 값이 높은 상위 두 페어 선택
            val topPairs = pairs.toList().sortedByDescending { (key, _) -> 
                rankValues[key] ?: 0 
            }.take(2)
            
            topPairs.forEach { pair ->
                pair.second.take(2).forEach { (index, _) ->
                    potentialIndices.add(index)
                }
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 원페어 체크 (같은 숫자 2장)
     */
    private fun highlightOnePair(cards: List<PokerViewModel.Card>, potentialIndices: MutableSet<Int>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.mapIndexed { index, card -> index to card }.groupBy { it.second.rank }
        
        // 2장 이상인 랭크들
        val pairs = rankGroups.filter { it.value.size >= 2 }
        
        if (pairs.isNotEmpty()) {
            // 가장 높은 페어 선택
            val highestPair = pairs.maxByOrNull { rankValues[it.key] ?: 0 }
            
            highestPair?.value?.take(2)?.forEach { (index, _) ->
                potentialIndices.add(index)
            }
            
            return true
        }
        
        return false
    }
} 