package com.example.terminalreport.repository;

import com.example.terminalreport.entity.ExportTerminal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExportTerminalRepository extends JpaRepository<ExportTerminal, Long> {

    List<ExportTerminal> findByBatchId(Long batchId);

    List<ExportTerminal> findByBatchIdOrderByIdAsc(Long batchId);
}
