package me.julionxn.versions.loaders;

import me.julionxn.versions.installers.LoaderInstaller;

public abstract class Loader {

    private final String version;
    protected LoaderType loader;

    public Loader(String version){
        this.version = version;
        this.loader = LoaderType.VANILLA;
    }

    public String getVersion(){
        return version;
    }

    public LoaderType getLoader(){
        return loader;
    }

    public abstract LoaderInstaller getInstaller();

}
