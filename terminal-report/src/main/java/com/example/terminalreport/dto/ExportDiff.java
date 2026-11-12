package com.example.terminalreport.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 本次匯出與前一次（基準）匯出的機台差異。 */
public record ExportDiff(
        Long baseBatchId,
        LocalDateTime baseExportedAt,
        List<TerminalBrief> added,        // 本次有、前次沒有（新增機台）
        List<TerminalBrief> removed,      // 前次有、本次沒有（消失機台）
        List<TerminalChange> changed,     // 兩次都有，但欄位內容不同
        List<String> duplicateKeys) {     // 本次批次內重複出現的「分類_機號」

    public record TerminalBrief(
            String category, String terminalId, String groupKey, String assetDescription,
            String counterNo, String machineNo, String assetStatus, boolean warehouseStock) {
    }

    public record TerminalChange(String groupKey, String category, String terminalId, List<FieldChange> fields) {
    }

    public record FieldChange(String field, String oldValue, String newValue) {
    }

    public ExportDiff withBase(Long id, LocalDateTime at) {
        return new ExportDiff(id, at, added, removed, changed, duplicateKeys);
    }
}
