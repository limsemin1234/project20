package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import java.text.NumberFormat
import java.util.Locale
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AlertDialog
import android.view.View.OnClickListener
import android.util.Log

// Fragment에서 BaseFragment로 상속 변경
class PokerFragment : BaseFragment() {

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
    
    // 노란색 배경을 위한 드로어블 추가
    private val handRankCardDrawable = GradientDrawable().apply {
        setStroke(3, Color.BLACK)
        cornerRadius = 8f
        setColor(Color.argb(255, 255, 255, 150)) // 연한 노란색
    }
    
    // 족보에 해당하는 카드 인덱스 저장용
    private val handRankCardIndices = mutableSetOf<Int>()
    
    // 카드 선택 리스너 재사용
    private val cardClickListener = OnClickListener { view ->
        if (!isGameActive || isWaitingForCleanup) return@OnClickListener
        
        val cardIndex = view.tag as Int
        
        // ViewModel의 toggleCardSelection 메서드 사용
        val result = pokerViewModel.toggleCardSelection(cardIndex)
        if (!result) {
            if (selectedCardIndices.size >= 5) {
                showMessage("최대 5장까지만 선택할 수 있습니다.")
            }
        } else {
            // 효과음 재생
            playCardSelectSound()
        }
        
        // 참고: ViewModel의 옵저버가 UI 업데이트를 처리할 것입니다.
        // 프래그먼트의 selectedCardIndices는 더 이상 직접 업데이트하지 않습니다.
    }
    
    // ViewModel
    private val pokerViewModel: PokerViewModel by viewModels()
    // BaseFragment에 이미 assetViewModel과 timeViewModel이 정의되어 있으므로 제거
    // private val assetViewModel: AssetViewModel by activityViewModels()
    // private val timeViewModel: TimeViewModel by activityViewModels()

    // mainHandler 대신 BaseFragment의 trackHandler/postDelayed 기능 사용
    // private val mainHandler = Handler(Looper.getMainLooper())
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
        
        // 효과음 리소스 ID
        private val SOUND_BETTING = R.raw.casino_card_select
        private val SOUND_CARD = R.raw.casino_card_receive
        private val SOUND_START_GAME = R.raw.casino_start
        private val SOUND_WIN = R.raw.casino_win
        private val SOUND_LOSE = R.raw.casino_lose
        private val SOUND_CARD_SELECT = R.raw.casino_card_select
        private val SOUND_BUTTON = R.raw.casino_stop
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
        showMessage("배팅 후 1인발라트로 게임을 시작해주세요!")

