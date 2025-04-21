package com.example.p20

import android.graphics.Color
import android.media.MediaPlayer
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
import androidx.fragment.app.activityViewModels
import java.text.NumberFormat
import java.util.Locale
import java.util.Random
import android.os.Handler
import android.os.Looper
import android.content.Context
import androidx.lifecycle.Observer
import android.graphics.drawable.Drawable
import com.example.p20.helpers.ButtonHelper

// Fragment를 BaseFragment로 변경
class BlackjackFragment : BaseFragment() {

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
    private lateinit var bet500kButton: Button    // 50만원 버튼 추가
    private lateinit var statsTextView: TextView   // 승률 통계 표시용 TextView 추가
    
    // SoundManager 인스턴스
    private lateinit var soundManager: SoundManager
    
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
    
    // 카드 관련 변수 - 불변 값으로 설정
    private val suits = listOf("♠", "♥", "♦", "♣")
    private val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    
    // 카드 덱과 손패 - 미리 용량 할당
    private val deck = ArrayList<Card>(52)
    private val playerCards = ArrayList<Card>(10)
    private val dealerCards = ArrayList<Card>(10)
    
    // 공유 자원 캐싱
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
    private var cleanupRunnable: Runnable? = null
    
    // 재사용 가능한 카드 배경 드로어블
    private lateinit var cardBackgroundDrawable: Drawable
    
    // 데이터 클래스 최적화
    data class Card(val rank: String, val suit: String) {
        // 텍스트 값을 한 번만 계산
        private val _toString = "$rank$suit"
        
        override fun toString(): String = _toString
        
        // 값을 즉시 계산해서 저장
        val value = cardValues[rank] ?: 0
    }
    
    // 효과음 리소스 ID
    companion object {
        // 카드 값 매핑을 companion object로 이동
        private val cardValues = mapOf(
            "A" to 11, "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9,
            "10" to 10, "J" to 10, "Q" to 10, "K" to 10
        )
        
        // 효과음 리소스 ID
        private val SOUND_BETTING = R.raw.casino_betting
        private val SOUND_CARD = R.raw.casino_card_receive
        private val SOUND_START_GAME = R.raw.casino_start
        private val SOUND_WIN = R.raw.casino_win
        private val SOUND_LOSE = R.raw.casino_lose
        private val SOUND_BUTTON = R.raw.casino_card_select
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
        bet500kButton = view.findViewById(R.id.bet500kButton)  // 50만원 버튼 초기화
        statsTextView = view.findViewById(R.id.statsTextView)
        
        // SoundManager 초기화
        soundManager = SoundManager.getInstance(requireContext())
        
        // 카드 배경 드로어블 초기화
        cardBackgroundDrawable = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)!!
        
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
        showMessage("배팅 후 블랙잭 게임을 시작해주세요!")
        
