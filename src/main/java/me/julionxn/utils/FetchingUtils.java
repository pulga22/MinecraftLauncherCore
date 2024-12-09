package me.julionxn.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.julionxn.version.data.MavenMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FetchingUtils {

    public static Optional<URL> getURL(String url){
        try {
            URL URL = new URL(url);
            return Optional.of(URL);
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    public static Optional<JsonObject> fetchJsonData(URL requestUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK){
            return Optional.empty();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder responseString = new StringBuilder();
        while ((inputLine = in.readLine()) != null){
            responseString.append(inputLine);
        }
        in.close();
        JsonObject response = JsonParser.parseString(responseString.toString()).getAsJsonObject();
        return Optional.of(response);
    }

    public static Optional<MavenMetadata> parseMavenMetadata(String url) throws IOException {
        Optional<String> metadataXmlOpt = fetchUrlContent(url);
        if (metadataXmlOpt.isEmpty()) return Optional.empty();
        String metadataXml = metadataXmlOpt.get();
        String release = extractFirstMatch("(?<=<release>).*?(?=</release>)", metadataXml);
        String latest = extractFirstMatch("(?<=<latest>).*?(?=</latest>)", metadataXml);
        List<String> versions = extractAllMatches(metadataXml);
        if (release == null || latest == null){
            return Optional.empty();
        }
        return Optional.of(new MavenMetadata(release, latest, versions));
    }

    private static Optional<String> fetchUrlContent(String urlString) throws IOException {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
            return Optional.empty();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return Optional.of(content.toString());
    }

    private static String extractFirstMatch(String regex, String text) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private static List<String> extractAllMatches(String text) {
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?<=<version>).*?(?=</version>)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    public static String getClassPathSeparator(String osName){
        return osName.equals("windows") ? ";" : ":";
    }

}
