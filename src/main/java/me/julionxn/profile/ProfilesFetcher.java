package me.julionxn.profile;

import me.julionxn.CoreLogger;
import me.julionxn.data.DataController;
import me.julionxn.version.installers.Installer;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public abstract class ProfilesFetcher extends Installer {

    protected final CoreLogger logger;
    public final URL url;


    protected ProfilesFetcher(CoreLogger logger, URL url) {
        this.logger = logger;
        this.url = url;
    }

    public abstract @Nullable URLProfiles fetch(ProfilesController profilesController, DataController dataController, URL url);

}
