package io.github.julionxn.profile;

import io.github.julionxn.CoreLogger;
import io.github.julionxn.data.DataController;
import io.github.julionxn.version.installers.Installer;
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
