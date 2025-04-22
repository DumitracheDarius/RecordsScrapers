package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpotontrackScraper {
    public static Map<String, Object> scrape(String song, String artist) {
        String username = "corina.vilcu@globalrecords.com";
        String password = "Romanica123^$$!";

        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        ChromeOptions options = new ChromeOptions();
        options.setBinary(System.getenv("CHROME_BIN"));

// ⛳️ Cele mai stabile flaguri pentru headless în Docker/Render:
        options.addArguments("--headless"); // 👈 Nu "new"
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

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        Map<String, Object> result = new HashMap<>();

        try {
            driver.get("https://www.spotontrack.com/login");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys(username);
            driver.findElement(By.id("password")).sendKeys(password);
            driver.findElement(By.xpath("//button[contains(text(),'Login')]")).click();

            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[aria-controls='search-modal']"))).click();

            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modal-search")));
            searchInput.sendKeys(artist);
            Thread.sleep(5000);

            wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.cssSelector("a.flex.items-center.p-2.text-gray-800")));
            List<WebElement> results = driver.findElements(By.cssSelector("a.flex.items-center.p-2.text-gray-800"));
            String normalizedArtist = normalize(artist) + "artist";
            WebElement exactMatch = null;

            System.out.println("🔍 Searching for exact match: " + normalizedArtist);
            for (WebElement resultEl : results) {
                String resultTextRaw = resultEl.getText();
                String resultText = normalize(resultTextRaw);
                System.out.println("👉 Option: '" + resultTextRaw + "' normalized: '" + resultText + "'");

                if (resultText.equals(normalizedArtist)) {
                    exactMatch = resultEl;
                    break;
                }
            }

            if (exactMatch != null) {
                exactMatch.click();
            } else {
                throw new RuntimeException("No exact artist match found on Spotontrack.");
            }

            wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Tracks"))).click();
            Thread.sleep(2000);

            WebElement trackSearchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='search'][placeholder='Filter...']")));
            trackSearchInput.sendKeys(song);
            Thread.sleep(2000);

            List<WebElement> trackResults = driver.findElements(By.cssSelector("div.flex.gap-x-4 h1.text-gray-900.font-bold a"));
            WebElement matchedTrack = null;
            for (WebElement el : trackResults) {
                if (normalize(el.getText()).equals(normalize(song))) {
                    matchedTrack = el;
                    break;
                }
            }

            if (matchedTrack != null) {
                matchedTrack.click();
            } else {
                throw new RuntimeException("No matching track found in artist's list.");
            }

            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href*='/playlists'][class*='items-center']"))).click();
            Thread.sleep(10000);

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Current playlists')]"))).click();
            Thread.sleep(10000);

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//th[contains(.,'Followers')]"))).click();
            Thread.sleep(10000);


            // Spotify area screenshot (specific table section)
            WebElement spotifyTable = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("div.flex.items-center.flex-col.justify-center.py-2.w-full")
            ));
            File spotifyShot = spotifyTable.getScreenshotAs(OutputType.FILE);

// Creează folderul 'images' dacă nu există
            Path imageDir = Paths.get(System.getProperty("user.dir"), "images");
            if (!Files.exists(imageDir)) {
                Files.createDirectories(imageDir);
            }

// Normalizează numele fișierului cu underscore (_) — așa va fi ușor de accesat și previzibil
            String filename = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_spotontrack_spotify.png";
            Path spotifyDest = imageDir.resolve(filename);

// Șterge dacă deja există
            if (Files.exists(spotifyDest)) {
                Files.delete(spotifyDest);
            }

// Copiază screenshotul în destinație
            Files.copy(spotifyShot.toPath(), spotifyDest);

// Adaugă în rezultat URL-ul de acces
            result.put("spotontrack_spotify_image", "http://localhost:8000/images/" + filename);



            System.out.println("CHROME_BIN = " + System.getenv("CHROME_BIN"));
            System.out.println("CHROMEDRIVER_PATH = " + System.getenv("CHROMEDRIVER_PATH"));
            System.out.println("PATH = " + System.getenv("PATH"));


        } catch (Exception e) {
            result.put("error", "Spotontrack Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
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
