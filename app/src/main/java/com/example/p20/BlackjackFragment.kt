package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale
import java.util.Random
import android.os.Handler
import android.os.Looper

class BlackjackFragment : Fragment() {

    // UI 컴포넌트
    private lateinit var dealerCardsLayout: LinearLayout
    private lateinit var playerCardsLayout: LinearLayout
    private lateinit var dealerScoreText: TextView
    private lateinit var playerScoreText: TextView
    private lateinit var betAmountText: TextView
    private lateinit var hitButton: Button
    private lateinit var standButton: Button
    private lateinit var newGameButton: Button
    private lateinit var bet10kButton: Button
    private lateinit var bet50kButton: Button
    private lateinit var bet100kButton: Button
    private lateinit var balanceText: TextView
    private lateinit var winLoseText: TextView
    
    // 게임 상태
    private var currentBet = 0L
    private var tempBetAmount = 0L
    private var isGameActive = false
    private var isGameOver = false
    private var winCount = 0
    private var loseCount = 0
    
    // 카드 관련 변수
    private val suits = listOf("♠", "♥", "♦", "♣")
    private val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    private val values = mapOf(
        "A" to 11, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9,
        "10" to 10, "J" to 10, "Q" to 10, "K" to 10
    )
    
    // 카드 덱과 손패
    private val deck = mutableListOf<Card>()
    private val playerCards = mutableListOf<Card>()
    private val dealerCards = mutableListOf<Card>()
    
    // ViewModel 공유
    private val assetViewModel: AssetViewModel by activityViewModels()

    data class Card(val rank: String, val suit: String) {
        override fun toString(): String {
            return "$rank$suit"
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_blackjack, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UI 초기화
        dealerCardsLayout = view.findViewById(R.id.dealerCardsLayout)
        playerCardsLayout = view.findViewById(R.id.playerCardsLayout)
        dealerScoreText = view.findViewById(R.id.dealerScoreText)
        playerScoreText = view.findViewById(R.id.playerScoreText)
        betAmountText = view.findViewById(R.id.betAmountText)
        hitButton = view.findViewById(R.id.hitButton)
        standButton = view.findViewById(R.id.standButton)
        newGameButton = view.findViewById(R.id.newGameButton)
        bet10kButton = view.findViewById(R.id.bet10kButton)
        bet50kButton = view.findViewById(R.id.bet50kButton)
        bet100kButton = view.findViewById(R.id.bet100kButton)
        balanceText = view.findViewById(R.id.balanceText)
        winLoseText = view.findViewById(R.id.winLoseText)
        
        // 잔액 업데이트
        updateBalanceText()
        updateBetAmountText()
        
        // 버튼 이벤트 리스너 설정
        setupButtonListeners()
        
        // 환영 메시지 표시
        showCustomSnackbar("배팅 후 블랙잭 게임을 시작해주세요!")
    }
    
