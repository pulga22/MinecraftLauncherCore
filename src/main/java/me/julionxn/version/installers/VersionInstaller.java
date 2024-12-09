package me.julionxn.version.installers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.system.Natives;
import me.julionxn.system.SystemController;
import me.julionxn.version.MinecraftVersion;
import me.julionxn.version.data.AssetIndexInfo;
import me.julionxn.version.data.RuntimeComponentInfo;
import me.julionxn.version.data.VersionJarInfo;
import me.julionxn.version.loaders.Loader;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class VersionInstaller extends Installer {

    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";
    private final CoreLogger logger;
    private final MinecraftVersion minecraftVersion;
    private final DataController dataController;
    private final String osName;
    private final Natives natives;
    private final ProgressCallback callback;
    private final int threads = 4;

    public VersionInstaller(CoreLogger logger, MinecraftVersion minecraftVersion, SystemController systemController, DataController dataController, ProgressCallback callback){
        this.logger = logger;
        this.minecraftVersion = minecraftVersion;
        this.dataController = dataController;
        this.osName = systemController.getOsName();
        this.natives = systemController.getNatives();
        this.callback = callback;
    }

    public boolean install(){
        if (installAssets() &&
            installLibraries() &&
            installRuntime() &&
            installClientJar()){
            Loader loader = minecraftVersion.getLoader();
            if (loader != null){
                LoaderInstaller loaderInstaller = loader.getInstaller();
                return loaderInstaller.install(logger, minecraftVersion, dataController, osName, natives, callback);
            } else {
                return true;
            }

        }
        return false;
    }

    private boolean installAssets(){
        String status = "Installing Assets";
        callback.onProgress(status, 0);
        AssetIndexInfo assetIndexInfo = minecraftVersion.getAssetIndexInfo();
        JsonObject assetData = assetIndexInfo.assetIndexData();
        String assetId = assetIndexInfo.id();
        addAssetIndex(assetId, assetData);
        JsonObject objects = assetData.get("objects").getAsJsonObject();
        URI resourcesURI;
        try {
            resourcesURI = new URI(RESOURCES_URL);
        } catch (URISyntaxException e) {
            logger.error("Error getting URL " + RESOURCES_URL + ".", e);
            return false;
        }
        List<Map.Entry<String, JsonElement>> entries = objects.entrySet().stream().toList();
        int totalEntries = entries.size();
        AtomicInteger entriesDone = new AtomicInteger();
        List<List<Map.Entry<String, JsonElement>>> batches = splitIntoBatches(entries, threads);
        executeConcurrent(batches, batch -> {
            for (Map.Entry<String, JsonElement> objectEntry : batch) {
                JsonObject object = objectEntry.getValue().getAsJsonObject();
                String hash = object.get("hash").getAsString();
                int size = object.get("size").getAsInt();
                URL objectUrl;
                try {
                    objectUrl = resourcesURI.resolve(hash.substring(0, 2)).resolve(hash).toURL();
                } catch (MalformedURLException e) {
                    logger.error("Malformed URL: ", e);
                    entriesDone.getAndIncrement();
                    callback.onProgress(status, (float) entriesDone.get() / totalEntries);
                    continue;
                }
                downloadAssetObject(objectUrl, hash, size);
                entriesDone.getAndIncrement();
                callback.onProgress(status, (float) entriesDone.get() / totalEntries);
            }
        }, threads);
        return true;
    }

    private void addAssetIndex(String id, JsonObject assetData){
        Optional<File> assetIndexFileOpt = this.dataController.prepareAssetIndexFile(id);
        if (assetIndexFileOpt.isEmpty()) {
            logger.error("Error saving assetIndexFile.");
            return;
        }
        File assetIndexFile = assetIndexFileOpt.get();
        try {
            Files.write(assetIndexFile.toPath(), assetData.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadAssetObject(URL url, String hash, int expectedSize) {
        Optional<File> outputFileOptional = dataController.prepareAssetObjectFile(hash);
        if (outputFileOptional.isEmpty()){
            logger.error("Error preparing " + hash + " asset file.");
            return;
        }
        File outputFile = outputFileOptional.get();
        downloadAndCheckFile(url, hash, expectedSize, outputFile, () -> {
            downloadAssetObject(url, hash, expectedSize);
            return DownloadStatus.OK;
        });
    }

    private boolean installLibraries(){
        String status = "Installing Libraries";
        callback.onProgress(status, 0);
        JsonArray libraries = minecraftVersion.getLibraries();
        int totalLibraries = libraries.size();
        AtomicInteger librariesDone = new AtomicInteger();
        List<List<JsonElement>> batches = splitIntoBatches(libraries.asList(), threads);
        executeConcurrent(batches, batch -> {
            for (JsonElement library : batch) {
                JsonObject downloads = library.getAsJsonObject().get("downloads").getAsJsonObject();
                JsonElement rulesElement = library.getAsJsonObject().get("rules");
                if (rulesElement != null) {
                    JsonArray rules = rulesElement.getAsJsonArray();
                    boolean breakLibrary = false;
                    for (JsonElement ruleElement : rules) {
                        String libraryOs = ruleElement.getAsJsonObject().get("os").getAsJsonObject().get("name").getAsString();
                        if (libraryOs == null || !libraryOs.equals(osName)){
                            breakLibrary = true;
                        }
                    }
                    if (breakLibrary) {
                        librariesDone.getAndIncrement();
                        callback.onProgress(status, (float) librariesDone.get() / totalLibraries);
                        continue;
                    }
                }
                for (Map.Entry<String, JsonElement> downloadEntry : downloads.entrySet()) {
                    JsonObject download = downloadEntry.getValue().getAsJsonObject();
                    String path = download.get("path").getAsString();
                    String url = download.get("url").getAsString();
                    String hash = download.get("sha1").getAsString();
                    int size = download.get("size").getAsInt();
                    URL artifactUrl;
                    try{
                        artifactUrl = new URL(url);
                    } catch (MalformedURLException e) {
                        logger.error("Error getting URL " + url + ".", e);
                        librariesDone.getAndIncrement();
                        callback.onProgress(status, (float) librariesDone.get() / totalLibraries);
                        return;
                    }
                    Optional<File> file = downloadLibraryArtifact(artifactUrl, path, hash, size);
                    librariesDone.getAndIncrement();
                    callback.onProgress(status, (float) librariesDone.get() / totalLibraries);
                    if (!path.contains("natives") || file.isEmpty()) continue;
                    Natives fileNatives = getNatives(path);
                    if (fileNatives == natives){
                        extractNatives(minecraftVersion.getVersion(), file.get());
                    }
                }
            }
        }, threads);
        return true;
    }

    private Optional<File> downloadLibraryArtifact(URL url, String path, String hash, int expectedSize){
        Optional<File> outputFileOptional = dataController.prepareLibraryFile(path);
        if (outputFileOptional.isEmpty()){
            logger.error("Error preparing " + path + " library.");
            return Optional.empty();
        }
        File outputFile = outputFileOptional.get();
        downloadAndCheckFile(url, hash, expectedSize, outputFile, () -> {
            downloadLibraryArtifact(url, path, hash, expectedSize);
            return DownloadStatus.OK;
        });
        return Optional.of(outputFile);
    }

    private void extractNatives(String version, File file){
        Optional<Path> nativesFolderOpt = dataController.prepareNativesFolder(version);
        if (nativesFolderOpt.isEmpty()) return;
        Path nativesFolder = nativesFolderOpt.get();
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.endsWith(".dll") || entryName.endsWith(".dylib") || entryName.endsWith(".so")) {
                    String fileName = Paths.get(entryName).getFileName().toString();
                    Path outputPath = nativesFolder.resolve(fileName);
                    Files.createDirectories(outputPath.getParent());
                    try (InputStream inputStream = jar.getInputStream(entry);
                         OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                    logger.info("Natives extracted from " + fileName + ".");
                }
            }
        } catch (IOException e) {
            logger.error("Error extracting natives", e);
        }
    }

    private boolean installRuntime(){
        String status = "Installing Runtime";
        callback.onProgress(status, 0);
        RuntimeComponentInfo runtimeComponentInfo = minecraftVersion.getRuntimeComponentInfo();
        String componentVersion = runtimeComponentInfo.version();
        JsonObject componentData = runtimeComponentInfo.runtimeData();
        Optional<Path> componentFolderOpt = dataController.prepareRuntimeFolder(componentVersion);
        if (componentFolderOpt.isEmpty()) return false;
        Path componentFolder = componentFolderOpt.get();
        JsonObject files = componentData.get("files").getAsJsonObject();
        List<Map.Entry<String, JsonElement>> sortedFiles = getSortedArray(files);
        int totalEntries = sortedFiles.size();
        AtomicInteger entriesDone = new AtomicInteger();
        List<List<Map.Entry<String, JsonElement>>> batches = splitIntoBatches(sortedFiles, threads);
        executeConcurrent(batches, batch -> {
            for (Map.Entry<String, JsonElement> file : batch) {
                String key = file.getKey();
                JsonObject data = file.getValue().getAsJsonObject();
                String type = data.get("type").getAsString();
                Path filePath = componentFolder.resolve(key);
                if (filePath.toFile().exists()) {
                    logger.info("Runtime Object " + key + " already exists.");
                    entriesDone.getAndIncrement();
                    callback.onProgress(status, (float) entriesDone.get() / totalEntries);
                    continue;
                }
                File objectFile = filePath.toFile();
                downloadRuntimeFile(objectFile, key, type, data);
                entriesDone.getAndIncrement();
                callback.onProgress(status, (float) entriesDone.get() / totalEntries);
            }
        }, threads);
        return true;
    }

    private void downloadRuntimeFile(File file, String key, String type, JsonObject data){
        switch (type) {
            case "directory" -> file.mkdir();
            case "file" -> {
                boolean executable = data.get("executable").getAsBoolean();
                JsonObject downloads = data.get("downloads").getAsJsonObject();
                JsonObject fileData;
                if (downloads.has("raw")){
                    fileData = downloads.get("raw").getAsJsonObject();
                } else {
                    fileData = downloads.get("lzma").getAsJsonObject();
                }
                String sha1 = fileData.get("sha1").getAsString();
                int size = fileData.get("size").getAsInt();
                String urlStr = fileData.get("url").getAsString();
                URL url;
                try {
                    url = new URL(urlStr);
                } catch (MalformedURLException e) {
                    logger.error("Error getting URL " + urlStr + ".", e);
                    return;
                }
                DownloadStatus status = downloadFile(url, file, sha1, size);
                //if executable
                if (status == DownloadStatus.OK) {
                    logger.info("Runtime file " + key + " downloaded.");
                    if (executable && natives != Natives.WIN
                            && natives != Natives.WIN86
                            && natives != Natives.WIN64
                            && natives != Natives.WIN_ARM64) {
                        try {
                            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file.toPath());
                            perms.add(PosixFilePermission.OWNER_EXECUTE);
                            perms.add(PosixFilePermission.GROUP_EXECUTE);
                            perms.add(PosixFilePermission.OTHERS_EXECUTE);
                            Files.setPosixFilePermissions(file.toPath(), perms);
                        } catch (IOException e) {
                            logger.error("Error while setting a RuntimeFile as executable " + file.getPath(), e);
                        }
                    }
                } else {
                    logger.error("Failed to download RuntimeFile. Code: " + status + ".");
                }
            }
            case "link" -> {
                //todo
            }
        }
    }

    private List<Map.Entry<String, JsonElement>> getSortedArray(JsonObject files) {
        Set<Map.Entry<String, JsonElement>> entrySet = files.entrySet();
        List<Map.Entry<String, JsonElement>> sortedEntries = new ArrayList<>(entrySet);
        sortedEntries.sort(Map.Entry.comparingByKey());
        return sortedEntries;
    }

    private boolean installClientJar(){
        String status = "Installing ClientJar";
        callback.onProgress(status, 0);
        VersionJarInfo versionJarInfo = minecraftVersion.getVersionJarInfo();
        String version = minecraftVersion.getVersion();
        JsonObject versionData = minecraftVersion.getVersionData();
        String sha1 = versionJarInfo.sha1();
        int size = versionJarInfo.size();
        URL url = versionJarInfo.url();
        Optional<File> clientJarFileOpt = dataController.prepareVersionJarFile(version);
        if (clientJarFileOpt.isEmpty()) return false;
        File clientJarFile = clientJarFileOpt.get();
        DownloadStatus downloadStatus = downloadAndCheckFile(url, sha1, size, clientJarFile, () -> {
            installClientJar();
            return DownloadStatus.OK;
        });
        callback.onProgress(status, 0.5f);
        if (downloadStatus != DownloadStatus.OK){
            logger.error("Failed to download Client Jar. Code: " + downloadStatus + ".");
            if (downloadStatus != DownloadStatus.ALREADY_EXISTS){
                return false;
            }
        }
        Optional<File> clientJarManifestFileOpt = dataController.prepareVersionManifestFile(version);
        if (clientJarManifestFileOpt.isEmpty()) return false;
        File clientJarManifestFile = clientJarManifestFileOpt.get();
        String versionContent = versionData.toString();
        try {
            Files.write(clientJarManifestFile.toPath(), versionContent.getBytes());
            callback.onProgress(status, 1);
        } catch (IOException e) {
            logger.error("Error writing VersionJar to " + clientJarManifestFile.getPath(), e);
            return false;
        }
        return true;
    }

}
