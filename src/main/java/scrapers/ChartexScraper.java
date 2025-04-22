package scrapers;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ChartexScraper {
    public static String scrape(String song, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        String searchTerm = artist + " " + song;
        String resultJson;

        try {
            driver.get("https://chartex.com");
            Thread.sleep(1500);

            WebElement searchInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[placeholder='Search for a sound']"))
            );
            searchInput.sendKeys(searchTerm);
            Thread.sleep(1000);
            searchInput.sendKeys(Keys.ENTER);

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
                    ));

            List<WebElement> tableRows = driver.findElements(By.cssSelector("#tiktok-videos tbody tr"));
            List<List<String>> allData = new ArrayList<>();
            StringBuilder rowsJson = new StringBuilder("[");

            for (WebElement row : tableRows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                List<String> rowData = new ArrayList<>();
                rowsJson.append("[");

                for (int i = 0; i < cells.size(); i++) {
                    String cellText = cells.get(i).getText().replace("\"", "'");

                    // Dacă este ultima coloană, încercăm să luăm href-ul
                    if (i == cells.size() - 1) {
                        WebElement link = cells.get(i).findElement(By.tagName("a"));
                        String href = link.getAttribute("href");
                        cellText = href;
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

            // Scriem CSV pe disk
            String fileName = song.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_tiktok.csv";
            Path csvPath = Paths.get(System.getProperty("user.dir"), fileName);
            System.out.println("CSV saved at: " + csvPath.toAbsolutePath());

            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8))) {
                // Header (opțional, dar recomandat)
                writer.println("Rank;Username;Followers;Country;Date;Views;Likes;Comments;Saves;Shares;Link");
                for (List<String> row : allData) {
                    writer.println(String.join(";", row)); // FOLOSIM `;` ca delimitator
                }
            }


            resultJson = "{\n" +
                    "  \"chartexStats\": \"" + statsBox.getText().replace("\"", "'").replace("\n", " ") + "\",\n" +
                    "  \"tiktokRows\": " + rowsJson.toString() + "\n" +
                    "}";

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Chartex scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        }

        driver.quit();
        return resultJson;
    }
}
