package com.example.p20

import android.content.Context // SharedPreferences 사용 위해 추가
import android.content.SharedPreferences // SharedPreferences 사용 위해 추가
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// import android.widget.Toast // 제거: Toast 대신 MessageManager 사용
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
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 아이템 관리 Fragment
 * 게임 시간 증가를 위한 아이템의 구매와 사용을 담당합니다.
 */
class ItemFragment : BaseFragment() {

    private var _binding: FragmentItemBinding? = null
    private val binding get() = _binding!!

    // SharedPreferences 관련 상수
    private val PREFS_FILENAME = "item_prefs"
    private val KEY_ITEM_QUANTITY_PREFIX = "item_quantity_"
    private val KEY_ITEM_STOCK_PREFIX = "item_stock_"
    private lateinit var prefs: SharedPreferences

    // 효과음 리소스 ID
    companion object {
        private val SOUND_BUY = R.raw.item_button_buy
        private val SOUND_USE = R.raw.item_button_use
    }

    // 아이템 어댑터
    private lateinit var itemAdapter: ItemAdapter

    // 아이템 목록
    private val itemList = mutableListOf<Item>()

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

        // 아이템 목록 초기화
        initItemList()

        // RecyclerView 설정
        setupRecyclerView()

        // 버튼 리스너 설정
        setupButtons()

        // 게임 리셋 이벤트 관찰
        observeGameResetEvent()

