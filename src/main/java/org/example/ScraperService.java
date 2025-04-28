package org.example;

import com.google.gson.Gson;
import scrapers.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class ScraperService {
    public String scrapeAll(String song, String artist) {
        Gson gson = new Gson();

        String youtube = "";
        String spotify = "";
        String shazam = "";
        String chartex = "";
        String mediaforest = "";
        String spotontrack = "";

        youtube = safeScrape(() -> YtbScraper.scrape(song, artist), "YTB");
        spotify = safeScrape(() -> SpotifyScraper.scrape(song, artist), "Spotify");
        shazam = safeScrape(() -> ShazamScraper.scrape(song, artist), "Shazam");
        chartex = safeScrape(() -> ChartexScraper.scrape(song, artist), "Chartex");

        // Decomentează dacă activezi Mediaforest
         mediaforest = safeScrape(() -> gson.toJson(MediaforestScraper.scrape(song, artist)), "Mediaforest");

        spotontrack = safeScrape(() -> gson.toJson(SpotontrackScraper.scrape(song, artist)), "Spotontrack");

        return "{\n" +
                "  \"song\": \"" + song + "\",\n" +
                "  \"artist\": \"" + artist + "\",\n" +
                "  \"youtube\": " + youtube + ",\n" +
                "  \"spotify\": " + spotify + ",\n" +
                "  \"shazam\": " + shazam + ",\n" +
                "  \"chartex\": " + chartex + ",\n" +
                "  \"mediaforest\": " + mediaforest + ",\n" +
                "  \"spotontrack\": " + spotontrack + "\n" +
                "}";
    }

    // Util funcțional pentru a trata erorile în mod sigur
    private String safeScrape(ScrapeFunction scrape, String platform) {
        try {
            return scrape.scrape();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString().replace("\"", "'").replace("\n", "\\n");
            return "{ \"error\": \"" + platform + " Exception\", \"trace\": \"" + trace + "\" }";
        }
    }

    // Functional interface
    @FunctionalInterface
    interface ScrapeFunction {
        String scrape() throws Exception;
    }
}
