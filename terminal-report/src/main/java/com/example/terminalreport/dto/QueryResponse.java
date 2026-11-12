package com.example.terminalreport.dto;

import java.util.List;

/** 查詢結果：columns 與 rows 以陣列對應，保留欄位順序（也允許重複欄名）。 */
public record QueryResponse(
        List<String> columns,
        List<List<Object>> rows,
        int rowCount,
        boolean truncated,
        int maxRows,
        long elapsedMs) {
}
