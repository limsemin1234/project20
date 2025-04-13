package com.example.p20

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class RealEstateAdapter(
    private var estateList: List<RealEstate>,
    private val onItemClick: (RealEstate) -> Unit,
    private val viewModel: RealEstateViewModel
) : RecyclerView.Adapter<RealEstateAdapter.RealEstateViewHolder>() {

    inner class RealEstateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val estateName: TextView = itemView.findViewById(R.id.estateName)
        val estatePrice: TextView = itemView.findViewById(R.id.estatePrice)
        val estateOwned: TextView = itemView.findViewById(R.id.estateOwned)
        val estateStageIndicator: TextView = itemView.findViewById(R.id.estateStageIndicator)
        val estateIncome: TextView = itemView.findViewById(R.id.estateIncome)
        val repairButton: Button = itemView.findViewById(R.id.repairButton)
    }

    // 부동산 복구 콜백 인터페이스
    interface RepairCallback {
        fun onRepairRequested(estate: RealEstate, cost: Long)
    }

    // 복구 콜백 속성
    private var repairCallback: RepairCallback? = null

    // 복구 콜백 설정 메서드
    fun setRepairCallback(callback: RepairCallback) {
        repairCallback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RealEstateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.real_estate_item_layout, parent, false)
        return RealEstateViewHolder(view)
    }

    override fun onBindViewHolder(holder: RealEstateViewHolder, position: Int) {
        val estate = estateList[position]
        val formatter = DecimalFormat("#,###")

        holder.estateName.text = estate.name
        
        // 전쟁 이벤트 영향 체크하여 가격 텍스트 색상 및 카드 배경색 설정
        val isAffectedByWar = viewModel.isAffectedByWar(estate.id)
        if (isAffectedByWar) {
            holder.estatePrice.text = "${formatter.format(estate.price)}원"
            holder.estatePrice.setTextColor(Color.parseColor("#FF0000"))  // 빨간색으로 변경
            // 카드 배경색 변경 - 약간 붉은 색조로
            (holder.itemView as androidx.cardview.widget.CardView).setCardBackgroundColor(Color.parseColor("#502020"))
            
            // 복구 버튼 표시 및 설정 (소유한 부동산인 경우에만)
            if (estate.owned) {
                holder.repairButton.visibility = View.VISIBLE
                val repairCost = viewModel.getRepairCost(estate.id)
                holder.repairButton.text = "복구 (${formatter.format(repairCost)}원)"
                
                // 복구 버튼 클릭 리스너
                holder.repairButton.setOnClickListener {
                    repairCallback?.onRepairRequested(estate, repairCost)
                }
            } else {
                holder.repairButton.visibility = View.GONE
            }
        } else {
            holder.estatePrice.text = "${formatter.format(estate.price)}원"
            holder.estatePrice.setTextColor(Color.parseColor("#FFFFFF"))  // 기본 색상(흰색)으로 복원
            // 카드 배경색 복원 - 원래 색상(#303555)으로
            (holder.itemView as androidx.cardview.widget.CardView).setCardBackgroundColor(Color.parseColor("#303555"))
            // 복구 버튼 숨김
            holder.repairButton.visibility = View.GONE
        }
        
        // 보유 상태 설정
        if (estate.owned) {
            holder.estateOwned.text = "보유중"
            holder.estateOwned.setTextColor(Color.parseColor("#00C853"))
            holder.estateOwned.setBackgroundResource(R.drawable.ownership_status_background_owned)
        } else {
            holder.estateOwned.text = "미보유"
            holder.estateOwned.setTextColor(Color.parseColor("#D50000"))
            holder.estateOwned.setBackgroundResource(R.drawable.ownership_status_background_unowned)
        }

        // ⭐️ 예상 임대 수익 표시
        val expectedIncome = estate.getExpectedRentalIncome()
        holder.estateIncome.text = "예상 수익: ${formatter.format(expectedIncome)}원"

        // 단계 표시
        val rate = estate.getCurrentRate()
        holder.estateStageIndicator.text = getStageIndicator(rate)

        // scale 조절
        val scale = when (rate) {
            5, -5 -> 0.6f
            10, -10 -> 0.9f
            15, -15 -> 1.2f
            else -> 0.5f
        }
        holder.estateStageIndicator.scaleX = scale
        holder.estateStageIndicator.scaleY = scale

        holder.itemView.setOnClickListener {
            onItemClick(estate)
        }
    }

    override fun getItemCount(): Int = estateList.size

    fun updateList(newList: List<RealEstate>) {
        estateList = newList
        notifyDataSetChanged()
    }

    // 🔺, 🔻, ➖ 표시
    private fun getStageIndicator(rate: Int): String {
        return when {
            rate > 0 -> "🔺"
            rate < 0 -> "🔻"
            else -> "➖"
        }
    }
}
