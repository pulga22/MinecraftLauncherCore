package io.github.julionxn.version.loaders;

import io.github.julionxn.version.installers.FabricInstaller;
import io.github.julionxn.version.installers.LoaderInstaller;
import org.jetbrains.annotations.NotNull;

public class FabricLoader extends Loader {

    private FabricInstaller fabricInstaller;

    public FabricLoader(String version) {
        super(version);
    }

    @Override
    public @NotNull LoaderInstaller getInstaller() {
        if (fabricInstaller == null) {
            fabricInstaller = new FabricInstaller();
        }
        return fabricInstaller;
    }

}