        // 정리 작업을 위한 Runnable 설정
        cleanupRunnable = Runnable {
            if (pokerViewModel.isWaitingForCleanup.value == true) {
                cleanupGame()
            }
        }
    }
    
    // BaseFragment의 onGameOver 메서드 오버라이드
    override fun onGameOver() {
        // 게임 상태 초기화
        isGameActive = false
        updateButtonStates(false)
    }


    private fun setupObservers() {
        // 플레이어 카드 변경 감지
        pokerViewModel.playerCards.observe(viewLifecycleOwner) { viewModelCards ->
            // ViewModel의 Card를 Fragment의 Card로 변환
            val fragmentCards = viewModelCards.map { vmCard ->
                Card(vmCard.rank, vmCard.suit)
            }
            updateCardViews(fragmentCards)
            
            // 카드가 변경되면 족보 확인
            if (isGameActive && fragmentCards.isNotEmpty()) {
                highlightHandRankCards()
            }
        }
        
        // 선택된 카드 변경 감지
        pokerViewModel.selectedCardIndices.observe(viewLifecycleOwner) { indices ->
            updateSelectedCards(indices)
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
                showMessage("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showMessage("게임 진행 중에는 베팅할 수 없습니다.")
            }
        }
        
        bet50kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(50_000L)) {
                showMessage("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showMessage("게임 진행 중에는 베팅할 수 없습니다.")
            }
        }
        
        bet100kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(100_000L)) {
                showMessage("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showMessage("게임 진행 중에는 베팅할 수 없습니다.")
            }
        }
        
        bet500kButton.setOnClickListener { 
            playBettingSound()
            if (pokerViewModel.addBet(500_000L)) {
                showMessage("베팅 금액: ${formatCurrency(pokerViewModel.tempBetAmount.value ?: 0L)}")
            } else {
                showMessage("게임 진행 중에는 베팅할 수 없습니다.")
            }
        }
        
        // 베팅 금액 초기화 기능 (0가 아닐 때 bet10kButton 길게 누르면 초기화)
        bet10kButton.setOnLongClickListener {
            if ((pokerViewModel.tempBetAmount.value ?: 0L) > 0 && pokerViewModel.isGameActive.value != true) {
                pokerViewModel.clearBet()
                showMessage("베팅 금액이 초기화되었습니다.")
                return@setOnLongClickListener true
            }
            false
        }
        
        // 새 게임 버튼
        newGameButton.setOnClickListener { 
            if (pokerViewModel.isGameActive.value == true) {
                showMessage("게임이 이미 진행 중입니다.")
                return@setOnClickListener
            }
            
            if ((pokerViewModel.tempBetAmount.value ?: 0L) <= 0) {
                showMessage("먼저 베팅해주세요.")
                return@setOnClickListener
            }
            
            // 자산 확인
            val currentAsset = assetViewModel.asset.value ?: 0L
            if ((pokerViewModel.tempBetAmount.value ?: 0L) > currentAsset) {
                showMessage("베팅 금액이 보유 자산을 초과합니다.")
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
                    
                // 족보에 해당하는 카드를 노란색으로 표시
                highlightHandRankCards()
                    
                // 사용자에게 알림
                showMessage("카드를 선택해서 최상의 패를 만드세요! 노란색 카드는 족보입니다.")
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
                    showMessage("카드 교체 비용이 부족합니다. 필요 금액: ${formatCurrency(changeCost)}")
                    return@setOnClickListener
                }
            }
            
            // 선택된 카드가 있는지 확인
            if (pokerViewModel.selectedCardIndices.value.isNullOrEmpty()) {
                showMessage("교체할 카드를 선택해주세요.")
                return@setOnClickListener
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
                showMessage(message)
                
                // Fragment의 선택 상태 초기화 (ViewModel에서는 이미 초기화되었음)
                selectedCardIndices.clear()
                
                // 카드 교체 후 족보 확인 (playerCards 업데이트 옵저버에서 처리됨)
            } else {
                showMessage(message)
            }
        }
        
        // 카드 확정 버튼
        endGameButton.setOnClickListener { 
            if (pokerViewModel.selectedCardIndices.value?.size != 5) {
                showMessage("정확히 5장의 카드를 선택해야 합니다.")
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
    
    /**
     * 베팅 효과음을 재생합니다.
     */
    private fun playBettingSound() {
        soundManager.playSound(SOUND_BETTING)
    }
    
    /**
     * 카드 효과음을 재생합니다.
     */
    private fun playCardSound() {
        soundManager.playSound(SOUND_CARD)
    }
    
    /**
     * 게임 시작 효과음을 재생합니다.
     */
    private fun playStartGameSound() {
        soundManager.playSound(SOUND_START_GAME)
    }
    
    /**
     * 승리 효과음을 재생합니다.
     */
    private fun playWinSound() {
        soundManager.playSound(SOUND_WIN)
    }
    
    /**
     * 패배 효과음을 재생합니다.
     */
    private fun playLoseSound() {
        soundManager.playSound(SOUND_LOSE)
    }
    
    /**
     * 카드 선택 효과음을 재생합니다.
     */
    private fun playCardSelectSound() {
        soundManager.playSound(SOUND_CARD_SELECT)
    }
    
    /**
     * 버튼 효과음을 재생합니다.
     */
    private fun playStopSound() {
        soundManager.playSound(SOUND_BUTTON)
    }
    
    // 메시지 표시 메서드를 BaseFragment에서 제공하는 메서드로 교체
    private fun showCustomSnackbar(message: String) {
        showMessage(message)
    }
    
    private fun showResultSnackbar(message: String, bgColor: Int) {
        showMessage(message)
    }
    
    private fun showItemRewardSnackbar(message: String) {
        showMessage(message)
    }
    
    // Handler 사용 부분을 BaseFragment의 postDelayed 메서드로 대체
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
        showMessage(result.message)
        
        // 통계 업데이트
        updateBalanceText()
        
        // 정리 작업 지연 - postDelayed 사용
        postDelayed(3000) {
            if (pokerViewModel.isWaitingForCleanup.value == true) {
                cleanupGame()
            }
        }
    }
    
    // formatCurrency 메서드는 BaseFragment의 메서드 사용
    // private fun formatCurrency(amount: Long): String {
    //    return currencyFormatter.format(amount)
    // }

    /**
     * 점수 텍스트를 업데이트합니다.
     */
    private fun updateScoreText(score: Int) {
        // 점수가 0이면 기본 텍스트 표시
        if (score <= 0) {
            scoreText.text = "점수: 0\n "
            return
        }
        
        // 배당률 계산 (배당률은 점수 기준 배수)
        val multiplier = SCORE_MULTIPLIERS.find { score >= it.first }?.second ?: 0
        
        // 텍스트 업데이트 (점수와 배당률 표시)
        if (multiplier > 0) {
            scoreText.text = "점수: $score\n배당률: ${multiplier}배"
        } else {
            scoreText.text = "점수: $score\n "
        }
    }

    /**
     * 게임 규칙을 보여주는 대화상자를 표시합니다.
     */
    private fun showGameRules() {
        val rulesBuilder = StringBuilder()
        rulesBuilder.append("[ 1인 발라트 포커 게임 규칙 ]\n\n")
        rulesBuilder.append("1. 총 7장의 카드 중 5장을 선택하여 가장 높은 패를 만듭니다.\n")
        rulesBuilder.append("2. 카드를 교체할 수 있으며, 첫 3번째까지 교체는 무료입니다.\n")
        rulesBuilder.append("3. 4번째 교체는 배팅 금액의 절반, 5번째는 배팅 금액 전액이 들어갑니다.\n")
        rulesBuilder.append("4. 패의 종류에 따라 다른 배당률이 적용됩니다.\n\n")
        rulesBuilder.append("[ 패 순위 (높은 순) ]\n")
        rulesBuilder.append("• 로얄 스트레이트 플러시: 같은 무늬의 10, J, Q, K, A (배당 10배)\n")
        rulesBuilder.append("• 스트레이트 플러시: 같은 무늬의 연속된 5장 (배당 6배)\n")
        rulesBuilder.append("• 포카드: 같은 숫자 4장 (배당 4배)\n")
        rulesBuilder.append("• 풀하우스: 같은 숫자 3장 + 같은 숫자 2장 (배당 3배)\n")
        rulesBuilder.append("• 플러시: 같은 무늬 5장 (배당 2배)\n")
        rulesBuilder.append("• 스트레이트: 연속된 숫자 5장 (배당 1배)\n\n")
        rulesBuilder.append("좋은 패를 만들어 최대한 많은 수익을 올려보세요!")

        // 대화상자 생성 및 표시
        AlertDialog.Builder(requireContext())
            .setTitle("게임 규칙")
            .setMessage(rulesBuilder.toString())
            .setPositiveButton("확인", null)
            .show()
    }

    /**
     * 잔액 텍스트를 업데이트합니다.
     * 실제 UI 요소가 제거되었으므로 이 메서드는 상태 관리만 수행합니다.
     */
    private fun updateBalanceText() {
        // 상단 정보바에서 잔액이 자동으로 표시되므로 별도 작업 필요 없음
        // BaseFragment와의 호환성을 위해 메서드만 유지
    }
    
    /**
     * 베팅 금액 텍스트를 업데이트합니다.
     */
    private fun updateBetAmountText() {
        val tempBet = pokerViewModel.tempBetAmount.value ?: 0L
        val currentBet = pokerViewModel.currentBet.value ?: 0L
        
        betAmountText.text = when {
            pokerViewModel.isGameActive.value == true -> "베팅 금액: ${formatCurrency(currentBet)}"
            else -> "베팅 금액: ${formatCurrency(tempBet)}"
        }
    }
    
    /**
     * 카드 교체 버튼 텍스트를 업데이트합니다.
     */
    private fun updateChangeButtonText(count: Int) {
        // 교체 비용 계산: 첫 3번은 무료, 4번째는 배팅금의 절반, 5번째는 배팅금 전액
        val currentBet = pokerViewModel.currentBet.value ?: 0L
        val cost = when (count) {
            0, 1, 2 -> 0L  // 첫 3번 교체는 무료
            3 -> currentBet / 2  // 4번째 교체는 배팅금의 절반
            else -> currentBet  // 5번째 이상은 배팅금 전액
        }
        
        // 남은 무료 교체 횟수 표시
        val freeChangesLeft = when {
            count < 3 -> 3 - count
            else -> 0
        }
        
        changeButton.text = when {
            cost > 0 -> "카드 교체 (${formatCurrency(cost)})"
            freeChangesLeft > 0 -> "카드 교체 (무료 ${freeChangesLeft}회)"
            else -> "카드 교체 (무료)"
        }
    }

    /**
     * 게임 상태를 관찰합니다.
     * 게임 오버 및 기타 상태 변화에 대응합니다.
     */
    private fun observeGameState() {
        // 게임 오버 상태 관찰
        timeViewModel.isGameOver.observe(viewLifecycleOwner) { isGameOver ->
            if (isGameOver) {
                // 게임 오버 시 게임 상태 리셋
                isGameActive = false
                updateButtonStates(false)
                
                // 게임 결과 저장 및 종료 메시지 표시
                if (pokerViewModel.isGameActive.value == true) {
                    pokerViewModel.resetGame()
                    showMessage("게임 시간이 종료되었습니다. 게임이 강제 종료됩니다.")
                }
            }
        }
    }
    
    /**
     * 게임 버튼들의 활성화 상태를 업데이트합니다.
     */
    private fun updateButtonStates(isActive: Boolean) {
        // 게임 중일 때는 게임 버튼만 활성화, 베팅 버튼은 비활성화
        // 게임 중이 아닐 때는 반대로 설정
        changeButton.isEnabled = isActive
        endGameButton.isEnabled = isActive
        
        bet10kButton.isEnabled = !isActive
        bet50kButton.isEnabled = !isActive
        bet100kButton.isEnabled = !isActive
        bet500kButton.isEnabled = !isActive
        newGameButton.isEnabled = !isActive
        
        // 게임 상태 업데이트
        isGameActive = isActive
    }

    /**
     * 게임 정리 작업을 수행합니다.
     * 결과 표시 후 새로운 게임을 시작할 수 있도록 상태를 초기화합니다.
     */
    private fun cleanupGame() {
        // UI 초기화
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        
        // 기타 상태 초기화
        handRankText.text = "패 없음"
        scoreText.text = "점수: 0\n "
        selectedCardIndices.clear()
        handRankCardIndices.clear() // 족보 카드 인덱스 초기화
        
        // 게임 상태 초기화
        pokerViewModel.resetGame()
        
        // 베팅 금액 초기화
        betAmountText.text = "베팅 금액: ${formatCurrency(0)}"
        
        // 버튼 상태 초기화
        updateButtonStates(false)
        
        // 정리 완료 메시지
        showMessage("새 게임을 위해 베팅해주세요.")
        
        // 대기 상태 해제
        isWaitingForCleanup = false
    }
    
    /**
     * 아이템 보상 처리를 수행합니다.
     */
    private fun processItemReward(betAmount: Long, multiplier: Int) {
        // 아이템 획득 처리 (포커는 gameType 2)
        val itemReward = ItemUtil.processCasinoWin(requireContext(), betAmount, 2)
        
        // 아이템을 획득했으면 메시지 표시
        itemReward?.let {
            // 1.5초 지연 후 아이템 획득 메시지 표시 (기존 승리 메시지와 겹치지 않게)
            postDelayed(1500) {
                showMessage("🎁 ${it.itemName} 아이템을 획득했습니다!")
            }
        }
    }

    /**
     * 카드 뷰를 업데이트합니다.
     */
    private fun updateCardViews(cards: List<Card>) {
        // 기존 카드 뷰 제거
        playerCardsLayout.removeAllViews()
        cardViews.clear()
        
        // 모든 카드에 대해 뷰 생성
        for (i in cards.indices) {
            val card = cards[i]
            addCardView(i, card)
        }
    }
    
    /**
     * 카드 뷰를 추가합니다.
     */
    private fun addCardView(index: Int, card: Card) {
        val cardView = TextView(requireContext())
        
        // 카드 너비 계산 (화면 가로 길이의 1/8에서 1/9로 조정)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val cardWidth = screenWidth / 9
        
        // 카드 레이아웃 파라미터 설정 - 마진 줄임
        val params = LinearLayout.LayoutParams(
            cardWidth,
            (cardWidth * 1.5).toInt()
        ).apply {
            setMargins(4, 8, 4, 8)
        }
        
        // 카드 뷰 속성 설정
        cardView.layoutParams = params
        cardView.gravity = Gravity.CENTER
        cardView.textSize = 18f
        cardView.text = card.toString()
        cardView.background = defaultCardDrawable.constantState?.newDrawable()
        cardView.tag = index
        
        // 카드 색상 설정 (하트/다이아는 빨간색, 스페이드/클럽은 검은색)
        cardView.setTextColor(
            if (card.suit == "♥" || card.suit == "♦") Color.RED else Color.BLACK
        )
        
        // 클릭 리스너 설정
        cardView.setOnClickListener(cardClickListener)
        
        // 카드 뷰 추가
        playerCardsLayout.addView(cardView)
        cardViews.add(cardView)
    }
    
    /**
     * 선택된 카드를 시각적으로 업데이트합니다.
     */
    private fun updateSelectedCards(indices: Set<Int>) {
        // 로컬 상태와 ViewModel 상태 동기화
        selectedCardIndices.clear()
        selectedCardIndices.addAll(indices)
        
        // 카드 표시 업데이트 (선택된 카드와 족보 카드)
        updateCardDisplay()
        
        // 선택한 카드가 5장이면 자동으로 패 평가를 위해 ViewModel에 알림
        if (indices.size == 5 && (pokerViewModel.currentScore.value ?: 0) <= 0) {
            pokerViewModel.updateScore()
        }
    }
    
    /**
     * 모든 카드 스타일을 초기화합니다.
     */
    private fun clearAllHighlights() {
        for (cardView in cardViews) {
            cardView.clearAnimation()
            cardView.alpha = 1.0f
            cardView.background = defaultCardDrawable.constantState?.newDrawable()
        }
    }

    /**
     * 족보에 해당하는 카드를 노란색으로 표시합니다.
     */
    private fun highlightHandRankCards() {
        // 현재 카드가 없으면 리턴
        val viewModelCards = pokerViewModel.playerCards.value ?: return
        if (viewModelCards.isEmpty() || viewModelCards.size < 5) return
        
        // ViewModel의 Card를 Fragment의 Card로 변환
        val cards = viewModelCards.map { vmCard ->
            Card(vmCard.rank, vmCard.suit)
        }
        
        // 족보 분석 결과 저장
        handRankCardIndices.clear()
        
        // 카드 조합으로 최상위 족보 분석
        findBestHandRank(cards)
        
        // 선택된 카드와 족보 카드 표시 업데이트
        updateCardDisplay()
    }
    
    /**
     * 주어진 카드에서 최상의 족보를 찾고 해당 카드 인덱스를 저장합니다.
     */
    private fun findBestHandRank(cards: List<Card>) {
        // 1. 로얄 스트레이트 플러시 또는 스트레이트 플러시 체크
        if (checkRoyalOrStraightFlush(cards)) return
        
        // 2. 포카드 체크
        if (checkFourOfAKind(cards)) return
        
        // 3. 풀하우스 체크
        if (checkFullHouse(cards)) return
        
        // 4. 플러시 체크
        if (checkFlush(cards)) return
        
        // 5. 스트레이트 체크
        if (checkStraight(cards)) return
        
        // 6. 트리플 체크
        if (checkThreeOfAKind(cards)) return
        
        // 7. 투페어 체크
        if (checkTwoPair(cards)) return
        
        // 8. 원페어 체크
        checkOnePair(cards)
    }
    
    /**
     * 카드 표시를 업데이트합니다.
     * 선택된 카드는 초록색, 족보 카드는 노란색으로 표시합니다.
     */
    private fun updateCardDisplay() {
        // 현재 선택된 카드 인덱스 가져오기
        val selectedIndices = pokerViewModel.selectedCardIndices.value ?: emptySet()
        
        // 모든 카드 스타일 초기화
        for (cardView in cardViews) {
            cardView.clearAnimation()
            cardView.alpha = 1.0f
            cardView.background = defaultCardDrawable.constantState?.newDrawable()
        }
        
        // 카드 상태에 따라 표시
        for (i in cardViews.indices) {
            val cardView = cardViews[i]
            
            when {
                // 1. 선택된 카드 - 초록색으로 표시 (선택이 우선)
                selectedIndices.contains(i) -> {
                    cardView.alpha = 0.7f
                    cardView.background = selectedCardDrawable.constantState?.newDrawable()
                }
                // 2. 족보 카드 - 노란색으로 표시
                handRankCardIndices.contains(i) -> {
                    cardView.background = handRankCardDrawable.constantState?.newDrawable()
                }
            }
        }
    }

    /**
     * 로얄 스트레이트 플러시 또는 스트레이트 플러시 확인
     */
    private fun checkRoyalOrStraightFlush(cards: List<Card>): Boolean {
        val royalValues = listOf(10, 11, 12, 13, 14) // 10, J, Q, K, A
        
        // 무늬별로 분류
        val suitGroups = cards.groupBy { it.suit }
        
        for ((suit, suitCards) in suitGroups) {
            // 같은 무늬의 카드가 5장 이상이어야 가능
            if (suitCards.size < 5) continue
            
            // 로얄 스트레이트 플러시 체크
            val royalCount = suitCards.count { royalValues.contains(it.value()) }
            if (royalCount >= 5) {
                // 해당 카드들의 인덱스 찾기
                cards.forEachIndexed { index, card ->
                    if (card.suit == suit && royalValues.contains(card.value())) {
                        handRankCardIndices.add(index)
                    }
                }
                return true
            }
            
            // 스트레이트 플러시 체크 - 연속된 값 5개 이상
            val values = suitCards.map { it.value() }.sorted()
            val consecutiveValues = findConsecutiveSequence(values)
            
            if (consecutiveValues.size >= 5) {
                // 해당 카드들의 인덱스 찾기
                cards.forEachIndexed { index, card ->
                    if (card.suit == suit && consecutiveValues.contains(card.value())) {
                        handRankCardIndices.add(index)
                    }
                }
                return true
            }
        }
        
        return false
    }
    
    /**
     * 포카드(같은 숫자 4장) 확인
     */
    private fun checkFourOfAKind(cards: List<Card>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.groupBy { it.rank }
        
        // 같은 숫자 4장 이상 있는 랭크 찾기
        val fourOfAKindRank = rankGroups.entries.find { it.value.size >= 4 }?.key
        
        if (fourOfAKindRank != null) {
            // 해당 카드들의 인덱스 찾기
            cards.forEachIndexed { index, card ->
                if (card.rank == fourOfAKindRank) {
                    handRankCardIndices.add(index)
                }
            }
            return true
        }
        
        return false
    }
    
    /**
     * 풀하우스(같은 숫자 3장 + 같은 숫자 2장) 확인
     */
    private fun checkFullHouse(cards: List<Card>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.groupBy { it.rank }
        
        // 3장 이상 있는 랭크들
        val tripletRanks = rankGroups.filter { it.value.size >= 3 }.keys.toList()
        
        // 2장 이상 있는 랭크들
        val pairRanks = rankGroups.filter { it.value.size >= 2 }.keys.toList()
        
        // 트리플이 하나 이상 있고, 페어(또는 다른 트리플)도 있는 경우
        if (tripletRanks.isNotEmpty() && (pairRanks.size >= 2 || tripletRanks.size >= 2)) {
            val tripleRank = tripletRanks[0]
            
            // 트리플과 다른 페어 선택
            val pairRank = if (pairRanks.size > 1) {
                // 트리플과 다른 첫 번째 랭크
                pairRanks.find { it != tripleRank } ?: pairRanks[0]
            } else if (tripletRanks.size > 1) {
                // 두 번째 트리플 사용
                tripletRanks[1]
            } else {
                // 이 경우는 발생하지 않음
                return false
            }
            
            // 해당 카드들의 인덱스 찾기
            var tripleCount = 0
            var pairCount = 0
            
            // 인덱스를 순서대로 검사하여 트리플과 페어 추가
            cards.forEachIndexed { index, card ->
                when (card.rank) {
                    tripleRank -> {
                        if (tripleCount < 3) {
                            handRankCardIndices.add(index)
                            tripleCount++
                        }
                    }
                    pairRank -> {
                        if (pairCount < 2) {
                            handRankCardIndices.add(index)
                            pairCount++
                        }
                    }
                }
            }
            
            return true
        }
        
        return false
    }
    
    /**
     * 플러시(같은 무늬 5장) 확인
     */
    private fun checkFlush(cards: List<Card>): Boolean {
        // 무늬별로 그룹화
        val suitGroups = cards.groupBy { it.suit }
        
        // 5장 이상 같은 무늬가 있는지 확인
        val flushSuit = suitGroups.entries.find { it.value.size >= 5 }?.key
        
        if (flushSuit != null) {
            // 해당 카드들의 인덱스 찾기 (최대 5장)
            var count = 0
            cards.forEachIndexed { index, card ->
                if (card.suit == flushSuit && count < 5) {
                    handRankCardIndices.add(index)
                    count++
                }
            }
            return true
        }
        
        return false
    }
    
    /**
     * 스트레이트(연속된 숫자 5장) 확인
     */
    private fun checkStraight(cards: List<Card>): Boolean {
        // 중복 제거한 값 목록
        val values = cards.map { it.value() }.distinct().sorted()
        
        // 연속된 5개 이상의 값 찾기
        val consecutiveValues = findConsecutiveSequence(values)
        
        if (consecutiveValues.size >= 5) {
            // 연속된 값 중 최상위 5개만 사용
            val topValues = consecutiveValues.takeLast(5)
            
            // 해당 카드들의 인덱스 찾기
            cards.forEachIndexed { index, card ->
                if (topValues.contains(card.value())) {
                    handRankCardIndices.add(index)
                }
            }
            return true
        }
        
        return false
    }
    
    /**
     * 트리플(같은 숫자 3장) 확인
     */
    private fun checkThreeOfAKind(cards: List<Card>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.groupBy { it.rank }
        
        // 3장 이상 있는 랭크 찾기 (값이 큰 것 우선)
        val tripleRank = rankGroups.filter { it.value.size >= 3 }
                              .maxByOrNull { rankValues[it.key] ?: 0 }?.key
        
        if (tripleRank != null) {
            // 해당 카드들의 인덱스 찾기
            var count = 0
            cards.forEachIndexed { index, card ->
                if (card.rank == tripleRank && count < 3) {
                    handRankCardIndices.add(index)
                    count++
                }
            }
            return true
        }
        
        return false
    }
    
    /**
     * 투페어(같은 숫자 2장 + 같은 숫자 2장) 확인
     */
    private fun checkTwoPair(cards: List<Card>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.groupBy { it.rank }
        
        // 2장 이상 있는 랭크들
        val pairRanks = rankGroups.filter { it.value.size >= 2 }
                            .keys.sortedByDescending { rankValues[it] ?: 0 }
        
        if (pairRanks.size >= 2) {
            // 최상위 2개의 페어만 사용
            val topPairs = pairRanks.take(2)
            
            // 해당 카드들의 인덱스 찾기
            topPairs.forEach { rank ->
                var count = 0
                cards.forEachIndexed { index, card ->
                    if (card.rank == rank && count < 2) {
                        handRankCardIndices.add(index)
                        count++
                    }
                }
            }
            return true
        }
        
        return false
    }
    
    /**
     * 원페어(같은 숫자 2장) 확인
     */
    private fun checkOnePair(cards: List<Card>): Boolean {
        // 랭크별로 그룹화
        val rankGroups = cards.groupBy { it.rank }
        
        // 2장 이상 있는 랭크 중 가장 높은 값
        val pairRank = rankGroups.filter { it.value.size >= 2 }
                           .maxByOrNull { rankValues[it.key] ?: 0 }?.key
        
        if (pairRank != null) {
            // 해당 카드들의 인덱스 찾기
            var count = 0
            cards.forEachIndexed { index, card ->
                if (card.rank == pairRank && count < 2) {
                    handRankCardIndices.add(index)
                    count++
                }
            }
            return true
        }
        
        return false
    }
    
    /**
     * 연속된 값의 시퀀스를 찾습니다.
     */
    private fun findConsecutiveSequence(values: List<Int>): List<Int> {
        if (values.size < 5) return emptyList()
        
        val result = mutableListOf<Int>()
        var current = mutableListOf(values[0])
        
        // 일반적인 연속 값 체크
        for (i in 1 until values.size) {
            if (values[i] == values[i-1] + 1) {
                // 연속된 값이면 추가
                current.add(values[i])
            } else if (values[i] > values[i-1] + 1) {
                // 연속이 끊기면, 결과가 더 크면 교체
                if (current.size >= 5 && (result.size < 5 || current.last() > result.last())) {
                    result.clear()
                    result.addAll(current)
                }
                current = mutableListOf(values[i])
            }
        }
        
        // 마지막 시퀀스 체크
        if (current.size >= 5 && (result.size < 5 || current.last() > result.last())) {
            result.clear()
            result.addAll(current)
        }
        
        // A-2-3-4-5 스트레이트 특별 체크 (A를 1로 취급)
        if (values.contains(14) && values.contains(2) && 
            values.contains(3) && values.contains(4) && values.contains(5)) {
            // 이미 찾은 결과가 없거나, A-5 스트레이트가 더 나은 경우
            if (result.size < 5) {
                return listOf(14, 2, 3, 4, 5)
            }
        }
        
        return result
    }
} 