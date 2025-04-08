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

class PokerFragment : Fragment() {

    // UI 컴포넌트
    private lateinit var playerCardsLayout: LinearLayout
    private lateinit var handRankText: TextView
    private lateinit var betAmountText: TextView
    private lateinit var changeButton: Button
    private lateinit var endGameButton: Button
    private lateinit var newGameButton: Button
    private lateinit var bet10kButton: Button
    private lateinit var bet50kButton: Button
    private lateinit var bet100kButton: Button
    
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
    
    // 선택된 카드 추적
    private val selectedCardIndices = mutableSetOf<Int>()
    private val cardViews = mutableListOf<TextView>()
    
    // 카드 관련 변수
    private val suits = listOf("♠", "♥", "♦", "♣")
    private val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
    
    // 카드 덱과 손패
    private val deck = mutableListOf<Card>()
    private val playerCards = mutableListOf<Card>()
    
    // 카드 교체 기본 비용
    private val baseCostForChange = 50000L
    
    // ViewModel 공유
    private val assetViewModel: AssetViewModel by activityViewModels()

    companion object {
        // 카드 랭크 값 매핑
        val rankValues = mapOf(
            "2" to 2, "3" to 3, "4" to 4, "5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9,
            "10" to 10, "J" to 11, "Q" to 12, "K" to 13, "A" to 14
        )
    }

    data class Card(val rank: String, val suit: String) {
        override fun toString(): String {
            return "$rank$suit"
        }
        
        fun value(): Int = PokerFragment.rankValues[rank] ?: 0
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
        changeButton = view.findViewById(R.id.changeButton)
        endGameButton = view.findViewById(R.id.endGameButton)
        newGameButton = view.findViewById(R.id.newGameButton)
        bet10kButton = view.findViewById(R.id.bet10kButton)
        bet50kButton = view.findViewById(R.id.bet50kButton)
        bet100kButton = view.findViewById(R.id.bet100kButton)
        
        // 잔액 업데이트
        updateBalanceText()
        updateBetAmountText()
        
        // 버튼 이벤트 리스너 설정
        setupButtonListeners()
        
        // 환영 메시지 표시
        showCustomSnackbar("배팅 후 1인포커 게임을 시작해주세요!")
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
            if (isGameActive && !isCardChanged) {
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
            
            // 게임 초기화 및 시작
            startNewGame()
            // 바로 카드 배포
            dealCards()
        }
        
        // 카드 교체 버튼
        changeButton.setOnClickListener {
            if (!isGameActive) {
                showCustomSnackbar("게임이 시작되지 않았습니다.")
                return@setOnClickListener
            }
            
            if (!isCardDealt) {
                showCustomSnackbar("먼저 카드를 받아야 합니다.")
                return@setOnClickListener
            }
            
            changeCards()
        }
        
        // 게임 종료 버튼
        endGameButton.setOnClickListener {
            if (!isGameActive) {
                showCustomSnackbar("게임이 시작되지 않았습니다.")
                return@setOnClickListener
            }
            
            if (!isCardDealt) {
                showCustomSnackbar("먼저 카드를 받아야 합니다.")
                return@setOnClickListener
            }
            
            // 족보 평가 후 게임 종료
            evaluateHand()
            endGame()
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
    }
    
    private fun startNewGame() {
        // 게임 상태 초기화
        isGameActive = true
        isCardDealt = true  // 바로 카드를 받으므로 true로 설정
        isCardChanged = false
        changeCount = 0  // 카드 교체 횟수 초기화
        playerCards.clear()
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        selectedCardIndices.clear()
        handRankText.text = "패 없음"
        
        // 덱 생성 및 섞기
        createShuffledDeck()
        
        // 버튼 활성화
        changeButton.isEnabled = true
        endGameButton.isEnabled = true
        
        // 교체 버튼 텍스트 업데이트
        updateChangeButtonText()
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
        // 카드 초기 배포 (5장)
        playerCardsLayout.removeAllViews()
        playerCards.clear()
        cardViews.clear()
        selectedCardIndices.clear()
        
        for (i in 0 until 5) {
            val card = drawCard()
            playerCards.add(card)
            addCardView(playerCardsLayout, card, i)
        }
        
        isCardDealt = true
        
        // 족보 평가
        evaluateHand()
    }
    
