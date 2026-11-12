package com.example.terminalreport.controller;

import com.example.terminalreport.dto.DatasetView;
import com.example.terminalreport.service.DatasetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    /** 資料庫最新批次還原成儀表板資料集；資料庫還沒有任何匯出資料時回 204。 */
    @GetMapping("/latest")
    public ResponseEntity<DatasetView> latest() {
        DatasetView view = datasetService.latest();
        return view == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(view);
    }
}
