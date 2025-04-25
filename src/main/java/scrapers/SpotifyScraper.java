package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;

public class SpotifyScraper {
    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        // 🟡 Creează un profil temporar unic pentru această instanță de Chrome
        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-spotify");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp Chrome profile: " + e.getMessage());
        }

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));
        options.addArguments(
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-software-rasterizer",
                "--window-size=1920,1080",
                "--user-data-dir=" + tempProfile.toAbsolutePath()
        );

        WebDriver driver = new ChromeDriver(options);

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(songName + " " + artist, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding failed: " + e.getMessage());
        }

        String resultJson;
        try {
            driver.get("https://open.spotify.com/search/" + encodedQuery);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            try {
                WebElement cookieButton = wait.until(
                        ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler"))
                );
                cookieButton.click();
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("❕ Cookie banner not found or already closed.");
            }

            WebElement trackLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[data-testid='tracklist-row'] a")));
            trackLink.click();

            WebElement titleEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.className("encore-text-headline-large")));
            WebElement playcountEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(@data-testid, 'playcount')]")));

            resultJson = "{ \"Spotify title\": \"" + titleEl.getText() + "\", \"Spotify streams\": \"" + playcountEl.getText() + "\" }";

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Spotify scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            driver.quit();

            // Șterge profilul temporar
            try {
                Files.walk(tempProfile)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception ignored) {}
        }

        return resultJson;
    }
}
