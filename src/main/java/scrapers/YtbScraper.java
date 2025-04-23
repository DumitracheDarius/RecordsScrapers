package scrapers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class YtbScraper {

    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));

        // Flaguri minimaliste
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        String resultJson = "";
        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);
            driver.get("https://www.google.com");

            String title = driver.getTitle();
            resultJson = "{ \"test\": \"Chrome launched successfully\", \"pageTitle\": \"" + title + "\" }";

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Chrome launch failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return resultJson;
    }
}
