package com.example.terminalreport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(@NotBlank @Size(max = 20000) String sql) {
}
