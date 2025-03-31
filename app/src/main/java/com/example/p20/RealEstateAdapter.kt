package com.example.p20

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat


class RealEstateAdapter(
    private var realEstateList: List<RealEstate>,
    private val onItemClick: (RealEstate) -> Unit
) : RecyclerView.Adapter<RealEstateAdapter.RealEstateViewHolder>() {

    class RealEstateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val estateName: TextView = itemView.findViewById(R.id.estateName)
        val estatePrice: TextView = itemView.findViewById(R.id.estatePrice)
        val estateOwned: TextView = itemView.findViewById(R.id.estateOwned)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RealEstateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.real_estate_item_layout, parent, false)
        return RealEstateViewHolder(view)
    }

    override fun onBindViewHolder(holder: RealEstateViewHolder, position: Int) {
        val estate = realEstateList[position]
        val formatter = DecimalFormat("#,###")

        holder.estateName.text = estate.name
        holder.estatePrice.text = "${formatter.format(estate.price)}원"
        holder.estateOwned.text = if (estate.owned) "보유 중" else "미보유"

        // 보유 상태에 따라 색상 표시 (선택사항)
        holder.estateOwned.setTextColor(
            if (estate.owned) Color.parseColor("#FF5733") else Color.parseColor("#888888")
        )

        holder.itemView.setOnClickListener {
            onItemClick(estate)
        }
    }

    override fun getItemCount(): Int = realEstateList.size

    fun updateList(newList: List<RealEstate>) {
        realEstateList = newList
        notifyDataSetChanged()
    }
}
