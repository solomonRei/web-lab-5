package org.uni.http;

import java.io.Serializable;
import java.time.Instant;

public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String content;
    private final String contentType;
    private final String etag;
    private final long expirationTime;

    public CacheEntry(String content, String contentType, String etag, int maxAgeSeconds) {
        this.content = content;
        this.contentType = contentType;
        this.etag = etag;
        this.expirationTime = Instant.now().plusSeconds(maxAgeSeconds).toEpochMilli();
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEtag() {
        return etag;
    }

    public boolean isExpired() {
        return Instant.now().toEpochMilli() > expirationTime;
    }
    
    @Override
    public String toString() {
        return "CacheEntry{" +
                "contentType='" + contentType + '\'' +
                ", etag='" + etag + '\'' +
                ", expirationTime=" + expirationTime +
                ", expired=" + isExpired() +
                '}';
    }
} 