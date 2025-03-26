package org.uni.search;

import org.uni.http.HttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchService {
    private final HttpClient httpClient;
    private static final String BING_SEARCH_URL = "http://www.bing.com/search?q=";
    private static final Pattern[] SEARCH_PATTERNS = {
            Pattern.compile("<h2[^>]*>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>"),
            Pattern.compile("<div class=\"b_title\">\\s*<h2>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>"),
            Pattern.compile("<a[^>]*href=\"([^\"]+)\"[^>]*title=\"([^\"]+)\"")
    };

    public SearchService() {
        this.httpClient = new HttpClient();
    }

    public List<String> search(String searchTerm) throws Exception {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Search term cannot be empty");
        }

        String encodedSearchTerm = httpClient.encodeUrl(searchTerm);
        String response = httpClient.makeRequest(BING_SEARCH_URL + encodedSearchTerm);
        List<String> results = new ArrayList<>();

        for (Pattern pattern : SEARCH_PATTERNS) {
            Matcher matcher = pattern.matcher(response);
            while (matcher.find() && results.size() < 10) {
                String url = matcher.group(1);
                String title = matcher.group(2).replaceAll("<[^>]+>", "").trim();

                if (isValidUrl(url) && !title.isEmpty() && !title.equals("Web")) {
                    results.add("\u001B[34m" + url + "\u001B[0m" + " - " + title);
                }
            }

            if (!results.isEmpty()) {
                break;
            }
        }

        if (results.isEmpty()) {
            throw new Exception("No search results found. The search engine might have blocked the request.");
        }

        return results;
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 