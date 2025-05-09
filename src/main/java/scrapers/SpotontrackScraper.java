package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;

public class SpotontrackScraper {

    public static Map<String, Object> scrape(String song, String artist) {
        final int maxRetries = 3;
        int attempt = 0;
        Map<String, Object> finalResult = null;

        while (attempt < maxRetries) {
            attempt++;
            System.out.println("🔄 Spotontrack scrape attempt " + attempt + "/" + maxRetries);

            Map<String, Object> result = attemptScrape(song, artist);
            if (result.containsKey("error")) {
                System.out.println("⚠ Attempt " + attempt + " failed: " + result.get("error"));
                if (attempt < maxRetries) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
                finalResult = result;
            } else {
                System.out.println("✅ Spotontrack scrape succeeded on attempt " + attempt);
                return result;
            }
        }

        System.out.println("❌ All attempts failed. Returning last error.");
        return finalResult;
    }

    private static Map<String, Object> attemptScrape(String song, String artist) {
        Map<String, Object> result = new HashMap<>();
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-spotontrack");
        } catch (Exception e) {
            result.put("error", "Failed to create temp Chrome profile: " + e.getMessage());
            return result;
        }

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));
        options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1920,1080",
                "--user-data-dir=" + tempProfile.toAbsolutePath()
        );

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));

        try {
            driver.get("https://www.spotontrack.com/login");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys("corina.vilcu@globalrecords.com");
            driver.findElement(By.id("password")).sendKeys("Romanica123^$$!");
            driver.findElement(By.xpath("//button[contains(text(),'Login')]")).click();

            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[aria-controls='search-modal']"))).click();
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modal-search")));
            searchInput.sendKeys(artist);
            Thread.sleep(3000);

            List<WebElement> results = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.cssSelector("a.flex.items-center.p-2.text-gray-800")));
            String normalizedArtist = normalize(artist) + "artist";
            WebElement exactMatch = results.stream()
                    .filter(el -> normalize(el.getText()).equals(normalizedArtist))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No exact artist match found on Spotontrack."));

            exactMatch.click();
            wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Tracks"))).click();

            WebElement trackSearchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[type='search'][placeholder='Filter...']")));
            trackSearchInput.sendKeys(song);
            Thread.sleep(2000);

            List<WebElement> trackResults = driver.findElements(By.cssSelector("div.flex.gap-x-4 h1.text-gray-900.font-bold a"));
            WebElement matchedTrack = trackResults.stream()
                    .filter(el -> normalize(el.getText()).equals(normalize(song)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No matching track found in artist's list."));

            matchedTrack.click();
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='/playlists'][class*='items-center']"))).click();
            Thread.sleep(5000);
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Current playlists')]"))).click();
            Thread.sleep(5000);
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//th[contains(.,'Followers')]"))).click();
            Thread.sleep(3000);

            WebElement spotifyTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.flex.items-center.flex-col.justify-center.py-2.w-full")));
            File spotifyShot = spotifyTable.getScreenshotAs(OutputType.FILE);

            Path imageDir = Paths.get("images");
            if (!Files.exists(imageDir)) Files.createDirectories(imageDir);

            String filename = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_spotontrack_spotify.png";
            Path destination = imageDir.resolve(filename);
            if (Files.exists(destination)) Files.delete(destination);

            Files.copy(spotifyShot.toPath(), destination);
            result.put("spotontrack_spotify_image", "http://localhost:8000/images/" + filename);

        } catch (Exception e) {
            result.put("error", "Spotontrack scrape failed: " + e.getMessage());
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

    private static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .toLowerCase()
                .trim();
    }
}
