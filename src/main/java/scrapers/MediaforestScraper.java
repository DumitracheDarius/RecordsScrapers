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

    public static Map<String, Object> scrape(String song, String artist) {
        String username = "stefan.lucian@globalrecords.com";
        String password = "Amprentare123!";

        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-mediaforest");
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Map<String, Object> result = new HashMap<>();

        try {
            driver.get("http://www.mediaforest.ro/Membership/Login.aspx");

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ContentPlaceHolder1_Login1_UserName"))).sendKeys(username);
            Thread.sleep(500);
            driver.findElement(By.id("ContentPlaceHolder1_Login1_Password")).sendKeys(password);
            Thread.sleep(500);
            driver.findElement(By.id("ContentPlaceHolder1_Login1_LoginButton")).click();

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='Artist/ArtistAccount.aspx']"))).click();
            Thread.sleep(1000);

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[@href='/channel/ArtistsScan.aspx']"))).click();
            Thread.sleep(1000);

            WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(By.id("ContentPlaceHolder1_cbSearchArtist")));
            if (checkbox.isSelected()) checkbox.click();
            Thread.sleep(500);

            WebElement artistInput = driver.findElement(By.id("ContentPlaceHolder1_ddlArtists"));
            artistInput.clear();
            artistInput.sendKeys(song);
            Thread.sleep(4000);

            SongSelector.selectBestMatch(driver, song, artist);
            Thread.sleep(1000);

            driver.findElement(By.cssSelector("button.ui-multiselect.ui-widget")).click();
            Thread.sleep(500);
            driver.findElement(By.xpath("//a[./span[contains(text(),'Select all')]]")).click();
            Thread.sleep(1000);

            WebElement dropdown = driver.findElement(By.id("ContentPlaceHolder1_ddlReportType"));
            dropdown.click();
            Thread.sleep(500);
            WebElement fourthOption = driver.findElement(By.xpath("//select[@id='ContentPlaceHolder1_ddlReportType']/option[4]"));
            fourthOption.click();
            Thread.sleep(1000);

            driver.findElement(By.id("ContentPlaceHolder1_btnGenerate")).click();
            Thread.sleep(5000);

            WebElement chartByChannel = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//ul[contains(@class, 'ui-tabs-nav')]/li)[4]")));
            chartByChannel.click();
            Thread.sleep(3000);

            WebElement chartSection = driver.findElement(By.id("channels_tab"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", chartSection);
            Thread.sleep(1000);

            File screenshot = chartSection.getScreenshotAs(OutputType.FILE);

            Path imageDir = Paths.get(System.getProperty("user.dir"), "images");
            if (!Files.exists(imageDir)) Files.createDirectories(imageDir);

            String fileName = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_mediaforest.png";
            Path dest = imageDir.resolve(fileName);
            Files.copy(screenshot.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            result.put("mediaforest_image_url", "http://localhost:8000/images/" + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "Mediaforest scrape failed: " + e.getMessage());
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
