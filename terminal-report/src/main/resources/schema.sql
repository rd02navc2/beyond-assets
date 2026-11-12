-- =====================================================================
-- 換機歷史報表：匯出紀錄表（MySQL 8 / InnoDB / utf8mb4）
--
-- 設計：每按一次「匯出 Excel 報表」＝ 一個批次 (export_batch)
--   export_batch    ← Excel 工作表「摘要」(8 個項目攤成一列) + 匯出中繼資料
--   export_event    ← Excel 工作表「換機事件明細」(每列一筆)
--   export_terminal ← Excel 工作表「機台清單」(每列一筆，動態欄位放 JSON)
-- 三張表都是「快照」：保留每次匯出當下的資料，可用 batch_id 追溯與比對。
-- =====================================================================

CREATE TABLE IF NOT EXISTS export_batch (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '批次編號',
    file_name           VARCHAR(255) NOT NULL                COMMENT '匯出的 Excel 檔名',
    exported_at         DATETIME(3)  NOT NULL                COMMENT '匯出時間（伺服器時間）',
    total_terminals     INT          NULL                    COMMENT '摘要：機台總數',
    total_categories    INT          NULL                    COMMENT '摘要：設備分類數',
    total_events        INT          NULL                    COMMENT '摘要：累計換機/異動事件數',
    period_start        DATE         NULL                    COMMENT '摘要：資料期間(起)',
    period_end          DATE         NULL                    COMMENT '摘要：資料期間(迄)',
    last_sync_time      DATETIME     NULL                    COMMENT '摘要：最後同步時間',
    report_time_text    VARCHAR(64)  NULL                    COMMENT '摘要：報表產生時間（前端顯示字串）',
    filter_desc         VARCHAR(500) NULL                    COMMENT '摘要：套用篩選',
    event_row_count     INT          NOT NULL DEFAULT 0      COMMENT '本批次事件明細筆數',
    terminal_row_count  INT          NOT NULL DEFAULT 0      COMMENT '本批次機台清單筆數',
    PRIMARY KEY (id),
    KEY idx_batch_exported_at (exported_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel 匯出批次（含摘要）';

CREATE TABLE IF NOT EXISTS export_event (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    batch_id        BIGINT        NOT NULL                COMMENT '所屬匯出批次',
    event_date      DATE          NULL                    COMMENT 'Excel 欄位：日期',
    category        VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：分類',
    terminal_id     VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：機號',
    group_key       VARCHAR(255)  NULL                    COMMENT 'Excel 欄位：分類_機號',
    remark          VARCHAR(1000) NULL                    COMMENT 'Excel 欄位：備註',
    field_changes   TEXT          NULL                    COMMENT 'Excel 欄位：欄位異動（原文字）',
    commit_time     DATETIME      NULL                    COMMENT 'Excel 欄位：對應Commit時間',
    operator        VARCHAR(100)  NULL                    COMMENT '操作人員（網頁還原用）',
    changes_json    JSON          NULL                    COMMENT '欄位異動（結構化，網頁還原用）',
    PRIMARY KEY (id),
    KEY idx_event_batch (batch_id),
    KEY idx_event_terminal (terminal_id),
    KEY idx_event_date (event_date),
    CONSTRAINT fk_event_batch FOREIGN KEY (batch_id) REFERENCES export_batch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel「換機事件明細」快照';

CREATE TABLE IF NOT EXISTS export_terminal (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    batch_id            BIGINT        NOT NULL                COMMENT '所屬匯出批次',
    category            VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：分類',
    terminal_id         VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：機號',
    group_key           VARCHAR(255)  NULL                    COMMENT 'Excel 欄位：分類_機號',
    asset_description   VARCHAR(255)  NULL                    COMMENT 'Excel 欄位：型號描述',
    counter_no          VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：目前櫃號',
    machine_no          VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：目前機號',
    merchant_code       VARCHAR(100)  NULL                    COMMENT 'Excel 欄位：商戶代碼',
    asset_status        VARCHAR(255)  NULL                    COMMENT 'Excel 欄位：目前狀態/櫃位',
    warehouse_stock     TINYINT(1)    NOT NULL DEFAULT 0      COMMENT 'Excel 欄位：倉庫庫存（是=1/否=0）',
    change_count        INT           NOT NULL DEFAULT 0      COMMENT 'Excel 欄位：累計換機次數',
    last_change_date    DATE          NULL                    COMMENT 'Excel 欄位：最近換機日期',
    last_commit_time    DATETIME      NULL                    COMMENT 'Excel 欄位：最後同步時間',
    extra_fields        JSON          NULL                    COMMENT 'Excel 動態欄位（例如 序號S/N）',
    source_file         VARCHAR(255)  NULL                    COMMENT '來源檔案（網頁還原用）',
    PRIMARY KEY (id),
    KEY idx_terminal_batch (batch_id),
    KEY idx_terminal_tid (terminal_id),
    KEY idx_terminal_category (category),
    CONSTRAINT fk_terminal_batch FOREIGN KEY (batch_id) REFERENCES export_batch (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Excel「機台清單」快照';
