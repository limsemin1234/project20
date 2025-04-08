package com.example.p20

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * ViewModel 생성을 관리하는 팩토리 클래스
 */
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    
    // 공유 Repository 인스턴스 생성
    private val assetRepository by lazy { AssetRepository(application) }
    
    // 공유 Calculator 인스턴스 생성
    private val interestCalculator by lazy { InterestCalculator(assetRepository, application) }
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AssetViewModel::class.java) -> {
                val viewModel = AssetViewModel(application, assetRepository, interestCalculator)
                viewModel as T
            }
            modelClass.isAssignableFrom(AlbaViewModel::class.java) -> AlbaViewModel(application) as T
            modelClass.isAssignableFrom(TimingAlbaViewModel::class.java) -> TimingAlbaViewModel(application) as T
            modelClass.isAssignableFrom(TimeViewModel::class.java) -> TimeViewModel(application) as T
            modelClass.isAssignableFrom(RealEstateViewModel::class.java) -> RealEstateViewModel(application) as T
            modelClass.isAssignableFrom(StockViewModel::class.java) -> StockViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}