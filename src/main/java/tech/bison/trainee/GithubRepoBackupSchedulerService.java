package tech.bison.trainee;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class GithubRepoBackupSchedulerService {
	private static final String TOKEN = System.getenv("TOKEN");

	@Scheduled(fixedRate = 50000)
	public void backupGitHub() {
		try {
			for (String repoUrl : findAllRepoUrls()) {
				System.out.println(repoUrl);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// TODO: API only returns first 30 repo URLs
	private List<String> findAllRepoUrls() throws IOException {
		String apiUrl = "https://api.github.com/user/repos";
		URL url = new URL(apiUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Authorization", "token " + TOKEN);
		conn.connect();
		int responseCode = conn.getResponseCode();
		if (responseCode != 200) {
			throw new RuntimeException("HttpResponseCode: " + responseCode);
		}
		Reader reader = new InputStreamReader(conn.getInputStream());
		JsonElement jsonElement = JsonParser.parseReader(reader);
		JsonArray reposArray = jsonElement.getAsJsonArray();
		List<String> repoUrls = new ArrayList<>();
		for (JsonElement repoElement : reposArray) {
			JsonObject repoObject = repoElement.getAsJsonObject();
			String repoUrl = repoObject.get("html_url").getAsString();
			repoUrls.add(repoUrl);
		}
		return repoUrls;
	}
}
