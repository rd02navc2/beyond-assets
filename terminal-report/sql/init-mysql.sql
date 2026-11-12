-- 在 Windows 本機 MySQL 8 以 root 執行一次（MySQL Workbench 或 mysql 命令列皆可）
-- 密碼請與 .env 的 DB_PASSWORD 一致

CREATE DATABASE IF NOT EXISTS terminal_report
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- '%' 允許從 Docker 容器連入（容器連線來源不是 localhost）
CREATE USER IF NOT EXISTS 'terminal'@'%' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON terminal_report.* TO 'terminal'@'%';

-- 網頁「SQL 查詢」專用的唯讀帳號：只有 SELECT 權限，即使查詢檢查被繞過也無法修改資料
-- 密碼請與 .env 的 DB_RO_PASSWORD 一致
CREATE USER IF NOT EXISTS 'terminal_ro'@'%' IDENTIFIED BY 'change_me_ro';
GRANT SELECT ON terminal_report.* TO 'terminal_ro'@'%';

FLUSH PRIVILEGES;
