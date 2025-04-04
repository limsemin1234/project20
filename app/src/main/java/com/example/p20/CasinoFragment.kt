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

class CasinoFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_casino, container, false)
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
        
        // 카드 초기 배포 (플레이어 2장, 딜러 2장)
        dealInitialCards()
        
        // 버튼 활성화
        hitButton.isEnabled = true
        standButton.isEnabled = true
        
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
    
    private fun dealInitialCards() {
        // 플레이어에게 카드 2장
        for (i in 0 until 2) {
            val card = drawCard()
            playerCards.add(card)
            addCardView(playerCardsLayout, card, false)
        }
        
        // 딜러에게 카드 2장 (첫 번째 카드는 숨김)
        for (i in 0 until 2) {
            val card = drawCard()
            dealerCards.add(card)
            addCardView(dealerCardsLayout, card, i == 0)
        }
        
        // 점수 업데이트
        updateScores(isDealerFirst = true)
    }
    
    private fun drawCard(): Card {
        if (deck.isEmpty()) {
            createShuffledDeck()
        }
        return deck.removeAt(0)
    }
    
    private fun addCardView(container: LinearLayout, card: Card, isHidden: Boolean) {
        val cardView = TextView(requireContext())
        // 카드 크기를 작게 수정
        cardView.layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) * 3/4, // 카드 너비 확장
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) / 20
        }
        
        cardView.background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        cardView.gravity = Gravity.CENTER
        cardView.textSize = 18f // 폰트 크기 증가
        cardView.setPadding(8, 8, 8, 8) // 내부 여백 추가
        
        if (isHidden) {
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
    
    private fun updateScores(isDealerFirst: Boolean = false) {
        val playerScore = calculateScore(playerCards)
        playerScoreText.text = "점수: $playerScore"
        
        if (isDealerFirst) {
            // 딜러의 첫 카드만 점수 계산
            dealerScoreText.text = "점수: ?"
        } else {
            val dealerScore = calculateScore(dealerCards)
            dealerScoreText.text = "점수: $dealerScore"
        }
    }
    
    private fun calculateScore(cards: List<Card>): Int {
        var score = 0
        var aceCount = 0
        
        for (card in cards) {
            val value = values[card.rank] ?: 0
            if (card.rank == "A") {
                aceCount++
            }
            score += value
        }
        
        // A는 상황에 따라 1 또는 11
        while (score > 21 && aceCount > 0) {
            score -= 10 // A를 11에서 1로 변경하는 효과
            aceCount--
        }
        
        return score
    }
    
    private fun checkForBlackjack() {
        val playerScore = calculateScore(playerCards)
        val dealerScore = calculateScore(dealerCards)
        
        if (playerScore == 21 || dealerScore == 21) {
            // 딜러 카드 모두 공개
            revealDealerCards()
            
            if (playerScore == 21 && dealerScore == 21) {
                // 양쪽 다 블랙잭 - 푸시(무승부)
                endGame(null, "양쪽 모두 블랙잭! 무승부입니다.")
            } else if (playerScore == 21) {
                // 플레이어만 블랙잭 - 2배 지급
                endGame(true, "블랙잭!")
            } else {
                // 딜러만 블랙잭 - 패배
                endGame(false, "딜러가 블랙잭을 가졌습니다.")
            }
        }
    }
    
    private fun playerHit() {
        // 플레이어에게 카드 한 장 추가
        val card = drawCard()
        playerCards.add(card)
        addCardView(playerCardsLayout, card, false)
        
        // 점수 업데이트
        val playerScore = calculateScore(playerCards)
        playerScoreText.text = "점수: $playerScore"
        
        // 21 초과하면 버스트
        if (playerScore > 21) {
            endGame(false, "버스트! 21을 초과했습니다.")
        }
    }
    
    private fun playerStand() {
        // 딜러의 모든 카드 공개
        revealDealerCards()
        
        // 딜러 규칙에 따라 카드 뽑기 (17 미만이면 계속 뽑음)
        dealerPlay()
    }
    
    private fun revealDealerCards() {
        // 모든 딜러 카드 보이게 하기
        dealerCardsLayout.removeAllViews()
        for (card in dealerCards) {
            addCardView(dealerCardsLayout, card, false)
        }
        updateScores(isDealerFirst = false)
    }
    
    private fun dealerPlay() {
        // 딜러는 점수가 17 이상될 때까지 카드를 뽑음
        var dealerScore = calculateScore(dealerCards)
        
        // 애니메이션 효과를 위해 Handler 사용
        val handler = Handler(Looper.getMainLooper())
        val drawRunnable = object : Runnable {
            override fun run() {
                if (dealerScore < 17) {
                    val card = drawCard()
                    dealerCards.add(card)
                    addCardView(dealerCardsLayout, card, false)
                    
                    dealerScore = calculateScore(dealerCards)
                    dealerScoreText.text = "점수: $dealerScore"
                    
                    handler.postDelayed(this, 700) // 0.7초 간격으로 카드 뽑기
                } else {
                    // 17 이상이면 게임 종료 및 승패 판정
                    determineWinner()
                }
            }
        }
        
        handler.post(drawRunnable)
    }
    
    private fun determineWinner() {
        val playerScore = calculateScore(playerCards)
        val dealerScore = calculateScore(dealerCards)
        
        val result = when {
            playerScore > 21 -> false // 플레이어 버스트
            dealerScore > 21 -> true // 딜러 버스트
            playerScore > dealerScore -> true // 플레이어 승
            playerScore < dealerScore -> false // 딜러 승
            else -> null // 무승부
        }
        
        val message = when {
            playerScore > 21 -> "버스트! 21을 초과했습니다."
            dealerScore > 21 -> "딜러 버스트! 승리했습니다."
            playerScore > dealerScore -> "플레이어 승리!"
            playerScore < dealerScore -> "딜러 승리!"
            else -> "점수가 같습니다. 무승부!"
        }
        
        endGame(result, message)
    }
    
    private fun endGame(result: Boolean?, message: String) {
        isGameActive = false
        isGameOver = true
        
        hitButton.isEnabled = false
        standButton.isEnabled = false
        
        var payout = 0L
        val outcomeMessage: String
        var snackbarColor = Color.argb(200, 33, 33, 33) // 기본 색상
        
        when (result) {
            true -> {
                // 승리
                val playerScore = calculateScore(playerCards)
                payout = when {
                    // 블랙잭(2장으로 21점) - 2배 지급
                    playerScore == 21 && playerCards.size == 2 -> {
                        currentBet * 3 // 원금 포함 3배 (순수 이득 2배)
                    }
                    // 21점 달성 - 1.5배 지급
                    playerScore == 21 -> {
                        (currentBet * 2.5).toLong() // 원금 포함 2.5배 (순수 이득 1.5배)
                    }
                    // 일반 승리 - 1배 지급
                    else -> {
                        currentBet * 2 // 원금 포함 2배 (순수 이득 1배)
                    }
                }
                
                // 승리 메시지 생성
                val winMessage = when {
                    playerScore == 21 && playerCards.size == 2 -> "블랙잭! 배당률 2배가 적용됩니다."
                    playerScore == 21 -> "21점 달성! 배당률 1.5배가 적용됩니다."
                    else -> "플레이어 승리!"
                }
                
                assetViewModel.increaseAsset(payout)
                winCount++
                outcomeMessage = "$message\n$winMessage +${formatCurrency(payout - currentBet)}"
                snackbarColor = Color.argb(200, 76, 175, 80) // 녹색
            }
            false -> {
                // 패배
                loseCount++
                outcomeMessage = "$message\n패배! -${formatCurrency(currentBet)}"
                snackbarColor = Color.argb(200, 244, 67, 54) // 빨간색
            }
            null -> {
                // 무승부
                assetViewModel.increaseAsset(currentBet) // 베팅금액만 돌려줌
                outcomeMessage = "$message\n무승부! 베팅금액이 반환됩니다."
                snackbarColor = Color.argb(200, 33, 150, 243) // 파란색
            }
        }
        
        // 스낵바로 결과 표시
        showGameResultSnackbar(outcomeMessage, snackbarColor)
        
        updateBalanceText()
        winLoseText.text = "승/패: $winCount/$loseCount"
        
        // 베팅 초기화
        currentBet = 0L
        
        // 2.5초 후에 카드 지우기
        Handler(Looper.getMainLooper()).postDelayed({
            // 카드 비우기
            dealerCardsLayout.removeAllViews()
            playerCardsLayout.removeAllViews()
            
            // 점수 초기화
            dealerScoreText.text = "점수: ?"
            playerScoreText.text = "점수: 0"
            
            // 게임 준비 메시지
            showCustomSnackbar("새 게임을 위해 베팅해주세요")
        }, 2500) // 2.5초 지연
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
    
    private fun showGameResultSnackbar(message: String, backgroundColor: Int) {
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