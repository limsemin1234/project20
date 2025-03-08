package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class AlbaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alba_layout, container, false)

        // 그림(ImageView) 객체 가져오기
        val albaImage: ImageView = view.findViewById(R.id.albaImage)

        // 그림 터치 시 자산 증가
        albaImage.setOnClickListener {
            // 100원 증가
            (activity as? MainActivity)?.increaseAsset(100)
        }

        return view
    }
}
