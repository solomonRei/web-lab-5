# Go2Web

A command-line tool for making HTTP requests and searching the web, with support for caching and content negotiation.

## Features

- Make HTTP requests to any URL with support for both HTTP and HTTPS
- Search the web with Bing search engine
- Content negotiation (HTML and JSON formats)
- Two-tier caching system (memory and file-based)
- Support for HTTP redirects (up to 5 redirects)
- Clickable links in terminal output
- Human-readable output with proper formatting
- Support for chunked transfer encoding
- Multi-word search terms

## Usage

```bash
go2web -u <URL> [-f <format>]  # make an HTTP request to URL (format: html or json)
go2web -s <search-term>        # search the term and print top 10 results
go2web -h                      # show this help
```

## Implementation Details

The application is implemented in Java with minimal dependencies:
- Raw TCP sockets for HTTP/HTTPS connections
- JSON library for JSON response formatting
- Two-tier caching system:
  - In-memory cache using ConcurrentHashMap
  - File-based cache in ~/.go2web_cache
- Content negotiation with quality values
- ETag and Cache-Control support

## Caching System

The caching mechanism supports:
- In-memory caching for fast access
- File-based persistence for longer-term storage
- Cache validation using ETags
- Automatic cache expiration based on Cache-Control headers
- Automatic cleanup of expired entries

## Content Negotiation

Supports multiple content types:
- HTML (default): `text/html,application/json;q=0.9,*/*;q=0.8`
- JSON: `application/json,text/html;q=0.9,*/*;q=0.8`
- Pretty-printed JSON output for better readability

## How it Works

1. Direct URL requests (`-u`):
   - Checks cache for valid response
   - Establishes TCP connection if needed
   - Handles redirects automatically
   - Supports content negotiation via `-f` flag
   - Caches responses for future requests

2. Web searches (`-s`):
   - Uses Bing search engine
   - Extracts and formats top 10 results
   - Provides clickable links in terminal
   - Supports multi-word search terms

## Example

![Go2Web Demo](demo.gif)

## Requirements

- Java 8 or higher
- Bash shell (for the executable script)
- Internet connection for search functionality
- Write permissions for cache directory (~/.go2web_cache)

## Installation

1. Clone this repository
2. Ensure the `go2web` script is executable:
   ```bash
   chmod +x go2web
   ```
3. Run `./go2web -h` to see the help

## Examples

1. Make an HTTP request in HTML format:
   ```bash
   ./go2web -u https://example.com
   ```

2. Get JSON response from an API:
   ```bash
   ./go2web -u https://api.github.com/users/octocat -f json
   ```

3. Search the web:
   ```bash
   ./go2web -s "Java programming"
   ```

## Cache Location

The file-based cache is stored in `~/.go2web_cache`. You can safely delete this directory to clear the cache. 


./go2web -u https://api.github.com/users/octocat -f json
./go2web -u https://example.com
./go2web -s "Java programming"
./go2web -h