    private fun changeCards() {
        // 카드 교체 비용 확인 (첫 번째는 무료)
        val changeCost = getChangeCost()
        
        // 교체 횟수가 0보다 크면 비용 지불
        if (changeCount > 0) {
            val currentAsset = assetViewModel.asset.value ?: 0L
            if (currentAsset < changeCost) {
                showCustomSnackbar("카드 교체 비용(${formatCurrency(changeCost)})이 부족합니다.")
                return
            }
            
            // 교체 비용 지불
            assetViewModel.decreaseAsset(changeCost)
            updateBalanceText()
        }
        
        // 선택한 카드 교체
        if (selectedCardIndices.isEmpty()) {
            showCustomSnackbar("교체할 카드를 선택하세요.")
            return
        }
        
        // 선택한 카드 교체
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        
        for (i in 0 until 5) {
            if (i in selectedCardIndices) {
                val newCard = drawCard()
                playerCards[i] = newCard
            }
            addCardView(playerCardsLayout, playerCards[i], i)
        }
        
        // 교체 횟수 증가 및 상태 업데이트
        changeCount++
        isCardChanged = true
        
        // 선택 초기화
        selectedCardIndices.clear()
        
        // 교체 버튼 텍스트 업데이트
        updateChangeButtonText()
        
        // 족보 재평가
        evaluateHand()
        
        // 교체 완료 메시지
        if (changeCount < 10) { // 너무 많은 교체를 방지하기 위한 상한선
            val nextCost = getChangeCost()
            showCustomSnackbar("카드 교체 완료. 다음 교체 비용: ${formatCurrency(nextCost)}")
        } else {
            showCustomSnackbar("최대 교체 횟수에 도달했습니다. 게임을 종료해주세요.")
            changeButton.isEnabled = false
        }
    }
    
    private fun drawCard(): Card {
        if (deck.isEmpty()) {
            createShuffledDeck()
        }
        return deck.removeAt(0)
    }
    
    private fun addCardView(container: LinearLayout, card: Card, index: Int) {
        val cardView = TextView(requireContext())
        cardView.layoutParams = LinearLayout.LayoutParams(
            (resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) * 0.95f).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            marginEnd = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width) / 20
        }
        
        cardView.background = ContextCompat.getDrawable(requireContext(), android.R.drawable.btn_default)
        cardView.gravity = Gravity.CENTER
        cardView.textSize = 20f
        cardView.setPadding(12, 12, 12, 12)
        
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
            if (!isGameActive || isWaitingForCleanup) return@setOnClickListener
            
            val cardIndex = it.tag as Int
            
