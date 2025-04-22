import com.google.gson.Gson;
import scrapers.ChartexScraper;
import scrapers.ShazamScraper;
import scrapers.SpotifyScraper;
import scrapers.YtbScraper;
import scrapers.MediaforestScraper;
import scrapers.SpotontrackScraper;

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


        try {
            youtube = YtbScraper.scrape(song, artist);
        } catch (Exception e) {
            youtube = "{ \"error\": \"YTB Exception: " + e.getMessage().replace("\"", "'") + "\" }";
        }

        try {
            spotify = SpotifyScraper.scrape(song, artist);
        } catch (Exception e) {
            spotify = "{ \"error\": \"Spotify Exception: " + e.getMessage().replace("\"", "'") + "\" }";
        }

        try {
            shazam = ShazamScraper.scrape(song, artist);
        } catch (Exception e) {
            shazam = "{ \"error\": \"Shazam Exception: " + e.getMessage().replace("\"", "'") + "\" }";
        }


        try {
            chartex = ChartexScraper.scrape(song, artist);
        } catch (Exception e) {
            chartex = "{ \"error\": \"Chartex Exception: " + e.getMessage().replace("\"", "'") + "\" }";
        }

//        try {
//            Map<String, Object> mediaforestMap = MediaforestScraper.scrape(song, artist);
//            mediaforest = gson.toJson(mediaforestMap);
//        } catch (Exception e) {
//            mediaforest = "{ \"error\": \"Mediaforest Exception: " + e.getMessage().replace("\"", "'") + "\" }";
//        }

        try {
            Map<String, Object> spotontrackMap = SpotontrackScraper.scrape(song, artist);
            spotontrack = gson.toJson(spotontrackMap);
        } catch (Exception e) {
            spotontrack = "{ \"error\": \"Spotontrack Exception: " + e.getMessage().replace("\"", "'") + "\" }";
        }

        return "{\n" +
                "  \"song\": \"" + song + "\",\n" +
                "  \"artist\": \"" + artist + "\",\n" +
                "  \"youtube\": " + youtube + ",\n" +
                "  \"spotify\": " + spotify + ",\n" +
                "  \"shazam\": " + shazam + ",\n" +
                "  \"chartex\": " + chartex + ",\n" +
//                "  \"mediaforest\": " + mediaforest + ",\n" +
                "  \"spotontrack\": " + spotontrack + "\n" +
                "}";
    }
}
