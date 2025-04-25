package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

public class ChartexScraper {
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

            Thread.sleep(2000);

            WebElement firstResult = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//a[contains(text(), '" + song + "')]")
                    )
            );
            firstResult.click();
            Thread.sleep(2000);

            WebElement statsBox = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div.w-full.md\\:w-\\[25vw\\].text-black.text-center.flex.flex-col.justify-center.items-center.border.p-6.rounded-lg")
                    )
            );

            List<WebElement> tableRows = driver.findElements(By.cssSelector("#tiktok-videos tbody tr"));
            List<List<String>> allData = new ArrayList<>();
            StringBuilder rowsJson = new StringBuilder("[");

            for (WebElement row : tableRows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                List<String> rowData = new ArrayList<>();
                rowsJson.append("[");

                for (int i = 0; i < cells.size(); i++) {
                    String cellText = cells.get(i).getText().replace("\"", "'");

                    if (i == cells.size() - 1) {
                        WebElement link = cells.get(i).findElement(By.tagName("a"));
                        cellText = link.getAttribute("href");
                    }

                    rowData.add(cellText);
                    rowsJson.append("\"").append(cellText).append("\"");
                    if (i < cells.size() - 1) rowsJson.append(",");
                }

                allData.add(rowData);
                rowsJson.append("],");
            }
            if (rowsJson.charAt(rowsJson.length() - 1) == ',') {
                rowsJson.setLength(rowsJson.length() - 1);
            }
            rowsJson.append("]");

            // Scrie CSV pe disk
            String fileName = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_tiktok.csv";
            Path csvPath = Paths.get(System.getProperty("user.dir"), fileName);

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
                writer.println("Rank;Username;Followers;Country;Date;Views;Likes;Comments;Saves;Shares;Link");
                for (List<String> row : allData) {
                    writer.println(String.join(";", row));
                }
            }

            resultJson = "{\n" +
                    "  \"chartexStats\": \"" + statsBox.getText().replace("\"", "'").replace("\n", " ") + "\",\n" +
                    "  \"tiktokRows\": " + rowsJson + "\n" +
                    "}";

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Chartex scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
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
