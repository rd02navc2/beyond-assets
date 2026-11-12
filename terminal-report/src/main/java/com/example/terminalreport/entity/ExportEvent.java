package com.example.terminalreport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Excel「換機事件明細」工作表的一列。 */
@Entity
@Table(name = "export_event")
public class ExportEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "terminal_id", length = 100)
    private String terminalId;

    @Column(name = "group_key", length = 255)
    private String groupKey;

    @Column(name = "remark", length = 1000)
    private String remark;

    @Column(name = "field_changes", columnDefinition = "text")
    private String fieldChanges;

    @Column(name = "commit_time")
    private LocalDateTime commitTime;

    @Column(name = "operator", length = 100)
    private String operator;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes_json", columnDefinition = "json")
    private List<Map<String, Object>> changes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getFieldChanges() {
        return fieldChanges;
    }

    public void setFieldChanges(String fieldChanges) {
        this.fieldChanges = fieldChanges;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Map<String, Object>> getChanges() {
        return changes;
    }

    public void setChanges(List<Map<String, Object>> changes) {
        this.changes = changes;
    }

    public LocalDateTime getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
    }
}
