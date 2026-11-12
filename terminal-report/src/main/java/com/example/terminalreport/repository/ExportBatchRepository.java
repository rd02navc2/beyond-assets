package com.example.terminalreport.repository;

import com.example.terminalreport.entity.ExportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExportBatchRepository extends JpaRepository<ExportBatch, Long> {

    /** 前一個批次（編號比指定批次小的最大者）。 */
    Optional<ExportBatch> findFirstByIdLessThanOrderByIdDesc(Long id);

    /** 目前最新的批次（編號最大者）。 */
    Optional<ExportBatch> findFirstByOrderByIdDesc();
}
