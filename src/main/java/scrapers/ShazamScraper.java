package scrapers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ShazamScraper {
    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));

// ⛳️ Cele mai stabile flaguri pentru headless în Docker/Render:
        options.addArguments("--headless=chrome"); // 👈 Nu "new"
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--single-process");
        options.addArguments("--disable-extensions");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-sync");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--mute-audio");
        options.addArguments("--no-first-run");
        options.addArguments("--safebrowsing-disable-auto-update");


        WebDriver driver = new ChromeDriver(options);

        String resultJson;
        try {
            String encodedQuery;
            try {
                encodedQuery = URLEncoder.encode(songName + " " + artist, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding failed: " + e.getMessage());
            }
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
        }

        System.out.println("CHROME_BIN = " + System.getenv("CHROME_BIN"));
        System.out.println("CHROMEDRIVER_PATH = " + System.getenv("CHROMEDRIVER_PATH"));
        System.out.println("PATH = " + System.getenv("PATH"));


        driver.quit();
        return resultJson;
    }
}

