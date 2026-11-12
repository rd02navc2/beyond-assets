package com.example.terminalreport.service;

import com.example.terminalreport.dto.ExportDiff;
import com.example.terminalreport.entity.ExportBatch;
import com.example.terminalreport.exception.QueryException;
import com.example.terminalreport.repository.ExportBatchRepository;
import com.example.terminalreport.repository.ExportTerminalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ExportDiffService {

    private final ExportBatchRepository batchRepository;
    private final ExportTerminalRepository terminalRepository;

    public ExportDiffService(ExportBatchRepository batchRepository, ExportTerminalRepository terminalRepository) {
        this.batchRepository = batchRepository;
        this.terminalRepository = terminalRepository;
    }

    /** 與前一個批次比對；沒有前一批次（第一次匯出）時回傳 null。 */
    public ExportDiff diffWithPrevious(Long batchId) {
        Optional<ExportBatch> prev = batchRepository.findFirstByIdLessThanOrderByIdDesc(batchId);
        return prev.map(b -> diff(batchId, b.getId())).orElse(null);
    }

    @Transactional(readOnly = true)
    public ExportDiff diff(Long batchId, Long baseBatchId) {
        batchRepository.findById(batchId).orElseThrow(() -> new QueryException("找不到批次 #" + batchId));
        ExportBatch base = batchRepository.findById(baseBatchId)
                .orElseThrow(() -> new QueryException("找不到批次 #" + baseBatchId));
        ExportDiff d = ExportDiffCalculator.compute(
                terminalRepository.findByBatchId(batchId),
                terminalRepository.findByBatchId(baseBatchId));
        return d.withBase(base.getId(), base.getExportedAt());
    }
}
