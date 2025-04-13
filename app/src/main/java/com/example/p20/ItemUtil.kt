package com.example.p20

import android.content.Context
import kotlin.random.Random

/**
 * 알바 레벨에 따른 아이템 획득 유틸리티 클래스
 */
object ItemUtil {
    
    // 아이템 획득 관련 SharedPreferences 이름
    private const val PREFS_FILENAME = "item_prefs"
    private const val KEY_ITEM_QUANTITY_PREFIX = "item_quantity_"
    private const val KEY_ITEM_STOCK_PREFIX = "item_stock_"
    
    /**
     * 클릭알바 레벨업 시 아이템 획득 처리
     * @param context Context
     * @param currentLevel 현재 레벨
     * @return 획득한 아이템 정보 (없으면 null)
     */
    fun processClickAlbaLevelUp(context: Context, currentLevel: Int): ItemReward? {
        // 5레벨 단위로 체크
        if (currentLevel % 5 != 0) {
            return null
        }
        
        return when (currentLevel) {
            5, 10 -> increaseItemStock(context, 1, 1) // 레벨 5, 10 -> 60초 아이템 재고 1개 증가
            15, 20 -> increaseItemStock(context, 2, 1) // 레벨 15, 20 -> 120초 아이템 재고 1개 증가
            25, 30 -> increaseItemStock(context, 3, 1) // 레벨 25, 30 -> 180초 아이템 재고 1개 증가
            else -> {
                // 30레벨 이후 랜덤으로 2개 아이템 재고 증가
                val randomItems = getRandomItems(2)
                val rewards = mutableListOf<ItemReward>()
                
                randomItems.forEach { itemId ->
                    increaseItemStock(context, itemId, 1)?.let {
                        rewards.add(it)
                    }
                }
                
                if (rewards.isEmpty()) null
                else ItemReward(
                    itemId = 0, // 복합 보상이므로 0
                    itemName = "랜덤 아이템 재고 2개",
                    quantity = 2,
                    isMultiple = true
                )
            }
        }
    }
    
    /**
     * 카지노 게임(블랙잭, 포커) 승리 시 아이템 획득 처리
     * @param context Context
     * @param betAmount 베팅 금액
     * @param gameType 게임 종류 (1: 블랙잭, 2: 포커)
     * @return 획득한 아이템 정보 (없으면 null)
     */
    fun processCasinoWin(context: Context, betAmount: Long, gameType: Int): ItemReward? {
        // 최소 베팅 금액 설정 (블랙잭: 5만원, 포커: 10만원)
        val minBetAmount = if (gameType == 1) 50000L else 100000L
        
        // 최소 베팅 금액 이상일 때만 아이템 획득 가능
        if (betAmount < minBetAmount) {
            return null
        }
        
        // 아이템 획득 확률 계산 (베팅 금액이 클수록 확률 증가)
        // 기본 확률: 블랙잭 20%, 포커 25%
        val baseChance = if (gameType == 1) 20 else 25
        // 베팅 금액에 따른 추가 확률 (최대 +30%)
        val additionalChance = minOf(30, (betAmount / 20000).toInt())
        val totalChance = baseChance + additionalChance
        
        // 확률에 따른 아이템 획득 여부 결정
        if (Random.nextInt(100) >= totalChance) {
            return null
        }
        
        // 랜덤으로 시간증폭 아이템 하나 선택 (ID: 1~3)
        val randomItemId = Random.nextInt(1, 4)
        
        // 아이템 재고 증가
        return increaseItemStock(context, randomItemId, 1)
    }
    
    /**
     * 아이템 재고를 증가시킵니다.
     * @param context Context
     * @param itemId 아이템 ID
     * @param quantity 증가시킬 수량
     * @return 증가된 아이템 정보
     */
    private fun increaseItemStock(context: Context, itemId: Int, quantity: Int): ItemReward? {
        val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val currentStock = prefs.getInt("${KEY_ITEM_STOCK_PREFIX}$itemId", 0)
        val newStock = currentStock + quantity
        
        prefs.edit().putInt("${KEY_ITEM_STOCK_PREFIX}$itemId", newStock).apply()
        
        val itemName = when (itemId) {
            1 -> "Time증폭(60초)"
            2 -> "Time증폭(120초)"
            3 -> "Time증폭(180초)"
            else -> "알 수 없는 아이템"
        }
        
        return ItemReward(
            itemId = itemId,
            itemName = itemName,
            quantity = quantity,
            isMultiple = false
        )
    }
    
    /**
     * 랜덤 아이템 ID 목록을 반환합니다.
     * @param count 필요한 아이템 개수
     * @return 아이템 ID 목록
     */
    private fun getRandomItems(count: Int): List<Int> {
        val itemIds = mutableListOf<Int>()
        repeat(count) {
            // 1, 2, 3 중 랜덤 선택
            val randomId = Random.nextInt(1, 4)
            itemIds.add(randomId)
        }
        return itemIds
    }
}

/**
 * 아이템 보상 정보 데이터 클래스
 */
data class ItemReward(
    val itemId: Int,        // 아이템 ID
    val itemName: String,   // 아이템 이름
    val quantity: Int,      // 수량
    val isMultiple: Boolean // 복합 보상 여부
)