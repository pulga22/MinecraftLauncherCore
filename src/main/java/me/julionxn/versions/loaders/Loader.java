package me.julionxn.versions.loaders;

import me.julionxn.versions.installers.LoaderInstaller;

public abstract class Loader {

    private final String version;

    public Loader(String version){
        this.version = version;
    }

    public String getVersion(){
        return version;
    }

    public abstract LoaderInstaller getInstaller();

}
