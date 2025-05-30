package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpServer;

public class SimpleScraperServer {

    static class ScraperHandler implements HttpHandler {
        static class RequestBody {
            String song_name;
            String artist;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Add CORS headers
                String origin = exchange.getRequestHeaders().getFirst("Origin");
                if (origin != null) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
                } else {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                }
                exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"POST".equals(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                RequestBody body = new Gson().fromJson(reader, RequestBody.class);

                if (body.song_name == null || body.artist == null ||
                        body.song_name.trim().isEmpty() || body.artist.trim().isEmpty()) {
                    String response = "Missing 'song_name' or 'artist' parameter.";
                    exchange.sendResponseHeaders(400, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                    return;
                }

                System.out.println("Scraping for: " + body.song_name + " by " + body.artist);

                ScraperService service = new ScraperService();
                String jsonResponse = service.scrapeAll(body.song_name, body.artist);

                if (jsonResponse == null) {
                    jsonResponse = "{ \"error\": \"Empty response from scraper.\" }";
                }

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String error = "{ \"error\": \"Exception: " + e.toString().replace("\"", "'") + "\", \"trace\": \"" + sw.toString().replace("\"", "'").replace("\n", "\\n") + "\" }";

                exchange.sendResponseHeaders(500, error.length());
                OutputStream os = exchange.getResponseBody();
                os.write(error.getBytes());
                os.close();
            }
        }
    }

    static class CsvDownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String[] params = query.split("&");
            String song = null, artist = null;
            for (String param : params) {
                if (param.startsWith("song=")) {
                    song = URLDecoder.decode(param.split("=")[1], "UTF-8");
                } else if (param.startsWith("artist=")) {
                    artist = URLDecoder.decode(param.split("=")[1], "UTF-8");
                }
            }

            if (song == null || artist == null) {
                String response = "Missing 'song' or 'artist' query parameter.";
                exchange.sendResponseHeaders(400, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }

            String sanitizedSong = song.replaceAll("\\s+", "_");
            String sanitizedArtist = artist.replaceAll("\\s+", "_");

            String[] possibleFiles = {
                    sanitizedSong + "_" + sanitizedArtist + "_tiktok.csv",
                    sanitizedSong + "_" + sanitizedArtist + "_spotontrack.csv"
            };

            Path filePath = null;
            String fileName = null;

            for (String fname : possibleFiles) {
                Path path = Paths.get("images", fname);
                if (Files.exists(path)) {
                    filePath = path;
                    fileName = fname;
                    break;
                }
            }

            if (filePath == null) {
                String response = "CSV file not found.";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "text/csv");
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            byte[] fileBytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, fileBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(fileBytes);
            os.close();

            try {
                Files.delete(filePath);
                System.out.println("CSV deleted: " + filePath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to delete CSV: " + e.getMessage());
            }
        }
    }

    static class ImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String uriPath = exchange.getRequestURI().getPath();
            String filename = uriPath.replace("/images/", "");

            Path imagePath = Paths.get("images", filename);
            if (!Files.exists(imagePath)) {
                String response = "Image not found.";
                exchange.sendResponseHeaders(404, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "image/png");
            byte[] imageBytes = Files.readAllBytes(imagePath);
            exchange.sendResponseHeaders(200, imageBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(imageBytes);
            os.close();
        }
    }
}
