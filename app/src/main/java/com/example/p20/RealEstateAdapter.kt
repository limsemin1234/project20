package com.example.p20

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class RealEstateAdapter(
    private var estateList: List<RealEstate>,
    private val onItemClick: (RealEstate) -> Unit
) : RecyclerView.Adapter<RealEstateAdapter.RealEstateViewHolder>() {

    inner class RealEstateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val estateName: TextView = itemView.findViewById(R.id.estateName)
        val estatePrice: TextView = itemView.findViewById(R.id.estatePrice)
        val estateOwned: TextView = itemView.findViewById(R.id.estateOwned)
        val estateStageIndicator: TextView = itemView.findViewById(R.id.estateStageIndicator)
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
        holder.estatePrice.text = "${formatter.format(estate.price)}원"
        holder.estateOwned.text = if (estate.owned) "보유 중" else "미보유"

        holder.estateOwned.setTextColor(
            if (estate.owned) Color.parseColor("#00C853") // 초록
            else Color.parseColor("#D50000") // 빨강
        )

        // 단계 표시
        val rate = estate.getCurrentRate()
        holder.estateStageIndicator.text = getStageIndicator(rate)

        // scale 조절
        val scale = when (rate) {
            10, -10 -> 0.7f
            20, -20 -> 1.0f
            30, -30 -> 1.3f
            else -> 0.9f
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
