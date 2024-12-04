package me.julionxn.versions;

import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.files.SystemController;
import me.julionxn.versions.loaders.FabricLoader;

import java.util.Optional;

public class VersionsController {

    private final CoreLogger logger;
    private final SystemController systemController;
    private final DataController dataController;

    public VersionsController(CoreLogger logger, SystemController systemController, DataController dataController){
        this.logger = logger;
        this.systemController = systemController;
        this.dataController = dataController;
    }

    public Optional<MinecraftVersion> installVersion(String version, ProgressCallback callback){
        MinecraftVersion minecraftVersion = new MinecraftVersion(version, null);
        return installVersion(minecraftVersion, callback);
    }

    public Optional<MinecraftVersion> installVersion(String version, FabricLoader fabricLoader, ProgressCallback callback){
        MinecraftVersion minecraftVersion = new MinecraftVersion(version, fabricLoader);
        return installVersion(minecraftVersion, callback);
    }

    public Optional<MinecraftVersion> installVersion(MinecraftVersion minecraftVersion, ProgressCallback callback){
        boolean loaded = minecraftVersion.loadMetadata(logger, systemController, callback);
        if (loaded){
            logger.info("Metadata of version " + minecraftVersion.getVersion() + " loaded successfully.");
            VersionInstaller installer = new VersionInstaller(logger, minecraftVersion, systemController, dataController, callback);
            installer.install();
            logger.info("Version " + minecraftVersion.getVersion() + " installed.");
            return Optional.of(minecraftVersion);
        } else {
            logger.error("Error loading metadata of version " + minecraftVersion.getVersion() + ".");
            return Optional.empty();
        }
    }

}
