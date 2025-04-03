package com.example.p20

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class RealInfoFragment : Fragment() {
    private lateinit var timeViewModel: TimeViewModel
    private lateinit var timeText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_real_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timeViewModel = ViewModelProvider(requireActivity()).get(TimeViewModel::class.java)
        timeText = view.findViewById(R.id.timeText)

        timeViewModel.time.observe(viewLifecycleOwner) { newTime ->
            timeText.text = "현재 시간: $newTime"
        }
    }
} 