package com.example.terminalreport.dto;

import com.example.terminalreport.entity.ExportBatch;

import java.time.LocalDateTime;

/** 匯出紀錄清單用的精簡檢視。 */
public record ExportBatchView(
        Long id,
        String fileName,
        LocalDateTime exportedAt,
        Integer totalTerminals,
        Integer totalEvents,
        int eventRowCount,
        int terminalRowCount,
        String filterDesc) {

    public static ExportBatchView from(ExportBatch b) {
        return new ExportBatchView(
                b.getId(), b.getFileName(), b.getExportedAt(),
                b.getTotalTerminals(), b.getTotalEvents(),
                b.getEventRowCount(), b.getTerminalRowCount(), b.getFilterDesc());
    }
}
