package ir.orix.school.shared.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PrintDocuments {
    private PrintDocuments() {}

    public static String page(String title, String body, boolean rtl) {
        String dir = rtl ? "rtl" : "ltr";
        String lang = rtl ? "fa" : "en";
        return """
            <!doctype html><html lang="%s" dir="%s"><head><meta charset="utf-8"/>
            <title>%s</title>
            <style>
              body{font-family:Tahoma,Vazirmatn,sans-serif;margin:24px;color:#111;background:#fff}
              .sheet{max-width:800px;margin:0 auto;border:1px solid #d4d4d4;padding:28px;border-radius:16px}
              h1{font-size:22px;margin:0 0 4px} .muted{color:#737373;font-size:13px}
              table{width:100%%;border-collapse:collapse;margin-top:16px;font-size:14px}
              th,td{border-bottom:1px solid #eee;padding:8px;text-align:start}
              .stamp{margin-top:28px;display:flex;justify-content:space-between;font-size:13px}
              .qr{width:96px;height:96px;border:1px dashed #bbb;display:flex;align-items:center;justify-content:center;font-size:11px;text-align:center}
              @media print { button{display:none} body{margin:0} .sheet{border:none;border-radius:0} }
            </style></head><body>
            <button onclick="window.print()">Print</button>
            <div class="sheet">%s</div></body></html>
            """.formatted(lang, dir, escape(title), body);
    }

    public static String money(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    public static String day(LocalDate date) { return date == null ? "—" : date.toString(); }
    public static String instant(Instant value, String tz) {
        if (value == null) return "—";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of(tz == null ? "UTC" : tz)).format(value);
    }
    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    public static String rows(List<String> htmlRows) { return String.join("", htmlRows); }
}
