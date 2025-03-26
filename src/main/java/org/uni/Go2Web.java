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
        this.httpClient = new HttpClient();
        this.htmlParser = new HtmlParser();
        this.searchService = new SearchService();
    }

    public static void main(String[] args) {
        Go2Web go2web = new Go2Web();
        go2web.run(args);
    }

    private void run(String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equals("-h"))) {
            printHelp();
            return;
        }

        if (args.length >= 2) {
            String option = args[0];
            StringBuilder valueBuilder = new StringBuilder(args[1]);

            if (option.equals("-s") && args.length > 2) {
                for (int i = 2; i < args.length; i++) {
                    valueBuilder.append(" ").append(args[i]);
                }
            }

            String value = valueBuilder.toString();

            try {
                switch (option) {
                    case "-u":
                        makeHttpRequest(value);
                        break;
                    case "-s":
                        searchWeb(value);
                        break;
                    default:
                        System.out.println("Unknown option: " + option);
                        printHelp();
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            printHelp();
        }
    }

    private void printHelp() {
        System.out.println("Usage:");
        System.out.println("  go2web -u <URL>         # make an HTTP request to the specified URL and print the response");
        System.out.println("  go2web -s <search-term> # make an HTTP request to search the term using Bing and print top 10 results");
        System.out.println("  go2web -h               # show this help");
    }

    private void makeHttpRequest(String urlString) throws Exception {
        String response = httpClient.makeRequest(urlString);
        System.out.println(htmlParser.parseHttpResponse(response));
    }

    private void searchWeb(String searchTerm) throws Exception {
        List<String> searchResults = searchService.search(searchTerm);

        System.out.println("Top results for: " + searchTerm);
        int count = 0;
        for (String result : searchResults) {
            if (count++ < 10) {
                System.out.println((count) + ". " + result);
            } else {
                break;
            }
        }
    }
} 