package org.uni;

import org.uni.http.HttpClient;
import org.uni.html.HtmlParser;
import org.uni.search.SearchService;

import java.util.List;

public class Go2Web {
    private final HttpClient httpClient;
    private final HtmlParser htmlParser;
    private final SearchService searchService;

    public Go2Web() {
        this.httpClient = new HttpClient(true);
        this.htmlParser = new HtmlParser();
        this.searchService = new SearchService();
    }

    public static void main(String[] args) {
        Go2Web go2web = new Go2Web();
        go2web.run(args);
    }

    public void run(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0];
        switch (command) {
            case "-u":
                if (args.length < 2) {
                    System.out.println("Error: URL is required");
                    printHelp();
                    return;
                }
                String url = args[1];
                String format = "auto";
                if (args.length >= 4 && args[2].equals("-f")) {
                    format = args[3];
                }
                
                try {
                    String acceptHeader;
                    switch (format) {
                        case "json":
                            acceptHeader = "application/json";
                            break;
                        case "html":
                            acceptHeader = "text/html; charset=UTF-8";
                            break;
                        case "auto":
                        default:
                            // For auto-detect, accept both HTML and JSON
                            acceptHeader = "text/html,application/json;q=0.9";
                            break;
                    }
                    
                    // Use the new socket-based request method
                    String response = httpClient.makeSocketRequest(url, acceptHeader);
                    System.out.println(response);
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case "-s":
                if (args.length < 2) {
                    System.out.println("Error: Search term is required");
                    printHelp();
                    return;
                }
                String searchTerm = args[1];
                String searchFormat = "html";
                if (args.length >= 4 && args[2].equals("-f")) {
                    searchFormat = args[3];
                }
                try {
                    List<String> results = searchService.search(searchTerm);
                    for (String result : results) {
                        System.out.println(result);
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                }
                break;
            case "-h":
                printHelp();
                break;
            default:
                System.out.println("Error: Unknown command");
                printHelp();
        }
    }

    private void printHelp() {
        System.out.println("Go2Web - HTTP Client with caching and content negotiation");
        System.out.println("\nUsage:");
        System.out.println("  go2web -u <URL> [-f <format>]  Make an HTTP request to the specified URL");
        System.out.println("  go2web -s <search term>        Search the web");
        System.out.println("  go2web -h                      Show this help message");
        System.out.println("\nFormat options:");
        System.out.println("  auto (default)                Automatically detect format from response");
        System.out.println("                                (Using Content-Type header and content structure)");
        System.out.println("  html                         Format response as HTML");
        System.out.println("  json                         Format response as JSON");
        System.out.println("\nFeatures:");
        System.out.println("  - Content negotiation (JSON/HTML)");
        System.out.println("  - HTTP caching with ETag support");
        System.out.println("  - Redirect handling");
        System.out.println("  - Socket-based connections with automatic SSL detection");
        System.out.println("  - Uses port 443 or HTTPS protocol for SSL connections");
        System.out.println("  - Web search with Bing");
    }
} 