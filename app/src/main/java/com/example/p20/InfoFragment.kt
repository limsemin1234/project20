package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.p20.databinding.FragmentInfoBinding
import androidx.lifecycle.Observer


class InfoFragment : Fragment() {

    private lateinit var assetViewModel: AssetViewModel
    private lateinit var assetTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentInfoBinding.inflate(inflater, container, false)

        assetViewModel = ViewModelProvider(requireActivity()).get(AssetViewModel::class.java)
        assetTextView = binding.assetInfoTextView  // 자산 텍스트를 표시하는 TextView

        // 초기 자산 값 표시
        assetViewModel.asset.observe(viewLifecycleOwner, Observer { newAsset ->
            assetTextView.text = assetViewModel.getAssetText()
        })

        // 자산 초기화 버튼 클릭 시 자산을 초기화
        binding.resetAssetButton.setOnClickListener {
            assetViewModel.resetAsset()
        }

        return binding.root
    }
}