    private fun setupButtonListeners() {
        // 베팅 버튼 리스너
        bet10kButton.setOnClickListener {
            addBet(10000)
        }
        
        bet50kButton.setOnClickListener {
            addBet(50000)
        }
        
        bet100kButton.setOnClickListener {
            addBet(100000)
        }
        
        // 새 게임 시작 버튼
        newGameButton.setOnClickListener {
            if (isGameActive) {
                showCustomSnackbar("현재 게임이 진행 중입니다.")
                return@setOnClickListener
            }
            
            if (tempBetAmount <= 0) {
                showCustomSnackbar("베팅 금액을 설정해주세요.")
                return@setOnClickListener
            }
            
            val currentAsset = assetViewModel.asset.value ?: 0L
            if (tempBetAmount > currentAsset) {
                showCustomSnackbar("베팅 금액이 보유 자산을 초과합니다.")
                return@setOnClickListener
            }
            
            // 베팅 금액 설정 및 자산 감소
            currentBet = tempBetAmount
            tempBetAmount = 0L
            assetViewModel.decreaseAsset(currentBet)
            updateBalanceText()
            updateBetAmountText()
            
            // 게임 시작
            startNewGame()
        }
        
        // 히트 버튼 - 카드 추가
        hitButton.setOnClickListener {
            if (!isGameActive) {
                showCustomSnackbar("게임이 진행 중이 아닙니다.")
                return@setOnClickListener
            }
            
            playerHit()
        }
        
        // 스탠드 버튼 - 턴 종료
        standButton.setOnClickListener {
            if (!isGameActive) {
                showCustomSnackbar("게임이 진행 중이 아닙니다.")
                return@setOnClickListener
            }
            
            playerStand()
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
        
        tempBetAmount += amount
        updateBetAmountText()
    }
    
    private fun updateBetAmountText() {
        betAmountText.text = "베팅 금액: ${formatCurrency(tempBetAmount)}"
    }
    
    private fun startNewGame() {
        // 게임 상태 초기화
        isGameActive = true
        isGameOver = false
        playerCards.clear()
        dealerCards.clear()
        dealerCardsLayout.removeAllViews()
        playerCardsLayout.removeAllViews()
        
        // 덱 생성 및 섞기
        createShuffledDeck()
        
        // 초기 카드 배포 (딜러 1장, 플레이어 2장)
        dealerCards.add(drawCard())
        addCardView(dealerCardsLayout, dealerCards[0], 0)
        
        playerCards.add(drawCard())
        playerCards.add(drawCard())
        for (i in playerCards.indices) {
            addCardView(playerCardsLayout, playerCards[i], i)
        }
        
        // 버튼 활성화
        hitButton.isEnabled = true
        standButton.isEnabled = true
        
        // 점수 업데이트
        dealerScoreText.text = "점수: ${calculateDealerScore(true)}"
        playerScoreText.text = "점수: ${calculatePlayerScore()}"
        
        // 블랙잭 체크
        checkForBlackjack()
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
    
    private fun drawCard(): Card {
        if (deck.isEmpty()) {
            createShuffledDeck()
        }
        return deck.removeAt(0)
    }
    
    private fun playerHit() {
        if (isGameOver) return
        
        // 플레이어에게 카드 한 장 추가
        val card = drawCard()
        playerCards.add(card)
        addCardView(playerCardsLayout, card, playerCards.size - 1)
        
        // 점수 업데이트
        val score = calculatePlayerScore()
        playerScoreText.text = "점수: $score"
        
        // 버스트 확인
        if (score > 21) {
            isGameOver = true
            showAllDealerCards()
            
            // 게임 종료 처리 (지는 경우)
            endGame(false, "버스트! 21점을 초과하여 패배했습니다.")
        }
    }
    
    private fun playerStand() {
        if (isGameOver) return
        
        isGameOver = true
        showAllDealerCards()
        
        // 딜러가 규칙에 따라 카드를 뽑음 (17점 이상까지)
        val handler = Handler(Looper.getMainLooper())
        
        fun dealerDrawStep() {
            val dealerScore = calculateDealerScore(false)
            
            if (dealerScore < 17) {
                // 딜러가 17점 미만이면 카드를 더 뽑음
                val card = drawCard()
                dealerCards.add(card)
                addCardView(dealerCardsLayout, card, dealerCards.size - 1)
                
                // 점수 업데이트
                dealerScoreText.text = "점수: $dealerScore"
                
                // 0.5초 후 다시 확인
                handler.postDelayed({ dealerDrawStep() }, 500)
            } else {
                // 딜러가 17점 이상이면 승패 판정
                val playerScore = calculatePlayerScore()
                dealerScoreText.text = "점수: $dealerScore"
                
                when {
                    dealerScore > 21 -> {
                        // 딜러가 버스트 -> 플레이어 승리
                        endGame(true, "딜러 버스트! 플레이어 승리!")
                    }
                    playerScore > dealerScore -> {
                        // 플레이어 점수가 더 높음 -> 플레이어 승리
                        endGame(true, "플레이어 승리!")
                    }
                    playerScore < dealerScore -> {
                        // 딜러 점수가 더 높음 -> 딜러 승리
                        endGame(false, "딜러 승리!")
                    }
                    else -> {
                        // 무승부
                        endGame(null, "무승부!")
                    }
                }
            }
        }
        
        // 딜러 카드 뽑기 시작
        dealerDrawStep()
    }
    
    private fun showAllDealerCards() {
        // 딜러의 모든 카드 공개
        dealerCardsLayout.removeAllViews()
        for (i in dealerCards.indices) {
            addCardView(dealerCardsLayout, dealerCards[i], i)
        }
        
        // 실제 점수로 업데이트
        dealerScoreText.text = "점수: ${calculateDealerScore(false)}"
    }
    
    private fun addCardView(container: LinearLayout, card: Card, index: Int) {
        val cardView = TextView(requireContext())
        cardView.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) / 2,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) / 20
        }
        
        cardView.background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        cardView.gravity = Gravity.CENTER
        cardView.textSize = 18f
        cardView.setPadding(8, 8, 8, 8)
        
        // 첫 번째 딜러 카드는 숨김 (게임 중일 때)
        if (container == dealerCardsLayout && index == 0 && !isGameOver) {
            cardView.text = "?"
            cardView.setTextColor(Color.BLACK)
        } else {
            cardView.text = card.toString()
            // 하트/다이아는 빨간색, 스페이드/클럽은 검은색
            val textColor = if (card.suit == "♥" || card.suit == "♦") Color.RED else Color.BLACK
            cardView.setTextColor(textColor)
        }
        
        container.addView(cardView)
    }
    
    private fun calculatePlayerScore(): Int {
        var score = 0
        var aceCount = 0
        
        for (card in playerCards) {
            val value = values[card.rank] ?: 0
            score += value
            if (card.rank == "A") aceCount++
        }
        
        // A는 필요에 따라 1 또는 11로 취급 (21을 넘지 않도록)
        while (score > 21 && aceCount > 0) {
            score -= 10  // 11에서 1로 변경 (차이 10)
            aceCount--
        }
        
        return score
    }
    
