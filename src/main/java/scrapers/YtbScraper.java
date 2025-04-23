package scrapers;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class YtbScraper {

    public static String scrape(String songName, String artist) {
        System.out.println("✅ STARTING scrape for: " + songName + " by " + artist);

        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));
        System.out.println("📍 Set CHROMEDRIVER_PATH: " + System.getenv("CHROMEDRIVER_PATH"));
        System.out.println("📍 Set CHROME_BIN: " + System.getenv("CHROME_BIN"));

        String uniqueProfile;
        try {
            Path tempDir = Files.createTempDirectory("chrome-profile-");
            uniqueProfile = tempDir.toAbsolutePath().toString();
            System.out.println("📁 Created temp profile dir: " + uniqueProfile);
        } catch (IOException e) {
            System.out.println("❌ Failed to create temp profile: " + e.getMessage());
            return "{ \"error\": \"Could not create temp Chrome profile dir.\" }";
        }

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));

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
        options.addArguments("--user-data-dir=" + uniqueProfile);
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = null;
        String resultJson = "";

        try {
            System.out.println("🚀 Attempting to start ChromeDriver...");
            driver = new ChromeDriver(options);
            System.out.println("✅ ChromeDriver started successfully.");

            driver.get("https://google.com");
            System.out.println("🌍 Navigated to google.com");

            String title = driver.getTitle();
            System.out.println("🧠 Page title: " + title);

            resultJson = "{ \"status\": \"success\", \"title\": \"" + title + "\" }";

        } catch (Exception e) {
            System.out.println("❌ Exception during driver start or page load:");
            e.printStackTrace();
            resultJson = "{ \"error\": \"Chrome launch failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("🧹 ChromeDriver quit.");
            }
            try {
                Files.walk(Paths.get(uniqueProfile))
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                System.out.println("🧼 Deleted profile dir: " + uniqueProfile);
            } catch (IOException ioEx) {
                System.out.println("⚠️ Failed to clean up temp dir: " + ioEx.getMessage());
            }
        }

        return resultJson;
    }
}
