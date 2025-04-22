package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SpotifyScraper {
    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(songName + " " + artist, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding failed: " + e.getMessage());
        }
        String resultJson;

        try {
            driver.get("https://open.spotify.com/search/" + encodedQuery);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                WebElement cookieButton = wait.until(
                        ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler"))
                );
                cookieButton.click();
                Thread.sleep(1000); // așteaptă puțin după click
            } catch (Exception e) {
                System.out.println("❕ Cookie banner not found or already closed.");
            }


            // Click pe primul rezultat din lista track-urilor
            WebElement trackLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[data-testid='tracklist-row'] a")));
            trackLink.click();

            // Așteaptă titlul și playcount-ul
            WebElement titleEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.className("encore-text-headline-large")));
            WebElement playcountEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(@data-testid, 'playcount')]")));

            resultJson = "{ \"Spotify title\": \"" + titleEl.getText() + "\", \"Spotify streams\": \"" + playcountEl.getText() + "\" }";
            System.out.println("Spotify: " + titleEl.getText() + " / " + playcountEl.getText());

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Spotify scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        }

        driver.quit();
        return resultJson;
    }
}

