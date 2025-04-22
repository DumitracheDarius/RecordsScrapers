package classes;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.Normalizer;
import java.time.Duration;
import java.util.*;

public class SongSelector {

    public static void selectBestMatch(WebDriver driver, String song, String artist) {
        String songNorm = normalize(song);
        String artistNorm = normalize(artist);
        System.out.println("🔍 Target comparare: " + songNorm + " + " + artistNorm);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("ul.ui-autocomplete li.ui-menu-item")));

        List<WebElement> listItems = driver.findElements(By.cssSelector("ul.ui-autocomplete li.ui-menu-item"));

        String bestText = null;
        double bestScore = 0;

        for (WebElement li : listItems) {
            WebElement anchor = li.findElement(By.tagName("a"));
            String optionRaw = anchor.getText();
            String normalizedOption = normalize(optionRaw);

            double score = smartScore(normalizedOption, songNorm, artistNorm);
            System.out.println("🎯 Compar cu: '" + optionRaw + "' => Scor: " + score);

            if (score > bestScore) {
                bestScore = score;
                bestText = optionRaw;
            }
        }

        if (bestText != null && bestScore > 0.4) {
            System.out.println("✅ Cel mai bun match: '" + bestText + "' (scor: " + bestScore + ")");
            try {
                WebElement finalClick = driver.findElement(By.xpath("//a[normalize-space(text())=\"" + bestText.trim() + "\"]"));
                finalClick.click();
            } catch (Exception e) {
                System.err.println("❌ Nu am putut da click pe: '" + bestText + "'");
                throw new RuntimeException("Match găsit dar nu am putut face click.");
            }
        } else {
            System.out.println("❌ Niciun match suficient de bun. Cel mai mare scor: " + bestScore);
            throw new RuntimeException("Piesa nu a fost gasita");
        }
    }

    private static String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .toLowerCase()
                .trim();
    }

    /**
     * Calculează scor pe baza prezenței termenilor din song și artist.
     * - Punctaj mai mare pentru potriviri în song decât în artist.
     */
    private static double smartScore(String candidate, String song, String artist) {
        Set<String> tokens = new HashSet<>(Arrays.asList(candidate.split(" ")));
        String[] songTokens = song.split(" ");
        String[] artistTokens = artist.split(" ");

        double score = 0;

        for (String token : songTokens) {
            if (tokens.contains(token)) score += 1.0;
        }

        for (String token : artistTokens) {
            if (tokens.contains(token)) score += 0.5;
        }

        // Normalizează scorul în funcție de lungimea totală (dar nu foarte agresiv)
        return score / (songTokens.length + artistTokens.length * 0.5);
    }
}
