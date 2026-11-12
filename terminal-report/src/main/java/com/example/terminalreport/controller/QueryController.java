package com.example.terminalreport.controller;

import com.example.terminalreport.dto.QueryRequest;
import com.example.terminalreport.dto.QueryResponse;
import com.example.terminalreport.dto.TableInfo;
import com.example.terminalreport.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    /** 執行唯讀 SELECT 查詢。 */
    @PostMapping
    public QueryResponse run(@Valid @RequestBody QueryRequest request) {
        return queryService.execute(request.sql());
    }

    /** 可查詢的資料表、欄位與筆數。 */
    @GetMapping("/schema")
    public List<TableInfo> schema() {
        return queryService.schema();
    }
}
