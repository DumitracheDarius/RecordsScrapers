import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebhookSender {
    public static void sendToMake(String jsonData) {
        try {
            URL url = new URL("https://hook.integromat.com/XYZ123"); // ← aici pui URL-ul tău
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Webhook response: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
