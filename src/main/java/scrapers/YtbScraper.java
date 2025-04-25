package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class YtbScraper {
    public static String scrape(String songName, String artist) {
        WebDriver driver = null;
        String resultJson = "";

        Path userDataDir = null;
        try {
            // Creează un director temporar unic
            userDataDir = Files.createTempDirectory("chrome-user-data-");

            ChromeOptions options = new ChromeOptions();
            options.setBinary(System.getenv("CHROME_BIN"));
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--remote-allow-origins=*",
                    "--user-data-dir=" + userDataDir.toAbsolutePath()
            );

            driver = new ChromeDriver(options);

            String encodedQuery = URLEncoder.encode(songName + " " + artist, StandardCharsets.UTF_8.toString());
            driver.get("https://www.youtube.com/results?search_query=" + encodedQuery);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ytd-video-renderer")));

            List<WebElement> videos = driver.findElements(By.cssSelector("ytd-video-renderer"));

            for (WebElement video : videos) {
                WebElement titleElement = video.findElement(By.id("video-title"));
                String titleText = titleElement.getAttribute("title").toLowerCase();

                if (titleText.contains(songName.toLowerCase()) && titleText.contains(artist.toLowerCase())) {
                    WebElement metadata = video.findElement(By.id("metadata-line"));
                    List<WebElement> spans = metadata.findElements(By.tagName("span"));
                    String viewsText = spans.get(0).getText(); // de obicei e primul span

                    resultJson = "{ \"Youtube title\": \"" + titleElement.getAttribute("title") + "\", " +
                            "\"Youtube views\": \"" + viewsText + "\" }";
                    break;
                }
            }

            if (resultJson.isEmpty()) {
                resultJson = "{ \"error\": \"No matching video found for " + songName + " by " + artist + "\" }";
            }

        } catch (Exception e) {
            resultJson = "{ \"error\": \"YouTube scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            if (driver != null) {
                driver.quit();
            }

            if (userDataDir != null) {
                try {
                    Files.walk(userDataDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException ignored) {}
            }
        }

        return resultJson;
    }
}

