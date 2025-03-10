package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer

class AlbaFragment : Fragment() {

    private lateinit var albaViewModel: AlbaViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alba_layout, container, false)

        // ViewModel 초기화
        albaViewModel = ViewModelProvider(requireActivity()).get(AlbaViewModel::class.java)

        // 그림(ImageView) 객체 가져오기
        val albaImage: ImageView = view.findViewById(R.id.albaImage)

        // 터치 횟수를 관찰하여 자산 증가
        albaViewModel.touchCount.observe(viewLifecycleOwner, Observer { count ->
            // 터치 횟수가 10 미만일 때만 자산 증가
            if (count <= 10) {
                albaImage.setOnClickListener {
                    // 터치 횟수 증가
                    albaViewModel.increaseTouchCount()

                    // 자산 증가 (100원씩)
                    if (count < 10) {
                        (activity as? MainActivity)?.increaseAsset(100)
                    }
                }
            } else {
                // 터치 횟수가 10번 이상이면 더 이상 터치 불가
                albaImage.setOnClickListener(null) // 클릭 리스너 제거
            }
        })

        return view
    }
}







