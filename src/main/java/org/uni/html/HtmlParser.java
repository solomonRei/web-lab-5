package org.uni.html;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

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

    public static String parseHttpResponse(HttpURLConnection conn, String responseBody) throws IOException {
        // Format headers
        StringBuilder headers = new StringBuilder();
        headers.append("Status: ").append(conn.getResponseCode()).append(" ").append(conn.getResponseMessage()).append("\n");

        for (Map.Entry<String, java.util.List<String>> header : conn.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                headers.append(header.getKey()).append(": ");
                for (String value : header.getValue()) {
                    headers.append(value);
                }
                headers.append("\n");
            }
        }

        String formattedHeaders = headers.toString().replaceAll("(?m)^", "  ");
        String parsedBody = parseHtmlContentInternal(responseBody);

        return "=== Headers ===\n" + formattedHeaders + "\n\n=== Body ===\n" + parsedBody;
    }

    private static String parseHtmlContentInternal(String htmlContent) {
        String body = htmlContent.replaceAll("(?s)<script.*?</script>", ""); // Remove scripts
        body = body.replaceAll("(?s)<style.*?</style>", "");   // Remove styles

        body = body.replaceAll("(?s)</?(div|p|section|article|header|footer|nav|main|aside)[^>]*>", "\n$0\n");
        body = body.replaceAll("(?s)<br[^>]*>", "\n");
        body = body.replaceAll("(?s)<hr[^>]*>", "\n" + "─".repeat(40) + "\n");

        // Format headings
        body = body.replaceAll("(?s)<h1[^>]*>(.*?)</h1>", "\n\n\033[1m$1\033[0m\n");
        body = body.replaceAll("(?s)<h2[^>]*>(.*?)</h2>", "\n\n\033[1m$1\033[0m\n");
        body = body.replaceAll("(?s)<h3[^>]*>(.*?)</h3>", "\n\n\033[1m$1\033[0m\n");
        body = body.replaceAll("(?s)<h[4-6][^>]*>(.*?)</h[4-6]>", "\n\n$1\n");

        // Format lists
        body = body.replaceAll("(?s)<ul[^>]*>", "\n");
        body = body.replaceAll("(?s)</ul>", "\n");
        body = body.replaceAll("(?s)<ol[^>]*>", "\n");
        body = body.replaceAll("(?s)</ol>", "\n");
        body = body.replaceAll("(?s)<li[^>]*>(.*?)</li>", "  • $1\n");

        // Format text styles
        body = body.replaceAll("(?s)<b[^>]*>(.*?)</b>", "\033[1m$1\033[0m");
        body = body.replaceAll("(?s)<i[^>]*>(.*?)</i>", "\033[3m$1\033[0m");
        body = body.replaceAll("(?s)<code[^>]*>(.*?)</code>", "\033[36m$1\033[0m");
        body = body.replaceAll("(?s)<pre[^>]*>(.*?)</pre>", "\n\033[36m$1\033[0m\n");

        // Remove remaining HTML tags
        body = body.replaceAll("<[^>]+>", "");

        // Decode HTML entities
        for (Map.Entry<String, String> entry : HTML_ENTITIES.entrySet()) {
            body = body.replace(entry.getKey(), entry.getValue());
        }

        // Clean up whitespace
        body = body.replaceAll("(?m)^\\s+$", "");
        body = body.replaceAll("\n{3,}", "\n\n");
        body = body.trim();

        body = body.replaceAll("(https?://[^\\s]+)", "\033[34m$1\033[0m");

        return body;
    }

    public static String parseHtmlContent(String htmlContent) {
        return parseHtmlContentInternal(htmlContent);
    }
} 