package me.julionxn.versions.installers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.data.TempFolder;
import me.julionxn.files.Natives;
import me.julionxn.versions.FetchingUtils;
import me.julionxn.versions.Library;
import me.julionxn.versions.MavenMetadata;
import me.julionxn.versions.MinecraftVersion;
import me.julionxn.versions.loaders.Loader;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FabricInstaller extends LoaderInstaller {

    private static final String FABRIC_INSTALLER_MAVEN_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";

    private final List<String> JVMArgs = new ArrayList<>();
    private List<Library> libraries;

    @Override
    public boolean install(CoreLogger logger, Loader loader, MinecraftVersion minecraftVersion, DataController dataController, String osName, Natives natives, ProgressCallback callback) {
        String fabricLoaderVersion = loader.getVersion();
        TempFolder tempFolder = dataController.prepareTempFolder();
        Path tempFolderPath = tempFolder.path();
        downloadFabricFiles(logger, minecraftVersion, dataController, tempFolderPath, fabricLoaderVersion);
        Path librariesPath = tempFolderPath.resolve("libraries");
        libraries = parseLibraries(logger, librariesPath, dataController.getLibrariesPath());
        moveContents(librariesPath, dataController.getLibrariesPath());
        String versionLoader = "fabric-loader-" + fabricLoaderVersion + "-" + minecraftVersion.getVersion();
        Path versionDataPath = tempFolderPath.resolve("versions")
                .resolve(versionLoader).resolve(versionLoader + ".json");
        Optional<JsonObject> versionDataOpt = loadJson(versionDataPath);
        if (versionDataOpt.isEmpty()) return false;
        JsonObject versionData = versionDataOpt.get();
        minecraftVersion.setMainClass(versionData.get("mainClass").getAsString());
        JsonObject args = versionData.getAsJsonObject("arguments");
        JsonArray jvmArgs = args.getAsJsonArray("jvm");
        for (JsonElement jvmArg : jvmArgs) {
            String arg = jvmArg.getAsString().replace(" ", "");
            JVMArgs.add(arg);
        }
        tempFolder.close().run();
        return true;
    }

    private void downloadFabricFiles(CoreLogger logger, MinecraftVersion minecraftVersion, DataController dataController, Path tempFolderPath, String fabricLoaderVersion){
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
        File installerFile = dataController.prepareFile(tempFolderPath.resolve(fabricLoaderVersion + ".jar"));
        DownloadStatus status = downloadFile(downloadUrl, installerFile);
        if (status != DownloadStatus.OK) {
            logger.error("Failed to download Fabric Installer. Code: " + status + ".");
            return;
        }
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
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing the command: " + command, e);
        }
    }

    private Optional<String> getLatestInstallerVersion(CoreLogger logger){
        try {
            MavenMetadata mavenMetadata = FetchingUtils.parseMavenMetadata(FABRIC_INSTALLER_MAVEN_URL);
            return Optional.of(mavenMetadata.latest());
        } catch (IOException e) {
            logger.error("Error while fetching Maven Metadata " + FABRIC_INSTALLER_MAVEN_URL + ".", e);
            return Optional.empty();
        }
    }

    private List<Library> parseLibraries(CoreLogger logger, Path librariesPath, Path finalLibrariesPath){
        Path baseDir = librariesPath.getParent();
        Path targetDir = finalLibrariesPath.getParent();
        Set<Path> paths;
        try (Stream<Path> jarFiles = Files.walk(librariesPath)) {
             paths = jarFiles.filter(path -> path.toString().endsWith(".jar"))
                    .map(path -> {
                        Path relativePath = baseDir.relativize(path);
                        return targetDir.resolve(relativePath);
                    }).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Library> fabricLibraries = new ArrayList<>();
        for (Path path : paths) {
            String version = path.getParent().getFileName().toString();
            String artifact = path.getParent().getParent().getFileName().toString();
            fabricLibraries.add(new Library(artifact, version, path));
        }
        return fabricLibraries;
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
