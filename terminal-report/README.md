# 換機歷史報表（Spring Boot 3 + JPA + MySQL 8）

按下「匯出 Excel 報表」→ 瀏覽器照舊產生 Excel，**同時**把 Excel 三個工作表的每個欄位存入 MySQL。

## 快速啟動

1. 把前端函式庫放到 `src/main/resources/static/js/`：
   `vue.global.prod.js`、`chart.umd.min.js`、`xlsx.full.min.js`（index.html 以 `/js/...` 引用）
2. 準備 MySQL 8（帳號需有 CREATE DATABASE 權限，首次啟動會自動建庫、建表）
3. 設定連線並啟動（JDK 21）：

```bash
export DB_USER=root
export DB_PASSWORD=你的密碼
mvn spring-boot:run
```

4. 開啟 http://localhost:8080/ → 匯出 Excel → 畫面上方出現綠色提示「批次 #N」即成功。

## 架構

```
index.html (Vue + SheetJS)            ← View（static/）
   │  POST /api/exports (JSON)
ExportController                       ← Controller：驗證、回應 201
   │
ExportService  @Transactional          ← Service：解析日期、截斷字串、單一交易寫入
   │
Export{Batch,Event,Terminal}Repository ← Spring Data JPA
   │
MySQL: export_batch / export_event / export_terminal
```

## 資料表（每次匯出＝一個批次快照）

| Excel 工作表 | 資料表 | 說明 |
|---|---|---|
| 摘要 | `export_batch` | 8 個摘要項目攤成一列＋檔名、匯出時間、筆數 |
| 換機事件明細 | `export_event` | 日期、分類、機號、分類_機號、備註、欄位異動、對應Commit時間 |
| 機台清單 | `export_terminal` | 固定欄位各一欄；動態欄位（如 序號S/N）存 `extra_fields` JSON |

DDL 見 `src/main/resources/schema.sql`（`CREATE TABLE IF NOT EXISTS`，每次啟動執行不會重複建立）。

## API

- `POST /api/exports` → `201 {"batchId":12,"exportedAt":"...","eventCount":129,"terminalCount":648}`
- `GET  /api/exports?size=20` → 最近的匯出紀錄

## 匯出前清空舊資料（`EXPORT_CLEAR_BEFORE_SAVE`，預設 `true`）

每次「匯出 Excel 報表」入庫前，先把 **`export_batch`、`export_event`、`export_terminal` 三張表**的全部資料刪除，
再寫入本次資料——資料庫永遠只保留「最近一次匯出」。

- **只清這三張表**，不會動資料庫裡其他任何資料表。
- **使用 `DELETE`，不是 `TRUNCATE`**：`TRUNCATE` 在 MySQL 會立即提交、無法回滾。這裡「清空＋寫入」在**同一個交易**內，
  寫入失敗時整個回滾，舊資料會還原。
- **差異比對仍有效**：清空前先取得上一次匯出的機台清單，用來產生「新增／消失／異動」提示（見下節）。
- 批次編號（`id`）會持續遞增，不會從 1 重新開始（`DELETE` 不重置 AUTO_INCREMENT）。
- 清空後資料庫沒有歷史，因此 `GET /api/exports/{id}/diff` 與 SQL 範例「新增的機台（對比前一批）」不會有結果。
- 要保留每次匯出的歷史批次：設 `EXPORT_CLEAR_BEFORE_SAVE=false`（`.env` 或環境變數）後重啟。

## 匯出差異偵測（新增／消失／異動／重複機台）

每次「匯出 Excel 報表」入庫後，後端會自動和**前一個批次**比對機台清單（以「分類_機號」為識別），
並在回應中帶回差異，網頁會顯示提示條與差異明細：

| 類型 | 意義 |
|---|---|
| 新增機台 | 前一次沒有、這次才出現（例如重新匯入資料後多了一台） |
| 消失機台 | 前一次有、這次沒有 |
| 欄位異動 | 兩次都有，但櫃號、機號、商戶代碼、狀態／櫃位、倉庫庫存、換機次數、序號 S/N 等不同 |
| 重複機台 | **同一批次內**同一台出現多列——會讓機台筆數變多，但不是新機台 |

- 第一次匯出（沒有前一批次）不會有差異資訊。
- 比對失敗不影響入庫結果（只記錄 log）。
- 事後比對任兩個批次：`GET /api/exports/{id}/diff?base={另一批次}`（省略 base 則與前一批次比）。
- SQL 查詢分頁內建範例「新增的機台（對比前一批）」，可直接用 SQL 查看。

