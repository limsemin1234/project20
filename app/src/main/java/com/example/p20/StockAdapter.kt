package com.example.p20

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StockAdapter(
    private var stockList: List<Stock>,
    private val onItemClick: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stock_item_layout, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stockList[position]

        holder.stockName.text = stock.name
        holder.stockPrice.text = "${String.format("%,d", stock.price)}원"
        holder.stockChangeValue.text = "${String.format("%,d", stock.changeValue)}원"
        holder.stockChangeRate.text = "${"%.2f".format(stock.changeRate)}%"
        holder.stockHolding.text = "${String.format("%,d", stock.holding)}주"

        val riseColor = Color.parseColor("#FF5733")
        val fallColor = Color.parseColor("#3385FF")
        val neutralColor = Color.parseColor("#333333")

        val color = when {
            stock.changeValue > 0 -> riseColor
            stock.changeValue < 0 -> fallColor
            else -> neutralColor
        }

        holder.stockChangeValue.setTextColor(color)
        holder.stockChangeRate.setTextColor(color)
        holder.stockPrice.setTextColor(color)
        
        // 호재 영향을 받는 주식인 경우 배경색 변경
        if (stock.isPositiveNews) {
            holder.itemView.setBackgroundColor(Color.argb(50, 0, 180, 0)) // 연한 녹색 배경
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener { onItemClick(stock) }
    }

    override fun getItemCount(): Int = stockList.size
    
    fun updateData(newStockList: List<Stock>) {
         stockList = newStockList
         notifyDataSetChanged()
    }

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stockName: TextView = itemView.findViewById(R.id.stockName)
        val stockPrice: TextView = itemView.findViewById(R.id.stockPrice)
        val stockChangeValue: TextView = itemView.findViewById(R.id.stockChangeValue)
        val stockChangeRate: TextView = itemView.findViewById(R.id.stockChangeRate)
        val stockHolding: TextView = itemView.findViewById(R.id.stockHolding)
    }
} 