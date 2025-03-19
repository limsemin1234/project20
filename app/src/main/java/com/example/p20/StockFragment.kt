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
    private lateinit var stockStatusText: TextView // 주식 상태를 표시할 TextView
    private lateinit var stockDetailsTextView: LinearLayout // 추가한 LinearLayout (가로로 나열된 텍스트뷰)
    private lateinit var assetViewModel: AssetViewModel

    private var selectedStock: Stock? = null // 선택된 주식 저장


    private lateinit var stockViewModel: StockViewModel

    // 주식 데이터 리스트를 ViewModel에서 가져옴
    private var stockItems: List<Stock> = listOf()


    // 주식 상세 정보를 미리 저장할 TextView 변수들 (nullable로 변경)
    private var avgPurchasePriceData: TextView? = null
    private var profitLossData: TextView? = null
    private var profitRateData: TextView? = null
    private var stockQuantityData: TextView? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.stock_layout, container, false)

        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)


        // LiveData 구독 (주식 가격 변동 시 UI 업데이트)
        stockViewModel.stockItems.observe(viewLifecycleOwner, Observer { updatedStockList ->
            updateStockList(updatedStockList)  // RecyclerView 업데이트
            selectedStock?.let { updateStockDetails(it) }  // 선택된 주식의 상세 정보 업데이트
        })


        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockStatusText = view.findViewById(R.id.stockStatusText) // TextView 연결
        stockDetailsTextView = view.findViewById(R.id.stockDetailsTextView) // 추가된 텍스트뷰
        val buyButton: Button = view.findViewById(R.id.buyButton)
        val sellButton: Button = view.findViewById(R.id.sellButton)


        // 주식 상세 정보를 미리 한 번만 findViewById로 초기화
        avgPurchasePriceData = view.findViewById(R.id.avgPurchasePriceData)
        profitLossData = view.findViewById(R.id.profitLossData)
        profitRateData = view.findViewById(R.id.profitRateData)
        stockQuantityData = view.findViewById(R.id.stockQuantityData)


        // ViewModel 초기화
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)



        // 자산 값 변경 시 UI 업데이트
        assetViewModel.asset.observe(viewLifecycleOwner, Observer { newAsset ->
            // 자산 값을 TextView에 표시하는 로직 추가
            // 예시로 자산을 화면에 표시할 수 있습니다
            stockStatusText.text = "현재 자산: ${String.format("%,d", newAsset)}원"
        })


        // RecyclerView 설정 (세로로 주식 항목을 나열)
        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockStatus("${stock.name}이(가) 선택되었습니다.")
        }
        stockRecyclerView.adapter = stockAdapter



        buyButton.setOnClickListener {
            selectedStock?.let {
                // ViewModel을 통해 자산 가져오기
                val currentAsset = assetViewModel.asset.value ?: 0


                if (currentAsset >= it.price) {
                    assetViewModel.decreaseAsset(it.price) // 자산 차감
                    it.buyStock(it.price) // 주식 매수
                    updateStockStatus("${it.name}을(를) 매수했습니다! 보유량: ${it.holding}주")
                    stockAdapter.notifyDataSetChanged()  // RecyclerView 갱신
                } else {
                    // 자산 부족 시 주식 상태창에 메시지 표시
                    updateStockStatus("자산이 부족합니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }



        // 매도 버튼 클릭 리스너
        sellButton.setOnClickListener {
            selectedStock?.let {
                if (it.holding > 0) {
                    val profitLoss = it.sellStock() // 주식 매도
                    assetViewModel.increaseAsset(it.price) // 자산 증가
                    updateStockStatus("${it.name} 매도! 손익: ${profitLoss}원")
                    stockAdapter.notifyDataSetChanged()  // RecyclerView 갱신
                } else {
                    // 보유한 주식이 없을 경우 메시지 표시
                    updateStockStatus("보유한 주식이 없습니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }

        return view
    }


    private fun updateStockList(stockItems: MutableList<Stock>) {
        // stockItems를 기반으로 UI 업데이트
        // 예: RecyclerView에 데이터 표시
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockStatus("${stock.name}이(가) 선택되었습니다.")
        }
        stockRecyclerView.adapter = stockAdapter
        stockAdapter.notifyDataSetChanged()  // 어댑터 갱신
    }


    // 주식 상태 업데이트 및 선택된 주식 상세 정보 갱신
    private fun updateStockStatus(message: String) {
        stockStatusText.text = "$message"
        selectedStock?.let { updateStockDetails(it) } // 선택된 주식이 있으면 상세 정보 갱신
    }



    // 주식 상세 정보 업데이트
    private fun updateStockDetails(stock: Stock) {
        if (avgPurchasePriceData != null && profitLossData != null && profitRateData != null && stockQuantityData != null) {
            // 주식명 업데이트
            val stockNameTextView: TextView = view?.findViewById(R.id.selectedStockName) ?: return
            stockNameTextView.text = stock.name // 선택된 주식의 이름 표시

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

            // 색상 설정: 상승, 하락, 보합
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

            // 클릭 시 선택된 주식 처리
            holder.itemView.setOnClickListener { onItemClick(stock) }
        }

        override fun getItemCount(): Int = stockList.size
    }

}

