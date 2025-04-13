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
import android.content.Context

class BlackjackFragment : Fragment() {

    // UI 컴포넌트
    private lateinit var dealerCardsLayout: LinearLayout
    private lateinit var playerCardsLayout: LinearLayout
    private lateinit var dealerScoreText: TextView
    private lateinit var playerScoreText: TextView
    private lateinit var betAmountText: TextView
    private lateinit var hitButton: Button
    private lateinit var standButton: Button
    private lateinit var doubleDownButton: Button  // 더블다운 버튼 추가
    private lateinit var newGameButton: Button
    private lateinit var bet10kButton: Button
    private lateinit var bet50kButton: Button
    private lateinit var bet100kButton: Button
    private lateinit var statsTextView: TextView   // 승률 통계 표시용 TextView 추가
    
    // 게임 상태
    private var currentBet = 0L
    private var tempBetAmount = 0L
    private var isGameActive = false
    private var isGameOver = false
    private var winCount = 0
    private var loseCount = 0
    private var drawCount = 0  // 무승부 카운트 추가
    private var isWaitingForCleanup = false
    private var hasDoubledDown = false  // 더블다운 사용 여부 플래그
    
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
        doubleDownButton = view.findViewById(R.id.doubleDownButton)  // 더블다운 버튼 초기화
        newGameButton = view.findViewById(R.id.newGameButton)
        bet10kButton = view.findViewById(R.id.bet10kButton)
        bet50kButton = view.findViewById(R.id.bet50kButton)
        bet100kButton = view.findViewById(R.id.bet100kButton)
        statsTextView = view.findViewById(R.id.statsTextView)
        
        // 게임 버튼 초기 비활성화
        hitButton.isEnabled = false
        standButton.isEnabled = false
        doubleDownButton.isEnabled = false
        
        // 저장된 통계 로드
        loadStats()
        
        // 통계 표시
        updateStatsDisplay()
        
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
            