    private fun calculateDealerScore(isHidden: Boolean): Int {
        var score = 0
        var aceCount = 0
        
        // 첫 번째 카드만 표시하는 경우
        val visibleCards = if (isHidden) dealerCards.take(1) else dealerCards
        
        for (card in visibleCards) {
            val value = values[card.rank] ?: 0
            score += value
            if (card.rank == "A") aceCount++
        }
        
        // A는 필요에 따라 1 또는 11로 취급 (21을 넘지 않도록)
        while (score > 21 && aceCount > 0) {
            score -= 10  // 11에서 1로 변경 (차이 10)
            aceCount--
        }
        
        return score
    }
    
    private fun checkForBlackjack() {
        // 블랙잭 확인 (처음 2장으로 21점)
        val playerHasBlackjack = playerCards.size == 2 && calculatePlayerScore() == 21
        
        // 딜러의 첫 카드가 A 또는 10점 카드인 경우, 블랙잭 가능성이 있으므로 확인
        val dealerFirstCard = dealerCards.firstOrNull()
        val dealerMayHaveBlackjack = dealerFirstCard != null && 
                (dealerFirstCard.rank == "A" || listOf("10", "J", "Q", "K").contains(dealerFirstCard.rank))
        
        if (playerHasBlackjack) {
            // 딜러도 블랙잭 가능성이 있는 경우, 2번째 카드를 뽑고 블랙잭 여부 확인
            if (dealerMayHaveBlackjack) {
                val dealerSecondCard = drawCard()
                dealerCards.add(dealerSecondCard)
                
                // 딜러의 모든 카드 공개
                showAllDealerCards()
                
                val dealerHasBlackjack = calculateDealerScore(false) == 21
                
                if (dealerHasBlackjack) {
                    // 둘 다 블랙잭 -> 푸시
                    endGame(null, "양쪽 모두 블랙잭! 무승부!")
                } else {
                    // 플레이어만 블랙잭 -> 플레이어 승리 (2배 보상)
                    endGame(true, "블랙잭!")
                }
            } else {
                // 딜러는 블랙잭 가능성이 낮음 -> 플레이어 승리 (2배 보상)
                showAllDealerCards()
                endGame(true, "블랙잭!")
            }
            isGameOver = true
        } else if (dealerMayHaveBlackjack) {
            // 딜러만 블랙잭 가능성이 있는 경우 확인
            val dealerSecondCard = drawCard()
            dealerCards.add(dealerSecondCard)
            
            if (calculateDealerScore(false) == 21) {
                // 딜러만 블랙잭 -> 딜러 승리
                showAllDealerCards()
                endGame(false, "딜러 블랙잭!")
                isGameOver = true
            }
        }
    }
    
    private fun endGame(isPlayerWin: Boolean?, message: String) {
        isGameActive = false
        hitButton.isEnabled = false
        standButton.isEnabled = false
        
        // 결과에 따라 자산 업데이트 및 메시지 표시
        val snackbarColor: Int
        var additionalMessage = ""
        
        when (isPlayerWin) {
            true -> {
                // 승리
                var payout = 0L
                
                // 플레이어 점수에 따른 배당률
                payout = when {
                    playerCards.size == 2 && calculatePlayerScore() == 21 -> {
                        // 블랙잭 (첫 2장으로 21점)
                        additionalMessage = " 배당률 2배가 적용됩니다."
                        currentBet * 3 // 배팅액의 2배 수익 + 원금
                    }
                    calculatePlayerScore() == 21 -> {
                        // 일반 21점
                        additionalMessage = " 배당률 1.5배가 적용됩니다."
                        (currentBet * 25) / 10 // 배팅액의 1.5배 수익 + 원금
                    }
                    else -> {
                        // 일반 승리
                        currentBet * 2 // 배팅액의 1배 수익 + 원금
                    }
                }
                
                assetViewModel.increaseAsset(payout)
                winCount++
                
                val winAmount = payout - currentBet
                additionalMessage += " +${formatCurrency(winAmount)}"
                snackbarColor = Color.argb(200, 76, 175, 80) // 녹색
            }
            false -> {
                // 패배
                loseCount++
                additionalMessage = " -${formatCurrency(currentBet)}"
                snackbarColor = Color.argb(200, 244, 67, 54) // 빨간색
            }
            null -> {
                // 무승부 (베팅액 반환)
                assetViewModel.increaseAsset(currentBet)
                additionalMessage = " 베팅액이 반환되었습니다."
                snackbarColor = Color.argb(200, 33, 150, 243) // 파란색
            }
        }
        
        // 결과 표시
        showResultSnackbar("$message$additionalMessage", snackbarColor)
        
        // 통계 업데이트
        updateBalanceText()
        winLoseText.text = "승/패: $winCount/$loseCount"
        
        // 베팅 초기화
        currentBet = 0L
    }
    
    private fun updateBalanceText() {
        val currentAsset = assetViewModel.asset.value ?: 0L
        balanceText.text = "잔액: ${formatCurrency(currentAsset)}"
    }
    
    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return formatter.format(amount)
    }
    
    private fun showCustomSnackbar(message: String) {
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(Color.argb(200, 33, 33, 33))
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
        }
        snackbar.show()
    }
    
    private fun showResultSnackbar(message: String, backgroundColor: Int) {
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.setBackgroundColor(backgroundColor)
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
        }
        snackbar.show()
    }
} 