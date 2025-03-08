package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Looper



class StockFragment : Fragment() {

    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var stockStatusText: TextView // 주식 상태를 표시할 TextView
    private var selectedStock: Stock? = null // 선택된 주식 저장
    private lateinit var assetManager: AssetManager // 자산 관리 객체

    // 주식 데이터 리스트 (가격, 변동값, 변동률)
    private val stockItems = listOf(
        Stock("주식 1", 10000, 0, 0.0, 0),
        Stock("주식 2", 10000, 0, 0.0, 0),
        Stock("주식 3", 10000, 0, 0.0, 0),
        Stock("주식 4", 10000, 0, 0.0, 0)
    )

    // Handler 설정
    private val handler = Handler(Looper.getMainLooper())

    private val updateInterval = 5000L // 5초 간격으로 주식 업데이트

    // 주식 변동을 주기적으로 업데이트하는 Runnable
    private val updateRunnable = object : Runnable {
        override fun run() {
            stockItems.forEach { it.updateChangeValue() } // 변동값 업데이트
            stockAdapter.notifyDataSetChanged() // RecyclerView 갱신
            handler.postDelayed(this, updateInterval) // 5초마다 반복
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.stock_layout, container, false)

        stockRecyclerView = view.findViewById(R.id.stockRecyclerView)
        stockStatusText = view.findViewById(R.id.stockStatusText) // TextView 연결
        val buyButton: Button = view.findViewById(R.id.buyButton)
        val sellButton: Button = view.findViewById(R.id.sellButton)

        assetManager = AssetManager()  // 자산 관리 객체 초기화

        // RecyclerView 설정 (세로로 주식 항목을 나열)
        stockRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        stockAdapter = StockAdapter(stockItems) { stock ->
            selectedStock = stock
            updateStockStatus("${stock.name}이(가) 선택되었습니다.")
        }
        stockRecyclerView.adapter = stockAdapter




        // 매수 버튼 클릭 이벤트
        buyButton.setOnClickListener {
            selectedStock?.let {
                // 자산 차감 및 보유량 증가
                if (assetManager.getAsset() >= it.price) {
                    (activity as? MainActivity)?.increaseAsset(-it.price)  // MainActivity의 increaseAsset 호출
                    it.buyStock(it.price) // 매입 가격 추가
                    updateStockStatus("${it.name}을(를) 매수했습니다! 보유량: ${it.holding}주")

                } else {
                    updateStockStatus("자산이 부족합니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }


        // 매도 버튼 클릭 이벤트
        sellButton.setOnClickListener {
            selectedStock?.let {
                if (it.holding > 0) {
                    val profitLoss = it.sellStock()
                    (activity as? MainActivity)?.increaseAsset(it.price)
                    updateStockStatus("${it.name} 매도! 손익: ${profitLoss}원")
                } else {
                    updateStockStatus("보유한 주식이 없습니다!")
                }
            } ?: updateStockStatus("주식을 선택하세요.")
        }

        // 주식 변동을 주기적으로 업데이트 시작
        handler.post(updateRunnable)

        return view
    }





    // 주식 상태 업데이트 함수
    private fun updateStockStatus(message: String) {
        stockStatusText.text = "주식 상태: $message"
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
            holder.itemView.setOnClickListener { onItemClick(stock) }
        }

        override fun getItemCount(): Int = stockList.size
    }








    data class Stock(
        val name: String,        // 주식 이름
        var price: Int,          // 주식 가격
        var changeValue: Int,    // 변동값
        var changeRate: Double,  // 변동률
        var holding: Int,        // 보유량
        val purchasePrices: MutableList<Int> = mutableListOf() // 매입 가격 리스트
    ) {
        // 변동값을 -500에서 +500 사이로 랜덤하게 설정 (십의자리까지 0으로 설정)
        fun updateChangeValue() {
            changeValue = ((Math.random() * 1001 - 500) / 10).toInt() * 10  // -500 ~ +500 범위, 십의자리까지 0으로 설정
            updatePriceAndChangeValue()
        }

        // 변동값에 맞춰 가격을 업데이트하고, 변동률도 계산
        private fun updatePriceAndChangeValue() {
            val oldPrice = price
            price = maxOf(price + changeValue, 10) // 최소 가격을 10원으로 설정
            changeRate = ((changeValue.toDouble() / oldPrice) * 100)  // 변동률 계산 (소수점 2자리)
        }

        // 매수 함수: 보유량 증가 및 자산에서 가격 차감
        fun buyStock(purchasePrice: Int) {
            holding += 1
            purchasePrices.add(purchasePrice) // 매입 가격 추가
        }

        fun sellStock(): Int {
            if (holding > 0) {
                holding -= 1
                val boughtPrice = purchasePrices.removeAt(0) // 가장 먼저 산 가격을 기준으로 매도
                return price - boughtPrice // 이익(+) or 손실(-) 반환
            }
            return 0
        }
    }

}
