package me.julionxn.versions.loaders;

import me.julionxn.versions.installers.FabricInstaller;
import me.julionxn.versions.installers.LoaderInstaller;

public class FabricLoader extends Loader {

    private FabricInstaller fabricInstaller;

    public FabricLoader(String version) {
        super(version);
    }

    @Override
    public LoaderInstaller getInstaller() {
        if (fabricInstaller == null) {
            fabricInstaller = new FabricInstaller();
        }
        return fabricInstaller;
    }

}
