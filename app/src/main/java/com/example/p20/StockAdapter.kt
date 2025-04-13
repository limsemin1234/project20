package com.example.p20

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import android.widget.ImageButton

class StockAdapter(
    private val onItemClick: (Stock) -> Unit,
    private val onGraphClick: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    private var stockList: List<Stock> = listOf()
    
    // 현재 선택된 주식을 추적
    private var selectedStockPosition: Int = -1
    
    // 기존 생성자 - 하위 호환성 유지
    constructor(
        stockList: List<Stock>,
        onItemClick: (Stock) -> Unit,
        onGraphClick: (Stock) -> Unit
    ) : this(onItemClick, onGraphClick) {
        this.stockList = stockList
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stock_item_layout, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stockList[position]

        // 주식명 표시 - 호재/악재 상태에 따라 표시 변경
        when {
            stock.isPositiveNews -> {
                holder.stockName.text = "${stock.name} [호재]"
            }
            stock.isNegativeNews -> {
                holder.stockName.text = "${stock.name} [악재]"
            }
            else -> {
                holder.stockName.text = stock.name
            }
        }

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
        
        // 배경색 및 선택 표시 설정
        val background = GradientDrawable()
        background.cornerRadius = 8f  // 둥근 모서리
        
        // 선택된 항목인 경우
        if (position == selectedStockPosition) {
            // 선택된 항목은 테두리와 함께 약간 어두운 배경색으로 표시
            background.setStroke(4, Color.parseColor("#303F9F"))  // 진한 파란색 테두리
            background.setColor(Color.parseColor("#DDEEEEEE"))    // 연한 회색 배경
            
            // 선택 표시기(좌측 세로 막대) 추가
            val selectionIndicator = View(holder.itemView.context)
            selectionIndicator.layoutParams = ViewGroup.LayoutParams(
                8, ViewGroup.LayoutParams.MATCH_PARENT)
            selectionIndicator.setBackgroundColor(Color.parseColor("#303F9F"))
            
            // 이미 자식 뷰가 있으면 제거
            if (holder.itemView is ViewGroup && (holder.itemView as ViewGroup).childCount > 0) {
                try {
                    (holder.itemView as ViewGroup).getChildAt(0)?.let { existingChild ->
                        if (existingChild.id == -100) {
                            (holder.itemView as ViewGroup).removeView(existingChild)
                        }
                    }
                } catch (e: Exception) {
                    // 오류 무시
                }
            }
            
            // 텍스트 굵게 처리
            holder.stockName.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            // 선택되지 않은 항목
            background.setStroke(1, Color.parseColor("#DDDDDD"))  // 연한 회색 테두리
            background.setColor(Color.WHITE)  // 기본 흰색 배경
            
            // 텍스트 일반 스타일로 복원
            holder.stockName.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        
        // 배경 설정
        holder.itemView.background = background
        
        // 클릭 이벤트 처리
        holder.itemView.setOnClickListener { 
            val previousPosition = selectedStockPosition
            selectedStockPosition = position
            
            // 이전 선택 항목과 현재 선택 항목 갱신
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(position)
            
            // 콜백 호출
            onItemClick(stock) 
        }
        
        // 그래프 버튼 클릭 이벤트 처리
        holder.stockGraphButton.setOnClickListener {
            onGraphClick(stock)
        }
    }

    override fun getItemCount(): Int = stockList.size
    
    fun updateData(newStockList: List<Stock>) {
        // 현재 선택된 주식 객체 유지
        val selectedStock = if (selectedStockPosition != -1 && selectedStockPosition < stockList.size) {
            stockList[selectedStockPosition]
        } else null
        
        stockList = newStockList
        
        // 선택된 주식이 새 목록에서도 존재하면 해당 위치로 업데이트
        if (selectedStock != null) {
            val newPosition = newStockList.indexOfFirst { it.name == selectedStock.name }
            selectedStockPosition = if (newPosition != -1) newPosition else -1
        }
        
        notifyDataSetChanged()
    }
    
    /**
     * updateData 메서드의 별칭 (코드 일관성을 위해 추가)
     */
    fun updateItems(newStockList: List<Stock>) {
        updateData(newStockList)
    }
    
    /**
     * 특정 주식을 선택 상태로 설정
     */
    fun setSelectedStock(stock: Stock) {
        val position = stockList.indexOfFirst { it.name == stock.name }
        if (position != -1 && position != selectedStockPosition) {
            val previousPosition = selectedStockPosition
            selectedStockPosition = position
            
            if (previousPosition != -1) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(position)
        }
    }

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stockName: TextView = itemView.findViewById(R.id.stockName)
        val stockPrice: TextView = itemView.findViewById(R.id.stockPrice)
        val stockChangeValue: TextView = itemView.findViewById(R.id.stockChangeValue)
        val stockChangeRate: TextView = itemView.findViewById(R.id.stockChangeRate)
        val stockHolding: TextView = itemView.findViewById(R.id.stockHolding)
        val stockGraphButton: ImageButton = itemView.findViewById(R.id.stockGraphButton)
    }
} 