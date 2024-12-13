package io.github.julionxn.version.loaders;

import io.github.julionxn.version.installers.LoaderInstaller;
import org.jetbrains.annotations.NotNull;

public abstract class Loader {

    private final String version;

    public Loader(String version){
        this.version = version;
    }

    public String getVersion(){
        return version;
    }

    public abstract @NotNull LoaderInstaller getInstaller();

}
