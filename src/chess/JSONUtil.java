package chess;

import java.util.LinkedHashMap;
import java.util.Map;

public class JSONUtil {

    public static String encode(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":");
            sb.append(encodeValue(e.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String encodeValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Integer) return v.toString();
        if (v instanceof int[]) {
            int[] arr = (int[]) v;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(arr[i]);
            }
            return sb.append("]").toString();
        }
        if (v instanceof int[][]) {
            int[][] arr = (int[][]) v;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(encodeValue(arr[i]));
            }
            return sb.append("]").toString();
        }
        if (v instanceof String[][]) {
            String[][] board = (String[][]) v;
            StringBuilder sb = new StringBuilder("[");
            for (int r = 0; r < board.length; r++) {
                if (r > 0) sb.append(",");
                sb.append("[");
                for (int c = 0; c < board[r].length; c++) {
                    if (c > 0) sb.append(",");
                    sb.append("\"").append(board[r][c]).append("\"");
                }
                sb.append("]");
            }
            return sb.append("]").toString();
        }
        return "\"" + v.toString().replace("\"", "\\\"") + "\"";
    }

    public static Map<String, Object> decode(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        json = json.trim();
        if (json.length() < 2) return map;
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return map;

        int i = 0;
        while (i < json.length()) {
            if (json.charAt(i) != '"') { i++; continue; }
            int keyEnd = json.indexOf('"', i + 1);
            if (keyEnd < 0) break;
            String key = json.substring(i + 1, keyEnd);
            i = keyEnd + 1;
            int colon = json.indexOf(':', i);
            if (colon < 0) break;
            i = colon + 1;
            while (i < json.length() && json.charAt(i) == ' ') i++;
            char ch = json.charAt(i);

            if (ch == '"') {
                int end = i + 1;
                while (end < json.length()) {
                    if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                    end++;
                }
                map.put(key, json.substring(i + 1, end));
                i = end + 1;
            } else if (ch == '[') {
                int depth = 0, end = i;
                while (end < json.length()) {
                    if (json.charAt(end) == '[') depth++;
                    else if (json.charAt(end) == ']') { depth--; if (depth == 0) break; }
                    end++;
                }
                String raw = json.substring(i, end + 1);
                if ("board".equals(key)) {
                    map.put(key, parseBoard(raw));
                } else if ("moves".equals(key)) {
                    map.put(key, parseIntArrayArray(raw));
                } else {
                    map.put(key, parseIntArray(raw));
                }
                i = end + 1;
            } else if (json.startsWith("null", i)) {
                map.put(key, null);
                i += 4;
            } else if (json.startsWith("true", i)) {
                map.put(key, Boolean.TRUE);
                i += 4;
            } else if (json.startsWith("false", i)) {
                map.put(key, Boolean.FALSE);
                i += 5;
            } else {
                int end = i;
                while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
                try { map.put(key, Integer.parseInt(json.substring(i, end).trim())); }
                catch (NumberFormatException e) { map.put(key, json.substring(i, end).trim()); }
                i = end;
            }
            while (i < json.length() && (json.charAt(i) == ',' || json.charAt(i) == ' ')) i++;
        }
        return map;
    }

    private static int[] parseIntArray(String raw) {
        raw = raw.trim();
        if (raw.equals("null") || raw.length() < 2) return null;
        raw = raw.substring(1, raw.length() - 1).trim();
        if (raw.isEmpty()) return new int[0];
        String[] parts = raw.split(",");
        int[] arr = new int[parts.length];
        for (int i = 0; i < parts.length; i++) arr[i] = Integer.parseInt(parts[i].trim());
        return arr;
    }

    private static int[][] parseIntArrayArray(String raw) {
        raw = raw.trim();
        if (raw.equals("null") || raw.length() < 2) return new int[0][];
        raw = raw.substring(1, raw.length() - 1).trim();
        if (raw.isEmpty()) return new int[0][];
        java.util.List<int[]> result = new java.util.ArrayList<>();
        int i = 0;
        while (i < raw.length()) {
            if (raw.charAt(i) != '[') { i++; continue; }
            int end = raw.indexOf(']', i);
            if (end < 0) break;
            result.add(parseIntArray(raw.substring(i, end + 1)));
            i = end + 1;
        }
        return result.toArray(new int[0][]);
    }

    private static String[][] parseBoard(String raw) {
        raw = raw.trim().substring(1, raw.length() - 1);
        String[][] board = new String[8][8];
        int row = 0, i = 0;
        while (i < raw.length() && row < 8) {
            if (raw.charAt(i) != '[') { i++; continue; }
            int end = raw.indexOf(']', i);
            String rowStr = raw.substring(i + 1, end);
            String[] cells = rowStr.split(",");
            for (int c = 0; c < 8 && c < cells.length; c++) {
                board[row][c] = cells[c].trim().replace("\"", "");
            }
            row++;
            i = end + 1;
        }
        return board;
    }
}
