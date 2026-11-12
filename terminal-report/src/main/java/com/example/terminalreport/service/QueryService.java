package com.example.terminalreport.service;

import com.example.terminalreport.dto.QueryResponse;
import com.example.terminalreport.dto.TableInfo;
import com.example.terminalreport.exception.QueryException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 網頁 SQL 查詢。使用「獨立的唯讀連線池」（不是 JPA 的 DataSource，也不註冊成 Spring Bean，
 * 以免干擾 JPA 自動設定）。若設定了 DB_RO_USER / DB_RO_PASSWORD 就以該帳號連線，
 * 否則退回使用主要帳號（仍是唯讀交易）。
 */
@Service
public class QueryService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 可在網頁上瀏覽的資料表（白名單）。 */
    private static final List<String> TABLES = List.of("export_batch", "export_event", "export_terminal");

    private final HikariDataSource pool;
    private final int maxRows;
    private final int timeoutSeconds;
    private final int maxSqlLength;

    public QueryService(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String mainUser,
            @Value("${spring.datasource.password}") String mainPassword,
            @Value("${app.query.username:}") String roUser,
            @Value("${app.query.password:}") String roPassword,
            @Value("${app.query.max-rows:1000}") int maxRows,
            @Value("${app.query.timeout-seconds:10}") int timeoutSeconds,
            @Value("${app.query.max-sql-length:5000}") int maxSqlLength) {
        this.maxRows = maxRows;
        this.timeoutSeconds = timeoutSeconds;
        this.maxSqlLength = maxSqlLength;

        boolean useRo = roUser != null && !roUser.isBlank();

        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("query-readonly");
        // 唯讀帳號沒有 CREATE 權限，因此移除 createDatabaseIfNotExist
        cfg.setJdbcUrl(url.replace("createDatabaseIfNotExist=true&", "")
                .replace("&createDatabaseIfNotExist=true", ""));
        cfg.setUsername(useRo ? roUser : mainUser);
        cfg.setPassword(useRo ? roPassword : mainPassword);
        cfg.setReadOnly(true);                    // Connector/J 會對 MySQL 送出 SET SESSION TRANSACTION READ ONLY
        cfg.setAutoCommit(true);
        cfg.setMaximumPoolSize(2);
        cfg.setMinimumIdle(0);
        cfg.setInitializationFailTimeout(-1);     // MySQL 暫時連不上時，不要讓整個應用程式啟動失敗
        cfg.setConnectionInitSql("SET SESSION max_execution_time=" + (timeoutSeconds * 1000L));
        this.pool = new HikariDataSource(cfg);

        log.info("SQL 查詢功能已啟用（連線帳號：{}，筆數上限 {}，逾時 {} 秒）",
                cfg.getUsername(), maxRows, timeoutSeconds);
    }

    public QueryResponse execute(String rawSql) {
        String sql = SqlGuard.validate(rawSql, maxSqlLength);
        log.info("SQL 查詢：{}", sql.length() > 300 ? sql.substring(0, 300) + "..." : sql);

        long start = System.nanoTime();
        try (Connection conn = pool.getConnection(); Statement st = conn.createStatement()) {
            st.setQueryTimeout(timeoutSeconds);
            st.setMaxRows(maxRows + 1); // 多取一筆，用來判斷是否被截斷
            if (!st.execute(sql)) {
                throw new QueryException("此查詢沒有回傳結果集");
            }
            try (ResultSet rs = st.getResultSet()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                List<String> columns = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) {
                    columns.add(md.getColumnLabel(i));
                }
                List<List<Object>> rows = new ArrayList<>();
                boolean truncated = false;
                while (rs.next()) {
                    if (rows.size() >= maxRows) {
                        truncated = true;
                        break;
                    }
                    List<Object> row = new ArrayList<>(n);
                    for (int i = 1; i <= n; i++) {
                        row.add(normalize(rs.getObject(i)));
                    }
                    rows.add(row);
                }
                long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                return new QueryResponse(columns, rows, rows.size(), truncated, maxRows, elapsedMs);
            }
        } catch (SQLTimeoutException e) {
            throw new QueryException("查詢逾時（超過 " + timeoutSeconds + " 秒），請縮小範圍或加上條件");
        } catch (SQLException e) {
            throw new QueryException("SQL 錯誤：" + e.getMessage());
        }
    }

    /** 回傳白名單資料表的欄位（含型別與備註）與目前筆數，供網頁側邊欄使用。 */
    public List<TableInfo> schema() {
        String placeholders = String.join(",", Collections.nCopies(TABLES.size(), "?"));
        String sql = "SELECT table_name AS tname, column_name AS cname, column_type AS ctype, "
                + "column_comment AS ccomment FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name IN (" + placeholders + ") "
                + "ORDER BY table_name, ordinal_position";

        Map<String, List<TableInfo.ColumnInfo>> columns = new LinkedHashMap<>();
        TABLES.forEach(t -> columns.put(t, new ArrayList<>()));

        try (Connection conn = pool.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < TABLES.size(); i++) {
                    ps.setString(i + 1, TABLES.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        List<TableInfo.ColumnInfo> list = columns.get(rs.getString("tname"));
                        if (list != null) {
                            list.add(new TableInfo.ColumnInfo(
                                    rs.getString("cname"), rs.getString("ctype"), rs.getString("ccomment")));
                        }
                    }
                }
            }
            List<TableInfo> result = new ArrayList<>();
            try (Statement st = conn.createStatement()) {
                for (String table : TABLES) {
                    long count = 0;
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `" + table + "`")) {
                        if (rs.next()) {
                            count = rs.getLong(1);
                        }
                    }
                    result.add(new TableInfo(table, count, columns.get(table)));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new QueryException("讀取資料表資訊失敗：" + e.getMessage());
        }
    }

    /** 轉成 JSON 友善的值：數字/布林維持原型，日期時間轉字串，二進位以說明文字代替。 */
    private static Object normalize(Object v) {
        if (v == null || v instanceof Number || v instanceof Boolean || v instanceof String) {
            return v;
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(DT);
        }
        if (v instanceof LocalDateTime ldt) {
            return ldt.format(DT);
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (v instanceof byte[] b) {
            return "(binary " + b.length + " bytes)";
        }
        return v.toString();
    }

    @Override
    public void destroy() {
        pool.close();
    }
}