        // 자산 변화 관찰하여 버튼 상태 업데이트
        assetViewModel.asset.observe(viewLifecycleOwner) { _ ->
            updateButtonStates()
        }
    }

    /**
     * 아이템 목록을 초기화합니다.
     */
    private fun initItemList() {
        itemList.clear()

        // 각 아이템 추가 (id, 이름, 설명, 가격, 효과, 초기 재고, 초기 수량)
        itemList.add(
            Item(
                id = 1,
                name = "Time증폭(60초)",
                description = "남은 시간을 60초 증가시킵니다.",
                price = 50000L,
                effect = 60,
                stock = loadItemStock(1),
                quantity = loadItemQuantity(1)
            )
        )

        itemList.add(
            Item(
                id = 2,
                name = "Time증폭(120초)",
                description = "남은 시간을 120초 증가시킵니다.",
                price = 90000L,
                effect = 120,
                stock = loadItemStock(2),
                quantity = loadItemQuantity(2)
            )
        )

        itemList.add(
            Item(
                id = 3,
                name = "Time증폭(180초)",
                description = "남은 시간을 180초 증가시킵니다.",
                price = 130000L,
                effect = 180,
                stock = loadItemStock(3),
                quantity = loadItemQuantity(3)
            )
        )

        // 아이템이 처음 생성될 때만 재고 초기화 (SharedPreferences에 저장된 적이 없는 경우에만)
        val hasInitializedKey = "has_initialized_stocks"
        val hasInitialized = prefs.getBoolean(hasInitializedKey, false)
        
        if (!hasInitialized) {
            // 최초 실행시에만 재고 초기화
            itemList.forEach { item ->
                item.stock = getInitialStock(item.id)
                saveItemStock(item.id, item.stock)
            }
            // 초기화 완료 표시
            prefs.edit().putBoolean(hasInitializedKey, true).apply()
        }
    }

    /**
     * 아이템 ID에 따른 초기 재고를 반환합니다.
     */
    private fun getInitialStock(itemId: Int): Int {
        return 1  // 모든 아이템 재고 1개로 통일
    }

    /**
     * RecyclerView를 설정합니다.
     */
    private fun setupRecyclerView() {
        itemAdapter = ItemAdapter(itemList) { selectedItem ->
            // 아이템이 클릭되었을 때 선택 정보 업데이트
            updateSelectedItemInfo(selectedItem)
            updateButtonStates()
        }

        binding.itemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = itemAdapter
        }
    }

    /**
     * 버튼 리스너를 설정합니다.
     */
    private fun setupButtons() {
        // 구매 버튼
        binding.buyButton.setOnClickListener {
            playBuySound()
            buySelectedItem()
        }

        // 사용 버튼
        binding.useButton.setOnClickListener {
            playUseSound()
            useSelectedItem()
        }

        // 초기 버튼 상태 설정
        updateButtonStates()
    }

    /**
     * 선택된 아이템 정보를 UI에 업데이트합니다.
     */
    private fun updateSelectedItemInfo(item: Item?) {
        if (item != null) {
            binding.selectedItemNameTextView.text = "${item.name} - ${item.description}"
            binding.selectedItemQuantityTextView.text = "보유: ${item.quantity}개"
        } else {
            binding.selectedItemNameTextView.text = "선택된 아이템 없음"
            binding.selectedItemQuantityTextView.text = "보유: 0개"
        }
    }

    /**
     * 버튼 상태를 업데이트합니다.
     */
    private fun updateButtonStates() {
        val selectedItem = itemAdapter.getSelectedItem()
        if (selectedItem != null) {
            // 구매 버튼: 재고가 있고 충분한 자산이 있을 때 활성화
            val currentAsset = assetViewModel.asset.value ?: 0L
            binding.buyButton.isEnabled = selectedItem.stock > 0 && currentAsset >= selectedItem.price

            // 사용 버튼: 보유 수량이 있을 때 활성화
            binding.useButton.isEnabled = selectedItem.quantity > 0
        } else {
            binding.buyButton.isEnabled = false
            binding.useButton.isEnabled = false
        }
    }

    /**
     * 선택된 아이템을 구매합니다.
     */
    private fun buySelectedItem() {
        val selectedItem = itemAdapter.getSelectedItem() ?: return
        val currentAsset = assetViewModel.asset.value ?: 0L

        if (currentAsset >= selectedItem.price && selectedItem.stock > 0) {
            // 자산 감소
            assetViewModel.decreaseAsset(selectedItem.price)

            // 아이템 수량 증가, 재고 감소
            selectedItem.quantity++
            selectedItem.stock--

            // 수량 및 재고 저장
            saveItemQuantity(selectedItem.id, selectedItem.quantity)
            saveItemStock(selectedItem.id, selectedItem.stock)

            // UI 업데이트
            itemAdapter.notifyDataSetChanged()
            updateSelectedItemInfo(selectedItem)
            updateButtonStates()

            // 안내 메시지
            showMessage("${selectedItem.name}을(를) 구매했습니다.")

            // 구매 효과음 재생
            playBuySound()
        } else if (selectedItem.stock <= 0) {
            showMessage("아이템 재고가 없습니다.")
        } else {
            showMessage("자산이 부족합니다.")
        }
    }

    /**
     * 선택된 아이템을 사용합니다.
     */
    private fun useSelectedItem() {
        val selectedItem = itemAdapter.getSelectedItem() ?: return

        if (selectedItem.quantity > 0) {
            // 시간 증가
            timeViewModel.increaseRemainingTime(selectedItem.effect)

            // 아이템 수량 감소
            selectedItem.quantity--

            // 수량 저장
            saveItemQuantity(selectedItem.id, selectedItem.quantity)

            // UI 업데이트
            itemAdapter.notifyDataSetChanged()
            updateSelectedItemInfo(selectedItem)
            updateButtonStates()

            // 안내 메시지
            showMessage("${selectedItem.name}을(를) 사용했습니다. 남은 시간이 ${selectedItem.effect}초 증가합니다.")

            // 사용 효과음 재생
            playUseSound()
        }
    }

    /**
     * 아이템 수량을 불러옵니다.
     */
    private fun loadItemQuantity(itemId: Int): Int {
        return prefs.getInt("${KEY_ITEM_QUANTITY_PREFIX}$itemId", 0)
    }

    /**
     * 아이템 수량을 저장합니다.
     */
    private fun saveItemQuantity(itemId: Int, quantity: Int) {
        prefs.edit().putInt("${KEY_ITEM_QUANTITY_PREFIX}$itemId", quantity).apply()
    }

    /**
     * 아이템 재고를 불러옵니다.
     */
    private fun loadItemStock(itemId: Int): Int {
        return prefs.getInt("${KEY_ITEM_STOCK_PREFIX}$itemId", 0)
    }

    /**
     * 아이템 재고를 저장합니다.
     */
    private fun saveItemStock(itemId: Int, stock: Int) {
        prefs.edit().putInt("${KEY_ITEM_STOCK_PREFIX}$itemId", stock).apply()
    }

    /**
     * 게임 리셋 이벤트를 관찰합니다.
     */
    private fun observeGameResetEvent() {
        timeViewModel.gameResetEvent.observe(viewLifecycleOwner) { isReset ->
            if (isReset) {
                resetItems()
                timeViewModel.consumedGameResetEvent()
            }
        }
    }

    /**
     * 모든 아이템을 리셋합니다.
     */
    private fun resetItems() {
        val editor = prefs.edit()
        
        // 모든 아이템 수량 초기화
        itemList.forEach { item ->
            item.quantity = 0
            editor.putInt("${KEY_ITEM_QUANTITY_PREFIX}${item.id}", 0)
            
            // 재고도 초기값으로 리셋
            val initialStock = getInitialStock(item.id)
            item.stock = initialStock
            editor.putInt("${KEY_ITEM_STOCK_PREFIX}${item.id}", initialStock)
        }
        
        editor.apply()
        
        // UI 업데이트
        itemAdapter.notifyDataSetChanged()
        updateSelectedItemInfo(null)
        updateButtonStates()
    }

    /**
     * 게임 오버 처리를 위한 메서드
     */
    override fun onGameOver() {
        super.onGameOver()
        // 게임 오버 시 아이템 관련 UI 비활성화 추가 처리
        showMessage("아이템 사용이 중단되었습니다.")
    }

    override fun onResume() {
        super.onResume()
        // 화면으로 돌아올 때 아이템 목록 업데이트
        updateItemsFromStorage()
        // 앱 재진입 시 자산 변화에 따른 버튼 상태 업데이트
        updateButtonStates()
    }

    /**
     * BaseFragment에서 상속받은 onReloadSounds 메서드를 오버라이드하여
     * 화면이 다시 보여질 때 효과음을 다시 로드합니다.
     */
    override fun onReloadSounds() {
        // 아이템 효과음 미리 로드
        soundManager.loadSound(SOUND_BUY)
        soundManager.loadSound(SOUND_USE)
        android.util.Log.d("ItemFragment", "아이템 효과음 다시 로드됨")
    }

    /**
     * SharedPreferences에서 아이템 정보를 로드하여 목록을 업데이트합니다.
     */
    private fun updateItemsFromStorage() {
        for (item in itemList) {
            // 저장된 수량과 재고 읽어오기
            item.quantity = loadItemQuantity(item.id)
            item.stock = loadItemStock(item.id)
        }
        // UI 업데이트
        itemAdapter.notifyDataSetChanged()
        
        // 선택된 아이템이 있으면 정보 업데이트
        val selectedItem = itemAdapter.getSelectedItem()
        if (selectedItem != null) {
            updateSelectedItemInfo(selectedItem)
        }
    }

    /**
     * 구매 효과음을 재생합니다.
     */
    private fun playBuySound() {
        soundManager.playSound(SOUND_BUY)
    }

    /**
     * 사용 효과음을 재생합니다.
     */
    private fun playUseSound() {
        soundManager.playSound(SOUND_USE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 