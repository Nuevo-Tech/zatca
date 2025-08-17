package com.nuevo.zatca.filter;

import java.util.regex.Pattern;

public class InputValidator {

    private static final Pattern HEX_ESCAPE = Pattern.compile("\\\\x([0-9a-fA-F]{2})");

    // Example: Add more dangerous patterns as needed
    private static final Pattern[] MALICIOUS_PATTERNS = {

            // XSS payloads
            Pattern.compile("(?i)<\\s*script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%3Cscript", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<.*?on\\w+\\s*=.*?>", Pattern.CASE_INSENSITIVE),  // onerror=, onclick=, etc.
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
            Pattern.compile("document\\.cookie", Pattern.CASE_INSENSITIVE),
            Pattern.compile("alert\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe.*?>.*?</iframe.*?>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<img.*?>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<svg.*?>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE), // CSS/JS expressions

            // SQL Injection payloads
            Pattern.compile("onload", Pattern.CASE_INSENSITIVE),
            Pattern.compile("drop table", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([';]+|(--)+)", Pattern.CASE_INSENSITIVE), // ' ; --
            Pattern.compile("union(.*?)select", Pattern.CASE_INSENSITIVE),
            Pattern.compile("drop\\s+table", Pattern.CASE_INSENSITIVE),
            Pattern.compile("insert\\s+into", Pattern.CASE_INSENSITIVE),
            Pattern.compile("update\\s+.*?set", Pattern.CASE_INSENSITIVE),
            Pattern.compile("delete\\s+from", Pattern.CASE_INSENSITIVE),
            Pattern.compile("sleep\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("benchmark\\s*\\(", Pattern.CASE_INSENSITIVE),

            // Path traversal / OS commands
            Pattern.compile("\\.\\./", Pattern.CASE_INSENSITIVE),   // ../
            Pattern.compile("etc/passwd", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cmd.exe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("powershell", Pattern.CASE_INSENSITIVE),
            Pattern.compile("wget ", Pattern.CASE_INSENSITIVE),
            Pattern.compile("curl ", Pattern.CASE_INSENSITIVE)

    };

    public static boolean isMalicious(String value) {
        if (value == null) return false;
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }
}
