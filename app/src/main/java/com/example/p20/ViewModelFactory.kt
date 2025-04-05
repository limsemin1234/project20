package com.example.p20

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AssetViewModel::class.java) -> AssetViewModel(application) as T
            modelClass.isAssignableFrom(AlbaViewModel::class.java) -> AlbaViewModel(application) as T
            modelClass.isAssignableFrom(TimingAlbaViewModel::class.java) -> TimingAlbaViewModel(application) as T
            modelClass.isAssignableFrom(TimeViewModel::class.java) -> TimeViewModel(application) as T
            modelClass.isAssignableFrom(RealEstateViewModel::class.java) -> RealEstateViewModel(application) as T
            modelClass.isAssignableFrom(StockViewModel::class.java) -> StockViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}