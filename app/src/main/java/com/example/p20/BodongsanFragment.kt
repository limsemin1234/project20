package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import java.text.NumberFormat

class BodongsanFragment : Fragment() {

    private lateinit var assetViewModel: AssetViewModel // 자산 관리 ViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var purchaseButton: Button
    private lateinit var priceText: TextView
    private lateinit var earningsText: TextView
    private lateinit var selectedBodongsanInfo: View // 선택된 부동산 상세 정보 영역

    private val bodongsanList = mutableListOf<Bodongsan>() // 부동산 목록 데이터 (예시)

    // 예시 데이터
    private fun getBodongsanList(): List<Bodongsan> {
        return listOf(
            Bodongsan("서울 아파트", 500000000, 5.0),
            Bodongsan("부산 빌라", 200000000, 3.0),
            Bodongsan("제주도 리조트", 800000000, 7.0)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_bodongsan, container, false)

        // 자산 ViewModel 초기화
        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)

        // RecyclerView 설정
        recyclerView = binding.findViewById(R.id.recyclerViewBodongsan)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = BodongsanAdapter(getBodongsanList(), ::onBodongsanSelected)

        // 선택된 부동산 정보 뷰
        selectedBodongsanInfo = binding.findViewById(R.id.selectedBodongsanInfo)

        // 가격 및 수익률 텍스트
        priceText = binding.findViewById(R.id.priceText)
        earningsText = binding.findViewById(R.id.earningsText)

        // 구매 버튼 처리
        purchaseButton = binding.findViewById(R.id.purchaseButton)
        purchaseButton.setOnClickListener {
            val selectedBodongsan = selectedBodongsanInfo.tag as? Bodongsan
            selectedBodongsan?.let {
                if (assetViewModel.asset.value!! >= it.price) {
                    assetViewModel.decreaseAsset(it.price) // 자산 차감
                    // 구매 완료 후 UI 업데이트
                    // 구매한 부동산을 목록에 추가하는 등의 작업을 진행할 수 있음
                } else {
                    // 자산 부족 메시지 표시
                }
            }
        }

        return binding
    }

    private fun onBodongsanSelected(bodongsan: Bodongsan) {
        // 부동산 선택 시 상세 정보 표시
        selectedBodongsanInfo.visibility = View.VISIBLE
        priceText.text = "가격: ${bodongsan.price}원"
        earningsText.text = "예상 수익률: ${bodongsan.earningsRate}%"
        selectedBodongsanInfo.tag = bodongsan // 선택한 부동산 객체 저장
    }

    // 부동산 항목을 나타내는 Adapter
    class BodongsanAdapter(
        private val bodongsanList: List<Bodongsan>,
        private val onBodongsanClick: (Bodongsan) -> Unit
    ) : RecyclerView.Adapter<BodongsanAdapter.BodongsanViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BodongsanViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.bodongsan_item_layout, parent, false)
            return BodongsanViewHolder(view)
        }

        override fun onBindViewHolder(holder: BodongsanViewHolder, position: Int) {
            val bodongsan = bodongsanList[position]
            holder.bind(bodongsan, onBodongsanClick)
        }

        override fun getItemCount() = bodongsanList.size

        class BodongsanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.textViewName)
            private val priceText: TextView = itemView.findViewById(R.id.textViewPrice)
            private val rentYieldText: TextView = itemView.findViewById(R.id.textViewRentYield)

            fun bind(bodongsan: Bodongsan, onBodongsanClick: (Bodongsan) -> Unit) {
                nameText.text = bodongsan.name

                // 천 단위로 구분된 가격을 표시
                val formattedPrice = NumberFormat.getNumberInstance().format(bodongsan.price)
                priceText.text = "₩$formattedPrice" // 예: ₩500,000

                rentYieldText.text = "${bodongsan.earningsRate}%" // 수익률 퍼센트 표시

                itemView.setOnClickListener {
                    onBodongsanClick(bodongsan)
                }
            }
        }
    }


    // 부동산 데이터 모델 클래스
    data class Bodongsan(
        val name: String, // 부동산 이름
        val price: Int, // 가격
        val earningsRate: Double // 예상 수익률
    )
}
