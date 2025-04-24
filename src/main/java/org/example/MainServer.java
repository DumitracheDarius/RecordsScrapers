package org.example;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;


public class MainServer {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // înregistrăm ambele endpointuri
        server.createContext("/scrape", new SimpleScraperServer.ScraperHandler());
        server.createContext("/download", new SimpleScraperServer.CsvDownloadHandler());
        server.createContext("/images", new SimpleScraperServer.ImageHandler());

        server.setExecutor(null); // default
        server.start();

        System.out.println("Server started on port " + port + "...");
    }
}
