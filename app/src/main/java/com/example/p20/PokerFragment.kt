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
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
    
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
            updateScoreText()
        } else {
            scoreText.text = "점수: 0\n "
        }
    }
    
    // ViewModel 공유
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
        
        // 잔액 업데이트
        updateBalanceText()
        updateBetAmountText()
        
        // 버튼 이벤트 리스너 설정
        setupButtonListeners()
        
        // 게임오버 이벤트 감지 - 람다 최적화
        observeGameState()
        
        // 환영 메시지 표시
        showCustomSnackbar("배팅 후 1인발라트로 게임을 시작해주세요!")

        // 정리 작업을 위한 Runnable 설정 - 한 번만 생성
        cleanupRunnable = Runnable {
            if (isWaitingForCleanup) {
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
    
    private fun setupButtonListeners() {
        // 베팅 버튼
        bet10kButton.setOnClickListener { 
            playBettingSound()
            addBet(10_000L) 
        }
        bet50kButton.setOnClickListener { 
            playBettingSound()
            addBet(50_000L) 
        }
        bet100kButton.setOnClickListener { 
            playBettingSound()
            addBet(100_000L) 
        }
        bet500kButton.setOnClickListener { 
            playBettingSound()
            addBet(500_000L) 
        }
        
        // 베팅 금액 초기화 기능 (0가 아닐 때 bet10kButton 길게 누르면 초기화)
        bet10kButton.setOnLongClickListener {
            if (tempBetAmount > 0 && !isGameActive) {
                tempBetAmount = 0L
                updateBetAmountText()
                showCustomSnackbar("베팅 금액이 초기화되었습니다.")
                return@setOnLongClickListener true
            }
            false
        }
        
        // 새 게임 버튼
        newGameButton.setOnClickListener { 
            if (isGameActive) {
                showCustomSnackbar("게임이 이미 진행 중입니다.")
                return@setOnClickListener
            }
            
            if (tempBetAmount <= 0) {
                showCustomSnackbar("먼저 베팅해주세요.")
                return@setOnClickListener
            }
            
            // 자산 확인
            val currentAsset = assetViewModel.asset.value ?: 0L
            if (tempBetAmount > currentAsset) {
                showCustomSnackbar("베팅 금액이 보유 자산을 초과합니다.")
                return@setOnClickListener
            }
            
            // 베팅 금액 설정 및 자산 감소
            currentBet = tempBetAmount
            tempBetAmount = 0L
            assetViewModel.decreaseAsset(currentBet)
            
            // 업데이트
            updateBalanceText()
            updateBetAmountText()
            
            // 새 게임 효과음 재생
            playStartGameSound()
            
            // 게임 시작
            startNewGame()
        }
        
        // 카드 교체 버튼
        changeButton.setOnClickListener { changeCards() }
        
        // 카드 확정 버튼
        endGameButton.setOnClickListener { 
            // 카드확정 효과음 재생
            playStopSound()
            endGame() 
        }
    }
    
    private fun addBet(amount: Long) {
        if (isGameActive) {
            showCustomSnackbar("게임 진행 중에는 베팅할 수 없습니다.")
            return
        }
        
        val currentAsset = assetViewModel.asset.value ?: 0L
        if (tempBetAmount + amount > currentAsset) {
            showCustomSnackbar("보유 자산을 초과하는 금액을 베팅할 수 없습니다.")
            return
        }
        
        // 이전 금액
        val previousBet = tempBetAmount
        
        // 금액 추가
        tempBetAmount += amount
        updateBetAmountText()
        
        // 메시지 표시
        showCustomSnackbar("베팅 금액: ${formatCurrency(previousBet)} → ${formatCurrency(tempBetAmount)}")
    }
    
    private fun updateBetAmountText() {
        betAmountText.text = "베팅 금액: ${formatCurrency(tempBetAmount)}"
    }
    
    private fun startNewGame() {
        if (currentBet <= 0) {
            showCustomSnackbar("게임을 시작하려면 먼저 베팅하세요.")
            return
        }
        
        // 게임 활성화
        isGameActive = true
        isWaitingForCleanup = false
        changeCount = 0
        isCardChanged = false
        
        // 버튼 활성화/비활성화 설정
        changeButton.isEnabled = true
        endGameButton.isEnabled = true
        
        // 베팅 버튼 비활성화
        bet10kButton.isEnabled = false
        bet50kButton.isEnabled = false
        bet100kButton.isEnabled = false
        bet500kButton.isEnabled = false
        
        // 교체 버튼 텍스트 업데이트
        updateChangeButtonText()

        // 덱 생성 및 섞기
        createShuffledDeck()

        // 카드 배분
        playerCards.clear()
        dealCards()
        
        // 카드 뷰 생성
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        
        for (i in 0 until 7) {
            addCardView(playerCardsLayout, playerCards[i], i)
        }
        
        // 패 평가
        selectedCardIndices.clear()
        handRankCardIndices.clear()
        evaluateHand()
        
        // 게임 시작 안내
        showCustomSnackbar("게임이 시작되었습니다. 카드를 선택하거나 교체하세요.")
    }
    
    private fun createShuffledDeck() {
        deck.clear()
        for (suit in suits) {
            for (rank in ranks) {
                deck.add(Card(rank, suit))
            }
        }
        deck.shuffle()
    }
    
    private fun dealCards() {
        // 플레이어에게 7장 배분
        for (i in 0 until 7) {
            playerCards.add(drawCard())
        }
    }
    
    private fun drawCard(): Card {
        if (deck.isEmpty()) {
            createShuffledDeck()
        }
        return deck.removeAt(0)
    }
    
    private fun addCardView(container: LinearLayout, card: Card, index: Int) {
        // 화면 너비에 맞게 카드 크기 계산
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        
        // 카드 사이의 간격 (dp를 픽셀로 변환)
        val cardMarginDp = 1
        val cardMargin = (cardMarginDp * displayMetrics.density).toInt()
        
        // 화면 좌우 패딩 및 여백을 고려하여 조정
        // 더 넉넉한 여백 확보
        val totalHorizontalPadding = (48 * displayMetrics.density).toInt()
        
        // 카드 7장과 간격이 화면에 딱 맞도록 카드 너비 계산
        // (전체 화면 너비 - 모든 간격 - 좌우 패딩) / 카드 개수
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
        cardView.textSize = (cardWidth * 0.25f) / displayMetrics.density // 텍스트 크기 증가 (15% → 25%)
        cardView.setPadding(2, 2, 2, 2) // 패딩 더 줄임
        cardView.setTypeface(null, Typeface.BOLD) // 텍스트를 굵게 설정
        
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
        cardView.setOnClickListener(cardClickListener)
        
        container.addView(cardView)
    }
    
    private fun changeCards() {
        if (!isGameActive) {
            showCustomSnackbar("게임이 시작되지 않았습니다.")
            return
        }
        
        if (selectedCardIndices.isEmpty()) {
            showCustomSnackbar("교체할 카드를 선택해주세요.")
            return
        }
        
        // 최대 교체 횟수 확인
        if (changeCount >= 5) {
            showCustomSnackbar("최대 5번까지만 카드 교체가 가능합니다.")
            return
        }
        
        // 교체 비용 계산
        val changeCost = getChangeCost()
        if (changeCost > 0) {
            val currentAsset = assetViewModel.asset.value ?: 0
            if (changeCost > currentAsset) {
                showCustomSnackbar("카드 교체 비용이 부족합니다. 필요 금액: ${formatCurrency(changeCost)}")
                return
            }
            assetViewModel.decreaseAsset(changeCost)
            updateBalanceText()
        }
        
        // 카드 교체 효과음 재생
        playCardSound()
        
        // 선택된 카드만 교체
        val selectedIndices = selectedCardIndices.toList() // 복사본 생성
        for (index in selectedIndices) {
            val newCard = drawCard()
            playerCards[index] = newCard
            updateCardView(playerCardsLayout, newCard, index)
        }
        
        // 카드 교체 횟수 증가 및 버튼 텍스트 업데이트
        changeCount++
        isCardChanged = true
        updateChangeButtonText()
        
        // 모든 카드 선택 상태 초기화 먼저 수행
        for (i in 0 until cardViews.size) {
            cardViews[i].alpha = 1.0f
            cardViews[i].background = defaultCardDrawable.constantState?.newDrawable()
            // 선택 상태도 초기화
            cardViews[i].setTypeface(cardViews[i].typeface, Typeface.NORMAL)
        }
        
        // 선택 인덱스 초기화
        selectedCardIndices.clear()
        
        // 패 재평가 (이전 족보 정보 초기화 후 새로 계산)
        handRankCardIndices.clear()
        val handRank = evaluateHand()
        
        // 새로운 족보에 포함된 카드 강조 표시
        if (handRank != HandRank.HIGH_CARD && handRank != HandRank.NONE) {
            highlightHandRankCards()
        }
        
        // 교체 완료 메시지
        val nextCost = getChangeCost()
        val message = if (nextCost == 0L && changeCount < 3) {
            "카드가 교체되었습니다. 교체 횟수: $changeCount/5 (무료 교체 ${3-changeCount}회 남음)"
        } else if (nextCost > 0 && changeCount < 5) {
            "카드가 교체되었습니다. 교체 횟수: $changeCount/5 (다음 교체 비용: ${formatCurrency(nextCost)})"
        } else {
            "카드가 교체되었습니다. 교체 횟수: $changeCount/5 (더 이상 교체할 수 없습니다)"
        }
        
        showCustomSnackbar(message)
    }
    
    // 카드 뷰 업데이트 메서드 분리 (성능 최적화)
    private fun updateCardView(container: LinearLayout, card: Card, index: Int) {
        if (index < cardViews.size) {
            val cardView = cardViews[index]
            cardView.text = card.toString()
            
            // 하트/다이아는 빨간색, 스페이드/클럽은 검은색
            val textColor = if (card.suit == "♥" || card.suit == "♦") Color.RED else Color.BLACK
            cardView.setTextColor(textColor)
            cardView.alpha = 1.0f
            cardView.background = defaultCardDrawable.constantState?.newDrawable()
        }
    }
    
    private fun evaluateHand(): HandRank {
        if (playerCards.size < 5) return HandRank.NONE
        
        // 족보에 포함된 카드 인덱스 초기화
        handRankCardIndices.clear()
        
        // 족보 순위
        val isFlush = isFlush()
        val isStraight = isStraight()
        
        val handRank = when {
            isRoyalStraightFlush() -> HandRank.ROYAL_STRAIGHT_FLUSH
            isFlush && isStraight -> HandRank.STRAIGHT_FLUSH
            isFourOfAKind() -> HandRank.FOUR_OF_A_KIND
            isFullHouse() -> HandRank.FULL_HOUSE
            isFlush -> HandRank.FLUSH
            isStraight -> HandRank.STRAIGHT
            isThreeOfAKind() -> HandRank.THREE_OF_A_KIND
            isTwoPair() -> HandRank.TWO_PAIR
            isPair() -> HandRank.ONE_PAIR
            else -> HandRank.HIGH_CARD
        }
        
        // 족보 업데이트
        handRankText.text = when(handRank) {
            HandRank.ROYAL_STRAIGHT_FLUSH -> "로얄 스트레이트 플러시"
            HandRank.STRAIGHT_FLUSH -> "스트레이트 플러시"
            HandRank.FOUR_OF_A_KIND -> "포카드"
            HandRank.FULL_HOUSE -> "풀하우스"
            HandRank.FLUSH -> "플러시"
            HandRank.STRAIGHT -> "스트레이트"
            HandRank.THREE_OF_A_KIND -> "트리플"
            HandRank.TWO_PAIR -> "투페어"
            HandRank.ONE_PAIR -> "원페어"
            HandRank.HIGH_CARD -> "하이카드"
            HandRank.NONE -> "패 없음"
        }
        
        // 만약 높은 패가 있다면 해당 카드들 강조
        if (handRank != HandRank.HIGH_CARD && handRank != HandRank.NONE) {
            highlightHandRankCards()
        }
        
        return handRank
    }
    
    private fun highlightHandRankCards() {
        // 모든 카드는 기본 스타일로 초기화
        for (i in 0 until cardViews.size) {
            if (!selectedCardIndices.contains(i)) {
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
        
        // 족보에 포함된 카드만 강조 표시 - 매우 연한 회색 배경으로 변경
        for (index in handRankCardIndices) {
            if (index < cardViews.size) {
                // 매우 연한 회색 배경으로 강조 (불투명도 15%)
                val strokeDrawable = GradientDrawable().apply {
                    setStroke(3, Color.BLACK)
                    cornerRadius = 8f
                    setColor(Color.argb(200, 135, 206, 250)) // 매우 연한 회색 배경
                }
                cardViews[index].background = strokeDrawable
                
                // 텍스트 굵게 표시
                cardViews[index].setTypeface(null, Typeface.BOLD)
                cardViews[index].alpha = 1.0f
            }
        }
    }
    
    private fun isRoyalStraightFlush(): Boolean {
        // 먼저 같은 무늬가 5장 이상 있는지 확인
        val suitGroups = playerCards.groupBy { it.suit }
        val flushSuit = suitGroups.entries.find { it.value.size >= 5 }?.key ?: return false
        
        // 해당 무늬의 카드들만 추출
        val sameSuitCards = playerCards.filter { it.suit == flushSuit }
        
        // 같은 무늬 중에서 10, J, Q, K, A가 있는지 확인
        val royalValues = listOf(10, 11, 12, 13, 14)
        val royalCards = sameSuitCards.filter { it.value() in royalValues }
        
        if (royalCards.size >= 5) {
            // 로얄 스트레이트 플러시를 구성하는 카드들의 인덱스 저장
            for (cardValue in royalValues) {
                val card = royalCards.find { it.value() == cardValue } ?: continue
                val index = playerCards.indexOf(card)
                if (index != -1) {
                    handRankCardIndices.add(index)
                }
            }
            return true
        }
        return false
    }
    
    private fun isFlush(): Boolean {
        // 같은 무늬가 5장 이상 있는지 확인
        val suitGroups = playerCards.groupBy { it.suit }
        val flushSuit = suitGroups.entries.find { it.value.size >= 5 }?.key
        
        if (flushSuit != null) {
            // 플러시를 구성하는 카드들의 인덱스만 저장 (5장)
            val flushCards = playerCards.withIndex().filter { it.value.suit == flushSuit }
                .sortedByDescending { it.value.value() }
                .take(5)
            
            for (card in flushCards) {
                handRankCardIndices.add(card.index)
            }
            return true
        }
        return false
    }
    
    private fun isStraight(): Boolean {
        // 중복 제거 후 값 정렬
        val uniqueValues = playerCards.map { it.value() }.toSet().toList().sorted()
        
        // 5개 이상의 연속된 값이 있는지 확인
        if (uniqueValues.size >= 5) {
            for (i in 0..uniqueValues.size - 5) {
                if (uniqueValues[i + 4] - uniqueValues[i] == 4) {
                    // 스트레이트를 구성하는 5개의 값 구하기
                    val straightValues = (uniqueValues[i]..uniqueValues[i + 4]).toList()
                    
                    // 해당 값에 해당하는 카드 찾아서 인덱스 저장 (5장)
                    val usedRanks = mutableSetOf<String>()
                    
                    for (value in straightValues) {
                        val card = playerCards.withIndex().find { 
                            it.value.value() == value && !usedRanks.contains(it.value.rank)
                        }
                        if (card != null) {
                            handRankCardIndices.add(card.index)
                            usedRanks.add(card.value.rank)
                        }
                    }
                    return true
                }
            }
        }
        
        // A-2-3-4-5 스트레이트 체크
        if (uniqueValues.containsAll(listOf(2, 3, 4, 5)) && uniqueValues.contains(14)) {
            val straightValues = listOf(14, 2, 3, 4, 5)
            
            // 해당 값에 해당하는 카드 찾아서 인덱스 저장 (5장)
            val usedRanks = mutableSetOf<String>()
            
            for (value in straightValues) {
                val card = playerCards.withIndex().find { 
                    it.value.value() == value && !usedRanks.contains(it.value.rank)
                }
                if (card != null) {
                    handRankCardIndices.add(card.index)
                    usedRanks.add(card.value.rank)
                }
            }
            return true
        }
        
        return false
    }
    
    private fun isFourOfAKind(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        val fourOfKind = rankGroups.entries.find { it.value.size >= 4 }
        
        if (fourOfKind != null) {
            // 포카드를 구성하는 카드들의 인덱스만 저장 (4장)
            for (i in playerCards.indices) {
                if (playerCards[i].rank == fourOfKind.key) {
                    handRankCardIndices.add(i)
                }
            }
            return true
        }
        return false
    }
    
    private fun isFullHouse(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        
        // 3장 이상 같은 카드가 있는지 확인
        val threeOfKind = rankGroups.entries.find { it.value.size >= 3 }
        
        // 다른 2장 이상 같은 카드가 있는지 확인
        val pair = rankGroups.entries.find { it.value.size >= 2 && it.key != threeOfKind?.key }
        
        if (threeOfKind != null && pair != null) {
            // 풀하우스를 구성하는 카드들의 인덱스만 저장 (트리플 3장 + 페어 2장)
            // 먼저 트리플 카드 추가
            var tripleCount = 0
            for (i in playerCards.indices) {
                if (playerCards[i].rank == threeOfKind.key && tripleCount < 3) {
                    handRankCardIndices.add(i)
                    tripleCount++
                }
            }
            
            // 그 다음 페어 카드 추가
            var pairCount = 0
            for (i in playerCards.indices) {
                if (playerCards[i].rank == pair.key && pairCount < 2) {
                    handRankCardIndices.add(i)
                    pairCount++
                }
            }
            
            return true
        }
        return false
    }
    
    private fun isThreeOfAKind(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        val threeOfKind = rankGroups.entries.find { it.value.size >= 3 }
        
        if (threeOfKind != null) {
            // 트리플을 구성하는 카드들의 인덱스만 저장 (3장)
            var count = 0
            for (i in playerCards.indices) {
                if (playerCards[i].rank == threeOfKind.key && count < 3) {
                    handRankCardIndices.add(i)
                    count++
                }
            }
            return true
        }
        return false
    }
    
    private fun isTwoPair(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        val pairs = rankGroups.entries.filter { it.value.size >= 2 }
        
        if (pairs.size >= 2) {
            // 높은 두 페어 선택
            val topTwoPairs = pairs.sortedByDescending { rankValues[it.key] ?: 0 }.take(2)
            
            // 두 페어를 구성하는 카드들의 인덱스만 저장 (각 2장씩)
            for (pair in topTwoPairs) {
                var count = 0
                for (i in playerCards.indices) {
                    if (playerCards[i].rank == pair.key && count < 2) {
                        handRankCardIndices.add(i)
                        count++
                    }
                }
            }
            return true
        }
        return false
    }
    
    private fun isPair(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        val pair = rankGroups.entries.find { it.value.size >= 2 }
        
        if (pair != null) {
            // 페어를 구성하는 카드들의 인덱스만 저장 (2장)
            var count = 0
            for (i in playerCards.indices) {
                if (playerCards[i].rank == pair.key && count < 2) {
                    handRankCardIndices.add(i)
                    count++
                }
            }
            return true
        }
        return false
    }
    
    // 점수에 따른 배율 계산 함수
    private fun getMultiplierByScore(score: Int): Int {
        for ((threshold, multiplier) in SCORE_MULTIPLIERS) {
            if (score >= threshold) {
                return multiplier
            }
        }
        return 0
    }
    
    private fun endGame() {
        // 5장의 카드를 선택했는지 확인
        if (selectedCardIndices.size != 5) {
            showCustomSnackbar("정확히 5장의 카드를 선택해야 합니다.")
            return
        }

        isGameActive = false
        isWaitingForCleanup = true
        changeButton.isEnabled = false
        endGameButton.isEnabled = false
        
        // 배팅 버튼 비활성화 유지
        bet10kButton.isEnabled = false
        bet50kButton.isEnabled = false
        bet100kButton.isEnabled = false
        bet500kButton.isEnabled = false
        newGameButton.isEnabled = false
        
        // 선택한 5장의 카드만 평가
        val selectedCards = selectedCardIndices.map { playerCards[it] }
        val handRank = evaluateSelected5Cards()
        val score = calculateScore(handRank, selectedCards)
        val multiplier = getMultiplierByScore(score)
        
        // 배당금 계산
        val payout = currentBet * multiplier
        val snackbarColor: Int
        val resultMessage: String
        
        if (multiplier > 0) {
            // 승리
            // 승리 효과음 재생
            playWinSound()
            
            assetViewModel.increaseAsset(payout)
            winCount++
            resultMessage = "축하합니다! ${score}점으로 ${multiplier}배 획득! (${handRank.koreanName}) +${formatCurrency(payout - currentBet)}"
            snackbarColor = Color.argb(200, 76, 175, 80) // 녹색
            
            // 승리 시 아이템 획득 처리
            processItemReward(currentBet, multiplier)
            
            // 승리 애니메이션 표시
            val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            CasinoAnimationManager.showWinAnimation(
                rootView,
                "${handRank.koreanName} (${score}점)",
                "+${formatCurrency(payout - currentBet)}"
            )
        } else {
            // 패배
            // 패배 효과음 재생
            playLoseSound()
            
            loseCount++
            resultMessage = "아쉽습니다. ${score}점으로 배당을 받지 못했습니다. (${handRank.koreanName}) -${formatCurrency(currentBet)}"
            snackbarColor = Color.argb(200, 244, 67, 54) // 빨간색
            
            // 패배 애니메이션 표시
            val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            CasinoAnimationManager.showLoseAnimation(
                rootView,
                "${handRank.koreanName} (${score}점)",
                "-${formatCurrency(currentBet)}"
            )
        }
        
        // 결과 표시
        showResultSnackbar(resultMessage, snackbarColor)
        
        // 통계 업데이트
        updateBalanceText()
        
        // 베팅 초기화
        currentBet = 0L
        tempBetAmount = 0L
        
        // 선택되지 않은 카드 흐리게 표시
        highlightSelectedCards()
        
        // 정리 작업 지연 (애니메이션을 위해 더 긴 시간 대기)
        cleanupRunnable?.let { runnable ->
            mainHandler.removeCallbacks(runnable) // 기존에 예약된 정리 작업 취소
            mainHandler.postDelayed(runnable, 3000) // 3초 지연으로 수정 (기존 4초)
        }
    }
    
    /**
     * 승리 시 아이템 획득 처리
     */
    private fun processItemReward(betAmount: Long, multiplier: Int) {
        // 배율이 높을수록 아이템 획득 확률 증가를 위해 베팅 금액 조정
        val adjustedBet = betAmount * multiplier.toLong() / 2
        
        // 아이템 획득 처리 (포커는 gameType 2)
        val itemReward = ItemUtil.processCasinoWin(requireContext(), adjustedBet, 2)
        
        // 아이템을 획득했으면 메시지 표시
        itemReward?.let {
            // 1.5초 지연 후 아이템 획득 메시지 표시 (기존 승리 메시지와 겹치지 않게)
            mainHandler.postDelayed({
                showCustomSnackbar("🎁 ${it.itemName} 아이템을 획득했습니다!")
            }, 1500)
        }
    }
    
    // 선택한 5장의 카드만으로 패 평가 함수들
    private fun evaluateSelected5Cards(): HandRank {
        if (selectedCardIndices.size != 5) return HandRank.NONE
        
        // 족보에 포함된 카드 인덱스 초기화
        handRankCardIndices.clear()
        // 선택한 5장의 카드만 추출
        val selectedCards = selectedCardIndices.map { playerCards[it] }
        
        // 족보 순위 평가 (선택한 5장으로만)
        val isFlush = isFlushForSelected(selectedCards)
        val isStraight = isStraightForSelected(selectedCards)
        
        val handRank = when {
            isRoyalStraightFlushForSelected(selectedCards) -> HandRank.ROYAL_STRAIGHT_FLUSH
            isFlush && isStraight -> HandRank.STRAIGHT_FLUSH
            isFourOfAKindForSelected(selectedCards) -> HandRank.FOUR_OF_A_KIND
            isFullHouseForSelected(selectedCards) -> HandRank.FULL_HOUSE
            isFlush -> HandRank.FLUSH
            isStraight -> HandRank.STRAIGHT
            isThreeOfAKindForSelected(selectedCards) -> HandRank.THREE_OF_A_KIND
            isTwoPairForSelected(selectedCards) -> HandRank.TWO_PAIR
            isPairForSelected(selectedCards) -> HandRank.ONE_PAIR
            else -> HandRank.HIGH_CARD
        }
        
        // 족보 업데이트
        handRankText.text = when(handRank) {
            HandRank.ROYAL_STRAIGHT_FLUSH -> "로얄 스트레이트 플러시"
            HandRank.STRAIGHT_FLUSH -> "스트레이트 플러시"
            HandRank.FOUR_OF_A_KIND -> "포카드"
            HandRank.FULL_HOUSE -> "풀하우스"
            HandRank.FLUSH -> "플러시"
            HandRank.STRAIGHT -> "스트레이트"
            HandRank.THREE_OF_A_KIND -> "트리플"
            HandRank.TWO_PAIR -> "투페어"
            HandRank.ONE_PAIR -> "원페어"
            HandRank.HIGH_CARD -> "하이카드"
            HandRank.NONE -> "패 없음"
        }
        
        // 모든 선택된 카드를 족보에 포함된 카드로 표시
        handRankCardIndices.addAll(selectedCardIndices)
        
        return handRank
    }
    
    // 선택된 카드 강조하고 선택되지 않은 카드 흐리게 표시
    private fun highlightSelectedCards() {
        // 모든 카드 흐리게 표시
        for (i in 0 until cardViews.size) {
            cardViews[i].alpha = 0.3f
            cardViews[i].background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        }
        
        // 선택된 카드만 선명하게 표시
        for (index in selectedCardIndices) {
            if (index < cardViews.size) {
                cardViews[index].alpha = 1.0f
                val drawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)?.mutate()
                drawable?.setTint(Color.argb(100, 0, 200, 0))  // 반투명 초록색
                cardViews[index].background = drawable
                cardViews[index].setTypeface(cardViews[index].typeface, android.graphics.Typeface.BOLD)
            }
        }
    }
    
    // 선택한 카드만으로 패 평가 함수들
    private fun isRoyalStraightFlushForSelected(cards: List<Card>): Boolean {
        // 같은 무늬인지 확인
        val suit = cards.groupBy { it.suit }.maxByOrNull { it.value.size }?.key ?: return false
        if (cards.count { it.suit == suit } != 5) return false
        
        // 로얄 스트레이트인지 확인 (10, J, Q, K, A)
        val values = cards.map { it.value() }.toSet()
        return values.containsAll(listOf(10, 11, 12, 13, 14))
    }
    
    private fun isFlushForSelected(cards: List<Card>): Boolean {
        val suitGroup = cards.groupBy { it.suit }
        return suitGroup.any { it.value.size == 5 }
    }
    
    private fun isStraightForSelected(cards: List<Card>): Boolean {
        // 값을 정렬
        val sortedValues = cards.map { it.value() }.sorted()
        
        // 일반 스트레이트 체크
        if (sortedValues[4] - sortedValues[0] == 4 && sortedValues.toSet().size == 5) {
            return true
        }
        
        // A-2-3-4-5 스트레이트 체크
        return sortedValues.containsAll(listOf(2, 3, 4, 5, 14))
    }
    
    private fun isFourOfAKindForSelected(cards: List<Card>): Boolean {
        val rankGroups = cards.groupBy { it.rank }
        return rankGroups.any { it.value.size == 4 }
    }
    
    private fun isFullHouseForSelected(cards: List<Card>): Boolean {
        val rankGroups = cards.groupBy { it.rank }
        val hasThree = rankGroups.any { it.value.size == 3 }
        val hasPair = rankGroups.any { it.value.size == 2 }
        return hasThree && hasPair
    }
    
    private fun isThreeOfAKindForSelected(cards: List<Card>): Boolean {
        val rankGroups = cards.groupBy { it.rank }
        return rankGroups.any { it.value.size == 3 }
    }
    
    private fun isTwoPairForSelected(cards: List<Card>): Boolean {
        val rankGroups = cards.groupBy { it.rank }
        val pairCount = rankGroups.count { it.value.size == 2 }
        return pairCount == 2
    }
    
    private fun isPairForSelected(cards: List<Card>): Boolean {
        val rankGroups = cards.groupBy { it.rank }
        return rankGroups.any { it.value.size == 2 }
    }
    
    private fun updateBalanceText() {
        // UI 요소 제거됨 - 메서드만 유지
    }
    
    private fun formatCurrency(amount: Long): String {
        return currencyFormatter.format(amount)
    }
    
    private fun showCustomSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
    }
    
    private fun showResultSnackbar(message: String, backgroundColor: Int) {
        // 결과 메시지도 상단 메시지로 표시 (배경색 정보 무시)
        MessageManager.showMessage(requireContext(), message)
    }
    
    // 카드 교체 비용 계산 함수
    private fun getChangeCost(): Long {
        return when (changeCount) {
            0, 1, 2 -> 0L  // 첫 3번은 무료
            3 -> currentBet / 2  // 4번째는 배팅금의 절반
            4 -> currentBet  // 5번째는 배팅금만큼
            else -> currentBet  // 최대 5번까지만 가능하므로 이 경우는 발생하지 않음
        }
    }
    
    // 교체 버튼 텍스트 업데이트
    private fun updateChangeButtonText() {
        val cost = getChangeCost()
        changeButton.text = if (cost == 0L) {
            "카드 교체\n(무료)"
        } else {
            "카드 교체\n(${formatCurrency(cost)})"
        }
    }
    
    // 점수 텍스트 업데이트 함수
    private fun updateScoreText() {
        if (selectedCardIndices.size != 5) {
            scoreText.text = "점수: 0\n "  // 줄바꿈 추가하여 항상 2줄 유지
            return
        }

        val selectedCards = selectedCardIndices.map { playerCards[it] }
        val handRank = evaluateSelected5Cards()
        val score = calculateScore(handRank, selectedCards)
        
        // 점수 계산식 생성
        val cardSum = selectedCards.sumOf { it.value() }
        val formula = when (handRank) {
            HandRank.ROYAL_STRAIGHT_FLUSH -> "(150 + $cardSum) × 10 = $score"
            HandRank.STRAIGHT_FLUSH -> "(100 + $cardSum) × 8 = $score"
            HandRank.FOUR_OF_A_KIND -> "(60 + $cardSum) × 7 = $score"
            HandRank.FULL_HOUSE -> "(40 + $cardSum) × 4 = $score"
            HandRank.FLUSH -> "(35 + $cardSum) × 4 = $score"
            HandRank.STRAIGHT -> "(30 + $cardSum) × 4 = $score"
            HandRank.THREE_OF_A_KIND -> "(30 + $cardSum) × 3 = $score"
            HandRank.TWO_PAIR -> "(20 + $cardSum) × 2 = $score"
            HandRank.ONE_PAIR -> "(10 + $cardSum) × 2 = $score"
            HandRank.HIGH_CARD -> "$cardSum = $score"
            HandRank.NONE -> "0"
        }
        
        // 카드값 표시 (예: 10, 10, 10, 10, 2)
        val cardValues = selectedCards.joinToString(", ") { "${it.rank}" }
        
        // 배당률 계산
        val multiplier = getMultiplierByScore(score)
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

    // 게임 규칙 및 배당률 정보 표시 함수
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
    
    // 게임 상태 감시 함수 - 옵저버 최적화
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
    
    // 게임 상태 초기화 함수
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
        changeButton.isEnabled = false
        endGameButton.isEnabled = false
        bet10kButton.isEnabled = true
        bet50kButton.isEnabled = true
        bet100kButton.isEnabled = true
        bet500kButton.isEnabled = true
        newGameButton.isEnabled = true
        
        // 베팅 금액 텍스트 초기화
        updateBetAmountText()
    }
    
    // 점수 계산 함수 최적화 - 중복 계산 제거
    private fun calculateScore(handRank: HandRank, cards: List<Card>): Int {
        val cardSum = cards.sumOf { it.value() }
        
        return when (handRank) {
            HandRank.ROYAL_STRAIGHT_FLUSH -> (150 + cardSum) * 10
            HandRank.STRAIGHT_FLUSH -> (100 + cardSum) * 8
            HandRank.FOUR_OF_A_KIND -> (60 + cardSum) * 7
            HandRank.FULL_HOUSE -> (40 + cardSum) * 4
            HandRank.FLUSH -> (35 + cardSum) * 4
            HandRank.STRAIGHT -> (30 + cardSum) * 4
            HandRank.THREE_OF_A_KIND -> (30 + cardSum) * 3
            HandRank.TWO_PAIR -> (20 + cardSum) * 2
            HandRank.ONE_PAIR -> (10 + cardSum) * 2
            HandRank.HIGH_CARD -> cardSum
            HandRank.NONE -> 0
        }
    }
    
    // 포커 패 족보 enum
    enum class HandRank(val koreanName: String) {
        ROYAL_STRAIGHT_FLUSH("로얄 스트레이트 플러시"),
        STRAIGHT_FLUSH("스트레이트 플러시"),
        FOUR_OF_A_KIND("포카드"),
        FULL_HOUSE("풀하우스"),
        FLUSH("플러시"),
        STRAIGHT("스트레이트"),
        THREE_OF_A_KIND("트리플"),
        TWO_PAIR("투페어"),
        ONE_PAIR("원페어"),
        HIGH_CARD("하이카드"),
        NONE("패 없음")
    }

    private fun cleanupGame() {
        // 게임 상태 초기화
        isGameActive = false
        isCardDealt = false
        isCardChanged = false
        changeCount = 0
        currentBet = 0L
        tempBetAmount = 0L
        
        // UI 초기화
        playerCardsLayout.removeAllViews()
        handRankText.text = ""
        scoreText.text = "점수: 0\n "  // 줄바꿈 추가하여 항상 2줄 유지
        updateBetAmountText()
        
        // 선택된 카드 초기화
        selectedCardIndices.clear()
        handRankCardIndices.clear()
        
        // 카드 덱과 손패 초기화
        deck.clear()
        playerCards.clear()
        
        // 버튼 상태 업데이트
        updateButtonStates()
    }

    private fun updateButtonStates() {
        // 버튼 상태 업데이트
        changeButton.isEnabled = false
        endGameButton.isEnabled = false
        bet10kButton.isEnabled = true
        bet50kButton.isEnabled = true
        bet100kButton.isEnabled = true
        bet500kButton.isEnabled = true
        newGameButton.isEnabled = true
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
    
    /**
     * 배팅 효과음을 재생합니다.
     */
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
    
    /**
     * 카드 교체 효과음을 재생합니다.
     */
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
    
    /**
     * 새 게임 효과음을 재생합니다.
     */
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
    
    /**
     * 승리 효과음을 재생합니다.
     */
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
    
    /**
     * 패배 효과음을 재생합니다.
     */
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

    /**
     * 카드 선택 효과음을 재생합니다.
     */
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

    /**
     * 카드 확정 효과음을 재생합니다.
     */
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
} 