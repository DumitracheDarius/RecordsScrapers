package scrapers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;

public class ChartexScraper {

    private static String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("[^\\p{ASCII}]", "");
        normalized = normalized.toLowerCase().replaceAll("\\s+", " ").trim();
        return normalized;
    }

    public static String scrape(String song, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-chartex");
        } catch (Exception e) {
            return errorJson("Failed to create temp Chrome profile: " + e.getMessage());
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        String resultJson;

        try {
            System.out.println("🔍 Chartex scrape started for: " + artist + " - " + song);
            driver.get("https://chartex.com");
            Thread.sleep(1500);

            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Search for a sound']")));
            String searchTerm = artist + " " + song;
            searchInput.sendKeys(searchTerm, Keys.ENTER);
            Thread.sleep(3000);

            List<WebElement> songLinks = driver.findElements(By.cssSelector("a.text-black.underline"));
            if (songLinks.isEmpty()) {
                return errorJson("No results found for search.");
            }

            String normalizedSong = normalize(song);
            String normalizedArtist = normalize(artist);

            boolean found = false;
            for (int i = 0; i < songLinks.size(); i++) {
                String linkText = normalize(songLinks.get(i).getText());
                System.out.println("🎯 Checking link: " + linkText);
                if (linkText.contains(normalizedSong) && linkText.contains(normalizedArtist)) {
                    try {
                        driver.findElements(By.cssSelector("a.text-black.underline")).get(i).click();
                        found = true;
                        System.out.println("✅ Found matching link, clicked.");
                        break;
                    } catch (Exception clickErr) {
                        return errorJson("Failed to click matched link: " + clickErr.getMessage());
                    }
                }
            }

            if (!found) {
                try {
                    driver.findElements(By.cssSelector("a.text-black.underline")).get(0).click();
                    System.out.println("⚠️ No exact match, clicked first result.");
                } catch (Exception fallbackErr) {
                    return errorJson("Fallback click failed: " + fallbackErr.getMessage());
                }
            }

            Thread.sleep(3000);

            WebElement statsBox = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.w-full.md\\:w-\\[25vw\\].text-black.text-center.flex.flex-col.justify-center.items-center.border.p-6.rounded-lg")
            ));

            List<WebElement> tableRows = driver.findElements(By.cssSelector("#tiktok-videos tbody tr"));
            if (tableRows.isEmpty()) {
                return errorJson("No TikTok sounds table found.");
            }

            boolean clickedOfficialSound = false;
            for (int i = 0; i < tableRows.size(); i++) {
                WebElement row = driver.findElements(By.cssSelector("#tiktok-sounds tbody tr")).get(i);
                List<WebElement> cells = row.findElements(By.tagName("td"));
                if (cells.size() >= 5) {
                    String fifthCol = cells.get(4).getText().trim();
                    System.out.println("🔎 Row " + i + " - 5th column: " + fifthCol);
                    if ("Official Sound".equalsIgnoreCase(fifthCol)) {
                        try {
                            cells.get(2).click();
                            clickedOfficialSound = true;
                            System.out.println("✅ Clicked 3rd column of Official Sound row.");
                            break;
                        } catch (Exception clickErr) {
                            return errorJson("Failed to click Official Sound cell: " + clickErr.getMessage());
                        }
                    }
                }
            }

            if (!clickedOfficialSound) {
                return errorJson("No row with 'Official Sound' found in table.");
            }

            Thread.sleep(3000);
            WebElement videoStatsTable = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tiktok-videos")));

            List<List<String>> allData = new ArrayList<>();
            List<WebElement> newTableRows = videoStatsTable.findElements(By.cssSelector("tbody tr"));
            for (int i = 0; i < newTableRows.size(); i++) {
                WebElement row = driver.findElements(By.cssSelector("#tiktok-videos tbody tr")).get(i);
                List<WebElement> cells = row.findElements(By.tagName("td"));
                List<String> rowData = new ArrayList<>();
                for (int j = 0; j < cells.size(); j++) {
                    String cellText = cells.get(j).getText().replace("\"", "'").replace("\n", " ").replace("\r", " ");
                    if (j == cells.size() - 1) {
                        WebElement link = cells.get(j).findElement(By.tagName("a"));
                        cellText = link.getAttribute("href");
                    }
                    rowData.add(cellText);
                }
                allData.add(rowData);
            }

            String fileName = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_tiktok.csv";
            Path imagesDir = Paths.get(System.getProperty("user.dir"), "images");
            Files.createDirectories(imagesDir);
            Path csvPath = imagesDir.resolve(fileName);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
                writer.println("Rank;Username;Followers;Country;Date;Views;Likes;Comments;Saves;Shares;Link");
                for (List<String> row : allData) {
                    writer.println(String.join(";", row));
                }
            }

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("chartexStats", statsBox.getText().replace("\"", "'").replace("\n", " ").replace("\r", " "));
            JsonArray rowsArray = new JsonArray();
            for (List<String> row : allData) {
                JsonArray rowArray = new JsonArray();
                for (String cell : row) {
                    rowArray.add(cell);
                }
                rowsArray.add(rowArray);
            }
            responseJson.add("tiktokRows", rowsArray);

            resultJson = new Gson().toJson(responseJson);
            System.out.println("✅ Chartex scrape completed successfully.");

        } catch (Exception e) {
            System.err.println("❌ Chartex scrape failed: " + e.getMessage());
            resultJson = errorJson("Chartex scrape failed: " + e.getMessage());
        } finally {
            driver.quit();
            try {
                Files.walk(tempProfile)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception ignored) {}
        }

        return resultJson;
    }

    private static String errorJson(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return new Gson().toJson(error);
    }
}
