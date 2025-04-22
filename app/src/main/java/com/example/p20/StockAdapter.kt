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
import java.lang.ref.WeakReference
import android.util.Log

/**
 * 주식 목록을 표시하기 위한 RecyclerView 어댑터
 * 
 * 주식 목록을 표시하고 주식 선택, 시각적 효과 및 상태 관리를 담당합니다.
 * 메모리 누수 방지를 위해 WeakReference 사용
 * 
 * @property onItemClick 주식 항목 클릭 시 호출되는 콜백 함수
 * @property onGraphClick 그래프 버튼 클릭 시 호출되는 콜백 함수
 */
class StockAdapter(
    onItemClick: (Stock) -> Unit,
    onGraphClick: (Stock) -> Unit
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    /** 표시할 주식 목록 */
    private var stockList: List<Stock> = listOf()
    
    /** 현재 선택된 주식의 위치 (-1은 선택된 항목 없음) */
    private var selectedStockPosition: Int = -1
    
    /** WeakReference를 사용하여 콜백 저장 - 메모리 누수 방지 */
    private val weakItemClickCallback = WeakReference(onItemClick)
    private val weakGraphClickCallback = WeakReference(onGraphClick)
    
    /** SoundManager 인스턴스에 대한 WeakReference */
    private var weakSoundManager: WeakReference<SoundManager>? = null
    
    /** 효과음 ID */
    companion object {
        private val SOUND_SELECT = R.raw.stock_select
    }
    
    /**
     * 기존 생성자 - 하위 호환성 유지
     * 
     * @param stockList 표시할 주식 목록
     * @param onItemClick 주식 항목 클릭 시 호출되는 콜백 함수
     * @param onGraphClick 그래프 버튼 클릭 시 호출되는 콜백 함수
     */
    constructor(
        stockList: List<Stock>,
        onItemClick: (Stock) -> Unit,
        onGraphClick: (Stock) -> Unit
    ) : this(onItemClick, onGraphClick) {
        this.stockList = stockList
    }
    
    /**
     * ViewHolder를 생성하고 레이아웃을 inflate합니다.
     * 
     * @param parent 상위 ViewGroup
     * @param viewType 뷰 타입
     * @return 새 StockViewHolder 인스턴스
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stock_item_layout, parent, false)
        
        // SoundManager를 WeakReference로 저장
        val soundManager = SoundManager.getInstance(parent.context)
        weakSoundManager = WeakReference(soundManager)
        
        return StockViewHolder(view)
    }

    /**
     * 지정된 위치의 데이터를 ViewHolder에 바인딩합니다.
     * 
     * @param holder 데이터를 바인딩할 ViewHolder
     * @param position 바인딩할 데이터의 위치
     */
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

        // 상승/하락에 따른 색상 설정
        val riseColor = ContextCompat.getColor(holder.itemView.context, R.color.red)
        val fallColor = ContextCompat.getColor(holder.itemView.context, R.color.blue)
        val neutralColor = ContextCompat.getColor(holder.itemView.context, R.color.text_primary)

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
            background.setStroke(4, ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary))
            background.setColor(ContextCompat.getColor(holder.itemView.context, R.color.gray))
            
            // 선택 표시기(좌측 세로 막대) 추가
            val selectionIndicator = View(holder.itemView.context)
            selectionIndicator.layoutParams = ViewGroup.LayoutParams(
                8, ViewGroup.LayoutParams.MATCH_PARENT)
            selectionIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary))
            
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
            background.setStroke(1, ContextCompat.getColor(holder.itemView.context, R.color.button_disabled))
            background.setColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            
            // 텍스트 일반 스타일로 복원
            holder.stockName.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        
        // 배경 설정
        holder.itemView.background = background
    }

    /**
     * 선택 효과음을 재생합니다.
     */
    private fun playSelectSound() {
        try {
            // WeakReference에서 SoundManager 가져오기
            val soundManager = weakSoundManager?.get()
            if (soundManager != null) {
                soundManager.playSound(SOUND_SELECT)
            }
        } catch (e: Exception) {
            Log.e("StockAdapter", "효과음 재생 오류: ${e.message}")
        }
    }

    /**
     * 데이터 목록의 항목 수를 반환합니다.
     * 
     * @return 주식 목록의 크기
     */
    override fun getItemCount(): Int = stockList.size
    
    /**
     * 주식 목록 데이터를 업데이트합니다.
     * 선택된 주식의 위치를 유지하려고 시도합니다.
     * 
     * @param newStockList 새 주식 목록
     */
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
     * 
     * @param newStockList 새 주식 목록
     */
    fun updateItems(newStockList: List<Stock>) {
        updateData(newStockList)
    }
    
    /**
     * 특정 주식을 선택 상태로 설정합니다.
     * 
     * @param stock 선택할 주식 객체
     */
    fun setSelectedStock(stock: Stock) {
        try {
            // 리스트에서 해당 주식 찾기
            val newPosition = stockList.indexOfFirst { it.name == stock.name }
            
            if (newPosition != -1 && newPosition != selectedStockPosition) {
                val previousPosition = selectedStockPosition
                selectedStockPosition = newPosition
                
                // 이전 선택 항목과 현재 선택 항목 갱신
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(newPosition)
            }
        } catch (e: Exception) {
            Log.e("StockAdapter", "주식 선택 설정 오류: ${e.message}")
        }
    }
    
    /**
     * 어댑터의 리소스를 해제합니다.
     * 사용이 끝나거나 어댑터가 소멸될 때 호출해야 합니다.
     */
    fun release() {
        // 리소스 해제는 SoundManager에서 처리되므로 여기서 필요 없음
    }

    /**
     * 주식 항목 표시를 위한 ViewHolder
     */
    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stockName: TextView = itemView.findViewById(R.id.stockName)
        val stockPrice: TextView = itemView.findViewById(R.id.stockPrice)
        val stockChangeValue: TextView = itemView.findViewById(R.id.stockChangeValue)
        val stockChangeRate: TextView = itemView.findViewById(R.id.stockChangeRate)
        val stockHolding: TextView = itemView.findViewById(R.id.stockHolding)
        val stockGraphButton: ImageButton = itemView.findViewById(R.id.stockGraphButton)
        
        init {
            // 클릭 이벤트 초기화
            itemView.setOnClickListener { 
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < stockList.size) {
                    val stock = stockList[position]
                    val previousPosition = selectedStockPosition
                    selectedStockPosition = position
                    
                    // 효과음 재생
                    playSelectSound()
                    
                    // 이전 선택 항목과 현재 선택 항목 갱신
                    if (previousPosition != -1) {
                        notifyItemChanged(previousPosition)
                    }
                    notifyItemChanged(position)
                    
                    // WeakReference에서 콜백 가져와서 호출
                    weakItemClickCallback.get()?.invoke(stock)
                }
            }
            
            // 그래프 버튼 클릭 이벤트
            stockGraphButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < stockList.size) {
                    val stock = stockList[position]
                    // WeakReference에서 콜백 가져와서 호출
                    weakGraphClickCallback.get()?.invoke(stock)
                }
            }
        }
    }
} 