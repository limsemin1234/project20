package com.example.p20

import android.content.Context // SharedPreferences 사용 위해 추가
import android.content.SharedPreferences // SharedPreferences 사용 위해 추가
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // 제거 또는 주석처리
import androidx.fragment.app.activityViewModels
import com.example.p20.databinding.FragmentItemBinding // View Binding import
import com.example.p20.AssetViewModel
import com.example.p20.TimeViewModel
import com.google.android.material.snackbar.Snackbar // Snackbar import 추가
import java.text.NumberFormat
import java.util.Locale
import android.graphics.Color // Color 사용 위해 추가
import android.view.Gravity // Gravity 사용 위해 추가
import android.widget.FrameLayout // FrameLayout 사용 위해 추가

class ItemFragment : Fragment() {

    private var _binding: FragmentItemBinding? = null
    private val binding get() = _binding!!

    // ViewModel 공유
    private val assetViewModel: AssetViewModel by activityViewModels()
    private val timeViewModel: TimeViewModel by activityViewModels()

    // SharedPreferences 관련 상수
    private val PREFS_FILENAME = "item_prefs"
    private val KEY_ITEM_QUANTITY = "time_amplifier_quantity"
    private lateinit var prefs: SharedPreferences

    // 아이템 정보
    private val itemPrice = 100_000L // Time증폭 아이템 가격 10만원
    private var itemQuantity = 0 // Time증폭 아이템 보유 수량 (이제 SharedPreferences에서 로드)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SharedPreferences 초기화
        prefs = requireContext().getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

        // 저장된 보유 수량 로드
        loadItemQuantity()

        // 초기 UI 설정 (로드 이후 호출)
        updateItemUI()

        // 구매 버튼 클릭 리스너
        binding.buyButton.setOnClickListener {
            buyItem()
        }

        // 사용 버튼 클릭 리스너
        binding.useButton.setOnClickListener {
            useItem()
        }

        // --- 추가: 게임 리셋 이벤트 관찰 ---
        timeViewModel.gameResetEvent.observe(viewLifecycleOwner) { isReset ->
            if (isReset) {
                loadItemQuantity() // 보유 수량 다시 로드
                updateItemUI()     // UI 갱신
                timeViewModel.consumedGameResetEvent() // 이벤트 소비 (선택 사항)
            }
        }
        // --- 추가 끝 ---
    }

    // 저장된 아이템 수량 로드 함수
    private fun loadItemQuantity() {
        itemQuantity = prefs.getInt(KEY_ITEM_QUANTITY, 0)
    }

    // 아이템 수량 저장 함수
    private fun saveItemQuantity() {
        with(prefs.edit()) {
            putInt(KEY_ITEM_QUANTITY, itemQuantity)
            apply() // 비동기 저장
        }
    }

    // UI 업데이트 함수
    private fun updateItemUI() {
        binding.itemPriceTextView.text = formatCurrency(itemPrice)
        binding.itemQuantityTextView.text = "${itemQuantity}개"
        // 보유 수량이 1 이상일 때만 사용 버튼 활성화
        binding.useButton.isEnabled = itemQuantity > 0
    }

    // 구매 로직
    private fun buyItem() {
        val currentAsset = assetViewModel.asset.value ?: 0L
        if (currentAsset >= itemPrice) {
            assetViewModel.decreaseAsset(itemPrice)
            itemQuantity++
            saveItemQuantity()
            updateItemUI()
            // Snackbar.make(requireView(), "Time증폭 아이템을 구매했습니다.", Snackbar.LENGTH_SHORT).show()
            showCustomSnackbar("Time증폭 아이템을 구매했습니다.")
        } else {
            // Snackbar.make(requireView(), "자산이 부족합니다.", Snackbar.LENGTH_SHORT).show()
            showCustomSnackbar("자산이 부족합니다.")
        }
    }

    // 사용 로직
    private fun useItem() {
        if (itemQuantity > 0) {
            timeViewModel.increaseRemainingTime(60)
            itemQuantity--
            saveItemQuantity()
            updateItemUI()
            // Snackbar.make(requireView(), "Time증폭 아이템을 사용했습니다. 남은 시간이 1분 증가합니다.", Snackbar.LENGTH_SHORT).show()
            showCustomSnackbar("Time증폭 아이템을 사용했습니다. 남은 시간이 1분 증가합니다.")
        }
    }

    // --- 수정: 상단 -> 중앙 스낵바 표시 함수, 투명도 조절 ---
    private fun showCustomSnackbar(message: String) { // 함수 이름 변경
        val snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        // 배경 투명도 설정 (약 60% 불투명한 어두운 회색)
        snackbarView.setBackgroundColor(Color.argb(150, 50, 50, 50)) // alpha 값 150으로 변경
        // 중앙으로 이동 시도
        try {
            val params = snackbarView.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER // Gravity.TOP -> Gravity.CENTER
            snackbarView.layoutParams = params
        } catch (e: ClassCastException) {
            // 부모 레이아웃이 FrameLayout이 아닐 경우 예외 발생 가능
        }
        snackbar.show()
    }
    // --- 수정 끝 ---

    // 숫자 포맷팅 함수 (원화)
    private fun formatCurrency(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
        return formatter.format(amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
} 