package me.julionxn.version.installers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.data.TempFolder;
import me.julionxn.system.Natives;
import me.julionxn.version.FetchingUtils;
import me.julionxn.version.MinecraftVersion;
import me.julionxn.version.data.Library;
import me.julionxn.version.data.MavenMetadata;
import me.julionxn.version.loaders.Loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class FabricInstaller extends LoaderInstaller {

    private static final String FABRIC_INSTALLER_MAVEN_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";
    private static final String STATUS = "Installing FabricLoader";

    private final List<String> JVMArgs = new ArrayList<>();
    private List<Library> libraries;

    @Override
    public boolean install(CoreLogger logger, MinecraftVersion minecraftVersion, DataController dataController, String osName, Natives natives, ProgressCallback callback) {
        callback.onProgress(STATUS, 0);
        Loader loader = minecraftVersion.getLoader();
        if (loader == null) return false;
        String fabricLoaderVersion = loader.getVersion();
        TempFolder tempFolder = dataController.prepareTempFolder();
        Path tempFolderPath = tempFolder.path();
        downloadFabricFiles(logger, minecraftVersion, tempFolderPath, fabricLoaderVersion, callback);
        Path librariesPath = tempFolderPath.resolve("libraries");
        libraries = parseLibraries(logger, librariesPath, dataController.getLibrariesPath());
        try {
            moveContents(librariesPath, dataController.getLibrariesPath());
            callback.onProgress(STATUS, 0.75f);
        } catch (IOException e) {
            logger.error("Error moving libraries: " + librariesPath, e);
        }
        String versionLoader = "fabric-loader-" + fabricLoaderVersion + "-" + minecraftVersion.getVersion();
        Path versionDataPath = tempFolderPath.resolve("versions")
                .resolve(versionLoader).resolve(versionLoader + ".json");
        JsonObject versionData;
        try {
            versionData = loadJson(versionDataPath);
        } catch (IOException e) {
            logger.error("Error trying to get JSON: " + versionDataPath, e);
            return false;
        }
        minecraftVersion.setMainClass(versionData.get("mainClass").getAsString());
        JsonObject args = versionData.getAsJsonObject("arguments");
        JsonArray jvmArgs = args.getAsJsonArray("jvm");
        for (JsonElement jvmArg : jvmArgs) {
            String arg = jvmArg.getAsString().replace(" ", "");
            JVMArgs.add(arg);
        }
        tempFolder.close().run();
        callback.onProgress(STATUS, 1f);
        return true;
    }

    private void downloadFabricFiles(CoreLogger logger, MinecraftVersion minecraftVersion, Path tempFolderPath, String fabricLoaderVersion, ProgressCallback callback) {
        Optional<String> latestInstallerVersionOpt = getLatestInstallerVersion(logger);
        if (latestInstallerVersionOpt.isEmpty()) return;
        String latestInstallerVersion = latestInstallerVersionOpt.get();
        String downloadUrlStr = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/" +
                latestInstallerVersion + "/fabric-installer-" + latestInstallerVersion + ".jar";
        Optional<URL> downloadUrlOpt = FetchingUtils.getURL(downloadUrlStr);
        if (downloadUrlOpt.isEmpty()) {
            logger.error("Error trying to get URL from MalformedURL: " + downloadUrlStr);
            return;
        }
        URL downloadUrl = downloadUrlOpt.get();
        File installerFile = tempFolderPath.resolve(fabricLoaderVersion + ".jar").toFile();
        DownloadStatus status = downloadFile(downloadUrl, installerFile);
        if (status != DownloadStatus.OK) {
            logger.error("Failed to download Fabric Installer. Code: " + status + ".");
            return;
        }
        callback.onProgress(STATUS, 0.25f);
        String command = "java -jar " +
                installerFile.toPath() +
                " client -dir " +
                tempFolderPath +
                " -mcversion " +
                minecraftVersion.getVersion() +
                " -loader " +
                fabricLoaderVersion +
                " -noprofile -snapshot";

        logger.info("Starting command execution: " + command);

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Command execution failed with exit code " + exitCode);
                logger.error("Output: " + output);
            } else {
                logger.info("Command executed successfully.");
                logger.info("Output: " + output);
            }
            callback.onProgress(STATUS, 0.5f);
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing the command: " + command, e);
        }
    }

    private Optional<String> getLatestInstallerVersion(CoreLogger logger){
        try {
            Optional<MavenMetadata> mavenMetadataOpt = FetchingUtils.parseMavenMetadata(FABRIC_INSTALLER_MAVEN_URL);
            return mavenMetadataOpt.map(MavenMetadata::latest);
        } catch (IOException e) {
            logger.error("Error while fetching Maven Metadata " + FABRIC_INSTALLER_MAVEN_URL + ".", e);
            return Optional.empty();
        }
    }

    private List<Library> parseLibraries(CoreLogger logger, Path librariesPath, Path finalLibrariesPath){
        Path baseDir = librariesPath.getParent();
        Path targetDir = finalLibrariesPath.getParent();
        try (Stream<Path> jarFiles = Files.walk(librariesPath)) {
             return jarFiles.filter(path -> path.toString().endsWith(".jar"))
                    .map(path -> {
                        Path relativePath = baseDir.relativize(path);
                        Path finalPath = targetDir.resolve(relativePath);
                        String artifact = finalPath.getParent().getParent().getFileName().toString();
                        String version = finalPath.getParent().getFileName().toString();
                        return new Library(artifact, version, finalPath);
                    }).toList();
        } catch (IOException e) {
            logger.error("Error while parsing libraries", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getJVMArgs() {
        return JVMArgs;
    }

    @Override
    public List<Library> getLibraries() {
        return libraries;
    }

}
