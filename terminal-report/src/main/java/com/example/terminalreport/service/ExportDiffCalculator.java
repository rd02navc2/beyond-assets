package com.example.terminalreport.service;

import com.example.terminalreport.dto.ExportDiff;
import com.example.terminalreport.dto.ExportDiff.FieldChange;
import com.example.terminalreport.dto.ExportDiff.TerminalBrief;
import com.example.terminalreport.dto.ExportDiff.TerminalChange;
import com.example.terminalreport.entity.ExportTerminal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 比對兩個批次的機台清單。機台以「分類_機號」(group_key) 作為唯一識別。 */
public final class ExportDiffCalculator {

    private ExportDiffCalculator() {
    }

    public static ExportDiff compute(List<ExportTerminal> current, List<ExportTerminal> base) {
        Map<String, List<ExportTerminal>> cur = index(current);
        Map<String, List<ExportTerminal>> old = index(base);

        List<TerminalBrief> added = new ArrayList<>();
        List<TerminalBrief> removed = new ArrayList<>();
        List<TerminalChange> changed = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();

        for (Map.Entry<String, List<ExportTerminal>> e : cur.entrySet()) {
            String key = e.getKey();
            List<ExportTerminal> rows = e.getValue();
            ExportTerminal now = rows.get(0);
            if (rows.size() > 1) {
                duplicates.add(key);
            }
            List<ExportTerminal> before = old.get(key);
            if (before == null) {
                added.add(brief(now));
            } else {
                List<FieldChange> fields = compare(before.get(0), now);
                if (!fields.isEmpty()) {
                    changed.add(new TerminalChange(key, now.getCategory(), now.getTerminalId(), fields));
                }
            }
        }
        for (Map.Entry<String, List<ExportTerminal>> e : old.entrySet()) {
            if (!cur.containsKey(e.getKey())) {
                removed.add(brief(e.getValue().get(0)));
            }
        }
        return new ExportDiff(null, null, added, removed, changed, duplicates);
    }

    static String key(ExportTerminal t) {
        if (t.getGroupKey() != null && !t.getGroupKey().isBlank()) {
            return t.getGroupKey();
        }
        return nz(t.getCategory()) + "_" + nz(t.getTerminalId());
    }

    private static Map<String, List<ExportTerminal>> index(List<ExportTerminal> list) {
        Map<String, List<ExportTerminal>> m = new LinkedHashMap<>();
        for (ExportTerminal t : list) {
            m.computeIfAbsent(key(t), k -> new ArrayList<>()).add(t);
        }
        return m;
    }

    private static TerminalBrief brief(ExportTerminal t) {
        return new TerminalBrief(t.getCategory(), t.getTerminalId(), key(t), t.getAssetDescription(),
                t.getCounterNo(), t.getMachineNo(), t.getAssetStatus(), t.isWarehouseStock());
    }

    private static List<FieldChange> compare(ExportTerminal o, ExportTerminal n) {
        List<FieldChange> out = new ArrayList<>();
        add(out, "型號描述", o.getAssetDescription(), n.getAssetDescription());
        add(out, "目前櫃號", o.getCounterNo(), n.getCounterNo());
        add(out, "目前機號", o.getMachineNo(), n.getMachineNo());
        add(out, "商戶代碼", o.getMerchantCode(), n.getMerchantCode());
        add(out, "目前狀態/櫃位", o.getAssetStatus(), n.getAssetStatus());
        add(out, "倉庫庫存", o.isWarehouseStock() ? "是" : "否", n.isWarehouseStock() ? "是" : "否");
        add(out, "累計換機次數", String.valueOf(o.getChangeCount()), String.valueOf(n.getChangeCount()));
        add(out, "最近換機日期", str(o.getLastChangeDate()), str(n.getLastChangeDate()));

        Map<String, Object> oe = o.getExtraFields() != null ? o.getExtraFields() : Map.of();
        Map<String, Object> ne = n.getExtraFields() != null ? n.getExtraFields() : Map.of();
        Set<String> keys = new LinkedHashSet<>(oe.keySet());
        keys.addAll(ne.keySet());
        for (String k : keys) {
            add(out, k, str(oe.get(k)), str(ne.get(k)));
        }
        return out;
    }

    private static void add(List<FieldChange> out, String label, String oldV, String newV) {
        String a = nz(oldV);
        String b = nz(newV);
        if (!a.equals(b)) {
            out.add(new FieldChange(label, a, b));
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.strip();
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
