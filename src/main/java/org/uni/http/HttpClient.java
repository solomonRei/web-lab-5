package org.uni.http;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpClient {
    private static final int MAX_REDIRECTS = 5;
    private static final int SOCKET_TIMEOUT = 10000; // 10 seconds
    private static final String USER_AGENT = "Go2Web/1.0";
    private final Map<String, String> cache;

    public HttpClient() {
        this.cache = new HashMap<>();
    }

    public String makeRequest(String urlString) throws Exception {
        return makeRequest(urlString, 0);
    }

    private String makeRequest(String urlString, int redirectCount) throws Exception {
        if (redirectCount > MAX_REDIRECTS) {
            throw new Exception("Too many redirects");
        }

        // Check cache first
        if (cache.containsKey(urlString)) {
            return cache.get(urlString);
        }

        URL url = new URL(urlString);
        String host = url.getHost();
        int port = url.getPort() == -1 ? (urlString.startsWith("https") ? 443 : 80) : url.getPort();
        String path = url.getPath().isEmpty() ? "/" : url.getPath();
        if (url.getQuery() != null) {
            path += "?" + url.getQuery();
        }

        Socket socket = null;
        try {
            if (urlString.startsWith("https")) {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = factory.createSocket(host, port);
            } else {
                socket = new Socket(host, port);
            }

            socket.setSoTimeout(SOCKET_TIMEOUT);

            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.println("GET " + path + " HTTP/1.1");
            writer.println("Host: " + host);
            writer.println("User-Agent: " + USER_AGENT);
            writer.println("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            writer.println("Accept-Language: en-US,en;q=0.5");
            writer.println("Connection: close");
            writer.println();

            StringBuilder response = new StringBuilder();
            String line;
            String statusLine = reader.readLine();
            response.append(statusLine).append("\n");

            Pattern statusPattern = Pattern.compile("HTTP/\\d\\.\\d (\\d+)");
            Matcher matcher = statusPattern.matcher(statusLine);
            if (matcher.find()) {
                int statusCode = Integer.parseInt(matcher.group(1));

                Map<String, String> headers = new HashMap<>();
                int contentLength = -1;
                boolean chunked = false;

                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    response.append(line).append("\n");
                    String[] parts = line.split(": ", 2);
                    if (parts.length == 2) {
                        String headerName = parts[0].toLowerCase();
                        String headerValue = parts[1];
                        headers.put(headerName, headerValue);

                        if (headerName.equals("content-length")) {
                            contentLength = Integer.parseInt(headerValue);
                        } else if (headerName.equals("transfer-encoding") &&
                                headerValue.equals("chunked")) {
                            chunked = true;
                        }
                    }
                }

                if (statusCode >= 300 && statusCode < 400) {
                    String location = headers.get("location");
                    if (location != null) {
                        if (!location.startsWith("http")) {
                            location = "http://" + host + (location.startsWith("/") ? "" : "/") + location;
                        }
                        return makeRequest(location, redirectCount + 1);
                    }
                }

                if (chunked) {
                    readChunkedBody(reader, response);
                } else if (contentLength > 0) {
                    readFixedLengthBody(reader, response, contentLength);
                } else {
                    readUntilClose(reader, response);
                }
            }

            String result = response.toString();
            cache.put(urlString, result);
            return result;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private void readChunkedBody(BufferedReader reader, StringBuilder response) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
            if (line.equals("0")) {
                break;
            }
        }
    }

    private void readFixedLengthBody(BufferedReader reader, StringBuilder response, int contentLength) throws IOException {
        char[] buffer = new char[contentLength];
        reader.read(buffer, 0, contentLength);
        response.append(buffer);
    }

    private void readUntilClose(BufferedReader reader, StringBuilder response) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
    }

    public String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }
} 