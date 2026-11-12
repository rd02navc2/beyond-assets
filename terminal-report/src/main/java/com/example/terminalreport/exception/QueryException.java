package com.example.terminalreport.exception;

/** SQL 查詢被拒絕或執行失敗；訊息會直接顯示在網頁上。 */
public class QueryException extends RuntimeException {
    public QueryException(String message) {
        super(message);
    }
}
