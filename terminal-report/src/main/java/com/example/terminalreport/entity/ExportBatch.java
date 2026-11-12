package com.example.terminalreport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 一次「匯出 Excel 報表」＝ 一個批次；同時存放 Excel「摘要」工作表的內容。 */
@Entity
@Table(name = "export_batch")
public class ExportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "exported_at", nullable = false)
    private LocalDateTime exportedAt;

    // ---- 摘要工作表 ----
    @Column(name = "total_terminals")
    private Integer totalTerminals;

    @Column(name = "total_categories")
    private Integer totalCategories;

    @Column(name = "total_events")
    private Integer totalEvents;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "report_time_text", length = 64)
    private String reportTimeText;

    @Column(name = "filter_desc", length = 500)
    private String filterDesc;

    @Column(name = "event_row_count", nullable = false)
    private int eventRowCount;

    @Column(name = "terminal_row_count", nullable = false)
    private int terminalRowCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }

    public Integer getTotalTerminals() {
        return totalTerminals;
    }

    public void setTotalTerminals(Integer totalTerminals) {
        this.totalTerminals = totalTerminals;
    }

    public Integer getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(Integer totalCategories) {
        this.totalCategories = totalCategories;
    }

    public Integer getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(Integer totalEvents) {
        this.totalEvents = totalEvents;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(LocalDateTime lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public String getReportTimeText() {
        return reportTimeText;
    }

    public void setReportTimeText(String reportTimeText) {
        this.reportTimeText = reportTimeText;
    }

    public String getFilterDesc() {
        return filterDesc;
    }

    public void setFilterDesc(String filterDesc) {
        this.filterDesc = filterDesc;
    }

    public int getEventRowCount() {
        return eventRowCount;
    }

    public void setEventRowCount(int eventRowCount) {
        this.eventRowCount = eventRowCount;
    }

    public int getTerminalRowCount() {
        return terminalRowCount;
    }

    public void setTerminalRowCount(int terminalRowCount) {
        this.terminalRowCount = terminalRowCount;
    }
}
