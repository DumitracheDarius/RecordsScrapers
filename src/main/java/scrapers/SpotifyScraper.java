package scrapers;

import com.google.gson.Gson;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SpotifyScraper {

    public static String scrape(String songName, String artist) {
        System.setProperty("webdriver.chrome.driver", System.getenv("CHROMEDRIVER_PATH"));

        Path tempProfile;
        try {
            tempProfile = Files.createTempDirectory("chrome-profile-spotify");
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
            driver.get("https://open.spotify.com/search/" + encodedQuery);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            try {
                WebElement cookieButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("onetrust-accept-btn-handler")));
                cookieButton.click();
                Thread.sleep(1000);
            } catch (TimeoutException e) {
                System.out.println("❕ No cookie popup found.");
            }

            WebElement trackLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[data-testid='tracklist-row'] a")));
            trackLink.click();

            WebElement titleEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.className("encore-text-headline-large")));
            WebElement playcountEl = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//span[contains(@data-testid, 'playcount')]")));

            String title = titleEl.getText();
            String rawStreams = playcountEl.getText().replace(",", "").replace(".", "").replaceAll("[^\\d]", "");
            long streamCount = Long.parseLong(rawStreams);

            String fileKey = songName.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_spotify.csv";
            Path dataDir = Paths.get(System.getProperty("user.dir"), "data");
            Files.createDirectories(dataDir);
            Path filePath = dataDir.resolve(fileKey);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);

            List<String> allLines = new ArrayList<>();
            if (Files.exists(filePath)) {
                allLines = Files.readAllLines(filePath);
            }

            long lastStreams = -1;
            if (!allLines.isEmpty()) {
                String[] lastEntry = allLines.get(allLines.size() - 1).split(",");
                lastStreams = Long.parseLong(lastEntry[1]);
            }

            long difference = lastStreams >= 0 ? streamCount - lastStreams : 0;

            allLines.add(timestamp + "," + streamCount);
            Files.write(filePath, allLines, StandardCharsets.UTF_8);

            // Calcul medie zilnică și săptămânală
            Map<String, List<Long>> dailyMap = new HashMap<>();
            Map<String, Long> dailyAverage = new HashMap<>();
            List<Long> last7daysStreams = new ArrayList<>();
            List<Map<String, Object>> chartData = new ArrayList<>();

            for (String line : allLines) {
                String[] parts = line.split(",");
                String date = parts[0].split(" ")[0];
                long streams = Long.parseLong(parts[1]);

                dailyMap.putIfAbsent(date, new ArrayList<>());
                dailyMap.get(date).add(streams);

                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", parts[0]);
                point.put("streams", streams);
                chartData.add(point);
            }

            for (String date : dailyMap.keySet()) {
                List<Long> streamsList = dailyMap.get(date);
                long sum = 0;
                for (long v : streamsList) sum += v;
                dailyAverage.put(date, sum / streamsList.size());
            }

            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                String day = today.minusDays(i).toString();
                if (dailyAverage.containsKey(day)) {
                    last7daysStreams.add(dailyAverage.get(day));
                }
            }

            long weeklyAverage = last7daysStreams.isEmpty() ? 0 :
                    last7daysStreams.stream().mapToLong(Long::longValue).sum() / last7daysStreams.size();

            resultJson = "{ \"Spotify title\": \"" + title + "\", " +
                    "\"Spotify streams\": \"" + streamCount + "\", " +
                    "\"Streams difference since last check\": \"" + difference + "\", " +
                    "\"Daily average (today)\": \"" + dailyAverage.getOrDefault(today.toString(), streamCount) + "\", " +
                    "\"Weekly average (last 7 days)\": \"" + weeklyAverage + "\", " +
                    "\"Chart data\": " + new Gson().toJson(chartData) + " }";

        } catch (Exception e) {
            resultJson = "{ \"error\": \"Spotify scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
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
