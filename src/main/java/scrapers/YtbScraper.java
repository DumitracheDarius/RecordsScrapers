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

public class YtbScraper {

    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        String uniqueProfile;
        try {
            Path tempDir = Files.createTempDirectory("chrome-profile-");
            uniqueProfile = tempDir.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp Chrome profile: " + e.getMessage());
        }


        System.out.println("Chrome version: " + System.getenv("CHROME_BIN"));
        System.out.println("Chromedriver path: " + System.getenv("CHROMEDRIVER_PATH"));
        System.out.println("Profile dir: " + uniqueProfile);

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));

        // 2️⃣ Adaugă toate flagurile corecte
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-sync");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--mute-audio");
        options.addArguments("--no-first-run");
        options.addArguments("--safebrowsing-disable-auto-update");
        options.addArguments("--remote-debugging-port=0");
        options.addArguments("--user-data-dir=" + uniqueProfile);  // ✅ păstrează-l


        WebDriver driver = null;
        String resultJson = "";

        try {
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
                    WebElement viewsElement = video.findElement(By.xpath(".//span[contains(text(), 'de vizionări')]"));

                    resultJson = "{ \"Youtube title\": \"" + titleElement.getAttribute("title") + "\", " +
                            "\"Youtube views\": \"" + viewsElement.getText() + "\" }";
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

            try {
                Files.walk(Paths.get(uniqueProfile))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignored) {
            }


            return resultJson;
        }
    }
}
