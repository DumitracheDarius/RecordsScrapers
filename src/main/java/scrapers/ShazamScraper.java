package scrapers;

import com.google.gson.Gson;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ShazamScraper {

    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-shazam");
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
        String resultJson;

        try {
            String encodedQuery = URLEncoder.encode(songName + " " + artist, StandardCharsets.UTF_8.toString());
            driver.get("https://www.shazam.com/");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            WebElement searchIcon = wait.until(ExpectedConditions.elementToBeClickable(By.className("Search_icon__Poc_G")));
            searchIcon.click();

            WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(By.className("Search_input__HkJTl")));
            searchInput.sendKeys(encodedQuery);
            Thread.sleep(2000);

            WebElement songResult = wait.until(ExpectedConditions.elementToBeClickable(By.className("Song_songItem__eD9I1")));
            songResult.click();
            Thread.sleep(2000);

            WebElement countEl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("TrackPageHeader_count__UPtDJ")));

            String rawCount = countEl.getText().replace(",", "").replace(".", "").replaceAll("[^\\d]", "");
            long shazamCount = Long.parseLong(rawCount);

            String fileKey = songName.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_shazam.csv";
            Path dataDir = Paths.get(System.getProperty("user.dir"), "data");
            Files.createDirectories(dataDir);
            Path filePath = dataDir.resolve(fileKey);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);

            List<String> allLines = new ArrayList<>();
            if (Files.exists(filePath)) {
                allLines = Files.readAllLines(filePath);
            }

            long lastCount = -1;
            if (!allLines.isEmpty()) {
                String[] lastEntry = allLines.get(allLines.size() - 1).split(",");
                lastCount = Long.parseLong(lastEntry[1]);
            }

            long difference = lastCount >= 0 ? shazamCount - lastCount : 0;

            allLines.add(timestamp + "," + shazamCount);
            Files.write(filePath, allLines, StandardCharsets.UTF_8);

            // Calcul medii și pregătire grafice
            Map<String, List<Long>> dailyMap = new HashMap<>();
            Map<String, Long> dailyAverage = new HashMap<>();
            List<Long> last7daysCounts = new ArrayList<>();
            List<Map<String, Object>> chartData = new ArrayList<>();

            for (String line : allLines) {
                String[] parts = line.split(",");
                String date = parts[0].split(" ")[0];
                long count = Long.parseLong(parts[1]);

                dailyMap.putIfAbsent(date, new ArrayList<>());
                dailyMap.get(date).add(count);

                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", parts[0]);
                point.put("shazamCount", count);
                chartData.add(point);
            }

            for (String date : dailyMap.keySet()) {
                List<Long> countList = dailyMap.get(date);
                long sum = 0;
                for (long v : countList) sum += v;
                dailyAverage.put(date, sum / countList.size());
            }

            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                String day = today.minusDays(i).toString();
                if (dailyAverage.containsKey(day)) {
                    last7daysCounts.add(dailyAverage.get(day));
                }
            }

            long weeklyAverage = last7daysCounts.isEmpty() ? 0 :
                    last7daysCounts.stream().mapToLong(Long::longValue).sum() / last7daysCounts.size();

            resultJson = "{ \"Shazam title\": \"" + songName + "\", " +
                    "\"Shazam count\": \"" + shazamCount + "\", " +
                    "\"Difference since last check\": \"" + difference + "\", " +
                    "\"Daily average (today)\": \"" + dailyAverage.getOrDefault(today.toString(), shazamCount) + "\", " +
                    "\"Weekly average (last 7 days)\": \"" + weeklyAverage + "\", " +
                    "\"Chart data\": " + new Gson().toJson(chartData) + " }";

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Shazam scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            driver.quit();
            try {
                Files.walk(tempProfile)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignored) {}
        }

        return resultJson;
    }
}