## 機號格式檢查（抓「打錯機號」造成多出來的機台）

「分類修正清單」原本只檢查**分類是否被改錯**，不看機號本身。若有人把機號打錯（例如 `AP2336150040` 打成
`AP22336150040`），會多出一個新檔案、多算一台機台，舊檢查抓不到。新增的檢查：

- **判定**：同一分類的機號長度應一致；若 ≥90% 為同一長度，長度不同者視為異常（多／少打字元、含空白）。
  樣本少於 5 台、或長度本來就混雜（<90%）的分類不判斷，避免誤報。
- **疑似應為**：刪掉一個字元後，列出符合該分類格式的候選，並標示「已存在（可能是重複建檔）」或「目前沒有此機號（缺號）」。
- **匯入時**：每次匯入／套用新資料，會與匯入前比較，提示新增、消失的機台，以及**新出現**的機號異常（舊資料就有的不重複提示）。
- **隨時查看**：「分類修正清單」分頁下方有完整清單，分頁徽章顯示待處理總數；可一鍵複製檔案路徑。
- SQL 查詢分頁有範例「機號長度分布（找異常）」，可在資料庫端驗證。

## 網頁 SQL 查詢

網頁上方分頁「SQL 查詢」：左側列出資料表與欄位（點資料表名產生 SELECT、點欄位名插入游標處），
右側輸入 SQL 後按「執行查詢」或 Ctrl+Enter，結果顯示在下方表格，並可匯出 CSV。

- `POST /api/query`　`{"sql":"SELECT ..."}` → `{columns, rows, rowCount, truncated, maxRows, elapsedMs}`
- `GET  /api/query/schema` → 資料表、欄位、筆數

安全機制（由外到內）：

1. **語法檢查**：只允許單一 `SELECT` / `WITH`；不允許分號、註解；擋掉 `INTO`、`SLEEP`、`LOAD_FILE`、系統資料庫等。
2. **唯讀連線**：獨立連線池，MySQL 工作階段為 READ ONLY。
3. **資源限制**：最多回傳 1000 筆、逾時 10 秒（`app.query.*` 可調整）。
4. **資料庫權限（最重要）**：設定 `DB_RO_USER` / `DB_RO_PASSWORD` 為只有 SELECT 權限的帳號。

> 語法檢查是字串比對，無法保證絕對安全；請務必使用唯讀帳號，且不要把這個服務暴露到不受信任的網路。

## 常用查詢

```sql
-- 最近的匯出批次
SELECT id, file_name, exported_at, event_row_count, terminal_row_count FROM export_batch ORDER BY id DESC LIMIT 10;

-- 某批次中，帶有序號 S/N 的機台
SELECT terminal_id, extra_fields->>'$."序號S/N"' AS sn
FROM export_terminal WHERE batch_id = 1 AND extra_fields->>'$."序號S/N"' IS NOT NULL;

-- 某台機器在各次匯出中的狀態變化
SELECT b.exported_at, t.counter_no, t.machine_no, t.asset_status
FROM export_terminal t JOIN export_batch b ON b.id = t.batch_id
WHERE t.group_key = 'POS機_AP2336150014' ORDER BY b.exported_at;
```

## Docker 部署（http://192.168.5.46:8089，連線 Windows 本機 MySQL）

只啟動應用程式容器；資料庫使用 Windows 上已安裝的 MySQL 8。

```bash
# 1) 在 Windows MySQL 執行一次 sql/init-mysql.sql（建立資料庫與專用帳號）
# 2) 把 vue.global.prod.js / chart.umd.min.js / xlsx.full.min.js 放進 src/main/resources/static/js/
# 3) 設定連線
cp .env.example .env && vi .env
# 4) 建置並啟動
docker compose up -d --build
docker compose logs -f app
```

容器透過 `host.docker.internal` 連到 Windows 主機的 MySQL（3306）。
若 Docker 與 MySQL 不在同一台機器，把 `.env` 的 `DB_HOST` 改成 MySQL 主機 IP。

```bash
docker compose ps                 # 狀態
docker compose restart app        # 重啟
docker compose down               # 停止（資料在 MySQL，不受影響）
docker compose up -d --build      # 改程式後重新部署
```
