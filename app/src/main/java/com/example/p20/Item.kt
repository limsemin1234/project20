package com.example.p20

/**
 * 아이템 데이터 클래스
 * @param id 아이템 고유 ID
 * @param name 아이템 이름
 * @param description 아이템 설명
 * @param price 아이템 가격
 * @param effect 아이템 효과(초)
 * @param stock 아이템 재고
 * @param quantity 보유 수량
 */
data class Item(
    val id: Int,
    val name: String,
    val description: String,
    val price: Long,
    val effect: Int,
    var stock: Int,
    var quantity: Int = 0
) 