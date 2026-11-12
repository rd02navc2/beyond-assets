package com.example.terminalreport.service;

import com.example.terminalreport.dto.DatasetView;
import com.example.terminalreport.entity.ExportBatch;
import com.example.terminalreport.entity.ExportEvent;
import com.example.terminalreport.entity.ExportTerminal;
import com.example.terminalreport.repository.ExportBatchRepository;
import com.example.terminalreport.repository.ExportEventRepository;
import com.example.terminalreport.repository.ExportTerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 把資料庫中「最新批次」的資料表欄位還原成網頁儀表板使用的資料集。 */
@Service
public class DatasetService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExportBatchRepository batchRepository;
    private final ExportEventRepository eventRepository;
    private final ExportTerminalRepository terminalRepository;

    public DatasetService(ExportBatchRepository batchRepository,
                          ExportEventRepository eventRepository,
                          ExportTerminalRepository terminalRepository) {
        this.batchRepository = batchRepository;
        this.eventRepository = eventRepository;
        this.terminalRepository = terminalRepository;
    }

    /** 資料庫沒有任何匯出批次時回傳 null。 */
    @Transactional(readOnly = true)
    public DatasetView latest() {
        ExportBatch b = batchRepository.findFirstByOrderByIdDesc().orElse(null);
        if (b == null) {
            return null;
        }
        List<ExportTerminal> ts = terminalRepository.findByBatchIdOrderByIdAsc(b.getId());
        List<ExportEvent> es = eventRepository.findByBatchIdOrderByIdAsc(b.getId());

        List<DatasetView.TerminalView> terminals = new ArrayList<>(ts.size());
        Set<String> categories = new LinkedHashSet<>();          // 依第一次出現的順序
        for (ExportTerminal t : ts) {
            if (t.getCategory() != null) {
                categories.add(t.getCategory());
            }
            terminals.add(new DatasetView.TerminalView(
                    t.getTerminalId(), t.getCategory(), t.getGroupKey(), t.getAssetDescription(),
                    t.getCounterNo(), t.getMachineNo(), t.getMerchantCode(), t.getAssetStatus(),
                    t.isWarehouseStock(), t.getSourceFile(), fmt(t.getLastCommitTime()),
                    t.getChangeCount(), dateStr(t.getLastChangeDate()), t.getExtraFields()));
        }

        List<DatasetView.EventView> events = new ArrayList<>(es.size());
        for (ExportEvent e : es) {
            events.add(new DatasetView.EventView(
                    e.getId(), e.getGroupKey(), e.getCategory(), e.getTerminalId(),
                    nz(dateStr(e.getEventDate())), nz(e.getRemark()), nz(e.getOperator()),
                    e.getChanges() == null ? List.of() : e.getChanges(), nz(fmt(e.getCommitTime()))));
        }

        // totalEvents 以「實際還原的事件數」為準，讓畫面上的數字彼此一致；
        // 匯出當時資料集的事件總數另放在 meta，供網頁判斷事件是否被篩選過。
        DatasetView.Summary summary = new DatasetView.Summary(
                terminals.size(), categories.size(), events.size(),
                List.of(nz(dateStr(b.getPeriodStart())), nz(dateStr(b.getPeriodEnd()))),
                fmt(b.getLastSyncTime()));
        DatasetView.Meta meta = new DatasetView.Meta(
                b.getId(), b.getFileName(), fmt(b.getExportedAt()), b.getFilterDesc(),
                terminals.size(), events.size(), b.getTotalEvents());

        return new DatasetView(meta, summary, new ArrayList<>(categories), terminals, events);
    }

    private static String fmt(LocalDateTime t) {
        return t == null ? null : t.format(DT);
    }

    private static String dateStr(LocalDate d) {
        return d == null ? null : d.toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
