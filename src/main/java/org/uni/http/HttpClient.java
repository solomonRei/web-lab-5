package org.uni.http;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private final CacheManager cacheManager;

    public HttpClient() {
        this.cacheManager = new CacheManager(true); // Enable both memory and file caching
    }

    public String makeRequest(String urlString) throws Exception {
        return makeRequest(urlString, "text/html,application/json;q=0.9,*/*;q=0.8");
    }

    public String makeRequest(String urlString, String acceptHeader) throws Exception {
        return makeRequest(urlString, acceptHeader, 0);
    }

    private String makeRequest(String urlString, String acceptHeader, int redirectCount) throws Exception {
        if (redirectCount > MAX_REDIRECTS) {
            throw new Exception("Too many redirects");
        }

        CacheEntry cachedEntry = cacheManager.get(urlString);
        if (cachedEntry != null) {
            boolean wantJson = acceptHeader.startsWith("application/json");
            boolean haveJson = cachedEntry.getContentType().contains("application/json");
            if (wantJson == haveJson) {
                return cachedEntry.getContent();
            }
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
            writer.println("Accept: " + acceptHeader);
            writer.println("Accept-Language: en-US,en;q=0.5");
            if (cachedEntry != null && cachedEntry.getEtag() != null) {
                writer.println("If-None-Match: " + cachedEntry.getEtag());
            }
            writer.println("Connection: close");
            writer.println();

            StringBuilder response = new StringBuilder();
            String line;
            String statusLine = reader.readLine();
            if (statusLine == null) {
                throw new IOException("No response from server");
            }

            Pattern statusPattern = Pattern.compile("HTTP/\\d\\.\\d (\\d+)");
            Matcher matcher = statusPattern.matcher(statusLine);
            if (matcher.find()) {
                int statusCode = Integer.parseInt(matcher.group(1));
                Map<String, String> headers = new HashMap<>();
                String contentType = null;
                String etag = null;
                long maxAge = 3600;

                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    String[] parts = line.split(": ", 2);
                    if (parts.length == 2) {
                        String headerName = parts[0].toLowerCase();
                        String headerValue = parts[1];
                        headers.put(headerName, headerValue);

                        if (headerName.equals("content-type")) {
                            contentType = headerValue;
                        } else if (headerName.equals("etag")) {
                            etag = headerValue;
                        } else if (headerName.equals("cache-control")) {
                            Matcher maxAgeMatcher = Pattern.compile("max-age=(\\d+)").matcher(headerValue);
                            if (maxAgeMatcher.find()) {
                                maxAge = Long.parseLong(maxAgeMatcher.group(1));
                            }
                        }
                    }
                }

                if (statusCode == 304 && cachedEntry != null) {
                    return cachedEntry.getContent();
                } else if (statusCode >= 300 && statusCode < 400) {
                    String location = headers.get("location");
                    if (location != null) {
                        if (!location.startsWith("http")) {
                            location = "http://" + host + (location.startsWith("/") ? "" : "/") + location;
                        }
                        return makeRequest(location, acceptHeader, redirectCount + 1);
                    }
                }

                StringBuilder body = new StringBuilder();
                if (headers.containsKey("transfer-encoding") && headers.get("transfer-encoding").equals("chunked")) {
                    while (true) {
                        String lengthLine = reader.readLine();
                        if (lengthLine == null) break;
                        int length = Integer.parseInt(lengthLine.trim(), 16);
                        if (length == 0) {
                            reader.readLine();
                            break;
                        }
                        char[] chunk = new char[length];
                        reader.read(chunk, 0, length);
                        body.append(chunk);
                        reader.readLine();
                    }
                } else {
                    while ((line = reader.readLine()) != null) {
                        body.append(line).append("\n");
                    }
                }

                String content = body.toString();

                if (contentType != null && contentType.contains("application/json")) {
                    try {
                        if (content.trim().startsWith("{")) {
                            JSONObject json = new JSONObject(content);
                            content = json.toString(2);
                        } else if (content.trim().startsWith("[")) {
                            JSONArray json = new JSONArray(content);
                            content = json.toString(2);
                        }
                    } catch (Exception e) {

                    }
                }

                if (statusCode == 200 && contentType != null) {
                    cacheManager.put(urlString, content, contentType, etag, maxAge);
                }

                return content;
            }

            throw new IOException("Invalid HTTP response");
        } finally {
            if (socket != null) {
                socket.close();
            }
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