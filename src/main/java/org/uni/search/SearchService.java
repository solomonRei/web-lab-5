package org.uni.search;

import org.uni.http.HttpClient;
import org.uni.html.HtmlParser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SearchService {
    private final HttpClient httpClient;
    private final HtmlParser htmlParser;
    private static final String SEARCH_URL = "http://www.bing.com/search?q=";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) org.uni.Go2Web/1.0";
    private static final String ACCEPT_LANGUAGE = "en-US,en;q=0.9";

    public SearchService() {
        this.httpClient = new HttpClient();
        this.htmlParser = new HtmlParser();
    }

    public List<String> search(String searchTerm) throws Exception {
        String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
        String response = httpClient.makeRequest(SEARCH_URL + encodedSearchTerm, USER_AGENT, ACCEPT_LANGUAGE);
        return htmlParser.parseSearchResults(response);
    }
} 