package org.uni.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".go2web_cache";
    private final Map<String, CacheEntry> memoryCache;
    private final boolean useFileCache;

    public CacheManager(boolean useFileCache) {
        this.memoryCache = new ConcurrentHashMap<>();
        this.useFileCache = useFileCache;
        if (useFileCache) {
            createCacheDirectory();
        }
    }

    private void createCacheDirectory() {
        try {
            Files.createDirectories(Paths.get(CACHE_DIR));
        } catch (IOException e) {
            System.err.println("Warning: Could not create cache directory: " + e.getMessage());
        }
    }

    public CacheEntry get(String url) {
        String key = generateKey(url);

        // Try memory cache first
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry;
        }

        // Try file cache if enabled
        if (useFileCache) {
            entry = readFromFile(key);
            if (entry != null && !entry.isExpired()) {
                // Update memory cache
                memoryCache.put(key, entry);
                return entry;
            }
        }

        return null;
    }

    public void put(String url, String content, String contentType, String etag, long maxAgeSeconds) {
        String key = generateKey(url);
        Instant expirationTime = Instant.now().plus(maxAgeSeconds, ChronoUnit.SECONDS);
        CacheEntry entry = new CacheEntry(content, contentType, expirationTime, etag);

        // Update memory cache
        memoryCache.put(key, entry);

        // Update file cache if enabled
        if (useFileCache) {
            writeToFile(key, entry);
        }
    }

    private String generateKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return url.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }

    private void writeToFile(String key, CacheEntry entry) {
        if (!useFileCache) return;

        String filePath = CACHE_DIR + File.separator + key;
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(entry);
        } catch (IOException e) {
            System.err.println("Warning: Could not write to cache file: " + e.getMessage());
        }
    }

    private CacheEntry readFromFile(String key) {
        if (!useFileCache) return null;

        String filePath = CACHE_DIR + File.separator + key;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (CacheEntry) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    public void clearExpired() {
        // Clear memory cache
        memoryCache.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Clear file cache if enabled
        if (useFileCache) {
            try {
                Files.list(Paths.get(CACHE_DIR)).forEach(path -> {
                    try {
                        CacheEntry entry = readFromFile(path.getFileName().toString());
                        if (entry == null || entry.isExpired()) {
                            Files.deleteIfExists(path);
                        }
                    } catch (IOException e) {
                        // Ignore errors when cleaning cache
                    }
                });
            } catch (IOException e) {
                // Ignore errors when cleaning cache
            }
        }
    }
} 