package com.example.p20

class AssetManager {
    private var asset: Int = 100000  // 초기 자산 100,000원

    // 자산 증가 함수
    fun increaseAsset(amount: Int) {
        asset += amount
    }

    fun decreaseAsset(amount: Int) {
        if (asset - amount >= 0) {
            asset -= amount
        } else {
            // 자산이 부족하면 알림을 표시하는 로직을 추가할 수 있습니다.
            println("자산이 부족합니다!")
        }
    }

    // 자산 조회 함수
    fun getAsset(): Int {
        return asset
    }

    // 자산 텍스트 포맷 (천 단위 구분)
    fun getAssetText(): String {
        return "자산: ${asset.toString().replace(Regex("(?<=\\d)(?=(\\d{3})+\\b)"), ",")}원"
    }
}

