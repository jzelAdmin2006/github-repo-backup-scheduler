package tech.bison.trainee;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class GithubRepoBackupSchedulerService {
  private static final String GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
  private static final String BACKUPPER_TOKEN = System.getenv("BACKUPPER_TOKEN");
  private static final String ENDPOINT = System.getenv("ENDPOINT");

  @Scheduled(fixedRate = 3600000)
  public void backupGitHub() {
    try {
      String repoUrls = findAllRepoUrls().stream().map(url -> url + ".git").collect(Collectors.joining("\n"));

      URL url = new URL(ENDPOINT);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("token", BACKUPPER_TOKEN);
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setDoOutput(true);

      try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        wr.writeBytes("repoUrls=" + URLEncoder.encode(repoUrls, "UTF-8"));
        wr.flush();
      }

      int responseCode = conn.getResponseCode();
      System.out.println("Response code: " + responseCode);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private List<String> findAllRepoUrls() throws IOException {
    int page = 1;
    List<String> repoUrls = new ArrayList<>();
    boolean hasMorePages;
    do {
      String apiUrl = "https://api.github.com/user/repos?page=" + page;
      URL url = new URL(apiUrl);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);
      conn.connect();
      int responseCode = conn.getResponseCode();
      if (responseCode != 200) {
        throw new RuntimeException("HttpResponseCode: " + responseCode);
      }
      Reader reader = new InputStreamReader(conn.getInputStream());
      JsonElement jsonElement = JsonParser.parseReader(reader);
      JsonArray reposArray = jsonElement.getAsJsonArray();
      hasMorePages = reposArray.size() > 0;
      for (JsonElement repoElement : reposArray) {
        JsonObject repoObject = repoElement.getAsJsonObject();
        String repoUrl = repoObject.get("html_url").getAsString();
        repoUrls.add(repoUrl);
      }
      page++;
    } while (hasMorePages);
    return repoUrls;
  }
}
