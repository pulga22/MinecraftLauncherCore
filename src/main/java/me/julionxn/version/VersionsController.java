package me.julionxn.version;

import me.julionxn.CoreLogger;
import me.julionxn.ProgressCallback;
import me.julionxn.data.DataController;
import me.julionxn.system.SystemController;
import me.julionxn.version.loaders.Loader;

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

    public Optional<MinecraftVersion> installVersion(String version, Loader loader, ProgressCallback callback){
        MinecraftVersion minecraftVersion = new MinecraftVersion(version, loader);
        return installVersion(minecraftVersion, callback);
    }

    public Optional<MinecraftVersion> installVersion(MinecraftVersion minecraftVersion, ProgressCallback callback){
        boolean loaded = minecraftVersion.loadMetadata(logger, systemController, callback);
        if (loaded){
            logger.info("Metadata of version " + minecraftVersion.getVersion() + " loaded successfully.");
            VersionInstaller installer = new VersionInstaller(logger, minecraftVersion, systemController, dataController, callback);
            boolean success = installer.install();
            if (!success){
                return Optional.empty();
            }
            logger.info("Version " + minecraftVersion.getVersion() + " installed.");
            return Optional.of(minecraftVersion);
        } else {
            logger.error("Error loading metadata of version " + minecraftVersion.getVersion() + ".");
            return Optional.empty();
        }
    }

}
