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
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class ChartexScraper {

    private static String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", ""); // eliminăm diacriticele
        normalized = normalized.replaceAll("[^\\p{ASCII}]", ""); // eliminăm orice altceva non-ascii
        normalized = normalized.toLowerCase().replaceAll("\\s+", " ").trim();
        return normalized;
    }

    public static String scrape(String song, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-chartex");
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
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        String resultJson;

        try {
            String searchTerm = artist + " " + song;
            driver.get("https://chartex.com");
            Thread.sleep(1500);

            WebElement searchInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Search for a sound']"))
            );
            searchInput.sendKeys(searchTerm, Keys.ENTER);

            Thread.sleep(2500);

            List<WebElement> allRows = driver.findElements(By.cssSelector("#tiktok-videos tbody tr"));

            if (allRows.isEmpty()) {
                throw new NoSuchElementException("Nicio melodie găsită în rezultate.");
            }

            boolean found = false;
            String normalizedSong = normalize(song);
            String normalizedArtist = normalize(artist);

            for (WebElement row : allRows) {
                List<WebElement> columns = row.findElements(By.tagName("td"));
                if (columns.size() >= 3) {
                    String siteSongTitle = normalize(columns.get(2).getText());
                    if (siteSongTitle.contains(normalizedSong) && siteSongTitle.contains(normalizedArtist)) {
                        WebElement link = columns.get(2).findElement(By.tagName("a"));
                        link.click();
                        found = true;
                        break;
                    }
                }
            }

            if (!found) {
                // fallback: click pe primul rezultat
                WebElement firstLink = allRows.get(0).findElements(By.tagName("td")).get(2).findElement(By.tagName("a"));
                firstLink.click();
            }

            Thread.sleep(2000);

            WebElement statsBox = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div.w-full.md\\:w-\\[25vw\\].text-black.text-center.flex.flex-col.justify-center.items-center.border.p-6.rounded-lg")
                    )
            );

            List<WebElement> tableRows = driver.findElements(By.cssSelector("#tiktok-videos tbody tr"));
            List<List<String>> allData = new ArrayList<>();

            for (WebElement row : tableRows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                List<String> rowData = new ArrayList<>();

                for (int i = 0; i < cells.size(); i++) {
                    String cellText = cells.get(i).getText().replace("\"", "'").replace("\n", " ").replace("\r", " ");
                    if (i == cells.size() - 1) {
                        WebElement link = cells.get(i).findElement(By.tagName("a"));
                        cellText = link.getAttribute("href");
                    }
                    rowData.add(cellText);
                }
                allData.add(rowData);
            }

            // Scriem CSV
            String fileName = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_tiktok.csv";
            Path imagesDir = Paths.get(System.getProperty("user.dir"), "images");
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
            }
            Path csvPath = imagesDir.resolve(fileName);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
                writer.println("Rank;Username;Followers;Country;Date;Views;Likes;Comments;Saves;Shares;Link");
                for (List<String> row : allData) {
                    writer.println(String.join(";", row));
                }
            }

            // Construim JSON
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

        } catch (Exception e) {
            e.printStackTrace();
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", "Chartex scrape failed: " + e.getMessage().replace("\"", "'"));
            resultJson = new Gson().toJson(errorJson);
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
}
