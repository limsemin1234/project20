package com.example.p20

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Looper
import android.widget.LinearLayout
import androidx.lifecycle.Observer




class StockFragment : Fragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockStatusText: TextView // 주식 상태를 표시할 TextView
    private lateinit var stockDetailsTextView: LinearLayout // 추가한 LinearLayout (가로로 나열된 텍스트뷰)
    private lateinit var assetManager: AssetManager // 자산 관리 객체

    private var selectedStock: Stock? = null // 선택된 주식 저장


    private lateinit var stockViewModel: StockViewModel

    // 주식 데이터 리스트를 ViewModel에서 가져옴
    private var stockItems: List<Stock> = listOf()


    // 주식 상세 정보를 미리 저장할 TextView 변수들 (nullable로 변경)
    private var avgPurchasePriceData: TextView? = null
    private var profitLossData: TextView? = null
    private var profitRateData: TextView? = null
    private var stockQuantityData: TextView? = null


    // Handler 설정
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L // 5초 간격으로 주식 업데이트




    // 주식 변동을 주기적으로 업데이트하는 Runnable
    private val updateRunnable = object : Runnable {
        override fun run() {
            stockViewModel.updateStockPrices() // 주식 가격 업데이트
            stockAdapter.notifyDataSetChanged() // RecyclerView 갱신

            // 주식 상세 정보 업데이트
            selectedStock?.let { updateStockDetails(it) }

            handler.postDelayed(this, updateInterval) // 3초마다 반복
        }
    }

    //HANDLER 사용 시 메모리 누수방지 (Fragment나 Activity가 종료될 때 Handler의 콜백을 제거)
    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)  // Handler의 콜백 제거
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.stock_layout, container, false)

        stockViewModel = ViewModelProvider(requireActivity()).get(StockViewModel::class.java)


        // LiveData 구독 (주식 목록 업데이트)
        stockViewModel.stockItems.observe(viewLifecycleOwner, Observer { stockItems ->
            // stockItems는 LiveData에서 가져온 MutableList<Stock>입니다
            updateStockList(stockItems)
        })

        // 예시: 3초마다 주식 가격 업데이트
        Handler(Looper.getMainLooper()).postDelayed({
            stockViewModel.updateStockPrices()
        }, 3000)



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




        assetManager = AssetManager()  // 자산 관리 객체 초기화




        // RecyclerView 설정 (세로로 주식 항목을 나열)
        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockStatus("${stock.name}이(가) 선택되었습니다.")
        }
        stockRecyclerView.adapter = stockAdapter




        buyButton.setOnClickListener {
            selectedStock?.let {
                if (assetManager.getAsset() >= it.price) {
                    assetManager.decreaseAsset(it.price) // 자산 차감
                    it.buyStock(it.price) // 주식 매수
                    updateStockStatus("${it.name}을(를) 매수했습니다! 보유량: ${it.holding}주")
                    (activity as? MainActivity)?.increaseAsset(-it.price) // MainActivity 자산 갱신
                    stockAdapter.notifyDataSetChanged()  // RecyclerView 갱신
                } else {
                    // 자산 부족 시 주식 상태창에 메시지 표시
                    updateStockStatus("자산이 부족합니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }



        sellButton.setOnClickListener {
            selectedStock?.let {
                if (it.holding > 0) {
                    val profitLoss = it.sellStock() // 주식 매도
                    assetManager.increaseAsset(it.price) // 자산 증가
                    (activity as? MainActivity)?.increaseAsset(it.price) // MainActivity 자산 갱신
                    updateStockStatus("${it.name} 매도! 손익: ${profitLoss}원")
                    stockAdapter.notifyDataSetChanged()  // RecyclerView 갱신
                } else {
                    // 보유한 주식이 없을 경우 메시지 표시
                    updateStockStatus("보유한 주식이 없습니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }


        // 주식 변동을 주기적으로 업데이트 시작
        handler.post(updateRunnable)

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

