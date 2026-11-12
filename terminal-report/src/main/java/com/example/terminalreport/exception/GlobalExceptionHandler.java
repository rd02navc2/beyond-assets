package com.example.terminalreport.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/** 統一回傳 {"message": "..."}，前端會直接顯示在提示條。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .limit(5)
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest().body(Map.of("message", "資料格式錯誤：" + detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("請求內容無法解析：{}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", "請求內容不是有效的 JSON"));
    }

    @ExceptionHandler(QueryException.class)
    public ResponseEntity<Map<String, String>> handleQuery(QueryException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDb(DataAccessException ex) {
        log.error("資料庫寫入失敗", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "資料庫寫入失敗，請檢查 MySQL 連線與表結構"));
    }
}
