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
        System.out.println("  go2web -s <search-term> # make an HTTP request to search the term using Google and print top 10 results");
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

        try (Socket socket = new Socket(host, port)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.print("GET " + path + " HTTP/1.1\r\n");
            out.print("Host: " + host + "\r\n");
            out.print("Connection: close\r\n");
            out.print("User-Agent: Go2Web/1.0\r\n");
            out.print("\r\n");
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            String htmlResponse = response.toString();
            String cleanResponse = removeHtmlTags(htmlResponse);

            int bodyStart = htmlResponse.indexOf("\r\n\r\n");
            if (bodyStart != -1) {
                cleanResponse = removeHtmlTags(htmlResponse.substring(bodyStart + 4));
            }

            System.out.println(cleanResponse);
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