            if (selectedCardIndices.contains(cardIndex)) {
                // 선택 해제
                selectedCardIndices.remove(cardIndex)
                cardView.alpha = 1.0f
                cardView.setBackgroundResource(android.R.drawable.btn_default)
            } else {
                // 선택
                selectedCardIndices.add(cardIndex)
                cardView.alpha = 0.7f
                cardView.setBackgroundResource(android.R.drawable.button_onoff_indicator_on)
            }
        }
        
        container.addView(cardView)
    }
    
    private fun evaluateHand(): HandRank {
        if (playerCards.size < 5) return HandRank.NONE
        
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
        
        return handRank
    }
    
    private fun isRoyalStraightFlush(): Boolean {
        if (!isFlush() || !isStraight()) return false
        
        val values = playerCards.map { it.value() }.sorted()
        return values == listOf(10, 11, 12, 13, 14)  // 10, J, Q, K, A
    }
    
    private fun isFlush(): Boolean {
        val firstSuit = playerCards[0].suit
        return playerCards.all { it.suit == firstSuit }
    }
    
    private fun isStraight(): Boolean {
        val values = playerCards.map { it.value() }.sorted()
        
        // 보통 스트레이트 (연속된 5개 숫자)
        if (values[4] - values[0] == 4 && values.toSet().size == 5) return true
        
        // A-2-3-4-5 스트레이트
        return values == listOf(2, 3, 4, 5, 14)
    }
    
    private fun isFourOfAKind(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        return rankGroups.any { it.value.size >= 4 }
    }
    
    private fun isFullHouse(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        return rankGroups.size == 2 && rankGroups.any { it.value.size == 3 }
    }
    
    private fun isThreeOfAKind(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        return rankGroups.any { it.value.size >= 3 }
    }
    
    private fun isTwoPair(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        val pairCount = rankGroups.count { it.value.size >= 2 }
        return pairCount >= 2
    }
    
    private fun isPair(): Boolean {
        val rankGroups = playerCards.groupBy { it.rank }
        return rankGroups.any { it.value.size >= 2 }
    }
    
    private fun getMultiplier(handRank: HandRank): Int {
        return when(handRank) {
            HandRank.ROYAL_STRAIGHT_FLUSH -> 100
            HandRank.STRAIGHT_FLUSH -> 50
            HandRank.FOUR_OF_A_KIND -> 20
            HandRank.FULL_HOUSE -> 10
            HandRank.FLUSH -> 5
            HandRank.STRAIGHT -> 4
            HandRank.THREE_OF_A_KIND -> 3
            HandRank.TWO_PAIR -> 2
            HandRank.ONE_PAIR -> 1
            else -> 0
        }
    }
    
    private fun endGame() {
        isGameActive = false
        isWaitingForCleanup = true
        changeButton.isEnabled = false
        endGameButton.isEnabled = false
        
        // 배팅 버튼 비활성화
        bet10kButton.isEnabled = false
        bet50kButton.isEnabled = false
        bet100kButton.isEnabled = false
        newGameButton.isEnabled = false
        
        // 선택된 카드 초기화
        selectedCardIndices.clear()
        
        // 족보 평가
        val handRank = evaluateHand()
        val multiplier = getMultiplier(handRank)
        
        // 배당금 계산
        val payout = currentBet * multiplier
        val snackbarColor: Int
        val resultMessage: String
        
        if (multiplier > 0) {
            // 승리
            assetViewModel.increaseAsset(payout)
            winCount++
            resultMessage = "축하합니다! ${handRank.koreanName}으로 ${multiplier}배 획득! +${formatCurrency(payout - currentBet)}"
            snackbarColor = Color.argb(200, 76, 175, 80) // 녹색
        } else {
            // 패배
            loseCount++
            resultMessage = "아쉽습니다. 패가 없어 베팅금을 잃었습니다. -${formatCurrency(currentBet)}"
            snackbarColor = Color.argb(200, 244, 67, 54) // 빨간색
        }
        
        // 결과 표시
        showResultSnackbar(resultMessage, snackbarColor)
        
        // 통계 업데이트
        updateBalanceText()
        
        // 베팅 초기화
        currentBet = 0L
        
        // 3초 후에 카드 지우기
        Handler(Looper.getMainLooper()).postDelayed({
            playerCardsLayout.removeAllViews()
            cardViews.clear()
            
            // 배팅 버튼 다시 활성화
            bet10kButton.isEnabled = true
            bet50kButton.isEnabled = true
            bet100kButton.isEnabled = true
            newGameButton.isEnabled = true
            
            // 정리 대기 상태 해제
            isWaitingForCleanup = false
            
            showCustomSnackbar("새 게임을 위해 베팅해주세요")
        }, 3000) // 3초 지연
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
    
    // 카드 교체 비용 계산 함수
    private fun getChangeCost(): Long {
        return if (changeCount == 0) 0L else baseCostForChange * changeCount
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
} 