        // 정리 작업 런너블 한 번만 생성
        cleanupRunnable = Runnable {
            if (isWaitingForCleanup) {
                cleanupGame()
            }
        }
    }
    
    /**
     * 게임 버튼 리스너 설정
     */
    private fun setupButtonListeners() {
        // 게임 버튼
        val gameButtonsMap = mapOf<Button, () -> Unit>(
            hitButton to { onHitButtonClicked() },
            standButton to { onStandButtonClicked() },
            doubleDownButton to { onDoubleDownButtonClicked() },
            newGameButton to { onNewGameButtonClicked() }
        )
        
        // 베팅 버튼
        val betButtonsMap = mapOf<Button, Long>(
            bet10kButton to 10_000L,
            bet50kButton to 50_000L,
            bet100kButton to 100_000L,
            bet500kButton to 500_000L
        )
        
        // ButtonHelper를 사용하여 게임 버튼 설정
        com.example.p20.helpers.ButtonHelper.setupButtons(gameButtonsMap, soundManager, SoundManager.SOUND_BLACKJACK_BUTTON)
        
        // ButtonHelper를 사용하여 베팅 버튼 설정
        com.example.p20.helpers.ButtonHelper.setupBettingButtons(
            betButtonsMap,
            { amount -> placeBet(amount) },
            soundManager,
            SoundManager.SOUND_BLACKJACK_BET
        )
        
        // 베팅 금액 초기화 기능 (0가 아닐 때 bet10kButton 길게 누르면 초기화)
        com.example.p20.helpers.ButtonHelper.setLongClickListener(bet10kButton) {
            if (tempBetAmount > 0 && !isGameActive) {
                tempBetAmount = 0L
                updateBetAmountText()
                showMessage("베팅 금액이 초기화되었습니다.")
                return@setLongClickListener true
            }
            false
        }
    }
    
    private fun addBet(amount: Long) {
        if (isGameActive) {
            showMessage("게임 진행 중에는 베팅할 수 없습니다.")
            return
        }
        
        if (isWaitingForCleanup) {
            showMessage("이전 게임 정리 중입니다. 잠시 기다려주세요.")
            return
        }
        
        val currentAsset = assetViewModel.asset.value ?: 0L
        if (tempBetAmount + amount > currentAsset) {
            showMessage("보유 자산을 초과하는 금액을 베팅할 수 없습니다.")
            return
        }
        
        // 금액 추가
        tempBetAmount += amount
        updateBetAmountText()
        
        // 메시지 표시
        showMessage("베팅 금액: ${formatCurrency(tempBetAmount)}")
    }
    
    private fun updateBetAmountText() {
        betAmountText.text = when {
            isGameActive && hasDoubledDown -> "베팅 금액: ${formatCurrency(currentBet)} (더블다운)"
            isGameActive -> "베팅 금액: ${formatCurrency(currentBet)}"
            else -> "베팅 금액: ${formatCurrency(tempBetAmount)}"
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
        // 초기 용량 지정하여 효율적으로 덱 만들기
        deck.ensureCapacity(52)
        
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
        
        // 카드 받기 효과음 재생
        playCardSound()
        
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
        
        // 재사용 가능한 드로어블 사용
        cardView.background = cardBackgroundDrawable.constantState?.newDrawable()
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
            // 카드 값 직접 접근으로 변경
            score += card.value
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
            // 카드 값 직접 접근으로 변경
            score += card.value
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
            val dealerHasAceOrTen = dealerFirstCardRank == "A" || dealerCards[0].value == 10
            
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
        
        var message: String
        var rewardAmount = 0L
        
        if (isDraw) {
            message = "무승부입니다. 베팅액이 반환됩니다."
            rewardAmount = currentBet
            drawCount++
        } else if (playerWins) {
            // 승리 효과음 재생
            playWinSound()
            
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
            
            // 승리 시 아이템 획득 처리
            processItemReward(currentBet)
            
            // 승리 애니메이션 표시
            val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            val winMessage = if (isBlackjack) "블랙잭!" else "승리!"
            val amountText = "+${formatCurrency(rewardAmount - currentBet)}"
            CasinoAnimationManager.showWinAnimation(rootView, winMessage, amountText)
        } else {
            // 패배 효과음 재생
            playLoseSound()
            
            message = "패배했습니다. 베팅액을 잃었습니다."
            loseCount++
            rewardAmount = 0
            
            // 패배 애니메이션 표시
            val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
            val loseMessage = if (dealerScore == 21 && dealerCards.size == 2) 
                "딜러 블랙잭!" 
            else 
                "플레이어: $playerScore vs 딜러: $dealerScore"
            CasinoAnimationManager.showLoseAnimation(rootView, loseMessage, "-${formatCurrency(currentBet)}")
        }
        
        // 통계 저장 및 표시 업데이트
        saveStats()
        updateStatsDisplay()
        
        // 보상 지급
        if (rewardAmount > 0) {
            assetViewModel.increaseAsset(rewardAmount)
        }
        
        // 게임 결과 메시지 표시
        showMessage(message)
        
        // 잔액 업데이트
        updateBalanceText()
        
        // 정리 플래그 설정
        isWaitingForCleanup = true
        
        // 정리 작업 지연 (애니메이션을 위해 더 긴 시간 대기)
        postDelayed(3000) {
            if (isWaitingForCleanup) {
                cleanupGame()
            }
        }
    }
    
    /**
     * 승리 시 아이템 획득 처리
     */
    private fun processItemReward(betAmount: Long) {
        // 아이템 획득 처리 (블랙잭은 gameType 1)
        val itemReward = ItemUtil.processCasinoWin(requireContext(), betAmount, 1)
        
        // 아이템을 획득했으면 메시지 표시
        itemReward?.let {
            // 0.5초 지연 후 아이템 획득 메시지 표시 (기존 승리 메시지와 겹치지 않게)
            postDelayed(1500) {
                showMessage("🎁 ${it.itemName} 아이템을 획득했습니다!")
            }
        }
    }
    
    private fun updateBalanceText() {
        // UI 요소 제거됨 - 메서드만 유지
    }
    
    private fun showCustomSnackbar(message: String) {
        showMessage(message)
    }
    
    // 승률 통계 저장 - 메모리 최적화
    private fun saveStats() {
        requireActivity().getSharedPreferences("blackjack_stats", Context.MODE_PRIVATE)
            .edit()
            .putInt("win_count", winCount)
            .putInt("lose_count", loseCount)
            .putInt("draw_count", drawCount)
            .apply()
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

    // 게임 정리 메서드
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
        showMessage("새 게임을 위해 베팅해주세요")
    }

    private fun initSounds() {
        // SoundManager에서 필요한 효과음 미리 로드
        soundManager.loadSound(SOUND_BETTING)
        soundManager.loadSound(SOUND_CARD)
        soundManager.loadSound(SOUND_START_GAME)
        soundManager.loadSound(SOUND_WIN)
        soundManager.loadSound(SOUND_LOSE)
        soundManager.loadSound(SOUND_BUTTON)
    }
    
    private fun playBettingSound() {
        soundManager.playSound(SOUND_BETTING)
    }
    
    private fun playCardSound() {
        soundManager.playSound(SOUND_CARD)
    }
    
    private fun playStartGameSound() {
        soundManager.playSound(SOUND_START_GAME)
    }
    
    private fun playWinSound() {
        soundManager.playSound(SOUND_WIN)
    }
    
    private fun playLoseSound() {
        soundManager.playSound(SOUND_LOSE)
    }
    
    private fun playButtonSound() {
        soundManager.playSound(SOUND_BUTTON)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // 정리 작업
        cleanupRunnable = null
    }

    /**
     * 히트 버튼 클릭 처리
     */
    private fun onHitButtonClicked() {
        if (!isGameActive) {
            showMessage("게임이 진행 중이 아닙니다.")
            return
        }
        
        playerHit()
        
        // 히트 후 더블다운 비활성화
        doubleDownButton.isEnabled = false
    }
    
    /**
     * 스탠드 버튼 클릭 처리
     */
    private fun onStandButtonClicked() {
        if (!isGameActive) {
            showMessage("게임이 진행 중이 아닙니다.")
            return
        }
        
        // 멈춤 효과음 재생
        playButtonSound()
        
        playerStand()
    }
    
    /**
     * 더블다운 버튼 클릭 처리
     */
    private fun onDoubleDownButtonClicked() {
        if (!isGameActive) {
            showMessage("게임이 진행 중이 아닙니다.")
            return
        }
        
        val currentAsset = assetViewModel.asset.value ?: 0L
        if (currentBet > currentAsset) {
            showMessage("더블다운할 만큼의 자산이 부족합니다.")
            return
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
        postDelayed(500) {
            if (isGameActive && !isGameOver) {
                playerStand()
            }
        }
    }
    
    /**
     * 새 게임 버튼 클릭 처리
     */
    private fun onNewGameButtonClicked() {
        if (isGameActive) {
            showMessage("현재 게임이 진행 중입니다.")
            return
        }
        
        if (tempBetAmount <= 0) {
            showMessage("베팅 금액을 설정해주세요.")
            return
        }
        
        val currentAsset = assetViewModel.asset.value ?: 0L
        if (tempBetAmount > currentAsset) {
            showMessage("베팅 금액이 보유 자산을 초과합니다.")
            return
        }
        
        // 베팅 금액 설정 및 자산 감소
        currentBet = tempBetAmount
        tempBetAmount = 0L
        assetViewModel.decreaseAsset(currentBet)
        updateBalanceText()
        updateBetAmountText()
        
        // 새 게임 효과음 재생
        playStartGameSound()
        
        // 게임 시작
        startNewGame()
    }
    
    /**
     * 베팅 처리
     */
    private fun placeBet(amount: Long) {
        if (isGameActive) {
            showMessage("게임 진행 중에는 베팅할 수 없습니다.")
            return
        }
        
        // 베팅 효과음 재생
        playBettingSound()
        
        addBet(amount)
    }
} 