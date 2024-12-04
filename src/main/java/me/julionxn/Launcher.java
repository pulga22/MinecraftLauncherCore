package me.julionxn;

import me.julionxn.cache.CacheController;
import me.julionxn.data.DataController;
import me.julionxn.files.SystemController;
import me.julionxn.profiles.ProfilesController;
import me.julionxn.versions.VersionsController;

import java.nio.file.Path;

public class Launcher {

    private final LauncherData launcherData;
    private final CoreLogger logger;
    private final ProfilesController profilesController;
    private final SystemController systemController;
    private final DataController dataController;
    private final CacheController cacheController;
    private final VersionsController versionsController;

    public Launcher(LauncherData launcherData){
        this.launcherData = launcherData;
        Path launcherPath = launcherData.rootPath().resolve(launcherData.launcherName());
        this.logger = new CoreLogger(launcherData);
        this.systemController = new SystemController(logger, launcherData);
        this.systemController.initialize();
        this.profilesController = new ProfilesController(logger, launcherPath.resolve("profiles"));
        this.profilesController.loadProfiles();
        this.dataController = new DataController(logger, launcherPath.resolve("data"));
        this.versionsController = new VersionsController(logger, systemController, dataController);
        this.cacheController = new CacheController(logger, launcherPath.resolve("cache"));
    }

    public ProfilesController getProfilesController(){
        return profilesController;
    }

    public LauncherData getLauncherData(){
        return launcherData;
    }

    public SystemController getSystemController(){
        return systemController;
    }

    public VersionsController getVersionsController(){
        return versionsController;
    }

    public DataController getDataController() {
        return dataController;
    }

    public CacheController getCacheController() {
        return cacheController;
    }

    public CoreLogger getLogger(){
        return logger;
    }
}
