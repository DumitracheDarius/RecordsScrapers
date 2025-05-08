package scrapers;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class YtbScraper {

    private static long parseViews(String viewsText) throws ParseException {
        viewsText = viewsText.toLowerCase().replace(" views", "").trim();
        if (viewsText.endsWith("m")) {
            return (long) (Double.parseDouble(viewsText.replace("m", "")) * 1_000_000);
        } else if (viewsText.endsWith("k")) {
            return (long) (Double.parseDouble(viewsText.replace("k", "")) * 1_000);
        } else {
            return NumberFormat.getNumberInstance(Locale.US).parse(viewsText).longValue();
        }
    }

    public static String scrape(String songName, String artist) {
        WebDriver driver = null;
        String resultJson = "";

        Path userDataDir = null;
        try {
            userDataDir = Files.createTempDirectory("chrome-user-data-");

            ChromeOptions options = new ChromeOptions();
            options.setBinary(System.getenv("CHROME_BIN"));
            options.addArguments(
                    "--headless=new",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--remote-allow-origins=*",
                    "--user-data-dir=" + userDataDir.toAbsolutePath()
            );

            driver = new ChromeDriver(options);

            String encodedQuery = URLEncoder.encode(songName + " " + artist, StandardCharsets.UTF_8.toString());
            driver.get("https://www.youtube.com/results?search_query=" + encodedQuery);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ytd-video-renderer")));

            List<WebElement> videos = driver.findElements(By.cssSelector("ytd-video-renderer"));
            boolean found = false;

            for (WebElement video : videos) {
                WebElement titleElement = video.findElement(By.id("video-title"));
                String titleText = titleElement.getAttribute("title").toLowerCase();

                if (titleText.contains(songName.toLowerCase()) && titleText.contains(artist.toLowerCase())) {
                    WebElement metadata = video.findElement(By.id("metadata-line"));
                    List<WebElement> spans = metadata.findElements(By.tagName("span"));
                    String viewsText = spans.get(0).getText();

                    long currentViews = parseViews(viewsText);

                    String fileKey = songName.replaceAll("\\s+", "_") + "_" + artist.replaceAll("\\s+", "_") + "_yt.csv";
                    Path dataDir = Paths.get(System.getProperty("user.dir"), "data");
                    Files.createDirectories(dataDir);
                    Path filePath = dataDir.resolve(fileKey);

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String timestamp = LocalDateTime.now().format(formatter);

                    List<String> allLines = new ArrayList<>();
                    if (Files.exists(filePath)) {
                        allLines = Files.readAllLines(filePath);
                    }

                    long lastViews = -1;
                    if (!allLines.isEmpty()) {
                        String[] lastEntry = allLines.get(allLines.size() - 1).split(",");
                        lastViews = Long.parseLong(lastEntry[1]);
                    }

                    long difference = lastViews >= 0 ? currentViews - lastViews : 0;

                    allLines.add(timestamp + "," + currentViews);
                    Files.write(filePath, allLines, StandardCharsets.UTF_8);

                    // Calcul medie zilnică și săptămânală
                    Map<String, List<Long>> dailyMap = new HashMap<>();
                    Map<String, Long> dailyAverage = new HashMap<>();
                    List<Long> last7daysViews = new ArrayList<>();

                    for (String line : allLines) {
                        String[] parts = line.split(",");
                        String date = parts[0].split(" ")[0];
                        long views = Long.parseLong(parts[1]);

                        dailyMap.putIfAbsent(date, new ArrayList<>());
                        dailyMap.get(date).add(views);
                    }

                    for (String date : dailyMap.keySet()) {
                        List<Long> viewsList = dailyMap.get(date);
                        long sum = 0;
                        for (long v : viewsList) sum += v;
                        dailyAverage.put(date, sum / viewsList.size());
                    }

                    LocalDate today = LocalDate.now();
                    for (int i = 0; i < 7; i++) {
                        String day = today.minusDays(i).toString();
                        if (dailyAverage.containsKey(day)) {
                            last7daysViews.add(dailyAverage.get(day));
                        }
                    }

                    long weeklyAverage = last7daysViews.isEmpty() ? 0 :
                            last7daysViews.stream().mapToLong(Long::longValue).sum() / last7daysViews.size();

                    resultJson = "{ \"Youtube title\": \"" + titleElement.getAttribute("title") + "\", " +
                            "\"Youtube views\": \"" + viewsText + "\", " +
                            "\"Views difference since last check\": \"" + difference + "\", " +
                            "\"Daily average (today)\": \"" + dailyAverage.getOrDefault(today.toString(), currentViews) + "\", " +
                            "\"Weekly average (last 7 days)\": \"" + weeklyAverage + "\" }";

                    found = true;
                    break;
                }
            }

            if (!found) {
                resultJson = "{ \"error\": \"No matching video found for " + songName + " by " + artist + "\" }";
            }

        } catch (Exception e) {
            resultJson = "{ \"error\": \"YouTube scrape failed: " + e.getMessage().replace("\"", "'") + "\" }";
        } finally {
            if (driver != null) {
                driver.quit();
            }

            if (userDataDir != null) {
                try {
                    Files.walk(userDataDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException ignored) {}
            }
        }

        return resultJson;
    }
}
