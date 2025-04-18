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
import androidx.appcompat.app.AlertDialog
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.AdapterView
import android.widget.SeekBar

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

        // SharedPreferences에서 설정 불러오기
        val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        // 배경음악 설정 스위치
        val soundSwitch = view.findViewById<Switch>(R.id.switchSound)
        soundSwitch.isChecked = prefs.getBoolean("sound_enabled", true)
        
        // 효과음 설정 스위치 
        val soundEffectSwitch = view.findViewById<Switch>(R.id.switchSoundEffect)
        soundEffectSwitch.isChecked = prefs.getBoolean("sound_effect_enabled", true)
        
        // 음소거 설정 스위치
        val muteSwitch = view.findViewById<Switch>(R.id.switchMute)
        muteSwitch.isChecked = prefs.getBoolean("mute_enabled", false)
        
        // 음소거 상태에 따라 스위치 초기 상태 설정
        soundSwitch.isEnabled = !muteSwitch.isChecked
        soundEffectSwitch.isEnabled = !muteSwitch.isChecked

        // 배경음악 설정 스위치 리스너
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            
            // 음소거 스위치와 동기화 (배경음악 켰을 때 음소거 해제)
            if (isChecked && muteSwitch.isChecked) {
                muteSwitch.isChecked = false
                prefs.edit().putBoolean("mute_enabled", false).apply()
                
                // 다른 스위치들도 다시 활성화
                soundSwitch.isEnabled = true
                soundEffectSwitch.isEnabled = true
            }
            
            // MainActivity의 배경음악 설정 변경
            (activity as? MainActivity)?.let { mainActivity ->
                if (isChecked) {
                    // 음악 활성화
                    mainActivity.startBackgroundMusic()
                } else {
                    // 음악 비활성화
                    mainActivity.stopBackgroundMusic()
                }
            }
            
            val message = if (isChecked) "배경음악이 켜졌습니다" else "배경음악이 꺼졌습니다"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // 효과음 설정 스위치 리스너
        soundEffectSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("sound_effect_enabled", isChecked).apply()
            
            // 음소거 스위치와 동기화 (효과음 켰을 때 음소거 해제)
            if (isChecked && muteSwitch.isChecked) {
                muteSwitch.isChecked = false
                prefs.edit().putBoolean("mute_enabled", false).apply()
                
                // 다른 스위치들도 다시 활성화
                soundSwitch.isEnabled = true
                soundEffectSwitch.isEnabled = true
            }
            
            // 효과음 설정 업데이트
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.updateSoundEffectSettings(isChecked)
            }
            
            val message = if (isChecked) "효과음이 켜졌습니다" else "효과음이 꺼졌습니다"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // 음소거 설정 스위치 리스너
        muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("mute_enabled", isChecked).apply()
            
            // 다른 소리 관련 스위치와 동기화
            if (isChecked) {
                // 음소거 켜면 다른 소리 설정 스위치 비활성화 (꺼짐 상태로)
                soundSwitch.isChecked = false
                soundEffectSwitch.isChecked = false
                prefs.edit()
                    .putBoolean("sound_enabled", false)
                    .putBoolean("sound_effect_enabled", false)
                    .apply()
                
                // 모든 소리 비활성화
                (activity as? MainActivity)?.let { mainActivity ->
                    mainActivity.stopBackgroundMusic()
                    mainActivity.updateSoundEffectSettings(false)
                }
                
                // 스위치 시각적 비활성화 (선택 불가능하게)
                soundSwitch.isEnabled = false
                soundEffectSwitch.isEnabled = false
            } else {
                // 음소거 해제하면 소리 설정 스위치들 다시 활성화
                soundSwitch.isEnabled = true
                soundEffectSwitch.isEnabled = true
            }
            
            // 효과음 설정 변경 알림 브로드캐스트 전송
            (activity as? MainActivity)?.let { mainActivity ->
                val intent = android.content.Intent("com.example.p20.SOUND_SETTINGS_CHANGED")
                intent.putExtra("mute_enabled", isChecked)
                mainActivity.sendBroadcast(intent)
            }
            
            val message = if (isChecked) "모든 소리가 음소거 되었습니다" else "음소거가 해제되었습니다"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // 볼륨 조절 슬라이더
        val volumeSeekBar = view.findViewById<SeekBar>(R.id.volumeSeekBar)
        volumeSeekBar.progress = prefs.getInt("volume_level", 70)
        
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 실시간으로 볼륨 조절 (선택적)
                if (fromUser) {
                    (activity as? MainActivity)?.let { mainActivity ->
                        mainActivity.setVolume(progress.toFloat() / 100f)
                    }
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 필요 시 구현
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 설정 저장
                val progress = seekBar?.progress ?: 70
                prefs.edit().putInt("volume_level", progress).apply()
                
                // 볼륨 조절 적용
                (activity as? MainActivity)?.let { mainActivity ->
                    mainActivity.setVolume(progress.toFloat() / 100f)
                }
                
                Toast.makeText(requireContext(), "볼륨: ${progress}%", Toast.LENGTH_SHORT).show()
            }
        })
        
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
        
        // 설명 버튼
        view.findViewById<Button>(R.id.btnExplanation).setOnClickListener {
            val mainActivity = activity as? MainActivity
            mainActivity?.let {
                // 다이얼로그 닫기
                dismiss()
                
                // ExplanationFragment 표시
                val explanationFragment = ExplanationFragment()
                it.supportFragmentManager.beginTransaction()
                    .add(R.id.contentFrame, explanationFragment, "ExplanationFragment")
                    .commit()
            }
        }
        
        // 종료 버튼
        view.findViewById<Button>(R.id.btnExit).setOnClickListener {
            showExitConfirmationDialog()
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

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("게임 나가기")
            .setMessage("정말로 게임을 나가시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                // 데이터 저장 후 앱 종료 (주식 데이터 초기화하지 않음)
                (activity as? MainActivity)?.let { mainActivity ->
                    // 현재 상태 저장만 하고 초기화하지 않음
                    mainActivity.stockViewModel.saveStockData()
                    mainActivity.assetViewModel.saveAssetToPreferences()
                    mainActivity.finishAffinity()
                }
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    companion object {
        const val TAG = "SettingsDialogFragment"
    }
} 