            // 히트 후 더블다운 비활성화
            doubleDownButton.isEnabled = false
        }
        
        // 스탠드 버튼 - 턴 종료
        standButton.setOnClickListener {
            if (!isGameActive) {
                showCustomSnackbar("게임이 진행 중이 아닙니다.")
                return@setOnClickListener
            }
            
            playerStand()
        }
        
        // 더블다운 버튼 - 베팅 2배 및 카드 1장 추가
        doubleDownButton.setOnClickListener {
            if (!isGameActive) {
                showCustomSnackbar("게임이 진행 중이 아닙니다.")
                return@setOnClickListener
            }
            
            val currentAsset = assetViewModel.asset.value ?: 0L
            if (currentBet > currentAsset) {
                showCustomSnackbar("더블다운할 만큼의 자산이 부족합니다.")
                return@setOnClickListener
            }
            
            // 자산에서 추가 베팅액 차감
            assetViewModel.decreaseAsset(currentBet)
            
            // 베팅액 2배로 증가
            currentBet *= 2
            updateBalanceText()
            updateBetAmountText()
            
            // 더블다운 사용 플래그 설정
            hasDoubledDown = true
            
            // 카드 1장 추가 후 턴 종료
            playerHit()
            
            // 버튼 비활성화
            doubleDownButton.isEnabled = false
            hitButton.isEnabled = false
            
            // 0.5초 후 자동으로 스탠드
            Handler(Looper.getMainLooper()).postDelayed({
                if (isGameActive && !isGameOver) {
                    playerStand()
                }
            }, 500)
        }
    }
    
    private fun addBet(amount: Long) {
        if (isGameActive) {
            showCustomSnackbar("게임 진행 중에는 베팅할 수 없습니다.")
            return
        }
        
        if (isWaitingForCleanup) {
            showCustomSnackbar("이전 게임 정리 중입니다. 잠시 기다려주세요.")
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
        if (isGameActive && !hasDoubledDown) {
            betAmountText.text = "베팅 금액: ${formatCurrency(currentBet)}"
        } else if (isGameActive && hasDoubledDown) {
            betAmountText.text = "베팅 금액: ${formatCurrency(currentBet)} (더블다운)"
        }
    }
    
    private fun startNewGame() {
        // 게임 상태 초기화
        isGameActive = true
        isGameOver = false
        hasDoubledDown = false
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
        
        // 더블다운 버튼 활성화 (초기 2장만 받은 상태)
        val currentAsset = assetViewModel.asset.value ?: 0L
        doubleDownButton.isEnabled = currentBet <= currentAsset
        
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
        val newCard = drawCard()
        playerCards.add(newCard)
        addCardView(playerCardsLayout, newCard, playerCards.size - 1)
        
        // 점수 업데이트
        val score = calculatePlayerScore()
        playerScoreText.text = "점수: $score"
        
        // 버스트 체크
        if (score > 21) {
            endGame(playerWins = false)
        }
    }
    
    private fun playerStand() {
        if (isGameOver) return
        
        // 딜러 턴 실행
        dealerTurn()
    }
    
    private fun dealerTurn() {
        // 딜러의 첫 번째 카드는 이미 보이는 상태
        
        // 딜러 규칙: 16 이하면 무조건 히트, 17 이상이면 스탠드
        while (calculateDealerScore(false) < 17) {
            val newCard = drawCard()
            dealerCards.add(newCard)
        }
        
        // 점수 계산
        val dealerScore = calculateDealerScore(false)
        val playerScore = calculatePlayerScore()
        
        // 결과 판정
        if (dealerScore > 21) {
            // 딜러 버스트
            endGame(playerWins = true)
        } else if (dealerScore > playerScore) {
            // 딜러 점수가 높음
            endGame(playerWins = false)
        } else if (dealerScore < playerScore) {
            // 플레이어 점수가 높음
            endGame(playerWins = true)
        } else {
            // 동점 처리
            endGame(playerWins = false, isDraw = true)
        }
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
        
        // 카드 너비 계산 (전체 화면 너비를 6등분하여 카드 크기 결정)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val cardWidth = screenWidth / 7  // 7로 나누면 약간의 여유 공간까지 확보
        
        cardView.layoutParams = LinearLayout.LayoutParams(
            cardWidth,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = cardWidth / 15  // 카드 간격 조정
        }
        
        cardView.background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        cardView.gravity = Gravity.CENTER
        cardView.textSize = 24f  // 텍스트 크기 증가
        cardView.setPadding(12, 12, 12, 12)  // 패딩 증가
        
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
        val playerScore = calculatePlayerScore()
        
        if (playerCards.size == 2 && playerScore == 21) {
            // 딜러 첫 카드가 A나 10점 카드인지 확인
            val dealerFirstCardRank = dealerCards[0].rank
            val dealerHasAceOrTen = dealerFirstCardRank == "A" || values[dealerFirstCardRank] == 10
            
            if (dealerHasAceOrTen) {
                // 딜러의 두 번째 카드 확인 (블랙잭 가능성)
                val secondCard = drawCard()
                dealerCards.add(secondCard)
                
                val dealerScore = calculateDealerScore(false)
                
                if (dealerScore == 21) {
                    // 양쪽 모두 블랙잭 - 무승부
                    endGame(playerWins = false, isDraw = true)
                } else {
                    // 플레이어만 블랙잭 - 승리
                    endGame(playerWins = true)
                }
            } else {
                // 딜러 블랙잭 가능성 없음 - 플레이어 블랙잭 승리
                endGame(playerWins = true)
            }
        }
    }
    
    private fun endGame(playerWins: Boolean, isDraw: Boolean = false) {
        isGameActive = false
        isGameOver = true
        hitButton.isEnabled = false
        standButton.isEnabled = false
        doubleDownButton.isEnabled = false
        
        // 뒤집어진 카드 공개
        dealerCardsLayout.removeAllViews()
        for (i in dealerCards.indices) {
            addCardView(dealerCardsLayout, dealerCards[i], i)
        }
        
        // 최종 점수 표시
        dealerScoreText.text = "점수: ${calculateDealerScore(false)}"
        
        // 메시지 생성
        val playerScore = calculatePlayerScore()
        val dealerScore = calculateDealerScore(false)
        
        var message = ""
        var rewardAmount = 0L
        
        if (isDraw) {
            message = "무승부입니다. 베팅액이 반환됩니다."
            rewardAmount = currentBet
            drawCount++
        } else if (playerWins) {
            val isBlackjack = playerScore == 21 && playerCards.size == 2
            val multiplier = if (isBlackjack) 2.5 else 2.0  // 블랙잭은 2.5배
            rewardAmount = (currentBet * multiplier).toLong()
            
            val reward = formatCurrency(rewardAmount)
            message = if (isBlackjack) {
                "블랙잭! $reward 획득! (2.5배)"
            } else {
                "$reward 획득! (2배)"
            }
            winCount++
        } else {
            message = "패배했습니다. 베팅액을 잃었습니다."
            loseCount++
            rewardAmount = 0
        }
        
        // 통계 저장 및 표시 업데이트
        saveStats()
        updateStatsDisplay()
        
        // 보상 지급
        if (rewardAmount > 0) {
            assetViewModel.increaseAsset(rewardAmount)
        }
        
        // 게임 결과 메시지 표시
        showCustomSnackbar(message)
        
        // 잔액 업데이트
        updateBalanceText()
        
        // 정리 플래그 설정
        isWaitingForCleanup = true
        
        // 3초 후 UI 정리
        Handler(Looper.getMainLooper()).postDelayed({
            if (isWaitingForCleanup) {
                cleanupGame()
            }
        }, 3000)
    }
    
    private fun updateBalanceText() {
        // UI 요소 제거됨 - 메서드만 유지
    }
    
    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return formatter.format(amount)
    }
    
    private fun showCustomSnackbar(message: String) {
        MessageManager.showMessage(requireContext(), message)
    }
    
    private fun showResultSnackbar(message: String, backgroundColor: Int) {
        // 결과 메시지도 상단 메시지로 표시 (배경색 정보 무시)
        MessageManager.showMessage(requireContext(), message)
    }

    // 승률 통계 저장
    private fun saveStats() {
        val prefs = requireActivity().getSharedPreferences("blackjack_stats", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("win_count", winCount)
            putInt("lose_count", loseCount)
            putInt("draw_count", drawCount)
            apply()
        }
    }
    
    // 승률 통계 로드
    private fun loadStats() {
        val prefs = requireActivity().getSharedPreferences("blackjack_stats", Context.MODE_PRIVATE)
        winCount = prefs.getInt("win_count", 0)
        loseCount = prefs.getInt("lose_count", 0)
        drawCount = prefs.getInt("draw_count", 0)
    }
    
    // 승률 표시 업데이트
    private fun updateStatsDisplay() {
        val totalGames = winCount + loseCount + drawCount
        val winRate = if (totalGames > 0) (winCount.toFloat() / totalGames * 100).toInt() else 0
        
        statsTextView.text = "승률: $winRate% (${winCount}승 ${loseCount}패 ${drawCount}무)"
    }

    // 게임 정리 메서드 추가
    private fun cleanupGame() {
        // 카드 지우기
        dealerCardsLayout.removeAllViews()
        playerCardsLayout.removeAllViews()
        
        // 점수 초기화
        dealerScoreText.text = "점수: ?"
        playerScoreText.text = "점수: 0"
        
        // 베팅 초기화
        currentBet = 0L
        
        // 정리 대기 상태 해제
        isWaitingForCleanup = false
        
        // 게임 준비 메시지
        showCustomSnackbar("새 게임을 위해 베팅해주세요")
    }
} 