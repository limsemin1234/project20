package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import androidx.lifecycle.Observer

class StockFragment : Fragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockStatusText: TextView
    private lateinit var stockDetailsTextView: LinearLayout
    private lateinit var assetViewModel: AssetViewModel

    private var selectedStock: Stock? = null

    private lateinit var stockViewModel: StockViewModel
    private var stockItems: List<Stock> = listOf()

    private var avgPurchasePriceData: TextView? = null
    private var profitLossData: TextView? = null
    private var profitRateData: TextView? = null
    private var stockQuantityData: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stock, container, false)

        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)

        stockViewModel.stockItems.observe(viewLifecycleOwner, Observer { updatedStockList ->
            updateStockList(updatedStockList)
            selectedStock?.let { updateStockDetails(it) }
        })

        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockStatusText = view.findViewById(R.id.stockStatusText)
        stockDetailsTextView = view.findViewById(R.id.stockDetailsTextView)

        val buyButton: Button = view.findViewById(R.id.buyButton)
        val sellButton: Button = view.findViewById(R.id.sellButton)
        val buyAllButton: Button = view.findViewById(R.id.buyAllButton)
        val sellAllButton: Button = view.findViewById(R.id.sellAllButton)

        avgPurchasePriceData = view.findViewById(R.id.avgPurchasePriceData)
        profitLossData = view.findViewById(R.id.profitLossData)
        profitRateData = view.findViewById(R.id.profitRateData)
        stockQuantityData = view.findViewById(R.id.stockQuantityData)

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        assetViewModel.asset.observe(viewLifecycleOwner, Observer { newAsset ->
            stockStatusText.text = "현재 자산: ${String.format("%,d", newAsset)}원"
        })

        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockStatus("${stock.name}이(가) 선택되었습니다.")
        }
        stockRecyclerView.adapter = stockAdapter

        buyButton.setOnClickListener {
            selectedStock?.let {
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= it.price.toLong()) {
                    assetViewModel.decreaseAsset(it.price.toLong())
                    stockViewModel.buyStock(it)
                    updateStockStatus("${it.name}을(를) 매수했습니다! 보유량: ${it.holding}주")
                    stockAdapter.notifyDataSetChanged()
                } else {
                    updateStockStatus("자산이 부족합니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }

        sellButton.setOnClickListener {
            selectedStock?.let {
                if (it.holding > 0) {
                    stockViewModel.sellStock(it)
                    assetViewModel.increaseAsset(it.price.toLong())
                    val profitLoss = it.getProfitLoss()
                    updateStockStatus("${it.name} 매도! 손익: ${profitLoss}원")
                    stockAdapter.notifyDataSetChanged()
                } else {
                    updateStockStatus("보유한 주식이 없습니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }

        buyAllButton.setOnClickListener {
            selectedStock?.let { stock ->
                val currentAsset = assetViewModel.asset.value ?: 0L
                if (currentAsset >= stock.price.toLong()) {
                    val buyCount = stockViewModel.buyAllStock(stock, currentAsset)
                    val usedAsset = stock.price.toLong() * buyCount
                    assetViewModel.decreaseAsset(usedAsset)
                    updateStockStatus("${stock.name}을(를) ${buyCount}주 전체 매수했습니다!")
                    stockAdapter.notifyDataSetChanged()
                } else {
                    updateStockStatus("자산이 부족합니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }

        sellAllButton.setOnClickListener {
            selectedStock?.let { stock ->
                if (stock.holding > 0) {
                    val sellCount = stockViewModel.sellAllStock(stock)
                    val gain = stock.price.toLong() * sellCount
                    assetViewModel.increaseAsset(gain)
                    updateStockStatus("${stock.name} ${sellCount}주 전체 매도 완료!")
                    stockAdapter.notifyDataSetChanged()
                } else {
                    updateStockStatus("보유한 주식이 없습니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }

        return view
    }

    private fun updateStockList(stockItems: MutableList<Stock>) {
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockStatus("${stock.name}이(가) 선택되었습니다.")
        }
        stockRecyclerView.adapter = stockAdapter
        stockAdapter.notifyDataSetChanged()
    }

    private fun updateStockStatus(message: String) {
        stockStatusText.text = "$message"
        selectedStock?.let { updateStockDetails(it) }
    }

    private fun updateStockDetails(stock: Stock) {
        if (avgPurchasePriceData != null && profitLossData != null && profitRateData != null && stockQuantityData != null) {
            val stockNameTextView: TextView = view?.findViewById(R.id.selectedStockName) ?: return
            stockNameTextView.text = stock.name

            if (stock.holding > 0) {
                val avgPurchasePrice = stock.getAvgPurchasePrice()
                val profitLoss = stock.getProfitLoss()
                val profitRate = stock.getProfitRate()

                avgPurchasePriceData?.text = "${avgPurchasePrice}원"
                profitLossData?.text = "${profitLoss}원"
                profitRateData?.text = "${"%.2f".format(profitRate)}%"
                stockQuantityData?.text = "${stock.holding}주"
            } else {
                avgPurchasePriceData?.text = "0원"
                profitLossData?.text = "0원"
                profitRateData?.text = "0%"
                stockQuantityData?.text = "0주"
            }
        }
    }

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stockName: TextView = itemView.findViewById(R.id.stockName)
        val stockPrice: TextView = itemView.findViewById(R.id.stockPrice)
        val stockChangeValue: TextView = itemView.findViewById(R.id.stockChangeValue)
        val stockChangeRate: TextView = itemView.findViewById(R.id.stockChangeRate)
        val stockHolding: TextView = itemView.findViewById(R.id.stockHolding)
    }

    class StockAdapter(
        private val stockList: List<Stock>,
        private val onItemClick: (Stock) -> Unit
    ) : RecyclerView.Adapter<StockViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.stock_item_layout, parent, false)
            return StockViewHolder(view)
        }

        override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
            val stock = stockList[position]

            holder.stockName.text = stock.name
            holder.stockPrice.text = "${stock.price}원"
            holder.stockChangeValue.text = "${stock.changeValue}원"
            holder.stockChangeRate.text = "${"%.2f".format(stock.changeRate)}%"
            holder.stockHolding.text = "${stock.holding}주"

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

            holder.itemView.setOnClickListener { onItemClick(stock) }
        }

        override fun getItemCount(): Int = stockList.size
    }
}
