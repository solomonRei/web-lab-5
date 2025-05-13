package org.uni.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.uni.html.HtmlParser;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpClient {
    private static final int MAX_REDIRECTS = 5;
    private final CacheManager cacheManager;
    private int redirectCount = 0;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    public HttpClient(boolean useFileCache) {
        this.cacheManager = new CacheManager(useFileCache);
    }

    public String makeSocketRequest(String urlString, String acceptHeader) throws IOException {
        if (redirectCount >= MAX_REDIRECTS) {
            throw new IOException("Too many redirects");
        }
        
        URL url = new URL(urlString);
        String host = url.getHost();
        int port = url.getPort();
        String path = url.getPath();
        if (path.isEmpty()) {
            path = "/";
        }
        if (url.getQuery() != null) {
            path += "?" + url.getQuery();
        }

        if (port == -1) {
            boolean isSecure = urlString.startsWith("https://");
            port = isSecure ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
        }

        // Проверяем кеш перед отправкой запроса
        CacheEntry cachedEntry = cacheManager.get(urlString);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            System.out.println("Using cached response for: " + urlString);
            return cachedEntry.getContent();
        }

        Socket socket = null;
        StringBuilder responseBuilder = new StringBuilder();
        String etag = null;
        
        try {
            if (port == DEFAULT_HTTPS_PORT || urlString.startsWith("https://")) {
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslSocketFactory.createSocket(host, port);
            } else {
                socket = new Socket(host, port);
            }

            StringBuilder requestBuilder = new StringBuilder();
            requestBuilder.append("GET ").append(path).append(" HTTP/1.1\r\n");
            requestBuilder.append("Host: ").append(host).append("\r\n");
            requestBuilder.append("Connection: close\r\n");
            requestBuilder.append("Accept: ").append(acceptHeader).append("\r\n");
            
            if (urlString.contains("bing.com") || urlString.contains("google.com")) {
                requestBuilder.append("User-Agent: ").append(USER_AGENT).append("\r\n");
                requestBuilder.append("Accept-Language: en-US,en;q=0.9\r\n");
                requestBuilder.append("Referer: https://www.google.com/\r\n");
            } else {
                requestBuilder.append("User-Agent: Go2Web/1.0\r\n");
            }
            
            // Добавляем If-None-Match, если у нас есть закешированное значение с ETag
            if (cachedEntry != null && cachedEntry.getEtag() != null) {
                requestBuilder.append("If-None-Match: ").append(cachedEntry.getEtag()).append("\r\n");
            }

            requestBuilder.append("\r\n");
            
            // Отправка запроса
            OutputStream out = socket.getOutputStream();
            out.write(requestBuilder.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Чтение ответа
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Игнорируем ошибки закрытия сокета
                }
            }
        }

        String fullResponse = responseBuilder.toString();
        if (fullResponse.isEmpty()) {
            throw new IOException("Empty response received from server");
        }

        String[] parts = fullResponse.split("\r?\n\r?\n", 2);
        String headers = parts[0];
        String body = parts.length > 1 ? parts[1] : "";


        String statusLine = headers.split("\r?\n", 2)[0];
        int statusCode;
        try {
            statusCode = Integer.parseInt(statusLine.split(" ", 3)[1]);
        } catch (Exception e) {
            throw new IOException("Invalid HTTP response: " + statusLine);
        }


        if (statusCode == 304 && cachedEntry != null && !cachedEntry.isExpired()) {
            System.out.println("304 Not Modified - using cache for: " + urlString);
            return cachedEntry.getContent();
        }


        if (statusCode >= 300 && statusCode < 400) {
            String[] headerLines = headers.split("\r?\n");
            String location = null;
            for (String header : headerLines) {
                if (header.toLowerCase().startsWith("location:")) {
                    location = header.substring(9).trim();
                    break;
                }
            }
            
            if (location != null) {
                if (!location.startsWith("http")) {
                    if (location.startsWith("/")) {
                        String protocol = port == DEFAULT_HTTPS_PORT ? "https" : "http";
                        location = protocol + "://" + host + location;
                    } else {
                        String protocol = port == DEFAULT_HTTPS_PORT ? "https" : "http";
                        if (path.lastIndexOf('/') > 0) {
                            location = protocol + "://" + host + path.substring(0, path.lastIndexOf('/') + 1) + location;
                        } else {
                            location = protocol + "://" + host + "/" + location;
                        }
                    }
                }
                redirectCount++;
                System.out.println("Redirect to: " + location);
                return makeSocketRequest(location, acceptHeader);
            }
        }


        String contentType = null;
        int cacheTime = 3600;
        
        if (statusCode == 200) {
            String[] headerLines = headers.split("\r?\n");
            for (String header : headerLines) {
                String lowerHeader = header.toLowerCase();
                if (lowerHeader.startsWith("content-type:")) {
                    contentType = header.substring(13).trim();
                } else if (lowerHeader.startsWith("etag:")) {
                    etag = header.substring(5).trim();
                } else if (lowerHeader.startsWith("cache-control:")) {
                    String cacheControl = header.substring(14).trim().toLowerCase();
                    if (cacheControl.contains("max-age=")) {
                        try {
                            String maxAgeStr = cacheControl.split("max-age=")[1].split("[,;]")[0];
                            cacheTime = Integer.parseInt(maxAgeStr);
                        } catch (Exception e) {

                        }
                    }
                }
            }
        }


        String processedResponse;
        
        if (isSearchRequest(urlString)) {
            processedResponse = body;
        } else if ((contentType != null && contentType.contains("application/json")) ||
                  (acceptHeader.contains("application/json") && !acceptHeader.contains("text/html")) || 
                  isJsonResponse(body)) {
            try {
                if (body.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(body);
                    processedResponse = jsonArray.toString(2);
                } else if (body.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(body);
                    processedResponse = jsonObject.toString(2);
                } else {
                    processedResponse = body;
                }
            } catch (JSONException e) {
                processedResponse = body;
            }
        } else if ((contentType != null && contentType.contains("text/html")) ||
                  (acceptHeader.contains("text/html")) || 
                  isHtmlResponse(body)) {
            Map<String, String> headerMap = new HashMap<>();
            String[] headerLines = headers.split("\r?\n");
            for (String header : headerLines) {
                int colonIndex = header.indexOf(':');
                if (colonIndex > 0) {
                    String key = header.substring(0, colonIndex).trim();
                    String value = header.substring(colonIndex + 1).trim();
                    headerMap.put(key, value);
                }
            }
            
            processedResponse = "=== Headers ===\n" + formatHeaders(headerMap) + "\n\n=== Body ===\n" + HtmlParser.parseHtmlContent(body);
        } else {
            processedResponse = body;
        }
        

        if (statusCode == 200) {
            CacheEntry entry = new CacheEntry(processedResponse, contentType, etag, cacheTime);
            cacheManager.put(urlString, entry);
            System.out.println("Cached processed response for: " + urlString + " with ETag: " + etag);
        }
        
        return processedResponse;
    }
    
    private String formatHeaders(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private boolean isJsonResponse(String content) {
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean isHtmlResponse(String content) {
        String trimmed = content.trim().toLowerCase();
        return trimmed.startsWith("<!doctype html") || 
               trimmed.startsWith("<html") || 
               (trimmed.contains("<head") && trimmed.contains("<body"));
    }

    private boolean isSearchRequest(String urlString) {
        return urlString.contains("bing.com/search") || 
               urlString.contains("google.com/search");
    }

    public String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }
} 