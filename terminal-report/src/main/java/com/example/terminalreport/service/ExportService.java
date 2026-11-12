package com.example.terminalreport.service;

import com.example.terminalreport.dto.ExportBatchView;
import com.example.terminalreport.dto.ExportDiff;
import com.example.terminalreport.dto.ExportRequest;
import com.example.terminalreport.dto.ExportResponse;
import com.example.terminalreport.entity.ExportBatch;
import com.example.terminalreport.entity.ExportEvent;
import com.example.terminalreport.entity.ExportTerminal;
import com.example.terminalreport.repository.ExportBatchRepository;
import com.example.terminalreport.repository.ExportEventRepository;
import com.example.terminalreport.repository.ExportTerminalRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExportBatchRepository batchRepository;
    private final ExportEventRepository eventRepository;
    private final ExportTerminalRepository terminalRepository;

    private final EntityManager entityManager;
    private final boolean clearBeforeSave;

    public ExportService(ExportBatchRepository batchRepository,
                         ExportEventRepository eventRepository,
                         ExportTerminalRepository terminalRepository,
                         EntityManager entityManager,
                         @Value("${app.export.clear-before-save:false}") boolean clearBeforeSave) {
        this.batchRepository = batchRepository;
        this.eventRepository = eventRepository;
        this.terminalRepository = terminalRepository;
        this.entityManager = entityManager;
        this.clearBeforeSave = clearBeforeSave;
        log.info("匯出入庫：寫入前清空舊資料 = {}", clearBeforeSave ? "開啟" : "關閉");
    }

    /**
     * 以單一交易完成：（可選）清空舊資料 → 寫入批次(摘要) + 事件明細 + 機台清單。
     * 任一步失敗整個交易回滾：不會留下半套資料，而且被清空的舊資料也會還原。
     * 注意：清空使用 DELETE 而非 TRUNCATE，因為 TRUNCATE 會立即提交，無法回滾。
     */
    @Transactional
    public ExportResponse save(ExportRequest req) {
        ExportRequest.Summary s = req.summary();

        // 0) 先取得前一批次與其機台清單（清空之前，供差異比對）
        ExportBatch prevBatch = batchRepository.findFirstByOrderByIdDesc().orElse(null);
        List<ExportTerminal> prevTerminals = prevBatch == null
                ? List.of() : new ArrayList<>(terminalRepository.findByBatchId(prevBatch.getId()));

        // 0') 依設定清空舊資料（子表先刪，再刪批次）
        ExportResponse.ClearedInfo cleared = clearBeforeSave ? clearAllRows() : null;

        ExportBatch batch = new ExportBatch();
        batch.setFileName(trunc(req.fileName(), 255));
        batch.setExportedAt(LocalDateTime.now());
        batch.setTotalTerminals(s.totalTerminals());
        batch.setTotalCategories(s.totalCategories());
        batch.setTotalEvents(s.totalEvents());
        batch.setPeriodStart(parseDate(s.periodStart()));
        batch.setPeriodEnd(parseDate(s.periodEnd()));
        batch.setLastSyncTime(parseDateTime(s.lastSyncTime()));
        batch.setReportTimeText(trunc(s.reportTime(), 64));
        batch.setFilterDesc(trunc(s.filterDesc(), 500));
        batch.setEventRowCount(req.events().size());
        batch.setTerminalRowCount(req.terminals().size());
        batchRepository.save(batch); // IDENTITY：此時即取得 batch.getId()

        Long batchId = batch.getId();

        List<ExportEvent> events = new ArrayList<>(req.events().size());
        for (ExportRequest.EventRow r : req.events()) {
            ExportEvent e = new ExportEvent();
            e.setBatchId(batchId);
            e.setEventDate(parseDate(r.eventDate()));
            e.setCategory(trunc(r.category(), 100));
            e.setTerminalId(trunc(r.terminalId(), 100));
            e.setGroupKey(trunc(r.groupKey(), 255));
            e.setRemark(trunc(r.remark(), 1000));
            e.setFieldChanges(r.fieldChanges());
            e.setCommitTime(parseDateTime(r.commitTime()));
            e.setOperator(trunc(r.operator(), 100));
            e.setChanges(r.changes());
            events.add(e);
        }
        eventRepository.saveAll(events);

        List<ExportTerminal> terminals = new ArrayList<>(req.terminals().size());
        for (ExportRequest.TerminalRow r : req.terminals()) {
            ExportTerminal t = new ExportTerminal();
            t.setBatchId(batchId);
            t.setCategory(trunc(r.category(), 100));
            t.setTerminalId(trunc(r.terminalId(), 100));
            t.setGroupKey(trunc(r.groupKey(), 255));
            t.setAssetDescription(trunc(r.assetDescription(), 255));
            t.setCounterNo(trunc(r.counterNo(), 100));
            t.setMachineNo(trunc(r.machineNo(), 100));
            t.setMerchantCode(trunc(r.merchantCode(), 100));
            t.setAssetStatus(trunc(r.assetStatus(), 255));
            t.setWarehouseStock(Boolean.TRUE.equals(r.warehouseStock()));
            t.setChangeCount(r.changeCount() == null ? 0 : r.changeCount());
            t.setLastChangeDate(parseDate(r.lastChangeDate()));
            t.setLastCommitTime(parseDateTime(r.lastCommitTime()));
            t.setExtraFields(cleanExtra(r.extraFields()));
            t.setSourceFile(trunc(r.sourceFile(), 255));
            terminals.add(t);
        }
        terminalRepository.saveAll(terminals);

        log.info("匯出批次 #{} 已寫入：事件 {} 筆、機台 {} 筆（{}）",
                batchId, events.size(), terminals.size(), batch.getFileName());

        // 與前一批次比對（純記憶體運算，失敗不影響入庫）
        ExportDiff diff = null;
        if (prevBatch != null) {
            try {
                diff = ExportDiffCalculator.compute(terminals, prevTerminals)
                        .withBase(prevBatch.getId(), prevBatch.getExportedAt());
            } catch (RuntimeException ex) {
                log.warn("批次 #{} 的差異比對失敗：{}", batchId, ex.getMessage());
            }
        }

        return new ExportResponse(batchId, batch.getExportedAt(), events.size(), terminals.size(), diff, cleared);
    }

    /** 刪除三張匯出資料表的全部資料（僅限本系統的三張表）。需在交易內呼叫，才能在失敗時回滾。 */
    private ExportResponse.ClearedInfo clearAllRows() {
        long batches = batchRepository.count();
        long events = eventRepository.count();
        long terminals = terminalRepository.count();
        eventRepository.deleteAllInBatch();       // 子表先刪（外鍵）
        terminalRepository.deleteAllInBatch();
        batchRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();                    // 清掉持久化快取，避免殘留已刪除的物件
        if (batches + events + terminals > 0) {
            log.warn("匯出前已清空舊資料：批次 {}、事件 {} 筆、機台 {} 筆", batches, events, terminals);
        }
        return new ExportResponse.ClearedInfo(batches, events, terminals);
    }

    @Transactional(readOnly = true)
    public List<ExportBatchView> listBatches(int size) {
        int limit = Math.max(1, Math.min(size, 100));
        return batchRepository
                .findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "exportedAt")))
                .map(ExportBatchView::from)
                .getContent();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /** 動態欄位：移除 null key，避免序列化成 JSON 時出錯。 */
    private static Map<String, Object> cleanExtra(Map<String, Object> src) {
        if (src == null || src.isEmpty()) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        src.forEach((k, v) -> {
            if (k != null) {
                out.put(k, v);
            }
        });
        return out.isEmpty() ? null : out;
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** 接受 yyyy-MM-dd（含更長的 ISO 字串，只取前 10 碼）；解析失敗回傳 null。 */
    static LocalDate parseDate(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(t.substring(0, 10));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** 接受 yyyy-MM-dd HH:mm:ss 或 ISO-8601（yyyy-MM-ddTHH:mm:ss）；解析失敗回傳 null。 */
    static LocalDateTime parseDateTime(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(t, DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(t.replace(' ', 'T'));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
