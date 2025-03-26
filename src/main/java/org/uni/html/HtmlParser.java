package org.uni.html;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {
    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();

    static {
        HTML_ENTITIES.put("&amp;", "&");
        HTML_ENTITIES.put("&lt;", "<");
        HTML_ENTITIES.put("&gt;", ">");
        HTML_ENTITIES.put("&quot;", "\"");
        HTML_ENTITIES.put("&nbsp;", " ");
        HTML_ENTITIES.put("&#39;", "'");
        HTML_ENTITIES.put("&apos;", "'");
        HTML_ENTITIES.put("&copy;", "©");
        HTML_ENTITIES.put("&reg;", "®");
        HTML_ENTITIES.put("&trade;", "™");
        HTML_ENTITIES.put("&mdash;", "—");
        HTML_ENTITIES.put("&ndash;", "–");
        HTML_ENTITIES.put("&hellip;", "…");
    }

    public String parseHttpResponse(String response) {
        String[] parts = response.split("\r?\n\r?\n", 2);
        if (parts.length < 2) {
            return response;
        }

        String headers = parts[0];
        String body = parts[1];

        StringBuilder formattedResponse = new StringBuilder();
        formattedResponse.append("\u001B[1m=== Headers ===\u001B[0m\n");
        for (String header : headers.split("\r?\n")) {
            formattedResponse.append(header).append("\n");
        }

        formattedResponse.append("\n\u001B[1m=== Body ===\u001B[0m\n");
        String text = body
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<style[^>]*>.*?</style>", "")
                .replaceAll("</(p|div|section|article|header|footer|nav|main)>", "\n")
                .replaceAll("<h([1-6])[^>]*>(.*?)</h\\1>", "\n\u001B[1m$2\u001B[0m\n")
                .replaceAll("<ul[^>]*>", "\n")
                .replaceAll("<ol[^>]*>", "\n")
                .replaceAll("<li[^>]*>", "• ")
                .replaceAll("<strong[^>]*>(.*?)</strong>", "\u001B[1m$1\u001B[0m")
                .replaceAll("<em[^>]*>(.*?)</em>", "\u001B[3m$1\u001B[0m")
                .replaceAll("<[^>]+>", "");

        for (Map.Entry<String, String> entry : HTML_ENTITIES.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }

        text = text
                .replaceAll("\\s+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n")
                .trim();

        Pattern linkPattern = Pattern.compile("https?://[^\\s]+");
        Matcher matcher = linkPattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group();
            matcher.appendReplacement(sb, "\u001B[34m" + url + "\u001B[0m");
        }
        matcher.appendTail(sb);

        formattedResponse.append(sb.toString());
        return formattedResponse.toString();
    }
} 