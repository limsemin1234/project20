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
        
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
            
            // 음소거 스위치와 동기화
            val muteSwitch = view.findViewById<Switch>(R.id.switchMute)
            if (isChecked && muteSwitch.isChecked) {
                muteSwitch.isChecked = false
                prefs.edit().putBoolean("mute_enabled", false).apply()
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
        
        // 효과음 설정 스위치
        val soundEffectSwitch = view.findViewById<Switch>(R.id.switchSoundEffect)
        soundEffectSwitch.isChecked = prefs.getBoolean("sound_effect_enabled", true)
        
        soundEffectSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("sound_effect_enabled", isChecked).apply()
            
            // 음소거 스위치와 동기화
            val muteSwitch = view.findViewById<Switch>(R.id.switchMute)
            if (isChecked && muteSwitch.isChecked) {
                muteSwitch.isChecked = false
                prefs.edit().putBoolean("mute_enabled", false).apply()
            }
            
            // SoundManager 설정 업데이트 (구현 필요)
            (activity as? MainActivity)?.let { mainActivity ->
                // 효과음 설정 변경 - 구현 필요
                mainActivity.updateSoundEffectSettings(isChecked)
            }
            
            val message = if (isChecked) "효과음이 켜졌습니다" else "효과음이 꺼졌습니다"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        
        // 음소거 설정 스위치
        val muteSwitch = view.findViewById<Switch>(R.id.switchMute)
        muteSwitch.isChecked = prefs.getBoolean("mute_enabled", false)
        
        muteSwitch.setOnCheckedChangeListener { _, isChecked ->
            // 설정 저장
            prefs.edit().putBoolean("mute_enabled", isChecked).apply()
            
            // 다른 소리 관련 스위치와 동기화
            if (isChecked) {
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
        
        // 배경음악 선택 Spinner 설정
        setupMusicSpinner(view, prefs)

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
    
    private fun setupMusicSpinner(view: View, prefs: android.content.SharedPreferences) {
        try {
            val musicSpinner = view.findViewById<Spinner>(R.id.musicSpinner)
            
            // 사용 가능한 배경음악 목록
            val musicOptions = arrayOf(
                "배경음악1",
                "배경음악2",
                "배경음악3",
                "배경음악4",
                "배경음악5"
            )
            
            // 스피너 어댑터 설정
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                musicOptions
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            musicSpinner.adapter = adapter
            
            // 저장된 설정 불러오기
            val savedMusicIndex = prefs.getInt("selected_music", 0)
            musicSpinner.setSelection(savedMusicIndex)
            
            // 음악 선택 리스너 설정
            musicSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // 이전 선택값과 같으면 아무것도 하지 않음
                    if (position == savedMusicIndex) return
                    
                    // 설정 저장
                    prefs.edit().putInt("selected_music", position).apply()
                    
                    // 음악 전환
                    (activity as? MainActivity)?.let { mainActivity ->
                        mainActivity.changeBackgroundMusic(position)
                    }
                    
                    // 선택한 음악 이름 메시지로 표시
                    Toast.makeText(requireContext(), "${musicOptions[position]}(으)로 변경되었습니다", Toast.LENGTH_SHORT).show()
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // 아무것도 하지 않음
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsDialog", "음악 스피너 설정 오류: ${e.message}")
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