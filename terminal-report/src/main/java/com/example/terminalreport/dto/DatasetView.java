package com.example.terminalreport.dto;

import java.util.List;
import java.util.Map;

/**
 * 由資料庫還原的「儀表板資料集」。欄位名稱與網頁內建資料集（EMBEDDED_DATASET）完全一致，
 * 網頁可以直接套用；meta 為資料來源資訊（批次、匯出時間等），不屬於資料集本身。
 */
public record DatasetView(Meta meta, Summary summary, List<String> categories,
                          List<TerminalView> terminals, List<EventView> events) {

    public record Meta(Long batchId, String fileName, String exportedAt, String filterDesc,
                       int terminalCount, int eventCount, Integer datasetTotalEvents) {
    }

    public record Summary(int totalTerminals, int totalCategories, int totalEvents,
                          List<String> dateRange, String lastCommitDate) {
    }

    public record TerminalView(String terminalId, String category, String groupKey, String assetDescription,
                               String counterNo, String machineNo, String merchantCode, String assetStatus,
                               boolean warehouseStock, String sourceFile, String lastCommitDate,
                               int changeCount, String lastChangeDate, Map<String, Object> extraFields) {
    }

    public record EventView(Long id, String groupKey, String category, String terminalId, String date,
                            String remark, String operator, List<Map<String, Object>> changes, String commitDate) {
    }
}
