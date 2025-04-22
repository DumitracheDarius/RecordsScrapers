package scrapers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class YtbScraper {
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

        String resultJson = "";
        try {
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

                    System.out.println("Titlu: " + titleElement.getAttribute("title"));
                    System.out.println("Vizualizări: " + viewsElement.getText());
                    break;
                }
            }

            if (resultJson.isEmpty()) {
                resultJson = "{ \"error\": \"No matching video found for " + songName + " by " + artist + "\" }";
            }

        } catch (Exception e) {
            resultJson = "{ \"error\": \"YouTube scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            driver.quit();
        }

        return resultJson;
    }
}
