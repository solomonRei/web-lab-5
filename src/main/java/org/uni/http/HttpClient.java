package org.uni.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;

public class HttpClient {
    private static final String USER_AGENT = "org.uni.Go2Web/1.0";

    public String makeRequest(String urlString) throws Exception {
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
            out.print("User-Agent: " + USER_AGENT + "\r\n");
            out.print("\r\n");
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            return response.toString();
        }
    }

    public String makeRequest(String urlString, String userAgent, String acceptLanguage) throws Exception {
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "http://" + urlString;
        }

        URL url = new URL(urlString);
        String host = url.getHost();
        int port = url.getPort() == -1 ? 80 : url.getPort();
        String path = url.getPath() + "?" + url.getQuery();

        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.print("GET " + path + " HTTP/1.1\r\n");
            out.print("Host: " + host + "\r\n");
            out.print("Connection: close\r\n");
            out.print("User-Agent: " + userAgent + "\r\n");
            if (acceptLanguage != null) {
                out.print("Accept-Language: " + acceptLanguage + "\r\n");
            }
            out.print("\r\n");
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            return response.toString();
        }
    }
} 