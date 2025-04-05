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
            5, 10 -> addItem(context, 1, 1) // 레벨 5, 10 -> 60초 아이템 1개
            15, 20 -> addItem(context, 2, 1) // 레벨 15, 20 -> 120초 아이템 1개
            25, 30 -> addItem(context, 3, 1) // 레벨 25, 30 -> 180초 아이템 1개
            else -> {
                // 30레벨 이후 랜덤으로 2개 획득
                val randomItems = getRandomItems(2)
                val rewards = mutableListOf<ItemReward>()
                
                randomItems.forEach { itemId ->
                    addItem(context, itemId, 1)?.let {
                        rewards.add(it)
                    }
                }
                
                if (rewards.isEmpty()) null
                else ItemReward(
                    itemId = 0, // 복합 보상이므로 0
                    itemName = "랜덤 아이템 2개",
                    quantity = 2,
                    isMultiple = true
                )
            }
        }
    }
    
    /**
     * 타이밍알바 레벨업 시 아이템 획득 처리
     * @param context Context
     * @param currentLevel 현재 레벨
     * @return 획득한 아이템 정보 (없으면 null)
     */
    fun processTimingAlbaLevelUp(context: Context, currentLevel: Int): ItemReward? {
        // 5레벨 단위로 체크
        if (currentLevel % 5 != 0) {
            return null
        }
        
        return when (currentLevel) {
            5, 10 -> addItem(context, 1, 1) // 레벨 5, 10 -> 60초 아이템 1개
            15, 20 -> addItem(context, 2, 1) // 레벨 15, 20 -> 120초 아이템 1개
            25, 30 -> addItem(context, 3, 1) // 레벨 25, 30 -> 180초 아이템 1개
            else -> {
                // 30레벨 이후 랜덤으로 2개 획득
                val randomItems = getRandomItems(2)
                val rewards = mutableListOf<ItemReward>()
                
                randomItems.forEach { itemId ->
                    addItem(context, itemId, 1)?.let {
                        rewards.add(it)
                    }
                }
                
                if (rewards.isEmpty()) null
                else ItemReward(
                    itemId = 0, // 복합 보상이므로 0
                    itemName = "랜덤 아이템 2개",
                    quantity = 2,
                    isMultiple = true
                )
            }
        }
    }
    
    /**
     * 특정 아이템을 획득합니다.
     * @param context Context
     * @param itemId 아이템 ID
     * @param quantity 수량
     * @return 획득한 아이템 정보
     */
    private fun addItem(context: Context, itemId: Int, quantity: Int): ItemReward? {
        val prefs = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val currentQuantity = prefs.getInt("${KEY_ITEM_QUANTITY_PREFIX}$itemId", 0)
        val newQuantity = currentQuantity + quantity
        
        prefs.edit().putInt("${KEY_ITEM_QUANTITY_PREFIX}$itemId", newQuantity).apply()
        
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