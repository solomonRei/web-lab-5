package org.uni.html;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {
    public String removeHtmlTags(String html) {
        String noHtml = html.replaceAll("<[^>]*>", "");

        noHtml = noHtml.replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&#39;", "'");

        noHtml = noHtml.replaceAll("\\s+", " ").trim();

        return noHtml;
    }

    public List<String> parseSearchResults(String html) {
        List<String> results = new ArrayList<>();

        int bodyStart = html.indexOf("\r\n\r\n");
        if (bodyStart != -1) {
            html = html.substring(bodyStart + 4);
        }

        Pattern pattern = Pattern.compile("<h2[^>]*>(.*?)</h2>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String result = removeHtmlTags(matcher.group(1));
            if (!result.trim().isEmpty() && !result.equals("Web")) {
                results.add(result.trim());
            }
        }

        if (results.isEmpty()) {
            pattern = Pattern.compile("<a[^>]*>(.*?)</a>", Pattern.DOTALL);
            matcher = pattern.matcher(html);

            while (matcher.find()) {
                String result = removeHtmlTags(matcher.group(1));
                if (!result.trim().isEmpty() && result.length() > 5) {
                    results.add(result.trim());
                }
            }
        }

        return results;
    }

    public String parseHttpResponse(String response) {
        int bodyStart = response.indexOf("\r\n\r\n");
        if (bodyStart != -1) {
            String headers = response.substring(0, bodyStart);
            String body = response.substring(bodyStart + 4);

            String[] headerLines = headers.split("\r\n");
            StringBuilder result = new StringBuilder();
            
            if (headerLines.length > 0) {
                result.append("Status: ").append(headerLines[0]).append("\n\n");
                result.append("Response Headers:\n");
                for (int i = 1; i < headerLines.length; i++) {
                    result.append("  ").append(headerLines[i]).append("\n");
                }
            }

            result.append("\nResponse Body:\n");
            result.append(removeHtmlTags(body));
            
            return result.toString();
        } else {
            return removeHtmlTags(response);
        }
    }
} 