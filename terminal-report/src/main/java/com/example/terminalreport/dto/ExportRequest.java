package com.example.terminalreport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 前端「匯出 Excel 報表」時 POST 的內容；三個巢狀 record 分別對應 Excel 的三個工作表。
 * 日期／時間一律以字串傳入（yyyy-MM-dd / yyyy-MM-dd HH:mm:ss），由 Service 容錯解析。
 */
public record ExportRequest(
        @NotBlank @Size(max = 255) String fileName,
        @NotNull @Valid Summary summary,
        @NotNull @Size(max = 50000) List<@NotNull @Valid EventRow> events,
        @NotNull @Size(max = 50000) List<@NotNull @Valid TerminalRow> terminals) {

    /** 工作表「摘要」 */
    public record Summary(
            Integer totalTerminals,
            Integer totalCategories,
            Integer totalEvents,
            String periodStart,
            String periodEnd,
            String lastSyncTime,
            String reportTime,
            String filterDesc) {
    }

    /** 工作表「換機事件明細」的一列 */
    public record EventRow(
            String eventDate,
            String category,
            String terminalId,
            String groupKey,
            String remark,
            String fieldChanges,
            String commitTime,
            String operator,
            List<Map<String, Object>> changes) {
    }

    /** 工作表「機台清單」的一列 */
    public record TerminalRow(
            String category,
            String terminalId,
            String groupKey,
            String assetDescription,
            String counterNo,
            String machineNo,
            String merchantCode,
            String assetStatus,
            Boolean warehouseStock,
            Integer changeCount,
            String lastChangeDate,
            String lastCommitTime,
            Map<String, Object> extraFields,
            String sourceFile) {
    }
}
