package org.uni;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;

public class Go2Web {

    public static void main(String[] args) {
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

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  go2web -u <URL>         # make an HTTP request to the specified URL and print the response");
        System.out.println("  go2web -s <search-term> # make an HTTP request to search the term using Bing and print top 10 results");
        System.out.println("  go2web -h               # show this help");
    }

    private static void makeHttpRequest(String urlString) throws Exception {
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "http://" + urlString;
        }

        URL url = new URL(urlString);
        String host = url.getHost();
        int port = url.getPort() == -1 ? 80 : url.getPort();
        String path = url.getPath().isEmpty() ? "/" : url.getPath();
        if (url.getQuery() != null) {
            path += "?" + url.getQuery();
        }

        System.out.println("Connecting to: " + host + " on port " + port);
        System.out.println("Requesting path: " + path);
        System.out.println();

        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.print("GET " + path + " HTTP/1.1\r\n");
            out.print("Host: " + host + "\r\n");
            out.print("Connection: close\r\n");
            out.print("User-Agent: org.uni.Go2Web/1.0\r\n");
            out.print("\r\n");
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            String htmlResponse = response.toString();

            int bodyStart = htmlResponse.indexOf("\r\n\r\n");
            if (bodyStart != -1) {
                String headers = htmlResponse.substring(0, bodyStart);
                String body = htmlResponse.substring(bodyStart + 4);

                String[] headerLines = headers.split("\r\n");
                if (headerLines.length > 0) {
                    System.out.println("Status: " + headerLines[0]);
                    System.out.println("\nResponse Headers:");
                    for (int i = 1; i < headerLines.length; i++) {
                        System.out.println("  " + headerLines[i]);
                    }
                }

                String cleanBody = removeHtmlTags(body);
                System.out.println("\nResponse Body:");
                System.out.println(cleanBody);
            } else {
                String cleanResponse = removeHtmlTags(htmlResponse);
                System.out.println(cleanResponse);
            }
        }
    }


    private static String removeHtmlTags(String html) {
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
} 