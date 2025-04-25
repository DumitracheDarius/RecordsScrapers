package scrapers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;

public class ShazamScraper {
    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-shazam");
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

        String resultJson;
        try {
            String encodedQuery = URLEncoder.encode(songName + " " + artist, StandardCharsets.UTF_8.toString());
            driver.get("https://www.shazam.com/");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement searchIcon = wait.until(ExpectedConditions.elementToBeClickable(By.className("Search_icon__Poc_G")));
            searchIcon.click();

            WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(By.className("Search_input__HkJTl")));
            searchInput.sendKeys(encodedQuery);
            Thread.sleep(2000);

            WebElement songResult = wait.until(ExpectedConditions.elementToBeClickable(By.className("Song_songItem__eD9I1")));
            songResult.click();
            Thread.sleep(2000);

            WebElement countEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("TrackPageHeader_count__UPtDJ")));

            resultJson = "{ \"Shazam count\": \"" + countEl.getText() + "\" }";
            System.out.println("Shazam: " + countEl.getText());

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Shazam scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            driver.quit();

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
