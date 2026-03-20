package com.ghlzm.iot.common.util;

/**
 * JSON 负载辅助工具。
 * 兼容历史设备在合法 JSON 文档前后附带控制字符或尾随脏字符的场景。
 */
public final class JsonPayloadUtils {

    private JsonPayloadUtils() {
    }

    public static String normalizeJsonDocument(String text) {
        if (text == null) {
            return null;
        }
        int startIndex = findJsonStart(text);
        if (startIndex < 0) {
            return text.trim();
        }
        int endIndex = findJsonDocumentEnd(text, startIndex);
        String candidate = endIndex >= startIndex
                ? text.substring(startIndex, endIndex + 1)
                : text.substring(startIndex);
        return candidate.trim();
    }

    private static int findJsonStart(String text) {
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '{' || current == '[') {
                return index;
            }
        }
        return -1;
    }

    private static int findJsonDocumentEnd(String text, int startIndex) {
        char opening = text.charAt(startIndex);
        char closing = opening == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int index = startIndex; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (current == '\\') {
                    escaping = true;
                    continue;
                }
                if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == opening) {
                depth++;
                continue;
            }
            if (current == closing) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }
}
