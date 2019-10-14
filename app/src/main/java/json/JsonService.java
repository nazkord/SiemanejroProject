package json;

import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import model.AllMatches;

public class JsonService<T> { //Some net stuff.
    private String TOKEN =  "7d273acc9c9548c9885398f85780fe2e";

    /*public AllMatches getObjectFromJson(String urlTarget, Class<T> myClass) throws ExecutionException, InterruptedException {
        Gson gson = new Gson();
        String json;
        AllMatches result;

        json = getJsonStringByUrl(urlTarget);
        result = gson.fromJson(json, myClass);

        return result;
    }*/

    public AllMatches importMatchesFPM (String competitionID) throws ExecutionException, InterruptedException {

        String url = "https://api.football-data.org/v2/competitions/" + competitionID + "/matches";

        Gson gson = new Gson();

        return gson.fromJson(getJsonStringByUrl(url), AllMatches.class);
    }

    private String getJsonStringByUrl(final String urlTarget) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable <String> callable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                URL url = new URL(urlTarget);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();

                http.setRequestMethod("GET");
                http.setRequestProperty("X-Auth-Token", TOKEN);
                http.setUseCaches(false);
                http.setAllowUserInteraction(false);
                http.connect();

                int status = http.getResponseCode();
                if (status == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }

                    br.close();
                    return sb.toString();

                } else {
                    throw new Exception("Status "+status);
                }
            }
        };
        Future<String> future = executor.submit(callable);
        executor.shutdown();
        return future.get();
    }


}

