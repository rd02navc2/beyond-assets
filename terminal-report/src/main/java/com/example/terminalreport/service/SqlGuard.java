package com.example.terminalreport.service;

import com.example.terminalreport.exception.QueryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 查詢前的語法檢查（第一道防線）：
 *  - 只允許單一 SELECT / WITH 查詢
 *  - 不允許註解（避免 /*!...*&#47; 之類的繞過）與分號（避免多重語句）
 *  - 擋掉寫入、檔案輸出、鎖定、延遲函式與系統資料庫
 *
 * 這是「盡力而為」的字串檢查，真正的安全邊界是：唯讀連線 + 只有 SELECT 權限的資料庫帳號。
 */
public final class SqlGuard {

    private static final Set<String> ALLOWED_FIRST = Set.of("SELECT", "WITH");

    private static final Set<String> FORBIDDEN = Set.of(
            // 寫入／DDL／權限／程序
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "GRANT", "REVOKE", "RENAME",
            "CALL", "EXECUTE", "PREPARE", "DEALLOCATE", "SET", "LOAD", "HANDLER", "LOCK", "UNLOCK",
            // 檔案輸出／變數寫入、鎖、延遲
            "INTO", "OUTFILE", "DUMPFILE", "LOAD_FILE", "SLEEP", "BENCHMARK",
            "GET_LOCK", "RELEASE_LOCK", "RELEASE_ALL_LOCKS", "IS_FREE_LOCK", "IS_USED_LOCK",
            // 系統資料庫
            "INFORMATION_SCHEMA", "MYSQL", "PERFORMANCE_SCHEMA", "SYS");

    private SqlGuard() {
    }

    /** 驗證通過時回傳「去掉結尾分號」的 SQL；否則丟出 QueryException。 */
    public static String validate(String raw, int maxLength) {
        if (raw == null || raw.isBlank()) {
            throw new QueryException("請輸入 SQL 查詢");
        }
        String sql = raw.strip();
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).stripTrailing();
        }
        if (sql.isEmpty()) {
            throw new QueryException("請輸入 SQL 查詢");
        }
        if (sql.length() > maxLength) {
            throw new QueryException("SQL 太長（上限 " + maxLength + " 字元）");
        }

        List<String> words = new ArrayList<>();
        int n = sql.length();
        int i = 0;
        while (i < n) {
            char ch = sql.charAt(i);
            if (ch == '\'' || ch == '"') {
                i = skipQuoted(sql, i, ch);
            } else if (ch == '`') {
                int end = sql.indexOf('`', i + 1);
                if (end < 0) {
                    throw new QueryException("反引號 ` 未結束");
                }
                words.add(sql.substring(i + 1, end).toUpperCase(Locale.ROOT));
                i = end + 1;
            } else if (ch == ';') {
                throw new QueryException("一次只能執行一個查詢（不可包含分號）");
            } else if (ch == '#'
                    || (ch == '-' && i + 1 < n && sql.charAt(i + 1) == '-')
                    || (ch == '/' && i + 1 < n && sql.charAt(i + 1) == '*')) {
                throw new QueryException("不支援 SQL 註解（# 、-- 、/* */），請移除");
            } else if (Character.isLetter(ch) || ch == '_') {
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(sql.charAt(j))
                        || sql.charAt(j) == '_' || sql.charAt(j) == '$')) {
                    j++;
                }
                words.add(sql.substring(i, j).toUpperCase(Locale.ROOT));
                i = j;
            } else {
                i++;
            }
        }

        if (words.isEmpty() || !ALLOWED_FIRST.contains(words.get(0))) {
            throw new QueryException("只允許 SELECT 查詢（可使用 WITH ... SELECT）");
        }
        for (String w : words) {
            if (FORBIDDEN.contains(w)) {
                throw new QueryException("不允許使用「" + w + "」");
            }
        }
        return sql;
    }

    /** 跳過字串常值，回傳結尾引號之後的位置。 */
    private static int skipQuoted(String s, int start, char quote) {
        int n = s.length();
        int i = start + 1;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == quote) {
                if (i + 1 < n && s.charAt(i + 1) == quote) {
                    i += 2; // 連續兩個引號＝跳脫
                } else {
                    return i + 1;
                }
            } else {
                i++;
            }
        }
        throw new QueryException("字串引號未結束");
    }
}
