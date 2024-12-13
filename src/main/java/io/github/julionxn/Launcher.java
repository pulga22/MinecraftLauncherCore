package io.github.julionxn;

import io.github.julionxn.cache.CacheController;
import io.github.julionxn.data.DataController;
import io.github.julionxn.profile.ProfilesController;
import io.github.julionxn.system.SystemController;
import io.github.julionxn.version.VersionsController;

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
        this.dataController = new DataController(logger, launcherPath.resolve("data"));
        this.profilesController = new ProfilesController(logger, dataController, launcherPath.resolve("profiles"));
        this.versionsController = new VersionsController(logger, systemController, dataController);
        this.cacheController = new CacheController(logger, launcherPath.resolve("cache"));
    }

    public void start(){
        this.systemController.initialize();
        this.profilesController.loadProfiles();
        this.cacheController.initialize();
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
