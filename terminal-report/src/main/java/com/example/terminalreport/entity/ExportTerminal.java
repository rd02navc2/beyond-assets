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
import java.util.Map;

/** Excel「機台清單」工作表的一列。固定欄位各自一欄，動態欄位（如 序號S/N）放在 JSON 欄位。 */
@Entity
@Table(name = "export_terminal")
public class ExportTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "terminal_id", length = 100)
    private String terminalId;

    @Column(name = "group_key", length = 255)
    private String groupKey;

    @Column(name = "asset_description", length = 255)
    private String assetDescription;

    @Column(name = "counter_no", length = 100)
    private String counterNo;

    @Column(name = "machine_no", length = 100)
    private String machineNo;

    @Column(name = "merchant_code", length = 100)
    private String merchantCode;

    @Column(name = "asset_status", length = 255)
    private String assetStatus;

    @Column(name = "warehouse_stock", nullable = false)
    private boolean warehouseStock;

    @Column(name = "change_count", nullable = false)
    private int changeCount;

    @Column(name = "last_change_date")
    private LocalDate lastChangeDate;

    @Column(name = "last_commit_time")
    private LocalDateTime lastCommitTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_fields", columnDefinition = "json")
    private Map<String, Object> extraFields;

    @Column(name = "source_file", length = 255)
    private String sourceFile;

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

    public String getAssetDescription() {
        return assetDescription;
    }

    public void setAssetDescription(String assetDescription) {
        this.assetDescription = assetDescription;
    }

    public String getCounterNo() {
        return counterNo;
    }

    public void setCounterNo(String counterNo) {
        this.counterNo = counterNo;
    }

    public String getMachineNo() {
        return machineNo;
    }

    public void setMachineNo(String machineNo) {
        this.machineNo = machineNo;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public void setMerchantCode(String merchantCode) {
        this.merchantCode = merchantCode;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public void setAssetStatus(String assetStatus) {
        this.assetStatus = assetStatus;
    }

    public boolean isWarehouseStock() {
        return warehouseStock;
    }

    public void setWarehouseStock(boolean warehouseStock) {
        this.warehouseStock = warehouseStock;
    }

    public int getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(int changeCount) {
        this.changeCount = changeCount;
    }

    public LocalDate getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(LocalDate lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }

    public LocalDateTime getLastCommitTime() {
        return lastCommitTime;
    }

    public void setLastCommitTime(LocalDateTime lastCommitTime) {
        this.lastCommitTime = lastCommitTime;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields;
    }
}
