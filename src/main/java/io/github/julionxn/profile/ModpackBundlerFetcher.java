package io.github.julionxn.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.julionxn.CoreLogger;
import io.github.julionxn.data.DataController;
import io.github.julionxn.data.TempFolder;
import io.github.julionxn.utils.FetchingUtils;
import io.github.julionxn.version.MinecraftVersion;
import io.github.julionxn.version.installers.DownloadStatus;
import io.github.julionxn.version.loaders.FabricLoader;
import io.github.julionxn.version.loaders.Loader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ModpackBundlerFetcher extends ProfilesFetcher {

    public ModpackBundlerFetcher(CoreLogger logger, URL url) {
        super(logger, url);
    }

    @Override
    public @Nullable URLProfiles fetch(ProfilesController profilesController, DataController dataController, URL url) {
        Optional<URL> manifestUrlOpt = resolveUrl(url, "manifest.json");
        if (manifestUrlOpt.isEmpty()) return null;
        URL manifestUrl = manifestUrlOpt.get();
        Optional<JsonObject> manifestOpt = fetchManifest(manifestUrl);
        if (manifestOpt.isEmpty()) return null;
        JsonObject manifest = manifestOpt.get();
        Set<Map.Entry<String, JsonObject>> entries = manifest.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getAsJsonObject()))
                .collect(Collectors.toSet());

        URLProfiles urlProfiles = new URLProfiles();
        for (Map.Entry<String, JsonObject> entry : entries) {
            String profileName = entry.getKey();
            JsonObject profileJson = entry.getValue();
            String profileManifestUrlStr = profileJson.get("manifest").getAsString();
            Optional<URL> profileManifestUrlOpt = resolveUrl(url, profileManifestUrlStr);
            if (profileManifestUrlOpt.isEmpty()) continue;
            URL profileManifestUrl = profileManifestUrlOpt.get();
            JsonObject profileFiles = profileJson.getAsJsonObject("files");
            Optional<JsonObject> profileManifestOpt = fetchManifest(profileManifestUrl);
            if (profileManifestOpt.isEmpty()) continue;
            JsonObject profileManifest = profileManifestOpt.get();
            String version = profileManifest.get("version").getAsString();
            JsonObject loader = profileManifest.getAsJsonObject("loader");
            String loaderType = loader.get("type").getAsString();
            String loaderVersion = loader.get("version").getAsString();
            Loader minecraftVersionLoader = null;
            if (!loaderType.equals("vanilla")){
                minecraftVersionLoader = new FabricLoader(loaderVersion);
            }
            MinecraftVersion minecraftVersion = new MinecraftVersion(version, minecraftVersionLoader);
            String description = profileManifest.get("description").getAsString();
            JsonObject imageData = profileManifest.getAsJsonObject("image");
            boolean hasImage = imageData.get("has").getAsBoolean();
            List<UUID> validUUIDs = profileManifest.getAsJsonArray("validUUIDs")
                    .asList().stream()
                    .map(element -> {
                        String uuidStr = element.getAsString();
                        return UUID.fromString(uuidStr);
                    }).toList();
            String imageFileName = null;
            if (hasImage){
                imageFileName = Path.of(imageData.get("path").getAsString()).getFileName().toString();
            }
            Optional<TempProfile> tempProfileOpt = getTempProfile(profilesController, dataController, profileName, description, hasImage, imageFileName, url, profileFiles);
            if (tempProfileOpt.isEmpty()) continue;
            TempProfile tempProfile = tempProfileOpt.get();
            URLProfile urlProfile = new URLProfile(minecraftVersion, tempProfile, validUUIDs);
            urlProfiles.addProfile(urlProfile);
        }
        return urlProfiles;
    }

    private Optional<TempProfile> getTempProfile(ProfilesController profilesController, DataController dataController, String profileName, String description, boolean hasImage, @Nullable String imageFileName, URL url, JsonObject files){
        TempFolder tempFolder = dataController.prepareTempFolder();
        Path tempImagePath = null;
        Path imagePath = null;
        if (hasImage && imageFileName != null){
            imagePath = profilesController.getProfilesPath().resolve(profileName).resolve("profile.png");
            tempImagePath = tempFolder.path().resolve(profileName).resolve(imageFileName);
        }
        TempProfile tempProfile = new TempProfile(profileName, tempFolder, tempImagePath, imagePath, description, profilesController);
        try {
            downloadProfileItems(tempFolder, url, files);
            return Optional.of(tempProfile);
        } catch (IOException e) {
            logger.error("Error downloading profile: " + profileName, e);
            return Optional.empty();
        }
    }

    private void downloadProfileItems(TempFolder tempFolder, URL url, JsonObject files) throws IOException {
        Path baseDirectory = tempFolder.path();
        Deque<Map.Entry<String, JsonObject>> deque = files.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getAsJsonObject()))
                .distinct()
                .collect(Collectors.toCollection(ArrayDeque::new));
        while (!deque.isEmpty()) {
            Map.Entry<String, JsonObject> entry = deque.poll();
            String path = entry.getKey();
            JsonObject details = entry.getValue();
            Path resolvedPath = baseDirectory.resolve(path);
            if (details.get("type").getAsString().equals("file")) {
                Files.createDirectories(resolvedPath.getParent());
                Path filePath = Files.createFile(resolvedPath);
                String hash = details.get("hash").getAsString();
                int size = details.get("size").getAsInt();
                Optional<URL> urlOpt = resolveUrl(url, path);
                if (urlOpt.isEmpty()) continue;
                DownloadStatus status = downloadAndCheckFile(urlOpt.get(), hash, size, filePath.toFile(), () ->
                        downloadFile(urlOpt.get(), filePath.toFile(), hash, size)
                );
                if (status != DownloadStatus.OK) {
                    logger.error("Something happened while downloading " + path + ", CODE: " + status + ".");
                } else {
                    logger.info("Created file: " + resolvedPath);
                }
            } else if (details.get("type").getAsString().equals("directory")) {
                Files.createDirectories(resolvedPath);
                logger.info("Created directory: " + resolvedPath);
                JsonObject nestedFiles = details.getAsJsonObject("files");
                if (nestedFiles != null) {
                    Set<Map.Entry<String, JsonElement>> entries = nestedFiles.entrySet();
                    if (!entries.isEmpty()){
                        deque.addAll(nestedFiles.entrySet().stream()
                                .map(nestedEntry -> Map.entry(nestedEntry.getKey(), nestedEntry.getValue().getAsJsonObject()))
                                .collect(Collectors.toSet()));
                    }
                }
            }
        }
    }

    private Optional<URL> resolveUrl(URL url, String path){
        try {
            return Optional.of(url.toURI().resolve(path).toURL());
        } catch (URISyntaxException | MalformedURLException e) {
            logger.error("Error resolving URL: " + url + " + " + path, e);
            return Optional.empty();
        }
    }

    private Optional<JsonObject> fetchManifest(URL url){
        try {
            return FetchingUtils.fetchJsonData(url);
        } catch (IOException e) {
            logger.error("Error fetching manifest: " + url, e);
            return Optional.empty();
        }
    }

}
