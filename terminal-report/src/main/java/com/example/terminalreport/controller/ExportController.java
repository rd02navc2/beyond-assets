package com.example.terminalreport.controller;

import com.example.terminalreport.dto.ExportBatchView;
import com.example.terminalreport.dto.ExportDiff;
import com.example.terminalreport.dto.ExportRequest;
import com.example.terminalreport.dto.ExportResponse;
import com.example.terminalreport.exception.QueryException;
import com.example.terminalreport.service.ExportDiffService;
import com.example.terminalreport.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;
    private final ExportDiffService diffService;

    public ExportController(ExportService exportService, ExportDiffService diffService) {
        this.exportService = exportService;
        this.diffService = diffService;
    }

    /** 前端匯出 Excel 時呼叫：（可選）先清空舊資料，再存入三張資料表，並回傳與前一次匯出的機台差異。 */
    @PostMapping
    public ResponseEntity<ExportResponse> create(@Valid @RequestBody ExportRequest request) {
        // 差異比對由 ExportService 在（可能的）清空之前完成，結果已包含在回應中
        return ResponseEntity.status(HttpStatus.CREATED).body(exportService.save(request));
    }

    /** 最近的匯出紀錄（預設 20 筆，最多 100 筆）。 */
    @GetMapping
    public List<ExportBatchView> list(@RequestParam(defaultValue = "20") int size) {
        return exportService.listBatches(size);
    }

    /** 指定批次與基準批次（預設為前一批次）的機台差異。 */
    @GetMapping("/{id}/diff")
    public ExportDiff diff(@PathVariable Long id, @RequestParam(required = false) Long base) {
        ExportDiff d = (base == null) ? diffService.diffWithPrevious(id) : diffService.diff(id, base);
        if (d == null) {
            throw new QueryException("沒有可比較的前一個批次");
        }
        return d;
    }
}
