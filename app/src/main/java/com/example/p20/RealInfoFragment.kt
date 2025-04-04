package com.example.p20

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class RealInfoFragment : Fragment() {
    private lateinit var viewModel: TimeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_real_info, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel 초기화
        viewModel = ViewModelProvider(requireActivity())[TimeViewModel::class.java]

        // 기존 정보 TextView들에 데이터 표시 (필요 시)
        val assetTextView = view.findViewById<TextView>(R.id.assetTextView)

        // 예시: AssetViewModel 데이터 표시 (다른 ViewModel도 유사하게 처리)
        val assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        assetViewModel.asset.observe(viewLifecycleOwner) {
             assetTextView.text = "현재 자산: ${String.format("%,d", it)}원"
        }
    }
} 