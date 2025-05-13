package org.uni.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".go2web_cache";
    private final ConcurrentHashMap<String, CacheEntry> memoryCache;
    private final boolean useFileCache;

    public CacheManager(boolean useFileCache) {
        this.memoryCache = new ConcurrentHashMap<>();
        this.useFileCache = useFileCache;
        if (useFileCache) {
            createCacheDirectory();
        }
    }

    private void createCacheDirectory() {
        File cacheDir = new File(CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    public CacheEntry get(String url) {
        String key = generateKey(url);

        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            System.out.println("test Cache is taken: " + entry);
            return entry;
        }

        if (useFileCache) {
            Path cacheFile = Paths.get(CACHE_DIR, key);
            if (Files.exists(cacheFile)) {
                try {
                    entry = readFromFile(cacheFile);
                    if (entry != null && !entry.isExpired()) {
                        memoryCache.put(key, entry);
                        return entry;
                    }
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    public void put(String url, CacheEntry entry) {
        String key = generateKey(url);
        memoryCache.put(key, entry);
        if (useFileCache) {
            try {
                writeToFile(Paths.get(CACHE_DIR, key), entry);
                System.out.println("put Cache: " + entry);
            } catch (IOException e) {

            }
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
            System.out.println("Key generated");
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return url.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }

    private void writeToFile(Path file, CacheEntry entry) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            oos.writeObject(entry);
        }
    }

    private CacheEntry readFromFile(Path file) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file.toFile()))) {
            return (CacheEntry) ois.readObject();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

} 