package scrapers;

import classes.SongSelector;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class MediaforestScraper {

    private static Map<String, Object> errorResult(String message) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", message);
        return errorMap;
    }

    public static Map<String, Object> scrape(String song, String artist) {
        final int maxRetries = 3;
        int attempt = 0;
        Map<String, Object> finalResult = null;

        while (attempt < maxRetries) {
            attempt++;
            System.out.println("🔄 Mediaforest scrape attempt " + attempt + "/" + maxRetries);

            Map<String, Object> result = attemptScrape(song, artist);
            if (result.containsKey("error")) {
                System.out.println("⚠ Attempt " + attempt + " failed: " + result.get("error"));
                if (attempt < maxRetries) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
                finalResult = result;  // keep the last error
            } else {
                System.out.println("✅ Mediaforest scrape succeeded on attempt " + attempt);
                return result;
            }
        }

        System.out.println("❌ All attempts failed. Returning last error.");
        return finalResult;
    }

    private static Map<String, Object> attemptScrape(String song, String artist) {
        String username = "stefan.lucian@globalrecords.com";
        String password = "Amprentare123!";

        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-mediaforest");
        } catch (Exception e) {
            return errorResult("Failed to create temp Chrome profile: " + e.getMessage());
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
                "--user-data-dir=" + tempProfile.toAbsolutePath(),
                "--allow-running-insecure-content",
                "--ignore-certificate-errors",
                "--unsafely-treat-insecure-origin-as-secure=http://www.mediaforest.ro"
        );

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Map<String, Object> result = new HashMap<>();

        try {
            driver.get("http://www.mediaforest.ro/Membership/Login.aspx");

            // Accept cookies if popup appears
            try {
                WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
                WebElement acceptCookiesButton = shortWait.until(ExpectedConditions.elementToBeClickable(By.id("btn-cookie-allow")));
                acceptCookiesButton.click();
                Thread.sleep(500);
            } catch (TimeoutException ignored) {}

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ContentPlaceHolder1_Login1_UserName"))).sendKeys(username);
            driver.findElement(By.id("ContentPlaceHolder1_Login1_Password")).sendKeys(password);
            driver.findElement(By.id("ContentPlaceHolder1_Login1_LoginButton")).click();

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='Artist/ArtistAccount.aspx']"))).click();
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='/channel/ArtistsScan.aspx']"))).click();

            WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(By.id("ContentPlaceHolder1_cbSearchArtist")));
            if (checkbox.isSelected()) checkbox.click();

            WebElement artistInput = driver.findElement(By.id("ContentPlaceHolder1_ddlArtists"));
            artistInput.clear();
            artistInput.sendKeys(song);
            Thread.sleep(4000);

            try {
                SongSelector.selectBestMatch(driver, song, artist);
            } catch (Exception selectErr) {
                return errorResult("Failed to select best match artist/song: " + selectErr.getMessage());
            }

            driver.findElement(By.cssSelector("button.ui-multiselect.ui-widget")).click();
            driver.findElement(By.xpath("//a[./span[contains(text(),'Select all')]]")).click();

            WebElement dropdown = driver.findElement(By.id("ContentPlaceHolder1_ddlReportType"));
            dropdown.click();
            WebElement fourthOption = driver.findElement(By.xpath("//select[@id='ContentPlaceHolder1_ddlReportType']/option[4]"));
            fourthOption.click();

            driver.findElement(By.id("ContentPlaceHolder1_btnGenerate")).click();
            Thread.sleep(5000);

            WebElement chartByChannel;
            try {
                chartByChannel = wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("(//ul[contains(@class, 'ui-tabs-nav')]/li)[4]")));
                chartByChannel.click();
            } catch (TimeoutException te) {
                return errorResult("Chart tab did not load or could not be clicked.");
            }

            WebElement chartSection = driver.findElement(By.id("channels_tab"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", chartSection);
            Thread.sleep(1000);

            File screenshot = chartSection.getScreenshotAs(OutputType.FILE);

            Path imageDir = Paths.get(System.getProperty("user.dir"), "images");
            Files.createDirectories(imageDir);

            String fileName = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_mediaforest.png";
            Path dest = imageDir.resolve(fileName);
            Files.copy(screenshot.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            result.put("mediaforest_image_url", "http://localhost:8000/images/" + fileName);

        } catch (Exception e) {
            result = errorResult("Mediaforest scrape failed: " + e.getMessage());
        } finally {
            driver.quit();
            try {
                Files.walk(tempProfile)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception ignored) {}
        }

        return result;
    }
}
