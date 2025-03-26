package org.uni.http;

import java.io.Serializable;
import java.time.Instant;

public class CacheEntry implements Serializable {
    private final String content;
    private final String contentType;
    private final Instant expirationTime;
    private final String etag;

    public CacheEntry(String content, String contentType, Instant expirationTime, String etag) {
        this.content = content;
        this.contentType = contentType;
        this.expirationTime = expirationTime;
        this.etag = etag;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getExpirationTime() {
        return expirationTime;
    }

    public String getEtag() {
        return etag;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }
} 