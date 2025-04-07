package com.example.p20

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import androidx.fragment.app.DialogFragment
import android.widget.Switch
import android.widget.Toast
import android.widget.ImageButton

class SettingsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // 다이얼로그 창 외부 배경을 어둡게 설정
        dialog.window?.setDimAmount(0.8f)
        
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기화 버튼
        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            // MainActivity로 이동
            val mainActivity = activity as? MainActivity
            mainActivity?.let {
                // 다이얼로그 닫기
                dismiss()
                
                // ResetFragment 표시
                it.showFragment(ResetFragment())
            }
        }

        // 소리 설정 스위치
        val soundSwitch = view.findViewById<Switch>(R.id.switchSound)
        
        // SharedPreferences에서 설정 불러오기
        val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        soundSwitch.isChecked = prefs.getBoolean("sound_enabled", true)
        
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            
            val message = if (isChecked) "소리가 켜졌습니다" else "소리가 꺼졌습니다"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // 진동 설정 스위치
        val vibrationSwitch = view.findViewById<Switch>(R.id.switchVibration)
        vibrationSwitch.isChecked = prefs.getBoolean("vibration_enabled", true)
        
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
            
            val message = if (isChecked) "진동이 켜졌습니다" else "진동이 꺼졌습니다"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        // X 닫기 버튼
        view.findViewById<ImageButton>(R.id.btnCloseX).setOnClickListener {
            dismiss()
        }
        
        // 종료 버튼
        view.findViewById<Button>(R.id.btnExit).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("게임 종료")
                .setMessage("정말 종료하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    // 주식 데이터 저장 등 필요한 저장 작업
                    val mainActivity = activity as? MainActivity
                    mainActivity?.let {
                        // 액티비티가 있는 경우 종료 처리
                        // stockViewModel이 MainActivity 내부 프로퍼티로 선언되어 있어 직접 접근 불가
                        dismiss()
                        // MainActivty에 추가한 public 메소드를 호출하여 데이터 저장 후 종료
                        mainActivity.saveDataAndExit()
                    } ?: run {
                        // 액티비티 참조가 없는 경우 안전하게 종료
                        requireActivity().finishAffinity()
                    }
                }
                .setNegativeButton("아니오", null)
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        
        // 다이얼로그 크기 설정
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        const val TAG = "SettingsDialogFragment"
    }
} 