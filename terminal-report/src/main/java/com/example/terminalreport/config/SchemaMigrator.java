package com.example.terminalreport.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 啟動時檢查既有資料表是否缺少「網頁還原」所需的欄位，缺少就補上（可重複執行）。
 * MySQL 的 ADD COLUMN 不支援 IF NOT EXISTS，因此先查 information_schema。
 * 新安裝的資料庫由 schema.sql 直接建立完整欄位，這裡不會有任何動作。
 */
@Component
public class SchemaMigrator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private final JdbcTemplate jdbc;

    public SchemaMigrator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterPropertiesSet() {
        addColumnIfMissing("export_terminal", "source_file", "VARCHAR(255) NULL COMMENT '來源檔案（網頁還原用）'");
        addColumnIfMissing("export_event", "operator", "VARCHAR(100) NULL COMMENT '操作人員（網頁還原用）'");
        addColumnIfMissing("export_event", "changes_json", "JSON NULL COMMENT '欄位異動（結構化，網頁還原用）'");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, table, column);
        if (n == null || n == 0) {
            jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            log.info("資料表遷移：{} 新增欄位 {}", table, column);
        }
    }
}
