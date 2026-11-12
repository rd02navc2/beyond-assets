package com.example.terminalreport.repository;

import com.example.terminalreport.entity.ExportEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExportEventRepository extends JpaRepository<ExportEvent, Long> {

    List<ExportEvent> findByBatchIdOrderByIdAsc(Long batchId);
}
