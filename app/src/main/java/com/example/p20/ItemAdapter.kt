package com.example.p20

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class ItemAdapter(
    private var items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var selectedPosition = -1
    private var selectSound: MediaPlayer? = null

    /**
     * 아이템 목록을 업데이트합니다.
     */
    fun updateItems(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * 선택된 아이템 위치를 설정합니다.
     */
    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
    }

    /**
     * 선택된 아이템을 반환합니다. 없으면 null을 반환합니다.
     */
    fun getSelectedItem(): Item? {
        return if (selectedPosition != -1 && selectedPosition < items.size) {
            items[selectedPosition]
        } else {
            null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
            
        // 효과음 초기화
        if (selectSound == null) {
            selectSound = MediaPlayer.create(parent.context, R.raw.item_button_select)
        }
            
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == selectedPosition)
        holder.itemView.setOnClickListener {
            // 효과음 재생
            playSelectSound()
            setSelectedPosition(position)
            onItemClick(item)
        }
    }
    
    /**
     * 선택 효과음을 재생합니다.
     */
    private fun playSelectSound() {
        selectSound?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }
    }

    override fun getItemCount() = items.size
    
    /**
     * 리소스 해제
     */
    fun release() {
        selectSound?.release()
        selectSound = null
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val priceTextView: TextView = itemView.findViewById(R.id.itemPriceTextView)
        private val stockTextView: TextView = itemView.findViewById(R.id.itemStockTextView)
        private val quantityTextView: TextView = itemView.findViewById(R.id.itemQuantityTextView)

        fun bind(item: Item, isSelected: Boolean) {
            nameTextView.text = item.name
            priceTextView.text = formatCurrency(item.price)
            stockTextView.text = "재고: ${item.stock}개"
            quantityTextView.text = "보유: ${item.quantity}개"

            // 선택된 아이템 하이라이트
            if (isSelected) {
                itemView.setBackgroundResource(R.drawable.selected_item_background)
            } else {
                itemView.background = null
            }
        }

        private fun formatCurrency(amount: Long): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
            return formatter.format(amount)
        }
    }
} 