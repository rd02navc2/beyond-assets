package com.example.terminalreport.dto;

import java.time.LocalDateTime;

/**
 * diff 為 null 表示沒有可比較的前一批次（第一次匯出，或前一批次已不存在）。
 * cleared 為 null 表示本次匯出前沒有清空舊資料（設定關閉）。
 */
public record ExportResponse(Long batchId, LocalDateTime exportedAt, int eventCount, int terminalCount,
                             ExportDiff diff, ClearedInfo cleared) {

    /** 匯出前被刪除的舊資料筆數。 */
    public record ClearedInfo(long batches, long events, long terminals) {
    }

    public ExportResponse withDiff(ExportDiff d) {
        return new ExportResponse(batchId, exportedAt, eventCount, terminalCount, d, cleared);
    }
}
