package io.github.julionxn.data;

import io.github.julionxn.CoreLogger;
import io.github.julionxn.utils.FilesUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class DataController {

    private final CoreLogger logger;
    private final Path assetsPath;
    private final Path librariesPath;
    private final Path versionsPath;
    private final Path nativesPath;
    private final Path runtimesPath;
    private final Path tempPath;

    public DataController(CoreLogger logger, Path dataPath) {
        this.logger = logger;
        this.assetsPath = dataPath.resolve("assets");
        this.librariesPath = dataPath.resolve("libraries");
        this.versionsPath = dataPath.resolve("versions");
        this.nativesPath = dataPath.resolve("natives");
        this.runtimesPath = dataPath.resolve("runtimes");
        this.tempPath = dataPath.resolve("temp");
    }

    public Path getNativesPath() {
        return nativesPath;
    }

    public Path getAssetsPath() {
        return assetsPath;
    }

    public Path getVersionsPath() {
        return versionsPath;
    }

    public Path getLibrariesPath() {
        return librariesPath;
    }

    public Path getRuntimesPath() {
        return runtimesPath;
    }

    public Optional<File> prepareAssetObjectFile(String hash) {
        String folderName = hash.substring(0, 2);
        File folderFile = assetsPath.resolve("objects").resolve(folderName).toFile();
        if (!folderFile.exists() && !folderFile.mkdir()) {
            logger.error("Failed to prepare Asset Object File " + hash + ".");
            return Optional.empty();
        }
        File objectFile = folderFile.toPath().resolve(hash).toFile();
        logger.info("Preparing Asset Object File " + hash + ".");
        return Optional.of(objectFile);
    }

    public Optional<File> prepareAssetIndexFile(String id) {
        File indexFile = assetsPath.resolve("indexes").resolve(id + ".json").toFile();
        if (indexFile.exists()) return Optional.empty();
        logger.info("Preparing Asset Index File " + id + ".");
        return Optional.of(indexFile);
    }

    public Optional<File> prepareLibraryFile(String path) {
        File libraryFile = librariesPath.resolve(path).toFile();
        File parentDir = libraryFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            logger.error("Failed to prepare Library File " + path + ".");
            return Optional.empty();
        }
        logger.info("Preparing Library File " + path + ".");
        return Optional.of(libraryFile);
    }

    public Optional<Path> prepareNativesFolder(String version) {
        Path folder = nativesPath.resolve(version);
        File file = folder.toFile();
        if (!file.exists() && !file.mkdir()) {
            logger.info("Error while preparing Native Folder " + version + ".");
            return Optional.empty();
        } else {
            logger.info("Preparing Natives Folder " + version + ".");
            return Optional.of(folder);
        }
    }

    public Optional<Path> prepareRuntimeFolder(String name) {
        Path folder = runtimesPath.resolve(name);
        File file = folder.toFile();
        if (!file.exists() && !file.mkdir()) {
            logger.error("Error while preparing Runtimes Folder " + name + ".");
            return Optional.empty();
        } else {
            logger.info("Preparing Runtime Folder " + name + ".");
            return Optional.of(folder);
        }
    }

    public Optional<Path> prepareVersionFolder(String version) {
        File versionFolder = versionsPath.resolve(version).toFile();
        if (!versionFolder.exists() && !versionFolder.mkdir()) {
            logger.error("Failed to prepare Version Folder " + version + ".");
            return Optional.empty();
        }
        logger.info("Preparing Version Folder " + version + ".");
        return Optional.of(versionFolder.toPath());
    }

    public Optional<File> prepareVersionJarFile(String version) {
        Optional<Path> versionFolderOpt = prepareVersionFolder(version);
        if (versionFolderOpt.isEmpty()) return Optional.empty();
        Path versionFolder = versionFolderOpt.get();
        File versionFile = versionFolder.resolve(version + ".jar").toFile();
        logger.info("Preparing Jar File " + version + ".");
        return Optional.of(versionFile);
    }

    public Optional<File> prepareVersionManifestFile(String version) {
        File versionFolder = versionsPath.resolve(version).toFile();
        if (!versionFolder.exists() && !versionFolder.mkdir()) {
            logger.error("Failed to prepare Manifest File " + version + ".");
            return Optional.empty();
        }
        File manifestFile = versionFolder.toPath().resolve(version + ".json").toFile();
        logger.info("Preparing Manifest File " + version + ".");
        return Optional.of(manifestFile);
    }

    public TempFolder prepareTempFolder() {
        Path folder = tempPath.resolve(String.valueOf(System.currentTimeMillis()));
        if (folder.toFile().mkdir()) {
            logger.info("Creating temp folder " + folder + ".");
        }
        Runnable delete = () -> {
            try {
                FilesUtils.deleteDirectory(folder);
                logger.info("Deleting temp folder " + folder);
                File file = tempPath.toFile();
                if (!file.exists() && !file.mkdir()) {
                    logger.error("Failed to prepare Temp Folder " + tempPath + ".");
                }
            } catch (IOException e) {
                logger.error("Failed to delete Temp Folder " + folder + ".");
            }
        };
        return new TempFolder(folder, delete);
    }

}
