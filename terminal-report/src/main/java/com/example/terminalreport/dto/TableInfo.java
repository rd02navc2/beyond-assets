package com.example.terminalreport.dto;

import java.util.List;

public record TableInfo(String name, long rowCount, List<ColumnInfo> columns) {

    public record ColumnInfo(String name, String type, String comment) {
    }
}
