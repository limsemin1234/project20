package com.example.p20.helpers

import android.util.Log

/**
 * 예외 처리 관련 유틸리티 기능을 제공하는 헬퍼 클래스
 * try-catch 블록의 중복 코드 제거를 위해 사용
 */
object ExceptionHelper {
    
    const val TAG = "ExceptionHelper"
    
    /**
     * 안전하게 작업을 실행합니다.
     * 실패 시 로그를 남기고 예외를 처리합니다.
     * 
     * @param tag 로그 태그
     * @param operation 실행할 작업
     * @param errorMessage 오류 메시지
     * @param defaultValue 오류 발생 시 반환할 기본값 (null 허용)
     * @return 작업 결과 또는 오류 발생 시 기본값
     */
    inline fun <T> safeExecute(
        tag: String = TAG,
        errorMessage: String = "작업 실행 오류",
        defaultValue: T? = null,
        operation: () -> T
    ): T? {
        return try {
            operation()
        } catch (e: Exception) {
            Log.e(tag, "$errorMessage: ${e.message}")
            defaultValue
        }
    }
    
    /**
     * 안전하게 작업을 실행하고 boolean 결과를 반환합니다.
     * 실패 시 로그를 남기고 예외를 처리합니다.
     * 
     * @param tag 로그 태그
     * @param operation 실행할 작업
     * @param errorMessage 오류 메시지
     * @return 작업 성공 여부
     */
    inline fun safeExecuteBoolean(
        tag: String = TAG,
        errorMessage: String = "작업 실행 오류",
        operation: () -> Unit
    ): Boolean {
        return try {
            operation()
            true
        } catch (e: Exception) {
            Log.e(tag, "$errorMessage: ${e.message}")
            false
        }
    }
    
    /**
     * 안전하게 리소스를 해제합니다.
     * 
     * @param tag 로그 태그
     * @param resourceName 리소스 이름
     * @param closeOperation 리소스 해제 작업
     */
    inline fun safeClose(
        tag: String = TAG,
        resourceName: String = "리소스",
        closeOperation: () -> Unit
    ) {
        try {
            closeOperation()
        } catch (e: Exception) {
            Log.e(tag, "$resourceName 해제 오류: ${e.message}")
        }
    }
    
    /**
     * 안전하게 UI 작업을 실행합니다.
     * UI 관련 예외를 전부 잡아서 처리합니다.
     * 
     * @param tag 로그 태그
     * @param operation UI 작업
     * @param errorMessage 오류 메시지
     */
    inline fun safeUIOperation(
        tag: String = TAG,
        errorMessage: String = "UI 작업 오류",
        operation: () -> Unit
    ) {
        try {
            operation()
        } catch (e: Exception) {
            Log.e(tag, "$errorMessage: ${e.message}")
        }
    }
    
    /**
     * 리소스 작업을 안전하게 처리합니다.
     * 리소스 초기화, 사용, 해제를 모두 안전하게 처리합니다.
     * 
     * @param tag 로그 태그
     * @param resourceName 리소스 이름
     * @param initOperation 리소스 초기화 작업
     * @param useOperation 리소스 사용 작업
     * @param closeOperation 리소스 해제 작업
     */
    inline fun <T> useResourceSafely(
        tag: String = TAG,
        resourceName: String = "리소스",
        initOperation: () -> T,
        useOperation: (T) -> Unit,
        closeOperation: (T) -> Unit
    ) {
        var resource: T? = null
        try {
            // 리소스 초기화
            resource = initOperation()
            // 리소스 사용
            useOperation(resource)
        } catch (e: Exception) {
            Log.e(tag, "$resourceName 사용 오류: ${e.message}")
        } finally {
            // 리소스 해제
            try {
                resource?.let { closeOperation(it) }
            } catch (e: Exception) {
                Log.e(tag, "$resourceName 해제 오류: ${e.message}")
            }
        }
    }
    
    /**
     * 오류 로그를 남깁니다.
     * 
     * @param tag 로그 태그
     * @param message 로그 메시지
     * @param e 예외
     */
    fun logError(tag: String = TAG, message: String, e: Exception? = null) {
        if (e != null) {
            Log.e(tag, "$message: ${e.message}")
        } else {
            Log.e(tag, message)
        }
    }
} 