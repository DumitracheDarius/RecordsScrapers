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

        // Încercăm să folosim /dev/shm pentru user-data-dir
        String profileDir = "/dev/shm/chrome-profile-test";
        System.out.println("Testing ChromeDriver with profile dir: " + profileDir);

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-data-dir=" + profileDir);

        try {
            WebDriver driver = new ChromeDriver(options);
            driver.get("https://www.google.com");
            System.out.println("✅ Page title: " + driver.getTitle());
            driver.quit();
        } catch (Exception e) {
            System.out.println("❌ Exception during Chrome sanity test: " + e.getMessage());
            e.printStackTrace();
        }

        return "hey";
        }
    }
