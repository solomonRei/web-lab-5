package org.uni.search;

import org.uni.http.HttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchService {
    private final HttpClient httpClient;
    private static final String BING_SEARCH_URL = "https://www.bing.com/search?q=";
    private static final Pattern[] SEARCH_PATTERNS = {
            Pattern.compile("<li class=\"b_algo\"[^>]*>.*?<h2><a href=\"([^\"]+)\"[^>]*>(.*?)</a></h2>", Pattern.DOTALL),
            Pattern.compile("<h2><a href=\"([^\"]+)\"[^>]*>(.*?)</a></h2>", Pattern.DOTALL),
            Pattern.compile("<a href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.DOTALL)
    };

    public SearchService() {
        this.httpClient = new HttpClient(true);
    }

    public List<String> search(String query) throws IOException {
        HttpClient httpClient = new HttpClient(true);
        String encodedQuery = httpClient.encodeUrl(query);
        String searchUrl = String.format("https://www.bing.com/search?q=%s", encodedQuery);
        
        String response = httpClient.makeSocketRequest(searchUrl, "text/html");
        
        List<String> results = extractSearchResults(response);
        if (results.isEmpty()) {
            throw new IOException("No search results found. Please try a different search term.");
        }
        
        return results;
    }

    private List<String> extractSearchResults(String response) {
        List<String> results = new ArrayList<>();

        for (Pattern pattern : SEARCH_PATTERNS) {
            Matcher matcher = pattern.matcher(response);
            while (matcher.find() && results.size() < 10) {
                if (matcher.groupCount() >= 2) {
                    String href = matcher.group(1);
                    String title = matcher.group(2).replaceAll("<[^>]+>", "").trim();

                    if (isValidUrl(href) && !title.isEmpty() && !title.equals("Web")) {
                        results.add(href + " - " + title);
                    }
                }
            }

            if (!results.isEmpty()) {
                break;
            }
        }

        if (results.isEmpty()) {
            Pattern urlPattern = Pattern.compile("https?://[^\\s\"><]+");
            Matcher urlMatcher = urlPattern.matcher(response);
            int count = 0;
            while (urlMatcher.find() && count < 10) {
                String url2 = urlMatcher.group();
                if (isValidUrl(url2) && !url2.contains("bing.com") && !url2.contains("microsoft.com")) {
                    results.add(url2);
                    count++;
                }
            }
        }

        return results;
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
} 